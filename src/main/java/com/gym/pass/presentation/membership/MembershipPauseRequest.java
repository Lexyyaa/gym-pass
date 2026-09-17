package com.gym.pass.presentation.membership;

import com.gym.pass.application.membership.MembershipPauseCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MembershipPauseRequest {

    /** 시작일이 오늘 이후인지는 도메인이 검증한다 (PAUSE_START_DATE_PAST). */
    public record Pause(
            @NotNull LocalDate startDate, @NotNull @Positive Integer days) {

        public MembershipPauseCommand.Pause toCommand(Long membershipId, Long branchId) {
            return new MembershipPauseCommand.Pause(membershipId, branchId, startDate, days);
        }
    }
}
