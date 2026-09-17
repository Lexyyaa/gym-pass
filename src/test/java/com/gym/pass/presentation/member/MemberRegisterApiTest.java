package com.gym.pass.presentation.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.pass.support.IntegrationTest;
import com.gym.pass.support.web.BranchIdArgumentResolver;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** API-1 회원 등록. 주입 MockMvc로 WebConfig에 등록된 지점 헤더 검증까지 확인한다. */
@IntegrationTest
class MemberRegisterApiTest {

    private static final long BRANCH_ID = 910_001L;
    private static final long MISSING_BRANCH_ID = 910_999L;
    private static final String PHONE = "010-9100-0001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
                "INSERT INTO branch (id, name, created_at, updated_at) VALUES (?, '회원테스트점', NOW(6), NOW(6))",
                BRANCH_ID);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM member WHERE phone = ?", PHONE);
        jdbcTemplate.update("DELETE FROM branch WHERE id = ?", BRANCH_ID);
    }

    @Test
    @DisplayName("[TC-2-01] 이름·연락처로 회원을 등록하면 201과 id를 반환하고 DB에 1건 저장된다")
    void register() throws Exception {
        // given
        String body = objectMapper.writeValueAsString(Map.of("name", "김지영", "phone", PHONE));

        // when
        String response = mockMvc.perform(post("/api/members")
                        .header(BranchIdArgumentResolver.HEADER, BRANCH_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").isNumber())
                .andExpect(jsonPath("$.name").value("김지영"))
                .andExpect(jsonPath("$.phone").value(PHONE))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then
        JsonNode json = objectMapper.readTree(response);
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT name, phone FROM member WHERE id = ?",
                json.get("memberId").asLong());
        assertThat(row).containsEntry("name", "김지영").containsEntry("phone", PHONE);
        assertThat(countMembers()).isEqualTo(1);
    }

    @Test
    @DisplayName("형식 오류 연락처로 회원을 등록하면 400 COMMON_INVALID_INPUT이고 저장되지 않는다")
    void invalidPhone() throws Exception {
        // given
        String body = objectMapper.writeValueAsString(Map.of("name", "김지영", "phone", "01091000001"));

        // when
        // then
        mockMvc.perform(post("/api/members")
                        .header(BranchIdArgumentResolver.HEADER, BRANCH_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"));
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM member WHERE phone = '01091000001'", Integer.class))
                .isZero();
    }

    @Test
    @DisplayName("[TC-1-03] 회원 등록에 X-Branch-Id 헤더가 없으면 400 COMMON_INVALID_INPUT이고 저장되지 않는다")
    void missingHeader() throws Exception {
        // given
        String body = objectMapper.writeValueAsString(Map.of("name", "김지영", "phone", PHONE));

        // when
        // then
        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("X-Branch-Id: 숫자 지점 ID가 필요합니다."));
        assertThat(countMembers()).isZero();
    }

    @Test
    @DisplayName("[TC-1-04] 회원 등록에 존재하지 않는 지점 ID 헤더면 404 BRANCH_NOT_FOUND이고 저장되지 않는다")
    void branchNotFound() throws Exception {
        // given
        String body = objectMapper.writeValueAsString(Map.of("name", "김지영", "phone", PHONE));

        // when
        // then
        mockMvc.perform(post("/api/members")
                        .header(BranchIdArgumentResolver.HEADER, MISSING_BRANCH_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("BRANCH_NOT_FOUND"));
        assertThat(countMembers()).isZero();
    }

    private Integer countMembers() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM member WHERE phone = ?", Integer.class, PHONE);
    }
}
