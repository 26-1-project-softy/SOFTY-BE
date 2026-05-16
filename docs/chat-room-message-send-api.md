# 채팅 메시지 전송 API

- 엔드포인트: `POST /chat-rooms/{chatRoomId}/messages`
- 목적: 학부모가 채팅방 상세 화면에서 일반 텍스트 메시지를 전송한다.
- 현재 세션의 `activeRole`이 `PARENT`여야 함
- 부수 동작:
  - 메시지를 저장한다.
  - 상대방의 안 읽은 메시지 수를 증가시킨다.
  - 채팅방의 마지막 메시지 표시 대상이 갱신된다.

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
  "content": "안녕하세요 선생님"
}
```

## 요청 필드

- `content` (String, 필수)
  - 전송할 메시지 내용
  - 공백만 있는 값은 허용하지 않음

## 처리 규칙

1. JWT로 로그인한 사용자를 확인한다.
2. 로그인 사용자가 학부모(`PARENT`) 또는 교사(`TEACHER`)인지 확인한다.
3. 해당 사용자가 `chatRoomId` 채팅방 참여자인지 확인한다.
4. 요청 본문의 `content`를 검증한다.
5. 채팅방 상태가 `COMPLETED`면 메시지 전송을 거부한다.
6. 메시지를 저장한다.
7. 전송자 본인의 `lastReadAt`은 현재 시각으로 갱신하고 `unreadCount`는 0으로 유지한다.
8. 상대방의 `unreadCount`를 증가시킨다.
9. 생성된 메시지 정보를 반환한다.

## 응답

### 성공

```json
{
  "success": true,
  "code": 201,
  "message": "메시지가 전송되었습니다.",
  "data": {
    "messageId": 101,
    "roomId": 1,
    "content": "안녕하세요 선생님",
    "createdAt": "2026-05-04T10:30:00"
  }
}
```

## 응답 필드

- `data.messageId` (Long)
  - 생성된 메시지 ID
- `data.roomId` (Long)
  - 채팅방 ID
- `data.content` (String)
  - 저장된 메시지 내용
- `data.createdAt` (String, ISO-8601)
  - 메시지 생성 시각

## 오류 응답

- `400 Bad Request`
  - 요청 본문이 없거나 `content`가 비어 있는 경우
- `401 Unauthorized`
  - JWT가 없거나 유효하지 않은 경우
- `403 Forbidden`
  - 학부모/교사 계정이 아니거나 해당 채팅방 참여자가 아닌 경우
  - 완료된 채팅방인 경우
- `404 Not Found`
  - 채팅방을 찾을 수 없는 경우

## 비고

- 종이비행기 버튼 클릭 시 이 API를 호출한다.
- 메시지 전송 성공 후 클라이언트는 응답의 `data`를 사용해 채팅 말풍선을 즉시 화면에 반영할 수 있다.
