package com.gym.pass.presentation.attendance;

import com.gym.pass.application.attendance.AttendanceInfo;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AttendanceResponse {

    public record Entered(Long attendanceId, Long membershipId, LocalDateTime attendedAt) {

        public static Entered from(AttendanceInfo.Entered info) {
            return new Entered(info.attendanceId(), info.membershipId(), info.attendedAt());
        }
    }
}
