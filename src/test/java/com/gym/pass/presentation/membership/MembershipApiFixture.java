package com.gym.pass.presentation.membership;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

/** 회원권 API 테스트용 데이터. 테스트 전용 지점 · 연락처 접두로 만들고 지운다 (seed에 기대지 않는다). */
class MembershipApiFixture {

    static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final JdbcTemplate jdbcTemplate;
    private final long branchId;
    private final String phonePrefix;

    MembershipApiFixture(JdbcTemplate jdbcTemplate, long branchId, String phonePrefix) {
        this.jdbcTemplate = jdbcTemplate;
        this.branchId = branchId;
        this.phonePrefix = phonePrefix;
    }

    static LocalDate today() {
        return LocalDate.now(KST);
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
                        java.time.LocalDateTime.now(KST),
                        "updated_at",
                        java.time.LocalDateTime.now(KST)))
                .longValue();
    }

    /** 상태 · 종료일을 직접 지정한 기간제 회원권 (API로 만들 수 없는 상태 준비용). */
    void insertPeriodMembership(long memberId, String status, LocalDate startDate, LocalDate endDate) {
        jdbcTemplate.update(
                "INSERT INTO membership (member_id, branch_id, type, status, start_date, end_date, months, price,"
                        + " created_at, updated_at) VALUES (?, ?, 'PERIOD', ?, ?, ?, 1, 100000, NOW(6), NOW(6))",
                memberId,
                branchId,
                status,
                startDate,
                endDate);
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
