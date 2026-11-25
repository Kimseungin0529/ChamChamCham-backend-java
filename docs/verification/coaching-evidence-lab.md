# 기록 코칭 근거 조건 실험 사용법

이 도구는 문단의 적용 조건을 비교한다. Docker/API/모델을 시작하지 않으며 DB에 저장하거나 답변을 생성하지 않는다. 결과의 의미와 한계는 [결과 보고서](coaching-evidence-results.md)에 있다.

## 1. 보존 위치

- 기존 원문/선택 본문: `.local/rag-api/corpus/` — 읽기만 한다.
- 추가 원문/선택 본문: `.local/rag-api/evidence-selection/sources/`.
- 평가용 문단 사본: `.local/rag-api/evidence-selection/corpus.json`.
- 원시 결과: `.local/rag-api/evidence-selection/runs/<run>/result.json`, `comparison.csv`, `identity.json`.
- 출처/해시: `docs/verification/coaching-evidence-sources.json`.
- 문단 조건: `api/src/test/resources/verification/coaching-evidence-applicability.json`.
- 합성 상황: `api/src/test/resources/verification/coaching-evidence-cases.json`.

DB/벡터/모델은 이전 실습 위치에 그대로 있다. 이 실험을 위해 재초기화·재적재·모델 다운로드할 필요가 없다. `.local`은 Git 제외 폴더이므로 **Git clone만으로 원문이 복원되지는 않는다.** 다른 컴퓨터로 옮길 때는 위 실습 폴더를 함께 보존하고 해시를 확인한다. 원문 9개는 평소 다시 다운로드하지 않는다.

## 2. 자료 확인과 결과 읽기

저장소 루트에서 실행한다.

```bash
python3 scripts/coaching-evidence.py check-sources
python3 scripts/coaching-evidence.py summary --run evidence-final
```

확인 명령은 원문/선택 본문·기존 corpus의 해시, 문단 ID/본문 해시/출처/인용 위치를 대조한다. 조건의 농업적 의미를 자동 인증하는 검사는 아니다. 자료가 없거나 다르면 덮어쓰지 않고 실패한다.

요약 명령은 결과 본문과 입력/실행 코드 해시가 같은지 확인한다. 코드나 자료를 바꾼 뒤 예전 결과를 현재 결과처럼 읽으려 하면 거부한다. 과거 원시 JSON은 보존되며 읽기 위해 검사 기능을 해제하지 않는다. 해당 결과의 커밋/해시를 대조하거나 새 이름으로 다시 실행한다.

## 3. 직접 다시 실행

JDK21/Python3와 이미 보존된 Gradle 의존성을 사용한다. 새 이름을 지정한다.

```bash
./gradlew :api:coachingEvidenceEvaluate -PevidenceRun=my-evidence-01 --offline --console=plain --no-daemon --max-workers=1
python3 scripts/coaching-evidence.py summary --run my-evidence-01
```

run 이름은 영문 소문자/숫자로 시작하고 하이픈을 포함하여 최대64자다. 이미 존재하는 성공/실패 폴더는 덮어쓰지 않는다. 실패는 `failure.json`에 남으며 새 이름으로 원인을 해결한 뒤 실행한다. 재실행은 동일 48개 조건 검사의 반복이며 새로운 독립 성능 자료가 아니다.

평가기의 `expected`, `requiredEvidenceIds`, `uncoveredNeed`는 채점·한계 기록이다. 선택기에는 전달하지 않는다. 직접 사례를 추가할 때 입력·후보·기대값을 결과 보기 전에 작성한다. 자연어 메모 사실 주석은 사람이 확정적 진술인지 확인한 뒤 정확한 위치/동일 문자열 값으로 넣는다. 불확실한 말에서 단어만 추출해 사실로 주석하면 안 된다. 이 도구는 부정/추측/대명사/복합 질문을 자동 해석하지 않는다.

## 4. 검사

```bash
python3 scripts/test-coaching-evidence.py
./gradlew :api:test --tests '*CoachingEvidenceSelectorTest' --offline --console=plain --no-daemon --max-workers=1
./gradlew test --offline --console=plain --no-daemon --max-workers=1
```

기본 루트 테스트는 Java 단위/기존 테스트만 실행한다. 실제 비교는 명시적 JavaExec task를 호출해야 한다. 별도 DB/벡터 환경이 필요한 기존 opt-in 검사의 스킵은 실제 실행 성공으로 세지 않는다.

이번 결과로 실제 코칭을 확인하려고 API를 켜거나 기존 DB를 변경하지 않는다. 실제 검색·답변 개선과 API 재검증은 다음 승인 작업 단위다.
