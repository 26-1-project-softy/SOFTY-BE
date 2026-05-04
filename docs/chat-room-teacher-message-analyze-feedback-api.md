# 교사 메시지 분석 피드백 저장 API

- 엔드포인트: `PUT /message-analyses/{analysisId}/feedback`
- 목적: 교사가 AI 메시지 분석 결과에 대해 1~5점 피드백을 남긴다.
- 부수 동작:
  - 해당 분석 결과의 피드백 점수를 저장한다.
  - 이미 저장된 피드백이 있으면 새 점수로 수정한다.

## 요청

### 헤더

- `Authorization: Bearer {JWT}`
- `Content-Type: application/json`

### Path Variable

- `analysisId` (Long, 필수)
  - 피드백 대상이 되는 메시지 분석 결과 ID

### Body

```json
{
  "score": 5
}
```

## 요청 필드

- `score` (Integer, 필수)
  - 분쟁 가능성 분석 결과에 대한 만족도 점수
  - 허용 범위: `1` ~ `5`

## 처리 규칙

1. JWT로 로그인한 사용자를 확인한다.
2. 로그인 사용자가 교사(`TEACHER`)인지 확인한다.
3. `analysisId`에 해당하는 메시지 분석 결과가 존재하는지 확인한다.
4. 해당 분석 결과의 작성자와 로그인 교사가 동일한지 확인한다.
5. 요청 본문의 `score`가 `1`~`5` 범위인지 검증한다.
6. 기존 피드백이 없으면 새로 저장한다.
7. 기존 피드백이 있으면 새 점수로 수정한다.
8. 저장 결과를 반환한다.

## 응답

### 성공

```json
{
  "success": true,
  "code": 200,
  "message": "피드백이 저장되었습니다."
}
```

## 오류 응답

- `400 Bad Request`
  - 요청 본문이 없거나 `score`가 허용 범위를 벗어난 경우
- `401 Unauthorized`
  - JWT가 없거나 유효하지 않은 경우
- `403 Forbidden`
  - 교사 계정이 아니거나 본인 분석 결과가 아닌 경우
- `404 Not Found`
  - `analysisId`에 해당하는 분석 결과가 없는 경우

## 비고

- 이 API는 분석 결과 자체에 대한 사용자 평가를 저장하는 용도이다.
- 피드백 대상은 `messageId`가 아니라 `analysisId`이다.
- 같은 분석 결과에 대해 피드백은 1건만 유지되며, 다시 호출하면 점수가 갱신된다.
