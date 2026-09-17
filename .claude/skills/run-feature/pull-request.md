# PR 올리기 절차

`/design` · `/run-feature` · `/wrap-up`의 마무리 단계가 공통으로 쓴다.
**사용자가 PR 올리기를 승인한 뒤에만** 실행한다.

## 원칙

- 스킬은 PR을 **올리기까지만** 한다
  - 머지는 사용자가 GitHub에서 PR을 보고 직접 한다
- 머지 방식은 Merge commit을 권장한다
  - 작업(T) 커밋을 남긴다
  - 기능(F) 경계를 머지 커밋으로 남긴다
- 스킬이 `gh pr merge`나 `git merge`를 실행하지 않는다

## 순서

1. 작업 트리가 깨끗한지 확인한다
2. `git push -u origin {브랜치}`
3. PR 본문 파일을 만든다
   - `.github/pull_request_template.md`의 모든 칸을 채운다. 빈 칸은 "없음"
   - 스크래치 디렉터리에 저장한다 (저장소에 커밋하지 않는다)
4. PR을 만든다
   - `gh pr create --base main --head {브랜치} --title "{F번호}. {기능명}" --body-file {본문 파일}`
5. 🛑 멈춤: 아래를 보고한다

```
PR: {URL}
GitHub에서 확인 후 머지해 주세요 (Merge commit 권장).
머지한 뒤 다음 명령: {다음 명령}
```

`gh`가 실패하면 실패 메시지와 함께 🛑 멈춤.

- 브랜치는 이미 push되어 있다
- GitHub에서 직접 PR을 열 수 있다고 안내한다

## 다음 스킬이 시작할 때 (이전 PR 확인)

`/run-feature` · `/wrap-up`의 0단계에서 실행한다.

1. 이전 브랜치를 정한다
   - task_list에서 `[x]`가 있는 직전 F 블록의 `feature/{name}`
   - F1 앞은 `feature/design`
2. `gh pr list --head {이전 브랜치} --state all --json number,state,url`
3. 상태별 처리
   - `MERGED` → `git switch main && git pull --ff-only` 후 진행
   - `OPEN` → 🛑 멈춤
     - "이전 PR({URL})이 아직 머지되지 않았습니다. 머지 후 다시 실행해 주세요"
   - PR 없음 → 🛑 멈춤
     - 이전 브랜치가 push만 되었는지, 로컬에만 있는지 보고한다
