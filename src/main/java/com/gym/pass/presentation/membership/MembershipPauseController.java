package com.gym.pass.presentation.membership;

import com.gym.pass.application.membership.MembershipPauseApplicationService;
import com.gym.pass.application.membership.MembershipPauseCommand;
import com.gym.pass.support.web.BranchId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/memberships/{membershipId}/pauses")
@RequiredArgsConstructor
public class MembershipPauseController {

    private final MembershipPauseApplicationService membershipPauseApplicationService;

    /** API-4 정지 등록 (예약). */
    @PostMapping
    public MembershipPauseResponse.Paused pause(
            @BranchId Long branchId,
            @PathVariable Long membershipId,
            @Valid @RequestBody MembershipPauseRequest.Pause request) {
        return MembershipPauseResponse.Paused.from(
                membershipPauseApplicationService.pause(request.toCommand(membershipId, branchId)));
    }

    /** API-5 정지 조기 해제. 본문 없음. */
    @PostMapping("/{pauseId}/release")
    public MembershipPauseResponse.Released release(
            @BranchId Long branchId, @PathVariable Long membershipId, @PathVariable Long pauseId) {
        return MembershipPauseResponse.Released.from(membershipPauseApplicationService.release(
                new MembershipPauseCommand.Release(membershipId, pauseId, branchId)));
    }
}
