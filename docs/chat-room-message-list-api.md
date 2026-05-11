# 채팅 메시지 목록 조회 API 명세

## 개요
- 로그인한 사용자가 참여 중인 특정 채팅방의 메시지 목록을 조회하는 API
- 엔드포인트: `GET /chat-rooms/{chatRoomId}/messages`
- 호출 가능 사용자: 학부모(`PARENT`), 교사(`TEACHER`)
- 페이지 방식: 커서 기반

## 인증/권한
- `Authorization: Bearer {accessToken}` 헤더 필수
- 학부모(`role=PARENT`) 또는 교사(`role=TEACHER`)만 조회 가능
- 로그인한 사용자가 해당 채팅방 참여자가 아니면 `403 Forbidden`

## 요청
### Headers
- `Authorization`: `Bearer {JWT}`

### Path Variables
- `chatRoomId` (Long)
  - 조회할 채팅방 ID

### Query Params
- `cursor` (Long, optional)
  - 다음 목록 조회용 커서
  - 첫 조회 시 전달하지 않음
  - 이전 응답의 `data.nextCursor` 값을 그대로 전달
- `size` (int, optional, default=`30`)
  - 한 번에 조회할 메시지 개수
  - 허용 범위: `1~100`

### 요청 예시
```http
GET /chat-rooms/15/messages?size=30
Authorization: Bearer {JWT}
```

```http
GET /chat-rooms/15/messages?cursor=102&size=30
Authorization: Bearer {JWT}
```

## 처리 규칙
1. JWT로 로그인한 사용자를 확인한다.
2. 로그인한 사용자의 역할이 `PARENT` 또는 `TEACHER`인지 확인한다.
3. 로그인한 사용자가 해당 채팅방의 참여자인지 확인한다.
4. 메시지는 최신 메시지부터 조회한 뒤, 응답에서는 오래된 순서부터 보이도록 오름차순으로 정렬해 반환한다.
5. `cursor`는 이전 응답에서 가장 오래된 메시지의 `messageId`이다.
6. 다음 조회 시에는 `cursor`보다 더 오래된 메시지만 조회한다.
7. `isMine`은 로그인한 사용자가 보낸 메시지인지 여부이다.
8. `senderName`, `senderRole`은 메시지 발신자 정보이다.
9. `isUnreadByCounterpart`는 "내가 보낸 메시지 중 상대방이 아직 읽지 않은 마지막 메시지 1건"에만 `true`로 내려간다.
10. `isUnreadByCounterpart` 계산은 상대방 참여자의 `chat_room_user_map.last_read_at` 기준으로 처리한다.
11. 내가 보낸 메시지라도 상대방이 이미 읽었거나, 마지막 미읽음 메시지가 아니면 `isUnreadByCounterpart=false`이다.

## 응답
### 성공 (200)
```json
{
  "success": true,
  "code": 200,
  "message": "채팅 메시지 조회에 성공했습니다.",
  "data": {
    "chatRoomId": 15,
    "messages": [
      {
        "messageId": 101,
        "isMine": false,
        "senderName": "김민수",
        "senderRole": "PARENT",
        "content": "선생님, 오늘 철수가 병원 진료가 있어 조금 늦을 것 같습니다.",
        "createdAt": "2026-02-07T13:19:00",
        "isUnreadByCounterpart": false
      },
      {
        "messageId": 102,
        "isMine": true,
        "senderName": "박선영",
        "senderRole": "TEACHER",
        "content": "안녕하세요. 철수 어머님, 철수의 결석 사유를 확인했습니다.",
        "createdAt": "2026-02-07T13:21:00",
        "isUnreadByCounterpart": true
      }
    ],
    "nextCursor": 101,
    "hasNext": true
  }
}
```

### 응답 필드 설명
- `data.chatRoomId` (Long): 채팅방 ID
- `data.messages`: 메시지 목록
  - `messageId` (Long): 메시지 ID
  - `isMine` (boolean): 현재 로그인한 사용자가 보낸 메시지 여부
  - `senderName` (String): 발신자 이름
  - `senderRole` (String): 발신자 역할
  - `content` (String): 메시지 내용
  - `createdAt` (LocalDateTime): 메시지 전송 시각
  - `isUnreadByCounterpart` (boolean): 상대방이 아직 읽지 않은 마지막 내 메시지 여부
- `data.nextCursor` (Long, nullable): 다음 조회에 사용할 커서
- `data.hasNext` (boolean): 다음 조회 가능 여부

## 오류 응답
- `400 Bad Request`
  - `chatRoomId`가 1 미만
  - `cursor`가 1 미만
  - `size`가 1~100 범위를 벗어남
- `401 Unauthorized`
  - Authorization 헤더 누락 또는 토큰 오류
- `403 Forbidden`
  - 학부모/교사 계정이 아님
  - 해당 채팅방 참여자가 아님
- `404 Not Found`
  - 채팅방이 존재하지 않음

## 비고
- 이 API는 상세 화면의 메시지 본문 조회용이다.
- 모바일/웹의 무한 스크롤 및 더보기 방식에서 동일하게 사용할 수 있다.
- 읽음 처리는 별도 API `POST /chat-rooms/{chatRoomId}/read`로 수행한다.
- 프론트는 `isUnreadByCounterpart=true`인 메시지에만 `1` 표시를 붙이면 된다.
