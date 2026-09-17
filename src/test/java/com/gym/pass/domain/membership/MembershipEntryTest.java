package com.gym.pass.domain.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gym.pass.domain.attendance.exception.AttendanceException;
import com.gym.pass.domain.branch.exception.BranchException;
import com.gym.pass.domain.exception.ErrorCode;
import java.time.LocalDate;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Membership의 출입 판정 · 차감 행위 (FR-3.2 · FR-3.3 · 03 §3.3 · §4). */
class MembershipEntryTest {

    private static final MembershipLimits LIMITS = new MembershipLimits(120, 1000);
    private static final LocalDate START = LocalDate.of(2026, 9, 17);
    private static final LocalDate END = LocalDate.of(2026, 10, 17);

    @Test
    @DisplayName("[TC-3-05] 기간제는 시작일 · 종료일 당일 출입이 허용되고, 전날 · 다음 날은 거부된다")
    void periodDateBoundary() {
        // given
        Membership membership = period();

        // when / then
        membership.validateEntry(START);
        membership.validateEntry(END);
        assertNoValidMembership(() -> membership.validateEntry(START.minusDays(1)));
        assertNoValidMembership(() -> membership.validateEntry(END.plusDays(1)));
    }

    @Test
    @DisplayName("기간제 차감은 아무것도 바꾸지 않고 false다")
    void periodDeductIsNoop() {
        // given
        Membership membership = period();

        // when
        boolean deducted = membership.deduct(START);

        // then
        assertThat(deducted).isFalse();
        assertThat(membership.getRemainingCount()).isNull();
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(membership.getHistories()).hasSize(1);
    }

    @Test
    @DisplayName("횟수제 차감은 잔여를 1 줄이고 DEDUCTED 이력을 before/after로 남긴다")
    void countDeduct() {
        // given
        Membership membership = count(10);

        // when
        boolean deducted = membership.deduct(START);

        // then
        assertThat(deducted).isTrue();
        assertThat(membership.getRemainingCount()).isEqualTo(9);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(membership.getHistories()).hasSize(2).last().satisfies(history -> {
            assertThat(history.getEventType()).isEqualTo(MembershipEventType.DEDUCTED);
            assertThat(history.getMemberId()).isEqualTo(7L);
            assertThat(history.getBranchId()).isEqualTo(3L);
            assertThat(history.getRemainingCountBefore()).isEqualTo(10);
            assertThat(history.getRemainingCountAfter()).isEqualTo(9);
            assertThat(history.getEndDateBefore()).isNull();
            assertThat(history.getEndDateAfter()).isNull();
        });
    }

    @Test
    @DisplayName("[TC-3-06] 잔여 1회를 차감하면 잔여 0 · EXPIRED가 되고, 이후 판정 · 차감은 거부된다")
    void lastCountExpires() {
        // given
        Membership membership = count(1);

        // when
        boolean deducted = membership.deduct(START);

        // then
        assertThat(deducted).isTrue();
        assertThat(membership.getRemainingCount()).isZero();
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.EXPIRED);
        assertNoValidMembership(() -> membership.validateEntry(START));
        assertNoValidMembership(() -> membership.deduct(START));
        assertThat(membership.getRemainingCount()).isZero();
        assertThat(membership.getHistories()).hasSize(2);
    }

    @Test
    @DisplayName("회원권 지점과 요청 지점이 다르면 BRANCH_FORBIDDEN이다")
    void otherBranch() {
        // given
        Membership membership = period();

        // when / then
        membership.verifyBranch(3L);
        assertThatThrownBy(() -> membership.verifyBranch(4L))
                .isInstanceOf(BranchException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BRANCH_FORBIDDEN);
    }

    private static Membership period() {
        return Membership.register(
                new MembershipRegistration(7L, 3L, MembershipType.PERIOD, START, 1, null, 100_000L), LIMITS);
    }

    private static Membership count(int count) {
        return Membership.register(
                new MembershipRegistration(7L, 3L, MembershipType.COUNT, START, null, count, 100_000L), LIMITS);
    }

    private static void assertNoValidMembership(ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(AttendanceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ATTENDANCE_NO_VALID_MEMBERSHIP);
    }
}
