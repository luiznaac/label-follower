import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

val exposedVersion = "1.0.0-rc-1"

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
    // (portfolio-2/chameidor). Single Gradle module here (see backend/CLAUDE.md §2),
    // so tables/repositories live under integrations/database/ instead of a
    // separate `persistence` module.
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")
    implementation("com.mysql:mysql-connector-j:9.4.0")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.kotest:kotest-framework-engine-jvm:5.9.1")
    testImplementation("io.kotest:kotest-extensions-spring:4.4.3")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
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

// Only ship the executable Spring Boot jar — skip the extra `-plain.jar`.
tasks.named<Jar>("jar") {
    enabled = false
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
    include("*/.kt", "*/.kts")
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
