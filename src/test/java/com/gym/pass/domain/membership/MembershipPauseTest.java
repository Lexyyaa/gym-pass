package com.gym.pass.domain.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gym.pass.domain.attendance.exception.AttendanceException;
import com.gym.pass.domain.exception.ErrorCode;
import com.gym.pass.domain.membership.exception.MembershipException;
import java.time.LocalDate;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

/** Membership의 정지 등록 · 해제 규칙과 정지 중 출입 판정 (FR-4.1 ~ FR-4.4 · 03 §3.3 · §4). */
class MembershipPauseTest {

    private static final MembershipLimits LIMITS = new MembershipLimits(120, 1000);
    private static final MembershipPauseLimits PAUSE_LIMITS = new MembershipPauseLimits(3, 7);
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 17);

    @Test
    @DisplayName("[TC-4-01] 미래 시작 정지를 등록하면 종료일이 일수만큼 늘고 ACTIVE가 유지되며 PAUSED 이력이 남는다")
    void pauseFuture() {
        // given
        Membership membership = period(3);
        LocalDate endDate = membership.getEndDate();

        // when
        MembershipPause pause = membership.pause(TODAY.plusDays(3), 7, TODAY, false, PAUSE_LIMITS);

        // then
        assertThat(pause.getStartDate()).isEqualTo(TODAY.plusDays(3));
        assertThat(pause.getEndDate()).isEqualTo(TODAY.plusDays(9));
        assertThat(pause.plannedDays()).isEqualTo(7);
        assertThat(pause.getReleasedDate()).isNull();
        assertThat(membership.getPauses()).containsExactly(pause);
        assertThat(membership.getEndDate()).isEqualTo(endDate.plusDays(7));
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(membership.getHistories()).hasSize(2).last().satisfies(history -> {
            assertThat(history.getEventType()).isEqualTo(MembershipEventType.PAUSED);
            assertThat(history.getEndDateBefore()).isEqualTo(endDate);
            assertThat(history.getEndDateAfter()).isEqualTo(endDate.plusDays(7));
            assertThat(history.getRemainingCountBefore()).isNull();
            assertThat(history.getRemainingCountAfter()).isNull();
        });
    }

    @Test
    @DisplayName("시작일이 오늘인 정지를 등록하면 즉시 PAUSED가 된다")
    void pauseToday() {
        // given
        Membership membership = period(3);

        // when
        membership.pause(TODAY, 1, TODAY, false, PAUSE_LIMITS);

        // then
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.PAUSED);
    }

    @Test
    @DisplayName("PAUSED 회원권에도 겹치지 않는 예약 정지를 추가할 수 있고 상태는 PAUSED로 남는다")
    void pauseWhilePaused() {
        // given
        Membership membership = period(3);
        membership.pause(TODAY, 3, TODAY, false, PAUSE_LIMITS);
        LocalDate endDate = membership.getEndDate();

        // when
        membership.pause(TODAY.plusDays(10), 2, TODAY, false, PAUSE_LIMITS);

        // then
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.PAUSED);
        assertThat(membership.getPauses()).hasSize(2);
        assertThat(membership.getEndDate()).isEqualTo(endDate.plusDays(2));
    }

    @Test
    @DisplayName("[TC-4-09] 시작일이 오늘 이전이면 PAUSE_START_DATE_PAST이고 아무것도 바뀌지 않는다")
    void startDatePast() {
        // given
        Membership membership = period(3);
        LocalDate endDate = membership.getEndDate();

        // when / then
        assertMembershipError(
                () -> membership.pause(TODAY.minusDays(1), 3, TODAY, false, PAUSE_LIMITS),
                ErrorCode.PAUSE_START_DATE_PAST);
        assertUnchanged(membership, endDate);
    }

    @Test
    @DisplayName("[TC-4-05] EXPIRED · CANCELED 상태면 MEMBERSHIP_NOT_PAUSABLE이다")
    void terminalStatus() {
        // given
        Membership expired = period(3);
        ReflectionTestUtils.setField(expired, "status", MembershipStatus.EXPIRED);
        Membership canceled = period(3);
        ReflectionTestUtils.setField(canceled, "status", MembershipStatus.CANCELED);

        // when / then
        assertMembershipError(
                () -> expired.pause(TODAY.plusDays(1), 3, TODAY, false, PAUSE_LIMITS),
                ErrorCode.MEMBERSHIP_NOT_PAUSABLE);
        assertMembershipError(
                () -> canceled.pause(TODAY.plusDays(1), 3, TODAY, false, PAUSE_LIMITS),
                ErrorCode.MEMBERSHIP_NOT_PAUSABLE);
        assertUnchanged(expired, expired.getEndDate());
        assertUnchanged(canceled, canceled.getEndDate());
    }

    @Test
    @DisplayName("[TC-4-05] 종료일 다음 날에는 ACTIVE여도 MEMBERSHIP_NOT_PAUSABLE이고, 종료일 당일은 정지할 수 있다 (경계)")
    void endDateBoundary() {
        // given
        Membership afterEnd = period(1);
        LocalDate endDate = afterEnd.getEndDate();
        Membership onEnd = period(1);

        // when
        onEnd.pause(endDate, 1, endDate, false, PAUSE_LIMITS);

        // then
        assertMembershipError(
                () -> afterEnd.pause(endDate.plusDays(1), 1, endDate.plusDays(1), false, PAUSE_LIMITS),
                ErrorCode.MEMBERSHIP_NOT_PAUSABLE);
        assertUnchanged(afterEnd, endDate);
        assertThat(onEnd.getEndDate()).isEqualTo(endDate.plusDays(1));
    }

    @Test
    @DisplayName("[TC-4-17] 횟수제 잔여 0이면 소진 당일에도 MEMBERSHIP_NOT_PAUSABLE이다")
    void exhaustedCount() {
        // given
        Membership membership = count(1);
        membership.deduct(TODAY);
        LocalDate endDate = membership.getEndDate();

        // when / then
        assertMembershipError(
                () -> membership.pause(TODAY.plusDays(1), 3, TODAY, false, PAUSE_LIMITS),
                ErrorCode.MEMBERSHIP_NOT_PAUSABLE);
        assertThat(membership.getPauses()).isEmpty();
        assertThat(membership.getEndDate()).isEqualTo(endDate);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    @DisplayName("횟수제는 잔여가 남아 있으면 기간제와 같은 규칙으로 정지된다 (누적 상한 6개월 × 7일)")
    void countMembershipPause() {
        // given
        Membership membership = count(10);
        LocalDate endDate = membership.getEndDate();

        // when
        membership.pause(TODAY.plusDays(1), 42, TODAY, false, PAUSE_LIMITS);

        // then
        assertThat(membership.getEndDate()).isEqualTo(endDate.plusDays(42));
        assertMembershipError(
                () -> membership.pause(TODAY.plusDays(100), 1, TODAY, false, PAUSE_LIMITS),
                ErrorCode.PAUSE_DAYS_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("[TC-4-04] 정지 3건 뒤 4번째 등록은 PAUSE_COUNT_LIMIT_EXCEEDED이고 아무것도 바뀌지 않는다")
    void countLimit() {
        // given
        Membership membership = period(3);
        membership.pause(TODAY.plusDays(1), 1, TODAY, false, PAUSE_LIMITS);
        membership.pause(TODAY.plusDays(3), 1, TODAY, false, PAUSE_LIMITS);
        membership.pause(TODAY.plusDays(5), 1, TODAY, false, PAUSE_LIMITS);
        LocalDate endDate = membership.getEndDate();

        // when / then
        assertMembershipError(
                () -> membership.pause(TODAY.plusDays(7), 1, TODAY, false, PAUSE_LIMITS),
                ErrorCode.PAUSE_COUNT_LIMIT_EXCEEDED);
        assertThat(membership.getPauses()).hasSize(3);
        assertThat(membership.getEndDate()).isEqualTo(endDate);
        assertThat(membership.getHistories()).hasSize(4);
    }

    @Test
    @DisplayName("[TC-4-04] 진행 중에 조기 해제한 정지(사용 1일 이상)는 횟수에 그대로 센다")
    void releasedAfterStartStillCounts() {
        // given
        Membership membership = period(3);
        MembershipPause first = membership.pause(TODAY, 3, TODAY, false, PAUSE_LIMITS);
        assignId(first, 1L);
        membership.releasePause(1L, TODAY);
        membership.pause(TODAY.plusDays(5), 1, TODAY, false, PAUSE_LIMITS);
        membership.pause(TODAY.plusDays(7), 1, TODAY, false, PAUSE_LIMITS);

        // when / then
        assertMembershipError(
                () -> membership.pause(TODAY.plusDays(9), 1, TODAY, false, PAUSE_LIMITS),
                ErrorCode.PAUSE_COUNT_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("[TC-4-03] 누적 일수가 months × 7일을 넘으면 PAUSE_DAYS_LIMIT_EXCEEDED이고, 같으면 등록된다")
    void daysLimit() {
        // given — 1개월권 상한 7일
        Membership exceeded = period(1);
        exceeded.pause(TODAY.plusDays(1), 5, TODAY, false, PAUSE_LIMITS);
        LocalDate endDate = exceeded.getEndDate();
        Membership boundary = period(1);
        boundary.pause(TODAY.plusDays(1), 5, TODAY, false, PAUSE_LIMITS);

        // when
        boundary.pause(TODAY.plusDays(10), 2, TODAY, false, PAUSE_LIMITS);

        // then
        assertMembershipError(
                () -> exceeded.pause(TODAY.plusDays(10), 3, TODAY, false, PAUSE_LIMITS),
                ErrorCode.PAUSE_DAYS_LIMIT_EXCEEDED);
        assertThat(exceeded.getPauses()).hasSize(1);
        assertThat(exceeded.getEndDate()).isEqualTo(endDate);
        assertThat(boundary.getPauses()).hasSize(2);
    }

    @Test
    @DisplayName("[TC-4-03] 한 번에 상한을 넘는 일수를 요청해도 PAUSE_DAYS_LIMIT_EXCEEDED다")
    void singleRequestOverLimit() {
        // given
        Membership membership = period(1);

        // when / then
        assertMembershipError(
                () -> membership.pause(TODAY.plusDays(1), 8, TODAY, false, PAUSE_LIMITS),
                ErrorCode.PAUSE_DAYS_LIMIT_EXCEEDED);
        assertMembershipError(
                () -> membership.pause(TODAY.plusDays(1), Integer.MAX_VALUE, TODAY, false, PAUSE_LIMITS),
                ErrorCode.PAUSE_DAYS_LIMIT_EXCEEDED);
        assertThat(membership.getPauses()).isEmpty();
    }

    @Test
    @DisplayName("[TC-4-03] 조기 해제한 정지는 사용 일수만 누적 일수에 센다")
    void releasedPauseCountsUsedDays() {
        // given — 1개월권 상한 7일, 7일 정지를 2일차에 해제 → 사용 2일
        Membership membership = period(1);
        MembershipPause first = membership.pause(TODAY, 7, TODAY, false, PAUSE_LIMITS);
        assignId(first, 1L);
        membership.releasePause(1L, TODAY.plusDays(1));

        // when
        membership.pause(TODAY.plusDays(10), 5, TODAY.plusDays(2), false, PAUSE_LIMITS);

        // then
        assertThat(membership.getPauses()).hasSize(2);
        assertMembershipError(
                () -> membership.pause(TODAY.plusDays(20), 1, TODAY.plusDays(2), false, PAUSE_LIMITS),
                ErrorCode.PAUSE_DAYS_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("[TC-4-08] 기존 정지와 하루라도 겹치면 PAUSE_OVERLAPPED이고, 앞뒤로 붙은 기간은 등록된다")
    void overlap() {
        // given — 기존 정지: TODAY+5 ~ TODAY+9
        Membership membership = period(3);
        membership.pause(TODAY.plusDays(5), 5, TODAY, false, PAUSE_LIMITS);
        LocalDate endDate = membership.getEndDate();

        // when / then
        assertMembershipError(
                () -> membership.pause(TODAY.plusDays(9), 2, TODAY, false, PAUSE_LIMITS), ErrorCode.PAUSE_OVERLAPPED);
        assertMembershipError(
                () -> membership.pause(TODAY.plusDays(4), 2, TODAY, false, PAUSE_LIMITS), ErrorCode.PAUSE_OVERLAPPED);
        assertMembershipError(
                () -> membership.pause(TODAY.plusDays(6), 1, TODAY, false, PAUSE_LIMITS), ErrorCode.PAUSE_OVERLAPPED);
        assertThat(membership.getPauses()).hasSize(1);
        assertThat(membership.getEndDate()).isEqualTo(endDate);

        membership.pause(TODAY.plusDays(3), 2, TODAY, false, PAUSE_LIMITS);
        membership.pause(TODAY.plusDays(10), 1, TODAY, false, PAUSE_LIMITS);
        assertThat(membership.getPauses()).hasSize(3);
    }

    @Test
    @DisplayName("[TC-4-08] 조기 해제한 정지는 해제일까지만 겹침에 쓰고, 시작 전 해제한 정지는 겹치지 않는다")
    void overlapUsesEffectiveInterval() {
        // given — 진행 중 정지(TODAY ~ TODAY+6)를 TODAY+1에 해제, 예약 정지(TODAY+10 ~ TODAY+12)를 시작 전에 해제
        Membership membership = period(3);
        MembershipPause ongoing = membership.pause(TODAY, 7, TODAY, false, PAUSE_LIMITS);
        assignId(ongoing, 1L);
        MembershipPause reserved = membership.pause(TODAY.plusDays(10), 3, TODAY, false, PAUSE_LIMITS);
        assignId(reserved, 2L);
        LocalDate releaseDay = TODAY.plusDays(1);
        membership.releasePause(1L, releaseDay);
        membership.releasePause(2L, releaseDay);

        // when / then — 해제 당일은 여전히 정지 구간이라 겹친다
        assertMembershipError(
                () -> membership.pause(releaseDay, 1, releaseDay, false, PAUSE_LIMITS), ErrorCode.PAUSE_OVERLAPPED);
        membership.pause(releaseDay.plusDays(1), 2, releaseDay, false, PAUSE_LIMITS);
        membership.pause(TODAY.plusDays(10), 3, releaseDay, false, PAUSE_LIMITS);
        assertThat(membership.getPauses()).hasSize(4);
    }

    @Test
    @DisplayName("정지 일수가 없거나 1 미만이면 MEMBERSHIP_INVALID_INPUT이다 (도메인 최후 방어선)")
    void invalidDays() {
        // given
        Membership membership = period(3);
        LocalDate endDate = membership.getEndDate();

        // when / then
        assertMembershipError(
                () -> membership.pause(TODAY, null, TODAY, false, PAUSE_LIMITS), ErrorCode.MEMBERSHIP_INVALID_INPUT);
        assertMembershipError(
                () -> membership.pause(TODAY, 0, TODAY, false, PAUSE_LIMITS), ErrorCode.MEMBERSHIP_INVALID_INPUT);
        assertMembershipError(
                () -> membership.pause(null, 3, TODAY, false, PAUSE_LIMITS), ErrorCode.MEMBERSHIP_INVALID_INPUT);
        assertUnchanged(membership, endDate);
    }

    @Test
    @DisplayName("정지 후 종료일이 9999-12-31을 넘으면 MEMBERSHIP_INVALID_INPUT이고, 같으면 등록된다 (경계)")
    void maxDateBoundary() {
        // given — 종료일 9999-12-30인 회원권
        LocalDate max = LocalDate.of(9999, 12, 31);
        Membership overflow = period(1);
        ReflectionTestUtils.setField(overflow, "endDate", max.minusDays(1));
        Membership fits = period(1);
        ReflectionTestUtils.setField(fits, "endDate", max.minusDays(1));
        LocalDate start = max.minusDays(10);

        // when
        fits.pause(start, 1, start, false, PAUSE_LIMITS);

        // then
        assertThat(fits.getEndDate()).isEqualTo(max);
        assertMembershipError(
                () -> overflow.pause(start, 2, start, false, PAUSE_LIMITS), ErrorCode.MEMBERSHIP_INVALID_INPUT);
        assertUnchanged(overflow, max.minusDays(1));
    }

    @Test
    @DisplayName("[TC-4-06] 정지 시작일 · 종료일 당일 출입은 ATTENDANCE_MEMBERSHIP_PAUSED이고, 전날 · 다음 날은 허용된다")
    void entryDuringPause() {
        // given — 정지: TODAY+2 ~ TODAY+4
        Membership membership = count(10);
        membership.pause(TODAY.plusDays(2), 3, TODAY, false, PAUSE_LIMITS);

        // when / then
        membership.validateEntry(TODAY.plusDays(1), false);
        assertPaused(() -> membership.validateEntry(TODAY.plusDays(2), false));
        assertPaused(() -> membership.validateEntry(TODAY.plusDays(4), true));
        membership.validateEntry(TODAY.plusDays(5), false);
    }

    @Test
    @DisplayName("[TC-4-06] 정지 중에는 차감도 거부되고 잔여 · 이력이 바뀌지 않는다")
    void deductDuringPause() {
        // given
        Membership membership = count(10);
        membership.pause(TODAY, 3, TODAY, false, PAUSE_LIMITS);
        int histories = membership.getHistories().size();

        // when / then
        assertPaused(() -> membership.deduct(TODAY));
        assertThat(membership.getRemainingCount()).isEqualTo(10);
        assertThat(membership.getHistories()).hasSize(histories);
    }

    @Test
    @DisplayName("[TC-4-06] 저장 상태가 ACTIVE여도(배치 전) 정지 구간이면 출입이 거부되고, PAUSED여도 구간 밖이면 허용된다")
    void entryJudgedByIntervalNotStatus() {
        // given
        Membership reserved = period(3);
        reserved.pause(TODAY.plusDays(1), 3, TODAY, false, PAUSE_LIMITS);
        Membership stalePaused = period(3);
        stalePaused.pause(TODAY, 1, TODAY, false, PAUSE_LIMITS);

        // when / then
        assertThat(reserved.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertPaused(() -> reserved.validateEntry(TODAY.plusDays(1), false));
        assertThat(stalePaused.getStatus()).isEqualTo(MembershipStatus.PAUSED);
        stalePaused.validateEntry(TODAY.plusDays(1), false);
    }

    @Test
    @DisplayName("[TC-4-14] 이 회원권에 없는 pauseId로 해제하면 PAUSE_NOT_FOUND이고 아무것도 바뀌지 않는다")
    void releaseNotFound() {
        // given
        Membership membership = period(3);
        MembershipPause pause = membership.pause(TODAY, 3, TODAY, false, PAUSE_LIMITS);
        assignId(pause, 1L);
        LocalDate endDate = membership.getEndDate();

        // when / then
        assertMembershipError(() -> membership.releasePause(2L, TODAY), ErrorCode.PAUSE_NOT_FOUND);
        assertThat(pause.getReleasedDate()).isNull();
        assertThat(membership.getEndDate()).isEqualTo(endDate);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.PAUSED);
    }

    @Test
    @DisplayName("[TC-4-10] 이미 해제한 정지 · 종료일이 지난 정지는 PAUSE_NOT_RELEASABLE이고, 종료일 당일은 해제할 수 있다 (경계)")
    void releaseNotReleasable() {
        // given — 정지: TODAY ~ TODAY+2
        Membership released = period(3);
        assignId(released.pause(TODAY, 3, TODAY, false, PAUSE_LIMITS), 1L);
        released.releasePause(1L, TODAY);
        LocalDate releasedEndDate = released.getEndDate();
        Membership ended = period(3);
        assignId(ended.pause(TODAY, 3, TODAY, false, PAUSE_LIMITS), 1L);
        LocalDate endedEndDate = ended.getEndDate();
        Membership lastDay = period(3);
        assignId(lastDay.pause(TODAY, 3, TODAY, false, PAUSE_LIMITS), 1L);
        LocalDate lastDayEndDate = lastDay.getEndDate();

        // when
        MembershipPause lastDayPause = lastDay.releasePause(1L, TODAY.plusDays(2));

        // then
        assertMembershipError(() -> released.releasePause(1L, TODAY), ErrorCode.PAUSE_NOT_RELEASABLE);
        assertThat(released.getEndDate()).isEqualTo(releasedEndDate);
        assertThat(released.getHistories()).hasSize(3);
        assertMembershipError(() -> ended.releasePause(1L, TODAY.plusDays(3)), ErrorCode.PAUSE_NOT_RELEASABLE);
        assertThat(ended.getEndDate()).isEqualTo(endedEndDate);
        assertThat(lastDayPause.usedDays()).isEqualTo(3);
        assertThat(lastDay.getEndDate()).isEqualTo(lastDayEndDate);
    }

    @Test
    @DisplayName("[TC-4-19] 오늘 출입 기록이 있으면 오늘 시작 정지는 PAUSE_START_DATE_USED이고 아무것도 바뀌지 않는다")
    void pauseTodayAfterEntry() {
        // given
        Membership membership = period(3);
        LocalDate endDate = membership.getEndDate();

        // when / then
        assertMembershipError(
                () -> membership.pause(TODAY, 3, TODAY, true, PAUSE_LIMITS), ErrorCode.PAUSE_START_DATE_USED);
        assertUnchanged(membership, endDate);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    @DisplayName("[TC-4-19] 오늘 출입 기록이 있어도 내일 시작 정지는 등록된다 (경계)")
    void pauseTomorrowAfterEntry() {
        // given
        Membership membership = period(3);
        LocalDate endDate = membership.getEndDate();

        // when
        membership.pause(TODAY.plusDays(1), 3, TODAY, true, PAUSE_LIMITS);

        // then
        assertThat(membership.getPauses()).hasSize(1);
        assertThat(membership.getEndDate()).isEqualTo(endDate.plusDays(3));
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    @DisplayName("[TC-4-19] 정지할 수 없는 회원권이면 오늘 출입이 있어도 MEMBERSHIP_NOT_PAUSABLE이 먼저다 (판정 순서)")
    void notPausableBeforeStartDateUsed() {
        // given
        Membership membership = count(1);
        membership.deduct(TODAY);

        // when / then
        assertMembershipError(
                () -> membership.pause(TODAY, 3, TODAY, true, PAUSE_LIMITS), ErrorCode.MEMBERSHIP_NOT_PAUSABLE);
        assertThat(membership.getPauses()).isEmpty();
    }

    @ParameterizedTest(name = "시작일 {0}")
    @ValueSource(strings = {"+10000-01-01", "+999999999-12-31"})
    @DisplayName("[TC-4-09] 정지 시작일이 9999-12-31을 넘으면 날짜 계산 전에 MEMBERSHIP_INVALID_INPUT이고 아무것도 바뀌지 않는다")
    void startDateBeyondMax(String startDate) {
        // given
        Membership membership = period(3);
        LocalDate endDate = membership.getEndDate();
        LocalDate start = LocalDate.parse(startDate);

        // when / then
        assertMembershipError(
                () -> membership.pause(start, 3, TODAY, false, PAUSE_LIMITS), ErrorCode.MEMBERSHIP_INVALID_INPUT);
        assertMembershipError(
                () -> membership.pause(start, Integer.MAX_VALUE, TODAY, false, PAUSE_LIMITS),
                ErrorCode.MEMBERSHIP_INVALID_INPUT);
        assertUnchanged(membership, endDate);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(
            value = MembershipStatus.class,
            names = {"EXPIRED", "CANCELED"})
    @DisplayName("[TC-4-10] 회원권이 EXPIRED · CANCELED면 미래 정지도 PAUSE_NOT_RELEASABLE이고 종료일 · 정지가 그대로다")
    void releaseOnTerminalMembership(MembershipStatus terminal) {
        // given
        Membership membership = period(3);
        MembershipPause pause = membership.pause(TODAY.plusDays(2), 3, TODAY, false, PAUSE_LIMITS);
        assignId(pause, 1L);
        ReflectionTestUtils.setField(membership, "status", terminal);
        LocalDate endDate = membership.getEndDate();

        // when / then
        assertMembershipError(() -> membership.releasePause(1L, TODAY), ErrorCode.PAUSE_NOT_RELEASABLE);
        assertThat(pause.getReleasedDate()).isNull();
        assertThat(membership.getEndDate()).isEqualTo(endDate);
        assertThat(membership.getStatus()).isEqualTo(terminal);
        assertThat(membership.getHistories()).hasSize(2);
    }

    @Test
    @DisplayName("[TC-4-18] 시작일이 회원권 시작일 앞이거나 현재 종료일 뒤면 PAUSE_OUT_OF_PERIOD이고 아무것도 바뀌지 않는다")
    void outOfPeriod() {
        // given — 회원권 기간 TODAY+5 ~ 종료일
        Membership membership = periodFrom(TODAY.plusDays(5), 3);
        LocalDate endDate = membership.getEndDate();

        // when / then
        assertMembershipError(
                () -> membership.pause(TODAY.plusDays(4), 1, TODAY, false, PAUSE_LIMITS),
                ErrorCode.PAUSE_OUT_OF_PERIOD);
        assertMembershipError(
                () -> membership.pause(TODAY, 1, TODAY, false, PAUSE_LIMITS), ErrorCode.PAUSE_OUT_OF_PERIOD);
        assertMembershipError(
                () -> membership.pause(endDate.plusDays(1), 1, TODAY, false, PAUSE_LIMITS),
                ErrorCode.PAUSE_OUT_OF_PERIOD);
        assertUnchanged(membership, endDate);
    }

    @Test
    @DisplayName("[TC-4-18] 시작일 = 회원권 시작일, 시작일 = 현재 종료일은 등록된다 (경계)")
    void periodBoundary() {
        // given
        LocalDate membershipStart = TODAY.plusDays(5);
        Membership onStart = periodFrom(membershipStart, 3);
        LocalDate onStartEndDate = onStart.getEndDate();
        Membership onEnd = periodFrom(membershipStart, 3);
        LocalDate onEndEndDate = onEnd.getEndDate();

        // when
        onStart.pause(membershipStart, 2, TODAY, false, PAUSE_LIMITS);
        onEnd.pause(onEndEndDate, 2, TODAY, false, PAUSE_LIMITS);

        // then
        assertThat(onStart.getPauses()).hasSize(1);
        assertThat(onStart.getEndDate()).isEqualTo(onStartEndDate.plusDays(2));
        assertThat(onEnd.getPauses()).hasSize(1);
        assertThat(onEnd.getPauses().get(0).getEndDate()).isEqualTo(onEndEndDate.plusDays(1));
        assertThat(onEnd.getEndDate()).isEqualTo(onEndEndDate.plusDays(2));
    }

    @Test
    @DisplayName("[TC-4-18] 앞선 정지로 늘어난 종료일까지는 시작일로 허용하고, 그 다음 날은 PAUSE_OUT_OF_PERIOD다")
    void extendedEndDateIsUpperBound() {
        // given — 1개월권에 3일 정지를 걸어 종료일이 3일 늘었다
        Membership membership = period(1);
        LocalDate originalEndDate = membership.getEndDate();
        membership.pause(TODAY.plusDays(1), 3, TODAY, false, PAUSE_LIMITS);
        LocalDate extendedEndDate = membership.getEndDate();

        // when
        membership.pause(originalEndDate.plusDays(3), 1, TODAY, false, PAUSE_LIMITS);

        // then
        assertThat(extendedEndDate).isEqualTo(originalEndDate.plusDays(3));
        assertThat(membership.getPauses()).hasSize(2);
        LocalDate latestEndDate = membership.getEndDate();
        assertMembershipError(
                () -> membership.pause(latestEndDate.plusDays(1), 1, TODAY, false, PAUSE_LIMITS),
                ErrorCode.PAUSE_OUT_OF_PERIOD);
        assertThat(membership.getPauses()).hasSize(2);
        assertThat(membership.getEndDate()).isEqualTo(latestEndDate);
    }

    @Test
    @DisplayName("[TC-4-20] 앞선 정지를 해제하면 뒤의 미해제 정지가 새 종료일 밖에 남는 경우 PAUSE_NOT_RELEASABLE이고 아무것도 바뀌지 않는다")
    void releaseLeavingPauseOutOfPeriod() {
        // given — A(TODAY+1, 3일)로 종료일 +3, 늘어난 종료일에 B(1일) 시작
        Membership membership = period(1);
        LocalDate originalEndDate = membership.getEndDate();
        MembershipPause first = membership.pause(TODAY.plusDays(1), 3, TODAY, false, PAUSE_LIMITS);
        assignId(first, 1L);
        MembershipPause second = membership.pause(originalEndDate.plusDays(3), 1, TODAY, false, PAUSE_LIMITS);
        assignId(second, 2L);
        LocalDate endDate = membership.getEndDate();
        int histories = membership.getHistories().size();

        // when / then
        assertMembershipError(() -> membership.releasePause(1L, TODAY), ErrorCode.PAUSE_NOT_RELEASABLE);
        assertThat(first.getReleasedDate()).isNull();
        assertThat(second.getReleasedDate()).isNull();
        assertThat(membership.getEndDate()).isEqualTo(endDate);
        assertThat(membership.getHistories()).hasSize(histories);
    }

    @Test
    @DisplayName("[TC-4-20] 뒤의 정지를 먼저 해제하면 앞선 정지도 해제되고 종료일이 원래 값으로 돌아온다")
    void releaseLaterPauseFirst() {
        // given
        Membership membership = period(1);
        LocalDate originalEndDate = membership.getEndDate();
        assignId(membership.pause(TODAY.plusDays(1), 3, TODAY, false, PAUSE_LIMITS), 1L);
        assignId(membership.pause(originalEndDate.plusDays(3), 1, TODAY, false, PAUSE_LIMITS), 2L);

        // when
        membership.releasePause(2L, TODAY);
        membership.releasePause(1L, TODAY);

        // then
        assertThat(membership.getEndDate()).isEqualTo(originalEndDate);
        assertThat(membership.getPauses())
                .allSatisfy(pause -> assertThat(pause.getReleasedDate()).isEqualTo(TODAY));
    }

    @Test
    @DisplayName("[TC-4-20] 해제 후 새 종료일에 시작하는 미해제 정지는 기간 안이라 해제되고, 하루 늦으면 거부된다 (경계)")
    void releaseBoundaryOnNewEndDate() {
        // given — A(TODAY+1, 3일) 해제 후 종료일 = originalEndDate + 1(B 1일분)
        Membership inside = period(1);
        LocalDate originalEndDate = inside.getEndDate();
        assignId(inside.pause(TODAY.plusDays(1), 3, TODAY, false, PAUSE_LIMITS), 1L);
        assignId(inside.pause(originalEndDate.plusDays(1), 1, TODAY, false, PAUSE_LIMITS), 2L);
        Membership outside = period(1);
        assignId(outside.pause(TODAY.plusDays(1), 3, TODAY, false, PAUSE_LIMITS), 1L);
        assignId(outside.pause(originalEndDate.plusDays(2), 1, TODAY, false, PAUSE_LIMITS), 2L);
        LocalDate outsideEndDate = outside.getEndDate();

        // when
        inside.releasePause(1L, TODAY);

        // then
        assertThat(inside.getEndDate()).isEqualTo(originalEndDate.plusDays(1));
        assertMembershipError(() -> outside.releasePause(1L, TODAY), ErrorCode.PAUSE_NOT_RELEASABLE);
        assertThat(outside.getEndDate()).isEqualTo(outsideEndDate);
    }

    @Test
    @DisplayName("[TC-4-20] 진행 중 정지를 해제하면 사용 일수를 뺀 새 종료일 기준으로 판단한다")
    void releaseOngoingUsesUsedDays() {
        // given — A(TODAY, 3일) 뒤 B(원래 종료일 + 3, 1일). 종료일 = 원래 + 4
        Membership allowed = period(1);
        LocalDate originalEndDate = allowed.getEndDate();
        assignId(allowed.pause(TODAY, 3, TODAY, false, PAUSE_LIMITS), 1L);
        assignId(allowed.pause(originalEndDate.plusDays(3), 1, TODAY, false, PAUSE_LIMITS), 2L);
        Membership rejected = period(1);
        assignId(rejected.pause(TODAY, 3, TODAY, false, PAUSE_LIMITS), 1L);
        assignId(rejected.pause(originalEndDate.plusDays(3), 1, TODAY, false, PAUSE_LIMITS), 2L);
        LocalDate rejectedEndDate = rejected.getEndDate();

        // when — TODAY+1 해제: 2일 사용 · 1일 되돌림 → 새 종료일 = 원래 + 3 (B 시작일과 같음)
        allowed.releasePause(1L, TODAY.plusDays(1));

        // then — TODAY 해제: 1일 사용 · 2일 되돌림 → 새 종료일 = 원래 + 2 (B 시작일보다 이름)
        assertThat(allowed.getEndDate()).isEqualTo(originalEndDate.plusDays(3));
        assertMembershipError(() -> rejected.releasePause(1L, TODAY), ErrorCode.PAUSE_NOT_RELEASABLE);
        assertThat(rejected.getEndDate()).isEqualTo(rejectedEndDate);
    }

    private static Membership periodFrom(LocalDate startDate, int months) {
        return Membership.register(
                new MembershipRegistration(7L, 3L, MembershipType.PERIOD, startDate, months, null, 100_000L), LIMITS);
    }

    private static Membership period(int months) {
        return Membership.register(
                new MembershipRegistration(7L, 3L, MembershipType.PERIOD, TODAY, months, null, 100_000L), LIMITS);
    }

    private static Membership count(int count) {
        return Membership.register(
                new MembershipRegistration(7L, 3L, MembershipType.COUNT, TODAY, null, count, 100_000L), LIMITS);
    }

    /** 저장 전 정지에는 id가 없어 해제 대상을 고를 수 없다. DB가 채우는 id를 흉내 낸다. */
    static void assignId(MembershipPause pause, long id) {
        ReflectionTestUtils.setField(pause, "id", id);
    }

    private static void assertUnchanged(Membership membership, LocalDate endDate) {
        assertThat(membership.getPauses()).isEmpty();
        assertThat(membership.getEndDate()).isEqualTo(endDate);
        assertThat(membership.getHistories()).hasSize(1);
    }

    private static void assertMembershipError(ThrowingCallable call, ErrorCode errorCode) {
        assertThatThrownBy(call)
                .isInstanceOf(MembershipException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }

    private static void assertPaused(ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(AttendanceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ATTENDANCE_MEMBERSHIP_PAUSED);
    }
}
