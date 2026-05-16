# Dev Login Guide

## 개요

카카오 계정을 매번 새로 만들지 않고도 로컬/개발 환경에서 로그인 흐름을 빠르게 테스트할 수 있도록 `dev-login` 기능을 제공합니다.

- 엔드포인트: `POST /auth/dev-login`
- 활성 프로필: `local`
- 운영 환경에서는 로드되지 않음

## 왜 필요한가

카카오 실로그인은 연동 검증에 필요하지만, 기능 개발과 화면 테스트 단계에서는 반복 비용이 큽니다.  
`dev-login`은 임의 사용자로 즉시 JWT를 발급해 인증 이후 흐름을 빠르게 확인할 수 있게 해줍니다.

## 현재 동작 방식

1. `socialId` 기준으로 `DEV_KAKAO` 소셜 계정을 조회하거나 생성합니다.
2. `role` 파라미터를 현재 세션의 `activeRole`로 사용합니다.
3. `role=TEACHER` 또는 `role=PARENT`이면 해당 역할을 dev 계정의 `user_role`에 추가하고, 해당 역할 세션으로 로그인합니다.
4. `role=UNASSIGNED`이면 dev 계정의 역할을 비우고 회원가입 미완료 상태를 재현합니다.

주의:

- 같은 `socialId`로 `role=TEACHER`, `role=PARENT`를 순서대로 호출하면 한 dev 계정이 두 역할을 모두 보유할 수 있습니다.
- `role=UNASSIGNED`는 예외적으로 기존 역할을 모두 제거합니다.

## 요청 파라미터

- `socialId` (필수): 테스트 계정 식별자
- `role` (선택): `UNASSIGNED` | `TEACHER` | `PARENT`
- `nickname` (선택): 미입력 시 `dev_{socialId}` 사용

## 사용 방법

### 1) 로컬 프로필로 실행

애플리케이션이 `local` 프로필로 실행되어 있어야 합니다.

### 2) dev 로그인 호출

```bash
curl -X POST "http://localhost:8080/auth/dev-login?socialId=test-001&role=TEACHER&nickname=devTeacher"
```

### 3) 응답 토큰 사용

응답의 `accessToken`을 기존 API 호출 시 `Authorization: Bearer {token}` 헤더로 사용합니다.

## 예시 시나리오

- 같은 `socialId`로 다시 호출하면 같은 사용자 계정으로 로그인됩니다.
- `role=TEACHER`로 로그인하면 현재 세션 `activeRole`은 `TEACHER`입니다.
- `role=PARENT`로 로그인하면 현재 세션 `activeRole`은 `PARENT`입니다.
- 같은 `socialId`로 먼저 `role=TEACHER`, 이후 `role=PARENT`로 로그인하면 해당 계정은 `TEACHER`, `PARENT`를 모두 보유하게 됩니다.
- `role=UNASSIGNED`이면 기존 역할을 비우고 회원가입 전 상태를 재현할 수 있습니다.

## 주의사항

- 이 기능은 개발 전용입니다.
- `@Profile("local")` 설정이 운영 환경에 포함되지 않도록 주의해야 합니다.
- 운영 배포 전에는 `local` 프로필 활성화 여부를 반드시 확인하세요.
