package com.gym.pass.domain.membership;

import java.time.LocalDate;
import java.util.Optional;

public interface MembershipRepository {

    Membership save(Membership membership);

    /** 종료일 ≥ today인 ACTIVE · PAUSED 회원권이 있는지 (전 지점, D-15 · D-19). */
    boolean existsValidByMemberId(Long memberId, LocalDate today);

    /**
     * 출입 판정 대상(종료일 ≥ today인 ACTIVE · PAUSED)의 id를 일반 조회한다 (03 §7).
     * 등록 규칙(C-22)으로 항상 1건 이하다. 회원 행 락(D-26 1번) 뒤에 호출한다.
     */
    Optional<Long> findValidIdByMemberId(Long memberId, LocalDate today);

    /**
     * 회원권의 회원 id만 조회한다. 없으면 MEMBERSHIP_NOT_FOUND.
     * 엔티티를 영속성 컨텍스트에 올리지 않는다. 락 트랜잭션 밖에서 불러 D-26 1번(member 락)이 첫 쿼리가 되게 한다.
     */
    Long getMemberIdById(Long id);

    /** 변경을 즉시 DB에 반영한다. 새로 추가한 하위 엔티티(정지)의 id를 채울 때 쓴다. */
    void flush();

    /** membership 행 비관적 락(PK FOR UPDATE). 없으면 MEMBERSHIP_NOT_FOUND (D-26 2번). */
    Membership getByIdForUpdate(Long id);
}
