# 교사 추천 문구 수정 확인 플로우

## 개요
- 교사가 AI 추천 문구를 받은 뒤 그대로 전송할 수도 있고, 내용을 수정해서 전송할 수도 있다.
- 추천 문구와 최종 전송 문구가 다를 경우 프론트에서 작은 확인 알림창을 띄운다.
- 이 플로우는 별도 API를 추가하지 않고 기존 분석 API와 최종 전송 API만으로 처리한다.

## 사용 API
- 추천 문구 분석 API
  - `POST /chat-rooms/{chatRoomId}/teacher-messages/analyze`
- 추천 문구 재분석 API
  - `POST /teacher-message-analyses/{analysisId}/recheck`
- 교사 최종 메시지 전송 API
  - `POST /chat-rooms/{chatRoomId}/teacher-messages`

## 동작 흐름
1. 교사가 초안 메시지를 입력한다.
2. 프론트가 분석 API를 호출한다.
3. BE는 `analysisId`, `riskLevel`, `recommendedMessage`를 반환한다.
4. 프론트는 반환받은 `recommendedMessage`를 입력창 또는 추천 UI에 표시한다.
5. 교사가 추천 문구를 그대로 사용하거나 일부 수정한다.
6. 교사가 수정한 문장을 다시 AI로 검토받고 싶다면 `POST /teacher-message-analyses/{analysisId}/recheck`를 호출한다.
7. 재분석 응답으로 새 `analysisId`, `riskLevel`, `recommendedMessage`를 받는다.
8. 전송 버튼 클릭 시 프론트가 현재 기준 `recommendedMessage`와 최종 입력 `content`를 비교한다.
9. 두 문장이 같으면 바로 최종 전송 API를 호출한다.
10. 두 문장이 다르면 프론트에서 확인 알림창을 띄운다.
11. 교사가 알림창에서 전송을 확정하면 최종 전송 API를 호출한다.

## analyze / recheck 사용 기준
- `POST /chat-rooms/{chatRoomId}/teacher-messages/analyze`
  - 첫 초안 분석 시 사용
  - 아직 분석 이력이 없는 상태에서 시작점으로 사용
- `POST /teacher-message-analyses/{analysisId}/recheck`
  - 이미 한 번 분석한 메시지를 수정한 뒤 다시 AI 검토를 받고 싶을 때 사용
  - 같은 메시지 흐름 안에서 재분석을 이어갈 때 사용
- 단순히 추천 문구와 최종 입력 문장이 다르다는 이유만으로 `recheck`가 필수인 것은 아니다.
- 교사가 추가 검토 없이 바로 보내고 싶다면 확인 알림창만 거친 뒤 최종 전송 API를 호출하면 된다.

## 프론트 비교 기준
- 권장 비교 방식
  - `recommendedMessage.trim()` 과 `content.trim()` 비교
- 둘 중 하나가 `null` 또는 빈 문자열이면 추천 문구가 없는 것으로 간주하고 바로 전송한다.

## 분석 API 응답 예시
```json
{
  "success": true,
  "code": 200,
  "message": "메시지 분석이 완료되었습니다.",
  "data": {
    "analysisId": 55,
    "riskLevel": "UNSAFE",
    "recommendedMessage": "조금 더 부드럽고 오해가 적은 표현으로 바꿔보는 것을 권장드립니다."
  }
}
```

## 최종 전송 API 요청 예시
```json
{
  "analysisId": 55,
  "content": "조금 더 부드럽고 오해가 적은 표현으로 바꿔보는 것을 권장드립니다."
}
```

## 재분석 API 요청 예시
```json
{
  "content": "조금 더 부드럽게 말씀드리면 좋을 것 같습니다."
}
```

## 서버 처리 기준
- 최종 전송 API는 `analysisId`와 `content`를 기준으로 메시지를 전송한다.
- 서버는 분석 결과의 `recommendedMessage`와 최종 전송 `content`를 비교해 추천 문구 사용 여부를 내부적으로 기록한다.
- 따라서 프론트의 알림창은 사용자 확인 UX를 위한 것이고, 추천 문구 채택 여부 기록은 기존 서버 로직으로 처리된다.

## 정리
- 첫 분석은 `analyze`, 수정본 재검토는 `recheck` 사용
- 추천 문구가 그대로 전송되면 알림창 없이 바로 전송
- 추천 문구가 수정되면
  - 다시 분석이 필요 없으면 확인 알림창 후 바로 전송
  - 다시 분석이 필요하면 `recheck` 호출 후 새 분석 결과 기준으로 전송
- 별도 확인용 API 추가는 필요하지 않음
