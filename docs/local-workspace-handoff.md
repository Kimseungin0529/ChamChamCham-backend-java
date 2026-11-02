# 로컬 작업 인계

## 정식 작업 공간

- 개인 GitHub: `https://github.com/Kimseungin0529/ChamChamCham-backend-java`
- 로컬 경로: `/Users/kimseungjin/develop/ChamChamCham-backend-java`
- 현재 작업 브랜치: `refactor/batch-java`
- 통합 대상: `dev`, 출시 대상: `main`
- `GodsMove`와 원본 `wingwogus/ChamChamCham`은 보관 대상이다. 이 작업의 원격·실행 대상에 포함하지 않는다.
- 이전 임시 복제본은 이후 작업 공간으로 사용하지 않는다.

## 완료한 변경

- 백엔드만 독립 저장소로 가져왔고 프런트엔드와 원본 배포 워크플로는 제외했다.
- 한글 공통 규칙과 AI 작업 절차를 작성했다.
- `batch` 시작점·스케줄러·테스트를 Java 21로 전환했다. 기존 cron·zone과 서비스 호출을 유지했다.
- 테스트 전용 CI를 추가했다. `main`·`dev` push 및 두 브랜치 대상 PR에서 전체 테스트를 실행한다.
- 기존 `main` 커밋 `5e48037`을 기준으로 `dev`를 만들고, `dev`에서 현재 작업 브랜치를 분기해 기존 두 변경을 가져왔다.
- 브랜치 규칙은 루트 `AGENTS.md`를 단일 기준으로 따른다.

## 이번 로컬 검증

2026-11-02 정식 로컬 폴더에서 실행했다.

- Java: OpenJDK 21.0.11, Gradle wrapper: 8.14.3.
- `./gradlew test :batch:bootJar --no-daemon`: 종료 코드 0, `BUILD SUCCESSFUL`.
- JUnit XML 합계: 총 1,165개, 실패 0개, 오류 0개, 건너뜀 1개.
- `git diff --check`: 통과.
- 독립 검토: 기존 batch 변경 보존, dev 분기 기준, 개인 origin, CI 범위에서 심각한 문제 없음.
- 기존 deprecated API·Gradle 경고는 남아 있다. 이번 전환에서는 관련 없는 코드를 변경하지 않았다.
- 실제 HTTP API 호출과 GitHub Actions 실행은 이번에 검증하지 않았다.
- 앱 프로젝트 등록과 새 채팅 전환은 사용자 폴더 선택을 기다린다.

## 앱 연결과 다음 채팅

Codex에 이 개인 폴더를 새 로컬 프로젝트로 등록하고, 이 폴더를 기본 폴더로 선택한다. 같은 경로를 IntelliJ에서도 연다. 기존 GodsMove 프로젝트의 폴더·Git 원격은 변경하지 않는다.

새 프로젝트 채팅의 첫 지시 예:

> 정식 작업 폴더는 /Users/kimseungjin/develop/ChamChamCham-backend-java다. AGENTS.md와 docs/local-workspace-handoff.md를 먼저 읽고 cwd, origin, 현재 브랜치, 미커밋 변경을 확인해. 현재 브랜치는 refactor/batch-java이고 통합 대상은 dev다. 남은 작업은 먼저 구체적인 변경 범위와 검증 계획을 제시하고, 조율·승인 후 수행해.

## 남은 개발 작업

1. `refactor/batch-java`에서 `dev` 대상 Draft PR을 만들고 실제 CI 실행 결과를 확인한다. 이번 로컬 전환에서는 PR 생성·병합을 하지 않는다.
2. `domain` → `application` → `api` 순서로 기능 묶음별 Java 전환을 계획·검증한다. 현재 Kotlin 파일은 각각 141·277·155개이며 테스트를 포함한다.
3. 개인 로컬 API 실호출 환경을 구성하고 HTTP·DB·Redis 계약을 검증한다.
4. 모든 소스·테스트 전환 후 Kotlin 플러그인·런타임을 정리한다.
5. 실제 개발·배포 서버와 브랜치 연결, 브랜치 보호 규칙은 별도로 구성한다. 현재 main/dev 생성만으로 서버가 배포되지는 않는다.

전환 작업의 상세 기준은 `docs/java-migration-plan.md`, AI 절차는 `docs/ai-workflow.md`를 따른다. 새 기능 구현은 이번 작업 공간 전환에 포함하지 않는다.
