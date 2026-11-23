# 기록 코칭 근거 적합성 점검 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking. 2026-11-23 사용자 ㄱㄱ 승인 범위 실행·검증·리뷰·로컬 커밋 완료.

**Goal:** 보존 자료의 적용 조건을 확인하고, 고정 후보에서 잘못 적용될 근거를 구분하면서 일반 안내를 보존하는 실험을 만든다.

**Architecture:** 원문/문단의 조건을 별도 JSON으로 보존한다. 제품 문맥과 후보 Document를 받는 test 전용 선택기가 분류·사유를 반환한다. 평가기는 기대값을 선택기에 전달하지 않고 결과를 대조한다. DB나 답변 모델 호출 없이 첫 단위를 완료한다.

**Tech Stack:** Java 21, 현재 Spring AI 1.1.8 Document, Jackson, JUnit 5, Python 표준 라이브러리, Gradle Kotlin DSL.

**Spec:** `docs/superpowers/specs/2026-11-23-coaching-evidence-selection-design.md`

## Global Constraints

- 첫 단위는 test 소스, 합성 사례, 출처/조건 목록, 실행 도구, 문서만 변경한다. 제품 src/main, 공개 계약, DB, 농약 명칭·사용 적합성 정책은 바꾸지 않는다.
- Docker/API와 모델은 첫 단위에서 시작하지 않는다. 현재 데이터·모델·백업을 삭제하지 않는다. 새 설치/다운로드는 하지 않는다.
- 원문/선택 본문 자체는 Git에 넣지 않는다. `.local/rag-api/evidence-selection/`은 기존 제외 규칙을 확인한다.
- push·PR·병합·배포는 하지 않는다. 브랜치/로컬 커밋은 실행 승인 후 수행한다.

## Review Focus

- 품종 미상은 특정 품종과 동일하다고 취급하지 않되, 제한 없는 일반 안내는 유지한다.
- 실제 수확 부위가 기본 작물 부위와 다르면 실제 입력 부위를 사용한다.
- 메모의 명시 사실과 추정/판정용 기대값이 섞이지 않도록 사실 위치를 검증한다.
- 출처/조건 주석 누락·해시 불일치·공식 자료 간 상충을 자동 확정하지 않는다.
- GENERAL 문서를 실제 제품의 농약 등록 적합성 근거로 바꾸지 않는다.

## 파일과 인터페이스

- Create `docs/verification/coaching-evidence-sources.json`: 기존/추가 자료의 출처·시각·해시·선정/제외 이유.
- Create `api/src/test/resources/verification/coaching-evidence-applicability.json`: 문단 ID별 조건과 본문 SHA-256. 원문 제한 인용과 위치, 각 필드 상태(EXPLICIT/NOT_STATED/UNRESTRICTED)를 저장한다. UNRESTRICTED는 검토 이유가 필요하다.
- Create `api/src/test/resources/verification/coaching-evidence-cases.json`: 개발/평가 구분, 고정 후보 ID, 실제 context, 명시 사실 주석, 별도 expected. 모든 사례는 synthetic 표기.
- Create `api/src/test/java/com/chamchamcham/lab/CoachingEvidenceSelector.java`: 선택 로직과 필요한 중첩 record/enum만 둔다. 메서드는 `static List<Decision> select(RecordFeedbackContext context, Map<String, Fact> explicitFacts, List<Document> candidates, Map<String, Applicability> rules)`이다. Decision은 documentId, status, reasons를 갖고 후보 순서를 보존한다.
- Create `api/src/test/java/com/chamchamcham/lab/CoachingEvidenceSelectorTest.java`: 조건 분류와 입력 사실/후보 무변경 회귀 검사.
- Create `api/src/test/java/com/chamchamcham/lab/CoachingEvidenceEvaluation.java`: JSON을 읽어 선택기를 실행하고 비교표/결과를 `.local/rag-api/evidence-selection/runs/<run>/`에 저장한다. static `evaluate(Path cases, Path rules, Path corpus, Path output)`가 진입점이며 사용한 파일 SHA-256을 출력에 포함한다.
- Create `scripts/coaching-evidence.py`와 `scripts/test-coaching-evidence.py`: 자료 검증(`check-sources`)과 모델/DB 없는 요약(`summary --run NAME`). 기존 결과 디렉터리 덮어쓰기와 허용 폴더 밖 경로는 거부한다.
- Modify `api/build.gradle.kts`: opt-in JavaExec `coachingEvidenceEvaluate`. test runtime classpath, workingDir=root, 최대 힙 256m, testClasses 의존. 기본 test에서 평가 실행을 시작하지 않는다.
- Create `docs/verification/coaching-evidence-results.md` 및 `coaching-evidence-lab.md`: 판정/실행법/한계/다음 단계.

