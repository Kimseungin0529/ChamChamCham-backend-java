#!/usr/bin/env python3
"""합성 데이터 전용 API 실습. 파괴적 초기화/볼륨 삭제/외부 인증 호출을 제공하지 않는다."""
import argparse
import base64
from datetime import datetime
import fcntl
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
import urllib.error
import urllib.request
import uuid

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parents[1]
STATE = ROOT / '.local/rag-api'
DB = 'ccc_rag_api'
MODEL = 'qwen3:4b-instruct'
MODEL_DIGEST = '0edcdef34593eac1aa2be9c7d06c432dcf81945adca5eca2f27662c18f168ba0'
EMBED_DIGEST = '7907646426070047a77226ac3e684fbbe8410524f7b4a74d02837e43f2146bab'

def run(args, **kwargs):
    return subprocess.run(args, check=True, capture_output=True, **kwargs)

def credentials(path):
    if not path.exists():
        values = {'POSTGRES_PASSWORD': secrets.token_urlsafe(32), 'LAB_TOKEN': uuid.uuid4().hex,
                  'JWT_SECRET': base64.b64encode(secrets.token_bytes(64)).decode(),
                  'MEMBER_PASSWORD': secrets.token_urlsafe(24)}
        with os.fdopen(os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), 'w') as file:
            file.write(''.join(key + '=' + value + '\n' for key, value in values.items()))
    path.chmod(0o600)
    values = dict(line.split('=', 1) for line in path.read_text().splitlines())
    if set(values) != {'POSTGRES_PASSWORD', 'LAB_TOKEN', 'JWT_SECRET', 'MEMBER_PASSWORD'}:
        raise RuntimeError('실습 자격 증명 파일이 올바르지 않습니다')
    if not all(values.values()) or not values['LAB_TOKEN'].isalnum():
        raise RuntimeError('실습 식별값이 올바르지 않습니다')
    return values

def settings():
    if not (STATE / '.env').exists():
        raise RuntimeError('먼저 up을 실행하세요. 기존 DB가 있다면 자격 증명 파일을 복구하세요')
    return credentials(STATE / '.env')

def environment():
    env = os.environ.copy()
    env.update(settings())
    env.update({'RAG_API_ROOT': str(STATE), 'SPRING_PROFILES_ACTIVE': 'test,rag-api-lab'})
    backend = STATE / 'backend.json'
    if backend.exists() and json.loads(backend.read_text())['backend'] == 'native':
        env['RAG_API_OLLAMA_URL'] = 'http://127.0.0.1:11437'
    else:
        env['RAG_API_OLLAMA_URL'] = 'http://127.0.0.1:11436'
    env['RAG_API_CHAT_MODEL'] = json.loads(backend.read_text()).get('chatModel', MODEL) if backend.exists() else MODEL
    return env

def compose(*args):
    return run(['docker', 'compose', '--project-directory', str(ROOT), '--env-file', str(STATE / '.env'),
                '-f', str(ROOT / 'compose.rag-api.yml'), *args], env=environment())

def container(service):
    value = compose('ps', '-q', service).stdout.decode().strip()
    if not value:
        raise RuntimeError('먼저 up으로 전용 서비스를 시작하세요')
    info = json.loads(run(['docker', 'inspect', value]).stdout)[0]
    if (info['Config'].get('Labels') or {}).get('com.docker.compose.project') != 'ccc-rag-api':
        raise RuntimeError('다른 컨테이너입니다')
    return value

def sql(query, database=DB):
    return run(['docker', 'exec', '-i', container('postgres'), 'psql', '-X', '-At', '-U', DB,
                '-d', database, '-v', 'ON_ERROR_STOP=1'], input=query.encode()).stdout.decode().strip()

