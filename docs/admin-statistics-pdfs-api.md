# 관리자 PDF 통계 API 명세

## 개요
- 관리자 대시보드에서 PDF 리포트 생성 통계를 조회하기 위한 API
- 엔드포인트: `GET /admin/statistics/pdfs`

## 인증/권한
- `Authorization: Bearer {accessToken}` 헤더 필수
- 관리자(`role=ADMIN`)만 조회 가능

## 요청
### Headers
- `Authorization`: `Bearer {JWT}`

### Query Params
- 없음

## 응답
### 성공 (200)
```json
{
  "success": true,
  "code": 200,
  "message": "PDF 조회에 성공했습니다.",
  "data": {
    "totalPdfCount": 114,
    "list": [
      {
        "teacherId": 10,
        "teacherName": "홍길동",
        "pdfCount": 24
      },
      {
        "teacherId": 12,
        "teacherName": "임예린",
        "pdfCount": 21
      }
    ]
  }
}
```

### 필드 설명
- `data.totalPdfCount`: 전체 PDF 생성 수 (`pdf_file` 전체 건수)
- `data.list`: 교사별 PDF 생성 수 목록
  - `teacherId`: 교사 사용자 ID
  - `teacherName`: 교사명
  - `pdfCount`: 해당 교사의 PDF 생성 수

## 집계 기준
- 소스 테이블: `pdf_file`
- 교사 매핑: `chat_room_user_map` + `users(role='TEACHER')`
- 정렬: `pdfCount DESC`, `teacherName ASC`, `teacherId ASC`

## 오류 응답
- `401 Unauthorized`: Authorization 헤더 누락/토큰 오류
- `403 Forbidden`: 관리자 권한 아님
- `404 Not Found`: 토큰의 사용자 ID에 해당하는 사용자 없음
