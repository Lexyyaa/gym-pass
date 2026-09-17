package com.gym.pass.application.attendance;

import com.gym.pass.domain.attendance.AttendanceRecord;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AttendanceInfo {

    public record Entered(Long attendanceId, Long membershipId, LocalDateTime attendedAt) {

        public static Entered from(AttendanceRecord attendanceRecord) {
            return new Entered(
                    attendanceRecord.getId(), attendanceRecord.getMembershipId(), attendanceRecord.getEntryAt());
        }
    }
}