def database_mode(identity, token, schemas, tables, initialize):
    if schemas != ['public'] or not identity.startswith(token + ':'):
        raise RuntimeError('전용 DB 식별 정보/스키마 불일치')
    state = identity.split(':', 1)[1]
    if state == 'INITIALIZING':
        raise RuntimeError('INITIALIZING 상태: 실패 원인을 조사하세요. 자동 재생성하지 않습니다')
    if initialize:
        if state != 'EMPTY' or tables != ['lab_identity']:
            raise RuntimeError('빈 DB만 최초 초기화할 수 있습니다')
        return 'create-only'
    if state != 'READY' or 'member' not in tables:
        raise RuntimeError('아직 초기화되지 않은 DB입니다')
    return 'validate'

def guard(initialize=False):
    schemas = sql("select nspname from pg_namespace where nspname !~ '^pg_' and nspname <> 'information_schema' order by nspname").splitlines()
    tables = sql("select c.relname from pg_class c join pg_namespace n on n.oid=c.relnamespace where n.nspname='public' "
                 "and c.relkind in ('r','p','v','m','S','f') order by c.relname").splitlines()
    return database_mode(sql("select token || ':' || state from lab_identity"), settings()['LAB_TOKEN'], schemas, tables, initialize)

def save(name, data):
    folder = STATE / 'results'
    folder.mkdir(parents=True, exist_ok=True)
    path = folder / (name + '.json')
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n')
    return path

def up():
    STATE.mkdir(parents=True, exist_ok=True, mode=0o700)
    if not (STATE / '.env').exists():
        run(['docker', 'info', '--format', '{{.ServerVersion}}'])
        found = subprocess.run(['docker', 'volume', 'inspect', 'ccc-rag-api-postgres'], capture_output=True)
        if found.returncode == 0 or b'no such volume' not in found.stderr.lower():
            raise RuntimeError('기존 DB/자격 증명 확인 필요. 새 비밀번호를 생성하지 않습니다')
    credentials(STATE / '.env')
    for name in ('ccc-rag-api-postgres', 'ccc-rag-api-redis'):
        result = subprocess.run(['docker', 'volume', 'inspect', name], capture_output=True)
        if result.returncode == 0 and (json.loads(result.stdout)[0].get('Labels') or {}).get('com.docker.compose.project') != 'ccc-rag-api':
            raise RuntimeError('다른 작업의 볼륨입니다')
    for ident in run(['docker', 'ps', '-q']).stdout.decode().split():
        item = json.loads(run(['docker', 'inspect', ident]).stdout)[0]
        if any(m.get('Name') == 'ccc-rag-bge-m3-cache' for m in item['Mounts']) and (
                (item['Config'].get('Labels') or {}).get('com.docker.compose.project') != 'ccc-rag-api'):
            raise RuntimeError('공유 모델 캐시를 다른 Ollama가 사용 중입니다. 기존 실행을 건드리지 않습니다')
    for service, port in (('postgres', 15433), ('ollama', 11436), ('redis', 16379)):
        if not compose('ps', '-q', service).stdout.strip():
            with socket.socket() as sock:
                sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                sock.bind(('127.0.0.1', port))
    compose('up', '-d', '--wait', '--wait-timeout', '120', '--pull', 'never')
    schemas = sql("select nspname from pg_namespace where nspname !~ '^pg_' and nspname <> 'information_schema'").splitlines()
    if schemas != ['public']:
        raise RuntimeError('예상하지 않은 DB 스키마')
    if sql("select count(*) from pg_class c join pg_namespace n on n.oid=c.relnamespace where n.nspname='public' and c.relkind in ('r','p','v','m','S','f')") == '0':
        sql("BEGIN; CREATE TABLE lab_identity(token text primary key, state text not null); "
            f"INSERT INTO lab_identity VALUES ('{settings()['LAB_TOKEN']}','EMPTY'); COMMIT;")
    identity = sql("select token || ':' || state from lab_identity")
    if not identity.startswith(settings()['LAB_TOKEN'] + ':'):
        raise RuntimeError('전용 DB 식별 정보 불일치')
    tags = request('http://127.0.0.1:11436/api/tags')['models']
    if not any(m['name'] == 'bge-m3:latest' and m['digest'] == EMBED_DIGEST for m in tags):
        raise RuntimeError('기존 bge-m3 식별값 불일치')
    images = {service: json.loads(run(['docker', 'inspect', container(service)]).stdout)[0]['Image'] for service in ('postgres','ollama','redis')}
    save('environment', {'images': images, 'modelVolume': 'ccc-rag-bge-m3-cache', 'dbVolume': 'ccc-rag-api-postgres',
                         'redisVolume': 'ccc-rag-api-redis', 'api': '127.0.0.1:18080', 'syntheticWeather': True})
    print('전용 PostgreSQL/Ollama/Redis 준비 완료. 기존 검색 DB 보존.')

