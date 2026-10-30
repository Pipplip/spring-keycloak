import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import org.openapitools.generator.gradle.plugin.tasks.ValidateTask

plugins {
    id("org.springframework.boot") version "3.5.16"
    id("org.openapi.generator") version "7.26.0"
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    kotlin("plugin.jpa") version "2.3.21"
}

group = "de.pipplip"
version = "0.0.1-SNAPSHOT"

val springBootVersion = "3.5.16"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.flywaydb:flyway-core")
    implementation(kotlin("reflect"))

    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

val openApiSpec = rootProject.file("../openapi/api.yaml").absolutePath
val generatedKotlinOpenApi = layout.buildDirectory.dir("generated/openapi/kotlin-spring")
val generatedAngularOpenApi = rootProject.file("../frontend/build/generated/openapi/typescript-angular")

tasks.named<ValidateTask>("openApiValidate") {
    inputSpec.set(openApiSpec)
}

tasks.register<GenerateTask>("generateKotlinOpenApi") {
    dependsOn(tasks.named("openApiValidate"))
    generatorName.set("kotlin-spring")
    inputSpec.set(openApiSpec)
    outputDir.set(generatedKotlinOpenApi.get().asFile.absolutePath)
    apiPackage.set("de.pipplip.abfallcontainerservice.generated.api")
    modelPackage.set("de.pipplip.abfallcontainerservice.generated.model")
    configOptions.set(
        mapOf(
            "delegatePattern" to "true",
            "documentationProvider" to "none",
            "generateJsonIncludeAnnotations" to "false",
            "generateJsonSetterNullsAnnotations" to "false",
            "hideGenerationTimestamp" to "true",
            "interfaceOnly" to "true",
            "useBeanValidation" to "true",
            "useSpringBoot3" to "true",
            "useTags" to "true",
            "annotationLibrary" to "none",
        ),
    )
    globalProperties.set(
        mapOf(
            "apiTests" to "false",
            "modelTests" to "false",
        ),
    )
}

tasks.register<GenerateTask>("generateAngularOpenApi") {
    dependsOn(tasks.named("openApiValidate"))
    generatorName.set("typescript-angular")
    inputSpec.set(openApiSpec)
    outputDir.set(generatedAngularOpenApi.absolutePath)
    configOptions.set(
        mapOf(
            "hideGenerationTimestamp" to "true",
        ),
    )
    globalProperties.set(
        mapOf(
            "apiTests" to "false",
            "modelTests" to "false",
        ),
    )
}

tasks.register("validateAngularOpenApiOutput") {
    dependsOn("generateAngularOpenApi")
    doLast {
        val requiredFiles =
            listOf(
                "api.module.ts",
                "index.ts",
                "api/catalogue.service.ts",
                "api/availability.service.ts",
                "api/bookings.service.ts",
                "api/administration.service.ts",
                "api/invoices.service.ts",
                "model/address.ts",
                "model/booking.ts",
                "model/containerAssignmentPage.ts",
                "model/problemDetail.ts",
            )
        val missingFiles =
            requiredFiles.filterNot { relativePath ->
                generatedAngularOpenApi.resolve(relativePath).isFile
            }

        check(missingFiles.isEmpty()) {
            "Angular client generation is missing required files: ${missingFiles.joinToString()}"
        }
    }
}

sourceSets {
    main {
        kotlin.srcDir(generatedKotlinOpenApi.map { it.dir("src/main/kotlin") })
    }
}

tasks.named("compileKotlin") {
    dependsOn("generateKotlinOpenApi")
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
