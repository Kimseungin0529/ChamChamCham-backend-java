# Batch 모듈 Java 전환 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `batch` 모듈의 애플리케이션 시작점, 정책 동기화 스케줄러, 테스트를 Java 21로 옮기고 기존 동작을 유지한다.

**Architecture:** 기존 `application`의 Kotlin 서비스는 그대로 호출한다. `batch`는 Java 소스만 컴파일하도록 정리하고 동일한 Spring 애너테이션·스케줄 설정을 유지한다.

**Tech Stack:** Java 21, Spring Boot 3.5.4, Gradle Kotlin DSL, JUnit 5, Mockito.

**Spec:** `docs/java-migration-plan.md`

## Global Constraints

- Git 원격의 쓰기 대상은 `Kimseungin0529/ChamChamCham-backend-java` 하나다.
- 원본 운영 서버와 API에는 연결하지 않는다.
- `batch -> application -> domain` 의존 방향과 기존 패키지·클래스명을 유지한다.
- 정책 동기화 cron 기본값 `0 20 3 * * *`, 시간대 기본값 `Asia/Seoul`을 유지한다.
- 전체 테스트 `./gradlew test`와 `./gradlew :batch:bootJar`로 검증한다.

## Review Focus

- 스케줄러가 한 번 실행될 때 `PolicySyncService.runScheduledSync()`를 정확히 한 번 호출하는가? → Task 1의 Java 테스트.
- `@Scheduled`의 cron·zone 설정 문자열이 유지되는가? → Task 1의 변경 검토.
- 애플리케이션에 `@EnableScheduling`이 유지되는가? → Task 1의 변경 검토와 `:batch:bootJar`.
- Kotlin `JobSummary`의 Java getter를 올바르게 호출하는가? → `compileJava`와 테스트.
- Spring Boot가 Java `main`을 실행 진입점으로 인식하는가? → `:batch:bootJar`.

---

### Task 1: Batch 소스와 테스트 전환

**Files:**
- Create: `batch/src/main/java/com/chamchamcham/BatchApplication.java`
- Create: `batch/src/main/java/com/chamchamcham/batch/policy/PolicySyncScheduler.java`
- Create: `batch/src/test/java/com/chamchamcham/batch/policy/PolicySyncSchedulerTest.java`
- Delete: 위 파일에 대응하는 `batch/src/main/kotlin`·`batch/src/test/kotlin`의 세 Kotlin 파일
- Modify: `batch/build.gradle.kts`

**Interfaces:**
- Consumes: `PolicySyncService.runScheduledSync(): PolicySyncResult.JobSummary`
- Produces: `BatchApplication.main(String[])`, `PolicySyncScheduler(PolicySyncService)`, `syncNongupEzPolicies(): void`

- [x] Java 테스트를 작성한다. 스케줄러의 서비스 호출 1회를 확인한다.
- [x] 대응 Kotlin 파일을 제거하고 `./gradlew :batch:test`를 실행해 클래스 부재로 실패하는 것을 확인한다.
- [x] 동일한 패키지·이름·애너테이션·로그 항목을 가진 Java 클래스를 최소한으로 구현한다.
- [x] `./gradlew :batch:test`를 실행해 통과를 확인한다.
- [x] `batch/build.gradle.kts`에서 이 모듈에만 필요한 Java·Spring 의존성만 남기고 Kotlin 플러그인·직접 의존성을 제거한다.
- [x] `./gradlew test`와 `./gradlew :batch:bootJar`를 실행하고 `batch/src`의 `*.kt`가 0개인지 확인한다.
- [x] 변경 내용을 검토한 뒤 `refactor(batch): 스케줄러와 시작점을 Java로 전환`으로 커밋한다.
