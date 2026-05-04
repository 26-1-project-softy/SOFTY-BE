# 교사 최종 메시지 전송 API

- 엔드포인트: `POST /chat-rooms/{chatRoomId}/teacher-messages`
- 목적: 교사가 AI 분석 결과를 참고해 최종 메시지를 전송한다.
- 부수 동작:
  - 최종 메시지를 저장한다.
  - 분석 결과와 최종 전송 메시지를 연결한다.
  - 필요 시 추천문장 채택 여부를 함께 기록한다.
  - 상대방의 안 읽은 메시지 수를 증가시킨다.

## 요청

### 헤더

- `Authorization: Bearer {JWT}`
- `Content-Type: application/json`

### Path Variable

- `chatRoomId` (Long, 필수)
  - 메시지를 전송할 채팅방 ID

### Body

```json
{
  "analysisId": 201,
  "content": "어머님 그건 아닙니다."
}
```

## 요청 필드

- `analysisId` (Long, 필수)
  - 이번 최종 메시지 전송의 기준이 되는 분석 결과 ID
- `content` (String, 필수)
  - 최종 전송할 메시지 내용
  - 공백만 있는 값은 허용하지 않음

## 처리 규칙

1. JWT로 로그인한 사용자를 확인한다.
2. 로그인 사용자가 교사(`TEACHER`)인지 확인한다.
3. 해당 사용자가 `chatRoomId` 채팅방 참여자인지 확인한다.
4. 요청 본문의 `analysisId`와 `content`를 검증한다.
5. `analysisId`에 해당하는 메시지 분석 결과가 존재하는지 확인한다.
6. 해당 분석 결과의 작성자와 로그인 교사가 동일한지 확인한다.
7. 해당 분석 결과가 현재 `chatRoomId`와 연결된 분석 결과인지 확인한다.
8. 최종 메시지를 저장한다.
9. 분석 결과의 추천문장과 최종 전송 문장을 비교해 추천문장 채택 여부를 기록한다.
10. 분석 결과와 최종 전송 메시지를 연결할 수 있도록 관련 이력을 저장한다.
11. 상대방의 `unreadCount`를 증가시킨다.
12. 생성된 메시지 정보를 반환한다.

## 응답

### 성공

```json
{
  "success": true,
  "code": 201,
  "message": "메시지가 전송되었습니다.",
  "data": {
    "messageId": 2,
    "roomId": 1
  }
}
```

## 응답 필드

- `data.messageId` (Long)
  - 생성된 메시지 ID
- `data.roomId` (Long)
  - 채팅방 ID

## 오류 응답

- `400 Bad Request`
  - 요청 본문이 없거나 `content`가 비어 있는 경우
  - `analysisId`가 잘못된 경우
- `401 Unauthorized`
  - JWT가 없거나 유효하지 않은 경우
- `403 Forbidden`
  - 교사 계정이 아니거나 해당 채팅방 참여자가 아닌 경우
  - 본인 분석 결과가 아닌 경우
- `404 Not Found`
  - 채팅방 또는 분석 결과를 찾을 수 없는 경우

## 비고

- 이 API는 기존 메시지를 수정하는 API가 아니다.
- 이 API는 교사가 최종적으로 전송할 새 메시지를 생성하는 API이다.
- 추천문장 적용 버튼을 눌렀더라도, 실제 최종 전송 문장이 추천문장과 다를 수 있다.
- 따라서 추천문장 채택 여부는 최종 전송 시점의 `content`를 기준으로 판단한다.
