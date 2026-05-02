# 채팅방 상세 정보 조회 API 명세

## 개요
- 로그인한 사용자가 참여 중인 특정 채팅방의 상세 정보를 조회하는 API
- 엔드포인트: `GET /chat-rooms/{chatRoomId}`
- 대상 사용자: 학부모(`PARENT`), 교사(`TEACHER`)

## 인증/권한
- `Authorization: Bearer {accessToken}` 헤더 필수
- 학부모(`role=PARENT`)와 교사(`role=TEACHER`)만 조회 가능
- 로그인한 사용자가 해당 채팅방 참여자가 아니면 `403 Forbidden`

## 요청
### Headers
- `Authorization`: `Bearer {JWT}`

### Path Variables
- `chatRoomId` (Long)
  - 조회할 채팅방 ID

### 요청 예시
```http
GET /chat-rooms/15
Authorization: Bearer {JWT}
```

## 처리 규칙
1. JWT로 로그인한 사용자를 확인한다.
2. 로그인한 사용자의 역할이 `PARENT` 또는 `TEACHER`인지 확인한다.
3. 로그인한 사용자가 해당 채팅방의 참여자인지 확인한다.
4. 채팅방 기본 정보와 상대방 정보를 조회한다.
5. `counterpartName`은 로그인한 사용자의 상대방 이름이다.
   - 학부모 로그인 시: 담임 교사 이름
   - 교사 로그인 시: 학부모 이름
6. `studentName`은 해당 채팅방과 연결된 학생 이름이다.
7. `intentLabel`과 `status`는 채팅방 저장값을 그대로 내려준다.

## 응답
### 성공 (200)
```json
{
  "success": true,
  "code": 200,
  "message": "채팅방 상세 정보 조회에 성공했습니다.",
  "data": {
    "chatRoomId": 15,
    "counterpartName": "김민수 학부모",
    "studentName": "김철수",
    "intentLabel": "결석/지각",
    "status": "IN_PROGRESS"
  }
}
```

### 응답 필드 설명
- `data.chatRoomId` (Long): 채팅방 ID
- `data.counterpartName` (String): 상대방 이름
- `data.studentName` (String): 학생 이름
- `data.intentLabel` (String): 문의 의도 태그
- `data.status` (String): 채팅방 처리 상태

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
- 이 API는 상세 화면 상단 메타정보 조회용이다.
- 메시지 목록은 별도 API `GET /chat-rooms/{chatRoomId}/messages`로 조회한다.
