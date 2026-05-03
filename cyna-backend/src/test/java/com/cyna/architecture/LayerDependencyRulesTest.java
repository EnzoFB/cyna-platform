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
    @DisplayName("Domain layer must not depend on JPA / Jakarta Persistence")
    void domainMustNotDependOnJpa() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
                .because("The domain layer must not use persistence annotations")
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
    @DisplayName("Application layer must not depend on Spring cache APIs")
    void applicationMustNotDependOnSpringCache() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework.cache..")
                .because("Caching must stay in infrastructure to keep the application layer framework-agnostic")
                .check(classes);
    }
}
