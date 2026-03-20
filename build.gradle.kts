// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    id("org.sonarqube") version "6.3.1.5724"
}

sonar {
    properties {
        property("sonar.projectKey", "elainedb_android_claude")
        property("sonar.organization", "elainedb")
        property("sonar.sourceEncoding", "UTF-8")
    }
}

subprojects {
    sonar {
        properties {
            if (name == "app") {
                property("sonar.sources", "src/main/java")
                property("sonar.tests", "src/test/java")
                property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/JacocoDebugCodeCoverage/JacocoDebugCodeCoverage.xml")
            }
        }
    }
}

tasks.register("jacocoTestReport") {
    dependsOn(":app:JacocoDebugCodeCoverage")
    group = "verification"
    description = "Generate Jacoco coverage reports for all modules"
}