# F2. 회원·회원권 등록 `feature/registration`

## 받은 지시

- `/run-feature F2`
- task_list F2의 작업 T2-1 ~ T2-5 구현
  - T2-1 Member·Membership 애그리거트 · 종료일 계산 · seed
  - T2-2 회원·회원권 등록 API
  - T2-3 등록 성공·실패 케이스
  - T2-4 종료일 계산·재등록 경계 엣지 케이스
  - T2-5 동시 등록 방어
- 시간
  - 시작 14:53
  - 구현 완료 15:02
  - 중단 15:03 ~ 18:21 (사용량 한도로 세션 멈춤)
  - 수정·검증 완료 약 18:58
  - F2에 쓴 시간(중단 제외): 약 45분
    - 이 중 약 80%가 리뷰 → 결정 → 수정 순환

## 작업 흐름

1. `implementer`가 T2-1 ~ T2-5 구현 · 커밋 (14:53 ~ 15:02)
   - T2-1 `feat: Member·Membership 애그리거트·종료일 계산 및 seed 구현` (78a287f)
   - T2-2 `feat: 회원·회원권 등록 API 구현` (ea888d6)
   - T2-3 `test: 등록 성공·실패 케이스` (15fff12)
   - T2-4 `test: 종료일 계산·재등록 경계 엣지 케이스` (cd9116a)
   - T2-5 `test: 동시 등록 방어` (07eec66)
2. 세션 중단 (15:03 ~ 18:21)
3. 1차 `reviewer` + `verifier` (18:21~)
   - reviewer: 높음 1 · 중간 2 · 낮음 5
   - verifier: 높음 0 · 중간 2 · 낮음 11
   - 사용자 결정: C-28 ~ C-32 → D-23 ~ D-25, D-7 보충
4. 수정 1회차
   - `docs: D-23 ~ D-25 결정 반영` (f819239)
   - `fix: 회원권 등록 입력 상한 및 반대 종류 값 거부` (3d58116)
   - `fix: 회원권 개월 수 저장` (95033b8)
   - `fix: 등록 테스트 단언 보강` (043b4a3)
5. 2차 `reviewer` + `verifier` + 메인 세션 실측
   - reviewer: 높음 1 (시작일 하한 없음)
   - verifier: 중간 2
   - 사용자 결정: C-33 · C-34
6. 수정 2회차
   - `fix: 회원권 시작일 하한 검사` (1e64a8d)
   - `docs: D-23 보충 결정 반영` (a53cb9a)
   - `refactor: 회원권 입력 상한을 설정값으로 이동` (3b5bc52)
   - `fix: 이력 NOT NULL · 종료일 계산 경로 · 테스트 fixture 정리` (8431dac)
7. 3차 `reviewer` + `verifier`
   - 높음 0 · 중간 0
   - 낮음은 스킬 규칙에 따라 수정하지 않고 아래 "검증"에 기록
8. `http/registration.http` 실측 (25건)
9. 작업 로그 작성

## 바뀐 파일

