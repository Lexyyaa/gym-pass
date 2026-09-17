# src/main 코드 규칙

`src/main` 아래 파일을 읽을 때 루트 CLAUDE.md와 함께 로드된다.
도구(ArchUnit·Spotless)가 막는 규칙은 적지 않는다.
사람이 리뷰로 확인하는 규칙만 둔다.

## 패키지

```text
com.gym.pass
├── presentation/{domain}                Controller, {Resource}Request, {Resource}Response
│                                        (MQ 리스너·스케줄러 같은 진입점도 여기)
├── application/{domain}                 {Resource}ApplicationService, {Resource}Command, {Resource}Info
├── domain
│   ├── {domain}                         애그리거트·엔티티, VO, Enum, 도메인 서비스, {Aggregate}Repository(인터페이스)
│   │   └── exception                    {Domain}Exception
│   ├── common                           여러 도메인이 쓰는 VO·Enum
│   └── exception                        ErrorCode, BusinessException
├── infrastructure
│   ├── persistence/{domain}             {Aggregate}RepositoryImpl, {Aggregate}JpaRepository
│   └── {external}                       외부 연동 구현
└── support
    ├── config                           설정 빈
    ├── properties                       @ConfigurationProperties
    └── exception                        ErrorResponse, GlobalExceptionHandler
```

## 도메인

- 애그리거트·엔티티에 `@Entity`를 직접 붙인다
  - 별도 영속 모델·매퍼는 두지 않는다
- 하위 엔티티 이름에는 루트 이름을 접두로 붙인다
  - 예: `Order` → `OrderItem`, `OrderStatusHistory`
- 생성은 정적 팩토리로 한다
  - 불변식 검증을 그 안에서 끝낸다
- `@Setter` 금지
  - 상태 변경은 의도가 드러나는 메서드로 한다 (`approve()`, `cancel()`)
- 상태 전이 검증은 애그리거트 메서드 안에서 한다
  - 서비스에 `if (status == ...)`를 두지 않는다
- 여러 애그리거트를 넘나드는 로직은 도메인 서비스로 뺀다
- 규칙이 실제로 여러 갈래일 때만 다형성(정책 인터페이스)을 쓴다
  - 규칙이 하나면 클래스 하나로 둔다
- 도메인 메서드의 입력 파라미터 객체는 DTO가 아니라 도메인 입력 VO다
  - 해당 애그리거트 옆에 둔다
- VO 생성자의 검증은 최후 방어선이다
  - 사용자 입력 오류는 그 전에 `BusinessException`으로 막는다
- 순서에 의미가 있는 컬렉션에는 `@OrderBy`를 붙인다
- 생성·수정 시각은 `domain/common/BaseTimeEntity`를 상속한다
  - 비즈니스 판단에 쓰는 시각(접수 시각 등)은 따로 필드를 둔다
- 제약·인덱스·NOT NULL은 엔티티에 선언한다
  - 기준: `docs/design/03-domain-model.md` §6과 같게

## Repository 3단

- 도메인 인터페이스: `domain/{domain}/{Aggregate}Repository`
  - 조회 메서드는 `getByXxx()`
    - 없으면 예외
  - 없는 게 정상인 경우만 `findByXxx()`
    - `Optional`을 돌려준다
  - 락 조회는 `getByXxxForUpdate()`
- 구현: `infrastructure/persistence/{domain}/{Aggregate}RepositoryImpl`
  - `@Repository`
  - `Optional`을 풀고 not-found 예외를 던지는 곳은 여기다
- Spring Data: `{Aggregate}JpaRepository`
  - Impl만 사용한다

## DTO

- record + 이너 record
- 리소스 하나에 홀더 4개
  - presentation: `{Resource}Request`, `{Resource}Response`
  - application: `{Resource}Command`, `{Resource}Info`
  - 요청 본문이 없는 리소스(multipart 등)는 Request 홀더를 두지 않는다
- 홀더 안의 이너 record 이름은 실제 행위를 따른다
  - 예: `OrderRequest.Place`, `OrderRequest.Cancel`
  - `Create`/`Read` 같은 범용 이름은 피한다
- 흐름: Request → Command → (도메인 호출) → Info → Response
- `Info` · `Command` 필드에 domain 타입(Enum · VO · 엔티티)을 담지 않는다
  - Enum은 `name()`, VO는 원시값으로 풀어 담는다
  - presentation은 domain을 참조할 수 없다
  - 담으면 `Response.from(info)`가 ArchUnit에 걸린다
