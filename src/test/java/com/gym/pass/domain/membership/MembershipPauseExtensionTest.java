package com.gym.pass.domain.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gym.pass.domain.attendance.exception.AttendanceException;
import com.gym.pass.domain.exception.ErrorCode;
import com.gym.pass.domain.membership.exception.MembershipException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 정지에 따른 종료일 연장 · 조기 해제 되돌림과 해제 후 상태 · 출입 (FR-4.2 ~ FR-4.4 · D-8 · C-24 · C-25). */
class MembershipPauseExtensionTest {

    private static final MembershipLimits LIMITS = new MembershipLimits(120, 1000);
    private static final MembershipPauseLimits PAUSE_LIMITS = new MembershipPauseLimits(3, 7);

    @ParameterizedTest(name = "[{index}] 회원권 {0}+{1}개월(종료 {2}), 정지 {3}부터 {4}일 → 정지 종료 {5}, 회원권 종료 {6}")
    @CsvSource({
        // 월말 클램프로 끝난 2월 종료일(평년) 위에 3월로 넘어가는 정지
        "2027-01-31, 1, 2027-02-28, 2027-02-25, 7, 2027-03-03, 2027-03-07",
        // 연말을 넘는 정지
        "2026-12-01, 1, 2027-01-01, 2026-12-29, 5, 2027-01-02, 2027-01-06",
        // 윤년 2/28 시작 3일 — 정지가 2/29를 포함한다
        "2028-01-28, 1, 2028-02-28, 2028-02-28, 3, 2028-03-01, 2028-03-02",
        // 윤년 월말 클램프(2/29 종료) 위의 정지
        "2028-01-31, 1, 2028-02-29, 2028-02-25, 7, 2028-03-02, 2028-03-07",
        // 1/31 시작 1일 정지 — 평년 2/28 종료가 3/1로
        "2027-01-31, 1, 2027-02-28, 2027-01-31, 1, 2027-01-31, 2027-03-01",
        // 1/31 시작 1일 정지 — 윤년 2/29 종료가 3/1로
        "2028-01-31, 1, 2028-02-29, 2028-01-31, 1, 2028-01-31, 2028-03-01",
        // 평년 2/28 종료일 당일 1일 정지 — 3/1로
        "2027-01-28, 1, 2027-02-28, 2027-02-28, 1, 2027-02-28, 2027-03-01",
        // 윤년 2/28 종료일 당일 1일 정지 — 2/29로
        "2028-01-28, 1, 2028-02-28, 2028-02-28, 1, 2028-02-28, 2028-02-29",
        // 여러 달 · 누적 상한(3개월 × 7일 = 21일) 한 번에
        "2026-11-30, 3, 2027-02-28, 2027-02-10, 21, 2027-03-02, 2027-03-21"
    })
    @DisplayName("[TC-4-02] 월말 · 2월 · 윤년에 걸친 정지는 정지 종료일 = 시작일 + 일수 − 1, 회원권 종료일 = 기존 종료일 + 일수다")
    void extendAcrossMonthEnd(
            LocalDate membershipStart,
            int months,
            LocalDate expectedMembershipEnd,
            LocalDate pauseStart,
            int days,
            LocalDate expectedPauseEnd,
            LocalDate expectedExtendedEnd) {
        // given
        Membership membership = period(membershipStart, months);
        assertThat(membership.getEndDate()).isEqualTo(expectedMembershipEnd);

        // when
        MembershipPause pause = membership.pause(pauseStart, days, pauseStart, false, PAUSE_LIMITS);

        // then
        assertThat(pause.getEndDate()).isEqualTo(expectedPauseEnd);
        assertThat(pause.plannedDays()).isEqualTo(days);
        assertThat(membership.getEndDate()).isEqualTo(expectedExtendedEnd);
    }