- `src/main/java/com/gym/pass/GymApplication.java` (수정) — `@ConfigurationPropertiesScan`
- `src/main/java/com/gym/pass/domain/member/Phone.java` (수정) — `isValid()` 추가
- `src/main/java/com/gym/pass/domain/member/Member.java` (생성) — 회원 애그리거트 · 이름·연락처 검증
- `src/main/java/com/gym/pass/domain/member/MemberRepository.java` (생성) — 도메인 저장소 인터페이스
- `src/main/java/com/gym/pass/domain/member/exception/MemberException.java` (생성) — 회원 도메인 예외
- `src/main/java/com/gym/pass/domain/membership/Membership.java` (생성) — 회원권 애그리거트 · `register()`
- `src/main/java/com/gym/pass/domain/membership/MembershipHistory.java` (생성) — 이력 엔티티
- `src/main/java/com/gym/pass/domain/membership/MembershipRegistration.java` (생성) — 등록 입력 VO
- `src/main/java/com/gym/pass/domain/membership/MembershipLimits.java` (생성) — 입력 상한 VO
- `src/main/java/com/gym/pass/domain/membership/MembershipRegistrationService.java` (생성) — 락 → 중복 검사 → 저장
- `src/main/java/com/gym/pass/domain/membership/MembershipRepository.java` (생성) — 도메인 저장소 인터페이스
- `src/main/java/com/gym/pass/domain/membership/MembershipType.java` (수정) — enum 정책 메서드 추가
- `src/main/java/com/gym/pass/domain/membership/exception/MembershipException.java` (생성) — 회원권 도메인 예외
- `src/main/java/com/gym/pass/infrastructure/persistence/member/MemberRepositoryImpl.java` (생성) — 저장소 구현
- `src/main/java/com/gym/pass/infrastructure/persistence/member/MemberJpaRepository.java` (생성) — Spring Data JPA
- `src/main/java/com/gym/pass/infrastructure/persistence/membership/MembershipRepositoryImpl.java` (생성) — 저장소 구현
- `src/main/java/com/gym/pass/infrastructure/persistence/membership/MembershipJpaRepository.java` (생성) — Spring Data JPA
- `src/main/java/com/gym/pass/application/member/MemberApplicationService.java` (생성) — 회원 등록 트랜잭션
- `src/main/java/com/gym/pass/application/member/MemberCommand.java` (생성) — Command 홀더
- `src/main/java/com/gym/pass/application/member/MemberInfo.java` (생성) — Info 홀더
- `src/main/java/com/gym/pass/application/membership/MembershipApplicationService.java` (생성) — 회원권 등록 트랜잭션 · today · 상한 전달
- `src/main/java/com/gym/pass/application/membership/MembershipCommand.java` (생성) — Command 홀더
- `src/main/java/com/gym/pass/application/membership/MembershipInfo.java` (생성) — Info 홀더
- `src/main/java/com/gym/pass/presentation/member/MemberController.java` (생성) — API-1
- `src/main/java/com/gym/pass/presentation/member/MemberRequest.java` (생성) — Request 홀더 · `toCommand`
- `src/main/java/com/gym/pass/presentation/member/MemberResponse.java` (생성) — Response 홀더
- `src/main/java/com/gym/pass/presentation/membership/MembershipController.java` (생성) — API-2
- `src/main/java/com/gym/pass/presentation/membership/MembershipRequest.java` (생성) — Request 홀더 · `toCommand`
- `src/main/java/com/gym/pass/presentation/membership/MembershipResponse.java` (생성) — Response 홀더
- `src/main/java/com/gym/pass/support/config/ClockConfig.java` (생성) — Clock 빈 (Asia/Seoul)
- `src/main/java/com/gym/pass/support/properties/MembershipProperties.java` (생성) — `gym.membership.*` 바인딩
- `src/main/resources/application.yml` (수정) — `max-months` · `max-count`
- `src/main/resources/data.sql` (수정) — member 4행 · membership 4행
- `src/test/java/com/gym/pass/domain/member/MemberCreateTest.java` (생성) — 회원 생성 도메인 테스트
- `src/test/java/com/gym/pass/domain/membership/MembershipRegisterTest.java` (생성) — 등록 · 종료일 계산 도메인 테스트
- `src/test/java/com/gym/pass/presentation/member/MemberRegisterApiTest.java` (생성) — API-1 테스트
- `src/test/java/com/gym/pass/presentation/membership/MembershipRegisterApiTest.java` (생성) — API-2 테스트
- `src/test/java/com/gym/pass/presentation/membership/MembershipReRegisterApiTest.java` (생성) — 재등록 경계 테스트
- `src/test/java/com/gym/pass/presentation/membership/MembershipApiFixture.java` (생성) — 테스트 fixture
- `src/test/java/com/gym/pass/application/membership/MembershipRegisterConcurrencyTest.java` (생성) — 동시 등록 테스트
- `src/test/java/com/gym/pass/infrastructure/persistence/membership/MembershipSchemaTest.java` (생성) — 이력 NOT NULL 스키마 검사
- `src/test/java/com/gym/pass/support/properties/MembershipPropertiesTest.java` (생성) — 설정 바인딩 테스트
- `http/registration.http` (생성) — 실측 요청 25건
- `docs/design/*` (수정) — D-23 ~ D-25 · D-7 보충 · D-23 보충 · TC-2-17 · TC-2-18 반영

## 설계대로 한 것

- Member
  - 이름·연락처 검증
  - 실패 시 `MEMBER_INVALID_INPUT`
  - `Phone.isValid`로 먼저 검사해 VO 예외가 500으로 새지 않게 함
- Membership
  - 정적 팩토리 `register()`
  - 인덱스 4개
  - 이력 컬렉션
