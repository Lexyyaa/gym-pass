package com.gym.pass.application.membership;

import com.gym.pass.domain.membership.MembershipRegistration;
import com.gym.pass.domain.membership.MembershipType;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MembershipCommand {

    /** type은 Request에서 PERIOD · COUNT로 검증된 문자열이다. */
    public record Register(
            Long memberId,
            Long branchId,
            String type,
            LocalDate startDate,
            Integer months,
            Integer count,
            Long paymentAmount) {

        public MembershipRegistration toRegistration() {
            return new MembershipRegistration(
                    memberId, branchId, MembershipType.valueOf(type), startDate, months, count, paymentAmount);
        }
    }
}
