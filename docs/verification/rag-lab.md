# 데이터를 보존하며 로컬에서 검색 실습하기

## 무엇을 직접 해볼 수 있는가

공식 문서를 실제 `bge-m3`로 임베딩하고 PostgreSQL에 저장한 뒤, 직접 입력한 검색어로 관련 문서·유사도·출처를 확인한다. 서비스를 중지하거나 컨테이너를 다시 만들어도 DB와 모델을 재사용한다.

현재 자료는 앞 단계에서 검토한 농촌진흥청 문서 2개와 검색 조각 2개다. 문서 확대나 전체 코칭 정확도 평가가 아니다. 답변 모델과 전체 API 서버는 실행하지 않으므로 코칭 답변·HTTP 인증·기록 생성부터 피드백 조회까지의 실행은 다음 단계다.

`bge-m3`는 이미 학습된 검색용 모델이다. 문서 적재는 원문을 숫자 벡터로 바꿔 별도 DB에 저장하는 과정이며 모델 가중치를 학습시키는 과정이 아니다. 제품 코칭은 검색된 문서를 프롬프트에 넣은 후 OpenClaw에 답변 생성을 요청하지만, 이 실습은 그 전 단계인 문서 적재·검색까지만 수행한다.

## 사전 준비와 최초 실행

Docker Desktop, Python 3, Java 21, 이 저장소의 Gradle 의존성 캐시가 필요하다. 기존에 확보한 PostgreSQL·Ollama 이미지와 `ccc-rag-bge-m3-cache` 모델 볼륨을 그대로 사용한다. 이미지나 모델을 자동 다운로드하지 않는다.

정식 폴더에서 실행한다.

```sh
cd /Users/kimseungjin/develop/ChamChamCham-backend-java
python3 scripts/rag-lab.py init
python3 scripts/rag-lab.py up
python3 scripts/rag-lab.py ingest
python3 scripts/rag-lab.py search --query '천궁은 고온기에 물 관리를 어떻게 해야 하나요?'
```

`init`은 이전 검증의 `/private/tmp/chamchamcham-rag-retrieval/`에서 검토된 자료만 복사하고 커밋된 출처·해시와 대조한다. 이후에는 `.local/rag/corpus/`를 사용하므로 임시 폴더나 인터넷 없이 재실행할 수 있다. 이미 보존한 자료는 덮어쓰지 않는다. 다른 컴퓨터의 새 clone에는 원문·모델이 포함되지 않으므로 이 안내만으로 즉시 실행되지는 않는다.

`up`은 loopback에서만 PostgreSQL `15432`, Ollama `11435`를 공개한다. 포트가 다른 실행에 점유됐거나 기존 모델 캐시를 다른 컨테이너가 사용 중이면 그 실행을 종료하지 않고 실패한다. `.env`를 잃은 상태에서 기존 DB 볼륨이 있으면 비밀번호를 새로 만들지 않는다.

첫 `ingest`는 CPU로 두 조각을 순차 임베딩한다. 두 번째부터는 동일 ID·본문·출처를 확인하고 이미 있는 자료를 건너뛴다. Java 작업은 명시적 실행 JUnit 도구를 통해 수행하며, 매 검색에도 Gradle/JVM 시작 시간이 포함된다. 따라서 명령 전체 소요 시간을 제품 API의 검색 지연 시간으로 해석하면 안 된다.

## 검색 결과 읽기

```sh
python3 scripts/rag-lab.py search --query '천궁 고온 수분 관리' --threshold 0.5
python3 scripts/rag-lab.py search --query '천궁 고온 수분 관리' --threshold 0.55
```

- `query`, `threshold`, `topK`: 실제 입력 조건. 검색당 최대 3개이며 현재 문서는 2개다.
- `results[].id`, `score`: 반환된 문서 ID와 코사인 유사도. 유사도는 정답 확률이 아니다.
- `text`: 실제 DB에서 검색된 본문.
- `metadata.sourceUrl`, `sourceId`, `documentTitle`: 원문 확인에 사용하는 출처.
- `results: []`: 설정한 필터·유사도 기준을 통과한 문서가 없다는 뜻이다. 오류나 생성된 답변이 아니다.

문서 유형은 `TECH_DOCUMENT`, 작물 분류는 `GENERAL`로 제한한다. GENERAL 자료가 반환됐다는 사실이 모든 작물에 적합하거나 질문에 답할 충분한 근거가 있다는 뜻은 아니다. 자료가 다루지 않는 농약 희석 배수·가격 질문도 일반 문서를 검색할 수 있다. 이 실행 도구는 제품 검색어 생성기를 실행하는 API가 아니라 실제 Spring AI 벡터 검색을 직접 실습하는 도구다.

