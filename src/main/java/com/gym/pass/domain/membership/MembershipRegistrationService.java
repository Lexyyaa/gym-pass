package com.gym.pass.domain.membership;

import com.gym.pass.domain.exception.ErrorCode;
import com.gym.pass.domain.member.MemberRepository;
import com.gym.pass.domain.membership.exception.MembershipException;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 회원권 등록 도메인 서비스 (FR-2.3 · NFR-3 · 03 §5 · §7).
 * member 행 락으로 검사-삽입 구간을 직렬화한다. 호출 측 트랜잭션 안에서 실행돼야 한다.
 */
@Component
@RequiredArgsConstructor
public class MembershipRegistrationService {

    private final MemberRepository memberRepository;
    private final MembershipRepository membershipRepository;

    public Membership register(MembershipRegistration registration, MembershipLimits limits, LocalDate today) {
        Membership membership = Membership.register(registration, limits);
        memberRepository.getByIdForUpdate(registration.memberId());
        if (membershipRepository.existsValidByMemberId(registration.memberId(), today)) {
            throw new MembershipException(ErrorCode.MEMBERSHIP_ALREADY_ACTIVE);
        }
        return membershipRepository.save(membership);
    }
}
