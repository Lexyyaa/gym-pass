package com.gym.pass.domain.membership;

import java.time.LocalDate;

/**
 * 회원권 등록 입력 VO (FR-2.2).
 * months는 기간제, count는 횟수제에서만 받는다. 반대 종류의 값이 오면 거부한다 (D-23).
 */
public record MembershipRegistration(
        Long memberId,
        Long branchId,
        MembershipType type,
        LocalDate startDate,
        Integer months,
        Integer count,
        Long price) {}
