package com.gym.pass.infrastructure.persistence.member;

import com.gym.pass.domain.exception.ErrorCode;
import com.gym.pass.domain.member.Member;
import com.gym.pass.domain.member.MemberRepository;
import com.gym.pass.domain.member.exception.MemberException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {

    private final MemberJpaRepository memberJpaRepository;

    @Override
    public Member save(Member member) {
        return memberJpaRepository.save(member);
    }

    @Override
    public Member getById(Long id) {
        return memberJpaRepository.findById(id).orElseThrow(() -> new MemberException(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Override
    public Member getByIdForUpdate(Long id) {
        return memberJpaRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new MemberException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
