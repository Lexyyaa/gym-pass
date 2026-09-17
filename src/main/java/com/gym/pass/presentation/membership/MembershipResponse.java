package com.gym.pass.presentation.membership;

import com.gym.pass.application.membership.MembershipInfo;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MembershipResponse {

    public record Registered(
            Long membershipId,
            Long memberId,
            Long branchId,
            String type,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            Integer remainingCount,
            Long paymentAmount) {

        public static Registered from(MembershipInfo.Registered info) {
            return new Registered(
                    info.membershipId(),
                    info.memberId(),
                    info.branchId(),
                    info.type(),
                    info.status(),
                    info.startDate(),
                    info.endDate(),
                    info.remainingCount(),
                    info.paymentAmount());
        }
    }
}
