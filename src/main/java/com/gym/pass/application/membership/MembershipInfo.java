package com.gym.pass.application.membership;

import com.gym.pass.domain.membership.Membership;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MembershipInfo {

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

        public static Registered from(Membership membership) {
            return new Registered(
                    membership.getId(),
                    membership.getMemberId(),
                    membership.getBranchId(),
                    membership.getType().name(),
                    membership.getStatus().name(),
                    membership.getStartDate(),
                    membership.getEndDate(),
                    membership.getRemainingCount(),
                    membership.getPrice());
        }
    }
}
