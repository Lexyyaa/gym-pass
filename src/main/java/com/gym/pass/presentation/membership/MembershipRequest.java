package com.gym.pass.presentation.membership;

import com.gym.pass.application.membership.MembershipCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MembershipRequest {

    /** type별 필수 값(months · count)은 도메인이 검증한다 (D-22). */
    public record Register(
            @NotNull @Positive Long memberId,
            @NotNull @Pattern(regexp = "PERIOD|COUNT") String type,
            @NotNull LocalDate startDate,
            @Positive Integer months,
            @Positive Integer count,
            @NotNull @PositiveOrZero Long paymentAmount) {

        public MembershipCommand.Register toCommand(Long branchId) {
            return new MembershipCommand.Register(memberId, branchId, type, startDate, months, count, paymentAmount);
        }
    }
}
