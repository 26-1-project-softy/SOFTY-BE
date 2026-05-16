# 학부모 설정 조회 API

- 엔드포인트: `GET /parent/setting`
- 목적: 학부모 설정 화면에 필요한 학생, 담임, 교사 근무시간 정보를 조회

## 요청

- 헤더
  - `Authorization: Bearer {JWT}`

## 처리 규칙

1. JWT로 로그인한 사용자를 확인한다.
2. 현재 세션의 `activeRole`이 `PARENT`인지 확인한다.
3. 해당 사용자가 실제로 `PARENT` 역할을 보유하는지 확인한다.
4. 해당 학부모의 연결 자녀 1명을 조회한다.
5. 자녀의 학급과 담임 교사를 조회한다.
6. 해당 교사의 근무시간 설정을 조회한다.
7. 근무시간이 없으면 `schedules`는 빈 배열(`[]`)로 반환한다.

## 응답

```json
{
  "success": true,
  "code": 200,
  "message": "학부모 설정 정보 조회가 완료되었습니다.",
  "data": {
    "grade": 3,
    "classNumber": 1,
    "studentName": "김철수",
    "teacherName": "김교사",
    "schedules": [
      {
        "dayOfWeek": 1,
        "startTime": "09:00",
        "endTime": "18:00"
      }
    ]
  }
}
```

## 응답 필드

- `data.grade` (`int`): 학년
- `data.classNumber` (`int`): 반 번호
- `data.studentName` (`String`): 학생 이름
- `data.teacherName` (`String`): 담임 교사 이름
- `data.schedules` (`Array`): 교사 근무시간 목록
- `data.schedules[].dayOfWeek` (`int`): 요일 번호 (`1~7`)
- `data.schedules[].startTime` (`String`): 시작 시간 (`HH:mm`)
- `data.schedules[].endTime` (`String`): 종료 시간 (`HH:mm`)

## 오류 응답

- `403 Forbidden`
  - 현재 세션의 `activeRole`이 `PARENT`가 아닌 경우
  - `PARENT` 역할이 실제로 없는 계정인 경우
- `404 Not Found`
  - 사용자, 연결 자녀, 학급, 담임 교사 정보를 찾을 수 없는 경우

## 비고

- 현재 MVP 정책상 학부모는 자녀 1명, 담임 교사 1명 기준으로 동작합니다.
