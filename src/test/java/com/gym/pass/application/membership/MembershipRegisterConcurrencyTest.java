package com.gym.pass.application.membership;

import static org.assertj.core.api.Assertions.assertThat;

import com.gym.pass.domain.exception.ErrorCode;
import com.gym.pass.domain.membership.exception.MembershipException;
import com.gym.pass.support.ConcurrencyRunner;
import com.gym.pass.support.IntegrationTest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

/** 동시 등록 방어 (NFR-3 · 03 §7). 결과는 DB에서 다시 읽어 확인하고, 만든 데이터는 직접 지운다. */
@IntegrationTest
class MembershipRegisterConcurrencyTest {

    private static final int THREADS = 10;
    private static final long BRANCH_ID = 940_001L;
    private static final String PHONE_PREFIX = "010-9400-";

    @Autowired
    private MembershipApplicationService membershipApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    @AfterEach
    void tearDown() {
        String memberIds = "SELECT id FROM member WHERE phone LIKE '" + PHONE_PREFIX + "%'";
        jdbcTemplate.update("DELETE FROM membership_history WHERE member_id IN (" + memberIds + ")");
        jdbcTemplate.update("DELETE FROM membership WHERE member_id IN (" + memberIds + ")");
        jdbcTemplate.update("DELETE FROM member WHERE phone LIKE ?", PHONE_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM branch WHERE id = ?", BRANCH_ID);
    }

    @Test
    @DisplayName("[TC-2-06] 같은 회원에 등록 요청이 동시에 들어오면 1건만 성공하고 DB 유효 회원권은 1건이다")
    void concurrentRegister() throws InterruptedException {
        // given
        jdbcTemplate.update(
                "INSERT INTO branch (id, name, created_at, updated_at) VALUES (?, '동시성테스트점', NOW(6), NOW(6))",
                BRANCH_ID);
        long memberId = createMember();
        LocalDate today = LocalDate.now(clock);

        // when
        List<ConcurrencyRunner.Result<MembershipInfo.Registered>> results = ConcurrencyRunner.run(
                THREADS, index -> membershipApplicationService.register(command(memberId, today, index)));

        // then
        assertThat(ConcurrencyRunner.successCount(results)).isEqualTo(1);
        assertThat(ConcurrencyRunner.errors(results))
                .hasSize(THREADS - 1)
                .allSatisfy(error -> assertThat(error)
                        .isInstanceOf(MembershipException.class)
                        .extracting("errorCode")
                        .isEqualTo(ErrorCode.MEMBERSHIP_ALREADY_ACTIVE));
        Integer validCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM membership WHERE member_id = ? AND status IN ('ACTIVE', 'PAUSED')"
                        + " AND end_date >= ?",
                Integer.class,
                memberId,
                today);
        assertThat(validCount).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM membership WHERE member_id = ?", Integer.class, memberId))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM membership_history WHERE member_id = ? AND event_type = 'REGISTERED'",
                        Integer.class,
                        memberId))
                .isEqualTo(1);
    }

    /** 짝수 스레드는 기간제, 홀수 스레드는 횟수제로 보내 종류와 무관하게 막히는지 본다. */
    private static MembershipCommand.Register command(long memberId, LocalDate today, int index) {
        if (index % 2 == 0) {
            return new MembershipCommand.Register(memberId, BRANCH_ID, "PERIOD", today, 1, null, 100_000L);
        }
        return new MembershipCommand.Register(memberId, BRANCH_ID, "COUNT", today, null, 10, 100_000L);
    }

    private long createMember() {
        return new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("member")
                .usingGeneratedKeyColumns("id")
                .executeAndReturnKey(Map.of(
                        "name",
                        "동시성회원",
                        "phone",
                        PHONE_PREFIX + "0001",
                        "created_at",
                        LocalDateTime.now(clock),
                        "updated_at",
                        LocalDateTime.now(clock)))
                .longValue();
    }
}
