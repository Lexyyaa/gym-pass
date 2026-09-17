package com.gym.pass.presentation.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.pass.support.IntegrationTest;
import com.gym.pass.support.web.BranchIdArgumentResolver;
import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** API-2 회원권 등록 성공 · 실패. */
@IntegrationTest
class MembershipRegisterApiTest {

    private static final long BRANCH_ID = 920_001L;
    private static final long MISSING_BRANCH_ID = 920_999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Clock clock;

    private MembershipApiFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new MembershipApiFixture(jdbcTemplate, clock, BRANCH_ID, "010-9200-");
        fixture.createBranch();
    }

    @AfterEach
    void tearDown() {
        fixture.cleanUp();
    }

    @Test
    @DisplayName("[TC-2-02] 기간제 3개월 회원권을 등록하면 201, 종료일 = 시작일+3개월, 상태 ACTIVE이고 REGISTERED 이력 1건이 남는다")
    void registerPeriod() throws Exception {
        // given
        long memberId = fixture.createMember(1);
        LocalDate startDate = LocalDate.of(2026, 9, 17);

        // when
        ResultActions result = register(BRANCH_ID, period(memberId, startDate, 3));

        // then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.membershipId").isNumber())
                .andExpect(jsonPath("$.memberId").value(memberId))
                .andExpect(jsonPath("$.branchId").value(BRANCH_ID))
                .andExpect(jsonPath("$.type").value("PERIOD"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.startDate").value("2026-09-17"))
                .andExpect(jsonPath("$.endDate").value("2026-12-17"))
                .andExpect(jsonPath("$.remainingCount").doesNotExist())
                .andExpect(jsonPath("$.paymentAmount").value(300000));
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT branch_id, type, status, start_date, end_date, months, remaining_count, price"
                        + " FROM membership WHERE member_id = ?",
                memberId);
        assertThat(row)
                .containsEntry("branch_id", BRANCH_ID)
                .containsEntry("type", "PERIOD")
                .containsEntry("status", "ACTIVE")
                .containsEntry("end_date", java.sql.Date.valueOf("2026-12-17"))
                .containsEntry("months", 3)
                .containsEntry("remaining_count", null)
                .containsEntry("price", 300000L);
        Map<String, Object> history = jdbcTemplate.queryForMap(
                "SELECT h.membership_id, h.branch_id, h.event_type, h.end_date_before, h.end_date_after"
                        + " FROM membership_history h WHERE h.member_id = ?",
                memberId);
        assertThat(history)
                .containsEntry("branch_id", BRANCH_ID)
                .containsEntry("event_type", "REGISTERED")
                .containsEntry("end_date_before", null)
                .containsEntry("end_date_after", java.sql.Date.valueOf("2026-12-17"));
        Number membershipId = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.membershipId");
        assertThat(((Number) history.get("membership_id")).longValue()).isEqualTo(membershipId.longValue());
    }

    @Test
    @DisplayName("[TC-2-04] 횟수제 회원권을 등록하면 201, 종료일 = 시작일+6개월, 개월 수 6, 잔여 횟수 = 등록 횟수로 저장된다")
    void registerCount() throws Exception {
        // given
        long memberId = fixture.createMember(2);

        // when
        ResultActions result = register(BRANCH_ID, count(memberId, LocalDate.of(2026, 9, 17), 10));

        // then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("COUNT"))
                .andExpect(jsonPath("$.endDate").value("2027-03-17"))
                .andExpect(jsonPath("$.remainingCount").value(10));
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT months, total_count, remaining_count FROM membership WHERE member_id = ?", memberId);
        assertThat(row)
                .containsEntry("months", 6)
                .containsEntry("total_count", 10)
                .containsEntry("remaining_count", 10);
        assertThat(fixture.countHistories(memberId)).isEqualTo(1);
    }

    @Test
    @DisplayName("[TC-2-05] 유효 회원권 보유 회원에 재등록하면 409 MEMBERSHIP_ALREADY_ACTIVE이고 저장되지 않는다")
    void alreadyActive() throws Exception {
        // given
        long memberId = fixture.createMember(3);
        LocalDate today = fixture.today();
        register(BRANCH_ID, period(memberId, today, 1)).andExpect(status().isCreated());

        // when
        ResultActions result = register(BRANCH_ID, count(memberId, today, 10));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MEMBERSHIP_ALREADY_ACTIVE"));
        assertThat(fixture.countMemberships(memberId)).isEqualTo(1);
        assertThat(fixture.countHistories(memberId)).isEqualTo(1);
    }

    static Stream<Arguments> invalidValues() {
        return Stream.of(
                Arguments.of("0개월", "PERIOD", "months", 0),
                Arguments.of("0횟수", "COUNT", "count", 0),
                Arguments.of("음수 금액", "PERIOD", "paymentAmount", -1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidValues")
    @DisplayName("[TC-2-07] 0개월 · 0횟수 · 음수 금액이면 400 COMMON_INVALID_INPUT이고 저장되지 않는다")
    void invalidValue(String label, String type, String field, int value) throws Exception {
        // given
        long memberId = fixture.createMember(4);
        Map<String, Object> body = typed(memberId, type);
        body.put(field, value);

        // when
        ResultActions result = register(BRANCH_ID, body);

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith(field + ":")));
        assertThat(fixture.countMemberships(memberId)).isZero();
    }

    @Test
    @DisplayName("[TC-2-08] 없는 회원 ID로 회원권을 등록하면 404 MEMBER_NOT_FOUND이고 저장되지 않는다")
    void memberNotFound() throws Exception {
        // given
        long missingMemberId = 999_999_999L;

        // when
        ResultActions result = register(BRANCH_ID, period(missingMemberId, LocalDate.of(2026, 9, 17), 1));

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
        assertThat(fixture.countMemberships(missingMemberId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM membership WHERE branch_id = ?", Integer.class, BRANCH_ID))
                .isZero();
    }

    @Test
    @DisplayName("[TC-2-09] 정지 중(PAUSED) 회원권 보유 회원에 재등록하면 409 MEMBERSHIP_ALREADY_ACTIVE이고 저장되지 않는다")
    void pausedExists() throws Exception {
        // given
        long memberId = fixture.createMember(5);
        LocalDate today = fixture.today();
        fixture.insertPeriodMembership(memberId, "PAUSED", today.minusDays(10), today.plusDays(20));

        // when
        ResultActions result = register(BRANCH_ID, period(memberId, today, 1));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MEMBERSHIP_ALREADY_ACTIVE"));
        assertThat(fixture.countMemberships(memberId)).isEqualTo(1);
        assertThat(fixture.countHistories(memberId)).isZero();
    }

    @Test
    @DisplayName("[TC-2-11] 시작일이 미래인 ACTIVE 회원권 보유 회원에 등록하면 409 MEMBERSHIP_ALREADY_ACTIVE이고 저장되지 않는다")
    void futureActiveExists() throws Exception {
        // given
        long memberId = fixture.createMember(6);
        LocalDate today = fixture.today();
        register(BRANCH_ID, period(memberId, today.plusMonths(2), 1)).andExpect(status().isCreated());

        // when
        ResultActions result = register(BRANCH_ID, period(memberId, today, 1));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MEMBERSHIP_ALREADY_ACTIVE"));
        assertThat(fixture.countMemberships(memberId)).isEqualTo(1);
        assertThat(fixture.countHistories(memberId)).isEqualTo(1);
    }

    static Stream<Arguments> missingTypeValues() {
        return Stream.of(Arguments.of("PERIOD", "months"), Arguments.of("COUNT", "count"));
    }

    @ParameterizedTest(name = "{0}인데 {1} 없음")
    @MethodSource("missingTypeValues")
    @DisplayName("[TC-2-13] 기간제인데 months 없음 · 횟수제인데 count 없음이면 400 MEMBERSHIP_INVALID_INPUT이고 저장되지 않는다")
    void missingTypeValue(String type, String missingField) throws Exception {
        // given
        long memberId = fixture.createMember(7);
        Map<String, Object> body = typed(memberId, type);
        body.remove(missingField);

        // when
        ResultActions result = register(BRANCH_ID, body);

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MEMBERSHIP_INVALID_INPUT"));
        assertThat(fixture.countMemberships(memberId)).isZero();
        assertThat(fixture.countHistories(memberId)).isZero();
    }

    static Stream<Arguments> overLimitValues() {
        return Stream.of(Arguments.of("PERIOD", "months", 121), Arguments.of("COUNT", "count", 1001));
    }

    @ParameterizedTest(name = "{0} {1}={2}")
    @MethodSource("overLimitValues")
    @DisplayName("[TC-2-14] 기간제 121개월 · 횟수제 1001회면 400 MEMBERSHIP_INVALID_INPUT이고 저장되지 않는다")
    void overLimit(String type, String field, int value) throws Exception {
        // given
        long memberId = fixture.createMember(11);
        Map<String, Object> body = typed(memberId, type);
        body.put(field, value);

        // when
        ResultActions result = register(BRANCH_ID, body);

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MEMBERSHIP_INVALID_INPUT"));
        assertThat(fixture.countMemberships(memberId)).isZero();
        assertThat(fixture.countHistories(memberId)).isZero();
    }

    @ParameterizedTest(name = "시작일 {0}")
    @ValueSource(strings = {"9999-12-31", "+999999999-12-31"})
    @DisplayName("[TC-2-15] 종료일이 9999-12-31을 넘는 시작일이면 400 MEMBERSHIP_INVALID_INPUT이고 저장되지 않는다")
    void endDateOutOfRange(String startDate) throws Exception {
        // given
        long memberId = fixture.createMember(12);
        Map<String, Object> body = typed(memberId, "PERIOD");
        body.put("startDate", startDate);

        // when
        ResultActions result = register(BRANCH_ID, body);

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MEMBERSHIP_INVALID_INPUT"));
        assertThat(fixture.countMemberships(memberId)).isZero();
        assertThat(fixture.countHistories(memberId)).isZero();
    }

    static Stream<Arguments> oppositeTypeValues() {
        return Stream.of(
                Arguments.of("PERIOD", "count", 5, "MEMBERSHIP_INVALID_INPUT"),
                Arguments.of("COUNT", "months", 3, "MEMBERSHIP_INVALID_INPUT"),
                Arguments.of("PERIOD", "count", 0, "COMMON_INVALID_INPUT"),
                Arguments.of("COUNT", "months", -1, "COMMON_INVALID_INPUT"));
    }

    @ParameterizedTest(name = "{0}에 {1}={2} → {3}")
    @MethodSource("oppositeTypeValues")
    @DisplayName("[TC-2-16] 기간제에 count · 횟수제에 months가 오면 400이고 저장되지 않는다 (0 · 음수는 요청 검증에서 COMMON)")
    void oppositeTypeValue(String type, String field, int value, String errorCode) throws Exception {
        // given
        long memberId = fixture.createMember(13);
        Map<String, Object> body = typed(memberId, type);
        body.put(field, value);

        // when
        ResultActions result = register(BRANCH_ID, body);

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(errorCode));
        assertThat(fixture.countMemberships(memberId)).isZero();
        assertThat(fixture.countHistories(memberId)).isZero();
    }

    @Test
    @DisplayName("허용되지 않은 type이면 400 COMMON_INVALID_INPUT이고 저장되지 않는다")
    void unknownType() throws Exception {
        // given
        long memberId = fixture.createMember(8);
        Map<String, Object> body = period(memberId, LocalDate.of(2026, 9, 17), 1);
        body.put("type", "LIFETIME");

        // when
        ResultActions result = register(BRANCH_ID, body);

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"));
        assertThat(fixture.countMemberships(memberId)).isZero();
    }

    @Test
    @DisplayName("[TC-1-03] 회원권 등록에 X-Branch-Id 헤더가 없으면 400 COMMON_INVALID_INPUT이고 저장되지 않는다")
    void missingHeader() throws Exception {
        // given
        long memberId = fixture.createMember(9);
        String body = objectMapper.writeValueAsString(period(memberId, LocalDate.of(2026, 9, 17), 1));

        // when
        // then
        mockMvc.perform(post("/api/memberships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("X-Branch-Id: 숫자 지점 ID가 필요합니다."));
        assertThat(fixture.countMemberships(memberId)).isZero();
    }

    @Test
    @DisplayName("[TC-1-04] 회원권 등록에 존재하지 않는 지점 ID 헤더면 404 BRANCH_NOT_FOUND이고 저장되지 않는다")
    void branchNotFound() throws Exception {
        // given
        long memberId = fixture.createMember(10);

        // when
        ResultActions result = register(MISSING_BRANCH_ID, period(memberId, LocalDate.of(2026, 9, 17), 1));

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("BRANCH_NOT_FOUND"));
        assertThat(fixture.countMemberships(memberId)).isZero();
    }

    private ResultActions register(long branchId, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/memberships")
                .header(BranchIdArgumentResolver.HEADER, branchId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    /** 종류에 맞는 값만 담은 정상 본문 (시작일 2026-09-17). */
    private static Map<String, Object> typed(long memberId, String type) {
        LocalDate startDate = LocalDate.of(2026, 9, 17);
        return "PERIOD".equals(type) ? period(memberId, startDate, 1) : count(memberId, startDate, 10);
    }

    static Map<String, Object> period(long memberId, LocalDate startDate, int months) {
        Map<String, Object> body = new HashMap<>();
        body.put("memberId", memberId);
        body.put("type", "PERIOD");
        body.put("startDate", startDate.toString());
        body.put("months", months);
        body.put("paymentAmount", 300000);
        return body;
    }

    static Map<String, Object> count(long memberId, LocalDate startDate, int count) {
        Map<String, Object> body = new HashMap<>();
        body.put("memberId", memberId);
        body.put("type", "COUNT");
        body.put("startDate", startDate.toString());
        body.put("count", count);
        body.put("paymentAmount", 200000);
        return body;
    }
}
