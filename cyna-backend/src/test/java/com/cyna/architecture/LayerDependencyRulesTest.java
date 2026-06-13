package com.cyna.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@DisplayName("Layer Dependency Rules")
class LayerDependencyRulesTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.cyna");
    }

    // ---------------------------------------------------------------------
    // Domain layer — must stay a pure, framework-free inner layer.
    // It may depend only on java.* and com.cyna.shared.domain.*
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Domain layer must not depend on Spring framework")
    void domainMustNotDependOnSpring() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .because("The domain layer must be framework-independent")
                .check(classes);
    }

    @Test
    @DisplayName("Domain layer must not depend on JPA / Jakarta or javax Persistence")
    void domainMustNotDependOnJpa() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.persistence..",
                        "javax.persistence..")
                .because("The domain layer must not use persistence annotations — JPA lives in *JpaEntity")
                .check(classes);
    }

    @Test
    @DisplayName("Domain layer must not depend on Hibernate")
    void domainMustNotDependOnHibernate() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("org.hibernate..")
                .because("The domain layer must not be coupled to the ORM implementation")
                .check(classes);
    }

    @Test
    @DisplayName("Domain layer must not depend on Bean Validation (jakarta/javax.validation)")
    void domainMustNotDependOnBeanValidation() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.validation..",
                        "javax.validation..")
                .because("Domain invariants are expressed with constructors, Guard and Result — not @NotNull")
                .check(classes);
    }

    @Test
    @DisplayName("Domain layer must not depend on the application layer")
    void domainMustNotDependOnApplication() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..application..")
                .because("Clean Architecture: inner layers never depend on outer layers")
                .check(classes);
    }

    @Test
    @DisplayName("Domain layer must not depend on the interfaces layer")
    void domainMustNotDependOnInterfaces() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..interfaces..")
                .because("Clean Architecture: inner layers never depend on outer layers")
                .check(classes);
    }

    @Test
    @DisplayName("Domain layer must not depend on infrastructure layer")
    void domainMustNotDependOnInfrastructure() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .because("The domain layer must not know about infrastructure implementations")
                .check(classes);
    }

    // ---------------------------------------------------------------------
    // Application layer — orchestrates the domain; stays free of the web,
    // persistence and presentation frameworks.
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Application layer must not depend on infrastructure layer")
    void applicationMustNotDependOnInfrastructure() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .because("The application layer must not know about infrastructure implementations")
                .check(classes);
    }

    @Test
    @DisplayName("Application layer must not depend on interfaces layer")
    void applicationMustNotDependOnInterfaces() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..interfaces..")
                .because("The application layer must not know about REST controllers or DTOs")
                .check(classes);
    }

    @Test
    @DisplayName("Application layer must not depend on JPA / Jakarta or javax Persistence")
    void applicationMustNotDependOnJpa() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.persistence..",
                        "javax.persistence..")
                .because("Persistence is an infrastructure concern — application depends on repository ports only")
                .check(classes);
    }

    @Test
    @DisplayName("Application layer must not depend on Spring Web")
    void applicationMustNotDependOnSpringWeb() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework.web..")
                .because("HTTP concerns belong to the interfaces layer, not the application layer")
                .check(classes);
    }

    @Test
    @DisplayName("Application layer must not depend on Spring cache APIs")
    void applicationMustNotDependOnSpringCache() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework.cache..")
                .because("Caching must stay in infrastructure to keep the application layer framework-agnostic")
                .check(classes);
    }

    // ---------------------------------------------------------------------
    // Interfaces layer — controllers and DTOs. Talk to the application layer
    // only: never the domain directly, never infrastructure.
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Interfaces layer must not depend on infrastructure layer")
    void interfacesMustNotDependOnInfrastructure() {
        noClasses()
                .that().resideInAPackage("..interfaces..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .because("Controllers must depend on the application layer, not infrastructure adapters")
                .check(classes);
    }

    @Test
    @DisplayName("Interfaces layer must not depend on the domain layer directly")
    void interfacesMustNotDependOnDomain() {
        noClasses()
                .that().resideInAPackage("..interfaces..")
                .should().dependOnClassesThat().resideInAPackage("..domain..")
                .because("Controllers and DTOs go through the application layer — they must not reuse domain types")
                .check(classes);
    }
}
