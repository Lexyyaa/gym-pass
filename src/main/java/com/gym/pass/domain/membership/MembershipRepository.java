package com.gym.pass.domain.membership;

import java.time.LocalDate;

public interface MembershipRepository {

    Membership save(Membership membership);

    /** 종료일 ≥ today인 ACTIVE · PAUSED 회원권이 있는지 (전 지점, D-15 · D-19). */
    boolean existsValidByMemberId(Long memberId, LocalDate today);
}
