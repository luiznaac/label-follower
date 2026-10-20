import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.util.concurrent.TimeUnit

plugins {
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.kotest") version "0.4.11"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("com.github.ben-manes.versions") version "0.51.0"
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
}

group = "com.rafaelfo"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

// Flyway 12's tools.jackson:jackson-databind:3.x needs jackson-annotations 2.21+
// (JsonSerializeAs) — without this override, io.spring.dependency-management's BOM pins
// the whole com.fasterxml.jackson.core family, this one included, to 2.19.x and Flyway
// dies at runtime with a NoClassDefFoundError. A plain Gradle constraints{} block does
// NOT win against Spring's forced BOM resolution — has to go through its own DSL. Stays
// compatible with the older jackson-core/-databind Spring still manages (annotations
// rarely breaks against nearby core versions).
dependencyManagement {
    dependencies {
        dependency("com.fasterxml.jackson.core:jackson-annotations:2.21")
    }
}

val exposedVersion = "1.0.0-rc-1"
val flywayVersion = "12.11.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")

    // Persistence — MySQL via Exposed, same stack as the sibling Kotlin services
    // (portfolio-2/chameidor). Single Gradle module here (see backend/AGENTS.md "Architecture"),
    // so tables/repositories live under integrations/database/ instead of a
    // separate `persistence` module.
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")
    implementation("com.mysql:mysql-connector-j:9.4.0")

    // Schema migrations. Exposed's migration module only *generates* and *diffs* SQL (see
    // MigrationScripts.kt and the MigrationSchemaTest guard); Flyway is what actually applies
    // the V*.sql files under db/migration. Flyway stays on 12.x — 13 requires Java 21 and this
    // module compiles for 17.
    implementation("org.jetbrains.exposed:exposed-migration-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-migration-jdbc:$exposedVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-mysql:$flywayVersion")

    // Flyway 12 pulls tools.jackson:jackson-databind:3.x, which needs jackson-annotations
    // 2.21+ (JsonSerializeAs) — see the dependencyManagement override below.

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.kotest:kotest-framework-engine-jvm:5.9.1")
    testImplementation("io.kotest:kotest-extensions-spring:4.4.3")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.testcontainers:mysql:1.21.3")
    testImplementation("com.lemonappdev:konsist:0.17.3")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// The Docker-free verification lane. The pre-commit hook runs this: specs tagged `integration`
// (currently just MigrationSchemaTest) are excluded, so committing never needs a Docker daemon.
// `test` is unchanged and still runs everything — that is the CI lane, where Docker is present.
val unitTest by tasks.registering(Test::class) {
    group = "verification"
    description = "Runs the test source set with `integration`-tagged specs excluded — no Docker needed."
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    dependsOn(tasks.named("testClasses"))
    useJUnitPlatform {
        excludeTags("integration")
    }
    // Kotest 5.9 filters on its own `@Tags`, not on the JUnit platform tags `excludeTags` reads,
    // so the same marker has to be honoured through Kotest's tag expression as well.
    systemProperty("kotest.tags", "!integration")
}

// Testcontainers only discovers an unreachable daemon once MigrationSchemaTest starts its first
// container, which surfaces as an opaque DockerClientProviderStrategy error buried in the HTML
// report. Probe the daemon first so the full lane fails in seconds with a message that says what
// to do — including the Docker-free lane that never needs it.
val dockerPreflight by tasks.registering {
    group = "verification"
    description = "Fails fast, with an actionable message, when the Docker daemon is unavailable."

    doLast {
        fun failDockerUnavailable(reason: String, detail: String = "", cause: Throwable? = null): Nothing =
            throw GradleException(
                """
                The full test suite needs $reason
                Make Docker available and retry, or run the Docker-free lane: ./gradlew unitTest --no-daemon
                $detail
                """.trimIndent(),
                cause,
            )

        // Drain to a file rather than a pipe: a noisy `docker info` that filled the pipe buffer
        // would block the child and turn a healthy daemon into a false 30s timeout.
        val outputFile = File.createTempFile("docker-preflight", ".log")
        try {
            val process = try {
                ProcessBuilder("docker", "info")
                    .redirectErrorStream(true)
                    .redirectOutput(outputFile)
                    .start()
            } catch (exception: java.io.IOException) {
                failDockerUnavailable("Docker, but the `docker` CLI is not on PATH.", cause = exception)
            }
            // A wedged daemon would otherwise hang the preflight itself — the failure mode this
            // task exists to cut short.
            val finished = process.waitFor(30, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                process.waitFor(5, TimeUnit.SECONDS)
            }
            val output = outputFile.readText()
            val exitCode = if (finished) process.exitValue() else null
            if (exitCode != 0) {
                val detail = output.lines()
                    .filter { it.contains("error", ignoreCase = true) }
                    .take(3)
                    .ifEmpty { output.trim().lines().takeLast(3) }
                    .joinToString("\n")
                failDockerUnavailable(
                    reason = if (exitCode == null) {
                        "a running Docker daemon, but `docker info` did not return in 30s."
                    } else {
                        "a running Docker daemon, but `docker info` exited $exitCode."
                    },
                    detail = detail,
                )
            }
        } finally {
            outputFile.delete()
        }
    }
}

