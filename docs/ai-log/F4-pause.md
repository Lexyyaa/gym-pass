# F4. 회원권 정지 `feature/pause`

## 받은 지시

- `/run-feature F4`
- task_list F4의 작업 T4-1 ~ T4-4 구현
  - T4-1 MembershipPause 및 상한·겹침·연장 규칙
  - T4-2 정지 등록·조기 해제 API
  - T4-3 정지 성공·실패 케이스
  - T4-4 정지 연장·해제 엣지 케이스
- 시간
  - 시작 19:39
  - 종료 약 20:25
  - 소요 약 46분
    - 구현 약 13분
    - 리뷰 → 결정 → 수정 순환 약 33분
  - 순환 중 수정 3회
  - 순환 중 결정 5건
    - C-37 ~ C-40
    - C-40 보충
- 순환이 길어진 이유
  - 정지 규칙에 서로 얽힌 날짜 경계가 많다
    - 예약
    - 해제
    - 상한
    - 기간
    - 당일 출입
  - 분석 단계에서 빠진 시나리오(S-28)가 있었다
  - 메인 세션이 판정 순서를 잘못 지시해 한 번 멈췄다

## 작업 흐름

1. F4 착수 전 결정 (19:39)
   - C-37 → D-28: 잔여 0인 횟수제는 정지·취소 불가
   - `docs: D-28 결정 반영` (ded1cf1)
2. `implementer`가 T4-1 ~ T4-4 구현 · 커밋 (약 13분)
   - T4-1 `feat: MembershipPause 및 상한·겹침·연장 규칙 구현` (f5ccdcd)
   - T4-2 `feat: 정지 등록·조기 해제 API 구현` (3340239)
   - T4-3 `test: 정지 성공·실패 케이스` (f2f7d0f)
   - T4-4 `test: 정지 연장·해제 엣지 케이스` (ed1009f)
   - 테스트 207개 전체 통과
     - 이 중 F4 69개
3. 1차 `reviewer` + `verifier`
   - 높음 4건 (아래 "검증" 절)
   - 사용자 결정
     - C-38 → D-29: 기간 밖 정지는 `PAUSE_OUT_OF_PERIOD` (400)
     - C-39 → D-30: 당일 출입 후 오늘 시작 정지는 `PAUSE_START_DATE_USED` (409)
   - `docs: D-29 · D-30 결정 반영` (1bfba8c)
   - 시작일 상한 검사 순서는 상시 결정(입력 범위)으로 풀려 묻지 않았다
4. 수정 1회차
   - `fix: 정지 시작일 상한 · 당일 출입 · 종결 상태 해제 검사` (6e630d2)
   - `fix: 정지 시작일 회원권 기간 범위 검사` (9cb0f8a)
   - `TransactionTemplate`를 `REQUIRES_NEW`로 바꿨다
   - 중간에 멈춤 (아래 "설계와 다르게 간 것")
   - `docs: 정지 등록 판정 순서 명시` (c8e6a49)
5. 재점검 1
   - verifier 높음 1
     - 앞선 정지를 해제하면, 늘어난 종료일에 맞춰 건 뒤 정지가 기간 밖에 남는다
   - 사용자 결정: C-40 → D-29 보충
     - 그런 해제는 `PAUSE_NOT_RELEASABLE`
     - 커밋 617fac1
   - 수정 2회차: `fix: 정지 해제 후 기간 밖에 남는 정지 거부` (d56df9a)
6. 재점검 2
   - reviewer가 C-40의 빈틈을 찾았다
     - 남은 정지의 시작일만 비교한다
     - 2일 이상인 정지는 끝이 새 종료일을 넘을 수 있다
   - 수정 라운드 상한(2회)에 닿았다
     - 사용자에게 보고했다
     - 한 번 더 고치기로 승인받았다
   - 결정 보충: 비교 기준을 정지 종료일로 바꾼다
     - 커밋 f7cb8f5
     - 커밋 3000d61 (01 결정 기록 정정)
   - 수정 3회차: `fix: 정지 해제 검사 기준을 정지 종료일로 변경` (9ee740d)
7. 재점검 3
   - reviewer 높음 0
   - verifier 높음 0
8. `http/pause.http` 실측 (47건)
   - `test: F4 .http 실행 케이스` (f2787ed)
   - 해제 순서 케이스 추가 (f6ef6b7)
   - 종료일 경계 케이스 추가 (45a3207)
9. 작업 로그 작성

## 바뀐 파일

- 출처
  - `git diff --name-status main...HEAD -- src http`
  - `git diff --stat main...HEAD -- docs`
- 경로 앞부분 표기
  - main: `src/main/java/com/gym/pass/`
  - test: `src/test/java/com/gym/pass/`

### 신규 main

- `domain/membership/MembershipPause` (생성)
- `domain/membership/MembershipPauseLimits` (생성)
- `domain/membership/MembershipPauseResult` (생성)
- `domain/membership/MembershipPauseService` (생성)
- `application/membership/MembershipPauseApplicationService` (생성)
- `application/membership/MembershipPauseCommand` (생성)
- `application/membership/MembershipPauseInfo` (생성)
- `presentation/membership/MembershipPauseController` (생성)
- `presentation/membership/MembershipPauseRequest` (생성)
- `presentation/membership/MembershipPauseResponse` (생성)

