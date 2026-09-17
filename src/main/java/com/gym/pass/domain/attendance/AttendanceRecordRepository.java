package com.gym.pass.domain.attendance;

import java.time.LocalDate;

public interface AttendanceRecordRepository {

    AttendanceRecord save(AttendanceRecord attendanceRecord);

    /** 이 회원권에 entryDate(KST) 날짜로 차감된 출입이 있는지 (D-5 · NFR-2). membership 행 락을 잡은 뒤에 호출한다. */
    boolean existsDeductedOn(Long membershipId, LocalDate entryDate);
}
