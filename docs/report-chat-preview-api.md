# 교사 채팅방 미리보기 API 명세

## 개요
- PDF 생성 전에 채팅방 대화 내용을 미리보기로 조회하는 API
- 엔드포인트: `GET /reports/chat-rooms/{chatRoomId}/preview`

## 인증/권한
- `Authorization: Bearer {accessToken}` 헤더 필수
- 교사(`role=TEACHER`) 계정만 조회 가능
- 요청한 교사가 해당 채팅방 참여자가 아니면 `403 Forbidden`

## 요청
### Path Variables
- `chatRoomId` (Long): 조회할 채팅방 ID

### Query Params
- `cursor` (Long, optional): 다음 페이지 조회용 커서(messageId). 미지정 시 최신 메시지부터 조회
- `size` (int, optional, default=`30`): 조회 개수 (`1~100`)

### Headers
- `Authorization`: `Bearer {JWT}`

## 동작 규칙
- 메시지 조회는 내부적으로 `message.id DESC` 기준으로 가져오고, 응답은 화면 렌더링 편의를 위해 시간 오름차순으로 반환
- `nextCursor`는 현재 응답에서 가장 오래된 메시지의 `messageId`
- 다음 페이지는 `cursor={nextCursor}`로 호출
- `hasNext=true`이면 더 오래된 메시지가 존재

## 응답
### 성공 (200)
```json
{
  "success": true,
  "code": 200,
  "message": "채팅방 미리보기 조회에 성공했습니다.",
  "data": {
    "chatRoomId": 15,
    "parentName": "김학부모",
    "studentName": "김학생",
    "intentLabel": "생활지도",
    "status": "OPEN",
    "messages": [
      {
        "messageId": 101,
        "senderId": 22,
        "senderRole": "PARENT",
        "isMine": false,
        "type": "TEXT",
        "content": "선생님 안녕하세요.",
        "createdAt": "2026-04-21T09:50:00"
      },
      {
        "messageId": 102,
        "senderId": 3,
        "senderRole": "TEACHER",
        "isMine": true,
        "type": "TEXT",
        "content": "안녕하세요. 문의 주신 내용 확인했습니다.",
        "createdAt": "2026-04-21T09:50:30"
      }
    ],
    "nextCursor": 101,
    "hasNext": true
  }
}
```

### 필드 설명
- `data.chatRoomId`: 채팅방 ID
- `data.parentName`: 채팅방의 최신 부모 참여자 이름
- `data.studentName`: 해당 부모에게 매핑된 최신 학생 이름
- `data.intentLabel`: 채팅방 의도 라벨
- `data.status`: 채팅방 상태
- `data.messages`: 메시지 목록(오름차순)
  - `messageId`: 메시지 ID
  - `senderId`: 발신자 ID
  - `senderRole`: 발신자 역할 (`TEACHER`, `PARENT` 등)
  - `isMine`: 로그인 사용자 발신 여부
  - `type`: 메시지 타입
  - `content`: 메시지 원문
  - `createdAt`: 생성 시각
- `data.nextCursor`: 다음 조회 커서
- `data.hasNext`: 다음 페이지 존재 여부

## 오류 응답
- `400 Bad Request`
  - `chatRoomId`가 1 미만
  - `size`가 1~100 범위를 벗어남
  - `cursor`가 1 미만
- `401 Unauthorized`
  - Authorization 헤더 누락/토큰 오류
- `403 Forbidden`
  - 교사 계정 아님
  - 채팅방 참여자가 아님
- `404 Not Found`
  - 채팅방 없음
