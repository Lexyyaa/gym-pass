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

    /** (member_id, status) 인덱스로 찾아 잠근다. 잠금 범위는 이 회원의 ACTIVE · PAUSED 행이다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Membership m"
            + " where m.memberId = :memberId and m.status in :statuses and m.endDate >= :today")
    Optional<Membership> findValidByMemberIdForUpdate(
            @Param("memberId") Long memberId,
            @Param("statuses") Collection<MembershipStatus> statuses,
            @Param("today") LocalDate today);
}
