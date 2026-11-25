#!/usr/bin/env python3
"""보존형 검색 비교 실습. 생성 모델·제품 API·기존 DB는 호출하지 않는다."""
import argparse
from collections import Counter
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import signal
import socket
import subprocess
import sys
import time
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
LAB = Path('.local/rag-api/retrieval-expansion')
MANIFEST = Path('docs/verification/coaching-retrieval-sources.json')
SPEC = Path('api/src/test/resources/verification/coaching-retrieval-corpus.json')
CASES = Path('api/src/test/resources/verification/coaching-retrieval-cases.json')
RUN = re.compile(r'[a-z0-9][a-z0-9-]{0,63}\Z')
FIELDS = {'cropName','workType','species','variety','harvestPart','growthStage','cultivation','propagation','region'}
DB = 'ccc_coaching_retrieval'
VOLUME = 'ccc-coaching-retrieval-postgres-v2'
EMBED_DIGEST = '7907646426070047a77226ac3e684fbbe8410524f7b4a74d02837e43f2146bab'
CODE = ['scripts/coaching-retrieval.py', 'compose.coaching-retrieval.yml', 'api/build.gradle.kts',
        'api/src/test/java/com/chamchamcham/lab/CoachingRetrievalLab.java',
        'api/src/test/java/com/chamchamcham/lab/CoachingEvidenceSelector.java',
        'application/src/main/java/com/chamchamcham/application/coaching/recordfeedback/generation/RecordFeedbackRetrievalQueryPlanner.java']


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def source_path(root, name):
    root = root.resolve()
    path = (root / name).resolve()
    allowed = ['.local/rag-api/corpus', '.local/rag-api/evidence-selection/sources', str(LAB / 'sources')]
    if not any(path.is_relative_to(root / folder) for folder in allowed) or not path.is_file():
        raise ValueError('invalid source path')
    return path


def build_corpus(root, manifest, spec):
    sources = {}
    for row in manifest['sources']:
        if row['sourceId'] in sources: raise ValueError('duplicate source')
        for kind in ['raw','selected']:
            if sha(source_path(root,row[kind+'File'])) != row[kind+'Sha256']: raise ValueError('source hash mismatch')
        sources[row['sourceId']] = row
    for name, expected in manifest['preservedHashes'].items():
        path = (root/name).resolve()
        if not path.is_relative_to(root.resolve()) or sha(path) != expected: raise ValueError('preserved hash mismatch')
    groups = {name: [] for name in ['baseline','split','expanded']}
    seen = set()
    for row in spec['segments']:
        if row['id'] in seen: raise ValueError('duplicate segment')
        seen.add(row['id'])
        cs, ns = row['constraints'], row['notStated']
        if (not isinstance(cs,dict) or not isinstance(ns,list) or len(set(ns))!=len(ns)
                or set(cs)&set(ns) or set(cs)|set(ns)!=FIELDS or type(row['unresolvedScope']) is not bool
                or not row['caveats'] or not all(isinstance(x,str) and x.strip() for x in row['caveats'])):
            raise ValueError('incomplete annotation')
        for c in cs.values():
            if (c['state'] not in ['EXPLICIT','NOT_STATED','UNRESTRICTED'] or not isinstance(c['allowed'],list)
                    or (c['state']=='EXPLICIT') != bool(c['allowed'])
                    or not all(isinstance(x,str) and x for x in c['allowed'])): raise ValueError('invalid annotation')
        source = sources[row['sourceId']]
        text = source_path(root,source['selectedFile']).read_text(encoding='utf-8')
        offset, length = row['offset'],row['length']
        if type(offset) is not int or type(length) is not int or offset<0 or length<1 or offset+length>len(text):
            raise ValueError('invalid source range')
        text = text[offset:offset+length]
        if hashlib.sha256(text.encode()).hexdigest() != row['textSha256']: raise ValueError('range hash mismatch')
        metadata = dict(row['metadata'],sourceId=row['sourceId'])
        constraints = dict(cs, **{field:{'state':'NOT_STATED','allowed':[]} for field in ns})
        doc = dict(id=row['id'],text=text,metadata=metadata,parentId=row.get('parentId'),
                   rule=dict(textSha256=row['textSha256'],constraints=constraints,
                             unresolvedScope=row['unresolvedScope'],caveats=row['caveats']))
        if not row['groups'] or len(set(row['groups']))!=len(row['groups']): raise ValueError('invalid groups')
        for group in row['groups']:
            if group not in groups: raise ValueError('invalid group')
            groups[group].append(doc)
    return groups