### Task 1: 출처와 조건 목록

- [x] 보존한 manifest/chunks/원문 해시를 읽고 26개 문단의 확인 상태를 작성한다. 기존 파일은 수정하지 않는다.
- [x] 위 5개 Review Focus와 8개 작업의 근거 누락을 표시하고 공개 1차 자료를 조사한다. 원문은 무시 폴더에 저장하고 출처 목록에 식별값을 기록한다. 신규 DB 적재는 하지 않는다.
- [x] `scripts/test-coaching-evidence.py`에 기존/추가 출처의 해시 검증, 원문 경로 탈출 거부, 원문 누락, 같은 ID의 다른 본문 거부를 작성한다.
- [x] `python3 scripts/test-coaching-evidence.py`를 실행해 필요한 함수가 없어 실패함을 확인한다.
- [x] 도구의 `check_sources(manifest: Path, root: Path) -> dict`와 `read_summary(run: Path) -> dict`를 최소 구현한다. 비밀값/원문 전체를 로그에 출력하지 않는다.
- [x] 위 검사 통과와 `python3 scripts/coaching-evidence.py check-sources`의 확인/보류/실패 집계를 확인한다.
- [x] 조건 JSON의 각 EXPLICIT 제한을 원문과 대조하고 원문 간 상충은 조건부로 남긴다. 기존/추가 문단의 평가용 사본을 `.local/rag-api/evidence-selection/corpus.json`에 생성하여 문단 ID·출처·본문 SHA-256을 연결한다. 기존 corpus는 수정하지 않는다. 로컬 커밋 `test(rag): 근거 출처와 적용 조건을 보존하고 검증`.

### Task 2: test 전용 조건 선택기

- [x] SelectorTest에 다른 작물 제외, 품종 미상 조건부, 제한 없는 안내 유지, 명시 품종 충돌 제외, 실제 수확 부위 우선, 생육 조건 미상, 규칙 누락 조건부, 문헌 상충 조건부를 작성한다.
- [x] GENERAL 농약 원칙이 제품 사용 적합성을 보증하지 않는 사유를 유지하는 검사와, 인용 위치가 다른 Fact를 입력 사실로 사용하지 않는 검사를 추가한다. Fact는 원문의 필드 경로/문자열/위치와 검증된 값으로 구성하며 불일치 시 그 사실을 사용하지 않고 사유를 남긴다.
- [x] `./gradlew :api:test --tests '*CoachingEvidenceSelectorTest' --offline --console=plain --no-daemon --max-workers=1`로 미구현 실패를 확인한다.
- [x] select와 중첩 자료형을 최소 구현한다. 자유 문장으로부터 값이나 수치를 추론하지 않고 검증된 입력·조건만 비교한다. 후보 순서/본문/메타데이터는 변경하지 않는다. 판정 순서는 ID/본문 해시 불일치 시 평가 오류 중단 → 명시적 충돌 하나라도 있으면 EXCLUDED → 조건 미상/규칙 누락/미해결 문헌 상충이면 CONDITIONAL → 나머지 APPLICABLE이다. 해시 불일치와 충돌+미확인 동시 발생을 각각 검사한다.
- [x] 위 명령 통과와 빈 후보/동일 ID의 상충 자료/중복 호출의 결정성 검사를 확인한다. 중복 ID 상충은 실패로 보고하고 임의 선택하지 않는다.
- [x] 로컬 커밋 `test(rag): 고정 후보의 조건 충돌과 미확인 조건 분리`.

### Task 3: 평가 실행과 재현 결과

