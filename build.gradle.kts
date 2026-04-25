plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.13")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.6")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("resources")
    }
    test {
        java.srcDirs("test")
    }
}

// The production code carries module-info.java, but the tests live in
// `org.oxoo2a.test` and are simplest to compile/run on the classpath.
// Disabling module-path inference for tests keeps the production module
// intact while letting JUnit reflectively discover tests without an
// `opens` directive.
tasks.compileTestJava {
    modularity.inferModulePath = false
}

tasks.test {
    modularity.inferModulePath = false
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
    }
}
