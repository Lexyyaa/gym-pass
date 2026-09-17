package com.gym.pass.support.web;

import com.gym.pass.domain.branch.BranchRepository;
import com.gym.pass.domain.exception.CommonException;
import com.gym.pass.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** X-Branch-Id 헤더를 지점 ID로 해석하고 지점 존재를 확인한다 (D-10 · NFR-1). */
@Component
@RequiredArgsConstructor
public class BranchIdArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String HEADER = "X-Branch-Id";

    private final BranchRepository branchRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(BranchId.class) && parameter.getParameterType() == Long.class;
    }

    @Override
    public Long resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        Long branchId = parse(webRequest.getHeader(HEADER));
        return branchRepository.getById(branchId).getId();
    }

    private static Long parse(String header) {
        if (header == null || !header.matches("\\d{1,18}")) {
            throw new CommonException(ErrorCode.COMMON_INVALID_INPUT, HEADER + ": 숫자 지점 ID가 필요합니다.");
        }
        return Long.parseLong(header);
    }
}
