# F3. 출입 기록 `feature/attendance`

## 받은 지시

- `/run-feature F3`
- task_list F3의 작업 T3-1 ~ T3-5 구현
  - T3-1 AttendanceRecord 및 출입 판정·차감
  - T3-2 출입 기록 API
  - T3-3 출입 성공·실패 케이스
  - T3-4 출입 경계 엣지 케이스
  - T3-5 동시 출입 하루 1회 차감
- 시간
  - 시작 19:09
  - 구현 완료 약 19:19
  - 결정 · 수정 · 재점검 · 실측 완료 19:34
  - 소요 약 25분
    - 구현 10분
    - 리뷰 → 결정 → 수정 순환 약 15분
  - 기능 리뷰 모드를 처음 적용했다
    - 높음(동작 · 설계 모순) 2건만 사용자에게 물었다
    - 중간 · 낮음은 묻지 않았다

## 작업 흐름

1. `implementer`가 T3-1 ~ T3-5 구현 · 커밋 (19:09 ~ 약 19:19)
   - T3-1 `feat: AttendanceRecord 및 출입 판정·차감 구현` (0e7c811)
   - T3-2 `feat: 출입 기록 API 구현` (ba7c8fa)
   - T3-3 `test: 출입 성공·실패 케이스` (7f07519)
   - T3-4 `test: 출입 경계 엣지 케이스` (6309a05)
   - T3-5 `test: 동시 출입 하루 1회 차감` (4cf0842)
2. 1차 `reviewer` + `verifier` (19:20~)
   - reviewer: 높음 0
   - verifier: 높음 1 (횟수제 소진 당일 재출입 409)
   - 사용자 결정
     - C-35 → D-26 (락 순서)
     - C-36 → D-27 (소진 당일 재출입 허용)
3. 결정 반영
   - `docs: D-26 결정 반영` (5570694)
   - `docs: D-27 결정 반영` (5d5795b)
4. 수정
   - `fix: 출입 락을 회원 행 → 회원권 행 순서로 변경` (3440587)
   - `fix: 횟수제 소진 당일 재출입 허용` (88ee19e)
5. 2차 `reviewer` + `verifier` (수정분 대상)
   - reviewer: 높음 0 · 낮음 2
   - verifier: 높음 0 · 중간 0 · 낮음 6
   - 남은 항목은 task_list "리뷰 백로그"(F3 행)에 기록
     - 기능 리뷰 모드라 묻지 않고 wrap-up에서 처리한다
6. `http/attendance.http` 실측 (13건)
7. 메인 세션 실서버 동시성 확인
8. 작업 로그 작성

## 바뀐 파일

