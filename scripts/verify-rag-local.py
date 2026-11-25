#!/usr/bin/env python3
"""새 pgvector DB와 로컬 bge-m3로 실제 코칭 검색 경로를 검증한다."""
import json
import os
from pathlib import Path
import secrets
import signal
import subprocess
import sys
import tempfile
import time
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / 'api/build/rag-local-verification'
CACHE = Path('/private/tmp/chamchamcham-rag-retrieval')
PG_IMAGE = 'pgvector/pgvector:0.8.5-pg16'
OLLAMA_IMAGE = 'ollama/ollama:latest'

def run(args, **kwargs):
    return subprocess.run(args, check=True, text=True, **kwargs)

def inspect(name):
    return json.loads(run(['docker', 'inspect', name], capture_output=True).stdout)[0]

def port(name, internal):
    binding = inspect(name)['NetworkSettings']['Ports'][internal + '/tcp'][0]
    if binding['HostIp'] != '127.0.0.1':
        raise RuntimeError('검증 서비스는 loopback에만 노출해야 합니다')
    return binding['HostPort']

def request(url, body=None, timeout=60):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(url, data=data, headers={'Content-Type': 'application/json'})
    with urllib.request.urlopen(req, timeout=timeout) as response:
        return json.load(response)

def run_verification(command, environment, log, timeout=1800):
    process = subprocess.Popen(command, cwd=ROOT, env=environment, stdout=log,
                               stderr=subprocess.STDOUT, start_new_session=True)
    try:
        code = process.wait(timeout=timeout)
        return subprocess.CompletedProcess(command, code)
    except subprocess.TimeoutExpired:
        os.killpg(process.pid, signal.SIGTERM)
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            pass
        try:
            os.killpg(process.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass
        process.wait()
        raise

def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    CACHE.mkdir(parents=True, exist_ok=True)
    for name in ('comparisons.json', 'database.json', 'infrastructure.json'):
        (OUTPUT / name).unlink(missing_ok=True)
    run([sys.executable, str(ROOT / 'scripts/prepare-rag-corpus.py'), '--output', str(CACHE)])
    # 최신 태그가 가리키는 이미지는 시작 전에 ID로 고정하여 한 실행 중 바뀌지 않게 한다.
    images = {}
    for tag in (PG_IMAGE, OLLAMA_IMAGE):
        info = json.loads(run(['docker', 'image', 'inspect', tag], capture_output=True).stdout)[0]
        images[tag] = {'id': info['Id'], 'digests': info.get('RepoDigests', [])}
    suffix = uuid.uuid4().hex[:10]
    pg, ollama = 'ccc-rag-pg-' + suffix, 'ccc-rag-embed-' + suffix
    created = []
    token, password = uuid.uuid4().hex, secrets.token_urlsafe(32)
    metadata = {'images': images, 'externalChat': 'disabled; capture prompt then intentionally stop',
                'inference': 'local CPU', 'dbStorage': 'new tmpfs', 'modelCache': 'ccc-rag-bge-m3-cache', 'exitCode': None}
    try:
        run(['docker', 'run', '-d', '--rm', '--pull=never', '--name', ollama,
             '--label', 'com.chamchamcham.verification=rag-local',
             '-e', 'OLLAMA_NUM_PARALLEL=1', '-e', 'OLLAMA_MAX_LOADED_MODELS=1', '-e', 'OLLAMA_NO_CLOUD=1',
             '-v', 'ccc-rag-bge-m3-cache:/root/.ollama', '-p', '127.0.0.1::11434',
             images[OLLAMA_IMAGE]['id']], capture_output=True)
        created.append(ollama)
        base = 'http://127.0.0.1:' + port(ollama, '11434')
        for _ in range(60):
            try:
                metadata['ollamaVersion'] = request(base + '/api/version')
                break
            except (OSError, ValueError):
                time.sleep(1)
        else:
            raise RuntimeError('Ollama 시작 실패')
        models = request(base + '/api/tags').get('models', [])
        if not any(m['name'] == 'bge-m3:latest' for m in models):
            print('공식 bge-m3 임베딩 모델 다운로드 중입니다. 전용 캐시를 유지합니다.', flush=True)
            pull = request(base + '/api/pull', {'model': 'bge-m3', 'stream': False}, timeout=3600)
            if pull.get('status') != 'success':
                raise RuntimeError('bge-m3 다운로드 실패')
        metadata['model'] = next(m for m in request(base + '/api/tags')['models'] if m['name'] == 'bge-m3:latest')
        print('모델 준비 완료. 독립 DB와 실제 검색 경로 검증을 시작합니다.', flush=True)
        with tempfile.TemporaryDirectory(prefix='ccc-rag-') as directory:
            envfile = Path(directory) / 'pg.env'
            envfile.write_text(f'POSTGRES_DB=ccc_rag_verify\nPOSTGRES_USER=ccc_rag_verify\nPOSTGRES_PASSWORD={password}\n')
            envfile.chmod(0o600)
            run(['docker', 'run', '-d', '--rm', '--pull=never', '--name', pg,
                 '--label', 'com.chamchamcham.verification=rag-local', '--env-file', str(envfile),
                 '--mount', 'type=tmpfs,destination=/var/lib/postgresql/data',
                 '-p', '127.0.0.1::5432', images[PG_IMAGE]['id']], capture_output=True)
            created.append(pg)
            for _ in range(120):
                ready = subprocess.run(['docker', 'exec', pg, 'pg_isready', '-h', '127.0.0.1', '-U', 'ccc_rag_verify'], capture_output=True)
                if ready.returncode == 0:
                    break
                time.sleep(.5)
            else:
                raise RuntimeError('PostgreSQL 시작 실패')
            run(['docker', 'exec', '-i', pg, 'psql', '-U', 'ccc_rag_verify', '-v', 'ON_ERROR_STOP=1'],
                input=f"CREATE TABLE local_verification_guard(token text NOT NULL); INSERT INTO local_verification_guard VALUES ('{token}');", capture_output=True)
            env = os.environ.copy()
            env.update({'RUN_LOCAL_RETRIEVAL_VERIFY': 'true', 'RAG_VERIFY_JDBC_URL': f'jdbc:postgresql://127.0.0.1:{port(pg, "5432")}/ccc_rag_verify',
                        'RAG_VERIFY_OLLAMA_URL': base, 'RAG_VERIFY_DB_PASSWORD': password,
                        'RAG_VERIFY_TOKEN': token, 'RAG_VERIFY_CORPUS': str(CACHE)})
            command = ['./gradlew', ':api:cleanTest', ':api:test', '--tests',
                       'com.chamchamcham.api.coaching.recordfeedback.RecordFeedbackRetrievalLocalTest', '--offline', '--console=plain', '--no-daemon']
            with (OUTPUT / 'gradle.log').open('w') as log:
                result = run_verification(command, env, log)
            metadata['exitCode'] = result.returncode
            # 비교 전후 모델 digest 동일 여부를 확인한다.
            after = next(m for m in request(base + '/api/tags')['models'] if m['name'] == 'bge-m3:latest')
            if after['digest'] != metadata['model']['digest']:
                raise RuntimeError('비교 도중 모델이 변경되었습니다')
            print('\n'.join((OUTPUT / 'gradle.log').read_text().splitlines()[-30:]))
            if result.returncode:
                raise SystemExit(result.returncode)
            if not (OUTPUT / 'comparisons.json').is_file():
                raise RuntimeError('비교 결과 누락')
    finally:
        (OUTPUT / 'infrastructure.json').write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + '\n')
        for name in reversed(created):
            subprocess.run(['docker', 'rm', '-f', name], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    print('결과: ' + str(OUTPUT), flush=True)

if __name__ == '__main__':
    main()
