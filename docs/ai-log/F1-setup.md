# F1. 기초 설정 `feature/setup`

## 받은 지시

- `/run-feature F1`
- task_list F1의 작업 T1-1 ~ T1-4 구현
  - T1-1 공통 값 객체 · enum · 지점 헤더 파싱 · 공통 예외 · 페이지 응답 (03 §1 · §2 `common`)
  - T1-2 도메인 에러 코드 (04 §4)
  - T1-3 Branch 엔티티 · branch seed (03 §9)
  - T1-4 TC-1-01 ~ TC-1-04
- 시간
  - 시작 14:31
  - 구현 완료 14:36
  - 리뷰·검증 완료 약 14:40

## 작업 흐름

1. `implementer`가 T1-1 ~ T1-4 구현 · 커밋
   - T1-1 `feat: 공통 값 객체 및 enum 구현` (448c4c8)
   - T1-2 `feat: 도메인 에러 코드 추가` (d73c27a)
   - T1-3 `feat: Branch 엔티티 및 지점 seed 구현` (e89b9df)
   - T1-4 `test: 값 객체 및 지점 헤더 검증 케이스` (1d293b7)
2. `reviewer` + `verifier`(구현 점검)
   - 높음 0건
   - 수정 라운드 없음
   - 중간 이하는 스킬 규칙에 따라 수정하지 않고 아래 "검증"에 기록
3. 실측 생략
   - F1은 API가 없다
   - task_list에 `.http` 줄이 없다
4. 작업 로그 작성 (T1-5)

## 바뀐 파일

- `src/main/java/com/gym/pass/domain/member/Phone.java` (생성) — 전화번호 VO
- `src/main/java/com/gym/pass/domain/membership/MembershipType.java` (생성) — enum
- `src/main/java/com/gym/pass/domain/membership/MembershipStatus.java` (생성) — enum
- `src/main/java/com/gym/pass/domain/membership/MembershipEventType.java` (생성) — enum
- `src/main/java/com/gym/pass/domain/notification/NotificationType.java` (생성) — enum
- `src/main/java/com/gym/pass/domain/notification/NotificationStatus.java` (생성) — enum
- `src/main/java/com/gym/pass/domain/exception/CommonException.java` (생성) — 공통 비즈니스 예외
- `src/main/java/com/gym/pass/domain/exception/ErrorCode.java` (수정) — 공통 코드 rename · 04 §4 코드 추가
- `src/main/java/com/gym/pass/support/web/PageResponse.java` (생성) — 페이지 응답
- `src/main/java/com/gym/pass/support/web/BranchId.java` (생성) — 지점 헤더 주입 애너테이션
- `src/main/java/com/gym/pass/support/web/BranchIdArgumentResolver.java` (생성) — 지점 헤더 검증 · 주입
- `src/main/java/com/gym/pass/support/config/WebConfig.java` (생성) — ArgumentResolver 등록
- `src/main/java/com/gym/pass/domain/branch/Branch.java` (생성) — 지점 엔티티
- `src/main/java/com/gym/pass/domain/branch/BranchRepository.java` (생성) — 도메인 저장소 인터페이스
- `src/main/java/com/gym/pass/infrastructure/persistence/branch/BranchRepositoryImpl.java` (생성) — 저장소 구현
- `src/main/java/com/gym/pass/infrastructure/persistence/branch/BranchJpaRepository.java` (생성) — Spring Data JPA
- `src/main/resources/data.sql` (신규) — branch seed 2행
- `src/test/java/com/gym/pass/domain/member/PhoneTest.java` (생성) — TC-1-01 · TC-1-02
- `src/test/java/com/gym/pass/support/web/BranchIdArgumentResolverTest.java` (생성) — TC-1-03 · TC-1-04
- `support/exception/GlobalExceptionHandler.java` · `GlobalExceptionHandlerTest.java` (수정) — `COMMON_` 접두 rename 반영

## 설계대로 한 것

- `Phone` VO
  - `@Embeddable`
  - 04 API-1의 패턴으로 검증
  - 위반 시 `IllegalArgumentException`
- enum 5개 (03 §1)
- `ErrorCode`
  - 04 §4 코드 전부 추가 (21개)
- 지점 헤더 검증 결과
  - 누락 · 형식 오류 → 400 `COMMON_INVALID_INPUT`
  - 없는 지점 → 404 `BRANCH_NOT_FOUND`
- `Branch` 엔티티 · Repository · Impl · JpaRepository
- `data.sql` branch seed
  - 2행
  - `INSERT IGNORE`
  - 시각 컬럼에 `NOW(6)`
  - member · membership seed는 T2-1로 미룸 (03 §9 투입 시점)

## 설계에 없어서 정한 것

