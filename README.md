# SOFTY Backend

SOFTY 백엔드는 교사-학부모 소통 과정의 분쟁 리스크 완화를 목표로 하는 서비스 서버입니다.

## 1. 서비스 개요

- 서비스명: **SOFTY**
- 목적: 교사-학부모 커뮤니케이션의 리스크를 줄이고, 안전하고 신뢰 가능한 소통 기반 제공
- 핵심 가치:
  - AI 기반 사전 분쟁 예방
  - 교권 보호 및 리스크 완화
  - 데이터 기반 서비스 개선

## 2. 기술 스택

- Java 17
- Spring Boot 3.5.13
- Spring Web
- Spring Data JPA
- PostgreSQL
- JWT (`jjwt`)
- Gradle

## 3. 실행 전 준비

`src/main/resources/application.yml` 기준으로 아래 환경변수가 필요합니다.

| 변수명 | 설명 | 기본값 |
| --- | --- | --- |
| `DB_URL` | PostgreSQL JDBC URL | 없음 |
| `DB_USERNAME` | DB 계정 | 없음 |
| `DB_PASSWORD` | DB 비밀번호 | 없음 |
| `JWT_SECRET` | JWT 서명 시크릿 | 없음 |
| `JWT_EXPIRATION_SECONDS` | Access Token 만료(초) | `86400` |
| `JWT_REFRESH_EXPIRATION_SECONDS` | Refresh Token 만료(초) | `1209600` |
| `ADMIN_PROVISION_KEY` | 관리자 생성용 프로비전 키 | 없음 |
| `SWAGGER_SERVER_URL` | 서버 URL | 빈 값 |
| `SWAGGER_LOCAL_SERVER_URL` | 로컬 URL | 빈 값 |
| `LOG_PATH` | 로그 파일 경로 | `/app/logs` |

## 4. 데이터베이스

- 기본 DB: `PostgreSQL`
- 연결 정보는 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`로 주입
- JPA 설정이 `ddl-auto: validate`이므로, 실행 전 DB 스키마가 준비되어 있어야 함
- 테스트(`application-test.yml`)는 H2 in-memory DB 사용
- 스키마 초안 문서: `docs/db-schema.md`

## 5. 로컬 실행

### 5.1 애플리케이션 실행

```bash
# Windows
./gradlew.bat bootRun

# macOS/Linux
./gradlew bootRun
```

기본 포트: `8080`

### 5.2 테스트 실행

```bash
./gradlew test
```

테스트 프로필은 `application-test.yml`을 사용하며 H2(in-memory) DB로 동작합니다.

### 5.3 Docker 실행

```bash
docker build -t softy-be .
docker run --env-file .env -p 8080:8080 softy-be
```

## 6. 폴더 구조

```text
be/
├─ src/
│  ├─ main/
│  │  ├─ java/com/softy/be/
│  │  │  ├─ admin/         # 관리자 인증 도메인 (controller/dto/service)
│  │  │  ├─ auth/          # 인증 도메인 (controller/dto/service)
│  │  │  ├─ user/          # 사용자 도메인 (controller/dto/service)
│  │  │  ├─ domain/        # JPA 엔티티
│  │  │  │  ├─ common/
│  │  │  │  ├─ user/
│  │  │  │  ├─ school/
│  │  │  │  └─ chat/
│  │  │  ├─ repository/    # JPA Repository
│  │  │  │  ├─ user/
│  │  │  │  └─ school/
│  │  │  ├─ global/        # 공통 설정/응답
│  │  │  └─ health/        # 헬스체크
│  │  │  └─ BeApplication.java
│  │  └─ resources/
│  │     └─ application.yml
│  └─ test/
│     ├─ java/com/softy/be/
│     └─ resources/application-test.yml
├─ gradle/wrapper/
├─ build.gradle
├─ settings.gradle
├─ Dockerfile
└─ README.md
```

## 7. 도메인 모델 요약

- `User`: 사용자 정보 및 역할 관리
- `SocialAccount`: 소셜 계정(KAKAO) 매핑
- `School`: 학교 정보
- `Classroom`: 학급 정보(학년/반/담임)
- `ClassCode`: 학급 참여 코드
- `Student`: 학생 정보
- `ParentStudent`: 학부모-학생 연결 정보
- `ChatRoom`: 채팅방 정보
- `Message`: 메시지 정보

## 8. 운영 시 주의사항

- JPA 설정이 `ddl-auto: validate`이므로 운영 DB 스키마를 사전에 준비해야 합니다.
- `.env`에는 민감정보(DB 비밀번호, OAuth 시크릿 등)가 포함될 수 있으므로 외부 공유를 금지하세요.
- 프로덕션 배포 전 `JWT_SECRET`, `ADMIN_PROVISION_KEY`를 강한 값으로 교체하세요.
