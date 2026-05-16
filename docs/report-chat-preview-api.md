# 교사 채팅방 미리보기 API 명세

## 개요

- PDF 생성 전 채팅 메시지를 미리보기로 조회하는 API
- 엔드포인트: `GET /reports/chat-rooms/{chatRoomId}/preview`

## 인증/권한

- `Authorization: Bearer {accessToken}` 헤더 필수
- 현재 세션의 `activeRole`이 `TEACHER`여야 함
- 사용자 계정이 실제로 `TEACHER` 역할을 보유해야 함
- 요청 교사가 해당 채팅방 참여자가 아니면 `403 Forbidden`

## 요청

### Path Variables

- `chatRoomId` (`Long`): 조회할 채팅방 ID

### Query Params

- `cursor` (`Long`, optional): 다음 페이지 조회용 커서(`messageId`)
- `size` (`int`, optional, default=`30`): 조회 개수 (`1~100`)

### Headers

- `Authorization`: `Bearer {JWT}`

## 동작 규칙

- 메시지 조회는 `message.id DESC` 기준
- 응답 메시지 목록은 UI 편의를 위해 오름차순으로 반환
- `nextCursor`는 현재 응답 메시지 중 가장 오래된 메시지의 `messageId`
- 다음 페이지는 `cursor={nextCursor}`로 조회
- `hasNext=true`이면 더 오래된 메시지가 존재

## 응답

### 성공 (200)

```json
{
  "success": true,
  "code": 200,
  "message": "채팅 미리보기 조회에 성공했습니다.",
  "data": {
    "chatRoomId": 15,
    "messages": [
      {
        "messageId": 101,
        "isMine": false,
        "content": "학생의 교실 환경 관련 문의드립니다.",
        "createdAt": "2026-04-21T09:50:00"
      },
      {
        "messageId": 102,
        "isMine": true,
        "content": "확인했습니다. 증빙 서류 제출 부탁드립니다.",
        "createdAt": "2026-04-21T09:50:30"
      }
    ],
    "nextCursor": 101,
    "hasNext": true
  }
}
```

## 필드 설명

- `data.chatRoomId`: 채팅방 ID
- `data.messages`: 메시지 목록
- `data.messages[].messageId`: 메시지 ID
- `data.messages[].isMine`: 로그인 사용자 발신 여부
- `data.messages[].content`: 메시지 본문
- `data.messages[].createdAt`: 생성 시각
- `data.nextCursor`: 다음 조회용 커서
- `data.hasNext`: 다음 페이지 존재 여부

## 오류 응답

- `400 Bad Request`
  - `chatRoomId`가 1 미만
  - `size`가 1~100 범위를 벗어남
  - `cursor`가 1 미만
- `401 Unauthorized`
  - Authorization 헤더 누락/토큰 오류
- `403 Forbidden`
  - 현재 세션의 `activeRole`이 `TEACHER`가 아님
  - 사용자 계정에 `TEACHER` 역할이 없음
  - 채팅방 참여자가 아님
- `404 Not Found`
  - 채팅방이 없음