### 수정 main

- `domain/membership/Membership` (수정)
- `domain/membership/MembershipHistory` (수정)
- `domain/membership/MembershipRepository` (수정)
- `domain/exception/ErrorCode` (수정)
  - `PAUSE_OUT_OF_PERIOD` 추가
  - `PAUSE_START_DATE_USED` 추가
- `domain/attendance/AttendanceRecordRepository` (수정) — `existsOn` 추가
- `infrastructure/persistence/membership/MembershipJpaRepository` (수정)
- `infrastructure/persistence/membership/MembershipRepositoryImpl` (수정)
- `infrastructure/persistence/attendance/AttendanceRecordJpaRepository` (수정)
- `infrastructure/persistence/attendance/AttendanceRecordRepositoryImpl` (수정)
- `support/properties/MembershipProperties` (수정)
  - `gym.membership.pause.max-count` 3
  - `gym.membership.pause.days-per-month` 7
- `src/main/resources/application.yml` (수정)

### 신규 test

- `domain/membership/MembershipPauseTest` (생성)
- `domain/membership/MembershipPauseExtensionTest` (생성) — TC-4-02 파라미터
- `application/membership/MembershipPauseReleaseTest` (생성)
- `application/membership/MembershipPauseConcurrencyTest` (생성)
- `presentation/membership/MembershipPauseApiTest` (생성)

### 수정 test

- `support/fixture/AttendanceFixture` (수정)
- `presentation/membership/MembershipApiFixture` (수정)

### 기타

- `http/pause.http` (생성) — 실측 요청 47건
- `docs/design/01` ~ `04` (수정)
  - D-28 ~ D-30
  - D-29 보충
  - 판정 순서
- `docs/task_list.md` (수정)

## 설계대로 한 것

- 정지 등록 · 조기 해제 API
- 정지 상한 · 겹침 · 종료일 연장 규칙 (MembershipPause)
- 정지 상한 수치는 설정으로 둔다
  - 최대 횟수 3
  - 월당 일수 7
- 리뷰 전후 사용자 결정 반영
  - D-28: 잔여 0인 횟수제는 정지·취소 불가 (C-37)
  - D-29: 회원권 기간 밖 정지는 `PAUSE_OUT_OF_PERIOD` 400 (C-38)
  - D-29 보충: 해제 후 기간 밖에 남는 정지가 생기면 `PAUSE_NOT_RELEASABLE` (C-40)
    - 비교 기준은 남은 정지의 종료일
  - D-30: 당일 출입 후 오늘 시작 정지는 `PAUSE_START_DATE_USED` 409 (C-39)
- 종결 상태 회원권의 해제 거부
  - 03 §4에 규칙이 있어 결정 없이 코드만 고쳤다

## 설계에 없어서 정한 것

- memberId 조회 위치
  - 고른 것
    - 트랜잭션 밖에서 memberId만 조회한다
    - `TransactionTemplate` 안에서 member 락 → membership 락 순서로 잡는다
  - 이유: 같은 트랜잭션에서 조회하면 스냅샷이 락보다 먼저 고정된다
  - 실험: 조회를 트랜잭션 안으로 옮기자 3건이어야 할 정지가 10건 등록됐다
- `TransactionTemplate` 전파
  - 고른 것: `REQUIRES_NEW`
  - 이유: 기본값(REQUIRED)은 바깥 트랜잭션에 합류해 새 스냅샷을 얻지 못한다
  - 전제: 호출자에 트랜잭션이 없어야 한다
- 정지 상한 설정 키
  - 고른 것: `gym.membership.pause.*`
- 정지 id 획득
  - 고른 것: `flush()` 호출
- 판정 순서
  - 고른 것: 04 API-4 Errors 표의 순서
- 시작일 상한 검사 순서
  - 고른 것: 날짜 계산(`plusDays`) 전에 입력 범위를 검사한다
  - 근거: 상시 결정(입력 범위)
- 이미 끝난 미해제 정지는 해제 검사에서 따로 제외하지 않는다
  - 근거: 불변식 "미해제 정지의 끝 ≤ 회원권 종료일"
  - 이 불변식 때문에 새 종료일은 항상 오늘 이후다
  - reviewer가 이 근거가 코드로 보장된다고 확인했다

## 설계와 다르게 간 것

- 수정 1회차의 판정 순서 (되돌림)
  - 무엇을: 메인 세션이 D-29 검사를 `NOT_PAUSABLE`보다 앞에 두라고 지시했다
  - 왜 문제인가: 날짜가 지나 만료된 회원권도 400을 받는다
  - 처리
    - 구현을 멈추고 보고했다
    - 선택지 1(`NOT_PAUSABLE` 먼저)로 정했다
    - 04 API-4에 판정 순서 블록을 추가했다 (c8e6a49)
- 그 외 없음

## 검증

