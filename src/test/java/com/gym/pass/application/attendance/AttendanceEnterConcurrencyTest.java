package com.gym.pass.application.attendance;

import static org.assertj.core.api.Assertions.assertThat;

import com.gym.pass.domain.attendance.exception.AttendanceException;
import com.gym.pass.domain.exception.ErrorCode;
import com.gym.pass.support.ConcurrencyRunner;
import com.gym.pass.support.IntegrationTest;
import com.gym.pass.support.fixture.AttendanceFixture;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 동시 출입 하루 1회 차감 (NFR-2 · NFR-4 · 03 §7). 결과는 DB에서 다시 읽어 확인하고, 만든 데이터는 직접 지운다. */
@IntegrationTest
class AttendanceEnterConcurrencyTest {

    private static final int THREADS = 20;
    private static final long BRANCH_ID = 950_201L;

    @Autowired
    private AttendanceApplicationService attendanceApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    private AttendanceFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new AttendanceFixture(jdbcTemplate, clock, "010-9502-");
        fixture.createBranch(BRANCH_ID);
    }

    @AfterEach
    void tearDown() {
        fixture.cleanUp(BRANCH_ID);
    }

    @Test
    @DisplayName("[TC-3-07] 같은 회원의 출입 요청이 동시에 들어오면 전부 성공하고 차감은 정확히 1회 · 기록은 요청 수만큼이다")
    void concurrentEntrySameMember() throws InterruptedException {
        // given
        long memberId = fixture.createMember(1);
        LocalDate today = fixture.today();
        long membershipId =
                fixture.insertCountMembership(memberId, BRANCH_ID, "ACTIVE", today, today.plusMonths(6), 10);

        // when
        List<ConcurrencyRunner.Result<AttendanceInfo.Entered>> results = ConcurrencyRunner.run(
                THREADS, index -> attendanceApplicationService.enter(new AttendanceCommand.Enter(memberId, BRANCH_ID)));

        // then
        assertThat(ConcurrencyRunner.errors(results)).isEmpty();
        assertThat(results).extracting(result -> result.value().membershipId()).containsOnly(membershipId);
        assertThat(fixture.remainingCount(membershipId)).isEqualTo(9);
        assertThat(fixture.status(membershipId)).isEqualTo("ACTIVE");
        assertThat(fixture.countAttendances(memberId)).isEqualTo(THREADS);
        assertThat(fixture.countDeductedAttendances(membershipId)).isEqualTo(1);
        assertThat(fixture.countDeductedHistories(membershipId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(DISTINCT entry_date) FROM attendance_record WHERE membership_id = ?",
                        Integer.class,
                        membershipId))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("[TC-3-07] 잔여 1회에서 동시 출입하면 1건만 성공하고 잔여는 0 · 차감 1회로 음수가 되지 않는다")
    void concurrentEntryLastCount() throws InterruptedException {
        // given
        long memberId = fixture.createMember(2);
        LocalDate today = fixture.today();
        long membershipId = fixture.insertCountMembership(memberId, BRANCH_ID, "ACTIVE", today, today.plusMonths(6), 1);

        // when
        List<ConcurrencyRunner.Result<AttendanceInfo.Entered>> results = ConcurrencyRunner.run(
                THREADS, index -> attendanceApplicationService.enter(new AttendanceCommand.Enter(memberId, BRANCH_ID)));

        // then
        assertThat(ConcurrencyRunner.successCount(results)).isEqualTo(1);
        assertThat(ConcurrencyRunner.errors(results))
                .hasSize(THREADS - 1)
                .allSatisfy(error -> assertThat(error)
                        .isInstanceOf(AttendanceException.class)
                        .extracting("errorCode")
                        .isEqualTo(ErrorCode.ATTENDANCE_NO_VALID_MEMBERSHIP));
        assertThat(fixture.remainingCount(membershipId)).isZero();
        assertThat(fixture.status(membershipId)).isEqualTo("EXPIRED");
        assertThat(fixture.countAttendances(memberId)).isEqualTo(1);
        assertThat(fixture.countDeductedHistories(membershipId)).isEqualTo(1);
    }
}
