package com.gym.pass.infrastructure.persistence.attendance;

import static org.assertj.core.api.Assertions.assertThat;

import com.gym.pass.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 엔티티로 생성된 attendance_record 스키마가 03 §6의 NOT NULL · 인덱스와 같은지 (실제 MySQL 기준). */
@IntegrationTest
class AttendanceRecordSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("attendance_record는 전 컬럼이 NOT NULL로 생성된다")
    void allColumnsNotNull() {
        // given
        String sql = "SELECT COLUMN_NAME FROM information_schema.COLUMNS"
                + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attendance_record' AND IS_NULLABLE = 'YES'";

        // when
        List<String> nullableColumns = jdbcTemplate.queryForList(sql, String.class);

        // then
        assertThat(nullableColumns).isEmpty();
    }

    @Test
    @DisplayName("attendance_record에 (membership_id, entry_date) · (member_id, branch_id, entry_at) 인덱스가 있다")
    void indexes() {
        // given
        String sql = "SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) FROM information_schema.STATISTICS"
                + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attendance_record'"
                + " GROUP BY INDEX_NAME";

        // when
        List<String> indexColumns = jdbcTemplate.queryForList(sql, String.class);

        // then
        assertThat(indexColumns).contains("membership_id,entry_date", "member_id,branch_id,entry_at");
    }

    @Test
    @DisplayName("attendance_record의 회원 · 회원권 · 지점 참조는 물리 FK 없는 논리 참조다 (D-25)")
    void noForeignKey() {
        // given
        String sql = "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS"
                + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attendance_record'"
                + " AND CONSTRAINT_TYPE = 'FOREIGN KEY'";

        // when
        Integer foreignKeyCount = jdbcTemplate.queryForObject(sql, Integer.class);

        // then
        assertThat(foreignKeyCount).isZero();
    }
}
