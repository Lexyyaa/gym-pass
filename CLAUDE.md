# CLAUDE.md

헬스장 회원권 관리 백엔드 (3시간 제한 과제).
설계 문서를 먼저 채우고(SDD), 그 문서를 기준으로 구현한다.

- Spring Boot 3.5.5 · Java 21 · Gradle
- JPA · MySQL 8.4
- Testcontainers · JUnit 5 · ArchUnit · Spotless

## 실행

- 인프라: `docker compose up -d`
  - `gym-mysql` 컨테이너
  - 호스트 포트 **3309**(3306 아님)
  - `gym`/`gym`, DB `gym`
- 초기화: `docker compose down -v`
  - seed나 컬럼 제약을 바꿨으면 필수
  - `ddl-auto: update`는 기존 컬럼을 고치지 못한다
- 서버: `./gradlew bootRun`
  - 프로파일 인자 불필요 (`local`이 기본)
- 전체 테스트: `./gradlew test`
  - Docker만 켜져 있으면 된다
  - Testcontainers가 MySQL을 따로 띄운다
- 단일 테스트: `./gradlew test --tests '*ClassName'`
  - 또는 `--tests '*ClassName.methodName'`
- 구조 검사만: `./gradlew archTest` — DB 없이 돈다
- 커밋 전 게이트: `./gradlew spotlessApply build`
  - 포맷 적용 → 포맷 검사 + 전체 테스트(구조 검사 포함)
- 테스트 리포트: `build/reports/tests/test/index.html`
- 서버 확인
  - `curl localhost:8080/actuator/health`
  - Swagger `http://localhost:8080/swagger-ui.html`

## 작업 시작 전

1. `docs/task_list.md`에서 **현재** 작업과 커밋 메시지를 확인한다
2. 아래 표에서 읽을 문서를 고른다
   - 읽은 문서와 계획을 짧게 보고한 뒤 진행한다
3. 규칙 파일을 따른다
   - 코드 규칙: `src/main/CLAUDE.md`
   - 테스트 규칙: `src/test/CLAUDE.md`
   - 해당 경로 파일을 읽을 때 로드된다

| 작업 | 읽을 문서 (`docs/design/`) |
|---|---|
| F0 설계 | `00-assignment` → `01-analysis` → 결정 → `02` → `03` → `04` |
| F1 기초 설정 | `03` §1 용어·§6 ERD·§9 seed / `04` §4 에러 코드 |
| 기능 구현 | `02` 해당 기능 절 / `03` 해당 애그리거트·§4 상태 전이·§7 동시성 / `04` 해당 API |
| 테스트 작성 | `02` 해당 기능의 TC 표 / `04` 해당 API의 Errors |
| F9 마무리 | `02` §4 제출물·§5 결정 / `04` 전체 / `task_list` 보류 목록 |

## 멈추고 물어볼 때

- 스펙에 없는 결정이 필요하다
  - → 구현하지 말고 선택지 2~3개를 제시
  - 각 선택지는 트레이드오프와 함께 한 줄씩
- 설계 문서와 코드가 어긋난다
  - → 어느 쪽이 맞는지 묻는다
- `docs/design`을 고쳐야 한다
  - → 고칠 내용을 제안만 한다
  - F0 설계 단계는 예외
- 기능(F) 하나가 끝났다
  - → 완료 보고 후 다음 기능으로 넘어갈지 확인
- 기능 안의 작업(T) 사이에서는 멈추지 않는다
  - 3시간 과제라 확인 비용이 크다

## 하지 말 것

- `docs/design/00-assignment.md` 수정 (과제 원문)
- 사용자 결정 없이 분석의 `C-n`을 정하기
  - 결정의 원본 기록은 `docs/design/01-analysis.md` §7이다
  - `02` §5의 `D-n`은 거기서 옮긴 것이다
- 스펙에 없는 기능·필드·API 추가
- seed 값을 과제 원문과 다르게 작성
  - 테스트에 필요한 값은 테스트 fixture로 만든다
- 실행 확인 없이 완료 보고
- 사용자 승인 없이 `git push` · PR 생성
  - 승인 후 절차: `.claude/skills/run-feature/pull-request.md`
- 머지 — 사용자가 GitHub에서 PR을 보고 직접 한다

## 규칙 — 도구가 검증한다

- Spotless (palantir-java-format)
  - 포매팅(다중 인자 줄바꿈 포함)
  - 미사용 import
- ArchUnit
  - 레이어 의존 방향
  - presentation(Request · Response · Controller)의 domain 참조 금지
  - 클래스 위치와 이름
    - Controller · Advice · Entity · Repository 구현 · JpaRepository
  - `src/test/java/com/gym/pass/architecture/ArchitectureTest.java`
- 도구가 막는 규칙은 여기 다시 적지 않는다
  - 규칙을 바꾸려면 도구 설정을 바꾼다

## 규칙 — 사람이 본다

- 비즈니스 규칙·불변식·상태 전이는 도메인 객체에 둔다
  - ApplicationService는 흐름 조율과 트랜잭션 경계만 맡는다
- DTO에는 변환 메서드(`from`/`of`)만 둔다
  - 계산·검증·분기 금지
- 컨트롤러 핸들러에 조건문 금지
  - 상태코드 결정은 Response의 정적 메서드로
