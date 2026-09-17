package com.gym.pass.domain.member;

public interface MemberRepository {

    Member save(Member member);

    /** 없으면 MEMBER_NOT_FOUND. */
    Member getById(Long id);

    /** member 행 비관적 락(PK FOR UPDATE). 없으면 MEMBER_NOT_FOUND (03 §7). */
    Member getByIdForUpdate(Long id);
}
