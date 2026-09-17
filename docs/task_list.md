# Task List

> 무엇을 어떤 순서로 하고, 어디까지 했는지만 적는다.
> 내용은 ID로 참조만 한다.
> - 요구사항·TC: [02](design/02-requirements.md)
> - API: [04](design/04-api-spec.md)
>

**현재:** F4 / T4-5

## 규칙

- 기능(F) = 브랜치, 작업(T) = 커밋 1개
- 작업 줄의 백틱 안 문장이 **그대로 커밋 메시지**다
- 작업 순서: `feat`(구현) → `test`(성공·실패) → `test`(엣지·동시성)
- 줄 끝에는 이 작업이 충족하는 `FR`·`NFR` 또는 구현하는 `TC`를 적는다
- 끝나면 `[x]`로 바꾸고 맨 위 **현재**를 다음 작업으로 옮긴다
- 모든 `FR`·`NFR`은 아래 어딘가에 한 번 이상 나와야 한다 (`verifier`가 검사)
- 기능 블록의 마지막 두 줄(`.http` · 작업 로그)
  - `implementer`가 아니라 `/run-feature`가 처리한다

## 시간 계획

<!--
과제 시작 시각 기준으로 채운다.
기능이 끝날 때 실제 종료 시각을 적어 밀림을 바로 본다.
-->

**시작:** 13:03

| 구간 | 목표 종료 | 실제 종료 |
|---|---|---|
| F0 분석 | +0:15 | +0:09 (13:12) |
| F0 결정 | +0:25 | +0:22 (13:25) — 설계 점검 후 추가 결정 C-19~C-26 포함 시 +1:23 |
| F0 설계 문서 · 점검 | +1:00 | +1:25 (14:28) |
| F1 기초 설정 | +1:15 | +1:39 (14:42) |
| F2 회원·회원권 등록 | +1:35 | 19:02 (+5:59, 사용량 한도로 15:03~18:21 중단 — 중단 제외 약 +2:41) |
| F3 출입 | +2:00 | 19:34 (시작 19:09, 소요 약 0:25) |
| F4 정지와 연장 | +2:20 | |
| F5 조회 | +2:35 | |
| F6 만료 안내 | +2:45 | |
| F7 관리 부가 (선택) | — | |
| F9 마무리 | +3:00 | |

- 마감 +3:00 = 16:03, 별도 버퍼 없음 — 구간이 밀리면 F7부터 포기한다
- F 순서는 분석 §5.2 평가 비중 순 (동시 정합성 · 핵심 흐름 → 경계 → 인덱스)
  - F3(동시 출입)에 가장 긴 25분을 배정한다
- F7은 D-18 최후순위 — F6이 +2:45 전에 끝난 경우에만 착수
  - 시간 초과 시 미구현, README에 방향만 기재 (SUB-7)

---

## F0. 설계 `feature/design`

- [x] T0-1 `docs: 과제 원문 추가`
- [x] T0-2 `docs: 요구사항 분석`
- [x] T0-3 `docs: 요구사항 구체화 및 테스트 케이스 작성`
- [x] T0-4 `docs: 도메인 모델 및 ERD 작성`
- [x] T0-5 `docs: API 명세 및 에러 코드 작성`
- [x] T0-6 `docs: 작업 목록 작성`

## F1. 기초 설정 `feature/setup`

- [x] T1-1 `feat: 공통 값 객체 및 enum 구현` — 03 §1 · NFR-1 · NFR-9
- [x] T1-2 `feat: 도메인 에러 코드 추가` — 04 §4 · NFR-8
- [x] T1-3 `feat: Branch 엔티티 및 지점 seed 구현` — 03 §9 branch 행 대조 완료 체크
- [x] T1-4 `test: 값 객체 및 지점 헤더 검증 케이스` — TC-1-01 · TC-1-02 · TC-1-03 · TC-1-04
- [x] T1-5 `docs: F1 작업 로그`

- T1-1은 지점 헤더 파싱 · 공통 예외 · 페이지 응답을 포함한다 (03 §2 `common`)
- T1-3은 branch seed만 넣는다 — member · membership seed는 T2-1 (03 §9 투입 시점)

## F2. 회원·회원권 등록 `feature/registration` — FR-2.1 ~ FR-2.4

