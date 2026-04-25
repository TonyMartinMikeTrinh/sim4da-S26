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

// The production code carries module-info.java and compiles as the
// JPMS module `org.oxoo2a.sim4da`. The test source set is deliberately
// kept on the classpath rather than the module path: tests get
// white-box access to internals when they need it (e.g. the
// RandomValuesTest exercises the package-private constructor of
// RandomValues), and JUnit can reflectively discover @Test methods
// without us having to add `opens` directives.
//
// The student-facing boundary — `org.oxoo2a.sim4da.internal` is
// unexported by `module-info.java` — is unaffected by this choice.
// When student code consumes sim4da as a real module, JPMS enforces
// the boundary at their compile site: `import
// org.oxoo2a.sim4da.internal.Network;` becomes a compile error.
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
