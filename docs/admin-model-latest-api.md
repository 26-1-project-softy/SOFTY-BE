# 관리자 최신 모델 정보 API 명세

## 개요
- 관리자 대시보드의 모델 정보 카드를 조회하기 위한 API
- 엔드포인트: `GET /admin/models/latest`
- 내부적으로 AI 서버의 `GET /ai/training-jobs`를 `job_id` 없이 호출해 최신 학습 작업 정보를 가져온다

## 인증/권한
- `Authorization: Bearer {accessToken}` 헤더 필수
- 관리자(`role=ADMIN`)만 호출 가능

## 요청
### Headers
- `Authorization`: `Bearer {JWT}`

### Query Params
- 없음

## 동작 규칙
- AI 서버 호출 경로: `{AI_SERVER_BASE_URL}/ai/training-jobs`
- `job_id`는 전달하지 않는다
- AI 응답의 `started_at`을 `lastTrainedAt`으로 사용한다
- 현재 `lastDeployedAt`은 `lastTrainedAt`과 같은 값으로 내려준다
- `status`는 AI 서버 응답 값을 그대로 사용한다
- `datasetVersion` 필드가 비어 있으면 `null`로 반환한다
- 최신 이력이 없으면 `404 Not Found`를 반환한다

## 응답
### 성공 (200)
```json
{
  "success": true,
  "code": 200,
  "message": "모델 정보 조회 성공",
  "data": {
    "jobId": "train_20260326_001",
    "modelName": "kanana-risk-detector",
    "modelVersion": "v1.0.3",
    "datasetVersion": "dataset-v5",
    "status": "completed",
    "lastTrainedAt": "2026-04-25T01:00:00",
    "lastDeployedAt": "2026-04-25T01:00:00"
  }
}
```

### 응답 필드 설명
- `success`: API 처리 성공 여부
- `code`: HTTP 상태 코드
- `message`: 처리 결과 메시지
- `data.jobId`: 최신 학습 작업 ID
- `data.modelName`: 생성된 모델명
- `data.modelVersion`: 생성된 모델 버전
- `data.datasetVersion`: 사용한 데이터셋 버전. AI 서버에서 내려주지 않으면 `null`
- `data.status`: AI 서버가 반환한 학습 상태 값
- `data.lastTrainedAt`: 최신 학습 시각 (`started_at`)
- `data.lastDeployedAt`: 현재는 최신 학습 시각과 동일한 값

## AI 서버 응답 매핑
- `job_id` -> `jobId`
- `model_name` -> `modelName`
- `version` -> `modelVersion`
- `dataset_version` -> `datasetVersion`
- `status` -> `status`
- `started_at` -> `lastTrainedAt`
- `started_at` -> `lastDeployedAt`

## 오류 응답
- `401 Unauthorized`
  - Authorization 헤더 누락 또는 토큰 오류
- `403 Forbidden`
  - 관리자 권한 없음
- `404 Not Found`
  - AI 서버에 최신 학습 이력이 없거나 `job_id`가 비어 있음
- `502 Bad Gateway`
  - AI 서버가 4xx/5xx로 응답
  - AI 서버 응답 바디가 비어 있음
- `504 Gateway Timeout`
  - AI 서버 연결 또는 응답 시간 초과
