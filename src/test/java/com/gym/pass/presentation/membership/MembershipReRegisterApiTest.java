package com.gym.pass.presentation.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.pass.support.IntegrationTest;
import com.gym.pass.support.web.BranchIdArgumentResolver;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** API-2 재등록 경계 (FR-2.3 · D-15 · D-21). 기준일은 서버 Clock 빈의 오늘이다. */
@IntegrationTest
class MembershipReRegisterApiTest {

    private static final long BRANCH_ID = 930_001L;
    private static final long OTHER_BRANCH_ID = 930_002L;

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
        fixture = new MembershipApiFixture(jdbcTemplate, clock, BRANCH_ID, "010-9300-");
        fixture.createBranch();
    }

    @AfterEach
    void tearDown() {
        fixture.cleanUp();
    }

    @Test
    @DisplayName("[TC-2-10] 과거 시작일로 등록해 종료일 < 오늘인 stale ACTIVE만 보유한 회원에 등록하면 201이고 새 회원권이 저장된다")
    void staleActiveByPastStartDate() throws Exception {
        // given
        long memberId = fixture.createMember(1);
        LocalDate today = fixture.today();
        register(MembershipRegisterApiTest.period(memberId, today.minusMonths(2), 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        LocalDate staleEndDate = jdbcTemplate.queryForObject(
                "SELECT end_date FROM membership WHERE member_id = ?", LocalDate.class, memberId);
        assertThat(staleEndDate).isBefore(today);

        // when
        ResultActions result = register(MembershipRegisterApiTest.period(memberId, today, 1));

        // then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.startDate").value(today.toString()));
        assertThat(fixture.countMemberships(memberId)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM membership WHERE member_id = ? AND status = 'ACTIVE'",
                        Integer.class,
                        memberId))
                .isEqualTo(2);
        assertThat(fixture.countHistories(memberId)).isEqualTo(2);
    }

    @Test
    @DisplayName("[TC-2-10] 종료일 = 어제인 stale ACTIVE만 보유한 회원에 등록하면 201이다 (경계: 전날)")
    void staleActiveEndedYesterday() throws Exception {
        // given
        long memberId = fixture.createMember(2);
        LocalDate today = fixture.today();
        fixture.insertPeriodMembership(memberId, "ACTIVE", today.minusMonths(1), today.minusDays(1), 1);

        // when
        ResultActions result = register(MembershipRegisterApiTest.period(memberId, today, 1));

        // then
        result.andExpect(status().isCreated());
        assertThat(fixture.countMemberships(memberId)).isEqualTo(2);
    }

    @Test
    @DisplayName("[TC-2-10] 종료일 = 어제인 stale PAUSED만 보유한 회원에 등록하면 201이다")
    void stalePausedEndedYesterday() throws Exception {
        // given
        long memberId = fixture.createMember(3);
        LocalDate today = fixture.today();
        fixture.insertPeriodMembership(memberId, "PAUSED", today.minusMonths(1), today.minusDays(1), 1);

        // when
        ResultActions result = register(MembershipRegisterApiTest.period(memberId, today, 1));

        // then
        result.andExpect(status().isCreated());
        assertThat(fixture.countMemberships(memberId)).isEqualTo(2);
    }

    @Test
    @DisplayName("[TC-2-10] 종료일 = 오늘인 ACTIVE를 보유한 회원에 등록하면 409이고 저장되지 않는다 (경계: 당일은 유효)")
    void activeEndsToday() throws Exception {
        // given
        long memberId = fixture.createMember(4);
        LocalDate today = fixture.today();
        fixture.insertPeriodMembership(memberId, "ACTIVE", today.minusMonths(1), today, 1);

        // when
        ResultActions result = register(MembershipRegisterApiTest.period(memberId, today.plusDays(1), 1));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MEMBERSHIP_ALREADY_ACTIVE"));
        assertThat(fixture.countMemberships(memberId)).isEqualTo(1);
    }

    @Test
    @DisplayName("[TC-2-10] EXPIRED · CANCELED만 보유한 회원은 종료일이 미래여도 등록할 수 있다")
    void expiredOrCanceledOnly() throws Exception {
        // given
        long memberId = fixture.createMember(5);
        LocalDate today = fixture.today();
        fixture.insertPeriodMembership(memberId, "EXPIRED", today.minusMonths(1), today.plusDays(10), 1);
        fixture.insertPeriodMembership(memberId, "CANCELED", today.minusMonths(1), today.plusDays(10), 1);

        // when
        ResultActions result = register(MembershipRegisterApiTest.period(memberId, today, 1));

        // then
        result.andExpect(status().isCreated());
        assertThat(fixture.countMemberships(memberId)).isEqualTo(3);
    }

    @Test
    @DisplayName("[TC-2-18] 등록 거부 검사는 전 지점을 본다 — 다른 지점의 유효 회원권이 있으면 409이고 저장되지 않는다")
    void activeInOtherBranch() throws Exception {
        // given
        long memberId = fixture.createMember(6);
        LocalDate today = fixture.today();
        fixture.insertPeriodMembership(memberId, OTHER_BRANCH_ID, "ACTIVE", today, today.plusMonths(1), 1);

        // when
        ResultActions result = register(MembershipRegisterApiTest.period(memberId, today, 1));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MEMBERSHIP_ALREADY_ACTIVE"));
        assertThat(fixture.countMemberships(memberId)).isEqualTo(1);
        assertThat(fixture.countHistories(memberId)).isZero();
    }

    private ResultActions register(Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/memberships")
                .header(BranchIdArgumentResolver.HEADER, BRANCH_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }
}
