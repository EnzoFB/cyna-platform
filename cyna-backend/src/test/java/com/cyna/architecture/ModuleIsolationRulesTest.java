package com.cyna.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@DisplayName("Module Isolation Rules")
class ModuleIsolationRulesTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.cyna");
    }

    @Test
    @DisplayName("User module must not access Product module internals")
    void userMustNotAccessProduct() {
        noClasses()
                .that().resideInAPackage("com.cyna.user..")
                .should().dependOnClassesThat().resideInAPackage("com.cyna.product.domain..")
                .orShould().dependOnClassesThat().resideInAPackage("com.cyna.product.infrastructure..")
                .because("Modules communicate only through public APIs (application.api)")
                .check(classes);
    }

    @Test
    @DisplayName("User module must not access Order module internals")
    void userMustNotAccessOrder() {
        noClasses()
                .that().resideInAPackage("com.cyna.user..")
                .should().dependOnClassesThat().resideInAPackage("com.cyna.order.domain..")
                .orShould().dependOnClassesThat().resideInAPackage("com.cyna.order.infrastructure..")
                .because("Modules communicate only through public APIs (application.api)")
                .check(classes);
    }

    @Test
    @DisplayName("Product module must not access User module internals")
    void productMustNotAccessUser() {
        noClasses()
                .that().resideInAPackage("com.cyna.product..")
                .should().dependOnClassesThat().resideInAPackage("com.cyna.user.domain..")
                .orShould().dependOnClassesThat().resideInAPackage("com.cyna.user.infrastructure..")
                .because("Modules communicate only through public APIs (application.api)")
                .check(classes);
    }

    @Test
    @DisplayName("Order module must not access User module internals")
    void orderMustNotAccessUserInternals() {
        noClasses()
                .that().resideInAPackage("com.cyna.order..")
                .should().dependOnClassesThat().resideInAPackage("com.cyna.user.domain..")
                .orShould().dependOnClassesThat().resideInAPackage("com.cyna.user.infrastructure..")
                .because("Order module references users only through CustomerId, not User domain")
                .check(classes);
    }

    @Test
    @DisplayName("Payment module must not access Order module internals beyond events")
    void paymentMustNotAccessOrderInternals() {
        noClasses()
                .that().resideInAPackage("com.cyna.payment..")
                .should().dependOnClassesThat().resideInAPackage("com.cyna.order.domain.model..")
                .orShould().dependOnClassesThat().resideInAPackage("com.cyna.order.infrastructure..")
                .because("Payment reacts to Order events via Published Language, not direct access")
                .check(classes);
    }
}
