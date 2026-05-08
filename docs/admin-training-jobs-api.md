# 관리자 학습 이력 조회 API 명세

## 개요
- 관리자 대시보드에서 모델 학습 이력 목록과 F1-score 추이를 조회하기 위한 API
- 엔드포인트: `GET /admin/training-jobs`
- 내부적으로 AI 서버 `GET /ai/training-history`를 호출해 학습 이력 데이터를 가져온다

## 인증/권한
- `Authorization: Bearer {accessToken}` 헤더 필수
- 관리자(`role=ADMIN`)만 조회 가능

## 요청
### Headers
- `Authorization`: `Bearer {JWT}`

### Query Params
- `page` (int, optional, default=`1`)
  - 조회할 페이지 번호
  - 1 이상
- `size` (int, optional, default=`20`)
  - 페이지당 조회 개수
  - 1 이상

### 요청 예시
```http
GET /admin/training-jobs?page=1&size=20
Authorization: Bearer {JWT}
```

## 동작 규칙
- AI 서버 호출 경로: `{AI_SERVER_BASE_URL}/ai/training-history`
- AI 서버 호출 쿼리 파라미터
  - `page` -> 그대로 전달
  - `size` -> `page_size`로 변환해 전달
- BE는 별도 DB 조회 없이 AI 서버 응답을 기반으로 프론트 응답을 구성한다
- `items`는 차트와 테이블에서 함께 사용할 수 있도록 같은 데이터 소스로 반환한다
- `f1Score`는 AI 서버 값이 `null`이면 그대로 `null`로 반환한다
- `status`는 AI 서버 응답 값을 대문자 형식으로 정규화해 반환한다
  - 예: `completed` -> `COMPLETED`
  - 예: `failed` -> `FAILED`
- AI 서버 응답이 비어 있거나 파싱할 수 없으면 `502 Bad Gateway`를 반환한다
- AI 서버 연결 또는 응답 시간이 초과되면 `504 Gateway Timeout`을 반환한다

## 응답
### 성공 (200)
```json
{
  "success": true,
  "code": 200,
  "message": "학습 이력 조회에 성공했습니다.",
  "data": {
    "items": [
      {
        "trainedAt": "2026-05-06T18:57:07",
        "version": "v1.2",
        "datasetVersion": "v1.1",
        "f1Score": null,
        "status": "COMPLETED"
      },
      {
        "trainedAt": "2026-05-06T18:57:07",
        "version": "v1.2",
        "datasetVersion": "v1.1",
        "f1Score": 0.8044,
        "status": "COMPLETED"
      }
    ],
    "page": 1,
    "size": 20,
    "totalCount": 38,
    "totalPages": 2
  }
}
```

### 응답 필드 설명
- `success`: API 처리 성공 여부
- `code`: HTTP 상태 코드
- `message`: 처리 결과 메시지
- `data.items`: 학습 이력 목록
  - `trainedAt`: 학습 시각
  - `version`: 모델 버전
  - `datasetVersion`: 데이터셋 버전
  - `f1Score`: F1-score 값 (`null` 가능)
  - `status`: 학습 상태
- `data.page`: 현재 페이지 번호
- `data.size`: 페이지당 조회 개수
- `data.totalCount`: 전체 이력 개수
- `data.totalPages`: 전체 페이지 수

## AI 서버 응답 매핑
- `pagination.page` -> `page`
- `pagination.page_size` -> `size`
- `pagination.total_count` -> `totalCount`
- `pagination.total_pages` -> `totalPages`
- `data[].training_date` -> `items[].trainedAt`
- `data[].version` -> `items[].version`
- `data[].dataset` -> `items[].datasetVersion`
- `data[].f1_score` -> `items[].f1Score`
- `data[].status` -> `items[].status`

## 오류 응답
- `400 Bad Request`
  - `page`가 1 미만인 경우
  - `size`가 1 미만인 경우
  - AI 서버 URL이 올바르지 않은 경우
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
- 프론트 차트는 `items[].version`과 `items[].f1Score`를 사용해 F1-score 추이를 표시할 수 있다.
- 프론트 테이블은 같은 `items`를 그대로 사용해 학습 시각, 버전, 데이터셋, F1-score, 상태를 표시할 수 있다.
- `f1Score`가 `null`인 경우 프론트에서 `-` 또는 `X` 등 별도 표기 정책을 적용한다.
