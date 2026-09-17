---
name: wrap-up
description: F9 마무리(제출물·전체 점검·최종 게이트)를 실행한다.
disable-model-invocation: true
---

# /wrap-up — F9 마무리

- 제출물(README·AI 활용 내역·.http)을 채운다
- 전체 추적성 점검을 돌린다
- 최종 게이트를 돌린다

이 스킬은 메인 세션에서 아래 순서를 **그대로** 실행한다.

- 순서를 바꾸거나 단계를 건너뛰지 않는다
- `🛑 멈춤`이 나오면 그 자리에서 사용자에게 보고하고 응답을 기다린다

## 0. 사전 확인

- 마지막으로 완료된 기능의 PR이 머지됐는지 확인하고 `main`을 최신화한다
  - 절차: `.claude/skills/run-feature/pull-request.md`의 "다음 스킬이 시작할 때"
- 작업 트리가 깨끗한지 확인한다
  - 아니면 → 🛑 멈춤: 변경 목록을 보여주고 처리 방법을 묻는다
- `main`에서 `feature/docs` 브랜치를 만든다
- 미완료 기능이 있으면 → 🛑 멈춤
  - 남은 기능 목록과 함께 "구현을 멈추고 마무리로 넘어갈까요?"를 묻는다
  - 넘어가기로 하면 남은 기능을 task_list "결정 필요 / 보류"로 옮긴다
    - 사유와 함께 옮긴다
    - 커밋: `docs: 미구현 기능 보류 처리`

## 1. 실행 케이스 정리

- `http/*.http`를 훑어 기능별 요청이 모두 남아 있는지 확인한다
- `@expect`가 빠진 요청이 없는지 확인한다
  - 기준: `.claude/skills/verify-http/SKILL.md` 작성 규칙
- 파일 맨 위에 사용법 주석(서버 실행 명령 · 요청 순서 의존성)을 단다
- 커밋: `docs: .http 실행 케이스 정리`

## 2. 제출물 작성

- `doc-writer`를 호출한다
  - 지시:
    - "`01` §4의 모든 SUB 항목을 채워 README를 제출용으로 다시 쓴다."
    - "AI 활용 내역은 `docs/ai-log/README.md`에 쓴다. 기능별 ai-log를 요약한다."
    - "미구현·보류 항목은 README의 한계 절에 사유와 함께 적는다"
- `OpenApiConfig`의 TODO(제목·설명)가 남았으면 채운다
  - 커밋: `chore: OpenAPI 제목·설명 작성`
- `01` §4 SUB 표의 완료 칸을 체크한다
- 커밋 (각 커밋에 task_list T9-x 체크를 넣는다)
  - `docs: README 작성 (실행 방법·기술 스택 및 선택 이유·API)`
  - `docs: AI 활용 내역 정리`

## 3. 전체 점검

- `verifier`를 **구현 점검 모드, 대상 전체 + SUB**로 호출한다
- 높음 지적
  - 문서만 고치면 되는 것 → `doc-writer`로 고치고 `verifier`를 한 번 더 돌린다
  - 코드를 고쳐야 하는 것 → 🛑 멈춤
    - 남은 시간과 함께 고칠지 README 한계로 남길지 묻는다

## 4. 최종 게이트

- `./gradlew clean spotlessApply build`
- `.claude/skills/verify-http/SKILL.md`의 "순서"를 대상 전체로 실행한다
  - 그 절차가 DB를 초기화하므로 seed부터 다시 확인하는 셈이다
- 불일치가 있으면 → 🛑 멈춤: 고칠지 README 한계로 남길지 묻는다

## 5. 마무리

🛑 멈춤: 아래를 보고하고 결정을 받는다.

```
## 제출 준비 완료 (누적 hh:mm / 5:00)
- 필수 FR: n/n 구현 · 선택 FR: n/n
- 테스트: n개 통과
- SUB: n/n 충족 (미충족 사유)
- 남긴 지적 · 한계: …

PR을 올릴까요? (push → PR 생성. 머지하면 제출 상태가 됩니다)
```

승인하면:

- `.claude/skills/run-feature/pull-request.md` 절차로 PR을 올린다
  - 대상 브랜치: `feature/docs`
- 머지 후 `main`이 제출본이다
