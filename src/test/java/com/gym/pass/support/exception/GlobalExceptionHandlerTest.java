package com.gym.pass.support.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gym.pass.domain.exception.BusinessException;
import com.gym.pass.domain.exception.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 예외 → 응답 매핑 검증. DB 없이 standalone MockMvc로 돈다.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SampleController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("BusinessException은 ErrorCode의 상태와 코드로 응답한다")
    void businessException() throws Exception {
        mockMvc.perform(get("/samples/business"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COMMON_RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("샘플이 없습니다."));
    }

    @Test
    @DisplayName("요청 본문 필드 검증에 실패하면 400 COMMON_INVALID_INPUT과 필드명을 응답한다")
    void invalidField() throws Exception {
        mockMvc.perform(post("/samples").contentType(MediaType.APPLICATION_JSON).content("""
                                {"name": " ", "items": [{"amount": 1}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("name:")));
    }

    @Test
    @DisplayName("리스트 요소의 필드 검증도 수행되어 음수는 500이 아니라 400으로 응답한다")
    void invalidListElement() throws Exception {
        mockMvc.perform(post("/samples").contentType(MediaType.APPLICATION_JSON).content("""
                                {"name": "a", "items": [{"amount": -1}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("items[0].amount:")));
    }

    @Test
    @DisplayName("본문 JSON 형식이 깨지면 400 COMMON_INVALID_INPUT으로 응답한다")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/samples").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"));
    }

    @Test
    @DisplayName("필수 헤더가 없으면 400 COMMON_INVALID_INPUT으로 응답한다")
    void missingHeader() throws Exception {
        mockMvc.perform(get("/samples/header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"));
    }

    @Test
    @DisplayName("경로 변수 타입이 맞지 않으면 400 COMMON_INVALID_INPUT으로 응답한다")
    void typeMismatch() throws Exception {
        mockMvc.perform(get("/samples/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"));
    }

    @Test
    @DisplayName("매핑되지 않은 경로는 404 COMMON_RESOURCE_NOT_FOUND로 응답한다")
    void noHandler() throws Exception {
        mockMvc.perform(get("/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COMMON_RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("@Validated 파라미터 제약 위반(ConstraintViolationException)도 400 COMMON_INVALID_INPUT으로 응답한다")
    void constraintViolation() throws Exception {
        mockMvc.perform(get("/samples/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"));
    }

    @Test
    @DisplayName("지원하지 않는 메서드는 405 COMMON_METHOD_NOT_ALLOWED로 응답한다")
    void methodNotAllowed() throws Exception {
        mockMvc.perform(post("/samples/business"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errorCode").value("COMMON_METHOD_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("예상하지 못한 예외는 500 COMMON_INTERNAL_SERVER_ERROR로 응답하고 내부 메시지를 노출하지 않는다")
    void unexpected() throws Exception {
        mockMvc.perform(get("/samples/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("COMMON_INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."));
    }

    @RestController
    static class SampleController {

        @GetMapping("/samples/business")
        void business() {
            throw new SampleException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "샘플이 없습니다.");
        }

        @PostMapping("/samples")
        void create(@RequestBody @Valid SampleRequest request) {}

        @GetMapping("/samples/header")
        void header(@RequestHeader("X-Sample-Id") String sampleId) {}

        @GetMapping("/samples/{id}")
        void byId(@PathVariable Long id) {}

        @GetMapping("/samples/constraint")
        void constraint() {
            throw new ConstraintViolationException("위반", Set.of());
        }

        @GetMapping("/samples/unexpected")
        void unexpected() {
            throw new IllegalStateException("내부 구현 정보");
        }
    }

    record SampleRequest(
            @NotBlank String name, @NotEmpty @Valid List<Item> items) {
        record Item(@NotNull @PositiveOrZero Long amount) {}
    }

    static class SampleException extends BusinessException {
        SampleException(ErrorCode errorCode, String detail) {
            super(errorCode, detail);
        }
    }
}
