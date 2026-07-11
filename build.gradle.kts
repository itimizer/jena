import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort

plugins {
    java
    id("org.springframework.boot") version "4.0.7"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.github.spotbugs") version "6.5.8"
    checkstyle
}

group = "com.itimizer.jena"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

springBoot {
    buildInfo()
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.2")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
    implementation("net.lbruun.springboot:preliquibase-spring-boot-starter:2.0.0")
    implementation("org.liquibase:liquibase-core")
    implementation("org.springframework.boot:spring-boot-liquibase")
    implementation("org.mapstruct:mapstruct:1.6.3")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")

    runtimeOnly ("org.postgresql:postgresql")
    runtimeOnly ("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly ("io.jsonwebtoken:jjwt-jackson:0.13.0")


    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    compileOnly("org.projectlombok:lombok")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.10.2")
    annotationProcessor ("org.mapstruct:mapstruct-processor:1.6.3")

    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.13.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.awaitility:awaitility")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-security-test")
    testImplementation ("org.springframework.boot:spring-boot-testcontainers")
    testImplementation ("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation ("org.testcontainers:testcontainers-postgresql")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-javaagent:${classpath.filter { it.name.contains("mockito-core") }.single().absolutePath}")
}

checkstyle {
    toolVersion = "13.4.0"
    isIgnoreFailures = false
    configFile = file("custom_google_checks.xml")
}

spotbugs {
    toolVersion = "4.10.2"
    effort = Effort.MAX
    reportLevel = Confidence.MEDIUM
    ignoreFailures = false
    excludeFilter = file("spotbugs/config/exclude.xml")
}

tasks.spotbugsTest {
    enabled = false
}

tasks.spotbugsMain {
    reports.create("html") {
        required = true
    }
    reports.create("xml") {
        required = true
    }
}

tasks.withType<Javadoc> {
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:all,-missing", true)
}