def request(url, body=None, token=None, method=None, timeout=30):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(url, data=None if body is None else json.dumps(body).encode(), headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=timeout) as response:
        return json.load(response)

def pull_model():
    # 태그 변경으로 다른 가중치를 받지 않도록 공식 레지스트리의 manifest를 먼저 고정한다.
    url = 'https://registry.ollama.ai/v2/library/qwen3/manifests/4b-instruct'
    with urllib.request.urlopen(urllib.request.Request(url, headers={'Accept': 'application/vnd.docker.distribution.manifest.v2+json'}), timeout=30) as response:
        raw = response.read()
    if hashlib.sha256(raw).hexdigest() != MODEL_DIGEST:
        raise RuntimeError('공식 모델 manifest가 승인 시점과 다릅니다')
    manifest = json.loads(raw)
    size = sum(layer['size'] for layer in manifest['layers'])
    if size > 10_000_000_000:
        raise RuntimeError('다운로드 한도 초과')
    started = time.monotonic()
    # 컨테이너가 레지스트리에서 다운로드한다. 진행 로그의 토큰/환경 변수는 출력하지 않는다.
    result = run(['docker', 'exec', container('ollama'), 'ollama', 'pull', MODEL])
    found = next(m for m in request('http://127.0.0.1:11436/api/tags')['models'] if m['name'] == MODEL)
    if found['digest'] != MODEL_DIGEST:
        raise RuntimeError('다운로드된 모델 식별값 불일치')
    save('model', {'name': MODEL, 'digest': MODEL_DIGEST, 'manifestLayerBytes': size,
                   'size': found['size'], 'seconds': round(time.monotonic()-started, 2), 'source': url,
                   'details': found['details'], 'newDownloadBudgetBytes': 10_000_000_000})
    print('공식 모델 다운로드·manifest 확인 완료:', MODEL)

def java_env(initialize=False):
    guard(initialize)
    env = environment()
    env['RAG_API_INITIALIZE'] = str(initialize).lower()
    return env

def start(initialize=False, recover=False):
    if recover:
        if sql("select token || ':' || state from lab_identity") != settings()['LAB_TOKEN'] + ':INITIALIZING':
            raise RuntimeError('최초 부팅이 중단된 전용 DB만 복구 검증할 수 있습니다')
        env = environment()
        env.update({'RAG_API_INITIALIZE': 'false', 'RAG_API_RECOVER': 'true'})
    else:
        env = java_env(initialize)
    pidpath = STATE / 'api.pid'
    if pidpath.exists():
        pid = int(pidpath.read_text())
        try:
            os.kill(pid, 0)
        except ProcessLookupError:
            pass
        else:
            raise RuntimeError('기존 API 실행 프로세스가 있습니다. 중복 실행하지 않습니다')
    with socket.socket() as sock:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind(('127.0.0.1', 18080))
    (STATE / 'ready.json').unlink(missing_ok=True)
    with (STATE / 'api.log').open('ab') as log:
        process = subprocess.Popen(['./gradlew', ':api:ragApiLabRun', '--offline', '--console=plain', '--no-daemon'],
                                   cwd=ROOT, env=env, stdout=log, stderr=log, start_new_session=True)
    pidpath.write_text(str(process.pid))
    for _ in range(120):
        if process.poll() is not None:
            raise RuntimeError('API 시작 실패. .local/rag-api/api.log를 확인하세요')
        if (STATE / 'ready.json').exists():
            print('API 준비 완료: http://127.0.0.1:18080 / 합성 회원·농장 유지');return
        time.sleep(.5)
    raise RuntimeError('API 준비 대기 만료. 자동 재실행하지 않습니다. api.log 확인 필요')