- [x] T2-1 `feat: Member·Membership 애그리거트·종료일 계산 및 seed 구현` — FR-2.1 · FR-2.2 · FR-2.4 · FR-5.5 · 03 §9 member·membership 행 대조 완료 체크
- [x] T2-2 `feat: 회원·회원권 등록 API 구현` — FR-2.3 · API-1 · API-2
- [x] T2-3 `test: 등록 성공·실패 케이스` — TC-2-01 · TC-2-02 · TC-2-05 · TC-2-07 · TC-2-08 · TC-2-09 · TC-2-11 · TC-2-12 · TC-2-13 · TC-2-14 · TC-2-15 · TC-2-16 · TC-2-17 · TC-2-18
- [x] T2-4 `test: 종료일 계산·재등록 경계 엣지 케이스` — TC-2-03 · TC-2-04 · TC-2-10
- [x] T2-5 `test: 동시 등록 방어` — TC-2-06 · NFR-3
- [x] T2-6 `test: F2 .http 실행 케이스`
- [x] T2-7 `docs: F2 작업 로그`

- T2-1의 FR-5.5는 `MembershipHistory` 엔티티와 `REGISTERED` 이력까지다 (03 §3.3)
- T2-1은 member · membership seed를 함께 넣는다 (branch seed는 T1-3)
- TC-2-14~17은 F2 리뷰 수정(fix 커밋)에서 구현
- TC-2-18은 기존 테스트에 DisplayName을 붙여 연결한다
- T2-3은 주입 MockMvc로 API-1 · API-2의 헤더 누락 400 · 없는 지점 404를 한 번 더 확인한다 (F1 리뷰 — `WebConfig` 등록 검증)

## F3. 출입 `feature/attendance` — FR-3.1 ~ FR-3.4

- [x] T3-1 `feat: AttendanceRecord 및 출입 판정·차감 구현` — FR-3.2 · FR-3.3 · FR-3.4
- [x] T3-2 `feat: 출입 기록 API 구현` — FR-3.1 · API-3 · NFR-4
- [x] T3-3 `test: 출입 성공·실패 케이스` — TC-3-01 · TC-3-02 · TC-3-04 · TC-3-08 · TC-3-09
- [x] T3-4 `test: 출입 경계 엣지 케이스` — TC-3-03 · TC-3-05 · TC-3-06 · TC-3-10
- [x] T3-5 `test: 동시 출입 하루 1회 차감` — TC-3-07 · NFR-2 · NFR-4
- [x] T3-6 `test: F3 .http 실행 케이스`
- [x] T3-7 `docs: F3 작업 로그`

## F4. 정지와 연장 `feature/pause` — FR-4.1 ~ FR-4.4

- [x] T4-1 `feat: MembershipPause 및 상한·겹침·연장 규칙 구현` — FR-4.1 · FR-4.2 · FR-4.3
- [x] T4-2 `feat: 정지 등록·조기 해제 API 구현` — API-4 · API-5
- [x] T4-3 `test: 정지 성공·실패 케이스` — TC-4-01 · TC-4-03 · TC-4-04 · TC-4-05 · TC-4-06 · TC-4-08 · TC-4-09 · TC-4-10 · TC-4-11 · TC-4-14 · TC-4-15 · TC-4-16 · TC-4-17 · TC-4-18 · TC-4-19 · FR-4.4
- [x] T4-4 `test: 정지 연장·해제 엣지 케이스` — TC-4-02 · TC-4-07 · TC-4-12 · TC-4-13
- [ ] T4-5 `test: F4 .http 실행 케이스`
- [ ] T4-6 `docs: F4 작업 로그`

- 잔여 0인 횟수제는 정지 거부 (D-28, TC-4-17) — T4-3에 포함
- TC-4-18 · TC-4-19(D-29 · D-30)는 F4 리뷰 수정(fix 커밋)에서 구현

## F5. 조회 `feature/query` — FR-5.1 ~ FR-5.6

