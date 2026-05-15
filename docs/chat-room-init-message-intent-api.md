# 초기 문의 의도 분석 API

- 엔드포인트: `POST /chat-rooms/init-messages`
- 현재 세션의 `activeRole`이 `PARENT`여야 함
- 목적: 학부모가 작성 중인 첫 문의 메시지의 의도를 AI로 분석해 반환
- 부수 동작: 없음
  채팅방 생성, 메시지 저장, 참여자 매핑 생성은 수행하지 않음

## 요청

- 헤더
  - `Authorization: Bearer {JWT}`
- 본문 (`application/json`)

```json
{
  "content": "선생님, 내일 결석하게 되어 문의드립니다."
}
```

## 요청 필드

- `content` (String, 필수)
  - 의도 분석 대상이 되는 문의 내용
  - 공백만 있는 값은 허용하지 않음

## 처리 규칙

1. JWT로 로그인 사용자를 확인한다.
2. 로그인 사용자가 학부모(`PARENT`)인지 확인한다.
3. 해당 학부모와 연결된 자녀 1명, 담당 교사 1명이 존재하는지 확인한다.
4. AI 서버 `POST /ai/inference/classify-intent`로 `content`를 전달한다.
5. AI 응답의 `intent`를 `intentLabel`로 변환해 반환한다.
6. AI 호출 실패 또는 응답 누락 시 `intentLabel`은 `null`로 반환한다.

## 응답

### 1. 의도 분석 성공

```json
{
  "success": true,
  "code": 200,
  "message": "의도 분석에 성공했습니다.",
  "data": {
    "intentLabel": "문의"
  }
}
```

### 2. AI 실패 포함 정상 처리

```json
{
  "success": true,
  "code": 200,
  "message": "의도 분석을 완료했습니다.",
  "data": {
    "intentLabel": null
  }
}
```

## 응답 필드

- `data.intentLabel` (String, nullable)
  - AI가 분석한 의도 태그

## 오류 응답

- `400 Bad Request`
  - 요청 본문이 없거나 `content`가 비어 있는 경우
- `403 Forbidden`
  - 학부모 계정이 아닌 경우
- `404 Not Found`
  - 사용자, 연결된 자녀, 담당 교사 정보를 찾을 수 없는 경우

## 비고

- 현재 MVP 정책상 학부모는 자녀 1명, 담당 교사 1명과 연결된 것으로 가정한다.
- 교사 근무시간 여부는 별도 API `GET /chat-rooms/working-hours`에서 조회한다.
