# 관리자 추천문장 채택률 API 명세

## 개요
- 관리자 대시보드에서 AI 추천문장 채택 통계를 조회하기 위한 API
- 엔드포인트: `GET /admin/statistics/recommendation-adoption`

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
  "message": "채택률 조회에 성공했습니다.",
  "data": {
    "adoptionRate": 75.0,
    "totalUsedAsIs": 54,
    "totalModified": 36,
    "totalNotUsed": 30
  }
}
```

### 필드 설명
- `data.adoptionRate`: 추천문장 채택률(0~100, 소수점 둘째 자리 반올림)
- `data.totalUsedAsIs`: 추천문장과 수정문장이 완전히 일치한 건수
- `data.totalModified`: 추천문장 방향으로 일부 수정한 건수
- `data.totalNotUsed`: 추천문장을 사실상 사용하지 않은 건수

## 집계 기준
- 집계 대상: `ai_recommendation`과 연결된 교사 메시지(`message.sender.role = TEACHER`)
- `totalUsedAsIs`
  - `message.modify_content IS NOT NULL`
  - `message.modify_content = ai_recommendation.content`
- `totalModified`
  - `message.similarity_modified IS NOT NULL`
  - `message.similarity_original IS NOT NULL`
  - `message.similarity_modified > message.similarity_original`
- `totalNotUsed`
  - 위 `totalUsedAsIs`, `totalModified` 조건에 해당하지 않는 나머지
- `adoptionRate`
  - `(totalUsedAsIs + totalModified) / totalRecommendationCount * 100`
  - `totalRecommendationCount = 0`이면 `0.0`

## 오류 응답
- `401 Unauthorized`: Authorization 헤더 누락/토큰 오류
- `403 Forbidden`: 관리자 권한 아님