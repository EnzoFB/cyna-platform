# Architecture Tests

## Purpose

This document defines the ArchUnit-based architecture tests that enforce the CYNA Platform's architectural rules. These tests run in CI and **fail the build** if any rule is violated.

---

## What Is ArchUnit?

**ArchUnit** is a Java testing library that lets you define and verify architectural rules as unit tests. It analyzes compiled bytecode to check package dependencies, class naming, annotation usage, and layer boundaries.

---

## Setup

### Gradle Dependency

```kotlin
testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
```

### Base Test Class

```java
package com.cyna;

import com.tngtech.archunit.junit.AnalyzeClasses;

@AnalyzeClasses(packages = "com.cyna")
class ArchitectureTestBase {}
```

---

## Test Categories

### 1. Layer Dependency Rules

These tests enforce the Clean Architecture dependency direction.

```java
@AnalyzeClasses(packages = "com.cyna")
class LayerDependencyRulesTest {

    // --- DOMAIN LAYER RULES ---

    @ArchTest
    static final ArchRule domain_should_not_depend_on_spring =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.springframework..")
            .as("Domain layer must not depend on Spring");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_jpa =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("jakarta.persistence..", "javax.persistence..")
            .as("Domain layer must not depend on JPA");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_jakarta_validation =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("jakarta.validation..")
            .as("Domain layer must not depend on Jakarta Validation");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_hibernate =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.hibernate..")
            .as("Domain layer must not depend on Hibernate");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_application =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..application..")
            .as("Domain layer must not depend on Application layer");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .as("Domain layer must not depend on Infrastructure layer");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_interfaces =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..interfaces..")
            .as("Domain layer must not depend on Interfaces layer");

    // --- APPLICATION LAYER RULES ---

    @ArchTest
    static final ArchRule application_should_not_depend_on_infrastructure =
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .as("Application layer must not depend on Infrastructure layer");

    @ArchTest
    static final ArchRule application_should_not_depend_on_interfaces =
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..interfaces..")
            .as("Application layer must not depend on Interfaces layer");

    @ArchTest
    static final ArchRule application_should_not_depend_on_jpa =
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("jakarta.persistence..", "javax.persistence..")
            .as("Application layer must not depend on JPA");

    // --- INTERFACES LAYER RULES ---

    @ArchTest
    static final ArchRule interfaces_should_not_depend_on_infrastructure =
        noClasses()
            .that().resideInAPackage("..interfaces..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .as("Interfaces layer must not depend on Infrastructure layer");
}
```

### 2. Module Isolation Rules

These tests prevent cross-module dependency violations.

```java
@AnalyzeClasses(packages = "com.cyna")
class ModuleIsolationRulesTest {

    @ArchTest
    static final ArchRule modules_should_not_access_other_modules_domain =
        slices().matching("com.cyna.(*).domain..")
            .should().notDependOnEachOther()
            .as("Module domains must not depend on each other");

    @ArchTest
    static final ArchRule modules_should_not_access_other_modules_infrastructure =
        slices().matching("com.cyna.(*).infrastructure..")
            .should().notDependOnEachOther()
            .as("Module infrastructures must not depend on each other");

    @ArchTest
    static final ArchRule modules_should_not_access_other_modules_interfaces =
        slices().matching("com.cyna.(*).interfaces..")
            .should().notDependOnEachOther()
            .as("Module interfaces must not depend on each other");

    @ArchTest
    static final ArchRule cross_module_access_only_through_api =
        noClasses()
            .that().resideInAPackage("com.cyna.order..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.cyna.product.domain..")
            .as("Order module must not access Product domain directly — use application.api");
}
```

### 3. Naming Convention Rules

```java
@AnalyzeClasses(packages = "com.cyna")
class NamingConventionRulesTest {

    @ArchTest
    static final ArchRule controllers_should_be_suffixed =
        classes()
            .that().resideInAPackage("..interfaces.rest..")
            .and().areAnnotatedWith(RestController.class)
            .should().haveSimpleNameEndingWith("Controller")
            .as("REST controllers must end with 'Controller'");

    @ArchTest
    static final ArchRule command_handlers_should_be_suffixed =
        classes()
            .that().implement(CommandHandler.class)
            .should().haveSimpleNameEndingWith("CommandHandler")
            .as("Command handlers must end with 'CommandHandler'");

    @ArchTest
    static final ArchRule query_handlers_should_be_suffixed =
        classes()
            .that().implement(QueryHandler.class)
            .should().haveSimpleNameEndingWith("QueryHandler")
            .as("Query handlers must end with 'QueryHandler'");

    @ArchTest
    static final ArchRule jpa_entities_should_be_suffixed =
        classes()
            .that().areAnnotatedWith(Entity.class)
            .should().haveSimpleNameEndingWith("JpaEntity")
            .as("JPA entities must end with 'JpaEntity'");

    @ArchTest
    static final ArchRule repository_adapters_should_be_prefixed =
        classes()
            .that().resideInAPackage("..infrastructure.persistence.repository..")
            .and().implement(assignableTo("..domain.repository.."))
            .should().haveSimpleNameStartingWith("Jpa")
            .as("Repository adapters must start with 'Jpa'");

    @ArchTest
    static final ArchRule domain_events_should_implement_interface =
        classes()
            .that().resideInAPackage("..domain.event..")
            .should().implement(DomainEvent.class)
            .as("Domain events must implement DomainEvent");
}
```