def inputs(root):
    manifest = json.loads((root/MANIFEST).read_text())
    verify_prepared(root,manifest,json.loads((root/SPEC).read_text()),json.loads((root/LAB/'prepared.json').read_text()))
    validate_cases(json.loads((root/CASES).read_text()))
    names = set(CODE+[str(MANIFEST),str(SPEC),str(CASES),str(LAB/'prepared.json')])
    names.update(manifest['preservedHashes'])
    for row in manifest['sources']:
        names.update([row['rawFile'],row['selectedFile']])
    return {name:sha(root/name) for name in sorted(names)}


def verify_prepared(root, manifest, spec, prepared):
    if build_corpus(root,manifest,spec)!=prepared:raise ValueError('prepared data differs from authoritative corpus; run prepare')


def validate_cases(data):
    crops=['천궁','참당귀','구기자나무','도라지','고추','상추']
    works=['PLANTING','WATERING','FERTILIZING','PEST_CONTROL','WEEDING','PRUNING','HARVEST','ETC']
    counts=Counter();matrix=Counter();ids=set()
    for row in data['cases']:
        split=row['split']
        if split not in ['dev','evaluation'] or row['id'] in ids or row.get('synthetic') is not True:
            raise ValueError('invalid case/split')
        ids.add(row['id']);counts[split]+=1
        matrix[(split,row['context']['crop']['name'],row['context']['record']['workType'])]+=1
    expected={(s,c,w):(1 if s=='dev' else 2) for s in ['dev','evaluation'] for c in (crops[:3] if s=='dev' else crops) for w in works}
    if dict(matrix)!=expected:raise ValueError('incomplete case matrix')
    return dict(counts)


def can_initialize(new_volume, relations, schemas):
    if not new_volume or relations or schemas!=['public']:raise ValueError('only a newly created empty volume can receive identity')
    return True


def validate_model(tags, loaded):
    if not any(x['name']=='bge-m3:latest' and x['digest']==EMBED_DIGEST for x in tags):
        raise ValueError('embedding model identity mismatch; no download')
    if any(x['name']!='bge-m3:latest' for x in loaded):raise ValueError('another model is loaded; do not evict it')


def new_run(root, name):
    if not RUN.fullmatch(name): raise ValueError('invalid run name')
    root = root.resolve()
    parent = root/LAB/'runs'
    if parent.resolve() != parent.absolute(): raise ValueError('symlink run path')
    parent.mkdir(parents=True,exist_ok=True)
    path=parent/name;path.mkdir()
    return path


def read_summary(root,name):
    if not RUN.fullmatch(name):raise ValueError('invalid run name')
    run=root/LAB/'runs'/name
    identity=json.loads((run/'identity.json').read_text())
    if sha(run/'result.json')!=identity['resultSha256']:raise ValueError('result hash mismatch')
    data=json.loads((run/'result.json').read_text())
    if not data['complete'] or not data['inputHashes']:raise ValueError('incomplete result')
    for name,expected in data['inputHashes'].items():
        p=(root/name).resolve()
        if not p.is_relative_to(root.resolve()) or not p.is_file() or sha(p)!=expected:raise ValueError('input changed')
    return data['summary']


def database_mode(identity, token, tables, schemas):
    if identity!=token or schemas!=['public']:raise ValueError('foreign database identity/schema')
    if set(tables)=={'lab_identity'}:return 'EMPTY'
    if set(tables)=={'lab_identity','baseline','split','expanded'}:return 'READY'
    raise ValueError('unexpected database tables')


def execute(args,**kw):
    return subprocess.run(args,check=True,capture_output=True,**kw)


def settings():
    path=ROOT/LAB/'.env'
    data=dict(line.split('=',1) for line in path.read_text().splitlines())
    if set(data)!={'POSTGRES_PASSWORD','LAB_TOKEN'} or not all(re.fullmatch(r'[A-Za-z0-9_-]+',x) for x in data.values()):
        raise ValueError('invalid credentials')
    if path.stat().st_mode&0o077:raise ValueError('credentials permission must be 0600')
    return data


