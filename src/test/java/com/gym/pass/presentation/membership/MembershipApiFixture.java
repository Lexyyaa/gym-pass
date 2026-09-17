package com.gym.pass.presentation.membership;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

/** 회원권 API 테스트용 데이터. 테스트 전용 지점 · 연락처 접두로 만들고 지운다 (seed에 기대지 않는다). */
class MembershipApiFixture {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final long branchId;
    private final String phonePrefix;

    MembershipApiFixture(JdbcTemplate jdbcTemplate, Clock clock, long branchId, String phonePrefix) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.branchId = branchId;
        this.phonePrefix = phonePrefix;
    }

    /** 서버가 쓰는 Clock 빈 기준 오늘. 테스트와 서버의 기준 시계를 하나로 맞춘다. */
    LocalDate today() {
        return LocalDate.now(clock);
    }

    void createBranch() {
        jdbcTemplate.update(
                "INSERT INTO branch (id, name, created_at, updated_at) VALUES (?, '회원권테스트점', NOW(6), NOW(6))",
                branchId);
    }

    long createMember(int seq) {
        return new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("member")
                .usingGeneratedKeyColumns("id")
                .executeAndReturnKey(Map.of(
                        "name",
                        "테스트회원" + seq,
                        "phone",
                        phonePrefix + String.format("%04d", seq),
                        "created_at",
                        LocalDateTime.now(clock),
                        "updated_at",
                        LocalDateTime.now(clock)))
                .longValue();
    }

    /** 상태 · 종료일 · 개월 수를 직접 지정한 기간제 회원권 (API로 만들 수 없는 상태 준비용). 지점은 fixture 지점이다. */
    void insertPeriodMembership(long memberId, String status, LocalDate startDate, LocalDate endDate, int months) {
        insertPeriodMembership(memberId, branchId, status, startDate, endDate, months);
    }

    /** 지점까지 지정한 기간제 회원권 (다른 지점 보유 상태 준비용). */
    void insertPeriodMembership(
            long memberId, long membershipBranchId, String status, LocalDate startDate, LocalDate endDate, int months) {
        jdbcTemplate.update(
                "INSERT INTO membership (member_id, branch_id, type, status, start_date, end_date, months, price,"
                        + " created_at, updated_at) VALUES (?, ?, 'PERIOD', ?, ?, ?, ?, 100000, NOW(6), NOW(6))",
                memberId,
                membershipBranchId,
                status,
                startDate,
                endDate,
                months);
    }

    int countMemberships(long memberId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM membership WHERE member_id = ?", Integer.class, memberId);
    }

    int countHistories(long memberId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM membership_history WHERE member_id = ?", Integer.class, memberId);
    }

    void cleanUp() {
        String memberIds = "SELECT id FROM member WHERE phone LIKE '" + phonePrefix + "%'";
        jdbcTemplate.update("DELETE FROM membership_history WHERE member_id IN (" + memberIds + ")");
        jdbcTemplate.update("DELETE FROM membership WHERE member_id IN (" + memberIds + ")");
        jdbcTemplate.update("DELETE FROM member WHERE phone LIKE ?", phonePrefix + "%");
        jdbcTemplate.update("DELETE FROM branch WHERE id = ?", branchId);
    }
}
