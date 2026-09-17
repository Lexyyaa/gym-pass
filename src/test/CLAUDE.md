# src/test 테스트 규칙

`src/test` 아래 파일을 읽을 때 루트 CLAUDE.md와 함께 로드된다.
테스트 케이스의 원본은 `docs/design/01-requirements.md`의 TC 표다.
여기에 없는 케이스를 만들었다면 TC 표에 추가를 제안한다.

## 이름과 위치

- `@DisplayName`은 한글로 쓰고 TC ID를 앞에 붙인다
  - → `@DisplayName("[TC-1-02] 만료된 계약으로 접수하면 예외가 발생한다")`
- 테스트 클래스는 검증 대상의 이름과 패키지를 따른다
  - 애그리거트 메서드: 애그리거트 접두
    - `OrderPlaceTest` = `Order`의 주문 행위
  - application·presentation: 서비스·컨트롤러 기준
    - `PlaceConcurrencyTest`, `PlaceApiTest`
- 본문은 `// given` `// when` `// then` 주석으로 나눈다

## 종류

| 대상 | 방식 | 위치 |
|---|---|---|
| Domain | 순수 단위 테스트 (Spring 없음) | `domain/**` |
| Service | `@IntegrationTest` (Testcontainers MySQL) | `application/**` |
| API | `@IntegrationTest` + 주입받은 `MockMvc` | `presentation/**` |
| 구조 | ArchUnit | `architecture/ArchitectureTest` |

- 통합 테스트는 `support/IntegrationTest` 하나만 붙인다
  - `@SpringBootTest`를 직접 쓰거나 클래스마다 구성을 바꾸면
    - → 컨텍스트와 컨테이너가 매번 새로 뜬다
- 프로파일은 `test`다
  - 덮어쓸 설정은 `src/test/resources/application-test.yml`에 둔다
  - main 설정을 통째로 대체하지 않는다
- 도메인 규칙은 Domain 테스트에서 촘촘하게 확인한다
  - API 테스트는 대표 성공·실패와 HTTP 매핑만 본다
- 락·트랜잭션·UNIQUE 제약은 실제 MySQL이 필요하다
  - H2로 대체하지 않는다

## 단언

- 상태코드만 단언하지 않는다
  - 응답 본문의 핵심 필드까지 확인한다
  - DB 상태(건수·값)까지 확인한다
- 실패 케이스는 예외 타입과 `ErrorCode`까지 단언한다
  ```java
  assertThatThrownBy(() -> order.cancel())
          .isInstanceOf(OrderException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
  ```
- 롤백 케이스는 "예외가 났다"로 끝내지 않는다
  - 부분 저장이 없는지 확인한다
- 입력 오류 케이스는 500이 아니라 4xx인지 확인한다

## 동시성

- `support/ConcurrencyRunner.run(스레드 수, 작업)`으로 실제 경합을 만든다
  - 스레드는 10개 이상
  - 내부 동작
    - 시작 래치로 모든 스레드를 같은 시점에 출발시킨다
    - 30초 안에 안 끝나면 실패시킨다 (데드락 의심)
- 결과는 DB에서 다시 읽어 확인한다
  - 생성 건수
  - 카운터 값
  - 중복 여부
- 동시성 테스트는 `@Transactional` 롤백을 쓸 수 없다
  - 테스트가 만든 데이터는 직접 정리한다

## 데이터

- 테스트 DB에도 `data.sql` seed가 들어간다 (`sql.init.mode: always`)
  - 건수 단언은 테스트가 만든 키로 필터한다
- 테스트에 필요한 데이터는 테스트가 직접 만든다
  - seed(`data.sql`) 값에 기대지 않는다
- seed를 바꾸거나 지우는 테스트를 만들지 않는다
  - 같은 컨텍스트를 쓰는 다른 테스트가 깨진다
- 시간에 의존하는 검증은 경계값(당일·전날·다음날)을 명시적으로 만든다
