package com.gym.pass.presentation.member;

import com.gym.pass.application.member.MemberApplicationService;
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
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberApplicationService memberApplicationService;

    /** API-1 회원 등록. 회원은 전사 공유라 지점 값은 쓰지 않지만 헤더 검증을 위해 받는다. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse.Registered register(
            @BranchId Long branchId, @Valid @RequestBody MemberRequest.Register request) {
        return MemberResponse.Registered.from(memberApplicationService.register(request.toCommand()));
    }
}
