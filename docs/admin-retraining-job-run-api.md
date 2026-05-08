# 관리자 재학습 요청 API 명세

## 개요
- 관리자 화면에서 위험 탐지 모델 재학습을 요청하기 위한 API
- 엔드포인트: `POST /admin/retraining`
- 내부적으로 AI 서버 `POST /ai/retraining-jobs/risk-detection`를 호출해 재학습 작업을 생성한다
- 프론트는 별도 요청값 없이 버튼 클릭만으로 호출한다

## 인증/권한
- `Authorization: Bearer {accessToken}` 헤더 필수
- 관리자(`role=ADMIN`)만 요청 가능

## 요청
### Headers
- `Authorization`: `Bearer {JWT}`
- `Content-Type`: `application/json`

### Body
- 없음
- BE는 AI 서버 호출 시 기본값 사용을 위해 빈 JSON `{}`를 전송한다

### 요청 예시
```http
POST /admin/retraining
Authorization: Bearer {JWT}
Content-Type: application/json

{}
```

## 동작 규칙
- AI 서버 호출 경로: `{AI_SERVER_BASE_URL}/ai/retraining-jobs/risk-detection`
- 프론트 요청 바디는 받지 않는다
- BE는 AI 서버에 빈 JSON `{}`를 전달해 AI 서버 기본 재학습 설정을 사용한다
- BE는 AI 서버 응답 중 프론트 화면에 필요한 값만 추려서 반환한다
  - `jobId`
  - `status`
- `status`는 AI 서버 응답 값을 대문자 형식으로 정규화해 반환한다
  - 예: `queued` -> `QUEUED`
- AI 서버 응답이 비어 있거나 파싱할 수 없으면 `502 Bad Gateway`를 반환한다
- AI 서버 연결 또는 응답 시간이 초과되면 `504 Gateway Timeout`을 반환한다

## 응답
### 성공 (200)
```json
{
  "success": true,
  "code": 200,
  "message": "재학습 요청에 성공했습니다.",
  "data": {
    "jobId": "retrain_20260326_001",
    "status": "QUEUED"
  }
}
```

### 응답 필드 설명
- `success`: API 처리 성공 여부
- `code`: HTTP 상태 코드
- `message`: 처리 결과 메시지
- `data.jobId`: 생성된 재학습 작업 ID
- `data.status`: 재학습 작업 상태

## AI 서버 응답 매핑
- `job_id` -> `jobId`
- `status` -> `status`

## 오류 응답
- `401 Unauthorized`
  - Authorization 헤더 누락 또는 토큰 오류
- `403 Forbidden`
  - 관리자 권한 없음
- `502 Bad Gateway`
  - AI 서버가 4xx/5xx로 응답
  - AI 서버 응답 바디가 비어 있음
  - AI 서버 응답 JSON 파싱 실패
- `504 Gateway Timeout`
  - AI 서버 연결 또는 응답 시간 초과

## 비고
- AI 서버는 요청값이 없으면 기본 재학습 설정을 자동 적용한다.
- 관리자 화면에서는 응답 `jobId`를 별도 노출하지 않더라도, 추후 재학습 상태 추적 기능에서 사용할 수 있다.
