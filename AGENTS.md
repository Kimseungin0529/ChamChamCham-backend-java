# ChamChamCham 백엔드 작업 규칙

이 저장소는 ChamChamCham 백엔드를 독립적으로 관리하고 Java 21로 전환하기 위한 개인 저장소다. 가져온 기준은 `wingwogus/ChamChamCham`의 `main` 커밋 `7c58801a2ae070bdd5733c47b9396a0a3f820986`이다. 가져온 소스는 현재 Kotlin이며 Java 전환은 검증하면서 진행한다.

## 저장소와 외부 환경 분리

- Git 원격 저장소의 쓰기 대상은 이 개인 저장소만 허용한다. 원본을 `origin`이나 `upstream`으로 등록하거나 원본으로 푸시·PR을 만들지 않는다.
- 원본의 운영 서버, 데이터베이스, Docker 이미지, 배포 계정에 연결하지 않는다. 기존 설정에 주소가 남았다면 실행 전에 개인 환경 값으로 교체한다.
- API 실호출 검증은 로컬 서버 또는 사용자가 소유한 별도 테스트 환경에서만 한다. 외부 제공자 호출은 테스트용 계정과 설정을 확인한 뒤 진행한다.
- 비밀키, 토큰, 자격 증명, 개인 정보는 커밋과 로그에 남기지 않는다.
- 사용자 변경을 되돌리지 않는다. 요청과 직접 관련된 파일만 바꾼다.

## 모듈과 의존 방향

```text
api -> application -> domain
batch -> application -> domain
```

- `api`: HTTP 컨트롤러, 요청·응답 DTO, 보안 필터, 예외 처리, OpenAPI 등 전송 계층.
- `application`: 유스케이스, 트랜잭션, `*Command`·`*Result`, 비즈니스 예외, 포트, 토큰·이메일·Redis 연동.
- `domain`: 엔티티, 도메인 저장소, enum, 핵심 비즈니스 개념.
- `batch`: 예약·일괄 작업의 진입점. 비즈니스 규칙은 `application` 서비스를 호출해 재사용한다.
- `domain`에서 `application`이나 `api`를 참조하지 않고, `application`에서 `api`를 참조하지 않는다.
- 기본 패키지 `com.chamchamcham`과 기능별 패키지 구조를 유지한다. 도메인 명칭은 `member`를 사용하고 프로젝트 고유의 `user` 명칭을 새로 만들지 않는다.
- 구체적인 필요가 없는 의존성·추상화·설정은 추가하지 않는다.

## 변경 순서와 검증

1. 현재 동작과 호출 경계를 확인한다. 요구가 불명확하면 가정과 선택지를 먼저 밝힌다.
2. 동작을 바꾸거나 리팩터링한다면 빠진 회귀 테스트를 먼저 보강한다.
3. 기능과 API 계약을 유지하는 최소 변경을 한다. 주변 코드는 함께 정리하지 않는다.
4. 해당 모듈 테스트를 실행하고 최종적으로 루트에서 `./gradlew test`를 실행한다. 생략한 검사는 이유와 위험을 기록한다.
5. 커밋은 작은 단위로 나누고 `type(scope): 제목` 형식을 사용한다.

테스트 위치:

- 애플리케이션 비즈니스 규칙: `application/src/test`
- 컨트롤러 입력 검증·HTTP 계약: `api/src/test`
- 보안 필터·인증 동작: API 통합 또는 MVC 테스트

## Java 전환

