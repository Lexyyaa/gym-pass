package com.gym.pass.application.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gym.pass.domain.attendance.exception.AttendanceException;
import com.gym.pass.domain.exception.ErrorCode;
import com.gym.pass.support.IntegrationTest;
import com.gym.pass.support.fixture.AttendanceFixture;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 출입 판정 · 차감 경계 (FR-3.2 ~ FR-3.4). 기준일은 서버 Clock 빈의 오늘이다. */
@IntegrationTest
class AttendanceEnterTest {

    private static final long BRANCH_ID = 950_101L;

    @Autowired
    private AttendanceApplicationService attendanceApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    private AttendanceFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new AttendanceFixture(jdbcTemplate, clock, "010-9501-");
        fixture.createBranch(BRANCH_ID);
    }

    @AfterEach
    void tearDown() {
        fixture.cleanUp(BRANCH_ID);
    }

    @Test
    @DisplayName("[TC-3-05] 종료일 당일 출입은 허용된다 (경계: 당일)")
    void entryOnEndDate() {
        // given
        long memberId = fixture.createMember(1);
        LocalDate today = fixture.today();
        long membershipId =
                fixture.insertPeriodMembership(memberId, BRANCH_ID, "ACTIVE", today.minusMonths(1), today, 1);

        // when
        AttendanceInfo.Entered entered = attendanceApplicationService.enter(command(memberId));

        // then
        assertThat(entered.membershipId()).isEqualTo(membershipId);
        assertThat(entered.attendedAt().toLocalDate()).isEqualTo(today);
        assertThat(fixture.countAttendances(memberId)).isEqualTo(1);
    }

    @Test
    @DisplayName("[TC-3-05] 종료일 다음 날 출입은 저장 상태가 ACTIVE여도 거부되고 기록이 없다 (경계: 다음 날)")
    void entryAfterEndDate() {
        // given
        long memberId = fixture.createMember(2);
        LocalDate today = fixture.today();
        fixture.insertCountMembership(memberId, BRANCH_ID, "ACTIVE", today.minusMonths(6), today.minusDays(1), 5);

        // when / then
        assertThatThrownBy(() -> attendanceApplicationService.enter(command(memberId)))
                .isInstanceOf(AttendanceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ATTENDANCE_NO_VALID_MEMBERSHIP);
        assertThat(fixture.countAttendances(memberId)).isZero();
    }

    @Test
    @DisplayName("시작일 전날 출입은 거부되고 기록 · 차감이 없다 (경계: 시작일 > 오늘)")
    void entryBeforeStartDate() {
        // given
        long memberId = fixture.createMember(3);
        LocalDate today = fixture.today();
        long membershipId = fixture.insertCountMembership(
                memberId,
                BRANCH_ID,
                "ACTIVE",
                today.plusDays(1),
                today.plusDays(1).plusMonths(6),
                5);

        // when / then
        assertThatThrownBy(() -> attendanceApplicationService.enter(command(memberId)))
                .isInstanceOf(AttendanceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ATTENDANCE_NO_VALID_MEMBERSHIP);
        assertThat(fixture.countAttendances(memberId)).isZero();
        assertThat(fixture.remainingCount(membershipId)).isEqualTo(5);
    }

    @Test
    @DisplayName("[TC-3-06] 잔여 1회로 출입하면 허용되고 잔여 0 · EXPIRED가 되며, 같은 날 재출입은 거부된다")
    void lastCountThenReEntry() {
        // given
        long memberId = fixture.createMember(4);
        LocalDate today = fixture.today();
        long membershipId = fixture.insertCountMembership(memberId, BRANCH_ID, "ACTIVE", today, today.plusMonths(6), 1);

        // when
        AttendanceInfo.Entered first = attendanceApplicationService.enter(command(memberId));

        // then
        assertThat(first.membershipId()).isEqualTo(membershipId);
        assertThat(fixture.remainingCount(membershipId)).isZero();
        assertThat(fixture.status(membershipId)).isEqualTo("EXPIRED");
        assertThat(fixture.countDeductedAttendances(membershipId)).isEqualTo(1);
        assertThat(fixture.countDeductedHistories(membershipId)).isEqualTo(1);

        // when / then
        assertThatThrownBy(() -> attendanceApplicationService.enter(command(memberId)))
                .isInstanceOf(AttendanceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ATTENDANCE_NO_VALID_MEMBERSHIP);
        assertThat(fixture.countAttendances(memberId)).isEqualTo(1);
        assertThat(fixture.remainingCount(membershipId)).isZero();
        assertThat(fixture.countDeductedHistories(membershipId)).isEqualTo(1);
    }

    @Test
    @DisplayName("[TC-3-06] 저장 상태가 ACTIVE여도 잔여 0이면 거부되고 기록이 없다")
    void zeroRemainingActive() {
        // given
        long memberId = fixture.createMember(5);
        LocalDate today = fixture.today();
        long membershipId = fixture.insertCountMembership(memberId, BRANCH_ID, "ACTIVE", today, today.plusMonths(6), 0);

        // when / then
        assertThatThrownBy(() -> attendanceApplicationService.enter(command(memberId)))
                .isInstanceOf(AttendanceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ATTENDANCE_NO_VALID_MEMBERSHIP);
        assertThat(fixture.countAttendances(memberId)).isZero();
        assertThat(fixture.remainingCount(membershipId)).isZero();
    }

    @Test
    @DisplayName("오늘 이미 차감된 출입이 있으면 차감 없이 기록만 남는다 (오늘 첫 출입 판정)")
    void alreadyDeductedToday() {
        // given
        long memberId = fixture.createMember(6);
        LocalDate today = fixture.today();
        long membershipId = fixture.insertCountMembership(memberId, BRANCH_ID, "ACTIVE", today, today.plusMonths(6), 7);
        fixture.insertAttendance(memberId, membershipId, BRANCH_ID, today.atTime(LocalTime.MIDNIGHT), true);

        // when
        attendanceApplicationService.enter(command(memberId));

        // then
        assertThat(fixture.countAttendances(memberId)).isEqualTo(2);
        assertThat(fixture.countDeductedAttendances(membershipId)).isEqualTo(1);
        assertThat(fixture.remainingCount(membershipId)).isEqualTo(7);
        assertThat(fixture.countDeductedHistories(membershipId)).isZero();
    }

    @Test
    @DisplayName("어제 차감된 출입만 있으면 오늘 첫 출입은 다시 차감된다 (경계: 전날 23:59:59)")
    void deductedYesterdayOnly() {
        // given
        long memberId = fixture.createMember(7);
        LocalDate today = fixture.today();
        long membershipId = fixture.insertCountMembership(
                memberId, BRANCH_ID, "ACTIVE", today.minusDays(1), today.plusMonths(6), 7);
        fixture.insertAttendance(
                memberId, membershipId, BRANCH_ID, today.minusDays(1).atTime(23, 59, 59), true);

        // when
        attendanceApplicationService.enter(command(memberId));

        // then
        assertThat(fixture.countAttendances(memberId)).isEqualTo(2);
        assertThat(fixture.countDeductedAttendances(membershipId)).isEqualTo(2);
        assertThat(fixture.remainingCount(membershipId)).isEqualTo(6);
        assertThat(fixture.countDeductedHistories(membershipId)).isEqualTo(1);
    }

    private static AttendanceCommand.Enter command(long memberId) {
        return new AttendanceCommand.Enter(memberId, BRANCH_ID);
    }
}