- MembershipHistory
  - 등록 시 `REGISTERED` 이력 1건
- MembershipType
  - enum 정책 메서드
    - 종료일 계산
    - 초기 잔여
    - 차감 여부
    - 검증
- MembershipRegistrationService
  - member PK `FOR UPDATE`
  - → 종료일 ≥ 오늘인 ACTIVE·PAUSED 존재 검사
  - → 저장
- 등록 API 2개 (API-1 · API-2)
  - 둘 다 `@BranchId Long`
- data.sql
  - member 4행
  - membership 4행
- 리뷰 후 사용자 결정 반영
  - D-23: 개월 ≤ 120, 횟수 ≤ 1000, 종료일 ≤ 9999-12-31 (C-28)
  - D-23: 반대 종류 값 거부 (C-30)
  - D-23 보충: 시작일 ≥ 1000-01-01 (C-33)
  - D-23 보충: 상한 120 · 1000을 설정값으로 이동 (C-34)
    - `application.yml`의 `gym.membership.max-months` · `max-count`
    - `@ConfigurationProperties`로 읽는다
    - `MembershipLimits` VO로 도메인에 넘긴다
  - D-24: `months` 컬럼 저장 (C-29)
  - D-25: 논리 참조, 물리 FK 없음 (C-31)
  - D-7 보충: 횟수제 6개월 상수 유지 (C-32)

## 설계에 없어서 정한 것

- 오늘 날짜 계산 위치
  - 고른 것: 서비스가 Clock 빈(Asia/Seoul)으로 today를 계산해 도메인에 넘긴다
  - 이유: 도메인을 시간에 대해 순수하게 두고, 테스트에서 today를 Clock 빈에서 얻는다
- 이력 매핑
  - 고른 것: 단방향 `@OneToMany` + cascade PERSIST
  - 이유: 회원권 저장 시 이력을 함께 저장한다
- Request → Command 변환
  - 고른 것: `Request.toCommand(...)`
  - 이유: ArchUnit이 application → presentation 참조를 막는다

## 설계와 다르게 간 것

- 없음

## 검증

- `./gradlew spotlessApply build` → 통과
  - 전체 테스트 108개, 실패 0
  - TC-2-01 ~ TC-2-18이 모두 `@DisplayName`에 있다
- `.http` 실측 → `http/registration.http` 25건 모두 일치
  - 대상 API: API-1 · API-2
  - 대상 TC
    - TC-1-03 · TC-1-04
    - TC-2-01 · 02 · 04 · 05 · 07 · 08 · 11 · 13 ~ 18
    - 준비 요청
  - 대조 항목: 상태코드 · 응답 본문 · DB
  - 불일치 0
  - 서버 로그 ERROR 0
- DB 추가 확인
  - 새 DB에서 `membership_history.membership_id`의 IS_NULLABLE = NO
  - 1000-01-01 경계값이 start `1000-01-01`, end `1000-02-01`로 그대로 저장됨
- 실측으로 도달하지 않는 에러
  - `MEMBER_INVALID_INPUT`: Request 검증이 먼저 막는다
  - `MEMBER_PHONE_DUPLICATED`: F7
  - TC-2-09: 정지 API가 F4라 실측하지 않음 (API 테스트로 확인)

### 1차 리뷰 · 검증 지적 (처리함)

- reviewer 높음: 극단 날짜·개월 입력이 500으로 샌다
  - `plusMonths`의 `DateTimeException`
  - MySQL DATE 범위 초과
  - → D-23 상한 검사 (3d58116)
- reviewer 중간: 횟수제 6개월이 상수다
  - → D-7 보충, 상수 유지
- reviewer 중간: 반대 종류 값을 무시하는 동작이 스펙에 없다
  - → D-23 거부 (3d58116)
- verifier 중간: 정지 상한에 쓸 개월 수를 저장하지 않는다
  - → D-24 (95033b8)
- verifier 중간: ERD의 FK 표기가 코드와 다르다
  - → D-25 (f819239)
- 테스트 단언 보강 (043b4a3)
  - TC-2-11 이력 단언
  - 이력 `membership_id` 값 비교
  - 테스트 today를 Clock 빈에서 얻기

### 2차 리뷰 · 검증 지적 (처리함)

