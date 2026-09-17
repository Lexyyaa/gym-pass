package com.gym.pass.support.web;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** 페이지 응답 공통 형식 (04 §1 페이지네이션). */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
