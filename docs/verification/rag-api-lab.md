# 직접 실행하는 로컬 영농 기록 코칭 API

## 무엇을 실행하는가

이 실습은 기록을 HTTP로 제출하고, **실제 비밀번호 로그인·JWT 인증 → PostgreSQL 저장 → 비동기 입력 조립 → bge-m3 검색 → Ollama 답변 생성 → 기존 출력 검증 → 상태/답변 조회**를 실행한다. Docker의 전용 DB·Redis와 macOS Ollama를 사용한다. 기존 OpenClaw 경로는 일반 프로필에 남아 있다.

모델을 새로 학습하거나 농업 데이터로 미세조정한 것이 아니다. 이미 학습된 모델에 검색한 공식 문단을 함께 전달한다. 원래 팀원의 모델·OpenClaw 설정·원본 문서 DB를 복원했다고 표현하지 않는다.

전용 환경 주소:

| 구성 | 주소 | 보존 위치 |
|---|---|---|
| API | `http://127.0.0.1:18080` | 호스트 Java 21, 명시적 test 소스 실행기 |
| PostgreSQL/pgvector | `127.0.0.1:15433/ccc_rag_api` | `ccc-rag-api-postgres` 볼륨 |
| Redis | `127.0.0.1:16379` | `ccc-rag-api-redis` 볼륨, AOF |
| macOS Ollama | `http://127.0.0.1:11437` | `.local/rag-api/native-models` |
| Docker Ollama 대조 환경 | `http://127.0.0.1:11436` | 기존 `ccc-rag-bge-m3-cache`, 기본 중지 |

기존 검색 실습의 DB `ccc-rag-lab-postgres`와 자료 `.local/rag/`는 별개다. 제품의 `local` 프로필이나 기본 `bootRun`으로 이 실습 DB를 실행하지 않는다. 전용 실행기는 최초 식별된 빈 DB만 `create-only`, 이후는 `validate`로 실행한다.

## 준비된 이 컴퓨터에서 시작

다음은 저장소 루트에서 실행한다. Docker Desktop이 실행 중이어야 한다. 이미지·모델·공식 자료는 현재 컴퓨터에 보존돼 있다. 다른 컴퓨터에 새로 복제한 저장소만으로 자동 다운로드/설치되는 도구가 아니다.

```bash
cd /Users/kimseungjin/develop/ChamChamCham-backend-java
python3 scripts/rag-api.py up
python3 scripts/rag-api.py native-up
python3 scripts/rag-api.py start
python3 scripts/rag-api.py status
```

`start`는 준비 완료를 확인하고 반환한다. 실패하면 `.local/rag-api/api.log`를 확인한다. 포트가 사용 중이면 기존 서비스를 종료하지 않고 중단한다. `native-up`은 공식 앱의 서명·공증을 확인하며 CPU 대조용 Ollama 컨테이너만 중지한다. Mac 앱 GUI나 로그인 항목을 설치하지 않는다.

이미 API가 실행 중이면 `start`는 중복 실행을 거부한다. 이때 기존 실행을 그대로 사용하면 된다. `native-up`은 이미 실행 중인 전용 macOS 서버를 유지한다.

비밀번호·서명키는 `.local/rag-api/.env`에 최초 생성한 값을 재사용한다. 파일 권한은 `600`, 폴더는 `700`이며 Git에서 제외된다. 파일이 없는데 DB가 이미 있으면 새 비밀번호를 만들어 덮어쓰지 않는다. `.env`는 개인적으로 보존하고 터미널 로그·메신저·Git에 복사하지 않는다.

## 요청 JSON을 고치고 실제 API 호출

```bash
python3 scripts/evaluate-rag-api.py --export
```

`.local/rag-api/requests/`에 작업별 요청을 준비한다. 이미 수정한 파일은 덮어쓰지 않는다. 예를 들어 `.local/rag-api/requests/dev-1-watering-0.json`을 복사해 메모와 상세를 수정한다.

- `farmId`, `cropId`는 `.local/rag-api/fixtures.json`에 있는 합성 회원의 농장·작물을 사용한다.
- `workedAt`은 직접 실행하려는 날짜·시각으로 수정한다. 예: `2026-11-15T09:00:00`. 과거 기록의 날씨 처리 규칙은 기존 코드대로 적용된다.
- 메모는 30~500자다. 텍스트 입력 모드는 `MANUAL`이다.
- 농약·병해충 ID도 합성 자료다. 실제 제품이나 실제 사용 권장을 뜻하지 않는다.
- 일부 평가 요청에는 의도적인 거부 입력이 포함돼 있다. 수확의 `growthPeriod: null`은 현재 계약에서 거부된다.

