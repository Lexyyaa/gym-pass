package com.gym.pass.presentation.membership;

import com.gym.pass.application.membership.MembershipApplicationService;
import com.gym.pass.support.web.BranchId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipApplicationService membershipApplicationService;

    /** API-2 회원권 등록. 헤더 지점 소속으로 생성된다. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipResponse.Registered register(
            @BranchId Long branchId, @Valid @RequestBody MembershipRequest.Register request) {
        return MembershipResponse.Registered.from(membershipApplicationService.register(request.toCommand(branchId)));
    }
}
