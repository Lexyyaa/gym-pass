package com.gym.pass.domain.membership;

import java.time.LocalDate;

/**
 * 회원권 등록 입력 VO (FR-2.2).
 * months는 기간제, count는 횟수제에서만 쓴다. 반대 종류의 값은 무시한다.
 */
public record MembershipRegistration(
        Long memberId,
        Long branchId,
        MembershipType type,
        LocalDate startDate,
        Integer months,
        Integer count,
        Long price) {}