def api_is_listening():
    with socket.socket() as sock:
        sock.settimeout(.2)
        return sock.connect_ex(('127.0.0.1',18080)) == 0

def stop_api():
    path = STATE / 'api.pid'
    if not path.exists():
        if api_is_listening():raise RuntimeError('API 포트가 사용 중이나 실행 식별 정보가 없습니다. 종료를 확인하세요')
        return
    pid = int(path.read_text())
    result = subprocess.run(['ps','-p',str(pid),'-o','command='],capture_output=True,text=True)
    wrapper_alive = bool(result.stdout.strip())
    if wrapper_alive and (str(ROOT/'gradle/wrapper/gradle-wrapper.jar') not in result.stdout or ':api:ragApiLabRun' not in result.stdout):
        raise RuntimeError('PID가 다른 프로세스입니다. 종료하지 않습니다')
    # wrapper가 먼저 사라졌어도 이 저장소의 실습 Java 앱과 웹 포트까지 확인한다.
    listing=run(['ps','-axo','pid=,command=']).stdout.decode().splitlines()
    apps=[int(line.split(None,1)[0]) for line in listing
          if 'com.chamchamcham.lab.RagApiLabApplication' in line and str(ROOT) in line]
    for app in apps:
        try:os.kill(app,signal.SIGTERM)
        except ProcessLookupError:pass
    if wrapper_alive:
        try:os.killpg(pid,signal.SIGTERM)
        except ProcessLookupError:pass
    for _ in range(40):
        live=subprocess.run(['ps','-p',','.join(map(str,[pid,*apps])),'-o','pid='],capture_output=True,text=True)
        if not live.stdout.strip() and not api_is_listening():break
        time.sleep(.25)
    else:
        raise RuntimeError('API 프로세스/포트 종료 대기 중. 실행 정보와 Docker를 유지합니다')
    path.unlink()
    (STATE/'ready.json').unlink(missing_ok=True)

def login(other=False):
    email = ('other' if other else 'learner') + '@example.invalid'
    return request('http://127.0.0.1:18080/api/v1/auth/login', {'email': email, 'password': settings()['MEMBER_PASSWORD']})['data']['accessToken']

def native_up():
    binary = STATE / 'native-app/Ollama.app/Contents/Resources/ollama'
    bundle = STATE / 'native-app/Ollama.app'
    if not (STATE / 'native-copy-verification.json').exists():
        raise RuntimeError('원본 모델 복사와 해시 검증을 먼저 완료하세요')
    run(['codesign', '--verify', '--deep', '--strict', str(bundle)])
    run(['spctl', '--assess', '--type', 'execute', str(bundle)])
    pidpath = STATE / 'native.pid'
    if pidpath.exists():
        pid = int(pidpath.read_text())
        result = subprocess.run(['ps','-p',str(pid),'-o','command='],capture_output=True,text=True)
        if result.stdout.strip():
            if str(binary) not in result.stdout: raise RuntimeError('기존 PID가 다른 프로세스입니다')
            print('기존 macOS Ollama 유지'); return
    with socket.socket() as sock:
        sock.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1)
        sock.bind(('127.0.0.1',11437))
    # CPU 실습 서버만 중지한다. 원본 모델 볼륨과 다른 컨테이너는 유지한다.
    compose('stop','ollama')
    env = os.environ.copy()
    env.update({'OLLAMA_HOST':'127.0.0.1:11437','OLLAMA_MODELS':str(STATE/'native-models'),
                'OLLAMA_NO_CLOUD':'1','OLLAMA_NOPRUNE':'1','OLLAMA_NUM_PARALLEL':'1','OLLAMA_MAX_LOADED_MODELS':'1'})
    with (STATE/'native.log').open('ab') as log:
        process = subprocess.Popen([str(binary),'serve'],env=env,stdout=log,stderr=log,start_new_session=True)
    pidpath.write_text(str(process.pid))
    for _ in range(60):
        try:
            tags = request('http://127.0.0.1:11437/api/tags')['models']
            byname = {m['name']:m['digest'] for m in tags}
            if byname.get(MODEL) != MODEL_DIGEST or byname.get('bge-m3:latest') != EMBED_DIGEST:
                raise RuntimeError('macOS 모델 식별값 불일치')
            prior=STATE/'backend.json'
            chosen=json.loads(prior.read_text()).get('chatModel',MODEL) if prior.exists() else MODEL
            if chosen not in (MODEL,'qwen2.5:7b-instruct-q4_K_M'):raise RuntimeError('승인되지 않은 모델 설정')
            prior.write_text(json.dumps({'backend':'native','url':'http://127.0.0.1:11437','version':'0.40.16','chatModel':chosen}))
            print('공증 확인된 macOS Ollama 시작. 기존 CPU 모델 볼륨 보존.'); return
        except (urllib.error.URLError,ConnectionError): time.sleep(.5)
    raise RuntimeError('macOS Ollama 시작 실패. native.log 확인 필요')

