package com.gym.pass.domain.membership;

import com.gym.pass.domain.member.MemberRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 정지 등록 · 조기 해제 도메인 서비스 (FR-4.1 ~ FR-4.3 · 03 §7 · D-26).
 * member 행 락 → membership 행 락 → 지점 검사 → 회원권 행위 순서로 실행한다.
 * lock 메서드는 호출 측 트랜잭션 안에서 실행돼야 하고, 그 트랜잭션의 첫 쿼리여야 한다.
 */
@Component
@RequiredArgsConstructor
public class MembershipPauseService {

    private final MemberRepository memberRepository;
    private final MembershipRepository membershipRepository;

    /**
     * 회원권의 회원 id. 없으면 MEMBERSHIP_NOT_FOUND.
     * 락 트랜잭션 밖에서 부른다. 같은 트랜잭션에서 부르면 REPEATABLE READ 스냅샷이 member 락보다 먼저 잡혀
     * 뒤이어 읽는 정지 목록이 먼저 커밋된 정지를 못 본다 (03 §7).
     */
    public Long memberIdOf(Long membershipId) {
        return membershipRepository.getMemberIdById(membershipId);
    }

    public MembershipPauseResult pause(
            Long memberId,
            Long membershipId,
            Long branchId,
            LocalDate startDate,
            Integer days,
            LocalDate today,
            MembershipPauseLimits limits) {
        Membership membership = lock(memberId, membershipId, branchId);
        MembershipPause pause = membership.pause(startDate, days, today, limits);
        membershipRepository.flush();
        return new MembershipPauseResult(membership, pause);
    }

    public MembershipPauseResult release(
            Long memberId, Long membershipId, Long branchId, Long pauseId, LocalDate today) {
        Membership membership = lock(memberId, membershipId, branchId);
        return new MembershipPauseResult(membership, membership.releasePause(pauseId, today));
    }

    /** 락을 잡은 회원권. 이후 적재되는 정지 목록은 member 락 뒤의 스냅샷이다 (정지 변경은 모두 member 락을 거친다). */
    private Membership lock(Long memberId, Long membershipId, Long branchId) {
        // D-26 1번: member 행 락이 트랜잭션의 첫 쿼리다
        memberRepository.getByIdForUpdate(memberId);
        // D-26 2번: 회원권의 member_id는 바뀌지 않으므로 락 밖에서 읽은 memberId가 그대로 유효하다
        Membership membership = membershipRepository.getByIdForUpdate(membershipId);
        membership.verifyBranch(branchId);
        return membership;
    }
}
