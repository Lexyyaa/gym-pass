# API 명세

> 아래 두 문서를 HTTP 계약으로 옮긴다.
> - 요구사항([01-requirements.md](01-requirements.md))
> - 도메인([02-domain-model.md](02-domain-model.md))
>
> 도메인 에러 코드도 여기서 관리한다.

---

## 1. 공통 규칙

- Base URL: `http://localhost:8080`
- Content-Type: `application/json` (파일 업로드만 `multipart/form-data`)
- 시간: ISO-8601, `Asia/Seoul` (예: `2026-07-01T14:30:00`)
- 금액: 정수(원 단위)
- 식별 헤더: `X-{Actor}-Id`
  - 인증은 범위 밖
  - 헤더로 호출자를 식별만 한다
- 멱등 헤더: `Idempotency-Key` (UUID, 생성 요청에만)

**에러 응답**

```json
{ "errorCode": "ERROR_CODE", "message": "설명" }
```

**상태코드 기준**

| 상태 | 쓰는 경우 |
|---|---|
| 200 | 조회 · 처리 성공 · 멱등 재요청 |
| 201 | 신규 생성 |
| 400 | 입력 형식 오류 · 비즈니스 규칙 위반 |
| 404 | 리소스 없음 |
| 409 | 현재 상태에서 불가능한 요청 · 중복 |
| 500 | 서버 결함만 (입력 오류가 500으로 나가면 버그다) |

---

## 2. API 목록

| ID | 액터 | 유즈케이스 | Method | Path | FR |
|---|---|---|---|---|---|
| API-1 | | | POST | `/resources` | FR-2.1 |
| API-2 | | | GET | `/resources/{id}` | |

---

## 3. API 상세

<!-- API 하나당 아래 블록을 복사한다 -->

### API-1. {유즈케이스} · `POST /resources`

- 액터:
- 관련: FR- / TC-

**Request**

Header

| 이름 | 필수 | 설명 |
|---|---|---|
| `Idempotency-Key` | O | UUID |

Body

<!--
검증 열은 Bean Validation 어노테이션 기준으로 적는다.
리스트 필드는 @Valid 여부까지 적는다.
-->

| 필드 | 타입 | 필수 | 검증 | 설명 |
|---|---|---|---|---|
| `name` | String | O | `@NotBlank`, 최대 50자 | |
| `amount` | Long | O | `@PositiveOrZero` | |
| `items` | List | O | `@NotEmpty @Valid`, 요소 중복 불가 | |

```json
{
}
```

**Response** · `201 Created`

| 필드 | 타입 | 설명 |
|---|---|---|
| | | |

```json
{
}
```

**Errors**

| 상태 | errorCode | 조건 |
|---|---|---|
| 400 | `INVALID_INPUT` | 필드 검증 실패 |
| 404 | `XXX_NOT_FOUND` | |
| 409 | `INVALID_STATUS_TRANSITION` | |

---

## 4. 에러 코드

<!--
코드의 ErrorCode enum과 1:1로 맞춘다.
새 에러는 여기 먼저 추가하고 코드에 반영한다.
-->

### 공통

| errorCode | HTTP | 메시지 | 발생 조건 |
|---|---|---|---|
| `INVALID_INPUT` | 400 | 입력값이 올바르지 않습니다. | 입력 형식 오류 (아래) |
| `RESOURCE_NOT_FOUND` | 404 | 요청한 리소스를 찾을 수 없습니다. | 매핑되지 않은 경로 |
| `METHOD_NOT_ALLOWED` | 405 | 지원하지 않는 HTTP 메서드입니다. | 경로는 있으나 메서드가 다름 |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 오류가 발생했습니다. | 처리되지 않은 예외 |

`INVALID_INPUT`

- 메시지
  - 본문 검증 실패 시 `필드: 사유`
- 발생 조건
  - Bean Validation 실패
  - 헤더·파라미터 형식 오류
  - 본문 파싱 실패

### {도메인}

| errorCode | HTTP | 메시지 | 발생 조건 | 예외 클래스 | API |
|---|---|---|---|---|---|
| | | | | `XxxException` | API- |
