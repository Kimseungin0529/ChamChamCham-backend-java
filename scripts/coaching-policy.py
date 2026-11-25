#!/usr/bin/env python3
"""Docker/API를 시작하지 않는 합성 문맥 코칭 정책 비교. 의미 판정은 명시적 자체 검토로만 집계한다."""
import argparse
import copy
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request
sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parents[1]
STATE = ROOT / '.local/rag-api'
POLICY = STATE / 'policy'
CASES = ROOT / 'api/src/test/resources/verification/coaching-policy-cases.json'
PROMPT = ROOT / 'api/src/test/resources/verification/coaching-policy-candidate.txt'
URL = 'http://127.0.0.1:11437'
MODEL = 'qwen3:4b-instruct'
DIGEST = '0edcdef34593eac1aa2be9c7d06c432dcf81945adca5eca2f27662c18f168ba0'
FLAGS = {'INVENTED_ACTION','CONDITION_MISMATCH','UNVERIFIED_PRODUCT','UNIT_CONFUSION',
         'SOURCE_CONTRADICTION','UNSUPPORTED_ADVICE','APPROPRIATE_LIMIT','UNNECESSARY_ABSTENTION'}


def digest(value):
    return hashlib.sha256(json.dumps(value, ensure_ascii=False, sort_keys=True).encode()).hexdigest()


def read(path):
    return json.loads(path.read_text())


def write(path, value):
    # 같은 폴더 안에서 교체해 중단 시에도 실험 JSON을 보존한다.
    path.parent.mkdir(parents=True, exist_ok=True)
    temp = path.with_suffix(path.suffix + '.tmp')
    temp.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')
    os.replace(temp, path)


def bind_run(folder, manifest):
    folder.mkdir(parents=True, exist_ok=True)
    identity = folder / 'manifest.json'
    if identity.exists():
        if read(identity) != manifest:
            raise RuntimeError('모델/사례/정책/도구 설정이 달라졌습니다. 새 --run 이름을 사용하세요')
    else:
        write(identity, manifest)


def validate_content(value, variant, case):
    if not isinstance(value, dict): return ['not_object']
    warnings = []
    required = {'goodPoint', 'nextActions'}
    if variant == 'candidate': required |= {'decision', 'questions', 'missingInformation'}
    if set(value) != required: warnings.append('fields')
    good = value.get('goodPoint')
    actions = value.get('nextActions')
    if not isinstance(actions, list): actions = []; warnings.append('actions_not_array')
    if not ((2 if variant == 'baseline' else 0) <= len(actions) <= 3): warnings.append('action_count')
    if good is None and variant == 'baseline': warnings.append('good_point_required')
    for item in ([good] if good is not None else []) + actions:
        if not isinstance(item, dict): warnings.append('item_not_object'); continue
        if not isinstance(item.get('basis'), str) or not item['basis'].strip(): warnings.append('basis_blank')
        text = item.get('text', '')
        if not isinstance(text, str): warnings.append('text_type'); text = ''
        if not 15 <= len(text) <= 60: warnings.append('text_length')
        if not text.rstrip().rstrip('.!?…。').endswith('요'): warnings.append('text_tone')
        if re.search('[A-Za-z]', text): warnings.append('text_english')
        refs = item.get('evidenceRefs', [])
        if not isinstance(refs, list) or not refs: warnings.append('evidence_refs_blank'); refs = []
        if any(not isinstance(r, str) or not r.strip() for r in refs): warnings.append('evidence_ref_blank')
        if any(r not in case['allowedCitationIds'] for r in refs): warnings.append('unknown_evidence')
    categories = {'WEATHER','PEST_DISEASE','IRRIGATION','FERTILIZING','PEST_CONTROL','HARVEST','CULTIVATION','GENERAL'}
    for action in actions:
        if not isinstance(action, dict): continue
        if action.get('category') not in categories: warnings.append('category')
        if action.get('due') not in {'TODAY','THIS_WEEK','NEXT_WEEK','NEXT_CHECK'}: warnings.append('due')
        refs = action.get('evidenceRefs', [])
        if not isinstance(refs, list): continue
        if action.get('category') == 'WEATHER' and not any(isinstance(r,str) and r.startswith('weather:') for r in refs): warnings.append('weather_without_weather')
        if action.get('category') == 'PEST_DISEASE' and not any(r in case.get('documentCitationIds',[]) for r in refs): warnings.append('pest_without_document')
    if variant == 'candidate':
        decision = value.get('decision')
        if decision not in {'ANSWER','CLARIFY','ABSTAIN'}: warnings.append('decision')
        questions = value.get('questions')
        missing = value.get('missingInformation')
        for field in ('questions','missingInformation'):
            values = value.get(field)
            if not isinstance(values,list) or any(not isinstance(x,str) or not x.strip() for x in values): warnings.append(field + '_invalid')
        if decision == 'CLARIFY' and not questions: warnings.append('clarify_without_question')
        if decision in {'CLARIFY','ABSTAIN'} and not missing: warnings.append('missing_information_required')
        if decision == 'ABSTAIN' and (good is not None or actions): warnings.append('abstention_with_advice')
        if decision == 'ANSWER' and not actions: warnings.append('answer_without_action')
    return sorted(set(warnings))


