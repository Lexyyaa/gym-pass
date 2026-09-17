# Task List

> 무엇을 어떤 순서로 하고, 어디까지 했는지만 적는다.
> 내용은 ID로 참조만 한다.
> - 요구사항·TC: [02](design/02-requirements.md)
> - API: [04](design/04-api-spec.md)
>

**현재:** F2 / T2-2

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
| F2 회원·회원권 등록 | +1:35 | |
| F3 출입 | +2:00 | |
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
- [ ] T2-2 `feat: 회원·회원권 등록 API 구현` — FR-2.3 · API-1 · API-2
- [ ] T2-3 `test: 등록 성공·실패 케이스` — TC-2-01 · TC-2-02 · TC-2-05 · TC-2-07 · TC-2-08 · TC-2-09 · TC-2-11 · TC-2-12 · TC-2-13
- [ ] T2-4 `test: 종료일 계산·재등록 경계 엣지 케이스` — TC-2-03 · TC-2-04 · TC-2-10
- [ ] T2-5 `test: 동시 등록 방어` — TC-2-06 · NFR-3
- [ ] T2-6 `test: F2 .http 실행 케이스`
- [ ] T2-7 `docs: F2 작업 로그`

- T2-1의 FR-5.5는 `MembershipHistory` 엔티티와 `REGISTERED` 이력까지다 (03 §3.3)
- T2-1은 member · membership seed를 함께 넣는다 (branch seed는 T1-3)
- T2-3은 주입 MockMvc로 API-1 · API-2의 헤더 누락 400 · 없는 지점 404를 한 번 더 확인한다 (F1 리뷰 — `WebConfig` 등록 검증)

## F3. 출입 `feature/attendance` — FR-3.1 ~ FR-3.4

- [ ] T3-1 `feat: AttendanceRecord 및 출입 판정·차감 구현` — FR-3.2 · FR-3.3 · FR-3.4
- [ ] T3-2 `feat: 출입 기록 API 구현` — FR-3.1 · API-3 · NFR-4
- [ ] T3-3 `test: 출입 성공·실패 케이스` — TC-3-01 · TC-3-02 · TC-3-04 · TC-3-08
- [ ] T3-4 `test: 출입 경계 엣지 케이스` — TC-3-03 · TC-3-05 · TC-3-06
- [ ] T3-5 `test: 동시 출입 하루 1회 차감` — TC-3-07 · NFR-2 · NFR-4
- [ ] T3-6 `test: F3 .http 실행 케이스`
- [ ] T3-7 `docs: F3 작업 로그`

## F4. 정지와 연장 `feature/pause` — FR-4.1 ~ FR-4.4

- [ ] T4-1 `feat: MembershipPause 및 상한·겹침·연장 규칙 구현` — FR-4.1 · FR-4.2 · FR-4.3
- [ ] T4-2 `feat: 정지 등록·조기 해제 API 구현` — API-4 · API-5
- [ ] T4-3 `test: 정지 성공·실패 케이스` — TC-4-01 · TC-4-03 · TC-4-04 · TC-4-05 · TC-4-06 · TC-4-08 · TC-4-09 · TC-4-10 · TC-4-11 · TC-4-14 · TC-4-15 · TC-4-16 · FR-4.4
- [ ] T4-4 `test: 정지 연장·해제 엣지 케이스` — TC-4-02 · TC-4-07 · TC-4-12 · TC-4-13
- [ ] T4-5 `test: F4 .http 실행 케이스`
- [ ] T4-6 `docs: F4 작업 로그`

## F5. 조회 `feature/query` — FR-5.1 ~ FR-5.6

- [ ] T5-1 `feat: 회원권 목록 조회 API·날짜 보정 및 인덱스 구현` — FR-5.1 · FR-5.2 · API-6 · NFR-5
- [ ] T5-2 `feat: 출입·변경 이력 조회 API 구현` — FR-5.3 · FR-5.4 · FR-5.5 · API-7 · API-8
- [ ] T5-3 `feat: 회원권 상태 동기화 배치 구현` — FR-5.6
- [ ] T5-4 `test: 조회 성공·실패 케이스` — TC-5-01 · TC-5-02 · TC-5-03 · TC-5-04 · NFR-1
- [ ] T5-5 `test: 목록 쿼리 실행 계획 검증` — TC-5-05 · NFR-5
- [ ] T5-6 `test: 상태 동기화 경계·멱등 및 날짜 보정 케이스` — TC-5-06 · TC-5-07 · TC-5-08 · TC-5-09
- [ ] T5-7 `test: F5 .http 실행 케이스`
- [ ] T5-8 `docs: F5 작업 로그`

- T5-1의 날짜 보정은 상태 필터 조건과 응답 상태 보정이다 (D-21, 04 API-6)
- T5-3은 00:00 KST cron과 3단계 bulk UPDATE다 (03 §5)
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

## 결정 필요 / 보류

<!--
진행 중에 생긴 것만 한 줄로 적는다.
결정되면 02 §5(D)로 옮기고 여기서는 지운다.
시간이 없어 미룬 작업은 이유와 함께 남긴다.
- README의 "한계"로 옮길 후보
-->

- 