```bash
python3 scripts/rag-api.py submit --file .local/rag-api/requests/dev-1-watering-0.json
```

성공 응답 `data.id`가 기록 ID다. 아래 `기록UUID`를 해당 값으로 바꾼다.

```bash
python3 scripts/rag-api.py get --record 기록UUID
python3 scripts/rag-api.py feedback --record 기록UUID
```

처음에는 `PENDING`, 검증까지 통과하면 `READY`와 답변이 나온다. 생성/검색/검증 실패는 `FAILED`와 `failureCode`로 표시된다. 같은 조회를 다시 실행해 상태를 확인한다. 도구는 정상 로그인 API에서 JWT를 발급받아 메모리에서 사용하며 인증을 우회하지 않는다. JWT나 로그인 응답을 파일에 저장하거나 터미널에 출력하지 않는다.

수정은 동일한 요청 JSON 형식이며 현재 API는 전체 필수값을 다시 받는다. 선택 상세를 빼면 기존 상세가 제거될 수 있다. 응답의 수확 `amountUnknown`을 요청에 그대로 붙이지 말고 요청 필드 `harvestAmountUnknown`을 사용한다.

```bash
python3 scripts/rag-api.py update --record 기록UUID --file .local/rag-api/requests/dev-1-watering-0.json
python3 scripts/rag-api.py feedback --record 기록UUID
python3 scripts/rag-api.py regenerate --record 기록UUID
python3 scripts/rag-api.py feedback --record 기록UUID
```

재생성은 현재 계약상 `FAILED`에서만 허용된다. `READY`에서 요청하면 409이며, 기록 수정을 통한 새 revision 생성과 구분한다. 수정·허용된 재생성은 실제 비동기 처리다. 완료 여부는 상태로 확인한다. 모델 한 요청의 제한은 240초, 제품 PENDING 만료는 900초다. 동시에 여러 생성을 몰아서 제출하지 말고 한 건이 끝난 뒤 다음 요청을 실행한다.

## 문서 재적재와 결과 확인

```bash
python3 scripts/rag-api.py ingest
```

원문/선택 본문 해시, DB 식별, 벡터 스키마를 확인하고 없는 문단만 넣는다. 동일 ID의 기존 본문이나 메타데이터가 다르면 덮어쓰지 않고 실패한다. 보통 재실행에서는 원문이나 모델을 다시 다운로드하지 않는다. 적재 결과는 `.local/rag-api/results/ingest.json`에 있다.

입력→조회→DB AI 문맥은 `.local/rag-api/evaluations/<실행명>/<사례ID>.json`에 남는다. 실제 검색어·임계값·유사도·본문·출처와 모델 원시 응답은 `.local/rag-api/traces/`에 남는다. 여기는 **합성 입력 실습용**이다. 실제 개인 영농 기록을 넣으면 관측 파일에도 그 내용이 남으므로 이 도구의 검증 자료로 섞지 않는다.

문서는 `.local/rag-api/corpus/`, 출처·선택 범위·제외 이유는 `docs/verification/rag-api-sources.json`에 있다. PDF의 `page`는 파일의 물리적 페이지 순서이며 인쇄 번호와 다를 수 있다. 최신 참당귀 매뉴얼은 PDF 27/32쪽(인쇄 28/33쪽)을 이미지로 대조했다.

## 평가를 다시 실행

```bash
python3 scripts/evaluate-rag-api.py --split dev --run my-development
python3 scripts/evaluate-rag-api.py --split evaluation --run my-evaluation
```

같은 실행명과 같은 모델/설정/사례라면 완료 기록을 재실행하지 않는다. 이미 생성된 미완료 기록은 조회부터 이어간다. API 요청 도중 결과를 잃어 생성 여부가 불명확하면 자동 재제출하지 않는다. 모델/설정/사례가 달라졌다면 새 실행명을 사용한다.

