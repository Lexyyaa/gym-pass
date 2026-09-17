package com.gym.pass.application.attendance;

import com.gym.pass.domain.attendance.AttendanceService;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttendanceApplicationService {

    private final AttendanceService attendanceService;
    private final Clock clock;

    /** 출입 기록 (FR-3.1 ~ FR-3.4 · API-3). 락 · 판정 · 차감 · 기록이 1트랜잭션이다 (NFR-4 · 03 §7). */
    @Transactional
    public AttendanceInfo.Entered enter(AttendanceCommand.Enter command) {
        return AttendanceInfo.Entered.from(
                attendanceService.enter(command.memberId(), command.branchId(), LocalDateTime.now(clock)));
    }
}
