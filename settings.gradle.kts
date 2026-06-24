plugins {
    // Lets the Java toolchain auto-provision a JDK 21 when one isn't installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "lift-lens"

include("analytics", "api")
