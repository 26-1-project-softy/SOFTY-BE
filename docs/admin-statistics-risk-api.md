# 관리자 리스크 통계 API 명세

## 개요
- 관리자 대시보드에서 분쟁 리스크 통계를 조회하기 위한 API
- 엔드포인트: `GET /admin/statistics/risk`

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
  "message": "리스크 건수 조회에 성공했습니다.",
  "data": {
    "totalMessageCount": 125,
    "detectedConflictCount": 42,
    "conflictDetectionRate": 33.6
  }
}
```

### 필드 설명
- `data.totalMessageCount`: 교사가 보낸 전체 메시지 수
- `data.detectedConflictCount`: 교사가 보낸 메시지 중 분쟁 리스크 탐지 건수
- `data.conflictDetectionRate`: 분쟁 리스크 탐지율(0~100, 소수점 둘째 자리 반올림)

## 집계 기준
- 대상 메시지: 발신 사용자(`message.sender_id`)가 `TEACHER` 역할을 보유한 메시지
- 총 메시지 수:
  - `count(message joined with user_role where user_role.role=TEACHER)`
- 탐지 건수:
  - `count(message joined with user_role where user_role.role=TEACHER and is_dispute_risk=true)`
- 탐지율:
  - `detectedConflictCount * 100 / totalMessageCount`
  - `totalMessageCount=0`이면 `0.0`

## 오류 응답
- `401 Unauthorized`: Authorization 헤더 누락/토큰 오류
- `403 Forbidden`: 관리자 권한 아님
- `404 Not Found`: 토큰의 사용자 ID에 해당하는 사용자 없음

