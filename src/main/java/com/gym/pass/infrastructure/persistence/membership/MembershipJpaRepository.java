package com.gym.pass.infrastructure.persistence.membership;

import com.gym.pass.domain.membership.Membership;
import com.gym.pass.domain.membership.MembershipStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipJpaRepository extends JpaRepository<Membership, Long> {

    boolean existsByMemberIdAndStatusInAndEndDateGreaterThanEqual(
            Long memberId, Collection<MembershipStatus> statuses, LocalDate today);

    /** 출입 판정 대상의 id만 일반 조회한다. 엔티티를 영속성 컨텍스트에 올리지 않아야 뒤의 락 조회가 최신 값을 적재한다. */
    @Query("select m.id from Membership m"
            + " where m.memberId = :memberId and m.status in :statuses and m.endDate >= :today")
    Optional<Long> findValidIdByMemberId(
            @Param("memberId") Long memberId,
            @Param("statuses") Collection<MembershipStatus> statuses,
            @Param("today") LocalDate today);

    /** 회원 id만 스칼라로 읽는다. 엔티티를 적재하지 않는다. */
    @Query("select m.memberId from Membership m where m.id = :id")
    Optional<Long> findMemberIdById(@Param("id") Long id);

    /** PK 등호라 잠금 범위는 1행이다 (D-26). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Membership m where m.id = :id")
    Optional<Membership> findByIdForUpdate(@Param("id") Long id);
}
