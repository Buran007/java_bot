plugins {
    application
}

group = "ru.meetbot"
version = "0.1.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
    }
}

application {
    mainClass = "ru.meetbot.Main"
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8")
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs(application.applicationDefaultJvmArgs)
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

dependencyLocking {
    lockAllConfigurations()
}