이미 결과를 본 평가 사례는 이후 조정용으로만 사용한다. 같은 48개로 반복한 값을 새로운 독립 평가 점수로 해석하지 않는다. 문헌 일치 판정은 인용 ID의 존재나 `READY`만으로 자동 통과시키지 않는다. 판정 기준은 `docs/verification/rag-api-evaluation-protocol.json`을 참고한다.

## 중지·재시작과 백업

```bash
python3 scripts/rag-api.py stop-api
python3 scripts/rag-api.py backup
python3 scripts/rag-api.py verify-restore
python3 scripts/rag-api.py stop
```

`stop-api`는 전용 API만 정상 종료한다. `stop`은 전용 API·macOS 모델 서버·Compose 서비스들을 중지한다. `down`은 전용 컨테이너를 제거하지만 DB/모델/Redis 볼륨을 삭제하지 않는다. 볼륨 삭제 명령은 이 도구에 없다.

`backup`은 접근 권한 `600`의 `.local/rag-api/backups/*.dump`를 만든다. `verify-restore`는 API가 중지됐고 PENDING이 없을 때 실행하며, 별도의 새 임시 DB로 복구해 모든 테이블의 행 수/내용 해시를 비교한다. 확인 후 이번 명령이 만든 임시 DB만 제거한다. 결과는 `.local/rag-api/results/restore-verification.json`에 남는다.

재시작은 처음의 `up → native-up → start`를 사용한다. 생성 완료된 기록·답변·문서는 DB에 남는다. 처리 중 앱을 종료한 경우는 완료 답변의 보존과 구분해야 한다. 기존 비동기 이벤트 큐는 메모리 기반이므로 재시작이 진행 중 요청을 자동으로 재실행하는 보장은 없다. 상태를 확인하고 만료/실패 후 재생성한다.

최초 부팅이 중단돼 DB가 `INITIALIZING`이면 `bootstrap`을 반복하지 않는다. 원인을 확인·수정한 뒤 `recover-bootstrap`은 기존 테이블을 **validate만** 하여 부팅하고 성공한 경우에만 완료 표시한다. 빈 DB로 되돌리거나 자동 재생성하지 않는다.

## 보존하는 모델과 제외 범위

기본은 `qwen3:4b-instruct` Q4_K_M, 대조용 `qwen2.5:7b-instruct-q4_K_M`도 보존한다. 신규 다운로드 manifest layer 합계는 7,180,380,161바이트로 10GB 이내다. bge-m3는 기존 가중치의 복사본을 사용했다. temperature 0, seed 42, 문맥 8,192, 생성 상한 1,024, 동시 실행 1이다. 실행 옵션 원문은 `.local/rag-api/runtime-options.json`과 각 평가 폴더의 사본에 있다.

모델을 바꾸려면 API를 중지하고 다음을 실행한다. 다운로드는 자동으로 하지 않는다.

```bash
python3 scripts/rag-api.py stop-api
python3 scripts/rag-api.py select-model --model qwen2.5:7b-instruct-q4_K_M
python3 scripts/rag-api.py start
```

기본으로 돌아갈 때는 `--model qwen3:4b-instruct`를 사용한다. 모델이 크다는 이유로 더 정확하다고 판단하지 않는다.

날씨는 `SYNTHETIC_LAB`의 고정 32℃/맑음이며 예보는 비어 있다. 보고서 재계산은 대역이다. 소셜 로그인·메일·음성·사진·실제 날씨 제공자와 보고서 AI는 이번 검증에서 호출하지 않았다. 전체 배포용 앱의 이 기능들까지 검증했다고 해석하지 않는다.

총살포량은 사용자 합의대로 **농약+물 최종 희석액 전체량**으로 관찰한다. 기존 프롬프트의 ‘약을 섞은 물의 양’ 표현은 사용자 지시에 따라 유지했다. 이 문구와 의미의 차이를 숨기지 않는다. 개별 농약의 현행 등록·희석량·안전 사용 적합성 판정은 구현하거나 검증한 기능이 아니다.

## 실제 결과 확인

- 전체 결과: [rag-api-results.md](rag-api-results.md)
- 48건 baseline과 개별 판정: [rag-api-independent-results.json](rag-api-independent-results.json) / [CSV](rag-api-independent-results.csv)
- 실제 입력에서 프롬프트까지: [rag-api-flow-examples.json](rag-api-flow-examples.json)
- 실행·수정·인증·보존·복구: [rag-api-runtime-checks.json](rag-api-runtime-checks.json)