def native_stop():
    path=STATE/'native.pid'
    if not path.exists():return
    pid=int(path.read_text())
    result=subprocess.run(['ps','-p',str(pid),'-o','command='],capture_output=True,text=True)
    if result.stdout.strip():
        if str(STATE/'native-app/Ollama.app/Contents/Resources/ollama') not in result.stdout:
            raise RuntimeError('다른 프로세스는 종료하지 않습니다')
        os.kill(pid,signal.SIGTERM)
    path.unlink()

def select_model(name):
    if (STATE/'api.pid').exists():
        raise RuntimeError('먼저 stop-api를 실행하세요. 실행 중인 API의 모델을 몰래 바꾸지 않습니다')
    backend=json.loads((STATE/'backend.json').read_text())
    expected=MODEL_DIGEST if name==MODEL else '845dbda0ea48ed749caafd9e6037047aa19acfcfd82e704d7ca97d631a0b697e'
    models=request(backend['url']+'/api/tags')['models']
    if not any(m['name']==name and m['digest']==expected for m in models):
        raise RuntimeError('승인된 모델이 준비되지 않았습니다. 자동 다운로드하지 않습니다')
    backend['chatModel']=name
    (STATE/'backend.json').write_text(json.dumps(backend))
    print('다음 API 실행의 모델:',name)

def snapshot(database=DB):
    tables=sql("select tablename from pg_tables where schemaname='public' order by tablename",database).splitlines()
    result={}
    for name in tables:
        if not re.fullmatch('[a-z_][a-z0-9_]*',name):raise RuntimeError('예상하지 않은 테이블 이름')
        value=sql('select count(*) || \':\' || md5(coalesce(string_agg(row_to_json(t)::text, chr(10) order by row_to_json(t)::text),\'\')) from public."'+name+'" t',database)
        count,digest=value.split(':')
        result[name]={'rows':int(count),'contentHash':digest}
    return result

def backup():
    guard()
    folder=STATE/'backups';folder.mkdir(exist_ok=True)
    path=folder/(datetime.now().strftime('%Y%m%d-%H%M%S')+'-'+uuid.uuid4().hex[:8]+'.dump')
    data=run(['docker','exec',container('postgres'),'pg_dump','-Fc','-U',DB,'-d',DB]).stdout
    with os.fdopen(os.open(path,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600),'wb') as file:file.write(data)
    return path