- 변환은 정적 팩토리
  - `from` — 원본 하나를 그대로 변환
    - `Command.from(request)`
    - `Info.from(aggregate)`
    - `Response.from(info)`
  - `of` — 여러 조각을 조립
    - `Info.of(order, created, warnings)`
- 요청 검증은 Request의 Bean Validation으로 한다
  - 리스트 필드는 `@Valid`까지 붙인다
- 컨트롤러는 Info를 지역변수로 꺼내지 않고 Response 변환으로 바로 넘긴다

## 트랜잭션 · 동시성

- `@Transactional`은 ApplicationService에 둔다
  - 조회는 `readOnly = true`
- `open-in-view: false`다
  - 필요한 연관은 트랜잭션 안에서 로드한다
- 같은 클래스 안의 `@Transactional` 메서드 호출은 프록시를 타지 않는다
  - → `TransactionTemplate`을 쓴다
- UNIQUE 충돌(`DataIntegrityViolationException`)을 잡아 기존 결과를 돌려줄 때
  - 트랜잭션 밖에서 잡는다
  - 새 트랜잭션에서 재조회한다
- 락 전략은 `docs/design/03-domain-model.md` §7을 따른다
  - 임의로 바꾸지 않는다
- 외부 I/O(파일·외부 API)와 DB 커밋의 순서를 의식한다
  - 한쪽만 성공한 상태가 남지 않게 한다

## 예외

- 공통 베이스 `BusinessException`·`ErrorCode`는 `domain/exception`에 둔다
- 도메인별 예외는 `domain/{domain}/exception/{Domain}Exception` 하나만 둔다
  - `ErrorCode`를 생성자로 받는다
  - `throw new OrderException(ErrorCode.ORDER_NOT_FOUND)`
  - 상황별 예외 클래스(`OrderNotFoundException` 등)는 만들지 않는다
- `ErrorCode`가 HTTP 상태를 갖는다
- `GlobalExceptionHandler`가 `BusinessException`을 한 곳에서 응답으로 바꾼다
  - 5xx 코드는 error 로그, 4xx는 info 로그로 남는다
  - 상황별 설명이 필요하면 `new OrderException(ErrorCode.X, "설명")`
    - 응답 `message`에 그대로 나간다
  - Bean Validation 실패는 자동 응답된다
    - `INVALID_INPUT` + `필드: 사유` 메시지
- 새 에러
  - `docs/design/04-api-spec.md` §4에 먼저 추가
  - → `ErrorCode`
  - → 도메인 예외로 던진다
- `catch (RuntimeException | Exception)`으로 뭉개지 않는다
  - 비즈니스 예외는 원래 코드로 흘려보낸다

## Lombok

- 컴포넌트: `@RequiredArgsConstructor`
  - 주입받은 빈으로 다른 객체를 조립해야 하면 생성자를 직접 쓴다
    - 예: `PlatformTransactionManager` → `TransactionTemplate`
- 엔티티
  - `@Getter`
  - `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
  - 생성은 정적 팩토리
    - 필요하면 private `@Builder`
- 인스턴스를 만들지 않는 홀더
  - `@NoArgsConstructor(access = AccessLevel.PRIVATE)`

## 설정

- 정책성 값은 코드에 하드코딩하지 않는다
  - 예: 한도·허용 확장자·최대 개수 등
  - `application.yml`에 둔다
  - `support/properties`의 `@ConfigurationProperties`로 둔다
  - domain은 properties를 직접 주입받지 않는다 (ArchUnit이 막는다)
    - ApplicationService가 읽어 도메인 메서드 인자로 넘긴다
- seed는 `src/main/resources/data.sql`에 `INSERT IGNORE`로 쓴다
  - 기동마다 실행된다 (`sql.init.mode: always`)
- API 제목·설명은 `support/config/OpenApiConfig`의 TODO를 채운다
- 시간은 `Asia/Seoul` 기준이다 (Hibernate·Jackson·MySQL 모두 설정됨)
- `JpaConfig`가 `@EnableJpaAuditing`을 켜 두었다
  - `@CreatedDate`·`@LastModifiedDate`를 바로 쓸 수 있다
- `ddl-auto: update`
  - 마이그레이션 도구 없음
  - 스키마는 엔티티를 따른다
