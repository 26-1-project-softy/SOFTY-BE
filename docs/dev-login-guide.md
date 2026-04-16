# Dev Login Guide

## 개요
카카오 계정을 매번 만들지 않고도 로컬/개발 환경에서 로그인 테스트를 빠르게 하기 위해 `dev-login` 기능을 추가했습니다.

- 엔드포인트: `POST /auth/dev-login`
- 활성 프로필: `local`, `dev` 에서만 활성화
- 운영(production)에서는 로드되지 않음

## 왜 만들었는가
카카오 실로그인은 연동 검증에는 필요하지만, 기능 개발/디버깅 단계에서 반복 테스트 비용이 큽니다.
`dev-login`은 내부적으로 사용자와 토큰을 즉시 만들어 인증 이후 플로우를 빠르게 테스트할 수 있게 합니다.

## 어떻게 구현했는가
아래 3개 파일을 수정/추가했습니다.

1. `src/main/java/com/softy/be/auth/controller/DevAuthController.java`
- `@Profile({"local", "dev"})`로 환경 제한
- `POST /auth/dev-login` 추가
- 응답 포맷은 기존 카카오 로그인과 동일하게 `KakaoLoginData` 사용

2. `src/main/java/com/softy/be/auth/service/AuthService.java`
- `loginForDev(String socialId, String role, String nickname)` 추가
- `DEV_KAKAO` provider 기준으로 `SocialAccount` 조회/생성(upsert)
- 역할 정규화 및 검증 (`UNASSIGNED`, `TEACHER`, `PARENT`)
- Access/Refresh JWT 발급

3. `src/main/java/com/softy/be/user/entity/User.java`
- `applyDevLoginProfile(name, role)` 메서드 추가
- dev 로그인 시 사용자 이름/역할을 업데이트

## 요청 파라미터
- `socialId` (필수): 테스트 계정 식별자
- `role` (선택): `UNASSIGNED` | `TEACHER` | `PARENT` (`USER`는 `UNASSIGNED`로 처리)
- `nickname` (선택): 미입력 시 `dev_{socialId}` 기본값 사용

## 사용 방법
### 1) 서버 실행 시 프로필 확인
`local` 또는 `dev` 프로필로 실행되어야 합니다.

### 2) dev 로그인 호출
```bash
curl -X POST "http://localhost:8080/auth/dev-login?socialId=test-001&role=TEACHER&nickname=devTeacher"
```

### 3) 응답에서 토큰 사용
응답의 `accessToken`을 기존 API 호출 시 `Authorization: Bearer {token}`으로 사용하면 됩니다.

## 동작 예시 시나리오
- 같은 `socialId`로 다시 호출하면 같은 사용자 계정으로 로그인됩니다.
- `role`을 바꿔 호출하면 해당 계정 역할이 업데이트됩니다.
- `role=UNASSIGNED`이면 회원가입 미완료 상태를 재현할 수 있습니다.

## 주의사항
- 이 기능은 개발 편의용입니다.
- 프로필 제한(`local`, `dev`)이 제거되지 않도록 유지해야 합니다.
- 운영 배포 설정에서 `local`/`dev` 프로필이 켜지지 않도록 반드시 확인하세요.

