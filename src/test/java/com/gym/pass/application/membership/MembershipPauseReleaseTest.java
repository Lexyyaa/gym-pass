package com.gym.pass.application.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gym.pass.application.attendance.AttendanceApplicationService;
import com.gym.pass.application.attendance.AttendanceCommand;
import com.gym.pass.application.attendance.AttendanceInfo;
import com.gym.pass.domain.attendance.exception.AttendanceException;
import com.gym.pass.domain.exception.ErrorCode;
import com.gym.pass.domain.membership.exception.MembershipException;
import com.gym.pass.support.IntegrationTest;
import com.gym.pass.support.fixture.AttendanceFixture;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 정지 조기 해제 · 해제 후 출입 · 횟수 상한 계산 (FR-4.2 ~ FR-4.4). 기준일은 서버 Clock 빈의 오늘이다. */
@IntegrationTest
class MembershipPauseReleaseTest {

    private static final long BRANCH_ID = 950_401L;

    @Autowired
    private MembershipPauseApplicationService membershipPauseApplicationService;

    @Autowired
    private AttendanceApplicationService attendanceApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    private AttendanceFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new AttendanceFixture(jdbcTemplate, clock, "010-9504-");
        fixture.createBranch(BRANCH_ID);
    }

    @AfterEach
    void tearDown() {
        fixture.cleanUp(BRANCH_ID);
    }

    @Test
    @DisplayName("[TC-4-07] 7일 정지를 3일차(오늘)에 해제하면 사용 3일 · 종료일 순연장 +3일 · ACTIVE · RESUMED 이력이다")
    void releaseOnThirdDay() {
        // given — 그저께 시작한 7일 정지, 등록 시 종료일 +7 반영
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(1);
        LocalDate originalEnd = today.plusMonths(3);
        long membershipId = fixture.insertPeriodMembership(
                memberId, BRANCH_ID, "PAUSED", today.minusDays(10), originalEnd.plusDays(7), 3);
        long pauseId = fixture.insertPause(membershipId, today.minusDays(2), today.plusDays(4), null);

        // when
        MembershipPauseInfo.Released released =
                membershipPauseApplicationService.release(release(membershipId, pauseId));

        // then
        assertThat(released.pauseId()).isEqualTo(pauseId);
        assertThat(released.usedDays()).isEqualTo(3);
        assertThat(released.membershipEndDate()).isEqualTo(originalEnd.plusDays(3));
        assertThat(fixture.endDate(membershipId)).isEqualTo(originalEnd.plusDays(3));
        assertThat(fixture.releasedDate(pauseId)).isEqualTo(today);
        assertThat(fixture.status(membershipId)).isEqualTo("ACTIVE");
        Map<String, Object> history = jdbcTemplate.queryForMap(
                "SELECT * FROM membership_history WHERE membership_id = ? AND event_type = 'RESUMED'", membershipId);
        assertThat(history.get("member_id")).isEqualTo(memberId);
        assertThat(history.get("branch_id")).isEqualTo(BRANCH_ID);
        assertThat(history.get("end_date_before").toString())
                .isEqualTo(originalEnd.plusDays(7).toString());
        assertThat(history.get("end_date_after").toString())
                .isEqualTo(originalEnd.plusDays(3).toString());
    }

    @Test
    @DisplayName("[TC-4-12] 진행 중 정지를 오늘 해제하면 상태는 ACTIVE지만 오늘 출입은 409 ATTENDANCE_MEMBERSHIP_PAUSED · 기록 0건이다")
    void entryOnReleaseDay() {
        // given
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(2);
        long membershipId = fixture.insertCountMembership(
                memberId, BRANCH_ID, "PAUSED", today.minusDays(10), today.plusMonths(6), 10);
        long pauseId = fixture.insertPause(membershipId, today.minusDays(1), today.plusDays(5), null);
        membershipPauseApplicationService.release(release(membershipId, pauseId));
        assertThat(fixture.status(membershipId)).isEqualTo("ACTIVE");

        // when / then
        assertThatThrownBy(() -> attendanceApplicationService.enter(new AttendanceCommand.Enter(memberId, BRANCH_ID)))
                .isInstanceOf(AttendanceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ATTENDANCE_MEMBERSHIP_PAUSED);
        assertThat(fixture.countAttendances(memberId)).isZero();
        assertThat(fixture.remainingCount(membershipId)).isEqualTo(10);
    }

    @Test
    @DisplayName("[TC-4-12] 어제 해제한 정지만 남은 회원권은 오늘(해제 다음 날) 출입이 허용되고 차감된다")
    void entryDayAfterRelease() {
        // given — 그끄저께 시작한 정지를 어제 해제한 상태 (해제 트랜잭션이 남기는 행과 같다)
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(3);
        long membershipId = fixture.insertCountMembership(
                memberId, BRANCH_ID, "ACTIVE", today.minusDays(10), today.plusMonths(6), 10);
        fixture.insertPause(membershipId, today.minusDays(3), today.plusDays(3), today.minusDays(1));

        // when
        AttendanceInfo.Entered entered =
                attendanceApplicationService.enter(new AttendanceCommand.Enter(memberId, BRANCH_ID));

        // then
        assertThat(entered.membershipId()).isEqualTo(membershipId);
        assertThat(fixture.countAttendances(memberId)).isEqualTo(1);
        assertThat(fixture.remainingCount(membershipId)).isEqualTo(9);
    }

    @Test
    @DisplayName("[TC-4-13] 정지 3건 중 1건을 시작 전에 해제하면 네 번째 정지가 등록되고, 다섯 번째는 PAUSE_COUNT_LIMIT_EXCEEDED다")
    void releasedBeforeStartNotCounted() {
        // given — 3개월권(상한 21일)에 3일씩 3건
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(4);
        LocalDate originalEnd = today.plusMonths(3);
        long membershipId = fixture.insertPeriodMembership(memberId, BRANCH_ID, "ACTIVE", today, originalEnd, 3);
        membershipPauseApplicationService.pause(pause(membershipId, today.plusDays(1)));
        long reservedId = membershipPauseApplicationService
                .pause(pause(membershipId, today.plusDays(5)))
                .pauseId();
        membershipPauseApplicationService.pause(pause(membershipId, today.plusDays(9)));
        MembershipPauseInfo.Released released =
                membershipPauseApplicationService.release(release(membershipId, reservedId));
        assertThat(released.usedDays()).isZero();

        // when
        MembershipPauseInfo.Paused fourth =
                membershipPauseApplicationService.pause(pause(membershipId, today.plusDays(13)));

        // then
        assertThat(fourth.startDate()).isEqualTo(today.plusDays(13));
        assertThat(fourth.membershipEndDate()).isEqualTo(originalEnd.plusDays(9));
        assertThat(fixture.countPauses(membershipId)).isEqualTo(4);
        assertThatThrownBy(() -> membershipPauseApplicationService.pause(pause(membershipId, today.plusDays(17))))
                .isInstanceOf(MembershipException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAUSE_COUNT_LIMIT_EXCEEDED);
        assertThat(fixture.countPauses(membershipId)).isEqualTo(4);
        assertThat(fixture.endDate(membershipId)).isEqualTo(originalEnd.plusDays(9));
        assertThat(fixture.countHistories(membershipId, "PAUSED")).isEqualTo(4);
        assertThat(fixture.countHistories(membershipId, "RESUMED")).isEqualTo(1);
    }

    private static MembershipPauseCommand.Pause pause(long membershipId, LocalDate startDate) {
        return new MembershipPauseCommand.Pause(membershipId, BRANCH_ID, startDate, 3);
    }

    private static MembershipPauseCommand.Release release(long membershipId, long pauseId) {
        return new MembershipPauseCommand.Release(membershipId, pauseId, BRANCH_ID);
    }
}