    @ParameterizedTest(name = "[{index}] 3개월권({0} 시작) 정지 {1}부터 {2}일, {3}에 해제 → 사용 {4}일, 회원권 종료 {5}")
    @CsvSource({
        // 윤년: 종료 2028-02-28, 2/27 시작 7일(~3/4)을 2/29에 해제 — 27 · 28 · 29 사용
        "2027-11-28, 2028-02-27, 7, 2028-02-29, 3, 2028-03-02",
        // 평년: 종료 2027-02-28, 2/27 시작 7일(~3/5)을 3/1에 해제 — 27 · 28 · 3/1 사용
        "2026-11-28, 2027-02-27, 7, 2027-03-01, 3, 2027-03-03",
        // 연말을 넘는 정지를 새해 첫날 해제 — 12/30 · 12/31 · 1/1 사용
        "2026-11-28, 2026-12-30, 5, 2027-01-01, 3, 2027-03-03",
        // 시작 당일 해제 — 1일 사용
        "2026-11-28, 2027-02-28, 3, 2027-02-28, 1, 2027-03-01",
        // 시작 전 해제 — 0일 사용, 종료일 원복
        "2026-11-28, 2027-02-28, 3, 2027-02-27, 0, 2027-02-28"
    })
    @DisplayName("[TC-4-02] 월말 · 윤년에 걸친 정지를 조기 해제하면 사용 일수만큼만 연장이 남는다")
    void releaseAcrossMonthEnd(
            LocalDate membershipStart,
            LocalDate pauseStart,
            int days,
            LocalDate releaseDay,
            int expectedUsedDays,
            LocalDate expectedEnd) {
        // given — 정지 시작 전날 등록
        Membership membership = period(membershipStart, 3);
        LocalDate originalEnd = membership.getEndDate();
        MembershipPause pause = membership.pause(pauseStart, days, pauseStart.minusDays(1), false, PAUSE_LIMITS);
        assignId(pause, 1L);

        // when
        MembershipPause released = membership.releasePause(1L, releaseDay);

        // then
        assertThat(released.usedDays()).isEqualTo(expectedUsedDays);
        assertThat(released.getReleasedDate()).isEqualTo(releaseDay);
        assertThat(membership.getEndDate()).isEqualTo(expectedEnd);
        assertThat(membership.getEndDate()).isEqualTo(originalEnd.plusDays(expectedUsedDays));
    }

    @Test
    @DisplayName("[TC-4-07] 7일 정지를 3일차에 조기 해제하면 사용 3일이고 종료일은 등록 전보다 +3일, RESUMED 이력이 남는다")
    void releaseOnThirdDay() {
        // given
        LocalDate start = LocalDate.of(2026, 9, 17);
        Membership membership = period(start, 3);
        LocalDate originalEnd = membership.getEndDate();
        MembershipPause pause = membership.pause(start, 7, start, false, PAUSE_LIMITS);
        assignId(pause, 1L);
        LocalDate thirdDay = start.plusDays(2);

        // when
        MembershipPause released = membership.releasePause(1L, thirdDay);

        // then
        assertThat(released.usedDays()).isEqualTo(3);
        assertThat(membership.getEndDate()).isEqualTo(originalEnd.plusDays(3));
        assertThat(membership.getHistories()).hasSize(3).last().satisfies(history -> {
            assertThat(history.getEventType()).isEqualTo(MembershipEventType.RESUMED);
            assertThat(history.getEndDateBefore()).isEqualTo(originalEnd.plusDays(7));
            assertThat(history.getEndDateAfter()).isEqualTo(originalEnd.plusDays(3));
            assertThat(history.getMemberId()).isEqualTo(7L);
            assertThat(history.getBranchId()).isEqualTo(3L);
        });
    }

    @Test
    @DisplayName("[TC-4-12] 진행 중 정지를 해제한 당일은 즉시 ACTIVE여도 출입이 거부되고, 다음 날은 허용된다")
    void entryOnReleaseDayAndNextDay() {
        // given
        LocalDate start = LocalDate.of(2026, 9, 17);
        Membership membership = count(start, 10);
        assignId(membership.pause(start, 7, start, false, PAUSE_LIMITS), 1L);
        LocalDate releaseDay = start.plusDays(2);

        // when
        membership.releasePause(1L, releaseDay);

        // then
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThatThrownBy(() -> membership.validateEntry(releaseDay, false))
                .isInstanceOf(AttendanceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ATTENDANCE_MEMBERSHIP_PAUSED);
        membership.validateEntry(releaseDay.plusDays(1), false);
        assertThat(membership.deduct(releaseDay.plusDays(1))).isTrue();
        assertThat(membership.getRemainingCount()).isEqualTo(9);
    }

