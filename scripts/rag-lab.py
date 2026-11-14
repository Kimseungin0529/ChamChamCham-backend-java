#!/usr/bin/env python3
"""보존형 로컬 검색 실습. 삭제·모델 다운로드·외부 AI 호출 명령은 제공하지 않는다."""
import argparse
from datetime import datetime, timezone
import fcntl
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import secrets
import shutil
import signal
import socket
import subprocess
import sys
import tempfile
import urllib.request
import uuid

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parents[1]
STATE = ROOT / '.local/rag'
CORPUS = STATE / 'corpus'
DB = 'ccc_rag_lab'
MODEL_DIGEST = '7907646426070047a77226ac3e684fbbe8410524f7b4a74d02837e43f2146bab'
IMAGES = {
    'pgvector/pgvector@sha256:1d533553fefe4f12e5d80c7b80622ba0c382abb5758856f52983d8789179f0fb':
        'sha256:405e59721305d49a4e570e38ec1fa7f3b2151efcc71c19a2009501dd975a39c7',
    'ollama/ollama@sha256:910d0d8107b538fb0894e63163eb195de0cc7aefcbc666dc62d3b18f71124592':
        'sha256:88ccff954f5fe575ba39317bf52a730c43cfd08789c09bac8f2a90d664b72a7b'}


def run(args, **kwargs):
    return subprocess.run(args, check=True, capture_output=True, **kwargs)


