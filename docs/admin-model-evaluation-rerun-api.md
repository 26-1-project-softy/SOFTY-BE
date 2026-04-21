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

### 요청 필드 의미
- `version`: 평가 대상 모델 버전 (optional)
- `datasetVersion`: 평가 데이터셋 버전 (optional)

### 버전 결정 규칙
- `version`, `datasetVersion`을 둘 다 보내면 요청값 그대로 사용
- 둘 중 하나라도 누락되면 DB(`model_training_history`) 최신 레코드의 버전 값을 사용
  - 조건: `model_version`, `dataset_version`이 모두 null/blank가 아닌 레코드
  - 정렬: `trained_at DESC, id DESC`

## 내부 연동 (BE -> AI)
- 호출 API: `POST {AI_SERVER_BASE_URL}/ai/evaluations/risk-detection`
- 요청 바디:
```json
{
  "version": "v1.0",
  "dataset_version": "v1.0-test"
}
```

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

### 응답 필드 의미
- `data.evaluationId`: 생성된 평가 작업 ID
- `data.status`: 평가 작업 상태 (`queued` 등)
- `data.resultCode`: AI 서버 처리 결과 코드
- `data.resultMessage`: AI 서버 처리 결과 메시지
- `data.contentType`: AI 서버 응답 데이터 형식
- `data.version`: 이번 재평가 요청에 실제 사용된 모델 버전
- `data.datasetVersion`: 이번 재평가 요청에 실제 사용된 데이터셋 버전

## 오류 응답
- `400 Bad Request`
  - 잘못된 AI 서버 URL 또는 요청 경로 구성 오류
  - 요청값 검증 실패 (`version`/`datasetVersion`가 최종적으로 비어 있는 경우)
- `401 Unauthorized`
  - Authorization 헤더 누락/토큰 오류
- `403 Forbidden`
  - 관리자 권한 아님
- `404 Not Found`
  - 요청에 버전 미입력 + DB에 사용 가능한 `model_version`/`dataset_version` 정보 없음
- `502 Bad Gateway`
  - AI 서버가 4xx/5xx로 응답
  - AI 서버 응답 바디가 비어 있음
- `504 Gateway Timeout`
  - AI 서버 연결/응답 타임아웃
