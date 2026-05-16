# 첫 메시지 최종 전송 API

- 엔드포인트: `POST /chat-rooms/init-messages/send`
- 목적: 학부모가 작성한 첫 문의를 최종 전송하고 새 채팅방과 첫 메시지를 생성
- 현재 세션의 `activeRole`이 `PARENT`여야 함

## 요청

- 헤더
  - `Authorization: Bearer {JWT}`
- 본문 (`application/json`)

```json
{
  "content": "선생님, 내일 수학여행 관련 공지 있어요 조금 늦을 것 같습니다. 혹시 결석 처리나 준비물, 필요한 서류가 있는지 궁금합니다.",
  "intentLabel": "결석/지각"
}
```

## 요청 필드

- `content` (String, 필수)
  - 첫 문의 메시지 내용
- `intentLabel` (String, 필수)
  - 사용자가 최종 확정한 의도 태그
  - AI 추천값 그대로일 수도 있고, 사용자가 직접 수정한 값일 수도 있음

## 처리 규칙

1. JWT로 로그인한 사용자를 확인한다.
2. 로그인 사용자가 학부모(`PARENT`)인지 확인한다.
3. 해당 학부모와 연결된 자녀 1명, 담당 교사 1명이 존재하는지 확인한다.
4. 새 `chat_room`을 생성한다.
5. `chat_room.intent_label`에 `intentLabel`을 저장한다.
6. 부모와 교사를 `chat_room_user_map`에 참여자로 저장한다.
7. 첫 `message`를 저장한다.
8. 생성된 `chatRoomId`, `messageId`를 반환한다.

## 응답

```json
{
  "success": true,
  "code": 201,
  "message": "첫 메시지가 전송되었습니다.",
  "data": {
    "chatRoomId": 1,
    "messageId": 1
  }
}
```

## 오류 응답

- `400 Bad Request`
  - 요청 본문이 없거나 `content`, `intentLabel`이 비어 있는 경우
- `403 Forbidden`
  - 학부모 계정이 아닌 경우
- `404 Not Found`
  - 사용자, 연결된 자녀, 담당 교사 정보를 찾을 수 없는 경우

## 비고

- 현재 MVP 정책상 학부모는 자녀 1명, 담당 교사 1명과 연결된 것으로 가정한다.
- 기존 AI 의도 분석 API(`POST /chat-rooms/init-messages`)에서 받은 의도를 프론트에서 수정한 뒤 최종값을 이 API로 전송한다.
