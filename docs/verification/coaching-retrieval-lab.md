# 실제 검색 비교 실습

이 실습은 **문서 검색과 조건 선별**을 실행한다. 답변 생성·농업 진단·제품 API 실행·모델 학습은 하지 않는다. 결과는 `coaching-retrieval-results.md`에서 한계와 함께 읽는다.

## 보존할 것

- 정식 저장소의 `.local/rag-api/corpus/`, `.local/rag-api/evidence-selection/`: 이전 원문/발췌/실험. 변경하지 않는다.
- `.local/rag-api/retrieval-expansion/`: 신규 원문, 준비된 corpus, 실행 결과, 로그, `.env`, 재개 기록.
- Docker 영구 볼륨 `ccc-coaching-retrieval-postgres-v2`: 새 평가 DB만 보관한다. 기존 `ccc-rag-api-postgres` 등은 사용하지 않는다.
- 기존 `.local/rag-api/native-app/`와 `native-models/`: 공증 확인한 macOS Ollama와 기존 모델. 삭제하거나 다시 받지 않는다.

Git clone만으로 `.local`의 원문과 모델이 복원되지 않는다. 다른 컴퓨터로 옮기려면 이 자료와 전용 DB 백업을 별도로 보존해야 한다. 원문은 비상업/변경금지 등 각 출처의 이용조건이 있으므로 이 저장소에서 원문 전체를 재배포하지 않는다. 비밀번호 `.env`는0600이며 공유·커밋하지 않는다.

## 실행

JDK21·Python3·이미 확보한 Gradle 의존성과 Docker 이미지가 필요하다. 이 명령들은 새 모델을 다운로드하지 않는다. Docker Desktop을 켠 다음 저장소 루트에서 실행한다.

```bash
python3 scripts/coaching-retrieval.py check-sources
python3 scripts/coaching-retrieval.py prepare
python3 scripts/coaching-retrieval.py up
python3 scripts/coaching-retrieval.py run --run my-retrieval-01
python3 scripts/coaching-retrieval.py summary --run my-retrieval-01
python3 scripts/coaching-retrieval.py stop
```

- PostgreSQL은 `127.0.0.1:15434/ccc_coaching_retrieval`만, 기존 macOS Ollama는11437만 사용한다. API18080·Redis16379·Docker Ollama11436은 시작하지 않는다.
- PG 컨테이너는1GiB/CPU1 한도. Docker 전역 자원 설정은 변경하지 않는다. 메모리가 부족하면 다른 사용자 서비스를 종료하지 말고 이번 실습을 중지한다.
- 기존11437이 살아 있으면 재사용한다. 다른 모델이 로드되어 있으면 밀어내지 않고 중단한다. 자신이 시작한 native PID만 `stop`이 종료한다. 기존에 실행 중이던 서버는 종료하지 않는다.
- 이름은 영문소문자/숫자로 시작하고 하이픈을 포함해 최대64자다. 이미 사용한 성공/실패 run은 덮어쓰지 않는다. 새 이름을 사용한다.
- 개발24개만 실행하려면 `run --run my-dev-01 --split dev`를 사용한다. 기본은 이미 노출된96개를 다시 확인하는 `regression`이다. 새 독립 평가로 세지 않는다.
- 처음에는 baseline26/split63/expanded82를 적재한다. 같은 자료는 재실행해도 기존 행을 덮어쓰지 않는다. 모든 그룹의 행과 스키마를 쓰기 전에 검사하고 최초 적재는 하나의 트랜잭션으로 처리한다. 원문·조건명세와 prepared가 다르거나 DB세대/ID/본문/메타데이터/차원이 다르거나 다른 테이블/스키마가 있으면 중단한다. corpus를 변경하면 다른 버전의 DB를 별도 계획으로 준비해야 한다. 자동 초기화/삭제 명령은 없다.

기존 자료를 바꾼 뒤 과거 결과를 현재 결과처럼 읽으려 하면 `summary`가 입력 해시 불일치로 거부한다. 과거 원시 결과는 그대로 남는다. 검사 기능을 해제하지 말고 해당 커밋/입력 해시로 읽거나 새로운 run을 만든다.

## 결과 읽기

```bash
python3 scripts/coaching-retrieval.py summary --run retrieval-eval-final
```

실행 폴더의 `started.json`은 입력 식별값, `raw.json`은 실제 SQL 검색 관찰, `result.json`은 최종 집계/원시 후보/선별 사유, `identity.json`은 결과 해시, `gradle.log`는 실행 로그다. `failure.json`이 있으면 완료로 세지 않는다. 로그에 자격 증명을 출력하지 않는다.

- baseline↔split은 **같은 기존 자료**의 분할 효과를 비교한다. 대상은 기존3작물48상황이다.
- expanded↔selected는 **같은82문단과 같은 후보**를 선별 전후 비교한다. 대상은6작물96상황이다.
- requiredEvidence는 조건부 자료를 포함해 찾아야 할 문헌이다. expectedApplicableEvidence는 그중 현재 입력 조건으로 적용 가능하다고 사전 주석한 부분이다. 둘의 분모가 다르다.
- forbiddenIds는 일부 대표 사례에 미리 지정한 부적합 문서다. 0이 되어도 전체 부적합 내용0이라는 뜻은 아니다.
- sourceId와 원문 구절을 함께 대조하며 인용ID/유사도만으로 정답을 판정하지 않는다. 0.5는 농업적 정답 확률이 아니다.
- 입력 사실의 코드형 주석은 사람이 위치와 확정 진술을 확인한 합성 데이터다. 자동 메모 해석 성능을 검증하지 않는다.

## 검사와 중지

```bash
python3 scripts/test-coaching-retrieval.py
./gradlew :api:test --tests '*CoachingRetrievalLabTest' --offline --console=plain --no-daemon --max-workers=1
./gradlew test --offline --console=plain --no-daemon --max-workers=1
python3 scripts/coaching-retrieval.py stop
```

기본 검사와 CI는 Docker/모델을 시작하지 않는다. 실제 검색은 명시적 CLI에서만 실행한다. `stop`은 컨테이너를 멈출 뿐 볼륨을 제거하지 않는다. 이번 데이터 삭제 기능은 제공하지 않는다. Docker Desktop을 종료하면 데이터는 남지만 다른 사용자 컨테이너가 실행 중인지 먼저 확인해야 한다.

최종DB는v2볼륨이고 이전실험의v1볼륨/백업은그대로있다. 원래96개평가뒤조건누락을고쳤으므로 현재도구는dev/regression만제공한다. 새평가자료와모델답변검증은별도작업이다.

이번백업은 `.local/rag-api/retrieval-expansion/retrieval-database-v2.dump` (이전v1은 `retrieval-database.dump`)에보존했다. 비밀번호파일과함께개인보관한다. 이번에는덤프를다른DB에복원하는검증은하지않았으며기존DB로복원/덮어쓰지않는다.