- `src/main/java/com/gym/pass/application/attendance/AttendanceApplicationService.java` (생성) — 출입 트랜잭션
- `src/main/java/com/gym/pass/application/attendance/AttendanceCommand.java` (생성) — Command 홀더
- `src/main/java/com/gym/pass/application/attendance/AttendanceInfo.java` (생성) — Info 홀더
- `src/main/java/com/gym/pass/domain/attendance/AttendanceRecord.java` (생성) — 출입 기록 엔티티
- `src/main/java/com/gym/pass/domain/attendance/AttendanceRecordRepository.java` (생성) — 도메인 저장소 인터페이스
- `src/main/java/com/gym/pass/domain/attendance/AttendanceService.java` (생성) — 락 → 출입 판정 → 차감 → 기록
- `src/main/java/com/gym/pass/domain/attendance/exception/AttendanceException.java` (생성) — 출입 도메인 예외
- `src/main/java/com/gym/pass/infrastructure/persistence/attendance/AttendanceRecordJpaRepository.java` (생성) — Spring Data JPA
- `src/main/java/com/gym/pass/infrastructure/persistence/attendance/AttendanceRecordRepositoryImpl.java` (생성) — 저장소 구현
- `src/main/java/com/gym/pass/presentation/attendance/AttendanceController.java` (생성) — API-3
- `src/main/java/com/gym/pass/presentation/attendance/AttendanceRequest.java` (생성) — Request 홀더 · `toCommand`
- `src/main/java/com/gym/pass/presentation/attendance/AttendanceResponse.java` (생성) — Response 홀더
- `src/main/java/com/gym/pass/domain/membership/Membership.java` (수정) — `validateEntry(today, deductedToday)` · `deduct`
- `src/main/java/com/gym/pass/domain/membership/MembershipHistory.java` (수정) — 차감 이력(DEDUCTED)
- `src/main/java/com/gym/pass/domain/membership/MembershipRepository.java` (수정) — 출입 후보 id 조회 · PK 락 조회
- `src/main/java/com/gym/pass/domain/membership/MembershipStatus.java` (수정) — `isUsable()` 추가 (ACTIVE · PAUSED → true)
- `src/main/java/com/gym/pass/infrastructure/persistence/membership/MembershipJpaRepository.java` (수정) — 후보 id 조회 · PK 락 쿼리
- `src/main/java/com/gym/pass/infrastructure/persistence/membership/MembershipRepositoryImpl.java` (수정) — 저장소 구현
- `src/test/java/com/gym/pass/application/attendance/AttendanceEnterTest.java` (생성) — 출입 판정 · 차감 테스트
- `src/test/java/com/gym/pass/application/attendance/AttendanceEnterConcurrencyTest.java` (생성) — 동시 출입 테스트
- `src/test/java/com/gym/pass/domain/membership/MembershipEntryTest.java` (생성) — 회원권 출입 판정 도메인 테스트
- `src/test/java/com/gym/pass/infrastructure/persistence/attendance/AttendanceRecordSchemaTest.java` (생성) — 출입 기록 스키마 검사
- `src/test/java/com/gym/pass/presentation/attendance/AttendanceEnterApiTest.java` (생성) — API-3 테스트
- `src/test/java/com/gym/pass/support/fixture/AttendanceFixture.java` (생성) — 테스트 fixture
- `http/attendance.http` (생성) — 실측 요청 13건
- `docs/design/01` ~ `04` (수정) — D-26 · D-27 반영
- `docs/task_list.md` (수정) — 작업 체크 · 리뷰 백로그

## 설계대로 한 것

- 출입 API (API-3)
- 출입 기록 AttendanceRecord 저장
  - 실패한 판정은 저장하지 않는다
- 횟수제 차감은 하루 1회 (D-11)
  - `deduct`는 오늘 차감 기록이 없을 때만 한다
  - 차감 시 DEDUCTED 이력 1건
- 리뷰 후 사용자 결정 반영
  - D-26: 회원권을 바꾸는 요청의 락 순서 (C-35)
    - member 행 PK 락(첫 쿼리) → membership 행 PK 락
    - 대상: 출입 · 정지 · 해제 · 취소
  - D-27: 횟수제 소진 당일 재출입 허용, 차감 없음 (C-36)
    - 잔여 0이어도 당일은 상태를 유지한다
    - EXPIRED 전환은 00:00 배치(F5)가 한다

## 설계에 없어서 정한 것

- 정지 판정 시점
  - 고른 것: F4로 미룬다. `ATTENDANCE_MEMBERSHIP_PAUSED`는 아직 던지지 않는다
  - 이유: 저장된 상태 대신 정지 구간으로 판정해야 한다 (H-10)
- `deduct` 거부 코드
  - 고른 것: `ATTENDANCE_NO_VALID_MEMBERSHIP`
- 검사 순서
  - 고른 것: 지점 확인이 유효 판정보다 먼저
- `entryAt` 정밀도
  - 고른 것: 초 단위로 절삭
- 적용한 상시 결정
  - 애그리거트 간 논리 참조 (물리 FK 없음)
  - `BaseTimeEntity` 상속

## 설계와 다르게 간 것

- 1차 구현의 락 방식 (수정함)
  - 무엇을: 후보 조회 자체를 `FOR UPDATE`로 했다
    - 인덱스 범위 락이 걸려 03 §7의 "PK 1행 락"과 달랐다
  - 왜: 락 앞에 일반 SELECT를 두면 스냅샷이 먼저 잡혀 이중 차감 위험이 있다
  - 처리: D-26 결정 후 member 행 → membership 행 PK 락으로 바꿨다 (3440587)
- 그 외 없음

## 검증

