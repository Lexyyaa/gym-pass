package com.gym.pass.infrastructure.persistence.attendance;

import com.gym.pass.domain.attendance.AttendanceRecord;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordJpaRepository extends JpaRepository<AttendanceRecord, Long> {

    boolean existsByMembershipIdAndEntryDateAndDeductedTrue(Long membershipId, LocalDate entryDate);

    /** idx_attendance_membership_entry_date(membership_id, entry_date)로 판정한다. */
    boolean existsByMembershipIdAndEntryDate(Long membershipId, LocalDate entryDate);
}