- [ ] T5-1 `feat: 회원권 목록 조회 API·날짜 보정 및 인덱스 구현` — FR-5.1 · FR-5.2 · API-6 · NFR-5
- [ ] T5-2 `feat: 출입·변경 이력 조회 API 구현` — FR-5.3 · FR-5.4 · FR-5.5 · API-7 · API-8
- [ ] T5-3 `feat: 회원권 상태 동기화 배치 구현` — FR-5.6
- [ ] T5-4 `test: 조회 성공·실패 케이스` — TC-5-01 · TC-5-02 · TC-5-03 · TC-5-04 · NFR-1
- [ ] T5-5 `test: 목록 쿼리 실행 계획 검증` — TC-5-05 · NFR-5
- [ ] T5-6 `test: 상태 동기화 경계·멱등 및 날짜 보정 케이스` — TC-5-06 · TC-5-07 · TC-5-08 · TC-5-09 · TC-5-10
- [ ] T5-7 `test: F5 .http 실행 케이스`
- [ ] T5-8 `docs: F5 작업 로그`

- T5-1의 날짜 보정은 상태 필터 조건과 응답 상태 보정이다 (D-21, 04 API-6)
- T5-3은 00:00 KST cron과 4단계 bulk UPDATE다 (03 §5)
  - 2단계(횟수제 잔여 0 → `EXPIRED`)는 D-27로 추가됐다
- 재점검(N-1)으로 T5-3 · T5-6이 추가돼 기존 T5-3~T5-6을 T5-4 · T5-5 · T5-7 · T5-8로 다시 매겼다

## F6. 만료 안내 `feature/notification` — FR-6.1 ~ FR-6.6

- [ ] T6-1 `feat: NotificationRecord 및 FakeMessageClient 구현` — FR-6.5 · FR-6.6
- [ ] T6-2 `feat: 만료·잔여 안내 배치 및 재시도 구현` — FR-6.1 · FR-6.2 · FR-6.3 · FR-6.4
- [ ] T6-3 `test: 발송·재시도 성공 케이스` — TC-6-05 · TC-6-06
- [ ] T6-4 `test: 대상 선정 경계 엣지 케이스` — TC-6-01 · TC-6-02 · TC-6-03
- [ ] T6-5 `test: 배치 재실행 멱등` — TC-6-04 · NFR-7
- [ ] T6-6 `docs: F6 작업 로그`

- F6은 HTTP API가 없어 `.http` 작업이 없다 (04 §2 배치 동작)

## F7. 회원·회원권 관리 부가 (선택) `feature/member-admin` — FR-7.1 ~ FR-7.4

- [ ] T7-1 `feat: 회원 수정·삭제 및 연락처 중복 검사 구현` — FR-7.1 · FR-7.2 · FR-7.3 · API-9 · API-10
- [ ] T7-2 `feat: 회원권 취소 API 구현` — FR-7.4 · API-11
- [ ] T7-3 `test: 관리 부가 성공·실패 케이스` — TC-7-01 · TC-7-02 · TC-7-03 · TC-7-04 · TC-7-05 · TC-7-06
- [ ] T7-4 `test: F7 .http 실행 케이스`
- [ ] T7-5 `docs: F7 작업 로그`

- D-18 최후순위 — 시간 초과 시 미구현 후보, README에 방향만 기재 (SUB-7)

## F9. 마무리 `feature/docs` — SUB-1 ~ SUB-9

- [ ] T9-1 `chore: 앱 Dockerfile 및 compose app 서비스 구성` — NFR-6
- [ ] T9-2 `docs: .http 실행 케이스 정리` — SUB-3
- [ ] T9-3 `docs: README 작성 (실행 방법·기술 스택 및 선택 이유·API)` — SUB-1 · SUB-2 · SUB-3 · SUB-5 · SUB-6 · SUB-7 · SUB-8
- [ ] T9-4 `docs: AI 활용 내역 정리` — SUB-4

- T9-1은 세 가지를 포함한다
  - `Dockerfile` (앱 이미지)
  - `docker-compose.yml`의 `app` 서비스 (mysql 서비스에 의존)
  - `docker` 프로파일 — datasource url의 호스트를 mysql 서비스명으로
- 재점검(N-9)으로 T9-1이 추가돼 기존 T9-1~T9-3을 T9-2~T9-4로 다시 매겼다
- SUB-9(저장소 제출)는 F9 완료 후 푸시로 충족한다

---

## 리뷰 백로그

기능별 리뷰·검증에서 나온 중간 · 낮음 지적을 쌓는다.
결정 없이 적기만 하고, `/wrap-up`에서 한 번에 처리한다 (문서 쪽은 일괄 수정, 코드 쪽은 사용자에게 묻고 fix 또는 README 한계).