- 새로운 백엔드 구현과 전환된 파일은 Java 21을 사용한다. 기존 Kotlin 파일은 대응하는 Java 코드와 테스트가 확인된 뒤 제거한다.
- API 경로, JSON 필드명, 상태 코드, 쿠키·헤더, 오류 코드, DB 테이블·열, 트랜잭션 경계의 의미를 유지한다. 의도적인 계약 변경은 별도 작업으로 분리한다.
- DTO·명령·결과에는 필요한 경우 Java `record`를 사용할 수 있다. JPA 엔티티는 프록시·생성자 요구를 고려해 일반 클래스로 작성한다.
- Kotlin의 null 허용성, 기본 인자, 확장 함수, `data class` 동등성, JPA 지연 로딩을 기계적으로 치환하지 않는다.
- 모든 모듈의 Java 소스·테스트 전환이 확인되기 전에는 Kotlin 플러그인과 런타임을 제거하지 않는다. Gradle Kotlin DSL(`*.gradle.kts`) 전환은 애플리케이션 코드 전환과 별개의 결정이다.

## API 경계와 계층 규칙

- 요청·응답 DTO는 `api.<feature>.dto`에 두고, 유스케이스 입출력은 `application`의 `*Command`·`*Result`로 둔다.
- 컨트롤러에서 요청 DTO를 명령으로 변환하고 애플리케이션 서비스를 호출한다. 엔티티를 API 응답으로 직접 노출하지 않는다.
- null, 공백, 형식, 범위, 컬렉션 크기·중복 등 요청 형식 검증은 API DTO에 두고 `@Valid`로 실행한다. 실제 비HTTP 호출자가 동일한 검증을 필요로 하기 전에는 서비스에 중복하지 않는다.
- 서비스에서는 소유권, 상태 전이, 저장소 조회 등 도메인 상태에 의존하는 규칙을 검증한다.
- 컨트롤러는 HTTP 상태·헤더·쿠키·`ApiResponse`를 결정한다. 비즈니스 규칙, 저장소 접근, 비밀번호 인코딩, 토큰 발급, 트랜잭션 로직을 넣지 않는다.
- 서비스는 생성자 주입을 사용하고 트랜잭션 유스케이스는 `@Service`에 둔다. 예상 가능한 실패는 `BusinessException(ErrorCode.X)`로 표현한다. 서비스에서 `ResponseEntity`, `ApiResponse`, 서블릿 타입, API DTO를 반환하지 않는다.
- 성공 응답은 기존 `ApiResponse` 형식을 따른다. 오류는 `ErrorCode`와 `GlobalExceptionHandler`를 사용하며 원본 예외 메시지를 클라이언트에 노출하지 않는다.

## 인증·설정·데이터

- Kakao 앱 로그인은 클라이언트 nonce를 포함한 OIDC ID 토큰을 검증한다. 서명, issuer, audience, 만료, 발급 시각, nonce와 Redis 재사용 방지를 유지한다. `oauth2Login` 흐름을 새로 도입하지 않는다.
- `member.password_hash`는 외부 로그인 회원에 대해 null을 허용하며 `external_identity`의 `(provider, provider_subject)` 고유성을 유지한다.
- JWT 서명키는 환경마다 별도로 생성한다. Redis 설정은 활성 Spring 프로필과 맞춘다.
- 자격 증명이 있는 CORS는 `app.cors.allowed-origins`에 명시적 출처를 지정한다. refresh token 쿠키의 `HttpOnly`와 HTTPS 환경의 `Secure` 설정을 유지한다.
- 운영 데이터베이스 스키마를 자동 삭제·재생성하지 않는다. 스키마 변경은 별도 검토와 마이그레이션 계획을 거친다.
- 비밀번호, JWT, refresh token, ID token, 인증 코드, 비밀값을 로깅하지 않는다. 기존 MDC의 `traceId`, `eventId`, `memberId`, `clientIp`를 사용한다.

## 실행

루트에서 `./gradlew :api:bootRun`으로 로컬 API를 실행하고 `./gradlew test`로 전체 백엔드를 검사한다. 실제 외부 서비스·데이터를 건드리는 명령은 대상 환경을 확인한 뒤 실행한다.

이 규칙의 설명은 한글로 쓰되 Java·Spring·Gradle의 식별자, 패키지명, 애너테이션, API 경로, 설정 키는 코드에 적힌 정확한 표기를 사용한다.