def verify_restore():
    guard()
    if (STATE/'api.pid').exists() or api_is_listening():raise RuntimeError('일관된 비교를 위해 먼저 stop-api로 API를 중지하세요')
    if sql("select count(*) from record_feedback where status='PENDING'")!='0':raise RuntimeError('처리 중인 코칭이 있습니다')
    before=snapshot();path=backup();restored=DB+'_restore_'+uuid.uuid4().hex[:12]
    run(['docker','exec',container('postgres'),'createdb','-U',DB,'-T','template0',restored])
    try:
        run(['docker','exec','-i',container('postgres'),'pg_restore','-U',DB,'--no-owner','--exit-on-error','-d',restored],input=path.read_bytes())
        after=snapshot(restored)
        if before!=after or snapshot()!=before:raise RuntimeError('백업/복구 내용 불일치. 원본은 변경하지 않았습니다')
        save('restore-verification',{'backup':str(path.relative_to(ROOT)),'backupSha256':hashlib.sha256(path.read_bytes()).hexdigest(),
             'allTablesEqual':True,'tables':before,'temporaryDatabase':restored,'originalUnchanged':True})
    finally:
        # 이번 호출에서 방금 만든 임시 복구 DB만 제거한다. 원본/사용자 DB 삭제 명령은 없다.
        run(['docker','exec',container('postgres'),'dropdb','-U',DB,restored])
    print('별도 임시 DB 복구·모든 테이블 내용 일치·원본 보존 확인')

def api(action, path=None, record=None):
    token = login()
    base = 'http://127.0.0.1:18080/api/v1/farming-records'
    if action in ('submit', 'update'):
        payload = json.loads(Path(path).read_text())
        url = base + ('/' + str(uuid.UUID(record)) if action == 'update' else '')
        result = request(url, payload, token, 'PATCH' if action == 'update' else 'POST')
    elif action == 'regenerate':
        result = request(base + '/' + str(uuid.UUID(record)) + '/feedback/regenerate', {}, token)
    else:
        result = request(base + '/' + str(uuid.UUID(record)) + ('/feedback' if action == 'feedback' else ''), token=token)
    print(json.dumps(result, ensure_ascii=False, indent=2))

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['up','model','bootstrap','recover-bootstrap','start','stop-api','select-model','stop','down','status','native-up','ingest','submit','get','feedback','update','regenerate','backup','verify-restore'])
    parser.add_argument('--model',choices=[MODEL,'qwen2.5:7b-instruct-q4_K_M'],default=MODEL)
    parser.add_argument('--file')
    parser.add_argument('--record')
    args = parser.parse_args()
    STATE.mkdir(parents=True, exist_ok=True, mode=0o700)
    with (STATE / 'command.lock').open('w') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        if args.action == 'up': up()
        elif args.action == 'model': pull_model()
        elif args.action == 'native-up': native_up()
        elif args.action == 'stop-api': stop_api()
        elif args.action == 'select-model': select_model(args.model)
        elif args.action == 'verify-restore': verify_restore()
        elif args.action in ('bootstrap','recover-bootstrap','start'): start(args.action == 'bootstrap', args.action == 'recover-bootstrap')
        elif args.action in ('stop','down'):
            stop_api()
            native_stop()
            compose('stop' if args.action == 'stop' else 'down')
            print('컨테이너 종료. DB·문서·모델·Redis 볼륨 유지.')
        elif args.action == 'status':
            print(compose('ps', '--format', 'table').stdout.decode())
        elif args.action == 'ingest':
            env = java_env()
            result = run(['./gradlew', ':api:ragApiLabIngest', '--offline', '--console=plain', '--no-daemon'], cwd=ROOT, env=env)
            (STATE / 'ingest.log').write_bytes(result.stdout + result.stderr)
            print('적재 완료: .local/rag-api/results/ingest.json')
        elif args.action == 'backup':
            path=backup()
            print('백업 저장:', path.relative_to(ROOT))
        else: api(args.action, args.file, args.record)

if __name__ == '__main__':
    try:
        main()
    except subprocess.CalledProcessError as error:
        # 명령/환경에는 자격 증명이 포함될 수 있으므로 예외 원문을 출력하지 않는다.
        print('실행 실패. 반환 코드:', error.returncode, file=sys.stderr)
        sys.exit(1)
    except (RuntimeError, OSError, ValueError, BlockingIOError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