- `./gradlew spotlessApply build` → BUILD SUCCESSFUL (Spotless 검사 포함), 전체 테스트 138개, 실패 0
  - TC-3-01 ~ TC-3-08이 `@DisplayName`에 있다
  - TC-3-09 · TC-3-10은 테스트는 있지만 DisplayName에 ID가 없다 (백로그)
- `.http` 실측 → `http/attendance.http` 13건 모두 일치
  - 대상 TC
    - TC-3-01 · 02 · 03 · 04 · 08 · 09
    - TC-3-06 (당일 재출입 포함)
    - TC-1-03
    - memberId 누락
    - 준비 요청
  - 서버 로그 ERROR 0
- 실서버 동시성 확인 (메인 세션)
  - 조건: 횟수제 5회 회원에게 동시 출입 20건
  - 응답: 전부 200
  - 잔여: 5 → 4
  - 출입 기록: 20건
  - 차감: 1건
  - DEDUCTED 이력: 1건
  - 서버 로그 ERROR 0
- 실측하지 않은 것
  - `ATTENDANCE_MEMBERSHIP_PAUSED`: F4

### 1차 리뷰 · 검증 지적 (처리함)

- verifier 높음: 횟수제 마지막 1회를 쓴 날 같은 날 재출입이 409
  - 문서끼리 모순
    - D-11 · FR-3.4 · TC-3-07: 성공
    - FR-3.2 · 03 §4 · 04 API-3: 잔여 0이면 409
  - → C-36 → D-27 (5d5795b)
  - → 코드 수정 (88ee19e)
  - → TC-3-07 단언 변경: 잔여 1에서 20건 모두 성공, 차감 1회
- 락 방식이 03 §7과 다름
  - → C-35 → D-26 (5570694)
  - → 코드 수정 (3440587)

### 2차 리뷰 · 검증 지적 (남김)

- reviewer 낮음 2 · verifier 낮음 6
- 남긴 사유: 기능 리뷰 모드라 중간 이하는 묻지 않고 기록만 한다
- 기록 위치: task_list "리뷰 백로그" F3 행
- 처리 시점: wrap-up

## 에러와 해결

- 에러: 횟수제 소진 당일 재출입이 409
  - 원인: 설계 문서끼리 모순 (D-11 · FR-3.4 · TC-3-07 ↔ FR-3.2 · 03 §4 · 04 API-3)
  - 해결: D-27 결정 → `validateEntry(today, deductedToday)`
- 에러: `FOR UPDATE`가 최신 값 대신 옛 값을 돌려줌
  - 원인: 후보 엔티티를 먼저 조회해 1차 캐시에 올렸다
  - 해결: 후보는 id만 조회한다
- 락 필요성 실험 (3440587)
  - 두 락 모두 제거: 데드락
  - member 락 제거: 실패
  - membership 락 제거: 통과
    - member 락이 이미 직렬화한다
    - membership 락은 배치 경합 방어용으로 둔다
- 남은 한계 (D-27)
  - 소진 당일에는 같은 회원의 신규 등록이 거부된다 (다음 날부터 가능)
  - 소진 당일 정지 · 취소 허용 여부는 F4 착수 전에 정한다

## 자주 틀리는 것 후보

- 락을 두 단계로 잡을 때 뒤의 락이 스냅샷을 새로 잡아 준다고 착각한다
  - → REPEATABLE READ 스냅샷은 첫 일반 조회 시점에 고정된다
  - 정합성의 근거는 그 조회 앞에 잡은 락이다
  - 대상: 루트 CLAUDE.md "자주 틀리는 것"
- `FOR UPDATE` 전에 같은 엔티티를 일반 조회로 영속성 컨텍스트에 올린다
  - → 잠금 조회가 새 값 대신 1차 캐시의 옛 값을 돌려준다
  - 후보는 id만 조회한다
  - 대상: 루트 CLAUDE.md "자주 틀리는 것"
- 범위 조건 `FOR UPDATE`가 대상이 없으면 락을 안 건다고 생각한다
  - → 인접 인덱스 구간에 갭 락을 건다
  - 대상: 루트 CLAUDE.md "자주 틀리는 것"
