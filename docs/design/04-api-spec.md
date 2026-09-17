# API 명세

> 아래 두 문서를 HTTP 계약으로 옮긴다.
> - 요구사항([02-requirements.md](02-requirements.md))
> - 도메인([03-domain-model.md](03-domain-model.md))
>
> 도메인 에러 코드도 여기서 관리한다.

---

## 1. 공통 규칙

- Base URL: `http://localhost:8080`
- Content-Type: `application/json`
- 날짜: `yyyy-MM-dd` (예: `2026-09-17`)
- 시각: ISO-8601, `Asia/Seoul` (예: `2026-09-17T14:30:00`)
- 금액: 정수(원 단위)

**지점 식별 헤더** (D-10 · NFR-1)

- 모든 API에 `X-Branch-Id` 헤더 필수 (숫자 지점 ID)
  - 인증은 범위 밖 — 헤더로 호출 지점을 식별만 한다
  - 각 API 상세에서는 이 헤더 표기를 생략한다
- 누락 · 숫자 아님 → 400 `COMMON_INVALID_INPUT`
- 존재하지 않는 지점 ID → 404 `BRANCH_NOT_FOUND` (D-10 보충 결정 C-20, TC-1-04)
- 타 지점 소유 데이터 접근 → 403 `BRANCH_FORBIDDEN`
  - 회원(Member)은 전사 공유라 지점 검사 없음
  - 회원권 · 출입 기록은 지점 귀속 → 소유 지점 검사

**페이지네이션** (D-12)

- offset 기반 쿼리 파라미터
  - `page`: 0부터, 기본 0
  - `size`: 1~100, 기본 20
