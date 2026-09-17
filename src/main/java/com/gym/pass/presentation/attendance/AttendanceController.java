package com.gym.pass.presentation.attendance;

import com.gym.pass.application.attendance.AttendanceApplicationService;
import com.gym.pass.support.web.BranchId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceApplicationService attendanceApplicationService;

    /** API-3 출입 기록. 같은 날 두 번째 이후 출입도 같은 200 응답이다 (D-11). */
    @PostMapping
    public AttendanceResponse.Entered enter(
            @BranchId Long branchId, @Valid @RequestBody AttendanceRequest.Enter request) {
        return AttendanceResponse.Entered.from(attendanceApplicationService.enter(request.toCommand(branchId)));
    }
}