def compose(*args):
    env=os.environ.copy();env.update(settings())
    return execute(['docker','compose','--env-file',str(ROOT/LAB/'.env'),'-f',str(ROOT/'compose.coaching-retrieval.yml'),*args],env=env)


def sql(query):
    cid=compose('ps','-q','postgres').stdout.decode().strip()
    if not cid:raise ValueError('dedicated database is not running')
    info=json.loads(execute(['docker','inspect',cid]).stdout)[0]
    if info['Config']['Labels'].get('com.docker.compose.project')!='ccc-coaching-retrieval':raise ValueError('foreign container')
    return execute(['docker','exec','-i',cid,'psql','-X','-At','-U',DB,'-d',DB,'-v','ON_ERROR_STOP=1'],input=query.encode()).stdout.decode().strip()


def http(path):
    with urlopen('http://127.0.0.1:11437'+path,timeout=15) as response:return json.load(response)


def up():
    state=ROOT/LAB;state.mkdir(parents=True,exist_ok=True)
    inputs(ROOT)
    env=state/'.env'
    inspected=subprocess.run(['docker','volume','inspect',VOLUME],capture_output=True)
    new_volume=inspected.returncode!=0 and b'no such volume' in inspected.stderr.lower()
    if inspected.returncode and not new_volume:raise ValueError('cannot verify dedicated volume')
    if not new_volume and (json.loads(inspected.stdout)[0].get('Labels') or {}).get('com.docker.compose.project')!='ccc-coaching-retrieval':
        raise ValueError('foreign volume')
    if not env.exists():
        if not new_volume:raise ValueError('existing volume: credentials recovery required')
        with os.fdopen(os.open(env,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600),'w') as file:
            file.write('POSTGRES_PASSWORD='+secrets.token_urlsafe(32)+'\nLAB_TOKEN='+secrets.token_hex(24)+'\n')
    if not compose('ps','-q','postgres').stdout.strip():
        with socket.socket() as sock:sock.bind(('127.0.0.1',15434))
    compose('up','-d','--wait','--wait-timeout','60','--pull','never','postgres')
    schemas=sql("select nspname from pg_namespace where nspname !~ '^pg_' and nspname<>'information_schema' order by 1").splitlines()
    tables=sql("select c.relname from pg_class c join pg_namespace n on n.oid=c.relnamespace where n.nspname='public' and c.relkind in ('r','p','v','m','S','f') order by 1").splitlines()
    if 'lab_identity' not in tables:
        can_initialize(new_volume,tables,schemas)
        generation=sha(state/'prepared.json')
        sql("BEGIN; CREATE TABLE lab_identity(token text primary key,corpus_sha256 text not null,load_state text not null); INSERT INTO lab_identity VALUES ('"+settings()['LAB_TOKEN']+"','"+generation+"','EMPTY'); COMMIT;")
        tables=['lab_identity']
    database_mode(sql('select token from lab_identity'),settings()['LAB_TOKEN'],tables,schemas)
    if sql('select corpus_sha256 from lab_identity')!=sha(state/'prepared.json'):raise ValueError('database generation differs; do not reuse')
    try:http('/api/tags')
    except OSError:
        bundle=ROOT/'.local/rag-api/native-app/Ollama.app';binary=bundle/'Contents/Resources/ollama'
        execute(['codesign','--verify','--deep','--strict',str(bundle)])
        execute(['spctl','--assess','--type','execute',str(bundle)])
        with socket.socket() as sock:sock.bind(('127.0.0.1',11437))
        procenv=os.environ.copy();procenv.update({'OLLAMA_HOST':'127.0.0.1:11437','OLLAMA_MODELS':str(ROOT/'.local/rag-api/native-models'),
          'OLLAMA_NO_CLOUD':'1','OLLAMA_NOPRUNE':'1','OLLAMA_NUM_PARALLEL':'1','OLLAMA_MAX_LOADED_MODELS':'1'})
        with (state/'native.log').open('ab') as log:process=subprocess.Popen([str(binary),'serve'],env=procenv,stdout=log,stderr=log,start_new_session=True)
        (state/'native.pid').write_text(str(process.pid))
        for _ in range(60):
            try:http('/api/tags');break
            except OSError:time.sleep(.5)
    tags=http('/api/tags')['models']
    loaded=http('/api/ps')['models']
    validate_model(tags,loaded)
    (state/'runtime.json').write_text(json.dumps({'embeddingDigest':EMBED_DIGEST,'loadedBefore':loaded,'newModelDownloads':0},indent=2)+'\n')
    print('전용 PG15434와 기존 bge-m3 준비 완료. API/Redis/생성모델 기동 없음.')


