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
- `data.totalUsedAsIs`: 추천문장을 그대로 사용한 건수
- `data.totalModified`: 추천문장을 일부 수정 후 사용한 건수
- `data.totalNotUsed`: 추천문장을 사용하지 않은 건수

## 집계 기준
- 집계 대상: `ai_recommendation`과 연결된 교사(`message.sender.role = TEACHER`) 메시지
- 그대로 사용:
  - `is_recommendation_used = true`
  - `similarity_modified >= 0.99`
- 일부 수정 후 사용:
  - `is_recommendation_used = true`
  - `similarity_modified < 0.99` 또는 `similarity_modified IS NULL`
- 미사용:
  - `is_recommendation_used = false` 또는 `NULL`
- 채택률:
  - `(totalUsedAsIs + totalModified) / totalRecommendationCount * 100`
  - `totalRecommendationCount = 0`이면 `0.0`

## 오류 응답
- `401 Unauthorized`: Authorization 헤더 누락/토큰 오류
- `403 Forbidden`: 관리자 권한 아님
- `404 Not Found`: 토큰의 사용자 ID에 해당하는 사용자 없음

