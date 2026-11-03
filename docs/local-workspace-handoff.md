# 로컬 작업 인계

## 정식 작업 공간

- 개인 GitHub: `https://github.com/Kimseungin0529/ChamChamCham-backend-java`
- 로컬 경로: `/Users/kimseungjin/develop/ChamChamCham-backend-java`
- 현재 정책 도메인 전환 브랜치: `refactor/domain-policy-java`, 분기 기준은 `dev`의 `529dc03`이다. 현재 작업은 Draft PR·CI 확인까지 승인됐으며 병합은 별도 승인이다.
- 통합 대상: `dev`, 출시 대상: `main`
- `GodsMove`와 원본 `wingwogus/ChamChamCham`은 보관 대상이다. 이 작업의 원격·실행 대상에 포함하지 않는다.
- 이전 임시 복제본은 이후 작업 공간으로 사용하지 않는다.

## 완료한 변경

- 백엔드만 독립 저장소로 가져왔고 프런트엔드와 원본 배포 워크플로는 제외했다.
- 한글 공통 규칙과 AI 작업 절차를 작성했다.
- `batch` 시작점·스케줄러·테스트를 Java 21로 전환했다. 기존 cron·zone과 서비스 호출을 유지했다.
- 테스트 전용 CI를 추가했다. `main`·`dev` push 및 두 브랜치 대상 PR에서 전체 테스트를 실행한다.
- 기존 `main` 커밋 `5e48037`을 기준으로 `dev`를 만들고, `dev`에서 현재 작업 브랜치를 분기해 기존 두 변경을 가져왔다.
- 브랜치 규칙과 계획·사용자 승인 절차는 루트 `AGENTS.md`를 단일 기준으로 따른다.
- [PR #1](https://github.com/Kimseungin0529/ChamChamCham-backend-java/pull/1)(batch), [PR #2](https://github.com/Kimseungin0529/ChamChamCham-backend-java/pull/2)(정책 enum 4개), [PR #3](https://github.com/Kimseungin0529/ChamChamCham-backend-java/pull/3)(정책 동기화 작업 엔티티·저장소)는 모두 `dev`에 병합됐다. 2026-11-03 GitHub 상태를 재조회했다.

## 최신 정책 도메인 전환 검증

2026-11-03 `refactor/domain-policy-java`의 코드 커밋 `8c6157a`에서 실행했다. 이후 문서 커밋에는 코드 변경이 없다.

- `domain.policy`의 남은 Kotlin 주 소스 8개와 테스트 4개를 Java로 전환했다. 엔티티·저장소·검색·추천 페이지 조회를 역할별 커밋으로 분리했다.
- 새 테스트는 기존 Kotlin 구현에서 기준선을 먼저 확인했다. 필요한 상위 호출 문법 수정은 `application` 7개 파일과 API 검색 통합 테스트 1개 파일로 제한했다.
- `./gradlew :domain:test --tests 'com.chamchamcham.domain.policy.*' --no-daemon`: 최초 기준선 성공. 각 단계에서 해당 도메인 및 정책 동기화·추천·검색 서비스·HTTP 경계를 추가 검증했다.
- `./gradlew clean test :batch:bootJar --no-daemon`: 종료 코드 0, `BUILD SUCCESSFUL in 1m 33s`, 32개 task 모두 실행.
- JUnit XML 합계: 총 1,212개, 실패 0개, 오류 0개, 건너뜀 1개. `domain` 164개, `application` 666개, `api` 381개, `batch` 1개다.
- 건너뛴 검사는 `RecordFeedbackGenerationVectorStoreSmokeTest`의 실제 벡터 저장소 smoke test다. 별도 로컬 RAG 환경을 활성화하지 않아 기존 조건대로 건너뛰었다.
- Java 클래스 major version 65와 batch JAR의 `Start-Class: com.chamchamcham.BatchApplication`, `Build-Jdk-Spec: 21`을 확인했다.
- JPA 엔티티 필드 37개의 애너테이션·순서와 JPA 저장소 JPQL 6개가 기준 코드와 일치한다. `git diff --check`도 통과했다.
- 독립 검토 범위 `529dc03` → `8c6157a`: Critical·Important·Minor 발견 사항 없음. 검토자는 최종 테스트 실행·운영 DB·문서 변경을 검증하지 않았으며 전체 테스트 결과는 위 로컬 실행 결과다.
- 미실행: 실제 PostgreSQL·Redis·로컬 HTTP 서버·외부 제공자 실호출. H2와 서비스·MVC 테스트로 검증했으며 실제 환경 검증은 별도 계획·승인이 필요하다.
- 기존 `BaseTimeEntity` 접근자와 `Member`의 Kotlin `final` 제약으로 JPA 프록시가 제한된다. Java 엔티티에서는 `BaseTimeEntity#getCreatedAt` 때문에 `HHH000305` 경고가 발생한다. 매핑·기존 동작을 보존했으며 공통 기반 클래스 변경은 이번 범위에 포함하지 않았다. 기존 deprecated API·Gradle 경고도 남는다.

## 이전 batch 로컬 검증

2026-11-02 정식 로컬 폴더에서 실행했다.

- Java: OpenJDK 21.0.11, Gradle wrapper: 8.14.3.
- `./gradlew test :batch:bootJar --no-daemon`: 종료 코드 0, `BUILD SUCCESSFUL`.
- JUnit XML 합계: 총 1,165개, 실패 0개, 오류 0개, 건너뜀 1개.
- `git diff --check`: 통과.
- 독립 검토: 기존 batch 변경 보존, dev 분기 기준, 개인 origin, CI 범위에서 심각한 문제 없음.
- 기존 deprecated API·Gradle 경고는 남아 있다. 이번 전환에서는 관련 없는 코드를 변경하지 않았다.
- 실제 HTTP API 호출은 검증하지 않았다. 후속 GitHub Actions 결과는 아래에 기록한다.
- 현재 채팅의 작업 경로가 정식 로컬 폴더임을 확인했다. 앱 프로젝트 등록 여부는 별도 UI 설정이다.

## 후속 검증과 GitHub 상태

- [PR #1](https://github.com/Kimseungin0529/ChamChamCham-backend-java/pull/1)을 `refactor/batch-java` → `dev` 대상으로 Draft 생성했다. 이후 Draft 해제·병합 여부는 PR의 현재 상태를 확인한다.
- 커밋 `de258f43812dc87f1d4e63065adf95ca9da54b0c`에서 `./gradlew :batch:test --no-daemon`과 `./gradlew test :batch:bootJar --no-daemon --rerun-tasks`를 실행해 종료 코드 0을 확인했다. 전체 JUnit 결과는 1,165개, 실패·오류 0개, 건너뜀 1개다.
- [Backend tests 실행 #41426198451](https://github.com/Kimseungin0529/ChamChamCham-backend-java/actions/runs/41426198451)이 성공했다. CI 로그는 `BUILD SUCCESSFUL`, 23개 task 실행이며 job 소요 시간은 8분 33초다.
- 위 CI 결과는 해당 커밋의 결과다. 문서 추가 등으로 PR head가 변경되면 최신 커밋의 CI 성공을 다시 확인하고 병합한다. 병합 후에는 `dev` push의 CI도 확인한다.
- 저장소를 공개로 전환했고, 로그인 없이 PR·CI URL이 HTTP 200으로 열리는 것을 확인했다.
- 공개 전 커밋 5개·고유 파일 내용 641개를 검사해 인식 가능한 실토큰·개인키와 자격 증명 파일이 발견되지 않았다. 고정 JWT는 사용자 확인상 로컬 전용이며 개발·배포 JWT는 각각 `DEV_JWT_SECRET`·`PROD_JWT_SECRET` 환경변수를 사용한다. 이 검사는 모든 보안 문제의 부재를 보증하지 않는다.

## 앱 연결과 다음 채팅

Codex에 이 개인 폴더를 새 로컬 프로젝트로 등록하고, 이 폴더를 기본 폴더로 선택한다. 같은 경로를 IntelliJ에서도 연다. 기존 GodsMove 프로젝트의 폴더·Git 원격은 변경하지 않는다.

새 프로젝트 채팅의 첫 지시 예:

> 정식 작업 폴더는 /Users/kimseungjin/develop/ChamChamCham-backend-java다. AGENTS.md와 docs/local-workspace-handoff.md를 먼저 읽고 cwd, origin, 현재 브랜치, 미커밋 변경을 확인해. refactor/domain-policy-java의 PR·최신 CI·dev 병합 여부를 확인해. 다음 작업은 최신 dev에서 새 작업 브랜치로 진행하고, 먼저 영역·파일 범위·검증·커밋·push·Draft PR·병합 여부를 포함한 계획을 제시해. 내 승인 후 수행해.

## 남은 개발 작업

1. 현재 정책 도메인 전환의 최신 Draft PR·CI 결과를 확인한다. 병합 승인 후 `dev` CI를 확인하고 다음 브랜치를 만든다.
2. `domain` → `application` → `api` 순서로 기능 묶음별 Java 전환을 계획·검증한다. 시범 브랜치 기준 남은 Kotlin 파일은 각각 122·277·155개(총 554개)이며 테스트를 포함한다. 현재 `dev`와 병합 전 시범 브랜치의 수를 구분한다.
3. 개인 로컬 API 실호출 환경을 구성하고 HTTP·DB·Redis 계약을 검증한다.
4. 모든 소스·테스트 전환 후 Kotlin 플러그인·런타임을 정리한다.
5. 실제 개발·배포 서버와 브랜치 연결, 브랜치 보호 규칙은 별도로 구성한다. 현재 main/dev 생성만으로 서버가 배포되지는 않는다.

전환 작업의 상세 기준은 `docs/java-migration-plan.md`, AI 절차는 `docs/ai-workflow.md`를 따른다. 새 기능 구현은 이번 작업 공간 전환에 포함하지 않는다.

남은 전환은 이번 시범을 포함해 약 11개 PR을 목표로 묶되 정확한 후속 범위는 각각 승인받는다. 커밋은 PR 안에서도 엔티티·저장소·검색·추천 페이지·문서처럼 역할을 나누고 대상과 변경 동작을 제목에 적는다.
