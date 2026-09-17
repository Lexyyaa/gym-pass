package com.gym.pass.application.membership;

import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MembershipPauseCommand {

    public record Pause(Long membershipId, Long branchId, LocalDate startDate, Integer days) {}

    public record Release(Long membershipId, Long pauseId, Long branchId) {}
}
