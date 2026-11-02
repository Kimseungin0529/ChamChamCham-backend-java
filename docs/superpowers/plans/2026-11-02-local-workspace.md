# 개인 로컬 작업 공간 전환 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 개인 GitHub 코드를 로컬 작업 공간으로 가져오고 승인된 브랜치 규칙을 적용한다.

**Architecture:** 기존 개인 로컬 복제본을 재사용한다. 기존 main에서 dev를 만들고 dev 기반 refactor/batch-java로 검증된 두 커밋을 가져온다. GodsMove는 보관하고 새 Codex 프로젝트는 개인 폴더에 연결한다.

**Tech Stack:** Git, Java 21, Gradle wrapper, GitHub Actions, Codex desktop.

**Spec:** 이 채팅에서 승인한 로컬 전환 계획과 main/dev 및 type/task 브랜치 규칙.

## Global Constraints

- 개인 원격: https://github.com/Kimseungin0529/ChamChamCham-backend-java.git
- 로컬: /Users/kimseungjin/develop/ChamChamCham-backend-java
- main은 배포 서버용, dev는 개발 서버용이다. 서버 연결 자체는 이번 범위가 아니다.
- 모든 작업은 dev에서 feat, fix, refactor, chore 접두사로 분기하고 dev로 PR을 연다.
- 원본 저장소·GodsMove·운영 환경을 변경하지 않는다.

## Review Focus

- main을 변경하지 않고 dev가 기존 main 기준에서 시작했는지 Git 이력으로 확인한다.
- refactor/batch-java가 dev를 포함하고 기존 두 변경이 보존됐는지 확인한다.
- CI는 main/dev push와 두 브랜치 대상 PR의 테스트만 실행하는지 확인한다.
- 앱 프로젝트 등록과 현재 채팅의 접근 권한 변경을 혼동하지 않는다. 사용자 폴더 선택이 필요하면 정확히 안내한다.

### Task 1: 로컬 동기화와 브랜치

- [x] 개인 origin을 확인하고 fetch한다.
- [x] main 기준 dev를 생성하고 개인 원격에 올린다.
- [x] dev에서 refactor/batch-java를 생성하고 기존 batch/CI 커밋을 가져온다.

### Task 2: 규칙과 검증

- [x] AGENTS.md와 README.md에 승인된 브랜치 흐름을 기록한다.
- [x] .github/workflows/backend-tests.yml에 dev와 PR 대상 브랜치를 추가한다.
- [x] ./gradlew test :batch:bootJar --no-daemon으로 로컬 기준선을 검증한다.
- [x] docs/local-workspace-handoff.md에 실제 결과와 다음 작업을 남긴다.
- [x] 변경을 검토·커밋하고 개인 작업 브랜치에 올린다.

### Task 3: Codex 프로젝트 전환

- [ ] 사용자가 앱에서 개인 폴더를 새 로컬 프로젝트로 등록한다.
- [ ] 새 프로젝트의 채팅에서 인계 문서를 읽고 작업 경로·브랜치를 확인한다.
