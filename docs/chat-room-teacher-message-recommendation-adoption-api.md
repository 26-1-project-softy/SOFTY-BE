# 교사 추천문장 적용 기록 API

- 엔드포인트: `POST /teacher-message-analyses/{analysisId}/recommendation-adoption`
- 목적: 교사가 AI 추천문장을 입력창에 적용했다는 행위를 기록한다.
- 부수 동작:
  - 해당 분석 결과의 추천문장 적용 이력을 저장한다.
  - 아직 실제 메시지를 전송하지는 않는다.

## 요청

### 헤더

- `Authorization: Bearer {JWT}`

### Path Variable

- `analysisId` (Long, 필수)
  - 추천문장 적용 대상이 되는 메시지 분석 결과 ID

### Body

```json
{}
```

## 처리 규칙

1. JWT로 로그인한 사용자를 확인한다.
2. 로그인 사용자가 교사(`TEACHER`)인지 확인한다.
3. `analysisId`에 해당하는 메시지 분석 결과가 존재하는지 확인한다.
4. 해당 분석 결과의 작성자와 로그인 교사가 동일한지 확인한다.
5. 해당 분석 결과에 추천문장이 존재하는지 확인한다.
6. 추천문장 적용 이력을 저장한다.
7. 저장 결과를 반환한다.

## 응답

### 성공

```json
{
  "success": true,
  "code": 200,
  "message": "추천문장 적용이 저장되었습니다."
}
```

## 오류 응답

- `400 Bad Request`
  - 추천문장이 없는 분석 결과에 대해 적용 요청한 경우
- `401 Unauthorized`
  - JWT가 없거나 유효하지 않은 경우
- `403 Forbidden`
  - 교사 계정이 아니거나 본인 분석 결과가 아닌 경우
- `404 Not Found`
  - `analysisId`에 해당하는 분석 결과가 없는 경우

## 비고

- 이 API는 추천문장을 실제 메시지로 전송하는 API가 아니다.
- 이 API는 "추천문장을 입력창에 적용했다"는 행위를 기록하는 용도이다.
- 피드백 API와 마찬가지로 대상 식별자는 `messageId`가 아니라 `analysisId`이다.