def credentials(path):
    if not path.exists():
        fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(fd, 'w') as file:
            file.write('POSTGRES_PASSWORD=' + secrets.token_urlsafe(32) + '\nLAB_TOKEN=' + uuid.uuid4().hex + '\n')
    path.chmod(0o600)
    values = dict(line.split('=', 1) for line in path.read_text().splitlines())
    if set(values) != {'POSTGRES_PASSWORD', 'LAB_TOKEN'} or any(
            not value or any(c not in 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-' for c in value)
            for value in values.values()):
        raise RuntimeError('실습 자격 증명 파일이 올바르지 않습니다')
    return values


def require_free_port(port):
    with socket.socket() as sock:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            sock.bind(('127.0.0.1', port))
        except OSError as error:
            raise RuntimeError(f'127.0.0.1:{port} 사용 중. 기존 서비스를 종료하지 않았습니다') from error


def settings():
    if not (STATE / '.env').exists():
        raise RuntimeError('실습 자격 증명 파일이 없습니다. 기존 DB가 있다면 보관한 파일을 복구하세요')
    return credentials(STATE / '.env')


def validate_corpus(path):
    trusted = json.loads((ROOT / 'docs/verification/rag-local-sources.json').read_text())
    manifest = json.loads((path / 'manifest.json').read_text())
    if len(manifest) != len(trusted):
        raise RuntimeError('검토된 문서 목록과 다릅니다')
    spec = importlib.util.spec_from_file_location('prepare', ROOT / 'scripts/prepare-rag-corpus.py')
    prepare = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(prepare)
    expected = []
    for row, reference in zip(manifest, trusted):
        for key in ('sourceId', 'title', 'url', 'published', 'rawSha256', 'selectedSha256'):
            if row[key] != reference[key]:
                raise RuntimeError('검토된 출처·해시와 다릅니다')
        source = row['sourceId']
        if hashlib.sha256((path / (source + '.html')).read_bytes()).hexdigest() != row['rawSha256']:
            raise RuntimeError('원문 해시가 다릅니다')
        text = '\n'.join(row['selectedParagraphs'])
        if hashlib.sha256(text.encode()).hexdigest() != row['selectedSha256'] or (
                path / (source + '.agriculture.txt')).read_text() != text + '\n':
            raise RuntimeError('선택 본문 해시가 다릅니다')
        for i, chunk in enumerate(prepare.chunks(row['selectedParagraphs'])):
            expected.append({'id': str(uuid.uuid5(uuid.NAMESPACE_URL, row['url'] + ':' + row['selectedSha256'] + ':' + str(i))),
                'text': chunk['text'], 'metadata': {'sourceType': 'TECH_DOCUMENT', 'cropName': 'GENERAL',
                'documentTitle': row['title'], 'sourceUrl': row['url'], 'sourceId': source, 'published': row['published'],
                'rawSha256': row['rawSha256'], 'selectedSha256': row['selectedSha256'],
                'paragraphStart': chunk['paragraphStart'], 'paragraphEnd': chunk['paragraphEnd'],
                'chunkIndex': i, 'overlapChars': 0 if i == 0 else 160}})
    if json.loads((path / 'chunks.json').read_text()) != expected:
        raise RuntimeError('검색 조각의 본문·ID·출처가 검토 자료와 다릅니다')


def compose(*args):
    # 호출 환경의 POSTGRES_PASSWORD가 이 실습의 저장된 비밀번호를 덮어쓰지 못하게 한다.
    environment = os.environ.copy()
    environment.update(settings())
    return run(['docker', 'compose', '--project-directory', str(ROOT), '--env-file', str(STATE / '.env'),
                '-f', str(ROOT / 'compose.rag-lab.yml'), *args], env=environment)


def container(service):
    ident = compose('ps', '-q', service).stdout.decode().strip()
    if not ident:
        raise RuntimeError('실습 서비스를 먼저 up으로 시작하세요')
    return ident


def sql(query, database=DB):
    return run(['docker', 'exec', '-i', container('postgres'), 'psql', '-X', '-At', '-U', DB,
                '-d', database, '-v', 'ON_ERROR_STOP=1'], input=query.encode()).stdout.decode().strip()


def guard():
    token = settings()['LAB_TOKEN']
    schemas = sql("select nspname from pg_namespace where nspname !~ '^pg_' and nspname <> 'information_schema' order by nspname").splitlines()
    if schemas != ['public']:
        raise RuntimeError('예상하지 않은 사용자 스키마: 자동 수정하지 않습니다')
    extensions = sql('select extname from pg_extension').splitlines()
    if set(extensions) - {'plpgsql', 'vector', 'hstore', 'uuid-ossp'}:
        raise RuntimeError('예상하지 않은 DB 확장: 자동 수정하지 않습니다')
    tables = sql("select c.relname from pg_class c join pg_namespace n on n.oid=c.relnamespace "
                 "where n.nspname='public' and c.relkind in ('r','p','v','m','S','f') order by c.relname").splitlines()
    if not tables:
        sql("BEGIN; CREATE TABLE lab_identity(token text PRIMARY KEY, schema_version integer NOT NULL); "
            f"INSERT INTO lab_identity VALUES ('{token}',1); COMMIT;")
    elif set(tables) - {'lab_identity', 'vector_store'} or 'lab_identity' not in tables:
        raise RuntimeError('예상하지 않은 DB 테이블: 자동 수정하지 않습니다')
    if sql('select token || \':\' || schema_version from lab_identity') != token + ':1':
        raise RuntimeError('실습 DB 식별 정보 불일치: 변경하지 않습니다')


def model():
    with urllib.request.urlopen('http://127.0.0.1:11435/api/tags', timeout=10) as response:
        models = json.load(response)['models']
    found = next((m for m in models if m['name'] == 'bge-m3:latest'), None)
    if not found or found['digest'] != MODEL_DIGEST:
        raise RuntimeError('검증된 bge-m3 캐시가 없습니다. 자동 다운로드·교체하지 않습니다')
    return {'name': found['name'], 'digest': found['digest'], 'size': found['size']}


def initialize():
    STATE.mkdir(parents=True, exist_ok=True, mode=0o700)
    if not (STATE / '.env').exists():
        run(['docker', 'info', '--format', '{{.ServerVersion}}'])
        volume = subprocess.run(['docker', 'volume', 'inspect', 'ccc-rag-lab-postgres'], capture_output=True)
        if volume.returncode == 0:
            raise RuntimeError('기존 DB의 자격 증명 파일이 없습니다. 새로 생성하지 않습니다')
        if b'no such volume' not in volume.stderr.lower():
            raise RuntimeError('기존 DB 볼륨 유무를 확인할 수 없습니다')
    credentials(STATE / '.env')
    if not CORPUS.exists():
        source = Path('/private/tmp/chamchamcham-rag-retrieval')
        validate_corpus(source)
        with tempfile.TemporaryDirectory(prefix='corpus-', dir=STATE) as directory:
            staged = Path(directory) / 'corpus'
            staged.mkdir()
            for name in ('manifest.json', 'chunks.json', '100000732652.html', '100000802838.html',
                         '100000732652.agriculture.txt', '100000802838.agriculture.txt'):
                shutil.copy2(source / name, staged / name)
            validate_corpus(staged)
            staged.rename(CORPUS)
    validate_corpus(CORPUS)


def start():
    initialize()
    for image, ident in IMAGES.items():
        if json.loads(run(['docker', 'image', 'inspect', image]).stdout)[0]['Id'] != ident:
            raise RuntimeError('기존 검증 이미지와 다릅니다. 자동 다운로드하지 않습니다')
    run(['docker', 'volume', 'inspect', 'ccc-rag-bge-m3-cache'])
    volume = subprocess.run(['docker', 'volume', 'inspect', 'ccc-rag-lab-postgres'], capture_output=True)
    if volume.returncode == 0 and (json.loads(volume.stdout)[0].get('Labels') or {}).get('com.docker.compose.project') != 'ccc-rag-lab':
        raise RuntimeError('동명 DB 볼륨이 다른 작업 소유입니다')
    for ident in run(['docker', 'ps', '-q']).stdout.decode().split():
        item = json.loads(run(['docker', 'inspect', ident]).stdout)[0]
        if any(m.get('Name') == 'ccc-rag-bge-m3-cache' for m in item['Mounts']) and (
                (item['Config'].get('Labels') or {}).get('com.docker.compose.project') != 'ccc-rag-lab'):
            raise RuntimeError('기존 모델 캐시를 다른 실행이 사용 중입니다')
    for service, port in (('postgres', 15432), ('ollama', 11435)):
        if not compose('ps', '-q', service).stdout.strip():
            require_free_port(port)
    compose('up', '-d', '--wait', '--wait-timeout', '120', '--pull', 'never')
    guard()
    save('environment', {'images': IMAGES, 'model': model(), 'postgresVolume': 'ccc-rag-lab-postgres',
                         'modelVolume': 'ccc-rag-bge-m3-cache', 'externalChat': False})


def save(name, data):
    path = STATE / 'results' / (name + '.json')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n')
    return path


def java(action, query='', threshold=0.5):
    output = STATE / 'results' / (action + '.json')
    output.parent.mkdir(parents=True, exist_ok=True)
    output.unlink(missing_ok=True)
    guard()
    validate_corpus(CORPUS)
    model()
    env = os.environ.copy()
    env.update({'RUN_RAG_LAB': 'true', 'RAG_LAB_ACTION': action, 'RAG_LAB_QUERY': query,
                'RAG_LAB_THRESHOLD': str(threshold), 'RAG_LAB_ROOT': str(STATE),
                'RAG_LAB_PASSWORD': settings()['POSTGRES_PASSWORD'],
                'RAG_LAB_TOKEN': settings()['LAB_TOKEN']})
    log = STATE / 'results/gradle.log'
    with log.open('w') as file:
        process = subprocess.Popen(['./gradlew', ':api:cleanTest', ':api:test', '--tests',
            'com.chamchamcham.api.coaching.recordfeedback.RagLabLocalTest',
            '--offline', '--console=plain', '--no-daemon'], cwd=ROOT, env=env,
            stdout=file, stderr=subprocess.STDOUT, start_new_session=True)
        try:
            code = process.wait(timeout=1800)
        except (subprocess.TimeoutExpired, KeyboardInterrupt):
            os.killpg(process.pid, signal.SIGTERM)
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL)
                process.wait()
            raise RuntimeError('실습 실행 중단. DB와 모델은 보존했습니다')
    if code or not output.exists():
        raise RuntimeError(f'실습 검사 실패. 확인: {log}')
    return json.loads(output.read_text())


