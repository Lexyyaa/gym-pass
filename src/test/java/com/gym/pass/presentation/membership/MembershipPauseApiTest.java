package com.gym.pass.presentation.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.pass.support.IntegrationTest;
import com.gym.pass.support.fixture.AttendanceFixture;
import com.gym.pass.support.web.BranchIdArgumentResolver;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** API-4 정지 등록 · API-5 조기 해제 (FR-4.1 ~ FR-4.4). 기준일은 서버 Clock 빈의 오늘이다. */
@IntegrationTest
class MembershipPauseApiTest {

    private static final long BRANCH_ID = 950_301L;
    private static final long OTHER_BRANCH_ID = 950_302L;

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
        fixture = new AttendanceFixture(jdbcTemplate, clock, "010-9503-");
        fixture.createBranch(BRANCH_ID);
        fixture.createBranch(OTHER_BRANCH_ID);
    }

    @AfterEach
    void tearDown() {
        fixture.cleanUp(BRANCH_ID, OTHER_BRANCH_ID);
    }

    @Test
    @DisplayName("[TC-4-01] 7일 정지를 예약하면 200이고 종료일 +7일 · 정지 1건 · PAUSED 이력 1건이며 상태는 ACTIVE다")
    void pauseSevenDays() throws Exception {
        // given
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(1);
        LocalDate endDate = today.plusMonths(3);
        long membershipId = fixture.insertPeriodMembership(memberId, BRANCH_ID, "ACTIVE", today, endDate, 3);
        LocalDate pauseStart = today.plusDays(3);

        // when
        ResultActions result = pause(membershipId, pauseStart, 7);

        // then
        String body = result.andExpect(status().isOk())
                .andExpect(jsonPath("$.pauseId").isNumber())
                .andExpect(jsonPath("$.membershipId").value(membershipId))
                .andExpect(jsonPath("$.startDate").value(pauseStart.toString()))
                .andExpect(jsonPath("$.endDate").value(pauseStart.plusDays(6).toString()))
                .andExpect(jsonPath("$.days").value(7))
                .andExpect(jsonPath("$.membershipEndDate")
                        .value(endDate.plusDays(7).toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long pauseId = objectMapper.readTree(body).get("pauseId").asLong();
        Map<String, Object> savedPause =
                jdbcTemplate.queryForMap("SELECT * FROM membership_pause WHERE id = ?", pauseId);
        assertThat(savedPause.get("membership_id")).isEqualTo(membershipId);
        assertThat(savedPause.get("start_date").toString()).isEqualTo(pauseStart.toString());
        assertThat(savedPause.get("end_date").toString())
                .isEqualTo(pauseStart.plusDays(6).toString());
        assertThat(savedPause.get("released_date")).isNull();
        assertThat(fixture.countPauses(membershipId)).isEqualTo(1);
        assertThat(fixture.endDate(membershipId)).isEqualTo(endDate.plusDays(7));
        assertThat(fixture.status(membershipId)).isEqualTo("ACTIVE");
        Map<String, Object> history =
                jdbcTemplate.queryForMap("SELECT * FROM membership_history WHERE membership_id = ?", membershipId);
        assertThat(history.get("event_type")).isEqualTo("PAUSED");
        assertThat(history.get("member_id")).isEqualTo(memberId);
        assertThat(history.get("branch_id")).isEqualTo(BRANCH_ID);
        assertThat(history.get("end_date_before").toString()).isEqualTo(endDate.toString());
        assertThat(history.get("end_date_after").toString())
                .isEqualTo(endDate.plusDays(7).toString());
    }

    @Test
    @DisplayName("[TC-4-03] 누적 정지 일수가 개월당 7일을 넘으면 409 PAUSE_DAYS_LIMIT_EXCEEDED이고 종료일 · 정지 건수가 그대로다")
    void daysLimitExceeded() throws Exception {
        // given — 1개월권 상한 7일 중 5일 사용 예정
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(2);
        long membershipId =
                fixture.insertPeriodMembership(memberId, BRANCH_ID, "ACTIVE", today, today.plusMonths(1), 1);
        pause(membershipId, today.plusDays(1), 5).andExpect(status().isOk());
        LocalDate endDate = fixture.endDate(membershipId);

        // when
        ResultActions result = pause(membershipId, today.plusDays(10), 3);

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PAUSE_DAYS_LIMIT_EXCEEDED"));
        assertThat(fixture.endDate(membershipId)).isEqualTo(endDate);
        assertThat(fixture.countPauses(membershipId)).isEqualTo(1);
        assertThat(fixture.countHistories(membershipId, "PAUSED")).isEqualTo(1);
    }

    @Test
    @DisplayName("[TC-4-03] 누적 정지 일수가 개월당 7일과 같으면 등록된다 (경계)")
    void daysLimitBoundary() throws Exception {
        // given
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(3);
        long membershipId =
                fixture.insertPeriodMembership(memberId, BRANCH_ID, "ACTIVE", today, today.plusMonths(1), 1);
        pause(membershipId, today.plusDays(1), 5).andExpect(status().isOk());

        // when
        ResultActions result = pause(membershipId, today.plusDays(10), 2);

        // then
        result.andExpect(status().isOk()).andExpect(jsonPath("$.days").value(2));
        assertThat(fixture.countPauses(membershipId)).isEqualTo(2);
        assertThat(fixture.endDate(membershipId)).isEqualTo(today.plusMonths(1).plusDays(7));
    }

    @Test
    @DisplayName("[TC-4-04] 4번째 정지를 등록하면 409 PAUSE_COUNT_LIMIT_EXCEEDED이고 정지 3건 · 종료일이 그대로다")
    void countLimitExceeded() throws Exception {
        // given
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(4);
        long membershipId =
                fixture.insertPeriodMembership(memberId, BRANCH_ID, "ACTIVE", today, today.plusMonths(3), 3);
        pause(membershipId, today.plusDays(1), 2).andExpect(status().isOk());
        pause(membershipId, today.plusDays(5), 2).andExpect(status().isOk());
        pause(membershipId, today.plusDays(9), 2).andExpect(status().isOk());
        LocalDate endDate = fixture.endDate(membershipId);

        // when
        ResultActions result = pause(membershipId, today.plusDays(13), 2);

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PAUSE_COUNT_LIMIT_EXCEEDED"));
        assertThat(fixture.countPauses(membershipId)).isEqualTo(3);
        assertThat(fixture.endDate(membershipId)).isEqualTo(endDate);
        assertThat(endDate).isEqualTo(today.plusMonths(3).plusDays(6));
    }

    @Test
    @DisplayName("[TC-4-05] 만료 상태 회원권을 정지하면 종료일이 남아 있어도 409 MEMBERSHIP_NOT_PAUSABLE이고 종료일 · 정지 건수가 그대로다")
    void expiredStatus() throws Exception {
        // given
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(5);
        LocalDate endDate = today.plusMonths(1);
        long membershipId = fixture.insertPeriodMembership(memberId, BRANCH_ID, "EXPIRED", today, endDate, 1);

        // when
        ResultActions result = pause(membershipId, today.plusDays(1), 3);

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MEMBERSHIP_NOT_PAUSABLE"));
        assertThat(fixture.endDate(membershipId)).isEqualTo(endDate);
        assertThat(fixture.countPauses(membershipId)).isZero();
        assertThat(fixture.countHistories(membershipId, "PAUSED")).isZero();
    }

    @Test
    @DisplayName("[TC-4-05] 종료일이 어제인 ACTIVE 회원권(배치 전)을 정지하면 409 MEMBERSHIP_NOT_PAUSABLE이고 종료일이 그대로다")
    void staleActiveAfterEndDate() throws Exception {
        // given
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(6);
        LocalDate endDate = today.minusDays(1);
        long membershipId =
                fixture.insertPeriodMembership(memberId, BRANCH_ID, "ACTIVE", endDate.minusMonths(1), endDate, 1);

        // when
        ResultActions result = pause(membershipId, today, 3);

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MEMBERSHIP_NOT_PAUSABLE"));
        assertThat(fixture.endDate(membershipId)).isEqualTo(endDate);
        assertThat(fixture.countPauses(membershipId)).isZero();
        assertThat(fixture.status(membershipId)).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("[TC-4-05] 취소된 회원권을 정지하면 409 MEMBERSHIP_NOT_PAUSABLE이고 종료일이 그대로다")
    void canceledStatus() throws Exception {
        // given
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(7);
        LocalDate endDate = today.plusMonths(1);
        long membershipId = fixture.insertPeriodMembership(memberId, BRANCH_ID, "CANCELED", today, endDate, 1);

        // when
        ResultActions result = pause(membershipId, today.plusDays(1), 3);

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MEMBERSHIP_NOT_PAUSABLE"));
        assertThat(fixture.endDate(membershipId)).isEqualTo(endDate);
        assertThat(fixture.countPauses(membershipId)).isZero();
    }

    @Test
    @DisplayName("[TC-4-06] 오늘 시작하는 정지를 등록하면 PAUSED가 되고, 정지 기간 중 출입은 409 ATTENDANCE_MEMBERSHIP_PAUSED · 기록 0건이다")
    void entryDuringPause() throws Exception {
        // given
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(8);
        long membershipId =
                fixture.insertCountMembership(memberId, BRANCH_ID, "ACTIVE", today, today.plusMonths(6), 10);
        pause(membershipId, today, 3).andExpect(status().isOk());
        assertThat(fixture.status(membershipId)).isEqualTo("PAUSED");

        // when
        ResultActions result = mockMvc.perform(post("/api/attendances")
                .header(BranchIdArgumentResolver.HEADER, BRANCH_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("memberId", memberId))));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ATTENDANCE_MEMBERSHIP_PAUSED"));
        assertThat(fixture.countAttendances(memberId)).isZero();
        assertThat(fixture.remainingCount(membershipId)).isEqualTo(10);
        assertThat(fixture.countDeductedHistories(membershipId)).isZero();
    }

    @Test
    @DisplayName("[TC-4-08] 기존 정지와 기간이 겹치는 정지를 등록하면 409 PAUSE_OVERLAPPED이고 정지 1건 · 종료일이 그대로다")
    void overlapped() throws Exception {
        // given — 기존 정지: today+2 ~ today+8
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(9);
        long membershipId =
                fixture.insertPeriodMembership(memberId, BRANCH_ID, "ACTIVE", today, today.plusMonths(3), 3);
        pause(membershipId, today.plusDays(2), 7).andExpect(status().isOk());
        LocalDate endDate = fixture.endDate(membershipId);

        // when — today+8 ~ today+10 (마지막 날 하루 겹침)
        ResultActions result = pause(membershipId, today.plusDays(8), 3);

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PAUSE_OVERLAPPED"));
        assertThat(fixture.countPauses(membershipId)).isEqualTo(1);
        assertThat(fixture.endDate(membershipId)).isEqualTo(endDate);
    }

    @Test
    @DisplayName("[TC-4-08] 기존 정지 종료 다음 날 시작하는 정지는 겹치지 않아 등록된다 (경계)")
    void adjacentNotOverlapped() throws Exception {
        // given — 기존 정지: today+2 ~ today+8
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(10);
        long membershipId =
                fixture.insertPeriodMembership(memberId, BRANCH_ID, "ACTIVE", today, today.plusMonths(3), 3);
        pause(membershipId, today.plusDays(2), 7).andExpect(status().isOk());

        // when
        ResultActions result = pause(membershipId, today.plusDays(9), 3);

        // then
        result.andExpect(status().isOk());
        assertThat(fixture.countPauses(membershipId)).isEqualTo(2);
        assertThat(fixture.endDate(membershipId)).isEqualTo(today.plusMonths(3).plusDays(10));
    }

    @Test
    @DisplayName("[TC-4-09] 시작일이 어제인 소급 정지는 400 PAUSE_START_DATE_PAST이고 종료일 · 정지 건수가 그대로다")
    void startDatePast() throws Exception {
        // given
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(11);
        LocalDate endDate = today.plusMonths(3);
        long membershipId = fixture.insertPeriodMembership(memberId, BRANCH_ID, "ACTIVE", today, endDate, 3);

        // when
        ResultActions result = pause(membershipId, today.minusDays(1), 3);

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PAUSE_START_DATE_PAST"));
        assertThat(fixture.endDate(membershipId)).isEqualTo(endDate);
        assertThat(fixture.countPauses(membershipId)).isZero();
    }

    @Test
    @DisplayName("[TC-4-10] 이미 해제된 정지를 다시 해제하면 409 PAUSE_NOT_RELEASABLE이고 종료일 · 해제일이 그대로다")
    void releaseTwice() throws Exception {
        // given — 예약 정지를 시작 전에 해제했다 (사용 0일)
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(12);
        LocalDate endDate = today.plusMonths(3);
        long membershipId = fixture.insertPeriodMembership(memberId, BRANCH_ID, "ACTIVE", today, endDate, 3);
        long pauseId = pauseIdOf(pause(membershipId, today.plusDays(2), 5));
        release(membershipId, pauseId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pauseId").value(pauseId))
                .andExpect(jsonPath("$.usedDays").value(0))
                .andExpect(jsonPath("$.membershipEndDate").value(endDate.toString()));

        // when
        ResultActions result = release(membershipId, pauseId);

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PAUSE_NOT_RELEASABLE"));
        assertThat(fixture.endDate(membershipId)).isEqualTo(endDate);
        assertThat(fixture.releasedDate(pauseId)).isEqualTo(today);
        assertThat(fixture.countHistories(membershipId, "RESUMED")).isEqualTo(1);
    }

    @Test
    @DisplayName("[TC-4-10] 정지 종료일이 지난 정지를 해제하면 409 PAUSE_NOT_RELEASABLE이고 종료일이 그대로다")
    void releaseEndedPause() throws Exception {
        // given — 어제로 끝난 3일 정지
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(13);
        LocalDate endDate = today.plusMonths(3);
        long membershipId =
                fixture.insertPeriodMembership(memberId, BRANCH_ID, "ACTIVE", today.minusDays(5), endDate, 3);
        long pauseId = fixture.insertPause(membershipId, today.minusDays(3), today.minusDays(1), null);

        // when
        ResultActions result = release(membershipId, pauseId);

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PAUSE_NOT_RELEASABLE"));
        assertThat(fixture.endDate(membershipId)).isEqualTo(endDate);
        assertThat(fixture.releasedDate(pauseId)).isNull();
    }

    @Test
    @DisplayName("[TC-4-11] 없는 회원권 ID로 정지를 등록하면 404 MEMBERSHIP_NOT_FOUND이고 정지 · 이력 저장이 없다")
    void membershipNotFound() throws Exception {
        // given
        LocalDate today = fixture.today();
        int pausesBefore = fixture.countAllPauses();
        int historiesBefore = countAllHistories();

        // when
        ResultActions result = pause(Long.MAX_VALUE, today.plusDays(1), 3);

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBERSHIP_NOT_FOUND"));
        assertThat(fixture.countAllPauses()).isEqualTo(pausesBefore);
        assertThat(countAllHistories()).isEqualTo(historiesBefore);
    }

    @Test
    @DisplayName("없는 회원권 ID로 해제하면 404 MEMBERSHIP_NOT_FOUND다")
    void releaseMembershipNotFound() throws Exception {
        // given
        long unknownMembershipId = Long.MAX_VALUE;

        // when
        ResultActions result = release(unknownMembershipId, 1L);

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBERSHIP_NOT_FOUND"));
    }

    @Test
    @DisplayName("[TC-4-14] 다른 회원권의 pauseId로 해제하면 404 PAUSE_NOT_FOUND이고 두 회원권 종료일 · 해제일이 그대로다")
    void pauseOfOtherMembership() throws Exception {
        // given
        LocalDate today = fixture.today();
        long ownerId = fixture.createMember(14);
        long ownerMembershipId =
                fixture.insertPeriodMembership(ownerId, BRANCH_ID, "ACTIVE", today, today.plusMonths(3), 3);
        long pauseId = pauseIdOf(pause(ownerMembershipId, today.plusDays(1), 3));
        LocalDate ownerEndDate = fixture.endDate(ownerMembershipId);
        long otherId = fixture.createMember(15);
        LocalDate otherEndDate = today.plusMonths(3);
        long otherMembershipId = fixture.insertPeriodMembership(otherId, BRANCH_ID, "ACTIVE", today, otherEndDate, 3);

        // when
        ResultActions result = release(otherMembershipId, pauseId);

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PAUSE_NOT_FOUND"));
        assertThat(fixture.releasedDate(pauseId)).isNull();
        assertThat(fixture.endDate(ownerMembershipId)).isEqualTo(ownerEndDate);
        assertThat(fixture.endDate(otherMembershipId)).isEqualTo(otherEndDate);
        assertThat(fixture.countHistories(otherMembershipId, "RESUMED")).isZero();
    }

    @Test
    @DisplayName("[TC-4-14] 없는 pauseId로 해제하면 404 PAUSE_NOT_FOUND이고 종료일이 그대로다")
    void pauseNotFound() throws Exception {
        // given
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(16);
        LocalDate endDate = today.plusMonths(3);
        long membershipId = fixture.insertPeriodMembership(memberId, BRANCH_ID, "ACTIVE", today, endDate, 3);

        // when
        ResultActions result = release(membershipId, Long.MAX_VALUE);

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PAUSE_NOT_FOUND"));
        assertThat(fixture.endDate(membershipId)).isEqualTo(endDate);
    }

    @Test
    @DisplayName("[TC-4-15] 다른 지점 회원권에 정지를 등록하면 403 BRANCH_FORBIDDEN이고 정지 · 이력 저장이 없다")
    void pauseOtherBranch() throws Exception {
        // given
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(17);
        LocalDate endDate = today.plusMonths(3);
        long membershipId = fixture.insertPeriodMembership(memberId, OTHER_BRANCH_ID, "ACTIVE", today, endDate, 3);

        // when
        ResultActions result = pause(membershipId, today.plusDays(1), 3);

        // then
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("BRANCH_FORBIDDEN"));
        assertThat(fixture.countPauses(membershipId)).isZero();
        assertThat(fixture.endDate(membershipId)).isEqualTo(endDate);
        assertThat(fixture.countHistories(membershipId, "PAUSED")).isZero();
    }

    @Test
    @DisplayName("[TC-4-16] 다른 지점 회원권의 정지를 해제하면 403 BRANCH_FORBIDDEN이고 종료일 · 해제일 · 상태가 그대로다")
    void releaseOtherBranch() throws Exception {
        // given — 오늘 시작한 3일 정지로 종료일이 이미 3일 늘어난 상태
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(18);
        LocalDate endDate = today.plusMonths(3).plusDays(3);
        long membershipId = fixture.insertPeriodMembership(memberId, OTHER_BRANCH_ID, "PAUSED", today, endDate, 3);
        long pauseId = fixture.insertPause(membershipId, today, today.plusDays(2), null);

        // when
        ResultActions result = release(membershipId, pauseId);

        // then
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("BRANCH_FORBIDDEN"));
        assertThat(fixture.endDate(membershipId)).isEqualTo(endDate);
        assertThat(fixture.releasedDate(pauseId)).isNull();
        assertThat(fixture.status(membershipId)).isEqualTo("PAUSED");
        assertThat(fixture.countHistories(membershipId, "RESUMED")).isZero();
    }

    @Test
    @DisplayName("[TC-4-17] 잔여 0인 횟수제(소진 당일)를 정지하면 409 MEMBERSHIP_NOT_PAUSABLE이고 종료일 · 정지 건수가 그대로다")
    void exhaustedCountMembership() throws Exception {
        // given — 오늘 마지막 1회를 차감한 상태
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(19);
        LocalDate endDate = today.plusMonths(6);
        long membershipId = fixture.insertCountMembership(memberId, BRANCH_ID, "ACTIVE", today, endDate, 0);
        fixture.insertAttendance(memberId, membershipId, BRANCH_ID, today.atStartOfDay(), true);

        // when
        ResultActions result = pause(membershipId, today.plusDays(1), 3);

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MEMBERSHIP_NOT_PAUSABLE"));
        assertThat(fixture.endDate(membershipId)).isEqualTo(endDate);
        assertThat(fixture.countPauses(membershipId)).isZero();
        assertThat(fixture.status(membershipId)).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("정지 일수가 0이거나 시작일이 없으면 400 COMMON_INVALID_INPUT이고 정지 저장이 없다")
    void invalidRequest() throws Exception {
        // given
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(20);
        long membershipId =
                fixture.insertPeriodMembership(memberId, BRANCH_ID, "ACTIVE", today, today.plusMonths(3), 3);
        Map<String, Object> noStartDate = new HashMap<>();
        noStartDate.put("startDate", null);
        noStartDate.put("days", 3);

        // when
        ResultActions zeroDays = pause(membershipId, today.plusDays(1), 0);
        ResultActions missingStart = perform(membershipId, noStartDate);

        // then
        zeroDays.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"));
        missingStart
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"));
        assertThat(fixture.countPauses(membershipId)).isZero();
    }

    @Test
    @DisplayName("X-Branch-Id 헤더 없이 해제하면 400이고 해제일이 그대로다")
    void releaseWithoutBranchHeader() throws Exception {
        // given
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(21);
        long membershipId =
                fixture.insertPeriodMembership(memberId, BRANCH_ID, "ACTIVE", today, today.plusMonths(3), 3);
        long pauseId = fixture.insertPause(membershipId, today.plusDays(1), today.plusDays(3), null);

        // when
        ResultActions result = mockMvc.perform(
                post("/api/memberships/{membershipId}/pauses/{pauseId}/release", membershipId, pauseId));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"));
        assertThat(fixture.releasedDate(pauseId)).isNull();
    }

    @Test
    @DisplayName("[TC-4-19] 오늘 출입한 회원권의 오늘 시작 정지는 409 PAUSE_START_DATE_USED이고 종료일 · 정지 건수가 그대로다")
    void pauseTodayAfterEntry() throws Exception {
        // given — 차감 없는 기간제 출입 1건
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(22);
        LocalDate endDate = today.plusMonths(3);
        long membershipId = fixture.insertPeriodMembership(memberId, BRANCH_ID, "ACTIVE", today, endDate, 3);
        fixture.insertAttendance(memberId, membershipId, BRANCH_ID, today.atTime(9, 0), false);

        // when
        ResultActions result = pause(membershipId, today, 3);

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PAUSE_START_DATE_USED"));
        assertThat(fixture.endDate(membershipId)).isEqualTo(endDate);
        assertThat(fixture.countPauses(membershipId)).isZero();
        assertThat(fixture.status(membershipId)).isEqualTo("ACTIVE");
        assertThat(fixture.countHistories(membershipId, "PAUSED")).isZero();
    }

    @Test
    @DisplayName("[TC-4-19] 오늘 출입한 회원권도 내일 시작 정지는 200이고 종료일이 일수만큼 늘어난다")
    void pauseTomorrowAfterEntry() throws Exception {
        // given — 차감된 횟수제 출입 1건
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(23);
        LocalDate endDate = today.plusMonths(6);
        long membershipId = fixture.insertCountMembership(memberId, BRANCH_ID, "ACTIVE", today, endDate, 9);
        fixture.insertAttendance(memberId, membershipId, BRANCH_ID, today.atTime(9, 0), true);

        // when
        ResultActions result = pause(membershipId, today.plusDays(1), 3);

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value(today.plusDays(1).toString()))
                .andExpect(jsonPath("$.membershipEndDate")
                        .value(endDate.plusDays(3).toString()));
        assertThat(fixture.endDate(membershipId)).isEqualTo(endDate.plusDays(3));
        assertThat(fixture.countPauses(membershipId)).isEqualTo(1);
        assertThat(fixture.status(membershipId)).isEqualTo("ACTIVE");
    }

    @ParameterizedTest(name = "시작일 {0}")
    @ValueSource(strings = {"+10000-01-01", "+999999999-12-31"})
    @DisplayName("정지 시작일이 9999-12-31을 넘으면 500이 아니라 400 MEMBERSHIP_INVALID_INPUT이고 정지 저장이 없다 (상시 결정 — 입력 범위)")
    void startDateBeyondMax(String startDate) throws Exception {
        // given
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(24);
        LocalDate endDate = today.plusMonths(3);
        long membershipId = fixture.insertPeriodMembership(memberId, BRANCH_ID, "ACTIVE", today, endDate, 3);

        // when
        ResultActions result = perform(membershipId, Map.of("startDate", startDate, "days", 3));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MEMBERSHIP_INVALID_INPUT"));
        assertThat(fixture.endDate(membershipId)).isEqualTo(endDate);
        assertThat(fixture.countPauses(membershipId)).isZero();
        assertThat(fixture.countHistories(membershipId, "PAUSED")).isZero();
    }

    @Test
    @DisplayName("[TC-4-10] 만료 상태 회원권의 미래 정지를 해제하면 409 PAUSE_NOT_RELEASABLE이고 종료일 · 해제일 · 상태가 그대로다")
    void releaseOnExpiredMembership() throws Exception {
        // given — 예약 정지 3일로 종료일이 이미 3일 늘어난 EXPIRED 회원권
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(25);
        LocalDate endDate = today.plusMonths(1).plusDays(3);
        long membershipId = fixture.insertPeriodMembership(memberId, BRANCH_ID, "EXPIRED", today, endDate, 1);
        long pauseId = fixture.insertPause(membershipId, today.plusDays(2), today.plusDays(4), null);

        // when
        ResultActions result = release(membershipId, pauseId);

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PAUSE_NOT_RELEASABLE"));
        assertThat(fixture.endDate(membershipId)).isEqualTo(endDate);
        assertThat(fixture.releasedDate(pauseId)).isNull();
        assertThat(fixture.countPauses(membershipId)).isEqualTo(1);
        assertThat(fixture.status(membershipId)).isEqualTo("EXPIRED");
        assertThat(fixture.countHistories(membershipId, "RESUMED")).isZero();
    }

    private ResultActions pause(long membershipId, LocalDate startDate, int days) throws Exception {
        return perform(membershipId, Map.of("startDate", startDate.toString(), "days", days));
    }

    private ResultActions perform(long membershipId, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/memberships/{membershipId}/pauses", membershipId)
                .header(BranchIdArgumentResolver.HEADER, BRANCH_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions release(long membershipId, long pauseId) throws Exception {
        return mockMvc.perform(post("/api/memberships/{membershipId}/pauses/{pauseId}/release", membershipId, pauseId)
                .header(BranchIdArgumentResolver.HEADER, BRANCH_ID));
    }

    private long pauseIdOf(ResultActions result) throws Exception {
        String body =
                result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("pauseId").asLong();
    }

    private int countAllHistories() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM membership_history", Integer.class);
    }
}
