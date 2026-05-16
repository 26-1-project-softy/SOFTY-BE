# 관리자 임베딩 실행 API 명세

## 개요
- 관리자 대시보드에서 임베딩 배치를 수동으로 실행하기 위한 API
- 엔드포인트: `POST /admin/embedding/run`

## 인증/권한
- `Authorization: Bearer {accessToken}` 헤더 필수
- 관리자(`role=ADMIN`)만 호출 가능

## 요청
### Headers
- `Authorization`: `Bearer {JWT}`

### Body
- 없음

## 응답
### 성공 (200)
```json
{
  "success": true,
  "code": 200,
  "message": "임베딩 배치 실행이 완료되었습니다.",
  "data": {
    "trigger": "MANUAL",
    "totalCandidates": 120,
    "successCount": 110,
    "failedCount": 6,
    "skippedCount": 4,
    "startedAt": "2026-04-19T13:40:12",
    "finishedAt": "2026-04-19T13:42:51"
  }
}
```

### 필드 설명
- `data.trigger`: 실행 트리거 구분 (`MANUAL` 또는 `SCHEDULED`)
- `data.totalCandidates`: 이번 실행에서 처리 대상으로 조회된 건수
- `data.successCount`: 임베딩/유사도 저장까지 성공한 건수
- `data.failedCount`: 실행 중 오류가 발생해 실패한 건수
- `data.skippedCount`: 필수 텍스트 누락 등으로 건너뛴 건수
- `data.startedAt`: 실행 시작 시각 (서버 로컬 시간)
- `data.finishedAt`: 실행 종료 시각 (서버 로컬 시간)

## 처리 기준
- 대상: 발신 사용자가 `TEACHER` 역할을 보유한 메시지와 연결된 추천문장 중 아래 조건 하나 이상 해당 건
  - `ai_recommendation.embedding IS NULL`
  - `message.content_embedding IS NULL`
  - `message.modify_content IS NOT NULL AND message.modify_content_embedding IS NULL`
  - `message.similarity_original IS NULL`
  - `message.modify_content IS NOT NULL AND message.similarity_modified IS NULL`
- 실패 정책: 건별 실패 시 해당 건만 스킵하고 다음 건 계속 진행

## 스케줄 실행 정보
- 동일 로직이 매주 일요일 03:00(Asia/Seoul)에 스케줄로도 실행됨
- 스케줄 설정
  - `embedding.schedule.cron` (기본값: `0 0 3 * * SUN`)
  - `embedding.schedule.zone` (기본값: `Asia/Seoul`)

## 오류 응답
- `401 Unauthorized`: Authorization 헤더 누락/토큰 오류
- `403 Forbidden`: 관리자 권한 아님
