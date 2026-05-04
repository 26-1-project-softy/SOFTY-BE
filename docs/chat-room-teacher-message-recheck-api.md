# 교사 수정 메시지 재분석 API

- 엔드포인트: `POST /message-analyses/{analysisId}/recheck`
- 목적: 교사가 추천문장을 수정한 뒤, 최종 전송 전에 수정된 문장을 다시 AI로 분석한다.
- 부수 동작:
  - 수정된 문장을 기준으로 분쟁 가능성을 다시 분석한다.
  - 필요하면 새로운 추천문장을 다시 생성한다.
  - 재분석 결과를 새로운 `message_analysis` 데이터로 저장한다.

## 요청

### 헤더

- `Authorization: Bearer {JWT}`
- `Content-Type: application/json`

### Path Variable

- `analysisId` (Long, 필수)
  - 재분석의 기준이 되는 기존 메시지 분석 결과 ID

### Body

```json
{
  "content": "안녕하세요, 철수 어머님. 철수 결석 사유는 확인했습니다. 관련 서류는 등교 후 전달 부탁드립니다."
}
```

## 요청 필드

- `content` (String, 필수)
  - 교사가 추천문장을 수정한 뒤 다시 분석받을 최종 후보 메시지
  - 공백만 있는 값은 허용하지 않음

## 처리 규칙

1. JWT로 로그인한 사용자를 확인한다.
2. 로그인 사용자가 교사(`TEACHER`)인지 확인한다.
3. `analysisId`에 해당하는 기존 메시지 분석 결과가 존재하는지 확인한다.
4. 해당 분석 결과의 작성자와 로그인 교사가 동일한지 확인한다.
5. 요청 본문의 `content`를 검증한다.
6. 수정된 문장을 기준으로 AI 분쟁 가능성 분석 API를 다시 호출한다.
7. 분석 결과가 `UNSAFE`이면 추천문장 생성 API를 다시 호출한다.
8. 재분석 결과를 새로운 `message_analysis` 데이터로 저장한다.
9. 새 `analysisId`, `riskLevel`, `recommendedMessage`를 반환한다.

## 응답

### 1. 안전한 메시지인 경우

```json
{
  "success": true,
  "code": 200,
  "message": "메시지 재분석이 완료되었습니다.",
  "data": {
    "analysisId": 305,
    "riskLevel": "SAFE",
    "recommendedMessage": null
  }
}
```

### 2. 위험 가능성이 있는 메시지인 경우

```json
{
  "success": true,
  "code": 200,
  "message": "메시지 재분석이 완료되었습니다.",
  "data": {
    "analysisId": 306,
    "riskLevel": "UNSAFE",
    "recommendedMessage": "조금 더 부드럽고 오해가 적은 표현으로 바꿔보는 것을 권장드립니다."
  }
}
```

## 응답 필드

- `data.analysisId` (Long)
  - 재분석 결과로 새로 생성된 분석 결과 ID
- `data.riskLevel` (String)
  - AI가 재판단한 분쟁 가능성 결과
  - `SAFE`, `UNSAFE`
- `data.recommendedMessage` (String, nullable)
  - 위험 가능성이 있을 때 AI가 추천한 대체 문장
  - `SAFE`인 경우 `null`

## 오류 응답

- `400 Bad Request`
  - 요청 본문이 없거나 `content`가 비어 있는 경우
- `401 Unauthorized`
  - JWT가 없거나 유효하지 않은 경우
- `403 Forbidden`
  - 교사 계정이 아니거나 본인 분석 결과가 아닌 경우
- `404 Not Found`
  - `analysisId`에 해당하는 기존 분석 결과가 없는 경우
- `502 Bad Gateway`
  - AI 서버 호출에 실패한 경우

## 비고

- 이 API는 기존 `analysisId`를 수정하지 않고, 항상 새로운 분석 결과를 생성한다.
- 따라서 교사가 문장을 여러 번 수정하면 `message_analysis` 데이터도 여러 건 누적될 수 있다.
- 이후 최종 전송 API는 가장 마지막에 선택한 `analysisId`를 기준으로 호출한다.
