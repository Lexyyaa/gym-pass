package com.gym.pass.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Service·API 통합 테스트용. 모든 통합 테스트가 이 애너테이션 하나만 쓰면
 * 스프링 컨텍스트와 MySQL 컨테이너가 테스트 클래스 사이에서 재사용된다.
 * 구성을 바꾸는 애너테이션(@MockitoBean, @TestPropertySource 등)을 개별 클래스에 붙이면 컨텍스트가 새로 뜬다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public @interface IntegrationTest {}
