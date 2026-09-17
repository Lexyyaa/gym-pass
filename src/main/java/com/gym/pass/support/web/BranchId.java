package com.gym.pass.support.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 붙이면 X-Branch-Id 헤더를 검증한 지점 ID(Long)가 들어온다.
 * 누락 · 숫자 아님 → 400 / 없는 지점 → 404 (04 §1).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface BranchId {}
