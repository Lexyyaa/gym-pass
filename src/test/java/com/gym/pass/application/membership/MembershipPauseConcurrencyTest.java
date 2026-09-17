package com.gym.pass.application.membership;

import static org.assertj.core.api.Assertions.assertThat;

import com.gym.pass.domain.exception.BusinessException;
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

/** 같은 회원권의 동시 정지 요청 (FR-4.3 · D-26 · 03 §7). 결과는 DB에서 다시 읽어 확인하고, 만든 데이터는 직접 지운다. */
@IntegrationTest
class MembershipPauseConcurrencyTest {

    private static final int THREADS = 10;
    private static final long BRANCH_ID = 950_501L;

    @Autowired
    private MembershipPauseApplicationService membershipPauseApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    private AttendanceFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new AttendanceFixture(jdbcTemplate, clock, "010-9505-");
        fixture.createBranch(BRANCH_ID);
    }

    @AfterEach
    void tearDown() {
        fixture.cleanUp(BRANCH_ID);
    }

    @Test
    @DisplayName("겹치지 않는 정지 요청이 동시에 들어와도 횟수 상한(3회)만큼만 등록되고 나머지는 PAUSE_COUNT_LIMIT_EXCEEDED다")
    void concurrentPauseCountLimit() throws InterruptedException {
        // given — 12개월권(누적 상한 84일), 1일씩 서로 다른 날짜
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(1);
        LocalDate originalEnd = today.plusMonths(12);
        long membershipId = fixture.insertPeriodMembership(memberId, BRANCH_ID, "ACTIVE", today, originalEnd, 12);

        // when
        List<ConcurrencyRunner.Result<MembershipPauseInfo.Paused>> results = ConcurrencyRunner.run(
                THREADS,
                index -> membershipPauseApplicationService.pause(
                        new MembershipPauseCommand.Pause(membershipId, BRANCH_ID, today.plusDays(1 + 2L * index), 1)));

        // then
        assertThat(ConcurrencyRunner.successCount(results)).isEqualTo(3);
        assertThat(ConcurrencyRunner.errors(results))
                .hasSize(THREADS - 3)
                .allSatisfy(error -> assertThat(error)
                        .isInstanceOf(BusinessException.class)
                        .extracting("errorCode")
                        .isEqualTo(ErrorCode.PAUSE_COUNT_LIMIT_EXCEEDED));
        assertThat(fixture.countPauses(membershipId)).isEqualTo(3);
        assertThat(fixture.endDate(membershipId)).isEqualTo(originalEnd.plusDays(3));
        assertThat(fixture.countHistories(membershipId, "PAUSED")).isEqualTo(3);
    }

    @Test
    @DisplayName("같은 기간의 정지 요청이 동시에 들어오면 1건만 등록되고 나머지는 PAUSE_OVERLAPPED다")
    void concurrentPauseOverlap() throws InterruptedException {
        // given
        LocalDate today = fixture.today();
        long memberId = fixture.createMember(2);
        LocalDate originalEnd = today.plusMonths(12);
        long membershipId = fixture.insertPeriodMembership(memberId, BRANCH_ID, "ACTIVE", today, originalEnd, 12);

        // when
        List<ConcurrencyRunner.Result<MembershipPauseInfo.Paused>> results = ConcurrencyRunner.run(
                THREADS,
                index -> membershipPauseApplicationService.pause(
                        new MembershipPauseCommand.Pause(membershipId, BRANCH_ID, today.plusDays(1), 5)));

        // then
        assertThat(ConcurrencyRunner.successCount(results)).isEqualTo(1);
        assertThat(ConcurrencyRunner.errors(results))
                .hasSize(THREADS - 1)
                .allSatisfy(error -> assertThat(error)
                        .isInstanceOf(BusinessException.class)
                        .extracting("errorCode")
                        .isEqualTo(ErrorCode.PAUSE_OVERLAPPED));
        assertThat(fixture.countPauses(membershipId)).isEqualTo(1);
        assertThat(fixture.endDate(membershipId)).isEqualTo(originalEnd.plusDays(5));
    }
}