- 애그리거트 간에는 식별자로만 참조한다
- 행동이나 불변식이 있는 값은 VO로 만든다
- 입력 오류는 전부 4xx다
  - 500은 서버 결함일 때만

`reviewer` 에이전트의 체크리스트

- 이 목록
- `src/main/CLAUDE.md`
- `src/test/CLAUDE.md`

## 작업 흐름

- 브랜치: `main` + `feature/{name}`
  - task_list의 기능(F) 하나가 브랜치 하나
- 커밋: `타입: 한글 설명`
  - 타입: `feat` `test` `fix` `refactor` `docs` `chore`
  - 메시지는 task_list 작업 줄을 그대로 쓴다
  - 예외: `fix: {지적 요약}` · `docs: D-n 결정 반영` · `docs: F{n} 리뷰 반영`
- 순서: `feat` → `test`(성공·실패) → `test`(엣지·동시성)
- 작업(T)이 끝나면 task_list 체크 + **현재** 갱신
- 기능(F)이 끝나면 `docs/ai-log/F{번호}-{name}.md` 작성
  - 형식: `docs/ai-log/_template.md`
- PR 본문: `.github/pull_request_template.md`
- 머지는 사용자가 GitHub에서 Merge commit으로
  - 작업 커밋을 남긴다
- 문서·보고 작성: 한 줄에 한 가지만 쓴다
  - 줄이 길어지거나 항목이 여러 개면 불릿으로 세로로 나눈다

## 스킬 — 과제 진행은 이 명령으로만 한다

순서와 멈춤 지점은 각 스킬(`.claude/skills/*/SKILL.md`)에 고정되어 있다.
메인 세션이 임의로 순서를 바꾸지 않는다.

| 명령 | 시점 | 하는 일 |
|---|---|---|
| `/preflight` | 과제 시작 10분 전 | 환경 점검 (JDK · Docker · 포트 · git · gh · 빌드 캐시) |
| `/design` | 원문을 붙여넣은 직후 | `analyst` → 결정 받기 → `doc-writer` → `verifier`(설계) → PR |
| `/run-feature F2` | 이전 PR 머지 후, 기능마다 | `implementer` → `reviewer` + `verifier` → 실측 → ai-log → 게이트 → PR |
| `/verify-http F2` | 수정 후 재확인 | 서버 기동 → `.http` 재연 → 상태 · 본문 · DB 대조 |
| `/progress` | 수시로 | 현재 위치 · 경과 시간 · 뺄 후보 |
| `/wrap-up` | 마지막 PR 머지 후 | `.http` 정리 → README · AI 내역 → `verifier`(전체) → 최종 게이트 → PR |

- 에이전트(`.claude/agents/`)는 사용자에게 직접 묻지 못한다
  - "멈추고 돌아옴" 보고가 오면 메인 세션이 사용자에게 묻는다
- 스킬 밖에서 요청받은 작업도 이 파일의 규칙을 따른다

## 완료 기준

- 작업(T): `./gradlew spotlessApply build` 통과
- 기능(F)
  - 해당 FR의 TC 전부 구현
  - `http/{name}.http` 실측 통과 (`/verify-http`)
    - API가 있는 기능만. task_list에 `.http` 줄이 없으면 생략
    - 상태코드
    - 응답 본문
    - DB 상태
  - task_list 갱신 + ai-log 작성

## 자주 틀리는 것 — 겪을 때마다 한 줄씩 추가

- `data.sql` INSERT에 `created_at` · `updated_at`을 빠뜨린다
  - → `BaseTimeEntity` 컬럼은 NOT NULL · DEFAULT 없음. `NOW(6)`를 넣는다
- `Info`에 domain Enum · VO를 그대로 담는다
  - → presentation이 domain을 참조하게 되어 ArchUnit 실패
  - `name()` · 원시값으로 푼다
- DTO 안에서 계산·검증을 한다
  - → 도메인 메서드로 옮긴다
- ERD의 UNIQUE·INDEX·NOT NULL을 엔티티에 선언하지 않는다
  - → `@Table`·`@Column(nullable = false)`에 없으면 DB에도 없다
- `FOR UPDATE` 조회 조건 컬럼에 인덱스가 없다
  - → 스캔한 행이 전부 잠겨 무관한 요청까지 직렬화된다
- 리스트 요청 필드에 `@Valid`를 빠뜨린다
  - → 요소 검증이 안 돌아 null·음수가 도메인까지 가서 500
- 잘못된 입력이 VO 생성자의 `IllegalArgumentException`으로 새서 500이 된다
  - → 도메인에서 `BusinessException`으로 먼저 막는다
- 리스트 입력의 요소 중복을 검사하지 않는다
  - → 같은 항목이 두 번 처리된다
- `catch (RuntimeException)`이 `BusinessException`까지 삼킨다
  - → 비즈니스 예외는 원래 에러 코드로 전달한다
- DB 커밋 후에 파일·외부 I/O를 한다
  - → I/O가 실패하면 레코드만 남는다
  - 트랜잭션 안으로 옮기거나 보상 처리한다
- `@OneToMany` 컬렉션 순서에 의존하면서 `@OrderBy`가 없다
- 부분 성공 API를 상태코드로만 검증한다
  - → 200인데 저장 0건인 거짓 통과
