# 관리자 모델 최신 평가 API 명세

## 개요
- AI 평가 서버의 성능 평가 결과를 관리자 대시보드에서 조회하기 위한 API
- 엔드포인트: `GET /admin/models/latest/evaluation`

## 인증/권한
- `Authorization: Bearer {accessToken}` 헤더 필수
- 관리자(`role=ADMIN`)만 조회 가능

## 요청
### Query Params
- `evaluationId` (String, optional): AI 평가 작업 ID (예: `eval_20260326_001`)
  - 값이 있으면 해당 ID로 조회
  - 값이 없으면 BE는 값을 보정하지 않고 AI 서버에 최신 평가 조회를 위임

### Headers
- `Authorization`: `Bearer {JWT}`

## 동작 규칙
- AI 서버 호출 경로: `{AI_SERVER_BASE_URL}/ai/evaluations`
- `evaluationId` 입력 시 AI 서버 호출 쿼리: `?evaluation_id={evaluationId}`
- `evaluationId` 미입력 시 쿼리 파라미터 없이 호출
- `AI_SERVER_BASE_URL`는 필수 환경변수 (기본값 없음)
- `evaluationId` 미입력 시 최신 평가 결정 기준은 AI 서버 정책을 따름
- AI 응답 점수값이 `null`이면 `0.0`으로 치환
  - `precision`
  - `recall`
  - `f1_score` -> `f1Score`
- `status`가 `completed`가 아니어도 정상 응답(`200`)으로 반환
- AI 서버 통신 실패/타임아웃은 각각 `502`/`504`로 반환

## 응답
### 성공 (200)
```json
{
  "success": true,
  "code": 200,
  "message": "성능 평가 조회에 성공했습니다.",
  "data": {
    "evaluationId": "eval_20260326_001",
    "precision": 0.84,
    "recall": 0.80,
    "f1Score": 0.86,
    "status": "completed",
    "passed": true,
    "version": "v1.0",
    "resultCode": 200,
    "resultMessage": "success"
  }
}
```

### 응답 필드 설명
- `success`: API 처리 성공 여부
- `code`: HTTP 상태 코드
- `message`: 처리 결과 메시지
- `data`: 성능 평가 결과 객체
  - `evaluationId`: 조회에 사용된 평가 ID
  - `precision`: 정밀도 점수 (`null` 입력 시 `0.0`)
  - `recall`: 재현율 점수 (`null` 입력 시 `0.0`)
  - `f1Score`: F1 score (`null` 입력 시 `0.0`)
  - `status`: 평가 상태 (`pending`, `running`, `completed`, `failed` 등)
  - `passed`: 배포 가능 여부
  - `version`: 평가 모델 버전
  - `resultCode`: AI 서버 내부 처리 결과 코드
  - `resultMessage`: AI 서버 내부 처리 결과 메시지

## 오류 응답
- `400 Bad Request`
  - 잘못된 AI 서버 URL 또는 요청 경로 구성 오류
- `401 Unauthorized`
  - Authorization 헤더 누락/토큰 오류
- `403 Forbidden`
  - 관리자 권한 아님
- `404 Not Found`
  - AI 서버에 조회 가능한 최신 평가가 없음
- `502 Bad Gateway`
  - AI 서버가 4xx/5xx로 응답
  - AI 서버 응답 바디가 비어 있음
- `504 Gateway Timeout`
  - AI 서버 연결/응답 타임아웃
