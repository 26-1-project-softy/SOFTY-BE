# 관리자 오류 검토 조회 API

- 엔드포인트: `GET /admin/risk-feedbacks`
- 목적: 관리자 화면에서 교사 메시지 분석 피드백 목록을 페이지 단위로 조회한다.

## 요청

### 헤더

- `Authorization: Bearer {JWT}`

### Query Parameter

- `page` (int, 선택)
  - 페이지 번호
  - 기본값: `1`
- `size` (int, 선택)
  - 페이지 크기
  - 기본값: `20`

## 응답

### 성공

```json
{
  "success": true,
  "code": 200,
  "message": "피드백 목록 조회에 성공했습니다.",
  "data": {
    "items": [
      {
        "feedbackId": 1,
        "teacherName": "김 선생",
        "feedbackResult": 5,
        "riskLevel": "UNSAFE",
        "originalMessage": "철수의 결석 사유를 확인했습니다.",
        "createdAt": "2026-03-24T10:30:00"
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 53,
    "totalPages": 3
  }
}
```

## 응답 필드

- `data.items[].feedbackId` (Long)
  - 리스크 피드백 ID
- `data.items[].teacherName` (String)
  - 교사 이름
- `data.items[].feedbackResult` (Integer)
  - 교사가 남긴 피드백 점수
- `data.items[].riskLevel` (String)
  - AI媛 ?먮떒??遺꾩웳 媛?μ꽦 寃곌낵
- `data.items[].originalMessage` (String)
  - AI가 분석한 교사 메시지 원문
- `data.items[].createdAt` (String)
  - 피드백 생성 시각
- `data.page` (int)
  - 현재 페이지 번호
- `data.size` (int)
  - 페이지 크기
- `data.totalElements` (long)
  - 전체 요소 수
- `data.totalPages` (int)
  - 전체 페이지 수

## 오류 응답

- `401 Unauthorized`
  - JWT가 없거나 유효하지 않은 경우
- `403 Forbidden`
  - 관리자 계정이 아닌 경우

## 비고

- 결과는 최신 피드백 순으로 조회된다.