- [x] 기존 24/48 사례는 개발용으로 표시한다. 새 평가 목표 48개의 입력·후보·기대 근거·제외/보류 기준을 모델 출력 없이 고정한다. 후보 제공 방식은 기존 보존 후보 또는 사람이 구성한 통제 후보이며 사례마다 표시한다. 실제 검색 성능으로 집계하지 않는다.
- [x] Evaluation 테스트를 SelectorTest에 추가하여 expected가 select 인자로 전달되지 않음, 변경된 자료/코드 해시의 결과 재사용 거부, 기존 run 덮어쓰기 거부, 분모/미실행 집계를 검증한다.
- [x] 해당 테스트를 먼저 실패시킨 뒤 evaluate와 opt-in Gradle task를 구현한다. task는 프로젝트 속성 `evidenceRun`을 받아 `[a-z0-9][a-z0-9-]{0,63}`만 허용하고 결과 경로를 고정한다. 평가 입력은 전용 `evidence-selection/corpus.json`이고 원문 경로는 기존 `.local/rag-api/corpus/`와 새 `.local/rag-api/evidence-selection/sources/` 안으로 제한한다. 조건 주석과 후보 본문의 해시 일치를 먼저 확인한다.
- [x] Task 2의 명령과 Python 검사를 실행하여 통과를 확인한다.
- [x] `./gradlew :api:coachingEvidenceEvaluate -PevidenceRun=evidence-v1 --offline --console=plain --no-daemon --max-workers=1`를 실행한다. 외부/모델/DB 호출 없이 비교표와 해시를 남긴다.
- [x] 조건 충돌 제외 건수/대상, 일반 안내 유지 건수/대상, 조건 미상 처리, 후보에 기대 근거 부재, 미확보/미실행을 각각 공개한다. 동일 문단 반복은 독립 문서 수에 더하지 않는다. 결과를 보고 수정하면 평가 세트의 개발용 전환을 기록한다.
- [x] 로컬 커밋 `test(rag): 조건 선별의 재현 가능한 비교와 분모 기록`.

### Task 4: 자체 검토·전체 검사·사용법

- [x] 사용자가 검토할 대표 정상/오류/조건부 사례의 입력→후보→사유→출처를 설명한다. 근거 없는 수치나 문헌의 제한 해석을 정답으로 고정하지 않았는지 원문을 다시 대조한다.
- [x] `python3 scripts/test-coaching-evidence.py` 및 `./gradlew test --offline --console=plain --no-daemon --max-workers=1`을 실행하고 실패/스킵을 그대로 기록한다. 기존 opt-in DB 검사는 Docker 미기동 때문에 미실행이며 통과로 세지 않는다.
- [x] `git diff --check`와 변경 파일 범위를 확인한다. 제품 src/main·기존 corpus·기존 결과 변경이 없는지 확인한다.
- [x] 결과·사용법·연속 작업 기록을 작성한다. 채택 판단과 다음 실제 검색/답변/API 단계의 필요 승인을 명시한다. 도구 실행과 의미 판정을 분리하고 전문가 검증 부재를 공개한다.
- [x] 로컬 커밋 `docs(rag): 근거 조건 점검 결과와 재실행 방법 정리`. 원격 Git 작업은 하지 않는다.

## 계획 자체 점검

- 자료 조사, 원문 보존, 조건 분류, 일반 안내 유지, 신규 평가, 미실행 보고는 Task 1~4에 대응한다.
- 사용자 기록 필드 추가·실시간 웹 검색·자동 농약 판정·챗봇·모델 학습은 범위에 없다.
- 실제 DB 검색과 생성 품질 검증은 다음 큰 단위다. 이 단위의 통과를 최종 정확도 향상으로 주장하지 않는다.
- 첫 단위는 직접 순서대로 구현한다. 별도 병렬 구현은 필요 없다. 2026-11-23 사용자 승인을 받아 실행했으며 다음 큰 단위는 별도 승인한다.
- 읽기 전용 별도 설계 검토에서 추가 원문 경로, 판정 우선순위, 조건 주석과 후보 본문 동일성의 누락 3개를 지적받아 위 설계/작업에 반영했다. 이는 농업 내용 검증이나 구현 검사가 아니다.

## 완료 기록

실제 결과와 판단은 `docs/verification/coaching-evidence-results.md` 및 `coaching-evidence-evaluation.json`을 따른다. Python9/Java22/전체1439(1423pass,16skip,0fail). 최종 run은 evidence-final이며 기존48개 재검증이다. Docker/API/모델 기동, 제품 코드/DB 변경, 원격 Git 쓰기 없음.
