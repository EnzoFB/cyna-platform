package com.cyna.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Enforces the modular-monolith isolation rules so the codebase stays
 * "split-ready" for an eventual extraction into microservices.
 *
 * <p>A module ({@code com.cyna.modules.X}) may reach another module
 * ({@code com.cyna.modules.Y}) through one of only two published channels:
 * <ol>
 *   <li><b>Synchronous</b> — the other module's public API,
 *       {@code com.cyna.modules.Y.application.api..}</li>
 *   <li><b>Asynchronous</b> — the other module's published domain events,
 *       {@code com.cyna.modules.Y.domain.event..}</li>
 * </ol>
 * Any other cross-module dependency (its {@code domain.model}, its
 * {@code infrastructure}, its {@code interfaces}, or the rest of its
 * {@code application}) couples the modules at a point that would break when
 * each module gets its own process and database.
 *
 * <p>Unlike the previous hand-written one-rule-per-pair approach, the rule
 * below is generic: it automatically covers every present and future module
 * pair, so a newly added module cannot silently bypass isolation.
 */
@DisplayName("Module Isolation Rules")
class ModuleIsolationRulesTest {

    private static final String MODULE_ROOT = "com.cyna.modules.";

    private static JavaClasses classes;

    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.cyna");
    }

    @Test
    @DisplayName("Modules must talk to each other only through application.api or published domain events")
    void modulesCommunicateOnlyThroughPublishedChannels() {
        classes()
                .that().resideInAPackage("com.cyna.modules..")
                .should(onlyAccessOtherModulesThroughPublishedChannels())
                .because("Modules must stay independently deployable: cross-module access goes through "
                        + "application.api (sync) or domain.event (async) only")
                .check(classes);
    }

    @Test
    @DisplayName("Modules must be free of cyclic dependencies")
    void modulesAreFreeOfCycles() {
        slices()
                .matching(MODULE_ROOT + "(*)..")
                .should().beFreeOfCycles()
                .because("Cyclic module dependencies cannot be split into separately deployable services")
                .check(classes);
    }

    @Test
    @DisplayName("The shared kernel must not depend on any module")
    void sharedKernelMustNotDependOnModules() {
        noClasses()
                .that().resideInAPackage("com.cyna.shared..")
                .should().dependOnClassesThat().resideInAPackage("com.cyna.modules..")
                .because("The shared kernel is the stable base every module builds on; depending on a module "
                        + "inverts that, creates a shared↔module cycle, and pulls module internals into the "
                        + "common layer. Cross-cutting concerns implemented by a module (e.g. token validation) "
                        + "must be exposed to shared through an SPI defined in shared")
                .check(classes);
    }

    /**
     * Fails for every dependency from one module onto another module's
     * non-published internals. {@code shared} and third-party packages are
     * ignored (they resolve to a {@code null} module).
     */
    private static ArchCondition<JavaClass> onlyAccessOtherModulesThroughPublishedChannels() {
        return new ArchCondition<>(
                "access other modules only through application.api or domain.event") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                String originModule = moduleOf(origin.getPackageName());
                if (originModule == null) {
                    return;
                }
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    String targetPackage = dependency.getTargetClass().getPackageName();
                    String targetModule = moduleOf(targetPackage);
                    if (targetModule == null || targetModule.equals(originModule)) {
                        continue; // same module, shared kernel, or third-party — allowed
                    }
                    String apiPrefix = MODULE_ROOT + targetModule + ".application.api";
                    String eventPrefix = MODULE_ROOT + targetModule + ".domain.event";
                    boolean published =
                            targetPackage.equals(apiPrefix) || targetPackage.startsWith(apiPrefix + ".")
                            || targetPackage.equals(eventPrefix) || targetPackage.startsWith(eventPrefix + ".");
                    if (!published) {
                        events.add(SimpleConditionEvent.violated(origin, dependency.getDescription()));
                    }
                }
            }
        };
    }

    /**
     * Returns the module name for a {@code com.cyna.modules.X...} package, or
     * {@code null} for anything outside the module tree (shared kernel,
     * third-party libraries, the JDK).
     */
    private static String moduleOf(String packageName) {
        if (packageName == null || !packageName.startsWith(MODULE_ROOT)) {
            return null;
        }
        String rest = packageName.substring(MODULE_ROOT.length());
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }
}