def run(name, split='regression'):
    validate_model(http('/api/tags')['models'],http('/api/ps')['models'])
    before=inputs(ROOT);path=new_run(ROOT,name)
    actual_split='evaluation' if split=='regression' else split
    planned=[c['id'] for c in json.loads((ROOT/CASES).read_text())['cases'] if c['split']==actual_split]
    (path/'started.json').write_text(json.dumps({'inputHashes':before,'plannedCaseIds':planned,'role':split},indent=2)+'\n')
    env=os.environ.copy();env.update(settings());env['COACHING_RETRIEVAL_RUN']=name;env['COACHING_RETRIEVAL_SPLIT']=split
    try:
        with (path/'gradle.log').open('wb') as log:
            subprocess.run(['./gradlew',':api:coachingRetrievalEvaluate','--offline','--console=plain','--no-daemon','--max-workers=1'],
                           cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT,check=True)
        if before!=inputs(ROOT):raise ValueError('input changed during run')
        data=json.loads((path/'raw.json').read_text())
        if data['summary']['cases']!=len(planned):raise ValueError('incomplete execution count')
        data.update(complete=True,inputHashes=before,embeddingDigest=EMBED_DIGEST)
        (path/'result.json').write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
        (path/'identity.json').write_text(json.dumps({'resultSha256':sha(path/'result.json')})+'\n')
    except Exception as error:
        progress=path/'progress.jsonl'
        completed=[json.loads(line)['caseId'] for line in progress.read_text().splitlines()] if progress.exists() else []
        (path/'failure.json').write_text(json.dumps({'complete':False,'errorType':type(error).__name__,
             'plannedCaseIds':planned,'completedCaseIds':completed,'unexecutedCaseIds':[x for x in planned if x not in completed]})+'\n');raise
    print(json.dumps(read_summary(ROOT,name),ensure_ascii=False,indent=2))


def stop():
    compose('stop','postgres')
    path=ROOT/LAB/'native.pid'
    if path.exists():
        pid=int(path.read_text());binary=str(ROOT/'.local/rag-api/native-app/Ollama.app/Contents/Resources/ollama')
        command=subprocess.run(['ps','-p',str(pid),'-o','command='],capture_output=True,text=True).stdout.strip()
        if command:
            if command!=binary+' serve':raise ValueError('PID belongs to another process')
            os.kill(pid,signal.SIGTERM)
        path.unlink()
    print('이번 전용 서비스 중지. DB·모델 볼륨/파일 보존.')


def main():
    parser=argparse.ArgumentParser(description=__doc__);sub=parser.add_subparsers(dest='command',required=True)
    for name in ['check-sources','prepare','up','stop']:sub.add_parser(name)
    for name in ['run','summary']:
        command=sub.add_parser(name);command.add_argument('--run',required=True)
        if name=='run':command.add_argument('--split',choices=['dev','regression'],default='regression')
    args=parser.parse_args()
    if args.command in ['check-sources','prepare']:
        groups=build_corpus(ROOT,json.loads((ROOT/MANIFEST).read_text()),json.loads((ROOT/SPEC).read_text()))
        if args.command=='prepare':(ROOT/LAB/'prepared.json').write_text(json.dumps(groups,ensure_ascii=False,indent=2)+'\n')
        print(json.dumps({k:len(v) for k,v in groups.items()}))
    elif args.command=='summary':print(json.dumps(read_summary(ROOT,args.run),ensure_ascii=False,indent=2))
    elif args.command=='run':run(args.run,args.split)
    elif args.command=='up':up()
    else:stop()


if __name__=='__main__':
    try:main()
    except (ValueError,OSError,KeyError,subprocess.CalledProcessError) as error:
        # 외부 명령의 stderr에는 연결 문자열이 있을 수 있으므로 그대로 출력하지 않는다.
        raise SystemExit('실행 중단: '+(str(error) if isinstance(error,ValueError) else type(error).__name__))
