---
name: progress
description: 과제 진행 상황과 남은 시간을 요약한다.
disable-model-invocation: true
allowed-tools: Read, Grep, Glob, Bash(git status:*), Bash(git log:*), Bash(git branch:*), Bash(date:*)
---

# /progress — 진행 상황

- 아무것도 수정하지 않는다
- 아래만 읽고 보고한다

- `docs/task_list.md` — **현재**, 기능별 완료 작업 수, 시간 계획
- `git branch --show-current`, `git status --short`, `git log --oneline -5`
- `date`로 현재 시각을 읽는다
  - task_list의 `**시작:**` 줄과 비교해 경과 시간을 계산한다
- 일정이 밀렸을 때 뺄 후보의 근거: `docs/design/01-analysis.md` §5.3

```
## 진행 상황 (경과 hh:mm / 5:00)
- 현재: F? / T?-? (브랜치 …, 커밋 안 된 변경 n개)
- 기능: F0 ✅ · F1 ✅ · F2 3/5 · F3 ⬜ …
- 일정: 목표 대비 +mm분 / -mm분
- 보류 · 결정 필요: n건

## 다음
- (다음에 칠 명령. 예: `/run-feature F2`)
- 일정이 밀렸으면 뺄 후보 기능과 그 이유
```
