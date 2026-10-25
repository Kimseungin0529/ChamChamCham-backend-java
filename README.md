# ChamChamCham 백엔드 Java 전환

ChamChamCham의 백엔드만 별도 개인 저장소에서 관리한다. 프런트엔드와 원본 저장소의 배포 자동화는 포함하지 않는다.

## 가져온 기준

- 출처: `wingwogus/ChamChamCham`의 `main`
- 기준 커밋: `7c58801a2ae070bdd5733c47b9396a0a3f820986`
- 가져온 범위: 원본의 `backend/` 전체를 이 저장소의 루트로 배치
- 이 저장소는 GitHub 포크가 아니며 원본 저장소로 푸시하지 않는다.

현재 코드는 Kotlin 원본을 가져온 기준 상태다. Java 21 전환은 [전환 계획](docs/java-migration-plan.md)에 따라 진행한다. `*.gradle.kts`는 빌드 스크립트이며 애플리케이션 소스 전환과 별도로 판단한다.

가져온 코드의 첫 전체 테스트에서는 378개 중 1개가 실패하고 1개가 건너뛰어졌다. `AuthControllerValidationTest`의 `experienceLevel` 오류 필드명 기대값과 실제 응답(`experienceLevelWithinAge`)이 다르다. Java 전환의 기준선을 만들기 전에 별도 변경으로 원인을 확인한다.

## 구성

```text
api          HTTP API 및 보안
application  유스케이스와 외부 연동
domain       도메인 모델과 저장소
batch        예약·일괄 작업
```

## 로컬 검증

```bash
./gradlew test
./gradlew :api:bootRun
```

실행 전 로컬 DB·Redis·외부 서비스 설정을 확인한다. 원본 운영 서버, 운영 데이터베이스, 운영 API를 시험 대상으로 사용하지 않는다. 작업 규칙은 [AGENTS.md](AGENTS.md), 단계별 절차는 [AI 개발 절차](docs/ai-workflow.md)에 있다.
