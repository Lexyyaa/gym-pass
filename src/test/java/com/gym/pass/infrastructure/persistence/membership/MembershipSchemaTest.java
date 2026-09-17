package com.gym.pass.infrastructure.persistence.membership;

import static org.assertj.core.api.Assertions.assertThat;

import com.gym.pass.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 엔티티로 생성된 스키마가 03 §6의 NOT NULL 제약과 같은지 (실제 MySQL 기준). */
@IntegrationTest
class MembershipSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("membership_history.membership_id 컬럼은 NOT NULL로 생성된다")
    void historyMembershipIdNotNull() {
        // given
        String sql = "SELECT IS_NULLABLE FROM information_schema.COLUMNS"
                + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'membership_history'"
                + " AND COLUMN_NAME = 'membership_id'";

        // when
        String nullable = jdbcTemplate.queryForObject(sql, String.class);

        // then
        assertThat(nullable).isEqualTo("NO");
    }
}