| F | 출처 | 심각도 | 쪽 | 내용 | 처리 |
|---|---|---|---|---|---|
| F2 | reviewer | 낮음 | 테스트 | 성격이 다른 테스트 3개가 TC-2-14 · TC-2-17 ID를 같이 쓴다 (설정 바인딩 · 넘겨받은 상한 · 하한 경계 성공) | |
| F2 | verifier | 낮음 | 문서 | 시작일 > 9999-12-31 사전 검사(`plusMonths` 예외 방지)가 D-23 · 03 §3.3 · 04 §4에 따로 적혀 있지 않다 | |
| F2 | verifier | 낮음 | 문서 | 04 §4 `MEMBERSHIP_INVALID_INPUT` "null · 음수" 문구가 모호하다 (음수 검사는 price만) | |
| F2 | verifier | 낮음 | 문서 | 입력 검증이 member 락보다 먼저 돈다 — 없는 회원 + 잘못된 입력이면 400. 우선순위가 03 §5에 없다 | |
| F2 | verifier | 낮음 | 테스트 | "서비스가 설정값을 도메인에 넘긴다" 연결을 검증하는 테스트가 없다 | |
| F2 | verifier | 낮음 | 문서 | 02 TC-2-07 행에 에러 코드 표기가 없다 (API는 `COMMON_`, 도메인은 `MEMBERSHIP_`) | |
| F2 | reviewer | 낮음 | 코드 | 저장 직후 `MembershipHistory.membershipId`가 메모리에서 null (`insertable = false`) — F4 이후 응답에 쓸 때 주의 | |
| F3 | verifier | 중간 | 테스트 | NFR-4(기록 · 차감 원자성)의 롤백 케이스가 없다 — 차감 뒤 기록 저장 실패 시 잔여가 되돌아가는지 미검증 | |
| F3 | verifier | 낮음 | 테스트 | TC-3-09 · TC-3-10 테스트에 ID가 없다 (task_list는 [x]) · TC-3-09의 "기록 0건" 단언이 항상 참 (`Long.MAX_VALUE`로 필터) | |
| F3 | reviewer | 낮음 | 테스트 | TC ID 없는 테스트 다수 (헤더 누락 · memberId null · 오늘/어제 차감 · 기간제 차감 noop · verifyBranch · 스키마) — 02 TC 표 추가 후보 | |
| F3 | reviewer | 낮음 | 테스트 | `deductedToday = true`로 상태 검사를 우회하지 않는지 EXPIRED · CANCELED 케이스가 없다 (기간 밖만 검증) | |
| F3 | verifier | 낮음 | 테스트 | TC-3-06 "다음 날 거부"가 연속 시나리오가 아니라 전날 기록을 직접 INSERT해 흉내 낸다 | |
| F3 | verifier | 낮음 | 테스트 | "출입 후보 항상 1건 이하" 전제를 깨는 데이터에서 `Optional` 조회가 500이 되는지 검증 없음 (API로는 도달 불가) | |
| F3 | verifier | 낮음 | 테스트 | 출입 테스트가 시스템 시계 기준이라 자정 전후 실행 시 흔들릴 수 있다 | |
| F3 | reviewer | 낮음 | 코드 | `AttendanceService.java:41` · `Membership.java:126` 주석 근거가 부정확 — 정합성 근거는 membership 락이 아니라 member 락 뒤 스냅샷 | |
| F3 | verifier | 낮음 | 문서 | 03 §3.3 `deduct` 실패 조건에 "기간 밖"이 없다 (코드는 `validateEntry(today, false)`로 거부) | |
| F3 | verifier | 낮음 | 문서 | 03 §7 · §8 흐름에 "membership 락 뒤 후보 조건 재검증(409)" 단계가 없다 | |
| F3 | verifier | 낮음 | 문서 | 03 §3.4에 출입 시각 초 단위 절삭이 적혀 있지 않다 | |
| F3 | verifier | 낮음 | 문서 | 04 API-3 Errors 표의 403 조건 "유효 회원권이 타 지점 소속" ↔ 판정 순서 "판정 대상이 타 지점" 표현이 다르다 | |

---

## 결정 필요 / 보류

<!--
진행 중에 생긴 것만 한 줄로 적는다.
결정되면 02 §5(D)로 옮기고 여기서는 지운다.
시간이 없어 미룬 작업은 이유와 함께 남긴다.
- README의 "한계"로 옮길 후보
-->

- 
