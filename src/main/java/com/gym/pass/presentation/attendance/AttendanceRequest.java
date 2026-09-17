package com.gym.pass.presentation.attendance;

import com.gym.pass.application.attendance.AttendanceCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AttendanceRequest {

    public record Enter(@NotNull @Positive Long memberId) {

        public AttendanceCommand.Enter toCommand(Long branchId) {
            return new AttendanceCommand.Enter(memberId, branchId);
        }
    }
}
