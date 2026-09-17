package com.gym.pass.presentation.membership;

import com.gym.pass.application.membership.MembershipPauseInfo;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MembershipPauseResponse {

    public record Paused(
            Long pauseId,
            Long membershipId,
            LocalDate startDate,
            LocalDate endDate,
            Integer days,
            LocalDate membershipEndDate) {

        public static Paused from(MembershipPauseInfo.Paused info) {
            return new Paused(
                    info.pauseId(),
                    info.membershipId(),
                    info.startDate(),
                    info.endDate(),
                    info.days(),
                    info.membershipEndDate());
        }
    }

    public record Released(Long pauseId, Integer usedDays, LocalDate membershipEndDate) {

        public static Released from(MembershipPauseInfo.Released info) {
            return new Released(info.pauseId(), info.usedDays(), info.membershipEndDate());
        }
    }
}