    @Test
    @DisplayName("진행 중 정지가 있는 PAUSED 회원권에서 미래 예약 정지를 해제하면 PAUSED가 유지되고 예정 일수만큼 종료일이 되돌아간다")
    void releaseFutureWhilePaused() {
        // given
        LocalDate today = LocalDate.of(2026, 9, 17);
        Membership membership = period(today, 3);
        LocalDate originalEnd = membership.getEndDate();
        assignId(membership.pause(today, 3, today, false, PAUSE_LIMITS), 1L);
        assignId(membership.pause(today.plusDays(10), 4, today, false, PAUSE_LIMITS), 2L);

        // when
        MembershipPause released = membership.releasePause(2L, today.plusDays(1));

        // then
        assertThat(released.usedDays()).isZero();
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.PAUSED);
        assertThat(membership.getEndDate()).isEqualTo(originalEnd.plusDays(3));
    }

    @Test
    @DisplayName("ACTIVE 회원권에서 예약 정지를 시작 전에 해제하면 ACTIVE가 유지되고 종료일이 원래대로 돌아간다")
    void releaseReservedWhileActive() {
        // given
        LocalDate today = LocalDate.of(2026, 9, 17);
        Membership membership = period(today, 3);
        LocalDate originalEnd = membership.getEndDate();
        assignId(membership.pause(today.plusDays(5), 4, today, false, PAUSE_LIMITS), 1L);

        // when
        MembershipPause released = membership.releasePause(1L, today);

        // then
        assertThat(released.usedDays()).isZero();
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(membership.getEndDate()).isEqualTo(originalEnd);
        membership.validateEntry(today.plusDays(5), false);
    }

    @Test
    @DisplayName("[TC-4-13] 정지 3건 중 1건을 시작 전에 해제하면 네 번째 정지가 등록되고, 다섯 번째는 PAUSE_COUNT_LIMIT_EXCEEDED다")
    void releasedBeforeStartNotCounted() {
        // given — 3개월권(상한 21일), 3일씩 3건
        LocalDate today = LocalDate.of(2026, 9, 17);
        Membership membership = period(today, 3);
        assignId(membership.pause(today.plusDays(1), 3, today, false, PAUSE_LIMITS), 1L);
        assignId(membership.pause(today.plusDays(5), 3, today, false, PAUSE_LIMITS), 2L);
        assignId(membership.pause(today.plusDays(9), 3, today, false, PAUSE_LIMITS), 3L);
        membership.releasePause(2L, today);

        // when
        MembershipPause fourth = membership.pause(today.plusDays(13), 3, today, false, PAUSE_LIMITS);

        // then
        assertThat(fourth.getStartDate()).isEqualTo(today.plusDays(13));
        assertThat(membership.getPauses()).hasSize(4);
        assertThatThrownBy(() -> membership.pause(today.plusDays(17), 3, today, false, PAUSE_LIMITS))
                .isInstanceOf(MembershipException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAUSE_COUNT_LIMIT_EXCEEDED);
        assertThat(membership.getPauses()).hasSize(4);
    }

    private static Membership period(LocalDate start, int months) {
        return Membership.register(
                new MembershipRegistration(7L, 3L, MembershipType.PERIOD, start, months, null, 100_000L), LIMITS);
    }

    private static Membership count(LocalDate start, int count) {
        return Membership.register(
                new MembershipRegistration(7L, 3L, MembershipType.COUNT, start, null, count, 100_000L), LIMITS);
    }

    private static void assignId(MembershipPause pause, long id) {
        MembershipPauseTest.assignId(pause, id);
    }
}