- reviewer 높음: 시작일 하한이 없다
  - 메인 세션 실측: `-0001-01-01`이 201, DB에는 `0002-01-01`로 조용히 바뀌어 저장
  - `0999-01-01`, `0001-01-01`도 201
  - → C-33, 하한 검사 (1e64a8d)
- verifier 중간: 하한이 문서에 없다
  - → D-23 보충 · TC-2-17 · TC-2-18 추가 (a53cb9a)
- verifier 중간: 상한 120·1000의 위치 결정이 기록되지 않았다
  - → C-34, 설정값으로 이동 (3b5bc52)
- 실측 DDL: `membership_history.membership_id`가 `DEFAULT NULL` (문서는 NOT NULL)
  - → NOT NULL 선언 · 스키마 테스트 (8431dac)

### 3차 리뷰 · 검증 지적 — 낮음 (남김)

- 남긴 사유: 스킬 규칙상 중간 이하는 기록만 한다
- 성격이 다른 테스트 3개가 TC-2-14 · TC-2-17 ID를 같이 쓴다
  - 설정 바인딩
  - 넘겨받은 상한 따르기
  - 하한 경계 성공
- 시작일 > 9999-12-31 사전 검사가 문서에 따로 적혀 있지 않다
- 04 §4 "null · 음수" 문구가 모호하다
  - 음수 검사는 price만 한다
- 입력 검증이 member 락보다 먼저 돈다
  - 없는 회원 + 잘못된 입력이면 400
  - 이 우선순위가 문서에 없다
- "서비스가 설정값을 넘긴다"는 연결을 검증하는 테스트가 없다
- TC-2-07 행에 에러 코드 표기가 없다
- 저장 직후 `MembershipHistory.membershipId`가 메모리에서 null이다
  - 원인: `insertable=false`
  - F4 이후 응답에 쓸 때 주의

## 에러와 해결

- 에러: 동시성 테스트가 실제로 락을 검증하는지 알 수 없었다
  - 확인: 락 조회를 일반 조회로 잠시 바꿔 돌림
  - 결과: TC-2-06 실패 확인
  - 해결: 원래 코드로 되돌림
- 에러: 극단 날짜·개월 입력이 500
  - 원인: `plusMonths`의 `DateTimeException` · MySQL DATE 범위 초과
  - 해결: D-23 상한 검사
- 에러: 음수·1000년 미만 시작일이 201로 저장되고 값이 바뀜
  - 원인: 시작일 하한 검사 없음
  - 해결: 시작일 ≥ 1000-01-01 검사
- 에러: `membership_history.membership_id`가 DB에서 NULL 허용
  - 원인: 엔티티에 NOT NULL 선언 누락
  - 해결: NOT NULL 선언 · 스키마 테스트 추가
- 시간 소모: 리뷰 → 결정 → 수정 순환이 F2 시간의 약 80%
  - 리뷰가 찾은 빈틈이 매번 새 결정이 됐다 (C-28 ~ C-34, 7건)
  - 3시간 과제에 비해 리뷰 기준이 깊다
  - 설계 문서가 커서(4개 합계 3,261줄) 에이전트 호출이 느리다

## 자주 틀리는 것 후보

- 날짜 범위 검사에서 상한만 막고 하한을 빠뜨린다
  - → MySQL DATE는 1000-01-01 ~ 9999-12-31. 양쪽을 짝으로 막는다
  - 음수 연도는 조용히 다른 날짜로 저장된다
  - 대상: 루트 CLAUDE.md "자주 틀리는 것"
- 경계값을 저장 전 값으로만 단언한다
  - → 저장 후 다시 읽어 단언한다
  - 1582년 이전 날짜는 JDBC 변환에서 바뀔 수 있다
  - 대상: 루트 CLAUDE.md "자주 틀리는 것"
- 락 → 검사 → 삽입 앞에 일반 SELECT를 둔다
  - → REPEATABLE READ 스냅샷이 첫 일반 조회 시점에 잡혀, 먼저 커밋된 행을 못 본다
  - 대상: 루트 CLAUDE.md "자주 틀리는 것"
- "무시한다"고 정한 필드에 Bean Validation을 그대로 둔다
  - → 무시될 값 때문에 400이 난다
  - 대상: 루트 CLAUDE.md "자주 틀리는 것"
- `insertable=false`로 FK 컬럼을 이중 매핑한 필드를 저장 직후 읽는다
  - → 메모리 값이 null이다
  - 대상: 루트 CLAUDE.md "자주 틀리는 것"
