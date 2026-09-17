package com.gym.pass.application.member;

import com.gym.pass.domain.member.Member;
import com.gym.pass.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberApplicationService {

    private final MemberRepository memberRepository;

    /** 회원 등록 (FR-2.1 · API-1). */
    @Transactional
    public MemberInfo.Registered register(MemberCommand.Register command) {
        Member member = memberRepository.save(Member.create(command.name(), command.phone()));
        return MemberInfo.Registered.from(member);
    }
}