def make_request(case, variant, config, candidate):
    return {'model': config['model'], 'stream': False, 'think': False,
            'messages': [{'role':'system','content':case['prompt']['system'] if variant == 'baseline' else candidate},
                         {'role':'user','content':case['prompt']['user']}],
            'format': config.get('baselineSchema' if variant == 'baseline' else 'candidateSchema', {}),
            'options': {'temperature':0,'seed':42,'num_ctx':8192,'num_predict':1024}, 'keep_alive':'0s'}


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise RuntimeError('루프백 모델 요청의 리다이렉트는 허용하지 않습니다')


def http(endpoint, body=None, timeout=240):
    if endpoint not in ('/api/tags','/api/version','/api/chat'): raise ValueError('허용하지 않은 모델 경로')
    request = urllib.request.Request(URL + endpoint, data=None if body is None else json.dumps(body).encode(),
                                     headers={'Content-Type':'application/json'})
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect())
    with opener.open(request, timeout=timeout) as response:
        return json.loads(response.read(4 * 1024 * 1024))


def execute(case, variant, folder, config, transport=None, candidate='', retry_incomplete=False):
    path = folder / (case['id'] + '-' + variant + '.json')
    attempt = 1
    if path.exists():
        previous = read(path)
        if previous['terminal']: return previous
        if not retry_incomplete: raise RuntimeError('완료 여부 불명 요청이 있습니다. --retry-incomplete로 명시적으로 재실행하세요')
        attempt = previous['attempt'] + 1
        archive = path.with_name(path.stem + '-interrupted-' + str(previous['attempt']) + '.json')
        if archive.exists(): raise RuntimeError('중단 기록이 이미 있습니다. 덮어쓰지 않습니다')
        path.rename(archive)
    body = make_request(case, variant, config, candidate)
    row = {'caseId':case['id'],'variant':variant,'attempt':attempt,'terminal':False,'requestSha256':digest(body),'request':body,'modelCalled':False}
    write(path, row)
    if variant == 'baseline' and case.get('documentCitationIds') == []:
        row.update(status='INSUFFICIENT_EVIDENCE',terminal=True,seconds=0,guard='existing-product-empty-document-gate')
        write(path,row)
        return row
    row['modelCalled'] = True
    started = time.monotonic()
    try:
        raw = (transport or (lambda b: http('/api/chat', b)))(body)
        row['raw'] = raw
        text = raw['message']['content']
        try: content = json.loads(text)
        except (json.JSONDecodeError, TypeError):
            row.update(status='FORMAT_INVALID', validation=['json_parse'])
        else:
            warnings = validate_content(content, variant, case)
            row.update(response=content, status='FORMAT_INVALID' if warnings else 'VALID', validation=warnings)
    except (urllib.error.URLError, TimeoutError, ConnectionError, OSError) as error:
        row.update(status='MODEL_ERROR', errorType=type(error).__name__)
        if isinstance(error, urllib.error.HTTPError): row['httpStatus'] = error.code
    except (KeyError, TypeError) as error:
        row.update(status='PROTOCOL_ERROR', errorType=type(error).__name__)
    row.update(terminal=True, seconds=round(time.monotonic()-started,3))
    write(path, row)
    return row


