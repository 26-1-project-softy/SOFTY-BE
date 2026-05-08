# 관리자 LLM 토큰 사용량 조회 API 명세

## 개요
- 관리자 대시보드에서 LLM 토큰 사용량 통계를 조회하기 위한 API
- 엔드포인트: `GET /admin/statistics/token-usage`
- 내부적으로 AI 서버 `GET /ai/token-usage`를 호출해 토큰 사용량 집계 결과를 가져온다

## 인증/권한
- `Authorization: Bearer {accessToken}` 헤더 필수
- 관리자(`role=ADMIN`)만 조회 가능

## 요청
### Headers
- `Authorization`: `Bearer {JWT}`

### Query Params
- 없음

## 동작 규칙
- AI 서버 호출 경로: `{AI_SERVER_BASE_URL}/ai/token-usage`
- BE는 별도 DB 조회 없이 AI 서버 응답을 기반으로 프론트 응답을 구성한다
- `totalUsage`, `details` 값은 AI 서버 응답 의미를 그대로 사용한다
- AI 서버 응답이 비어 있거나 파싱할 수 없으면 `502 Bad Gateway`를 반환한다
- AI 서버 연결 또는 응답 시간이 초과되면 `504 Gateway Timeout`을 반환한다

## 응답
### 성공 (200)
```json
{
  "success": true,
  "code": 200,
  "message": "토큰 사용량 조회에 성공했습니다.",
  "data": {
    "totalUsage": {
      "inputTokens": 9106,
      "outputTokens": 205,
      "totalTokens": 9311
    },
    "details": [
      {
        "modelName": "classify-intent",
        "inputTokens": 8996,
        "outputTokens": 174,
        "totalTokens": 9170
      },
      {
        "modelName": "recommend-alternative",
        "inputTokens": 110,
        "outputTokens": 31,
        "totalTokens": 141
      }
    ]
  }
}
```

### 응답 필드 설명
- `success`: API 처리 성공 여부
- `code`: HTTP 상태 코드
- `message`: 처리 결과 메시지
- `data.totalUsage`: 전체 토큰 사용량 집계
  - `inputTokens`: 전체 입력 토큰 수
  - `outputTokens`: 전체 출력 토큰 수
  - `totalTokens`: 전체 합산 토큰 수
- `data.details`: 엔드포인트별 토큰 사용량 목록
  - `modelName`: 프론트에 노출할 모델/기능 이름
  - `inputTokens`: 해당 엔드포인트 입력 토큰 수
  - `outputTokens`: 해당 엔드포인트 출력 토큰 수
  - `totalTokens`: 해당 엔드포인트 합산 토큰 수

## AI 서버 응답 매핑
- `total_usage.input_tokens` -> `totalUsage.inputTokens`
- `total_usage.output_tokens` -> `totalUsage.outputTokens`
- `total_usage.total_tokens` -> `totalUsage.totalTokens`
- `details[].endpoint` -> `details[].modelName`
- `details[].input_tokens` -> `details[].inputTokens`
- `details[].output_tokens` -> `details[].outputTokens`
- `details[].total_tokens` -> `details[].totalTokens`

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
- 현재 화면에서는 `totalUsage`를 요약 카드에 사용하고, `details`를 엔드포인트별 상세 표시에 사용할 수 있다.
- `modelName`에는 현재 AI 서버가 내려준 `endpoint` 값(`classify-intent`, `recommend-alternative` 등)을 그대로 사용한다.
