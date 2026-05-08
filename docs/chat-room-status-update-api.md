# 채팅방 처리 상태 변경 API

- 메서드와 경로: `PATCH /chat-rooms/{chatRoomId}/status`
- 목적: 교사가 채팅방 상세 상단 드롭다운에서 채팅방 처리 상태를 `처리중`, `완료` 중 하나로 변경한다.
- 변경 대상: `chat_room.status`

## 요청

- 헤더
  - `Authorization: Bearer {JWT}`
  - `Content-Type: application/json`

- Path Variable
  - `chatRoomId` (Long, 필수)
    - 상태를 변경할 채팅방 ID

- Body

```json
{
  "status": "COMPLETED"
}
```

## 요청 필드

- `status` (String, 필수)
  - 변경할 채팅방 처리 상태
  - 허용 값
    - `IN_PROGRESS`: 처리중
    - `COMPLETED`: 완료

## 처리 규칙

1. JWT로 로그인한 사용자를 확인한다.
2. 로그인 사용자가 교사(`TEACHER`)인지 확인한다.
3. 로그인 사용자가 해당 `chatRoomId` 채팅방 참여자인지 확인한다.
4. `chatRoomId`에 해당하는 채팅방이 존재하는지 확인한다.
5. 요청 본문의 `status`가 허용된 상태값인지 검증한다.
6. `chat_room.status`를 요청한 값으로 변경한다.
7. 같은 상태값으로 다시 요청해도 오류 없이 성공 응답을 반환한다.
8. 변경된 상태를 응답으로 반환한다.

## 응답

```json
{
  "success": true,
  "code": 200,
  "message": "채팅방 상태가 변경되었습니다.",
  "data": {
    "chatRoomId": 15,
    "status": "COMPLETED"
  }
}
```

## 응답 필드

- `data.chatRoomId` (Long)
  - 상태가 변경된 채팅방 ID
- `data.status` (String)
  - 변경 후 채팅방 처리 상태

## 오류 응답

- `400 Bad Request`
  - 요청 본문이 없거나 `status`가 비어 있는 경우
  - `status`가 허용되지 않은 값인 경우
  - `chatRoomId`가 1 미만인 경우
- `401 Unauthorized`
  - JWT가 없거나 유효하지 않은 경우
- `403 Forbidden`
  - 교사 계정이 아닌 경우
  - 해당 채팅방 참여자가 아닌 경우
- `404 Not Found`
  - 채팅방을 찾을 수 없는 경우

## 비고

- 프론트 표시 문구와 서버 상태값 매핑은 다음과 같다.
  - `처리중` -> `IN_PROGRESS`
  - `완료` -> `COMPLETED`
- 상태 변경 성공 후, 채팅방 상세 조회 API와 채팅방 목록 조회 API의 `status` 값에도 동일하게 반영된다.