- 헤더 검증 방식
  - 선택지: ArgumentResolver / 필터·인터셉터 / 서비스별 검사
  - 고른 것: ArgumentResolver (`@BranchId` + `BranchIdArgumentResolver`)
  - 이유: 핸들러 파라미터로 검증된 지점 ID를 바로 받는다
- `support/web` 패키지 신설
  - 이유: presentation은 domain 참조 금지 (ArchUnit)
  - 지점 존재 확인에 `BranchRepository`가 필요해 presentation 밖에 둠
- 공통 에러 코드 이름
  - 기존 공통 코드 4개에 `COMMON_` 접두를 붙임
- seed 지점 이름
  - 강남점, 잠실점
- 헤더 허용 형식
  - 숫자 1~18자리 (`Long` 범위 초과 방지)

## 설계와 다르게 간 것

- `Branch`가 `BaseTimeEntity`를 상속
  - 결과: ERD에 없는 `updated_at` 컬럼이 생김
  - 이유: 기존 엔티티 기반 클래스를 그대로 따름
  - 사용자 확인 필요 (아래 "검증" 문서 쪽 항목)

## 검증

- `./gradlew spotlessApply build` → 통과
  - 전체 테스트 38개 통과
  - `PhoneTest` 9개 (TC-1-01 · TC-1-02)
  - `BranchIdArgumentResolverTest` (TC-1-03 · TC-1-04 + TC 없는 성공 1개)
- TC-1-03 · TC-1-04 방식
  - `@IntegrationTest` 안에서 테스트 전용 컨트롤러 + standalone MockMvc
  - 이유: 운영 API가 아직 없음
- `.http` 실측 → 대상 없음 (API 없음, task_list에 `.http` 줄 없음)

### 리뷰 · 검증 지적 — 코드 쪽 중간 (남김)

- 헤더 검증이 `@BranchId Long` 파라미터를 선언한 핸들러에서만 동작
  - 파라미터를 빠뜨리거나 원시 `long`으로 쓰면 검증이 조용히 빠짐
  - 04 §1 "모든 API 필수"를 강제하는 장치(인터셉터 · ArchUnit)가 없음
- TC-1-03 · TC-1-04가 standalone MockMvc
  - `WebConfig` 등록 자체는 검증하지 않음
  - 후속: F2 첫 API 테스트에서 주입 MockMvc로 헤더 400/404 보완

### 리뷰 · 검증 지적 — 문서 쪽 · 결정 필요 (사용자에게 물음)

- `BaseTimeEntity` 상속 vs ERD 시각 컬럼 불일치
  - 대상: branch
  - 이후 대상: membership_pause · history · notification_record · attendance_record
- 03 §3.2 · §3.3의 "MEMBER_/MEMBERSHIP_ 계열 400"에 해당하는 코드가 04 §4에 없음
  - 위험: VO의 `IllegalArgumentException`이 500으로 샐 수 있음

### 리뷰 · 검증 지적 — 낮음

- `CommonException` 위치
  - `domain/exception` vs `domain/common/exception`
- TC 없는 테스트
  - 성공 케이스 1개
  - `GlobalExceptionHandlerTest` 10개
- 패키지 표기 불일치
  - 03 §2는 `common` 책임으로 표기, 실제는 `support/web`
  - `src/main/CLAUDE.md` 패키지 트리에 `support/web` 없음
- 헤더 19자리 숫자가 400 (스펙상 404)
- 에러 코드 이름 문서 불일치
  - 04 §4에 오래된 "접두 없음" 안내문
  - 03 §1 접두 목록에 `COMMON_` 없음
  - `src/main/CLAUDE.md` 예외 절에 옛 이름 `INVALID_INPUT`
- 헤더 400 메시지 문구가 문서에 없음
- seed 지점 이름이 03 §9에 없음
- `branch.name` NOT NULL 미지정 (문서 · 코드 모두)
- `OpenApiConfig` 제목 · 설명 TODO가 작업 목록에 없음

## 에러와 해결

- 에러: 에러 코드 일괄 rename 후 일부 `@DisplayName` 문자열에 옛 이름이 남음
  - 원인: 한글이 바로 붙은 코드명은 정규식 `\b` 경계에 걸리지 않음
  - 해결: 남은 문자열을 따로 찾아 고침

## 자주 틀리는 것 후보

- 요청 헤더 검증을 ArgumentResolver에만 둔다
  - → 파라미터를 선언하지 않은 핸들러에서 검증이 빠진다
  - 대상: 루트 CLAUDE.md "자주 틀리는 것"
- `BaseTimeEntity`를 습관처럼 상속한다
  - → ERD에 없는 `updated_at` NOT NULL 컬럼이 생긴다
  - 대상: 루트 CLAUDE.md "자주 틀리는 것"
