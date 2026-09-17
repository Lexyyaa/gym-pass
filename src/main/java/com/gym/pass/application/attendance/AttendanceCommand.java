package com.gym.pass.application.attendance;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AttendanceCommand {

    public record Enter(Long memberId, Long branchId) {}
}
