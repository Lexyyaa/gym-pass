---
name: verify-http
description: 서버를 띄워 .http 실행 케이스를 재연하고 기대값과 대조한다.
argument-hint: "[F번호 또는 .http 파일명 — 비우면 전체]"
disable-model-invocation: true
---

# /verify-http $ARGUMENTS — 실측

대상: `$ARGUMENTS` (비어 있으면 `http/*.http` 전체)

- 서버를 띄워 .http 실행 케이스를 curl로 재연한다
- 대조 항목: 상태코드·응답 본문·DB 상태
- 코드는 고치지 않는다

`/run-feature` 3단계도 이 절차를 그대로 따른다.
이 스킬은 **확인만** 한다.

- 코드 · 문서를 고치지 않는다
- 커밋하지 않는다

## .http 작성 규칙

기대값을 요청 바로 위 주석에 적는다. 이 주석이 대조 기준이다.

```http
@host = http://localhost:8080

### [TC-2-01] 정상 주문 생성
# @expect 201
# @expect $.status == "PLACED"
# @db SELECT COUNT(*) FROM orders WHERE idempotency_key = '1111...' => 1
POST {{host}}/orders
Content-Type: application/json
Idempotency-Key: 11111111-1111-1111-1111-111111111111

{ "productId": 1, "quantity": 2 }

### [TC-2-05] 종결된 주문 취소 시 409
# @uses orderId = [TC-2-01].$.id
# @expect 409
# @expect $.errorCode == "INVALID_STATUS_TRANSITION"
POST {{host}}/orders/{{orderId}}/cancel
```

- `# @expect {상태코드}` — 필수
- `# @expect $.errorCode == "..."` — 에러 케이스는 필수
  - 같은 400끼리 거짓 통과가 나지 않게
- `# @expect $.필드 == 값` — 부분 성공 · 생성 API는 필수
  - 상태코드만 보면 거짓 통과가 난다
- `# @db {SQL} => {기대값}` — 저장 · 변경이 있는 요청은 필수
- 앞 요청의 응답 값(id 등)을 쓰는 요청은 의존을 적는다
  - 선언: `# @uses {변수} = [TC-x-yy].$.경로`
  - 사용: 경로 · 헤더 · 본문에 `{{변수}}`
  - 같은 파일의 앞 요청, 또는 앞 파일(F 순서)의 요청만 참조한다

## 순서

1. **대상 결정**
   - 인자가 `F번호`면 task_list에서 브랜치 이름을 찾아 `http/{name}.http`
   - 파일명이면 그 파일
   - 없으면 전체 — task_list의 F 순서대로 파일을 돈다
2. **DB 초기화 · 서버 기동**
   - `docker compose down -v && docker compose up -d`
     - 매번 빈 DB에서 시작한다
     - 이전 실행의 데이터(멱등 키 · UNIQUE 값)가 남으면 거짓 불일치가 난다
     - `ddl-auto: update`가 반영 못 하는 제약 변경도 이렇게 반영된다
   - 아래 명령이 성공할 때까지 대기한다
     - `docker exec gym-mysql mysql -ugym -pgym gym -e "SELECT 1"`
     - `mysqladmin ping`은 초기화 중인 임시 서버에도 응답하므로 쓰지 않는다
   - 8080이 비어 있는지 확인한다: `lsof -i :8080`
     - 남아 있으면 `lsof -ti :8080 | xargs kill` 후 다시 확인
   - `./gradlew bootRun > build/bootrun.log 2>&1 &`
     - → `curl -s localhost:8080/actuator/health`가 `UP`일 때까지 대기
     - 최대 90초
   - 기동 실패 → `build/bootrun.log` 마지막 부분과 함께 🛑 멈춤
3. **요청 재연**: 파일 순서대로 각 요청을 `curl -s -w '\n%{http_code}'`로 보낸다
   - `@uses`가 있으면 앞 응답에서 값을 꺼내 `{{변수}}`를 치환한다
   - 멀티파트는 `curl -F 'files=@http/sample/파일'`로 실제 파일을 붙인다
4. **대조**
   - 상태코드
   - `$.필드` (jq 또는 응답 파싱)
   - `@db` 쿼리
     - `docker exec -e MYSQL_PWD=gym gym-mysql mysql -ugym gym -N -e "..."`
     - `-p암호`를 쓰면 경고가 출력에 섞이므로 환경 변수로 넘긴다
   - `build/bootrun.log`에 `ERROR`가 새로 찍혔는지 확인한다
     - 4xx 기대 요청에서 ERROR가 나오면 불일치로 본다
5. **서버 종료**
   - `lsof -ti :8080 | xargs kill`
   - `lsof -i :8080`이 비었는지 확인한다
   - MySQL 컨테이너는 켜 둔다

## 보고

```
## 실측 결과 — 대상 … (n건)

| TC | 요청 | 상태 | 본문 | DB | 결과 |
|---|---|---|---|---|---|
| TC-2-01 | POST /orders | 201 ✅ | status ✅ | 1 ✅ | ✅ |
| TC-2-05 | POST /orders | 500 ❌ (기대 400) | — | — | ❌ |

## 불일치
- [TC-2-05] 기대 400 INVALID_INPUT / 실제 500 — 서버 로그: (핵심 줄)
  - 추정 원인: …

## 기대값이 없는 요청
- (@expect가 빠진 요청 — 작성 규칙 위반)
```

불일치가 있으면 여기서 끝낸다.
고치는 것은 호출한 쪽(`/run-feature` 또는 사용자)이 정한다.