tasks.test {
    dependsOn(dockerPreflight)
}

// Only ship the executable Spring Boot jar — skip the extra `-plain.jar`.
tasks.named<Jar>("jar") {
    enabled = false
}

// config/migration/{Migrator,MigrationScripts}.kt each declare their own fun main() (run
// via the generateMigrationScript/migrate JavaExec tasks below, not this one) — without this,
// Boot can't auto-resolve which of the three is the actual app entry point for bootJar/bootRun.
springBoot {
    mainClass.set("com.rafaelfo.labelfollower.application.BootKt")
}

// Local dev convenience: load secrets from the repo-root .env (gitignored) into
// `./gradlew bootRun` so they don't have to be exported by hand. In Docker the
// real environment variables are injected by docker-compose instead. Values
// already present in the environment are left untouched.
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    val dotenv = file("../.env")
    if (dotenv.exists()) {
        dotenv.readLines()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
            .forEach { line ->
                val (key, rawValue) = line.split("=", limit = 2)
                val value = rawValue.trim().trim('"')
                if (value.isNotEmpty() && System.getenv(key.trim()) == null) {
                    environment(key.trim(), value)
                }
            }
    }
}

// io.spring.dependency-management manages every configuration by default, including
// detekt-gradle-plugin's own analysis-engine classpath — bumping its bundled Kotlin
// compiler to whatever this project uses (2.2.x) and breaking detekt's own runtime
// version check. Pin that one configuration back to what detekt was actually built
// against; see https://detekt.dev/docs/gettingstarted/gradle#dependencies.
configurations.named("detekt") {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("2.0.21")
        }
    }
}

tasks.withType<Detekt> {
    parallel = true
    disableDefaultRuleSets = true
    buildUponDefaultConfig = true
    autoCorrect = true
    ignoreFailures = false
    setSource(files(projectDir))
    include("**/*.kt", "**/*.kts")
    config.setFrom(files("$rootDir/config/detekt/config.yml", "$rootDir/config/detekt/format.yml"))
    reports {
        xml.required.set(false)
        html.required.set(true)
    }
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure {
    // optional parameters
    checkForGradleUpdate = true
    outputFormatter = "csv"
    outputDir = "build/dependencyUpdates"
    reportfileName = "report"
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

// Authoring half of the migration workflow (backend/AGENTS.md "Database migrations"). Diffs
// the Exposed tables in `allTables` against a local database already migrated to head, and writes
// the SQL that closes the gap into src/main/resources/db/migration/. Exposed only *generates* —
// Flyway applies.
//
//   docker compose -f docker-compose.yml up -d mysql
//   ./gradlew generateMigrationScript -Pname=V2__add_something
tasks.register<JavaExec>("generateMigrationScript") {
    group = "database"
    description = "Diff the Exposed tables against the local database into db/migration/<name>.sql"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.rafaelfo.labelfollower.config.migration.MigrationScriptsKt")
    systemProperty("migration.name", providers.gradleProperty("name").getOrElse(""))
}

// Applies db/migration/V*.sql to the local database — the same entry point deploy/entrypoint.sh
// runs in the container, so what you get locally is what production gets.
tasks.register<JavaExec>("migrate") {
    group = "database"
    description = "Apply pending migrations to the local database"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.rafaelfo.labelfollower.config.migration.MigratorKt")
}

// Flyway 12.4+ runs on Jackson 3 (`tools.jackson.*`), whose databind needs the 2.21 line of the
// shared `jackson-annotations` artifact — it reads @JsonSerializeAs while copying configuration
// extensions, on the `Flyway.configure().load()` path. io.spring.dependency-management otherwise
// pins that artifact to whatever Spring Boot manages (2.19.2 here), and Flyway dies with a
// NoClassDefFoundError before it can migrate anything. Annotations are the most compatible
// Jackson artifact and are safe to run ahead of databind, so only this one is forced; Spring's
// own Jackson 2 stack stays on the managed version.
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.fasterxml.jackson.core" && requested.name == "jackson-annotations") {
            useVersion("2.21")
        }
    }
}
