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
| `EMBEDDING_API_KEY` | 임베딩 API 인증 키(Bearer) | 없음 |
| `EMBEDDING_MODEL` | 임베딩 모델명 | 없음 |
| `EMBEDDING_API_URL` | 임베딩 API URL | 없음 |
| `EMBEDDING_REQUEST_INTERVAL_MS` | 임베딩 API 호출 간 최소 간격(ms) | `100` |
| `EMBEDDING_RETRY_MAX_ATTEMPTS` | 503/504/429 재시도 최대 횟수 | `3` |
| `EMBEDDING_RETRY_INITIAL_DELAY_MS` | 재시도 초기 대기(ms) | `300` |
| `EMBEDDING_RETRY_BACKOFF_MULTIPLIER` | 재시도 백오프 배수 | `2.0` |
| `SWAGGER_SERVER_URL` | 서버 URL | 빈 값 |
| `SWAGGER_LOCAL_SERVER_URL` | 로컬 URL | 빈 값 |
| `LOG_PATH` | 로그 파일 경로 | `/app/logs` |

## 4. 데이터베이스

- 기본 DB: `PostgreSQL`
- 연결 정보는 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`로 주입
- JPA 설정이 `ddl-auto: validate`이므로, 실행 전 DB 스키마가 준비되어 있어야 함
- 테스트(`application-test.yml`)는 H2 in-memory DB 사용
- 스키마 문서:
  - 요약: `docs/db-schema.md`
  - 실행용 SQL: `docs/db-schema-postgres.sql`

## 5. 인증/권한 모델

현재 백엔드는 단일 `users.role` 컬럼 대신 아래 구조를 사용합니다.

- `users`
  - 로그인 주체 계정 정보 보관
- `user_role`
  - 한 계정이 보유한 역할 목록 보관
  - 예: `TEACHER`, `PARENT`, `ADMIN`
- JWT `activeRole`
  - 현재 세션이 어떤 역할로 동작 중인지 표현

예시:

- 같은 카카오 계정이 `TEACHER`, `PARENT`를 둘 다 가질 수 있음
- `/auth/kakao/login`으로 로그인하면 학부모 세션(`activeRole=PARENT`)
- `/auth/kakao/login/teacher`로 로그인하면 교사 세션(`activeRole=TEACHER`)

## 6. 로컬 실행

### 6.1 애플리케이션 실행

```bash
# Windows
./gradlew.bat bootRun

# macOS/Linux
./gradlew bootRun
```

기본 포트: `8080`

### 6.2 테스트 실행

```bash
./gradlew test
```

테스트 프로필은 `application-test.yml`을 사용하며 H2(in-memory) DB로 동작합니다.

### 6.3 Docker 실행

```bash
docker build -t softy-be .
docker run --env-file .env -p 8080:8080 softy-be
```

## 7. 폴더 구조

```text
softy-be/
├─ src/
│  ├─ main/
│  │  ├─ java/com/softy/be/
│  │  │  ├─ admin/         # 관리자 인증/통계
│  │  │  ├─ auth/          # 인증/JWT
│  │  │  ├─ user/          # 사용자/설정/회원가입
│  │  │  ├─ school/        # 학교/학급
│  │  │  ├─ chat/          # 채팅
│  │  │  ├─ report/        # 증빙 리포트
│  │  │  ├─ common/        # 공통 설정/응답/엔티티
│  │  │  ├─ health/        # 헬스체크
│  │  │  └─ BeApplication.java
│  │  └─ resources/
│  │     └─ application.yml
│  └─ test/
│     ├─ java/com/softy/be/
│     └─ resources/application-test.yml
├─ docs/
├─ gradle/wrapper/
├─ build.gradle
├─ settings.gradle
├─ Dockerfile
└─ README.md
```

## 8. 도메인 모델 요약

- `User`: 사용자 계정 정보
- `UserRole`: 사용자 보유 역할
- `SocialAccount`: 소셜 계정(KAKAO) 매핑
- `School`: 학교 정보
- `Classroom`: 학급 정보(학년/반/담임)
- `ClassCode`: 학급 참여 코드
- `Student`: 학생 정보
- `ParentStudent`: 학부모-학생 연결 정보
- `TeacherSetting`: 교사 근무시간 설정
- `ChatRoom`: 채팅방 정보
- `ChatRoomUserMap`: 채팅방 참여자 매핑
- `Message`: 메시지 정보

## 9. 운영 시 주의사항

- JPA 설정이 `ddl-auto: validate`이므로 운영 DB 스키마를 사전에 준비해야 합니다.
- `.env`에는 민감정보(DB 비밀번호, OAuth 시크릿 등)가 포함될 수 있으므로 외부 공유를 금지하세요.
- 프로덕션 배포 전 `JWT_SECRET`, `ADMIN_PROVISION_KEY`를 강한 값으로 교체하세요.
