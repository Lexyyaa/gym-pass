package com.gym.pass.support.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gym.pass.support.IntegrationTest;
import com.gym.pass.support.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * X-Branch-Id 헤더 검증 (04 §1). 지점 헤더를 쓰는 운영 API가 아직 없어
 * 실제 resolver 빈 + 실제 DB에 테스트 전용 컨트롤러를 붙여 HTTP 매핑까지 확인한다.
 */
@IntegrationTest
class BranchIdArgumentResolverTest {

    private static final long TEST_BRANCH_ID = 900_001L;
    private static final long MISSING_BRANCH_ID = 900_999L;

    @Autowired
    private BranchIdArgumentResolver branchIdArgumentResolver;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SampleController())
                .setCustomArgumentResolvers(branchIdArgumentResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        jdbcTemplate.update(
                "INSERT INTO branch (id, name, created_at, updated_at) VALUES (?, '테스트점', NOW(6), NOW(6))",
                TEST_BRANCH_ID);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM branch WHERE id IN (?, ?)", TEST_BRANCH_ID, MISSING_BRANCH_ID);
    }

    @Test
    @DisplayName("존재하는 지점 ID 헤더면 해당 ID가 컨트롤러로 전달된다")
    void resolve() throws Exception {
        // given
        // when
        // then
        mockMvc.perform(get("/samples/branch").header(BranchIdArgumentResolver.HEADER, TEST_BRANCH_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(String.valueOf(TEST_BRANCH_ID)));
    }

    @Test
    @DisplayName("[TC-1-03] X-Branch-Id 헤더가 없으면 400 COMMON_INVALID_INPUT으로 응답한다")
    void missingHeader() throws Exception {
        // given
        // when
        // then
        mockMvc.perform(get("/samples/branch"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("X-Branch-Id: 숫자 지점 ID가 필요합니다."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1a", "-1", " ", "1.5", "99999999999999999999"})
    @DisplayName("[TC-1-03] X-Branch-Id 헤더가 숫자가 아니면 400 COMMON_INVALID_INPUT으로 응답한다")
    void notNumber(String header) throws Exception {
        // given
        // when
        // then
        mockMvc.perform(get("/samples/branch").header(BranchIdArgumentResolver.HEADER, header))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"));
    }

    @Test
    @DisplayName("[TC-1-04] 존재하지 않는 지점 ID 헤더면 404 BRANCH_NOT_FOUND로 응답한다")
    void branchNotFound() throws Exception {
        // given
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM branch WHERE id = ?", Integer.class, MISSING_BRANCH_ID);
        assertThat(count).isZero();

        // when
        // then
        mockMvc.perform(get("/samples/branch").header(BranchIdArgumentResolver.HEADER, MISSING_BRANCH_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("BRANCH_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("지점을 찾을 수 없습니다."));
    }

    @RestController
    static class SampleController {

        @GetMapping("/samples/branch")
        Long branch(@BranchId Long branchId) {
            return branchId;
        }
    }
}
