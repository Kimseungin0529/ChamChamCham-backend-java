# ChamChamCham 백엔드 Java 전환

ChamChamCham의 백엔드만 별도 개인 저장소에서 관리한다. 프런트엔드와 원본 저장소의 배포 자동화는 포함하지 않는다.

## 가져온 기준

- 출처: `wingwogus/ChamChamCham`의 `main`
- 기준 커밋: `7c58801a2ae070bdd5733c47b9396a0a3f820986`
- 가져온 범위: 원본의 `backend/` 전체를 이 저장소의 루트로 배치
- 이 저장소는 GitHub 포크가 아니며 원본 저장소로 푸시하지 않는다.

현재는 Kotlin과 Java가 공존한다. `batch` 모듈의 시작점·스케줄러·테스트를 Java 21로 옮겼고 나머지 모듈은 [전환 계획](docs/java-migration-plan.md)에 따라 진행한다. `*.gradle.kts`는 빌드 스크립트이며 애플리케이션 소스 전환과 별도로 판단한다.

가져온 코드의 첫 전체 테스트에서 `experienceLevel` 범위 검증과 나이 검증이 동시에 실패해 응답 필드명이 흔들리는 문제가 발견되었다. 이 검증 중복을 별도 변경으로 고쳤다. `batch` Java 전환 후 `./gradlew test` 전체 실행도 통과했다.

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
