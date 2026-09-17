package com.gym.pass.domain.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gym.pass.domain.exception.ErrorCode;
import com.gym.pass.domain.membership.exception.MembershipException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MembershipRegisterTest {

    @ParameterizedTest(name = "{0} 시작 1개월 → {1}")
    @CsvSource({"2027-01-31, 2027-02-28", "2028-01-31, 2028-02-29"})
    @DisplayName("[TC-2-03] 1/31 시작 기간제 1개월의 종료일은 평년 2/28 · 윤년 2/29다")
    void periodEndOfMonth(LocalDate startDate, LocalDate expectedEndDate) {
        // given
        MembershipRegistration registration = period(startDate, 1);

        // when
        Membership membership = Membership.register(registration);

        // then
        assertThat(membership.getEndDate()).isEqualTo(expectedEndDate);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(membership.getRemainingCount()).isNull();
        assertThat(membership.getTotalCount()).isNull();
    }

    @Test
    @DisplayName("[TC-2-04] 횟수제 등록의 종료일은 시작일+6개월이고 잔여 횟수 = 등록 횟수다")
    void countEndDate() {
        // given
        LocalDate startDate = LocalDate.of(2026, 9, 17);
        MembershipRegistration registration =
                new MembershipRegistration(1L, 1L, MembershipType.COUNT, startDate, null, 10, 200_000L);

        // when
        Membership membership = Membership.register(registration);

        // then
        assertThat(membership.getEndDate()).isEqualTo(LocalDate.of(2027, 3, 17));
        assertThat(membership.getMonths()).isEqualTo(6);
        assertThat(membership.getTotalCount()).isEqualTo(10);
        assertThat(membership.getRemainingCount()).isEqualTo(10);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    @DisplayName("[TC-2-04] 횟수제 8/31 시작의 종료일은 2/28(월말 보정)이다")
    void countEndDateEndOfMonth() {
        // given
        MembershipRegistration registration =
                new MembershipRegistration(1L, 1L, MembershipType.COUNT, LocalDate.of(2026, 8, 31), null, 5, 0L);

        // when
        Membership membership = Membership.register(registration);

        // then
        assertThat(membership.getEndDate()).isEqualTo(LocalDate.of(2027, 2, 28));
    }

    @Test
    @DisplayName("등록하면 REGISTERED 이력 1건이 종료일 · 잔여 횟수 after 값으로 함께 생성된다")
    void registeredHistory() {
        // given
        MembershipRegistration registration =
                new MembershipRegistration(7L, 3L, MembershipType.COUNT, LocalDate.of(2026, 9, 17), null, 10, 200_000L);

        // when
        Membership membership = Membership.register(registration);

        // then
        assertThat(membership.getHistories()).singleElement().satisfies(history -> {
            assertThat(history.getEventType()).isEqualTo(MembershipEventType.REGISTERED);
            assertThat(history.getMemberId()).isEqualTo(7L);
            assertThat(history.getBranchId()).isEqualTo(3L);
            assertThat(history.getEndDateBefore()).isNull();
            assertThat(history.getEndDateAfter()).isEqualTo(LocalDate.of(2027, 3, 17));
            assertThat(history.getRemainingCountBefore()).isNull();
            assertThat(history.getRemainingCountAfter()).isEqualTo(10);
        });
    }

    @Test
    @DisplayName("[TC-2-13] 기간제인데 개월 수가 없으면 MEMBERSHIP_INVALID_INPUT 예외가 발생한다")
    void periodWithoutMonths() {
        // given
        MembershipRegistration registration = period(LocalDate.of(2026, 9, 17), null);

        // when
        // then
        assertThatThrownBy(() -> Membership.register(registration))
                .isInstanceOf(MembershipException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBERSHIP_INVALID_INPUT);
    }

    @Test
    @DisplayName("[TC-2-13] 횟수제인데 횟수가 없으면 MEMBERSHIP_INVALID_INPUT 예외가 발생한다")
    void countWithoutCount() {
        // given
        MembershipRegistration registration =
                new MembershipRegistration(1L, 1L, MembershipType.COUNT, LocalDate.of(2026, 9, 17), null, null, 0L);

        // when
        // then
        assertThatThrownBy(() -> Membership.register(registration))
                .isInstanceOf(MembershipException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBERSHIP_INVALID_INPUT);
    }

    @Test
    @DisplayName("[TC-2-07] 도메인에서도 0개월 · 음수 금액은 MEMBERSHIP_INVALID_INPUT 예외가 발생한다")
    void invalidNumbers() {
        // given
        MembershipRegistration zeroMonths = period(LocalDate.of(2026, 9, 17), 0);
        MembershipRegistration negativePrice =
                new MembershipRegistration(1L, 1L, MembershipType.PERIOD, LocalDate.of(2026, 9, 17), 1, null, -1L);

        // when
        // then
        assertThatThrownBy(() -> Membership.register(zeroMonths))
                .isInstanceOf(MembershipException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBERSHIP_INVALID_INPUT);
        assertThatThrownBy(() -> Membership.register(negativePrice))
                .isInstanceOf(MembershipException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBERSHIP_INVALID_INPUT);
    }

    @Test
    @DisplayName("[TC-2-14] 기간제 120개월 · 횟수제 1000회는 상한 이내라 등록된다 (경계)")
    void upperBoundAccepted() {
        // given
        LocalDate startDate = LocalDate.of(2026, 9, 17);
        MembershipRegistration period = period(startDate, 120);
        MembershipRegistration countType = count(startDate, 1000);

        // when
        Membership periodMembership = Membership.register(period);
        Membership countMembership = Membership.register(countType);

        // then
        assertThat(periodMembership.getEndDate()).isEqualTo(LocalDate.of(2036, 9, 17));
        assertThat(periodMembership.getMonths()).isEqualTo(120);
        assertThat(countMembership.getRemainingCount()).isEqualTo(1000);
    }

    @Test
    @DisplayName("[TC-2-14] 기간제 121개월 · 횟수제 1001회는 MEMBERSHIP_INVALID_INPUT 예외가 발생한다")
    void upperBoundExceeded() {
        // given
        LocalDate startDate = LocalDate.of(2026, 9, 17);
        MembershipRegistration tooManyMonths = period(startDate, 121);
        MembershipRegistration tooManyCount = count(startDate, 1001);

        // when
        // then
        assertInvalidInput(tooManyMonths);
        assertInvalidInput(tooManyCount);
    }

    @ParameterizedTest(name = "시작일 {0}")
    @CsvSource({"9999-12-31", "+999999999-12-31", "+10000-01-01"})
    @DisplayName("[TC-2-15] 종료일이 9999-12-31을 넘거나 계산할 수 없는 시작일이면 MEMBERSHIP_INVALID_INPUT 예외가 발생한다")
    void endDateOutOfRange(String startDate) {
        // given
        LocalDate parsed = LocalDate.parse(startDate);
        MembershipRegistration period = period(parsed, 1);
        MembershipRegistration countType = count(parsed, 1);

        // when
        // then
        assertInvalidInput(period);
        assertInvalidInput(countType);
    }

    @Test
    @DisplayName("[TC-2-15] 종료일이 정확히 9999-12-31이면 등록된다 (경계)")
    void endDateAtMax() {
        // given
        MembershipRegistration registration = period(LocalDate.of(9999, 10, 31), 2);

        // when
        Membership membership = Membership.register(registration);

        // then
        assertThat(membership.getEndDate()).isEqualTo(LocalDate.of(9999, 12, 31));
    }

    @Test
    @DisplayName("[TC-2-16] 기간제에 count, 횟수제에 months가 오면 MEMBERSHIP_INVALID_INPUT 예외가 발생한다")
    void oppositeTypeValue() {
        // given
        LocalDate startDate = LocalDate.of(2026, 9, 17);
        MembershipRegistration periodWithCount =
                new MembershipRegistration(1L, 1L, MembershipType.PERIOD, startDate, 3, 5, 0L);
        MembershipRegistration countWithMonths =
                new MembershipRegistration(1L, 1L, MembershipType.COUNT, startDate, 3, 5, 0L);

        // when
        // then
        assertInvalidInput(periodWithCount);
        assertInvalidInput(countWithMonths);
    }

    @Test
    @DisplayName("모르는 회원권 종류 문자열은 MEMBERSHIP_INVALID_INPUT 예외가 발생한다")
    void unknownTypeName() {
        // given
        String value = "LIFETIME";

        // when
        // then
        assertThatThrownBy(() -> MembershipType.from(value))
                .isInstanceOf(MembershipException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBERSHIP_INVALID_INPUT);
    }

    private static void assertInvalidInput(MembershipRegistration registration) {
        assertThatThrownBy(() -> Membership.register(registration))
                .isInstanceOf(MembershipException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBERSHIP_INVALID_INPUT);
    }

    private static MembershipRegistration count(LocalDate startDate, Integer count) {
        return new MembershipRegistration(1L, 1L, MembershipType.COUNT, startDate, null, count, 200_000L);
    }

    private static MembershipRegistration period(LocalDate startDate, Integer months) {
        return new MembershipRegistration(1L, 1L, MembershipType.PERIOD, startDate, months, null, 300_000L);
    }
}