기본 임계값 `0.5`는 현재 설정을 따른 것이며 최적값으로 검증된 수치가 아니다. 작은 자료에서 임계값을 올려 결과가 좋아 보이더라도 전체 성능 향상으로 표현하지 않는다.

## 중지와 재사용

```sh
python3 scripts/rag-lab.py status
python3 scripts/rag-lab.py stop
python3 scripts/rag-lab.py up
python3 scripts/rag-lab.py search --query '천궁 고온 수분 관리'
```

`stop`은 서비스만 중지한다. `down`은 이 Compose 프로젝트의 컨테이너와 네트워크를 제거하지만 볼륨은 유지한다. 그 후 `up`으로 다시 만든다. 도구는 볼륨 삭제 옵션을 제공하지 않는다. 수동으로 Docker 볼륨을 삭제하면 이 보존 조건을 벗어난다.

## 보존·백업·복구 검증

```sh
python3 scripts/rag-lab.py verify
python3 scripts/rag-lab.py backup
```

`verify`는 현재 두 문서가 적재된 상태에서 실행한다. 재적재 → stop/up → down/up 후 모든 문서 ID·본문·metadata·벡터가 같은지 비교한다. 모델 digest도 비교하고 재시작 후 실제 검색 결과가 있는지 확인한다.

그다음 `pg_dump -Fc` 백업을 **같은 전용 PostgreSQL 안의 별도 임시 DB**에 복구해 원본과 비교한다. 원본 DB는 덮어쓰지 않으며, 검증이 끝난 임시 복구 DB만 제거한다. 이는 DB 논리 백업의 복구 확인이며 다른 컴퓨터나 PostgreSQL 인스턴스 장애 복구까지 검증한 것은 아니다. 백업 파일은 계속 보존한다.

결과는 `.local/rag/results/verification.json`에 있다. 각 보존 조건의 성공 여부와 검색 결과, 백업 경로, 모델 digest, 원본 행 전체의 SHA-256을 확인할 수 있다. 실패한 새 실행에서는 이전 성공 파일을 먼저 제거하고 성공한 경우에만 다시 작성한다.

## 데이터와 결과가 있는 위치

| 대상 | 위치 | 보존 방식 |
|---|---|---|
| DB 본문·출처·벡터 | Docker 볼륨 `ccc-rag-lab-postgres` | stop/down 후 유지 |
| 기존 임베딩 모델 파일 | 외부 Docker 볼륨 `ccc-rag-bge-m3-cache` | 이 프로젝트 종료 시 유지 |
| 원문·선택 본문·manifest·chunks | `.local/rag/corpus/` | 임시 폴더 밖에 보존 |
| 비밀번호·DB 식별값 | `.local/rag/.env` | 권한 600, 재초기화 시 재사용 |
| 검색·적재·환경·검증 결과 | `.local/rag/results/` | 같은 이름의 최신 실행 결과로 갱신 |
| PostgreSQL 논리 백업 | `.local/rag/backups/` | 시각·임의 ID로 구분, 자동 삭제 없음 |

`.local/rag/` 전체는 Git에서 제외한다. DB 백업은 모델 파일과 원문 보관 폴더까지 백업하는 것이 아니다. 다른 컴퓨터로 옮기려면 DB 백업 외에 원문 폴더·자격 증명과 모델 볼륨도 별도로 보존해야 한다.

일반 `./gradlew test`에서는 이 실제 모델 검사가 조건부로 생략된다. `rag-lab.py`가 전용 실행 환경 변수를 전달한 경우에만 실행한다. 검사는 전용 DB의 식별값·테이블·열 타입·벡터 차원·HNSW 코사인 인덱스·pgvector 버전을 확인하며 예상과 다르면 자동 수정하지 않는다.

## 실패했을 때

- 포트 충돌: 오류에 나온 포트를 확인한다. 이 도구는 다른 서비스를 종료하지 않는다.
- 이미지·모델 불일치: 기존 식별값과 캐시를 확인한다. 자동 교체하지 않는다.
- 자격 증명 누락: 보관한 `.local/rag/.env`를 복구한다. 기존 DB 비밀번호를 임의로 바꾸지 않는다.
- 문서·DB 내용·스키마 불일치: 원본과 변경 내용을 먼저 확인한다. 도구는 이를 덮어써서 맞추지 않는다.
- Java 검사 실패·시간 초과: `.local/rag/results/gradle.log`를 확인한다. DB와 모델은 보존한다. 제한 시간은 30분이며 중단 시 Gradle 실행 그룹을 종료한다.

다음 작업은 답변 모델 선택·OpenClaw 연결과 실제 API 실행 계획이다. 그 뒤 자료 확대와 별도 평가 사례로 기준 성능을 측정하고, 실패 원인을 바탕으로 조정 전후를 비교한다.
