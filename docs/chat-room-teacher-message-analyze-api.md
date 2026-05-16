# 교사 메시지 분석 API

- 엔드포인트: `POST /chat-rooms/{chatRoomId}/teacher-messages/analyze`
- 현재 세션의 `activeRole`이 `TEACHER`여야 함
- 목적: 교사가 전송 전 작성한 메시지를 AI가 분석하여 분쟁 가능성과 추천 문장을 반환한다.
- 부수 동작:
  - 분석 결과를 `message_analysis` 테이블에 저장한다.
  - 채팅 메시지는 아직 저장하지 않는다.

## 요청

- 헤더
  - `Authorization: Bearer {JWT}`
  - `Content-Type: application/json`
- Path Variable
  - `chatRoomId` (Long, 필수)
- Body

```json
{
  "content": "안녕하세요, 철수 어머님. 철수의 결석 사유를 확인했습니다."
}
```

## 응답

```json
{
  "success": true,
  "code": 200,
  "message": "메시지 분석이 완료되었습니다.",
  "data": {
    "analysisId": 55,
    "riskLevel": "UNSAFE",
    "recommendedMessage": "조금 더 신중하게 생각해보면 좋을 것 같아요."
  }
}
```

## 비고

- `riskLevel`이 `UNSAFE`일 때만 AI 추천 문장을 생성한다.
- 반환된 `analysisId`는 교사 최종 메시지 전송 API에서 사용한다.
