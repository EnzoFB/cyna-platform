package com.cyna.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the database-level half of the modular monolith: every module owns its
 * own Postgres schema and must read only that schema. Cross-schema access (joins
 * or reads of another module's tables) is forbidden — it cannot survive a split
 * into separately deployed databases. Cross-module data must instead flow through
 * {@code application.api} (the seam that becomes a remote call at extraction).
 *
 * <p>ArchUnit can't see this: schema names live in SQL string literals, not in the
 * type graph. So this test scans module source for {@code <name>_schema.} tokens
 * and fails if any module references a schema other than its own. The Flyway
 * migration is intentionally excluded — it creates every schema (and the
 * cross-schema FK constraints kept as documented extraction debt).
 */
@DisplayName("Schema Isolation Rules")
class SchemaIsolationRulesTest {

    private static final Path MODULES_ROOT = Paths.get("src/main/java/com/cyna/modules");

    private static final Pattern SCHEMA_REF = Pattern.compile("(\\w+)_schema\\.");

    /** Module name → the schema it is allowed to reference. Modules absent here own no schema. */
    private static final Map<String, String> OWN_SCHEMA = Map.of(
            "user", "user_schema",
            "product", "product_schema",
            "cart", "cart_schema",
            "order", "order_schema",
            "subscription", "subscription_schema",
            "payment", "payment_schema",
            "dashboard", "dashboard_schema"
    );

    @Test
    @DisplayName("A module's code must reference only its own Postgres schema")
    void noCrossSchemaReferencesInModuleCode() throws IOException {
        assertThat(Files.isDirectory(MODULES_ROOT))
                .as("modules source root must exist at %s (run from the cyna-backend project dir)", MODULES_ROOT.toAbsolutePath())
                .isTrue();

        List<String> violations = new ArrayList<>();
        int[] scanned = {0};

        try (Stream<Path> files = Files.walk(MODULES_ROOT)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(file -> {
                scanned[0]++;
                String module = moduleOf(file);
                String ownSchema = OWN_SCHEMA.get(module);
                String content = readString(file);

                Matcher matcher = SCHEMA_REF.matcher(content);
                while (matcher.find()) {
                    String referenced = matcher.group(1) + "_schema";
                    if (!referenced.equals(ownSchema)) {
                        violations.add(String.format(
                                "%s references %s — module '%s' owns %s",
                                MODULES_ROOT.relativize(file), referenced, module,
                                ownSchema == null ? "no schema" : ownSchema));
                    }
                }
            });
        }

        assertThat(scanned[0])
                .as("expected to scan module source files — none found, the scan path is likely wrong")
                .isGreaterThan(0);

        assertThat(violations)
                .as("Module code must read only its own schema; cross-module data goes through application.api")
                .isEmpty();
    }

    private static String moduleOf(Path file) {
        return MODULES_ROOT.relativize(file).getName(0).toString();
    }

    private static String readString(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
