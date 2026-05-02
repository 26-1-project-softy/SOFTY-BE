# 학부모 학급 변경 API

- 엔드포인트: `PATCH /parent/me/class`
- 목적: 학부모가 입력한 학급 코드로 자녀의 현재 학급을 실제로 변경

## 요청

- 헤더
  - `Authorization: Bearer {JWT}`
- 본문 (`application/json`)

```json
{
  "classCode": "QWGX12"
}
```

## 처리 규칙

1. JWT로 로그인한 사용자를 확인한다.
2. 로그인 사용자가 학부모(`PARENT`)인지 확인한다.
3. 현재 학부모와 연결된 자녀를 조회한다.
4. 입력한 `classCode`로 활성 학급 코드를 조회한다.
5. 해당 학급으로 학생의 `classroom`을 변경한다.
6. 변경된 학교명, 학년, 반 정보를 반환한다.

## 응답

```json
{
  "success": true,
  "code": 200,
  "message": "학급이 변경되었습니다.",
  "data": {
    "schoolName": "한국초등학교",
    "grade": 6,
    "classNumber": 2
  }
}
```

## 응답 필드

- `data.schoolName` (`String`)
  - 변경된 학교 이름
- `data.grade` (`int`)
  - 변경된 학년
- `data.classNumber` (`int`)
  - 변경된 반 번호

## 오류 응답

- `400 Bad Request`
  - 요청 본문이 없거나 `classCode`가 비어 있는 경우
- `403 Forbidden`
  - 학부모 계정이 아닌 경우
- `404 Not Found`
  - 사용자, 연결된 자녀, 유효한 학급 코드, 학급 정보를 찾을 수 없는 경우

## 비고

- 현재 MVP 정책상 학부모는 자녀 1명과 연결된 것으로 가정한다.
- 현재 구조에서는 `ParentStudent`를 바꾸지 않고, 연결된 학생의 `classroom`을 새 학급으로 변경한다.
