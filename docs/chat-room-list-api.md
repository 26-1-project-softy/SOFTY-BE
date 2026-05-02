# 채팅방 목록 조회 API 명세

## 개요
- 로그인한 사용자가 참여 중인 채팅방 목록을 조회하는 API
- 엔드포인트: `GET /chat-rooms`
- 대상 사용자: 학부모(`PARENT`), 교사(`TEACHER`)
- 페이징 방식: 커서 기반

## 인증/권한
- `Authorization: Bearer {accessToken}` 헤더 필수
- 학부모(`role=PARENT`)와 교사(`role=TEACHER`)만 조회 가능
- 그 외 권한은 `403 Forbidden`

## 요청
### Headers
- `Authorization`: `Bearer {JWT}`

### Query Params
- `cursor` (Long, optional)
  - 다음 목록 조회용 커서
  - 첫 조회 시 전달하지 않는다
  - 이전 응답의 `data.nextCursor` 값을 그대로 전달한다
- `size` (int, optional, default=`20`)
  - 한 번에 조회할 채팅방 개수
  - 허용 범위는 `1~100`

### 요청 예시
```http
GET /chat-rooms?size=20
Authorization: Bearer {JWT}
```

```http
GET /chat-rooms?cursor=15&size=20
Authorization: Bearer {JWT}
```

## 처리 규칙
1. JWT로 로그인한 사용자를 확인한다.
2. 로그인한 사용자의 역할이 `PARENT` 또는 `TEACHER`인지 확인한다.
3. 로그인한 사용자가 참여한 채팅방만 조회한다.
4. 목록은 최신 메시지 시각 기준 내림차순으로 정렬한다.
   - 메시지가 있으면 마지막 메시지의 생성 시각을 사용한다.
   - 메시지가 없으면 채팅방 생성 시각을 사용한다.
5. `cursor`는 이전 응답의 마지막 채팅방 ID이다.
6. 다음 조회 시에는 `cursor`에 해당 채팅방보다 더 오래된 항목만 조회한다.
   - 비교 기준은 `(lastMessageAt DESC, chatRoomId DESC)`이다.
7. `counterpartName`은 로그인한 사용자의 상대방 이름이다.
   - 학부모 로그인 시: 담임 교사 이름
   - 교사 로그인 시: 학부모 이름
8. `studentName`은 해당 채팅방과 연결된 학생 이름이다.
9. `unreadCount`는 현재 로그인한 사용자의 읽지 않은 메시지 수이다.
10. `status`는 `chat_room.status` 값을 그대로 사용한다.
   - 현재 사용 가능한 값: `IN_PROGRESS`, `COMPLETED`
11. `intentLabel`은 채팅방 생성 시 저장된 문의 의도 태그를 내려준다.

## 응답
### 성공 (200)
```json
{
  "success": true,
  "code": 200,
  "message": "채팅방 목록 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "chatRoomId": 15,
        "counterpartName": "김민수 학부모",
        "studentName": "김철수",
        "lastMessage": "선생님, 내일 철수가 병원 진료가 있어 조금 늦을 것 같습니다.",
        "lastMessageAt": "2026-03-24T10:30:00",
        "unreadCount": 2,
        "status": "IN_PROGRESS",
        "intentLabel": "결석/지각"
      },
      {
        "chatRoomId": 12,
        "counterpartName": "홍길동 선생님",
        "studentName": "김철수",
        "lastMessage": "확인했습니다. 필요한 서류가 있으면 안내드리겠습니다.",
        "lastMessageAt": "2026-03-23T16:10:00",
        "unreadCount": 0,
        "status": "COMPLETED",
        "intentLabel": "상담"
      }
    ],
    "size": 20,
    "nextCursor": 12,
    "hasNext": true
  }
}
```

### 응답 필드 설명
- `data.content`: 채팅방 목록
  - `chatRoomId` (Long): 채팅방 ID
  - `counterpartName` (String): 상대방 이름
  - `studentName` (String): 학생 이름
  - `lastMessage` (String): 마지막 메시지 내용
  - `lastMessageAt` (LocalDateTime): 마지막 메시지 시각
  - `unreadCount` (int): 현재 로그인한 사용자의 안 읽은 메시지 수
  - `status` (String): 채팅방 처리 상태
  - `intentLabel` (String): 문의 의도 태그
- `data.size` (int): 요청한 조회 개수
- `data.nextCursor` (Long, nullable): 다음 조회에 사용할 커서
- `data.hasNext` (boolean): 다음 조회 가능 여부

## 오류 응답
- `400 Bad Request`
  - `cursor`가 1 미만
  - `size`가 1~100 범위를 벗어남
- `401 Unauthorized`
  - Authorization 헤더 누락 또는 토큰 오류
- `403 Forbidden`
  - 학부모/교사 계정이 아님

## 비고
- 목록 조회 API는 학부모와 교사가 공통으로 사용한다.
- UI의 시간 표시(`오후 9:30`, `어제`)는 프론트엔드에서 `lastMessageAt`을 기준으로 가공한다.
- 모바일 앱의 무한 스크롤과 웹의 더보기 방식에서 동일하게 사용할 수 있다.