def check_review(row, review):
    if row['status'] != 'VALID': raise ValueError('형식 통과 답변만 의미 판정합니다')
    if review.get('responseSha256') != digest(row['response']): raise ValueError('판정 대상 응답 해시 불일치')
    if review.get('label') not in {'SUPPORTED','PARTIAL','UNSUPPORTED','UNCERTAIN'}: raise ValueError('판정 분류 필요')
    if not isinstance(review.get('reason'),str) or not review['reason'].strip(): raise ValueError('판정 이유 필요')
    if not isinstance(review.get('flags'),list) or set(review['flags']) - FLAGS: raise ValueError('판정 flag 오류')
    if not isinstance(review.get('evidence'),list) or not review['evidence']: raise ValueError('기록/문헌 인용 또는 자료 부재의 구체 근거 필요')


def summarize(rows, reviews):
    import statistics
    result = {}
    for variant in ('baseline','candidate'):
        chosen = [r for r in rows if r['variant'] == variant and r['terminal']]
        valid = [r for r in chosen if r['status'] == 'VALID']
        graded = []
        for row in valid:
            review = reviews.get(row['caseId'] + '-' + variant)
            if review:
                check_review(row, review); graded.append(review)
        times = [r['seconds'] for r in chosen if r.get('modelCalled',True)]
        result[variant] = {'executed':len(chosen),'formatValid':len(valid),'modelError':sum(r['status']=='MODEL_ERROR' for r in chosen),
                           'modelCalls':sum(r.get('modelCalled',True) and not r.get('reusedFrom') for r in chosen),'reusedResponses':sum(bool(r.get('reusedFrom')) for r in chosen),'insufficientEvidence':sum(r['status']=='INSUFFICIENT_EVIDENCE' for r in chosen),'formatInvalid':sum(r['status']=='FORMAT_INVALID' for r in chosen),'protocolError':sum(r['status']=='PROTOCOL_ERROR' for r in chosen),
                           'reviewed':len(graded),'semanticPending':len(valid)-len(graded),
                           'supported':sum(r['label']=='SUPPORTED' for r in graded),
                           'partial':sum(r['label']=='PARTIAL' for r in graded),'unsupported':sum(r['label']=='UNSUPPORTED' for r in graded),
                           'uncertain':sum(r['label']=='UNCERTAIN' for r in graded),
                           'flags':{flag:sum(flag in r['flags'] for r in graded) for flag in sorted(FLAGS)},
                           'medianSeconds':statistics.median(times) if times else None}
    return result


def candidate_schema(baseline):
    result = copy.deepcopy(baseline)
    result['properties']['goodPoint'] = {'anyOf':[result['properties']['goodPoint'],{'type':'null'}]}
    result['properties']['nextActions']['minItems'] = 0
    result['properties']['decision'] = {'type':'string','enum':['ANSWER','CLARIFY','ABSTAIN']}
    for field in ('questions','missingInformation'):
        result['properties'][field] = {'type':'array','items':{'type':'string','pattern':'^[가-힣0-9 .,!?~℃㎝%·:()（）/-]{1,120}$'},'maxItems':3}
    result['required'] = list(result['properties'])
    return result