def snapshot(database=DB):
    return json.loads(sql("select coalesce(jsonb_agg(to_jsonb(v) order by id),'[]') from vector_store v", database))


def backup():
    guard()
    path = STATE / 'backups' / (datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S') + '-' + uuid.uuid4().hex[:8] + '.dump')
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    data = run(['docker', 'exec', container('postgres'), 'pg_dump', '-U', DB, '-d', DB, '-Fc']).stdout
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, 'wb') as file:
        file.write(data)
    return path


def verify():
    (STATE / 'results/verification.json').unlink(missing_ok=True)
    guard()
    before = snapshot()
    if not before:
        raise RuntimeError('먼저 ingest를 실행하세요')
    digest = model()
    java('ingest')
    if snapshot() != before:
        raise RuntimeError('재적재로 저장 내용 변경')
    compose('stop')
    start()
    if snapshot() != before or model() != digest:
        raise RuntimeError('재시작 후 보존 실패')
    compose('down')  # --volumes 금지: 영구 볼륨은 계속 보존한다.
    start()
    if snapshot() != before or model() != digest:
        raise RuntimeError('컨테이너 재생성 후 보존 실패')
    search = java('search', '천궁 고온 수분 관리', 0.5)
    if not search['results']:
        raise RuntimeError('재시작 후 실제 검색 결과 없음')
    path = backup()
    restore = 'ccc_rag_restore_' + uuid.uuid4().hex
    sql('CREATE DATABASE ' + restore)
    try:
        run(['docker', 'exec', '-i', container('postgres'), 'pg_restore', '-U', DB,
             '-d', restore, '--exit-on-error', '--no-owner'], input=path.read_bytes())
        if snapshot(restore) != before or sql('select token from lab_identity', restore) != settings()['LAB_TOKEN']:
            raise RuntimeError('백업 복구 후 본문·출처·벡터 또는 DB 식별 정보 불일치')
    finally:
        sql('DROP DATABASE ' + restore)
    return {'documents': len(before), 'reingest': True, 'restart': True, 'recreate': True,
            'backupRestore': True, 'searchAfterRestart': search, 'backup': str(path), 'model': digest,
            'rowSha256': hashlib.sha256(json.dumps(before, sort_keys=True).encode()).hexdigest()}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['init', 'up', 'stop', 'down', 'status', 'ingest', 'search', 'backup', 'verify'])
    parser.add_argument('--query', default='천궁 고온 수분 관리')
    parser.add_argument('--threshold', type=float, default=0.5)
    args = parser.parse_args()
    if not 0 <= args.threshold <= 1 or not args.query.strip():
        parser.error('검색어는 공백일 수 없고 임계값은 0~1이어야 합니다')
    STATE.mkdir(parents=True, exist_ok=True, mode=0o700)
    with (STATE / '.lock').open('w') as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            raise RuntimeError('다른 실습 명령이 실행 중입니다')
        if args.action == 'init':
            initialize()
            print('원문·조각·자격 증명 보존 준비 완료: ' + str(STATE))
        elif args.action == 'up':
            start()
            print('시작 완료. DB 127.0.0.1:15432 / 임베딩 127.0.0.1:11435')
        elif args.action in ('stop', 'down', 'status'):
            print(compose('ps', '--all').stdout.decode() if args.action == 'status' else compose(args.action).stderr.decode())
        elif args.action in ('ingest', 'search'):
            print(json.dumps(java(args.action, args.query, args.threshold), ensure_ascii=False, indent=2))
        elif args.action == 'backup':
            print('백업: ' + str(backup()))
        else:
            result = verify()
            print('보존·복구 검증 결과: ' + str(save('verification', result)))


if __name__ == '__main__':
    try:
        main()
    except (RuntimeError, OSError, ValueError, subprocess.SubprocessError) as error:
        # 명령·환경 변수에 비밀값이 담기는 예외 repr은 출력하지 않는다.
        print(str(error) if isinstance(error, RuntimeError) else '실행 실패: ' + type(error).__name__, file=sys.stderr)
        sys.exit(1)