- 응답 공통 형식

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 123,
  "totalPages": 7
}
```

**에러 응답** (`ErrorResponse` record와 1:1)

```json
{ "errorCode": "COMMON_INVALID_INPUT", "message": "입력값이 올바르지 않습니다." }
```

**모든 API 공통 에러** (각 상세의 Errors 표에서 생략)

| 상태 | errorCode | 조건 |
|---|---|---|
| 400 | `COMMON_INVALID_INPUT` | 본문 · 파라미터 검증 실패, `X-Branch-Id` 누락 · 형식 오류 |
| 404 | `BRANCH_NOT_FOUND` | 헤더의 지점 ID가 존재하지 않음 (TC-1-04) |

**상태코드 기준**

| 상태 | 쓰는 경우 |
|---|---|
| 200 | 조회 · 처리 성공 (출입 · 정지 · 해제 · 수정 · 취소) |
| 201 | 신규 생성 (회원 · 회원권 등록) |
| 204 | 삭제 성공 |
| 400 | 입력 형식 오류 · 날짜 규칙 위반 |
| 403 | 타 지점 데이터 접근 |
| 404 | 리소스 없음 |
| 409 | 현재 상태에서 불가능한 요청 · 중복 · 상한 초과 |
| 500 | 서버 결함만 (입력 오류가 500으로 나가면 버그다) |

---

## 2. API 목록

액터는 전부 지점 관리자다 (`X-Branch-Id`로 식별).

| ID | 유즈케이스 | Method | Path | FR |
|---|---|---|---|---|
| API-1 | 회원 등록 | POST | `/api/members` | FR-2.1 · FR-7.3 |
| API-2 | 회원권 등록 | POST | `/api/memberships` | FR-2.2 · FR-2.3 · FR-2.4 |
| API-3 | 출입 기록 | POST | `/api/attendances` | FR-3.1 ~ FR-3.4 · FR-4.4 |
| API-4 | 정지 등록 (예약) | POST | `/api/memberships/{membershipId}/pauses` | FR-4.1 ~ FR-4.3 |
| API-5 | 정지 조기 해제 | POST | `/api/memberships/{membershipId}/pauses/{pauseId}/release` | FR-4.1 · FR-4.2 · FR-4.4 |
| API-6 | 회원권 목록 조회 | GET | `/api/memberships` | FR-5.1 · FR-5.2 |
| API-7 | 회원 출입 이력 조회 | GET | `/api/members/{memberId}/attendances` | FR-5.3 |
| API-8 | 회원권 변경 이력 조회 | GET | `/api/members/{memberId}/membership-histories` | FR-5.4 · FR-5.5 |
| API-9 | 회원 수정 (선택) | PUT | `/api/members/{memberId}` | FR-7.1 · FR-7.3 |
| API-10 | 회원 삭제 (선택) | DELETE | `/api/members/{memberId}` | FR-7.2 |
| API-11 | 회원권 취소 (선택) | POST | `/api/memberships/{membershipId}/cancel` | FR-7.4 |

- FR-2.4(종류 확장 구조)는 API-2의 `type` 필드와 [03 §2](03-domain-model.md)의 종류별 분리 설계로 충족한다
- FR-5.5(변경 이력 기록)는 API-2 · 4 · 5 · 11과 출입(API-3) 차감의 부수 효과이고, 조회 표면은 API-8이다

### 배치 동작 (API 없음)

아래 두 배치는 HTTP API가 아니라 스케줄 배치다.

- 상태 동기화 배치 (F5, 매일 00:00 KST)
- 만료 안내 배치 (F6, 매일 09:00 KST)

| FR | 동작 |
|---|---|
| FR-5.6 | 상태 동기화: 매일 00:00 KST cron 실행 (D-21) |
| FR-5.6 | 상태 동기화: 종료일 < 오늘인 `ACTIVE` · `PAUSED` → `EXPIRED` |
| FR-5.6 | 상태 동기화: 어제로 끝난 정지만 있는(오늘을 포함하는 정지가 없는) `PAUSED` → `ACTIVE` |
| FR-5.6 | 상태 동기화: 오늘 시작하는 예약 정지가 있는 `ACTIVE` → `PAUSED` |
| FR-5.6 | 상태 동기화: 현재 상태를 조건에 넣은 bulk UPDATE — 재실행 멱등 |
| FR-6.1 | `EXPIRY_D7`: 오늘 < 종료일 ≤ 오늘+7 대상 발송 (기준일 = 종료일) |
| FR-6.1 | `EXPIRY_D0`: 종료일 = 오늘 대상 발송 (기준일 = 종료일) |
| FR-6.2 | `REMAINING_3`: 횟수제 잔여 == 3 대상 발송 (회원권당 1회) |
| FR-6.3 | 매일 09:00 KST cron 실행 |
| FR-6.4 | `FAILED`이고 시도 < 3인 건을 다음 배치에서 재시도, 3회 도달 시 중단 |
| FR-6.5 | `MessageClient` 가짜 구현체 (지연 100~500ms · 실패율 프로퍼티, 기본 10%) |
| FR-6.6 | 발송 이력 유일 키 (회원권, `NotificationType`, 기준일) — 재실행 멱등 |

- 상태 동기화 배치의 단계 · 인덱스 · 트랜잭션은 [03 §5 · §7](03-domain-model.md)
  - 배치 전이는 변경 이력(API-8)에 나타나지 않는다
- 발송 결과는 `NotificationStatus`(`SENT` · `FAILED`)와 시도 횟수로 기록한다
  - 3회 도달한 `FAILED`는 더 이상 재시도하지 않는다 (D-3)
- `MessageClient.send(...)`는 `SendResult(boolean success, String failureReason)`를 반환한다 ([03 §1](03-domain-model.md))
  - 실패는 예외가 아니라 `success=false` — 타임아웃 별도 처리 없음 (지연 상한 500ms)
- 이미 만료(종료일 < 오늘) · `CANCELED` 회원권은 대상에서 제외한다
- `PAUSED`여도 현재 종료일 기준으로 발송한다 (D-16)
- HTTP 에러 코드는 없다 — 실패는 상태로 기록될 뿐 예외로 응답하지 않는다

---

## 3. API 상세

### API-1. 회원 등록 · `POST /api/members`

- 관련: FR-2.1 · FR-7.3(선택) / TC-2-01 · TC-7-02

**Request** Body

| 필드 | 타입 | 필수 | 검증 | 설명 |
|---|---|---|---|---|
| `name` | String | O | `@NotBlank`, 최대 50자 | 회원 이름 |
| `phone` | String | O | `@NotBlank`, 패턴 `^01\d-\d{3,4}-\d{4}$` | 연락처 |

```json
{ "name": "김지영", "phone": "010-1234-5678" }
```

**Response** · `201 Created`

| 필드 | 타입 | 설명 |
|---|---|---|
| `memberId` | Long | 회원 ID |
| `name` | String | 이름 |
| `phone` | String | 연락처 |

```json
{ "memberId": 1, "name": "김지영", "phone": "010-1234-5678" }
```

**Errors**

| 상태 | errorCode | 조건 |
|---|---|---|
| 400 | `MEMBER_INVALID_INPUT` | 도메인 검증 실패 (D-22, TC-2-12) |
| 409 | `MEMBER_PHONE_DUPLICATED` | 연락처 중복 (FR-7.3 구현 시에만, 선택) |

### API-2. 회원권 등록 · `POST /api/memberships`

- 관련: FR-2.2 · FR-2.3 · FR-2.4 / TC-2-02 ~ TC-2-11
- 회원권은 헤더 지점 소속으로 생성된다
- 종료일 계산: 기간제 = 시작일 + 개월 (D-6), 횟수제 = 시작일 + 6개월 (D-7)
- 등록 거부 검사는 회원 기준 전 지점을 본다 (D-15 · D-19, C-22)
  - 거부 조건 = 상태 `ACTIVE` · `PAUSED` **그리고** 종료일 ≥ 오늘인 회원권 존재
  - 정지 중(`PAUSED`) 회원도 거부된다 (TC-2-09)
  - 시작일이 미래인 회원권이 있어도 거부된다 (TC-2-11)
  - 종료일 < 오늘인 stale `ACTIVE`만 있으면 등록된다 (TC-2-10)
  - 한계: 만료 전 미리 재등록할 수 없다
- 동시 등록 방어는 [03 §7](03-domain-model.md)

**Request** Body

| 필드 | 타입 | 필수 | 검증 | 설명 |
|---|---|---|---|---|
| `memberId` | Long | O | `@NotNull`, `@Positive` | 대상 회원 |
| `type` | String | O | `@NotNull`, `PERIOD` 또는 `COUNT` | 회원권 종류 |
| `startDate` | String | O | `@NotNull`, `yyyy-MM-dd` | 시작일 (값 제한 없음) |
| `months` | Integer | △ | `@Positive`, `type=PERIOD`일 때 필수 (서비스 검증) | 기간(개월) |
| `count` | Integer | △ | `@Positive`, `type=COUNT`일 때 필수 (서비스 검증) | 이용 횟수 |
| `paymentAmount` | Long | O | `@NotNull`, `@PositiveOrZero` | 결제 금액 (저장만) |

```json
{ "memberId": 1, "type": "PERIOD", "startDate": "2026-09-17", "months": 3, "paymentAmount": 300000 }
```

**Response** · `201 Created`

| 필드 | 타입 | 설명 |
|---|---|---|
| `membershipId` | Long | 회원권 ID |
| `memberId` | Long | 회원 ID |
| `branchId` | Long | 소속 지점 ID |
| `type` | String | `PERIOD` · `COUNT` |
| `status` | String | `ACTIVE` (등록 직후) |
| `startDate` | String | 시작일 |
| `endDate` | String | 종료일 (당일 포함) |
| `remainingCount` | Integer | 잔여 횟수 (기간제는 `null`) |
| `paymentAmount` | Long | 결제 금액 |

```json
{
  "membershipId": 10,
  "memberId": 1,
  "branchId": 1,
  "type": "PERIOD",
  "status": "ACTIVE",
  "startDate": "2026-09-17",
  "endDate": "2026-12-17",
  "remainingCount": null,
  "paymentAmount": 300000
}
```

**Errors**

| 상태 | errorCode | 조건 |
|---|---|---|
| 400 | `MEMBERSHIP_INVALID_INPUT` | `type`별 필수 값 누락 등 도메인 검증 실패 (D-22, TC-2-13) |
| 404 | `MEMBER_NOT_FOUND` | 회원 없음 (TC-2-08) |
| 409 | `MEMBERSHIP_ALREADY_ACTIVE` | 종료일 ≥ 오늘인 `ACTIVE` · `PAUSED` 회원권 보유 (TC-2-05 · TC-2-06 · TC-2-09 · TC-2-11) |

### API-3. 출입 기록 · `POST /api/attendances`

- 관련: FR-3.1 ~ FR-3.4 · FR-4.4 / TC-3-01 ~ TC-3-08 · TC-4-06 · TC-4-12
- 출입 시각은 서버 시각(KST)으로 기록한다
- 판정 대상 = 회원의 상태 `ACTIVE` · `PAUSED` 그리고 종료일 ≥ 오늘인 회원권
  - 등록 규칙(C-22)으로 항상 1건 이하다
- 판정 조건
  - 시작일 ≤ 오늘 ≤ 종료일
  - 오늘이 정지 구간 밖 — 조기 해제한 정지는 해제 당일까지 정지 구간이다 (C-23, TC-4-12)
  - 횟수제는 잔여 ≥ 1
  - 저장 상태가 아니라 날짜 · 정지 구간 · 잔여로 판정한다
- 같은 KST 달력일의 두 번째 이후 출입도 **동일한 성공 응답** — 기록은 남기고 차감만 생략 (D-11)
  - 응답만으로 차감 여부를 구분할 수 없다 (검증은 DB 잔여 횟수로)
- 거부된 출입 시도는 기록하지 않는다
- 유효 회원권이 타 지점 소속이면 403 (TC-3-08)
- 동시 요청 제어는 [03 §7](03-domain-model.md)

**Request** Body

| 필드 | 타입 | 필수 | 검증 | 설명 |
|---|---|---|---|---|
| `memberId` | Long | O | `@NotNull`, `@Positive` | 출입 회원 |

```json
{ "memberId": 1 }
```

**Response** · `200 OK`

| 필드 | 타입 | 설명 |
|---|---|---|
| `attendanceId` | Long | 출입 기록 ID |
| `membershipId` | Long | 판정에 사용된 회원권 ID |
| `attendedAt` | String | 출입 시각 (서버 KST) |

```json
{ "attendanceId": 100, "membershipId": 10, "attendedAt": "2026-09-17T07:31:20" }
```

**Errors**

| 상태 | errorCode | 조건 |
|---|---|---|
| 404 | `MEMBER_NOT_FOUND` | 회원 없음 |
| 409 | `ATTENDANCE_NO_VALID_MEMBERSHIP` | 유효 회원권 없음 — 미보유 · 기간 밖 · 만료 · 취소 · 잔여 0 (TC-3-04 ~ 06) |
| 409 | `ATTENDANCE_MEMBERSHIP_PAUSED` | 정지 기간 중 출입 · 조기 해제 당일 출입 (TC-4-06 · TC-4-12) |
| 403 | `BRANCH_FORBIDDEN` | 유효 회원권이 타 지점 소속 (TC-3-08) |

### API-4. 정지 등록 (예약) · `POST /api/memberships/{membershipId}/pauses`

- 관련: FR-4.1 ~ FR-4.3 / TC-4-01 ~ TC-4-05 · TC-4-08 · TC-4-09 · TC-4-11 · TC-4-13 · TC-4-15
- 정지 기간: 시작일 ~ 시작일 + 일수 − 1 (양끝 포함), 시작일 당일부터 출입 거부
- 등록 시 예정 일수만큼 종료일을 즉시 연장한다 (D-8)
- 상한 (D-2): 회원권당 최대 3회, 누적 개월당 7일, 1회 최소 1일
- 상한 계산 기준 (C-24)
  - 횟수: 시작 전 해제한 정지는 세지 않는다 (TC-4-13)
  - 누적 일수: 해제된 건은 사용 일수, 해제되지 않은 건은 예정 일수
- 시작일은 오늘 이후(오늘 포함)만 허용, 기존 정지와 기간 겹침 거부
  - 겹침은 유효 정지 구간(해제된 건은 시작일 ~ 해제일)으로 판정한다
- `ACTIVE` · `PAUSED`(추가 예약, 비겹침 + 상한 통과 시) 회원권만 정지 가능 — 만료 · 취소 불가 (D-19)
  - 종료일 < 오늘인 회원권은 저장 상태와 무관하게 만료로 본다 (D-21)
- 상태 변화 ([03 §4](03-domain-model.md))
  - 시작일 = 오늘이면 즉시 `PAUSED`
  - 시작일 > 오늘이면 상태 유지, 시작일 00:00 상태 동기화 배치가 `PAUSED`로 바꾼다
- 변경 이력(정지 · 연장)을 남긴다 (FR-5.5)

**Request** Body

| 필드 | 타입 | 필수 | 검증 | 설명 |
|---|---|---|---|---|
| `startDate` | String | O | `@NotNull`, `yyyy-MM-dd`, 오늘 이후 (서비스 검증) | 정지 시작일 (예약 가능) |
| `days` | Integer | O | `@NotNull`, `@Positive` | 정지 일수 |

```json
{ "startDate": "2026-09-20", "days": 7 }
```

**Response** · `200 OK`

| 필드 | 타입 | 설명 |
|---|---|---|
| `pauseId` | Long | 정지 ID |
| `membershipId` | Long | 회원권 ID |
| `startDate` | String | 정지 시작일 |
| `endDate` | String | 정지 종료일 (시작일 + 일수 − 1) |
| `days` | Integer | 정지 일수 |
| `membershipEndDate` | String | 연장된 회원권 종료일 |

```json
{
  "pauseId": 5,
  "membershipId": 10,
  "startDate": "2026-09-20",
  "endDate": "2026-09-26",
  "days": 7,
  "membershipEndDate": "2026-12-24"
}
```

**Errors**

| 상태 | errorCode | 조건 |
|---|---|---|
| 404 | `MEMBERSHIP_NOT_FOUND` | 회원권 없음 (TC-4-11) |
| 403 | `BRANCH_FORBIDDEN` | 타 지점 회원권 (TC-4-15) |
| 400 | `PAUSE_START_DATE_PAST` | 시작일이 오늘 이전 (소급 불가, TC-4-09) |
| 409 | `MEMBERSHIP_NOT_PAUSABLE` | 만료 · 취소된 회원권 (TC-4-05) |
| 409 | `PAUSE_COUNT_LIMIT_EXCEEDED` | 시작 전 해제 건을 뺀 4번째 정지 (TC-4-04) |
| 409 | `PAUSE_DAYS_LIMIT_EXCEEDED` | 누적 일수가 개월당 7일 초과 (TC-4-03) |
| 409 | `PAUSE_OVERLAPPED` | 기존 정지와 기간 겹침 (TC-4-08) |

### API-5. 정지 조기 해제 · `POST /api/memberships/{membershipId}/pauses/{pauseId}/release`

- 관련: FR-4.1 · FR-4.2 · FR-4.4 / TC-4-07 · TC-4-10 · TC-4-12 · TC-4-14 · TC-4-16
- 사용 일수 = 정지 시작일 ~ 해제 당일 (양끝 포함), 시작 전 해제는 0일
- 미사용 일수만큼 회원권 종료일을 되돌린다 → 순연장 = 실제 정지 일수 (D-8)
- 해제 다음 날부터 출입 가능 — 해제 당일까지 정지로 계산한다 (C-23, TC-4-12)
- 해제 후 상태 ([03 §4](03-domain-model.md))
  - 해제 대상을 제외하고 오늘을 포함하는 정지가 남으면 `PAUSED` 유지
  - 없으면 `ACTIVE` — 이때도 해제 당일 출입은 거부된다
- 시작 전 해제한 정지는 정지 횟수에서 빠진다 (C-24)
- 변경 이력(해제 · 연장 조정)을 남긴다 (FR-5.5)
- 본문 없음

**Response** · `200 OK`

| 필드 | 타입 | 설명 |
|---|---|---|
| `pauseId` | Long | 정지 ID |
| `usedDays` | Integer | 실제 사용한 정지 일수 |
| `membershipEndDate` | String | 되돌린 후 회원권 종료일 |

```json
{ "pauseId": 5, "usedDays": 3, "membershipEndDate": "2026-12-20" }
```

**Errors**

| 상태 | errorCode | 조건 |
|---|---|---|
| 404 | `MEMBERSHIP_NOT_FOUND` | 회원권 없음 |
| 404 | `PAUSE_NOT_FOUND` | 정지 없음 · 해당 회원권의 정지가 아님 (TC-4-14) |
| 403 | `BRANCH_FORBIDDEN` | 타 지점 회원권 (TC-4-16) |
| 409 | `PAUSE_NOT_RELEASABLE` | 이미 해제됐거나 정지 종료일이 지남 (TC-4-10) |

### API-6. 회원권 목록 조회 · `GET /api/memberships`

- 관련: FR-5.1 · FR-5.2 / TC-5-01 · TC-5-02 · TC-5-05 · TC-5-08
- 헤더 지점 소속 회원권만 반환한다 (NFR-1)
- 정렬: 만료 임박 순 = 종료일 오름차순, 동률이면 ID 오름차순 (고정, `sort` 파라미터 없음)
- 상태 필터는 저장 상태에 날짜 보정을 더해 판정한다 (D-21)
  - `ACTIVE`: 상태 `ACTIVE` 그리고 종료일 ≥ 오늘
  - `PAUSED`: 상태 `PAUSED` 그리고 종료일 ≥ 오늘
  - `EXPIRED`: 상태 `EXPIRED`, 또는 상태 `ACTIVE` · `PAUSED` 그리고 종료일 < 오늘
  - 생략: 전체
- 응답의 `status`도 같은 규칙으로 보정한 값이다 (종료일 < 오늘인 `ACTIVE` · `PAUSED` → `EXPIRED`, TC-5-08)
- 정지 시작 · 종료에 따른 `ACTIVE` ↔ `PAUSED`는 조회에서 보정하지 않는다 — 00:00 상태 동기화 배치가 맞춘다
- 날짜 보정은 00:00 배치가 실패한 날의 안전망이다 — 평소에는 배치가 저장 상태를 맞춰 둔다
- 인덱스 설계와 보정 조건의 실행 계획은 [03 §6](03-domain-model.md) (NFR-5)

**Request** Query

| 파라미터 | 타입 | 필수 | 검증 | 설명 |
|---|---|---|---|---|
| `status` | String | X | `ACTIVE` · `PAUSED` · `EXPIRED` | 상태 필터 (날짜 보정 포함), 생략 시 전체 (`CANCELED` 포함) |
| `page` | Integer | X | 0 이상, 기본 0 | 페이지 번호 |
| `size` | Integer | X | 1~100, 기본 20 | 페이지 크기 |

**Response** · `200 OK` — 페이지 공통 형식, `content` 요소:

| 필드 | 타입 | 설명 |
|---|---|---|
| `membershipId` | Long | 회원권 ID |
| `memberId` | Long | 회원 ID |
| `memberName` | String | 회원 이름 |
| `type` | String | `PERIOD` · `COUNT` |
| `status` | String | `ACTIVE` · `PAUSED` · `EXPIRED` · `CANCELED` |
| `startDate` | String | 시작일 |
| `endDate` | String | 종료일 |
| `remainingCount` | Integer | 잔여 횟수 (기간제 `null`) |

```json
{
  "content": [
    {
      "membershipId": 10,
      "memberId": 1,
      "memberName": "김지영",
      "type": "COUNT",
      "status": "ACTIVE",
      "startDate": "2026-09-01",
      "endDate": "2027-03-01",
      "remainingCount": 7
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

**Errors**: 공통 에러만 (`status` 허용 외 값 · `page`/`size` 범위 밖은 400 `COMMON_INVALID_INPUT`)

### API-7. 회원 출입 이력 조회 · `GET /api/members/{memberId}/attendances`

- 관련: FR-5.3 / TC-5-03 · TC-5-04
- 헤더 지점에 귀속된 출입 기록만 반환한다 (D-10)
  - 타 지점 기록은 필터되어 0건으로 나타난다 (TC-5-04의 "0건" 방식)
- 정렬: 출입 시각 최신순 (고정)

**Request** Query: `page` · `size` (공통 규칙과 같음)

**Response** · `200 OK` — 페이지 공통 형식, `content` 요소:

| 필드 | 타입 | 설명 |
|---|---|---|
| `attendanceId` | Long | 출입 기록 ID |
| `membershipId` | Long | 회원권 ID |
| `attendedAt` | String | 출입 시각 |

```json
{
  "content": [
    { "attendanceId": 100, "membershipId": 10, "attendedAt": "2026-09-17T07:31:20" }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

**Errors**

| 상태 | errorCode | 조건 |
|---|---|---|
| 404 | `MEMBER_NOT_FOUND` | 회원 없음 |

### API-8. 회원권 변경 이력 조회 · `GET /api/members/{memberId}/membership-histories`

- 관련: FR-5.4 · FR-5.5 / TC-5-03 · TC-5-04
- 헤더 지점 소속 회원권의 이력만 반환한다 (타 지점 이력은 0건)
- 정렬: 발생 시각 최신순 (고정)
- `eventType` 값(등록 · 정지 · 해제 · 차감 · 취소)의 정의는 [03 §1](03-domain-model.md)의 `MembershipEventType`을 따른다
  - `REGISTERED` · `PAUSED` · `RESUMED` · `DEDUCTED` · `CANCELED`
  - 정지에 따른 연장은 별도 이벤트 없이 `PAUSED` · `RESUMED`의 종료일 before/after로 기록된다
- 서버가 조립한 설명 문자열 없이 이력 컬럼(before/after)을 그대로 반환한다 ([03 §3.3](03-domain-model.md) `MembershipHistory`와 1:1)

**Request** Query: `page` · `size` (공통 규칙과 같음)

**Response** · `200 OK` — 페이지 공통 형식, `content` 요소:

| 필드 | 타입 | 설명 |
|---|---|---|
| `historyId` | Long | 이력 ID |
| `membershipId` | Long | 회원권 ID |
| `eventType` | String | 변경 종류 |
| `endDateBefore` | String | 변경 전 종료일 (변경 없으면 `null`) |
| `endDateAfter` | String | 변경 후 종료일 (변경 없으면 `null`) |
| `remainingCountBefore` | Integer | 변경 전 잔여 횟수 (해당 없으면 `null`) |
| `remainingCountAfter` | Integer | 변경 후 잔여 횟수 (해당 없으면 `null`) |
| `createdAt` | String | 발생 시각 |

```json
{
  "content": [
    {
      "historyId": 30,
      "membershipId": 10,
      "eventType": "PAUSED",
      "endDateBefore": "2026-12-17",
      "endDateAfter": "2026-12-24",
      "remainingCountBefore": null,
      "remainingCountAfter": null,
      "createdAt": "2026-09-17T10:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

**Errors**

| 상태 | errorCode | 조건 |
|---|---|---|
| 404 | `MEMBER_NOT_FOUND` | 회원 없음 |

### API-9. 회원 수정 (선택) · `PUT /api/members/{memberId}`

- 관련: FR-7.1 · FR-7.3 / TC-7-01 · TC-7-02
- D-18: 우선순위 최하위 — 미구현 시 README에 방향만 기재

**Request** Body

| 필드 | 타입 | 필수 | 검증 | 설명 |
|---|---|---|---|---|
| `name` | String | O | `@NotBlank`, 최대 50자 | 이름 |
| `phone` | String | O | `@NotBlank`, 패턴 `^01\d-\d{3,4}-\d{4}$` | 연락처 |

```json
{ "name": "김지영", "phone": "010-9999-8888" }
```

**Response** · `200 OK` — API-1 응답과 같은 형식

**Errors**

| 상태 | errorCode | 조건 |
|---|---|---|
| 404 | `MEMBER_NOT_FOUND` | 회원 없음 |
| 409 | `MEMBER_PHONE_DUPLICATED` | 다른 회원과 연락처 중복 (TC-7-02) |

### API-10. 회원 삭제 (선택) · `DELETE /api/members/{memberId}`

- 관련: FR-7.2 / TC-7-05 · TC-7-06
- 물리 삭제, 연관 회원권 · 출입 기록이 있으면 거부 (02 §2 F7 가정)
- 본문 없음

**Response** · `204 No Content` (본문 없음)

**Errors**

| 상태 | errorCode | 조건 |
|---|---|---|
| 404 | `MEMBER_NOT_FOUND` | 회원 없음 |
| 409 | `MEMBER_HAS_RELATED_DATA` | 연관 회원권 · 출입 기록 존재 (TC-7-06) |

### API-11. 회원권 취소 (선택) · `POST /api/memberships/{membershipId}/cancel`

- 관련: FR-7.4 / TC-7-03 · TC-7-04
- `ACTIVE` · `PAUSED`에서만 취소 가능, 환불 계산 없음 (상태 전이만, D-18)
  - 종료일 < 오늘인 회원권은 저장 상태와 무관하게 만료로 보고 거부한다 (D-21)
- 변경 이력(취소)을 남긴다 (FR-5.5)
- 본문 없음

**Response** · `200 OK`

| 필드 | 타입 | 설명 |
|---|---|---|
| `membershipId` | Long | 회원권 ID |
| `status` | String | `CANCELED` |

```json
{ "membershipId": 10, "status": "CANCELED" }
```

**Errors**

| 상태 | errorCode | 조건 |
|---|---|---|
| 404 | `MEMBERSHIP_NOT_FOUND` | 회원권 없음 |
| 403 | `BRANCH_FORBIDDEN` | 타 지점 회원권 |
| 409 | `MEMBERSHIP_NOT_CANCELABLE` | 만료 · 이미 취소된 회원권 (TC-7-04) |

---

## 4. 에러 코드

코드의 `ErrorCode` enum과 1:1로 맞춘다. 새 에러는 여기 먼저 추가하고 코드에 반영한다.

### 공통

| errorCode | HTTP | 메시지 | 발생 조건 |
|---|---|---|---|
| `COMMON_INVALID_INPUT` | 400 | 입력값이 올바르지 않습니다. | 입력 형식 오류 (아래) |
| `COMMON_RESOURCE_NOT_FOUND` | 404 | 요청한 리소스를 찾을 수 없습니다. | 매핑되지 않은 경로 |
| `COMMON_METHOD_NOT_ALLOWED` | 405 | 지원하지 않는 HTTP 메서드입니다. | 경로는 있으나 메서드가 다름 |
| `COMMON_INTERNAL_SERVER_ERROR` | 500 | 서버 오류가 발생했습니다. | 처리되지 않은 예외 |

`COMMON_INVALID_INPUT`

- 메시지
  - 본문 검증 실패 시 `필드: 사유`
  - 헤더 오류 시 `X-Branch-Id: 숫자 지점 ID가 필요합니다.`
- 발생 조건
  - Request Bean Validation 실패 (도메인 검증 실패는 전용 코드, D-22)
  - `X-Branch-Id` 헤더 누락 · 1~18자리 숫자가 아님 (TC-1-03)
  - 쿼리 파라미터 형식 · 범위 오류
  - 본문 파싱 실패

### 지점

| errorCode | HTTP | 메시지 | 발생 조건 | 예외 클래스 | API |
|---|---|---|---|---|---|
| `BRANCH_NOT_FOUND` | 404 | 지점을 찾을 수 없습니다. | 헤더의 지점 ID가 DB에 없음 | `BranchException` | 전체 |
| `BRANCH_FORBIDDEN` | 403 | 다른 지점의 데이터에 접근할 수 없습니다. | 타 지점 소유 회원권 · 기록 접근 | `BranchException` | API-3 ~ 5 · 11 |

### 회원

| errorCode | HTTP | 메시지 | 발생 조건 | 예외 클래스 | API |
|---|---|---|---|---|---|
| `MEMBER_INVALID_INPUT` | 400 | 회원 정보가 올바르지 않습니다. | 도메인 생성 · 수정 검증 실패 — 공백 이름 · 형식 오류 연락처 (D-22, TC-2-12) | `MemberException` | API-1 · 9 |
| `MEMBER_NOT_FOUND` | 404 | 회원을 찾을 수 없습니다. | 회원 ID가 DB에 없음 | `MemberException` | API-2 · 3 · 7 ~ 10 |
| `MEMBER_PHONE_DUPLICATED` | 409 | 이미 등록된 연락처입니다. | 등록 · 수정 시 다른 회원과 연락처 중복 (선택) | `MemberException` | API-1 · 9 |
| `MEMBER_HAS_RELATED_DATA` | 409 | 연관 데이터가 있어 삭제할 수 없습니다. | 회원권 · 출입 기록 보유 회원 삭제 (선택) | `MemberException` | API-10 |

### 회원권

| errorCode | HTTP | 메시지 | 발생 조건 | 예외 클래스 | API |
|---|---|---|---|---|---|
| `MEMBERSHIP_INVALID_INPUT` | 400 | 회원권 정보가 올바르지 않습니다. | 기간제인데 개월 없음 · 횟수제인데 횟수 없음 등 도메인 등록 검증 실패 (D-22, TC-2-13) | `MembershipException` | API-2 |
| `MEMBERSHIP_NOT_FOUND` | 404 | 회원권을 찾을 수 없습니다. | 회원권 ID가 DB에 없음 | `MembershipException` | API-4 · 5 · 11 |
| `MEMBERSHIP_ALREADY_ACTIVE` | 409 | 이미 유효한 회원권이 있습니다. | 종료일 ≥ 오늘인 `ACTIVE` · `PAUSED` 회원권 보유 회원의 신규 등록 (D-15 · D-19) | `MembershipException` | API-2 |
| `MEMBERSHIP_NOT_PAUSABLE` | 409 | 정지할 수 없는 회원권입니다. | 만료(종료일 < 오늘 포함) · 취소된 회원권 정지 요청 | `MembershipException` | API-4 |
| `MEMBERSHIP_NOT_CANCELABLE` | 409 | 취소할 수 없는 회원권입니다. | 만료(종료일 < 오늘 포함) · 이미 취소된 회원권 취소 요청 (선택) | `MembershipException` | API-11 |

### 정지

| errorCode | HTTP | 메시지 | 발생 조건 | 예외 클래스 | API |
|---|---|---|---|---|---|
| `PAUSE_NOT_FOUND` | 404 | 정지 내역을 찾을 수 없습니다. | 정지 ID 없음 · 해당 회원권의 정지가 아님 | `MembershipException` | API-5 |
| `PAUSE_START_DATE_PAST` | 400 | 정지 시작일은 오늘 이후여야 합니다. | 시작일 < 오늘 (소급 정지) | `MembershipException` | API-4 |
| `PAUSE_COUNT_LIMIT_EXCEEDED` | 409 | 정지 가능 횟수를 초과했습니다. | 시작 전 해제 건을 뺀 4번째 정지 등록 (D-2) | `MembershipException` | API-4 |
| `PAUSE_DAYS_LIMIT_EXCEEDED` | 409 | 정지 가능 일수를 초과했습니다. | 누적 정지 일수 > 개월 수 × 7일 (D-2, 해제 건은 사용 일수) | `MembershipException` | API-4 |
| `PAUSE_OVERLAPPED` | 409 | 기존 정지와 기간이 겹칩니다. | 겹치는 기간의 정지 재등록 | `MembershipException` | API-4 |
| `PAUSE_NOT_RELEASABLE` | 409 | 해제할 수 없는 정지입니다. | 이미 해제됐거나 종료일이 지난 정지 해제 | `MembershipException` | API-5 |

### 출입

| errorCode | HTTP | 메시지 | 발생 조건 | 예외 클래스 | API |
|---|---|---|---|---|---|
| `ATTENDANCE_NO_VALID_MEMBERSHIP` | 409 | 유효한 회원권이 없습니다. | 회원권 미보유 · 기간 밖 · 만료 · 취소 · 잔여 0 | `AttendanceException` | API-3 |
| `ATTENDANCE_MEMBERSHIP_PAUSED` | 409 | 정지 중인 회원권입니다. | 정지 기간(조기 해제 당일 포함) 중 출입 요청 (FR-4.4) | `AttendanceException` | API-3 |

### 안내 (배치)

- HTTP 에러 코드 없음 — 발송 실패는 `NotificationStatus.FAILED`와 시도 횟수로 기록한다 (§2 배치 동작)
- 상태 동기화 배치도 HTTP 에러 코드가 없다 — 조건에 맞는 행이 없으면 변경 0건으로 끝난다
