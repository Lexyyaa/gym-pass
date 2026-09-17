package com.gym.pass.infrastructure.persistence.attendance;

import com.gym.pass.domain.attendance.AttendanceRecord;
import com.gym.pass.domain.attendance.AttendanceRecordRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AttendanceRecordRepositoryImpl implements AttendanceRecordRepository {

    private final AttendanceRecordJpaRepository attendanceRecordJpaRepository;

    @Override
    public AttendanceRecord save(AttendanceRecord attendanceRecord) {
        return attendanceRecordJpaRepository.save(attendanceRecord);
    }

    @Override
    public boolean existsDeductedOn(Long membershipId, LocalDate entryDate) {
        return attendanceRecordJpaRepository.existsByMembershipIdAndEntryDateAndDeductedTrue(membershipId, entryDate);
    }
}
