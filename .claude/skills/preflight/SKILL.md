---
name: preflight
description: 과제 시작 전 개발 환경을 점검한다.
disable-model-invocation: true
---

# /preflight — 시작 전 점검

- 과제 시작 **10분 전에** 실행한다
- JDK·Docker·포트·git·gh·빌드 캐시를 확인한다
- 문제를 찾으면 고칠 명령을 알려준다
  - 직접 고치는 것은 아래 "허용된 조치"뿐이다
  - 나머지는 보고만 한다

## 점검 항목

| # | 항목 | 확인 방법 | 기준 |
|---|---|---|---|
| 1 | JDK 21 | `/usr/libexec/java_home -v 21` (macOS) 또는 `./gradlew -q javaToolchains` | 21 존재 |
| 2 | Gradle | `./gradlew --version` | 정상 출력 |
| 3 | Docker | `docker info` | 데몬 응답 |
| 4 | 포트 | `lsof -i :3309 -i :8080` | 비어 있음 (`gym-mysql`이 3309를 쓰는 것은 정상) |
| 5 | git 상태 | `git status -sb`, `git fetch` | `main`, 변경 없음, 원격과 같음 |
| 6 | 원격 · gh | `git remote -v`, `gh auth status`, `gh repo view --json name,viewerPermission` | 로그인됨, 쓰기 권한 |
| 6-1 | curl · jq | `curl --version`, `jq --version` | 둘 다 있음 (실측에 필요) |
| 7 | 템플릿 상태 | `docs/design/00~04`, `docs/task_list.md` | 채워지지 않은 템플릿 (아래 참고) |
| 8 | 빌드 · 이미지 캐시 | `./gradlew spotlessApply build` | 통과 (아래 참고) |
| 9 | 실서버 | `docker compose up -d` → `./gradlew bootRun` → `/actuator/health` · `/swagger-ui.html` | UP · 200 |

- 7번: 아직 채워지지 않은 템플릿이어야 한다
  - 지난 과제 내용이 남아 있으면 경고
- 8번: 의존성 · Testcontainers 이미지를 미리 받아 둔다

## 허용된 조치

- Docker가 꺼져 있으면 Docker Desktop을 켠다
  - macOS: `open -a Docker`
  - 켰다고 보고한다
- 8 · 9번 실행 (빌드 산출물 · 컨테이너 생성)
- 9번 확인이 끝나면 bootRun을 내린다. MySQL 컨테이너는 켜 둔다

그 외(브랜치 전환, 파일 수정, 커밋, 프로세스 종료 등)는 하지 않는다.

## 보고

```
## 시작 전 점검

| # | 항목 | 결과 | 조치 |
|---|---|---|---|
| 1 | JDK 21 | ✅ 21.0.x | |
| 4 | 포트 8080 | ❌ PID 1234 (java) 사용 중 | `kill 1234` |
| … | | | |

## 과제 시작 시
1. `docs/design/00-assignment.md`에 원문 붙여넣기
2. `/design`
```

❌가 하나라도 있으면 맨 위에 **"시작 전에 고칠 것 n건"**을 먼저 적는다.
