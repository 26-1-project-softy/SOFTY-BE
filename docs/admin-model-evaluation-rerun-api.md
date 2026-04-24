# 관리자 모델 재평가 요청 API 명세

## 개요
- 관리자 대시보드의 `다시 평가` 버튼 클릭 시 AI 서버에 성능 재평가 작업 생성을 요청하는 API
- 엔드포인트: `POST /admin/models/latest/evaluation/re-run`

## 인증/권한
- `Authorization: Bearer {accessToken}` 헤더 필수
- 관리자(`role=ADMIN`)만 호출 가능

## 요청
### Headers
- `Authorization`: `Bearer {JWT}`
- `Content-Type`: `application/json`

### Body (optional)
```json
{
  "version": "v1.0",
  "datasetVersion": "v1.0-test"
}
```

### 요청 필드 설명
- `version`: 평가 대상 모델 버전 (optional)
- `datasetVersion`: 평가 대상 데이터셋 버전 (optional)

## 동작 규칙
- `version`, `datasetVersion`이 있으면 해당 값을 AI 서버로 전달
- 값이 없으면 BE는 DB에서 보정하지 않고 `null` 그대로 AI 서버에 전달
- 최신 모델 버전 선택 기준은 AI 서버 정책을 따름

## 내부 연동 (BE -> AI)
- 호출 API: `POST {AI_SERVER_BASE_URL}/ai/evaluations/risk-detection`
- 요청 바디:
```json
{
  "version": "v1.0",
  "dataset_version": "v1.0-test"
}
```
- 값이 없으면 해당 필드는 `null`로 전달될 수 있음

## 응답
### 성공 (200)
```json
{
  "success": true,
  "code": 200,
  "message": "재평가 요청에 성공했습니다.",
  "data": {
    "evaluationId": "eval_20260326_001",
    "status": "queued",
    "resultCode": 200,
    "resultMessage": "evaluation job created",
    "contentType": "json",
    "version": "v1.0",
    "datasetVersion": "v1.0-test"
  }
}
```

### 응답 필드 설명
- `data.evaluationId`: 생성된 평가 작업 ID
- `data.status`: 평가 작업 상태 (`queued` 등)
- `data.resultCode`: AI 서버 처리 결과 코드
- `data.resultMessage`: AI 서버 처리 결과 메시지
- `data.contentType`: AI 서버 응답 데이터 형식
- `data.version`: 요청에 사용한 모델 버전
- `data.datasetVersion`: 요청에 사용한 데이터셋 버전

## 오류 응답
- `400 Bad Request`
  - 잘못된 AI 서버 URL 또는 요청 경로 구성 오류
- `401 Unauthorized`
  - Authorization 헤더 누락/토큰 오류
- `403 Forbidden`
  - 관리자 권한 아님
- `502 Bad Gateway`
  - AI 서버가 4xx/5xx로 응답
  - AI 서버 응답 바디가 비어 있음
- `504 Gateway Timeout`
  - AI 서버 연결/응답 타임아웃
