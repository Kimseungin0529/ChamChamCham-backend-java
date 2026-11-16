# 근거 부족 코칭 정책 실험 사용법

이 실험은 **기록·문서가 같을 때 답변 정책의 차이**를 비교한다. 실제 DB 저장·인증·검색 API를 다시 검사하는 도구가 아니다. 기존 로컬 API와 Docker는 시작하지 않는다. 공식 문서/DB/모델을 삭제하지 않는다.

## 입력과 두 정책

`api/src/test/resources/verification/coaching-policy-cases.json`에 조정용 24건과 새 평가용 48건이 있다. 기존 24건은 실제 API 검증의 문맥과 검색 문단을 복사했다. 새 48건은 직접 작성한 합성 문맥과 선정 문단이다. 작물 3개×작업 8개×2상황이며 실제 농가 분포를 대표하지 않는다. 일부 상황에는 메모와 상세의 충돌이 의도적으로 있다. 실제 API가 이런 요청을 접수한다는 증거는 아니다.

기존 정책은 제품 `RecordFeedbackPromptBuilder`와 로컬 실습의 최종 생성 스키마를 사용한다. 실험 정책은 칭찬을 생략하고 질문하거나 답변을 보류할 수 있다. 제품 프롬프트나 응답 DTO는 바뀌지 않는다. Spring AI의 응답 변환 지시와 API 전체를 재생하는 대신 Ollama에 두 방식의 같은 사용자 문맥과 각 정책/스키마를 직접 보낸다. 정책과 스키마를 함께 바꾼 비교이며 한 문구의 효과나 이전 API 점수와의 직접 비교가 아니다.

## 실행

저장소 루트에서 기존 Gradle 캐시와 보존된 macOS Ollama가 있어야 한다. 모델 다운로드 명령은 없다.

```bash
./gradlew :api:coachingPolicyExport --offline --console=plain --no-daemon --max-workers=1
python3 scripts/coaching-policy.py start-model
python3 scripts/coaching-policy.py run --split dev --run my-policy-dev-v1
python3 scripts/coaching-policy.py summary --run my-policy-dev-v1
```

내보내기는 Spring을 시작하지 않고 실제 제품 빌더로 `.local/rag-api/policy/prepared.json`을 만든다. `start-model`은 기존 앱의 서명·공증과 모델 manifest를 확인하고 127.0.0.1:11437만 사용한다. 기존 Docker 의존 도구의 `native-up`을 호출하지 않는다.

- 모델: 기존 qwen3:4b-instruct Q4_K_M. temperature=0, seed=42, 문맥8192, 최대출력1024, 요청240초, 직렬 실행.
- 각 사례의 baseline/candidate 순서를 번갈아 실행한다.
- 완료 결과는 재실행하지 않는다. 실패도 완료된 시도로 보존한다.
- 실행 중단으로 완료 여부가 불분명한 요청은 자동 재전송하지 않는다. 확인 후 `--retry-incomplete`로 재시도하면 기존 파일을 interrupted 기록으로 보존한다. 재시도를 독립 사례에 더하지 않는다.
- 사례/프롬프트/소스/모델/도구 식별값이 바뀌면 기존 실행명에 이어 쓸 수 없다. 새 이름을 사용한다.

## 의미 판정과 평가 잠금

`VALID`는 답변 내용이 맞다는 뜻이 아니다. [판정 기준](coaching-policy-rubric.json)으로 기록과 문헌을 직접 대조한다. 결과 폴더의 `reviews.json`은 `사례ID-baseline` 또는 `사례ID-candidate`를 key로 하며 `label`, `reason`, `flags`, `evidence`, `responseSha256`를 남긴다. 응답 해시가 다르면 집계하지 않는다. 검토하지 않은 답변은 성공으로 세지 않는다.

개발 24건의 양쪽 실행과 형식 통과 답변의 자체 검토가 끝나야 평가용 실행을 허용한다.

```bash
python3 scripts/coaching-policy.py run --split evaluation --run my-policy-held-v1 --development-run my-policy-dev-v1
python3 scripts/coaching-policy.py summary --run my-policy-held-v1
```

`--case 사례ID` 또는 `--limit N`으로 일부부터 실행할 수 있다. 새48건의 기대 기준은 출력 확인 전에 고정했지만, 같은 작성자가 기존 실패 유형을 알고 작성했다. 무작위 독립 표본이나 외부 전문가 평가로 해석하지 않는다. 평가를 보고 후보를 고치면 해당 사례는 조정용으로 전환해야 한다.

## 보존과 자원

실행 결과는 `.local/rag-api/policy/runs/<실행명>/`에, 기존 DB 백업·문서·모델은 원래 위치에 남는다. 이 도구는 DB/Redis/Docker를 호출하지 않는다. 모델은 요청 후 `keep_alive=0s`로 메모리에서 내리며 파일은 유지한다. 서버 프로세스는 유지한다. 가중치는 요청 후 메모리에서 내리므로 다음 실행에도 보존한 모델 파일을 다시 사용할 수 있다. 별도 서버 종료가 필요하면 실행 중인 요청이 없는지 먼저 확인해야 한다.

민감한 실제 기록을 사례에 넣지 않는다. 현재 파일은 가상 주소·가상 제품·합성 기록만 담는다. 로컬 실험 결과를 원래 팀 서비스 성능, 현장 효과, 농업 안전성 인증으로 설명하지 않는다.
