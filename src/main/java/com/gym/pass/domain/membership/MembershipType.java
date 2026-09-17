package com.gym.pass.domain.membership;

import com.gym.pass.domain.exception.ErrorCode;
import com.gym.pass.domain.membership.exception.MembershipException;
import java.time.LocalDate;

/**
 * 회원권 종류와 종류별 정책 (FR-2.4 · 03 §2 · §5).
 * 새 종류 추가 = enum 값과 정책 메서드 구현 추가. 출입 · 안내 · 조회 로직은 바뀌지 않는다.
 */
public enum MembershipType {

    /** 기간제. 종료일 = 시작일 + 개월 (D-6), 차감 없음. */
    PERIOD {
        @Override
        public int validityMonths(Integer months) {
            return months;
        }

        @Override
        public Integer initialCount(Integer count) {
            return null;
        }

        @Override
        public boolean deductible() {
            return false;
        }

        @Override
        void validate(Integer months, Integer count, MembershipLimits limits) {
            requireInRange(months, limits.maxMonths(), "기간제는 개월 수(months)가 1 이상 " + limits.maxMonths() + " 이하여야 합니다.");
            requireAbsent(count, "기간제에는 이용 횟수(count)를 보낼 수 없습니다.");
        }
    },

    /** 횟수제. 종료일 = 시작일 + 6개월 (D-7), 출입 시 차감. */
    COUNT {
        @Override
        public int validityMonths(Integer months) {
            return COUNT_VALIDITY_MONTHS;
        }

        @Override
        public Integer initialCount(Integer count) {
            return count;
        }

        @Override
        public boolean deductible() {
            return true;
        }

        @Override
        void validate(Integer months, Integer count, MembershipLimits limits) {
            requireInRange(count, limits.maxCount(), "횟수제는 이용 횟수(count)가 1 이상 " + limits.maxCount() + " 이하여야 합니다.");
            requireAbsent(months, "횟수제에는 개월 수(months)를 보낼 수 없습니다.");
        }
    };

    /** 횟수제 유효기간 (D-7 가정 · C-32 enum 상수 유지). */
    private static final int COUNT_VALIDITY_MONTHS = 6;

    /** 요청 문자열을 종류로 바꾼다. 모르는 값은 MEMBERSHIP_INVALID_INPUT. */
    public static MembershipType from(String value) {
        for (MembershipType type : values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }
        throw new MembershipException(ErrorCode.MEMBERSHIP_INVALID_INPUT, "회원권 종류는 PERIOD 또는 COUNT여야 합니다.");
    }

    /** 이 회원권의 개월 수. 기간제 = 요청 개월, 횟수제 = 6 (D-24). */
    public abstract int validityMonths(Integer months);

    /** 종료일(당일 포함)을 계산한다. 시작일 + 개월 수. */
    public LocalDate calculateEndDate(LocalDate startDate, Integer months) {
        return startDate.plusMonths(validityMonths(months));
    }

    /** 등록 시 잔여 횟수. 차감이 없는 종류는 null. */
    public abstract Integer initialCount(Integer count);

    /** 출입 시 횟수 차감 대상인지. */
    public abstract boolean deductible();

    /** 종류별 필수 값 · 상한 · 반대 종류 값 검증. 상한은 설정값이다 (C-34). 위반 시 MEMBERSHIP_INVALID_INPUT (D-22 · D-23). */
    abstract void validate(Integer months, Integer count, MembershipLimits limits);

    private static void requireInRange(Integer value, int max, String detail) {
        if (value == null || value < 1 || value > max) {
            throw new MembershipException(ErrorCode.MEMBERSHIP_INVALID_INPUT, detail);
        }
    }

    private static void requireAbsent(Integer value, String detail) {
        if (value != null) {
            throw new MembershipException(ErrorCode.MEMBERSHIP_INVALID_INPUT, detail);
        }
    }
}
