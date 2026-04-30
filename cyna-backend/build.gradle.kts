buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.flywaydb:flyway-database-postgresql:10.22.0")
        classpath("org.postgresql:postgresql:42.7.5")
    }
}

plugins {
    java
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.flywaydb.flyway") version "10.22.0"
}

group = "com.cyna"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // --- Spring Boot ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // --- Database ---
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // --- JWT ---
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // --- OpenAPI / Swagger ---
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.3")

    // --- Cache ---
    implementation("com.github.ben-manes.caffeine:caffeine")

    // --- Brevo ---
    implementation("com.konghq:unirest-java:3.13.6")

    // --- Thymeleaf ---
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    // --- Lombok ---
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // --- Testing ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:jdbc")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("PASSED", "FAILED", "SKIPPED")
    }
}

fun envOrDefault(name: String, defaultValue: String): String =
    providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() } ?: defaultValue

fun firstEnvOrDefault(defaultValue: String, vararg names: String): String {
    for (name in names) {
        val value = providers.environmentVariable(name).orNull
        if (!value.isNullOrBlank()) {
            return value
        }
    }
    return defaultValue
}

flyway {
    val dbHost = envOrDefault("DB_HOST", "localhost")
    val dbPort = envOrDefault("DB_PORT", "5432")
    val dbName = envOrDefault("DB_NAME", "cyna")

    url = firstEnvOrDefault(
        "jdbc:postgresql://$dbHost:$dbPort/$dbName",
        "FLYWAY_URL",
        "SPRING_DATASOURCE_URL",
        "JDBC_DATABASE_URL"
    )
    user = firstEnvOrDefault("cyna", "FLYWAY_USER", "SPRING_DATASOURCE_USERNAME", "DB_USERNAME")
    password = firstEnvOrDefault("cyna_dev_password", "FLYWAY_PASSWORD", "SPRING_DATASOURCE_PASSWORD", "DB_PASSWORD")
    schemas = arrayOf("user_schema", "product_schema", "cart_schema", "order_schema", "subscription_schema", "payment_schema")
}
