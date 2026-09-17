package com.gym.pass.support.fixture;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

/**
 * 출입 테스트용 데이터. 테스트 전용 지점 ID · 연락처 접두로 만들고 지운다 (seed에 기대지 않는다).
 * 오늘은 서버가 쓰는 Clock 빈 기준이다.
 */
public class AttendanceFixture {

    /** 횟수제 개월 수 (D-24 · MembershipType.COUNT). */
    private static final int COUNT_MONTHS = 6;

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final String phonePrefix;

    public AttendanceFixture(JdbcTemplate jdbcTemplate, Clock clock, String phonePrefix) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.phonePrefix = phonePrefix;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public void createBranch(long branchId) {
        jdbcTemplate.update(
                "INSERT INTO branch (id, name, created_at, updated_at) VALUES (?, '출입테스트점', NOW(6), NOW(6))", branchId);
    }

    public long createMember(int seq) {
        return new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("member")
                .usingGeneratedKeyColumns("id")
                .executeAndReturnKey(Map.of(
                        "name",
                        "출입회원" + seq,
                        "phone",
                        phonePrefix + String.format("%04d", seq),
                        "created_at",
                        LocalDateTime.now(clock),
                        "updated_at",
                        LocalDateTime.now(clock)))
                .longValue();
    }

    /** 기간제 회원권. months는 시작일~종료일의 개월 수를 넘긴다. */
    public long insertPeriodMembership(
            long memberId, long branchId, String status, LocalDate startDate, LocalDate endDate, int months) {
        return insertMembership(memberId, branchId, "PERIOD", status, startDate, endDate, months, null);
    }

    /** 횟수제 회원권. 개월 수는 6, 총 횟수 = 10이고 잔여 횟수를 지정한다. */
    public long insertCountMembership(
            long memberId, long branchId, String status, LocalDate startDate, LocalDate endDate, int remaining) {
        return insertMembership(memberId, branchId, "COUNT", status, startDate, endDate, COUNT_MONTHS, remaining);
    }

    private long insertMembership(
            long memberId,
            long branchId,
            String type,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            int months,
            Integer remaining) {
        Map<String, Object> row = new HashMap<>();
        row.put("member_id", memberId);
        row.put("branch_id", branchId);
        row.put("type", type);
        row.put("status", status);
        row.put("start_date", startDate);
        row.put("end_date", endDate);
        row.put("months", months);
        row.put("total_count", remaining == null ? null : 10);
        row.put("remaining_count", remaining);
        row.put("price", 100_000L);
        row.put("created_at", LocalDateTime.now(clock));
        row.put("updated_at", LocalDateTime.now(clock));
        return new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("membership")
                .usingGeneratedKeyColumns("id")
                .executeAndReturnKey(row)
                .longValue();
    }

    public int countAttendances(long memberId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance_record WHERE member_id = ?", Integer.class, memberId);
    }

    public int countDeductedAttendances(long membershipId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance_record WHERE membership_id = ? AND deducted = TRUE",
                Integer.class,
                membershipId);
    }

    public Integer remainingCount(long membershipId) {
        return jdbcTemplate.queryForObject(
                "SELECT remaining_count FROM membership WHERE id = ?", Integer.class, membershipId);
    }

    public String status(long membershipId) {
        return jdbcTemplate.queryForObject("SELECT status FROM membership WHERE id = ?", String.class, membershipId);
    }

    public int countDeductedHistories(long membershipId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM membership_history WHERE membership_id = ? AND event_type = 'DEDUCTED'",
                Integer.class,
                membershipId);
    }

    public void cleanUp(long... branchIds) {
        String memberIds = "SELECT id FROM member WHERE phone LIKE '" + phonePrefix + "%'";
        jdbcTemplate.update("DELETE FROM attendance_record WHERE member_id IN (" + memberIds + ")");
        jdbcTemplate.update("DELETE FROM membership_history WHERE member_id IN (" + memberIds + ")");
        jdbcTemplate.update("DELETE FROM membership WHERE member_id IN (" + memberIds + ")");
        jdbcTemplate.update("DELETE FROM member WHERE phone LIKE ?", phonePrefix + "%");
        for (long branchId : branchIds) {
            jdbcTemplate.update("DELETE FROM branch WHERE id = ?", branchId);
        }
    }
}