def start_model():
    binary = STATE/'native-app/Ollama.app/Contents/Resources/ollama'
    bundle = STATE/'native-app/Ollama.app'
    for args in (['codesign','--verify','--deep','--strict',str(bundle)],['spctl','--assess','--type','execute',str(bundle)]):
        subprocess.run(args, check=True, capture_output=True)
    if not (STATE/'native-copy-verification.json').exists(): raise RuntimeError('보존 모델 검증 기록 없음')
    try:
        tags = http('/api/tags',timeout=3)
    except urllib.error.URLError:
        with socket.socket() as sock: sock.bind(('127.0.0.1',11437))
        env = os.environ.copy()
        env.update(OLLAMA_HOST='127.0.0.1:11437',OLLAMA_MODELS=str(STATE/'native-models'),OLLAMA_NO_CLOUD='1',
                   OLLAMA_NOPRUNE='1',OLLAMA_NUM_PARALLEL='1',OLLAMA_MAX_LOADED_MODELS='1')
        with (POLICY/'native.log').open('ab') as log:
            process = subprocess.Popen([str(binary),'serve'],env=env,stdout=log,stderr=log,start_new_session=True)
        (STATE/'native.pid').write_text(str(process.pid))
        write(POLICY/'model-start.json',{'pid':process.pid,'binary':str(binary),'dockerStarted':False})
        for _ in range(30):
            if process.poll() is not None: raise RuntimeError('모델 시작 실패. policy/native.log 확인')
            try: tags = http('/api/tags',timeout=2);break
            except urllib.error.URLError: time.sleep(.5)
        else: raise RuntimeError('모델 시작 대기 만료')
    if not any(m['name']==MODEL and m['digest']==DIGEST for m in tags['models']): raise RuntimeError('모델 식별값 불일치. 다운로드하지 않습니다')
    print('macOS Ollama 준비: Docker/API 시작 없음, 기존 모델만 사용')


def verify_sources(files, root=ROOT):
    for name, expected in files.items():
        if hashlib.sha256((root/name).read_bytes()).hexdigest() != expected:
            raise RuntimeError('내보낸 입력과 현재 소스가 다릅니다. 다시 export하세요: ' + name)


def require_development(folder, candidate_sha, expected=None, cases=None, candidate=None):
    manifest = read(folder/'manifest.json')
    if manifest.get('split') != 'dev' or manifest.get('candidateSha256') != candidate_sha:
        raise RuntimeError('같은 후보 정책의 개발 검토부터 완료하세요')
    for key, value in (expected or {}).items():
        if manifest.get(key) != value:
            raise RuntimeError('현재 실행 조건과 개발 검토의 식별값이 다릅니다: ' + key)
    if cases is None or len(cases) != 24 or len({c['id'] for c in cases}) != 24:
        raise RuntimeError('정확한 개발 24개 사례 정의가 필요합니다')
    expected_files = {c['id']+'-'+v+'.json' for c in cases for v in ('baseline','candidate')}
    found = {p.name for p in folder.glob('*.json') if re.fullmatch(r'.+-(baseline|candidate)\.json',p.name)}
    if found != expected_files: raise RuntimeError('개발 실행의 사례 ID 집합이 현재 정의와 다릅니다')
    rows = []
    for case in cases:
        for variant in ('baseline','candidate'):
            row = read(folder/(case['id']+'-'+variant+'.json'))
            if row.get('caseId') != case['id'] or row.get('variant') != variant or not row.get('terminal'):
                raise RuntimeError('개발 결과 식별값/완료 상태 불일치')
            body = make_request(case,variant,manifest['config'],candidate if candidate is not None else PROMPT.read_text())
            if row.get('requestSha256') != digest(body) or row.get('request') != body:
                raise RuntimeError('개발 결과가 현재 기록/정책 요청과 다릅니다')
            rows.append(row)
    reviews = read(folder/'reviews.json') if (folder/'reviews.json').exists() else {}
    summary = summarize(rows, reviews)
    if any(v['executed'] != 24 or not v['formatValid'] or v['semanticPending'] for v in summary.values()):
        raise RuntimeError('개발 24건의 양쪽 실행 및 형식 통과 답변의 자체 검토를 먼저 완료하세요')
    return digest({'manifest':manifest,'reviews':reviews,'results':rows})


