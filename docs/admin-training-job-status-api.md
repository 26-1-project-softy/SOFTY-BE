# 관리자 학습 작업 상태 조회 API 명세

## 개요
- 관리자 화면에서 재학습 요청 후 반환받은 `jobId`로 작업 상태를 polling 하기 위한 API
- 엔드포인트는 `GET /admin/training-jobs/{jobId}`
- 내부적으로 AI 서버 `GET /ai/training-jobs/{jobId}`를 호출해 상태 정보를 가져온다
- 프론트는 `POST /admin/retraining` 응답의 `jobId`를 사용해 이 API를 반복 호출할 수 있다

## 인증/권한
- `Authorization: Bearer {accessToken}` 헤더 필수
- 관리자(`role=ADMIN`)만 조회 가능

## 요청
### Headers
- `Authorization`: `Bearer {JWT}`

### Path Params
- `jobId` (string, required)
  - 재학습 시작 API 응답으로 받은 작업 ID

### 요청 예시
```http
GET /admin/training-jobs/retrain_20260516_6D8
Authorization: Bearer {JWT}
```

## 동작 규칙
- AI 서버 호출 경로: `{AI_SERVER_BASE_URL}/ai/training-jobs/{jobId}`
- `jobId`가 비어 있으면 `400 Bad Request`
- AI 응답에서 필요한 값만 프론트용 응답으로 매핑한다
  - `jobId`
  - `status`
  - `progressPercent`
  - `startedAt`
  - `finishedAt`
  - `modelName`
  - `modelVersion`
  - `datasetVersion`
- `status`는 대문자 형식으로 정규화해 반환한다
  - 예: `running` -> `RUNNING`
  - 예: `failed` -> `FAILED`
- AI 쪽 일부 필드가 없어도 `job_id`만 있으면 응답 가능하도록, 없는 문자열 값은 빈 문자열로 반환한다
- `progressPercent`는 AI 값이 없으면 `null`로 반환한다

## 응답
### 성공 (200)
```json
{
  "success": true,
  "code": 200,
  "message": "학습 작업 상태 조회에 성공했습니다.",
  "data": {
    "jobId": "retrain_20260516_6D8",
    "modelName": "kanana-risk-detector",
    "modelVersion": "v1.2.9",
    "datasetVersion": "v1.1",
    "status": "FAILED",
    "progressPercent": 0,
    "startedAt": "2026-05-16T12:15:36",
    "finishedAt": ""
  }
}
```

## 응답 필드 설명
- `success`: API 처리 성공 여부
- `code`: HTTP 상태 코드
- `message`: 처리 결과 메시지
- `data.jobId`: 학습 작업 ID
- `data.modelName`: 모델 이름
- `data.modelVersion`: 모델 버전
- `data.datasetVersion`: 데이터셋 버전
- `data.status`: 학습 작업 상태
- `data.progressPercent`: 진행률 퍼센트
- `data.startedAt`: 시작 시각
- `data.finishedAt`: 종료 시각

## AI 서버 응답 매핑
- `job_id` -> `jobId`
- `model_name` -> `modelName`
- `version` -> `modelVersion`
- `dataset_version` -> `datasetVersion`
- `status` -> `status`
- `progress_percent` -> `progressPercent`
- `started_at` -> `startedAt`
- `finished_at` -> `finishedAt`

## 오류 응답
- `400 Bad Request`
  - `jobId`가 비어 있는 경우
  - AI 서버 URL이 올바르지 않은 경우
- `401 Unauthorized`
  - Authorization 헤더 누락 또는 토큰 오류
- `403 Forbidden`
  - 관리자 권한 없음
- `404 Not Found`
  - 해당 `jobId`의 학습 작업을 찾을 수 없음
- `502 Bad Gateway`
  - AI 서버가 4xx/5xx로 응답
  - AI 서버 응답 바디가 비어 있음
- `504 Gateway Timeout`
  - AI 서버 연결 또는 응답 시간 초과

## 프론트 사용 흐름
1. `POST /admin/retraining` 호출
2. 응답 `data.jobId` 저장
3. `GET /admin/training-jobs/{jobId}`를 일정 간격으로 polling
4. `status`가 `COMPLETED` 또는 `FAILED`가 되면 polling 중단
