package com.gym.pass.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * CLAUDE.md의 "도구가 검증한다" 규칙. 규칙을 바꾸려면 문서가 아니라 여기를 고친다.
 */
@AnalyzeClasses(packages = "com.gym.pass", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String PRESENTATION = "..presentation..";
    private static final String APPLICATION = "..application..";
    private static final String DOMAIN = "..domain..";
    private static final String INFRASTRUCTURE = "..infrastructure..";
    private static final String SUPPORT = "..support..";

    // presentation → application → domain ← infrastructure
    // presentation(Request/Response)은 domain을 모른다. 변환은 application의 Command/Info가 맡는다
    // MQ 리스너·스케줄러 같은 진입점도 application을 호출하므로 presentation에 둔다
    @ArchTest
    static final ArchRule 레이어_의존_방향 = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Presentation")
            .definedBy(PRESENTATION)
            .layer("Application")
            .definedBy(APPLICATION)
            .layer("Domain")
            .definedBy(DOMAIN)
            .layer("Infrastructure")
            .definedBy(INFRASTRUCTURE)
            .layer("Support")
            .definedBy(SUPPORT)
            .whereLayer("Presentation")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Application")
            .mayOnlyBeAccessedByLayers("Presentation")
            .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Support")
            .whereLayer("Infrastructure")
            .mayNotBeAccessedByAnyLayer()
            // domain은 properties·config를 직접 받지 않는다. 정책값은 application이 읽어 인자로 넘긴다
            .whereLayer("Support")
            .mayOnlyBeAccessedByLayers("Presentation", "Application", "Infrastructure");

    @ArchTest
    static final ArchRule 도메인은_웹_계층을_모른다 = noClasses()
            .that()
            .resideInAPackage(DOMAIN)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework.web..", "jakarta.servlet..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule 컨트롤러는_presentation에 = classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .resideInAPackage(PRESENTATION)
            .andShould()
            .haveSimpleNameEndingWith("Controller")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule 예외_응답_매핑은_support에 = classes()
            .that()
            .areAnnotatedWith(RestControllerAdvice.class)
            .should()
            .resideInAPackage(SUPPORT)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule 애플리케이션_서비스_이름 = classes()
            .that()
            .resideInAPackage(APPLICATION)
            .and()
            .haveSimpleNameEndingWith("Service")
            .should()
            .haveSimpleNameEndingWith("ApplicationService")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule 엔티티는_domain에 = classes()
            .that()
            .areAnnotatedWith(Entity.class)
            .should()
            .resideInAPackage(DOMAIN)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule 스프링데이터_인터페이스는_infrastructure에 = classes()
            .that()
            .areInterfaces()
            .and()
            .areAssignableTo(org.springframework.data.repository.Repository.class)
            .should()
            .resideInAPackage(INFRASTRUCTURE)
            .andShould()
            .haveSimpleNameEndingWith("JpaRepository")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule 리포지토리_구현은_infrastructure에 = classes()
            .that()
            .areAnnotatedWith(Repository.class)
            .should()
            .resideInAPackage(INFRASTRUCTURE)
            .andShould()
            .haveSimpleNameEndingWith("RepositoryImpl")
            .allowEmptyShould(true);
}
