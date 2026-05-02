# 채팅방 읽음 처리 API 명세

## 개요
- 로그인한 사용자가 특정 채팅방의 안 읽은 메시지를 모두 읽음 처리하는 API
- 엔드포인트: `POST /chat-rooms/{chatRoomId}/read`
- 대상 사용자: 학부모(`PARENT`), 교사(`TEACHER`)

## 인증/권한
- `Authorization: Bearer {accessToken}` 헤더 필수
- 학부모(`role=PARENT`)와 교사(`role=TEACHER`)만 호출 가능
- 로그인한 사용자가 해당 채팅방 참여자가 아니면 `403 Forbidden`

## 요청
### Headers
- `Authorization`: `Bearer {JWT}`

### Path Variables
- `chatRoomId` (Long)
  - 읽음 처리할 채팅방 ID

### Body
- 없음

### 요청 예시
```http
POST /chat-rooms/15/read
Authorization: Bearer {JWT}
```

## 처리 규칙
1. JWT로 로그인한 사용자를 확인한다.
2. 로그인한 사용자의 역할이 `PARENT` 또는 `TEACHER`인지 확인한다.
3. 로그인한 사용자가 해당 채팅방의 참여자인지 확인한다.
4. 해당 사용자의 `chat_room_user_map.unread_count`를 `0`으로 변경한다.
5. 해당 사용자의 `chat_room_user_map.last_read_at`을 현재 시각으로 갱신한다.
6. 이미 `unreadCount`가 `0`이어도 정상 처리한다.

## 응답
### 성공 (200)
```json
{
  "success": true,
  "code": 200,
  "message": "채팅방 읽음 처리에 성공했습니다.",
  "data": {
    "chatRoomId": 15,
    "unreadCount": 0,
    "lastReadAt": "2026-05-02T22:10:00"
  }
}
```

### 응답 필드 설명
- `data.chatRoomId` (Long): 채팅방 ID
- `data.unreadCount` (int): 읽음 처리 후 안 읽은 메시지 수
- `data.lastReadAt` (LocalDateTime): 읽음 처리 시각

## 오류 응답
- `400 Bad Request`
  - `chatRoomId`가 1 미만
- `401 Unauthorized`
  - Authorization 헤더 누락 또는 토큰 오류
- `403 Forbidden`
  - 학부모/교사 계정이 아님
  - 해당 채팅방 참여자가 아님
- `404 Not Found`
  - 채팅방이 존재하지 않음

## 비고
- 이 API는 상세 화면 진입 직후 또는 메시지 목록 조회 직후 호출할 수 있다.
- 읽음 처리 시 상대방의 `unreadCount`에는 영향을 주지 않는다.
- 이후 실시간 메시지 기능이 추가되더라도 동일한 읽음 처리 기준으로 확장할 수 있다.