def load_run(run, split, candidate_path=PROMPT, development_run=None):
    if not re.fullmatch('[a-zA-Z0-9_-]+',run): raise ValueError('안전한 --run 이름 필요')
    prepared = read(POLICY/'prepared.json')
    verify_sources(prepared['sourceFiles'])
    if prepared['casesSha256'] != hashlib.sha256(CASES.read_bytes()).hexdigest(): raise RuntimeError('사례 변경: prepare/export부터 다시 실행')
    tags = http('/api/tags',timeout=5)
    if not any(m['name']==MODEL and m['digest']==DIGEST for m in tags['models']): raise RuntimeError('모델 식별값 불일치')
    config = {'model':MODEL, 'baselineSchema':prepared['baselineSchema'],'candidateSchema':candidate_schema(prepared['baselineSchema'])}
    candidate = candidate_path.read_text()
    development = None
    manifest = {'model':MODEL,'modelDigest':DIGEST,'serverVersion':http('/api/version',timeout=5)['version'],
                'split':split,'developmentReviewSha256':development,'casesSha256':hashlib.sha256(CASES.read_bytes()).hexdigest(),
                'preparedSha256':digest(prepared),'candidateSha256':hashlib.sha256(candidate.encode()).hexdigest(),
                'scriptSha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                'config':config,'sourceFiles':prepared['sourceFiles'],'measurement':'generation-only; fixed curated/captured evidence; synthetic context'}
    if split == 'evaluation':
        if not development_run or not re.fullmatch('[a-zA-Z0-9_-]+',development_run): raise ValueError('--development-run 필요')
        expected = {k:v for k,v in manifest.items() if k not in ('split','developmentReviewSha256')}
        development_cases = [c for c in prepared['cases'] if c['split']=='dev']
        manifest['developmentReviewSha256'] = require_development(POLICY/'runs'/development_run,manifest['candidateSha256'],expected,development_cases,candidate)
    folder = POLICY/'runs'/run
    bind_run(folder,manifest)
    return [c for c in prepared['cases'] if c['split']==split],config,candidate,folder


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command',required=True)
    commands.add_parser('start-model')
    execute_parser = commands.add_parser('run')
    execute_parser.add_argument('--run',required=True)
    execute_parser.add_argument('--split',choices=['dev','evaluation'],required=True)
    execute_parser.add_argument('--limit',type=int)
    execute_parser.add_argument('--case')
    execute_parser.add_argument('--candidate',type=Path,default=PROMPT)
    execute_parser.add_argument('--retry-incomplete',action='store_true')
    execute_parser.add_argument('--development-run')
    summary_parser = commands.add_parser('summary'); summary_parser.add_argument('--run',required=True)
    args = parser.parse_args()
    POLICY.mkdir(parents=True,exist_ok=True)
    if args.command == 'start-model': start_model();return
    if args.command == 'summary':
        if not re.fullmatch('[a-zA-Z0-9_-]+',args.run): raise ValueError('실행명 오류')
        folder=POLICY/'runs'/args.run
        rows=[read(p) for p in folder.glob('*.json') if re.fullmatch(r'.+-(baseline|candidate)\.json',p.name)]
        reviews=read(folder/'reviews.json') if (folder/'reviews.json').exists() else {}
        result=summarize(rows,reviews)
        manifest=read(folder/'manifest.json')
        cases=[c for c in read(CASES)['cases'] if c['split']==manifest['split']]
        result['plannedCases']=len(cases)
        result['incomplete']={v:[c['id'] for c in cases if not any(r['caseId']==c['id'] and r['variant']==v and r['terminal'] for r in rows)] for v in ('baseline','candidate')}
        result['limits']=['동일 AI의 문헌 자체 검토. 전문가/현장 검증 아님.','실제 API/저장/검색 미실행. 고정 근거로 생성 정책만 비교.','모델 응답 VALID는 형식 판정이며 내용 정확도 아님.']
        write(folder/'summary.json',result);print(json.dumps(result,ensure_ascii=False,indent=2));return
    with (STATE/'command.lock').open('a') as lock:
        fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
        cases,config,candidate,folder=load_run(args.run,args.split,args.candidate,args.development_run)
        if args.case: cases=[c for c in cases if c['id']==args.case]
        if args.limit is not None:
            if args.limit < 1: raise ValueError('--limit은 양수')
            cases=cases[:args.limit]
        if not cases: raise ValueError('실행할 사례 없음')
        for index,case in enumerate(cases):
            for variant in (('baseline','candidate') if index % 2 == 0 else ('candidate','baseline')):
                row=execute(case,variant,folder,config,candidate=candidate,retry_incomplete=args.retry_incomplete)
                print(case['id'],variant,row['status'],row['seconds'],flush=True)

if __name__ == '__main__':
    try: main()
    except (RuntimeError,ValueError,BlockingIOError,urllib.error.URLError) as error:
        print(type(error).__name__ + ': ' + str(error),file=sys.stderr);sys.exit(1)
