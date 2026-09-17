package com.gym.pass.application.membership;

import com.gym.pass.domain.membership.MembershipPauseResult;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MembershipPauseInfo {

    public record Paused(
            Long pauseId,
            Long membershipId,
            LocalDate startDate,
            LocalDate endDate,
            Integer days,
            LocalDate membershipEndDate) {

        public static Paused from(MembershipPauseResult result) {
            return new Paused(
                    result.pause().getId(),
                    result.membership().getId(),
                    result.pause().getStartDate(),
                    result.pause().getEndDate(),
                    result.pause().plannedDays(),
                    result.membership().getEndDate());
        }
    }

    public record Released(Long pauseId, Integer usedDays, LocalDate membershipEndDate) {

        public static Released from(MembershipPauseResult result) {
            return new Released(
                    result.pause().getId(),
                    result.pause().usedDays(),
                    result.membership().getEndDate());
        }
    }
}
