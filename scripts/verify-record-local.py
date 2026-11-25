#!/usr/bin/env python3
"""독립된 일회용 PostgreSQL에서 영농 기록 API 검증을 실행한다."""
import json
import os
from pathlib import Path
import secrets
import shutil
import xml.etree.ElementTree as ET
import subprocess
import tempfile
import time
import uuid

ROOT = Path(__file__).resolve().parents[1]
IMAGE = "pgvector/pgvector:0.8.5-pg16"
DATABASE = "ccc_record_verify"


def run(args, **kwargs):
    return subprocess.run(args, check=True, text=True, **kwargs)


def main():
    name = "ccc-record-verify-" + uuid.uuid4().hex[:10]
    token = uuid.uuid4().hex
    output = ROOT / "api/build/record-local-verification"
    output.mkdir(parents=True, exist_ok=True)
    # 이전 성공 결과를 이번 실패 결과로 오인하지 않도록 시작 시 제거한다.
    for filename in ("comparisons.json", "infrastructure.json"):
        (output / filename).unlink(missing_ok=True)
    image = json.loads(run(["docker", "image", "inspect", IMAGE], capture_output=True).stdout)[0]
    started = False
    try:
        with tempfile.TemporaryDirectory(prefix="ccc-record-verify-") as temporary:
            envfile = Path(temporary) / "postgres.env"
            password = secrets.token_urlsafe(32)
            envfile.write_text(f"POSTGRES_DB={DATABASE}\nPOSTGRES_USER=ccc_record_verify\nPOSTGRES_PASSWORD={password}\n")
            envfile.chmod(0o600)
            run(["docker", "run", "--detach", "--rm", "--pull=never", "--name", name,
                 "--label", "com.chamchamcham.verification=record-local",
                 "--env-file", str(envfile), "--mount", "type=tmpfs,destination=/var/lib/postgresql/data",
                 "--publish", "127.0.0.1::5432", IMAGE], capture_output=True)
            started = True
            for _ in range(120):
                ready = subprocess.run(["docker", "exec", name, "pg_isready", "-h", "127.0.0.1", "-U", "ccc_record_verify", "-d", DATABASE], capture_output=True)
                if ready.returncode == 0:
                    break
                time.sleep(0.5)
            else:
                raise RuntimeError("검증용 PostgreSQL 시작 시간 초과")
            run(["docker", "exec", "-i", name, "psql", "-U", "ccc_record_verify", "-d", DATABASE, "-v", "ON_ERROR_STOP=1"],
                input="CREATE TABLE local_verification_guard(token text NOT NULL);\n"
                      f"INSERT INTO local_verification_guard VALUES ('{token}');\n", capture_output=True)
            binding = json.loads(run(["docker", "inspect", name], capture_output=True).stdout)[0]["NetworkSettings"]["Ports"]["5432/tcp"][0]
            if binding["HostIp"] != "127.0.0.1":
                raise RuntimeError("검증 DB가 로컬 주소에만 노출되어야 합니다")
            environment = os.environ.copy()
            environment.update({
                "RUN_LOCAL_RECORD_VERIFY": "true",
                "RECORD_VERIFY_JDBC_URL": f"jdbc:postgresql://127.0.0.1:{binding['HostPort']}/{DATABASE}",
                "RECORD_VERIFY_DB_USER": "ccc_record_verify",
                "RECORD_VERIFY_DB_PASSWORD": password,
                "RECORD_VERIFY_TOKEN": token,
            })
            metadata = {"image": IMAGE, "imageId": image["Id"], "repoDigests": image.get("RepoDigests", []),
                        "container": name, "hostIp": binding["HostIp"], "port": binding["HostPort"],
                        "database": DATABASE, "storage": "new ephemeral tmpfs", "externalProviders": "mocked", "exitCode": None}
            print("독립 PostgreSQL 준비 완료. HTTP 처리 → DB → 조회 → AI 입력 검증 중입니다.", flush=True)
            command = ["./gradlew", ":api:cleanTest", ":api:test", "--tests", "com.chamchamcham.api.farming.FarmingRecordPostgresVerificationTest",
                       "--offline", "--console=plain"]
            with (output / "gradle.log").open("w") as log:
                result = subprocess.run(command, cwd=ROOT, env=environment, stdout=log, stderr=subprocess.STDOUT)
            metadata["exitCode"] = result.returncode
            junit = ROOT / "api/build/test-results/test/TEST-com.chamchamcham.api.farming.FarmingRecordPostgresVerificationTest.xml"
            if junit.is_file():
                shutil.copyfile(junit, output / "junit.xml")
                suite = ET.parse(junit).getroot()
                metadata["tests"] = {key: int(suite.get(key, "0")) for key in ("tests", "failures", "errors", "skipped")}
            (output / "infrastructure.json").write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n")
            print("\n".join((output / "gradle.log").read_text().splitlines()[-28:]))
            if result.returncode:
                raise SystemExit(result.returncode)
            if not (output / "comparisons.json").is_file():
                raise RuntimeError("테스트 비교 결과가 생성되지 않았습니다")
            print(f"검증 결과: {output}")
    finally:
        if started:
            # 이 실행에서 생성한 컨테이너만 제거한다. 기존 컨테이너·볼륨은 조회/변경하지 않는다.
            subprocess.run(["docker", "rm", "--force", name], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


if __name__ == "__main__":
    main()