- `./gradlew spotlessApply build` → BUILD SUCCESSFUL (Spotless 검사 포함), 전체 테스트 233개, 실패 0
  - TC-4-01 ~ TC-4-20이 모두 `@DisplayName`에 있다
  - TC-4-02는 파라미터 14행
    - 평년 2월
    - 윤년 2월
    - 연말 넘김
    - 2/28 종료일 당일 정지
    - 2/29 종료일 당일 정지
    - 해제 행 5개
- `.http` 실측 → `http/pause.http` 47건 모두 일치
  - 대상 TC
    - TC-4-01 · 03 · 04 · 05 · 06 · 07 · 08 · 09 · 10
    - TC-4-11 · 12 · 13 · 14 · 15 · 16 · 17 · 18 · 19 · 20
  - 추가 케이스
    - 시작일 상한 +10000
    - 해제 순서
    - 종료일 경계
  - 서버 로그 ERROR 0
- 실측하지 않은 것
  - TC-4-02 (월말 · 2월 파라미터): 도메인 테스트로 확인

### 1차 리뷰 · 검증 지적 (처리함)

- 높음: 회원권 기간 밖 정지로 종료일만 늘어남
  - 출처: reviewer · verifier
  - → C-38 → D-29 (1bfba8c)
  - → 코드 수정 (9cb0f8a)
- 높음: 시작일 상한 검사보다 `plusDays`가 먼저 돌아 500
  - 출처: reviewer
  - → 상시 결정(입력 범위)으로 수정 (6e630d2)
- 높음: 오늘 출입한 회원의 오늘 시작 정지로 이용일이 이중 계산됨
  - 출처: verifier
  - 분석 S-28에서 빠진 시나리오
  - → C-39 → D-30 (1bfba8c)
  - → 코드 수정 (6e630d2)
- 높음: 종결 상태 회원권의 해제
  - 원래 중간이었다
  - 03 §4에 규칙이 있어 코드만 고쳤다 (6e630d2)

### 재점검 지적 (처리함)

- 재점검 1 · verifier 높음: 앞선 정지 해제 후 뒤 정지가 기간 밖에 남음
  - → C-40 → D-29 보충 (617fac1)
  - → 코드 수정 (d56df9a)
- 재점검 2 · reviewer: C-40이 정지 시작일만 비교함
  - → 결정 보충 (f7cb8f5 · 3000d61)
  - → 코드 수정 (9ee740d)
  - 수정 라운드 상한 초과분은 사용자 승인을 받았다
- 재점검 3: reviewer · verifier 높음 0

## 에러와 해결

- 에러: 동시 정지 요청에서 3건이어야 할 정지가 10건 등록됨 (실험)
  - 원인: memberId 조회가 락보다 먼저 스냅샷을 고정했다
  - 해결
    - memberId 조회를 트랜잭션 밖으로 옮긴다
    - `TransactionTemplate`(`REQUIRES_NEW`) 안에서 락부터 잡는다
- 에러: 먼 미래 시작일에서 500
  - 원인: ISO 파싱은 `+999999999-12-31`까지 받는다
    - `plusDays`가 `DateTimeException`을 던졌다
  - 해결: 날짜 계산 전에 입력 범위를 검사한다
- 에러: 기간 밖 정지로 회원권 종료일만 늘어남
  - 원인: 정지 구간이 회원권 기간 안에 있는지 검사하지 않았다
  - 해결: D-29 `PAUSE_OUT_OF_PERIOD`
- 에러: 해제 후 남은 정지가 새 종료일을 넘음
  - 원인: 남은 정지의 시작일만 비교했다
    - 경계 테스트가 1일짜리 정지뿐이었다
  - 해결: 정지 종료일로 비교한다 (9ee740d)
  - 종료일 경계 케이스를 `.http`에 추가했다 (45a3207)

## 자주 틀리는 것 후보

- 날짜 계산을 범위 검사보다 먼저 한다
  - → ISO 파싱은 `+999999999-12-31`까지 받아 `plusDays`가 `DateTimeException`을 던지고 500이 된다
  - 범위 검사를 먼저 한다
  - 대상: 루트 CLAUDE.md "자주 틀리는 것"
- 하위 기간이 부모 기간 안에 있는지 검사하지 않는다
  - → 정지가 회원권 기간 밖이어도 종료일만 늘어나 규칙에 없는 혜택이 생긴다
  - 대상: 루트 CLAUDE.md "자주 틀리는 것"
- 여러 날 구간을 시작일로만 비교하고, 경계 테스트를 1일짜리로만 만든다
  - → 구간 끝이 기준일을 넘는 경우를 놓친다
  - 대상: 루트 CLAUDE.md "자주 틀리는 것"
- `TransactionTemplate`를 기본 전파(REQUIRED)로 쓴다
  - → 바깥 트랜잭션에 합류해 새 스냅샷을 얻지 못한다
  - 새 스냅샷이 필요하면 `REQUIRES_NEW`를 쓴다
  - 호출자에 트랜잭션이 없어야 한다는 전제를 문서에 적는다
  - 대상: 루트 CLAUDE.md "자주 틀리는 것"