### 4. Annotation Placement Rules

```java
@AnalyzeClasses(packages = "com.cyna")
class AnnotationPlacementRulesTest {

    @ArchTest
    static final ArchRule jpa_annotations_only_in_infrastructure =
        noClasses()
            .that().resideOutsideOfPackage("..infrastructure..")
            .should().beAnnotatedWith("jakarta.persistence.Entity")
            .as("@Entity annotation must only be in infrastructure layer");

    @ArchTest
    static final ArchRule rest_controller_only_in_interfaces =
        noClasses()
            .that().resideOutsideOfPackage("..interfaces..")
            .should().beAnnotatedWith(RestController.class)
            .as("@RestController must only be in interfaces layer");

    @ArchTest
    static final ArchRule validation_annotations_only_in_interfaces =
        noClasses()
            .that().resideOutsideOfPackage("..interfaces..")
            .should().dependOnClassesThat()
            .resideInAPackage("jakarta.validation..")
            .as("Jakarta Validation annotations must only be in interfaces layer")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule transactional_not_in_domain_or_application =
        noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
            .as("@Transactional must not be used in domain or application layers");
}
```

### 5. Structural Rules

```java
@AnalyzeClasses(packages = "com.cyna")
class StructuralRulesTest {

    @ArchTest
    static final ArchRule controllers_should_only_depend_on_mediator =
        classes()
            .that().areAnnotatedWith(RestController.class)
            .should().onlyHaveDependentClassesThat()
            .areAssignableTo(Mediator.class)
            .orShould().onlyHaveDependentClassesThat()
            .resideInAPackage("..interfaces..")
            .as("Controllers should depend only on Mediator for dispatching");

    @ArchTest
    static final ArchRule domain_classes_should_not_use_field_injection =
        noFields()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..")
            .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .as("Domain classes must not use Spring field injection");

    @ArchTest
    static final ArchRule aggregate_roots_should_extend_base_class =
        classes()
            .that().resideInAPackage("..domain.model..")
            .and().haveSimpleNameNotEndingWith("Id")
            .and().haveSimpleNameNotEndingWith("Status")
            .and().areNotRecords()
            .and().areNotEnums()
            .should().beAssignableTo(AggregateRoot.class)
            .orShould().beAssignableTo(Entity.class)
            .orShould().beAssignableTo(ValueObject.class)
            .as("Domain model classes should extend AggregateRoot, Entity, or ValueObject");
}
```

---

## Running Architecture Tests

### Local

```bash
./gradlew test --tests "*ArchitectureTest*"
./gradlew test --tests "*LayerDependencyRulesTest*"
```

### CI Pipeline

Architecture tests run as part of the standard test suite:

```bash
./gradlew test
```

A failure in any architecture test **blocks the pull request from being merged**.

---

## Adding New Rules

When a new architectural constraint is introduced:

1. Define the rule in the appropriate test class.
2. Verify it passes against the current codebase.
3. Add it to CI.
4. Document it in the relevant architecture document.
5. Announce to the team.

---

## Troubleshooting Failures

### "No classes match the pattern"

The package pattern may be wrong. Verify with:

```java
@Test
void verify_packages_exist() {
    JavaClasses classes = new ClassFileImporter().importPackages("com.cyna");
    assertThat(classes).isNotEmpty();
}
```

### "Rule violated by N classes"

ArchUnit outputs the violating classes. Read the violation message to identify:
- Which class is in the wrong layer.
- Which dependency is forbidden.
- Fix the code, not the test (unless the rule is wrong).

---

## Rules Summary

| Rule Category | Purpose |
|---------------|---------|
| Layer dependencies | Enforce Clean Architecture direction |
| Module isolation | Prevent cross-module coupling |
| Naming conventions | Enforce consistent naming |
| Annotation placement | Ensure annotations are in correct layers |
| Structural rules | Enforce design patterns (Mediator, base classes) |

---

## Related Documents

- [Dependency Rules](../architecture/dependency-rules.md)
- [Module Structure](../architecture/module-structure.md)
- [Testing Strategy](testing-strategy.md)
- [Coding Standards](../development/coding-standards.md)
