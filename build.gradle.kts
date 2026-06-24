// Root build — shared coordinates + repositories only.
// Module-specific plugins/deps live in analytics/ and api/.

allprojects {
    group = "com.liftlens"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
