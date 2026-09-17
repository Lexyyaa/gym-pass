package com.gym.pass.presentation.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.pass.support.IntegrationTest;
import com.gym.pass.support.fixture.AttendanceFixture;
import com.gym.pass.support.web.BranchIdArgumentResolver;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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

/** API-3 출입 기록 (FR-3.1 ~ FR-3.4). 기준일은 서버 Clock 빈의 오늘이다. */
@IntegrationTest
class AttendanceEnterApiTest {

    private static final long BRANCH_ID = 950_001L;
    private static final long OTHER_BRANCH_ID = 950_002L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Clock clock;

    private AttendanceFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new AttendanceFixture(jdbcTemplate, clock, "010-9500-");
        fixture.createBranch(BRANCH_ID);
        fixture.createBranch(OTHER_BRANCH_ID);
    }

    @AfterEach
    void tearDown() {
        fixture.cleanUp(BRANCH_ID, OTHER_BRANCH_ID);
    }

    @Test
    @DisplayName("[TC-3-01] 유효 기간제 회원권으로 출입하면 200이고 서버 시각으로 기록 1건이 남으며 차감은 없다")
    void periodEntry() throws Exception {
        // given
        long memberId = fixture.createMember(1);
        LocalDate today = fixture.today();
        long membershipId = fixture.insertPeriodMembership(
                memberId,
                BRANCH_ID,
                "ACTIVE",
                today.minusDays(3),
                today.minusDays(3).plusMonths(1),
                1);

        // when
        ResultActions result = enter(memberId);

        // then
        String body = result.andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipId").value(membershipId))
                .andExpect(jsonPath("$.attendedAt").value(startsWith(today.toString())))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        Map<String, Object> saved = jdbcTemplate.queryForMap(
                "SELECT * FROM attendance_record WHERE id = ?",
                json.get("attendanceId").asLong());
        assertThat(saved.get("member_id")).isEqualTo(memberId);
        assertThat(saved.get("membership_id")).isEqualTo(membershipId);
        assertThat(saved.get("branch_id")).isEqualTo(BRANCH_ID);
        assertThat(saved.get("deducted")).isEqualTo(false);
        assertThat(saved.get("entry_date").toString()).isEqualTo(today.toString());
        assertThat(((LocalDateTime) saved.get("entry_at")).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .isEqualTo(json.get("attendedAt").asText());
        assertThat(fixture.countAttendances(memberId)).isEqualTo(1);
        assertThat(fixture.remainingCount(membershipId)).isNull();
        assertThat(fixture.status(membershipId)).isEqualTo("ACTIVE");
        assertThat(fixture.countDeductedHistories(membershipId)).isZero();
    }

    @Test
    @DisplayName("[TC-3-02] 횟수제 첫 출입이면 200이고 기록 1건 · 잔여 10→9 · DEDUCTED 이력 1건이다")
    void countFirstEntry() throws Exception {
        // given
        long memberId = fixture.createMember(2);
        LocalDate today = fixture.today();
        long membershipId =
                fixture.insertCountMembership(memberId, BRANCH_ID, "ACTIVE", today, today.plusMonths(6), 10);

        // when
        ResultActions result = enter(memberId);

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceId").isNumber())
                .andExpect(jsonPath("$.membershipId").value(membershipId));
        assertThat(fixture.countAttendances(memberId)).isEqualTo(1);
        assertThat(fixture.countDeductedAttendances(membershipId)).isEqualTo(1);
        assertThat(fixture.remainingCount(membershipId)).isEqualTo(9);
        assertThat(fixture.status(membershipId)).isEqualTo("ACTIVE");
        Map<String, Object> history = jdbcTemplate.queryForMap(
                "SELECT * FROM membership_history WHERE membership_id = ? AND event_type = 'DEDUCTED'", membershipId);
        assertThat(history.get("member_id")).isEqualTo(memberId);
        assertThat(history.get("branch_id")).isEqualTo(BRANCH_ID);
        assertThat(history.get("remaining_count_before")).isEqualTo(10);
        assertThat(history.get("remaining_count_after")).isEqualTo(9);
    }

    @Test
    @DisplayName("[TC-3-04] 회원권이 없는 회원이 출입하면 409 ATTENDANCE_NO_VALID_MEMBERSHIP이고 기록이 없다")
    void noMembership() throws Exception {
        // given
        long memberId = fixture.createMember(3);

        // when
        ResultActions result = enter(memberId);

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ATTENDANCE_NO_VALID_MEMBERSHIP"));
        assertThat(fixture.countAttendances(memberId)).isZero();
    }

    @Test
    @DisplayName("[TC-3-04] 만료 · 취소 상태 회원권만 있으면 종료일이 남아 있어도 409이고 기록 · 차감이 없다")
    void expiredOrCanceledOnly() throws Exception {
        // given
        long memberId = fixture.createMember(4);
        LocalDate today = fixture.today();
        long expiredId = fixture.insertCountMembership(memberId, BRANCH_ID, "EXPIRED", today, today.plusMonths(6), 5);
        long canceledId = fixture.insertCountMembership(memberId, BRANCH_ID, "CANCELED", today, today.plusMonths(6), 5);

        // when
        ResultActions result = enter(memberId);

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ATTENDANCE_NO_VALID_MEMBERSHIP"));
        assertThat(fixture.countAttendances(memberId)).isZero();
        assertThat(fixture.remainingCount(expiredId)).isEqualTo(5);
        assertThat(fixture.remainingCount(canceledId)).isEqualTo(5);
        assertThat(fixture.countDeductedHistories(expiredId)).isZero();
        assertThat(fixture.countDeductedHistories(canceledId)).isZero();
    }

    @Test
    @DisplayName("[TC-3-08] 유효 회원권이 다른 지점 소속이면 403 BRANCH_FORBIDDEN이고 기록 · 차감이 없다")
    void otherBranchMembership() throws Exception {
        // given
        long memberId = fixture.createMember(5);
        LocalDate today = fixture.today();
        long membershipId =
                fixture.insertCountMembership(memberId, OTHER_BRANCH_ID, "ACTIVE", today, today.plusMonths(6), 10);

        // when
        ResultActions result = enter(memberId);

        // then
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("BRANCH_FORBIDDEN"));
        assertThat(fixture.countAttendances(memberId)).isZero();
        assertThat(fixture.remainingCount(membershipId)).isEqualTo(10);
        assertThat(fixture.countDeductedHistories(membershipId)).isZero();
    }

    @Test
    @DisplayName("없는 회원이 출입하면 404 MEMBER_NOT_FOUND다")
    void memberNotFound() throws Exception {
        // given
        long unknownMemberId = Long.MAX_VALUE;

        // when
        ResultActions result = enter(unknownMemberId);

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
        assertThat(fixture.countAttendances(unknownMemberId)).isZero();
    }

    @Test
    @DisplayName("memberId가 없으면 400 COMMON_INVALID_INPUT이다")
    void memberIdMissing() throws Exception {
        // given
        Map<String, Object> body = new HashMap<>();
        body.put("memberId", null);

        // when
        ResultActions result = mockMvc.perform(post("/api/attendances")
                .header(BranchIdArgumentResolver.HEADER, BRANCH_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"));
    }

    @Test
    @DisplayName("X-Branch-Id 헤더가 없으면 400이고 기록 · 차감이 없다")
    void branchHeaderMissing() throws Exception {
        // given
        long memberId = fixture.createMember(6);
        LocalDate today = fixture.today();
        long membershipId =
                fixture.insertCountMembership(memberId, BRANCH_ID, "ACTIVE", today, today.plusMonths(6), 10);

        // when
        ResultActions result = mockMvc.perform(post("/api/attendances")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("memberId", memberId))));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"));
        assertThat(fixture.countAttendances(memberId)).isZero();
        assertThat(fixture.remainingCount(membershipId)).isEqualTo(10);
    }

    private ResultActions enter(long memberId) throws Exception {
        return mockMvc.perform(post("/api/attendances")
                .header(BranchIdArgumentResolver.HEADER, BRANCH_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("memberId", memberId))));
    }
}
