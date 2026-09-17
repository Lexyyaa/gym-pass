package com.gym.pass.application.membership;

import com.gym.pass.domain.membership.Membership;
import com.gym.pass.domain.membership.MembershipLimits;
import com.gym.pass.domain.membership.MembershipRegistrationService;
import com.gym.pass.support.properties.MembershipProperties;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MembershipApplicationService {

    private final MembershipRegistrationService membershipRegistrationService;
    private final MembershipProperties membershipProperties;
    private final Clock clock;

    /** 회원권 등록 (FR-2.2 · FR-2.3 · API-2). 락 → 검사 → 삽입이 1트랜잭션이다 (03 §7). 입력 상한은 설정값이다 (C-34). */
    @Transactional
    public MembershipInfo.Registered register(MembershipCommand.Register command) {
        MembershipLimits limits =
                new MembershipLimits(membershipProperties.maxMonths(), membershipProperties.maxCount());
        Membership membership =
                membershipRegistrationService.register(command.toRegistration(), limits, LocalDate.now(clock));
        return MembershipInfo.Registered.from(membership);
    }
}
