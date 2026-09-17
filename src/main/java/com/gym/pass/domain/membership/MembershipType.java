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
        public LocalDate calculateEndDate(LocalDate startDate, Integer months) {
            return startDate.plusMonths(months);
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
        void validate(Integer months, Integer count) {
            requirePositive(months, "기간제는 개월 수(months)가 1 이상이어야 합니다.");
        }
    },

    /** 횟수제. 종료일 = 시작일 + 6개월 (D-7), 출입 시 차감. */
    COUNT {
        @Override
        public LocalDate calculateEndDate(LocalDate startDate, Integer months) {
            return startDate.plusMonths(COUNT_VALIDITY_MONTHS);
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
        void validate(Integer months, Integer count) {
            requirePositive(count, "횟수제는 이용 횟수(count)가 1 이상이어야 합니다.");
        }
    };

    /** 횟수제 유효기간 (D-7 가정). */
    private static final int COUNT_VALIDITY_MONTHS = 6;

    /** 종료일(당일 포함)을 계산한다. */
    public abstract LocalDate calculateEndDate(LocalDate startDate, Integer months);

    /** 등록 시 잔여 횟수. 차감이 없는 종류는 null. */
    public abstract Integer initialCount(Integer count);

    /** 출입 시 횟수 차감 대상인지. */
    public abstract boolean deductible();

    /** 종류별 필수 값 검증. 위반 시 MEMBERSHIP_INVALID_INPUT (D-22). */
    abstract void validate(Integer months, Integer count);

    private static void requirePositive(Integer value, String detail) {
        if (value == null || value < 1) {
            throw new MembershipException(ErrorCode.MEMBERSHIP_INVALID_INPUT, detail);
        }
    }
}
