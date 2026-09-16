plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

group = "de.ole101.lodestone"
version = providers.gradleProperty("lodestone_version").get()

repositories {
    mavenCentral()
}

dependencies {
    // This dependency is exported to consumers, that is to say found on their compile classpath.
    api(libs.commons.math3)

    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    implementation(libs.guava)
}

tasks {
    jar {
        val projectName = project.name
        inputs.property("projectName", projectName)

        from("LICENSE") {
            rename { "${it}_$projectName" }
        }
    }
}

java {
    withSourcesJar()
}

kotlin {
    jvmToolchain(25)
    explicitApi()
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])

            artifactId = "lodestone"
        }
    }

    repositories {
        val reposiliteUrl = providers.environmentVariable("REPOSILITE_URL").orNull
        if (!reposiliteUrl.isNullOrBlank()) {
            maven {
                name = "reposilite"
                url = uri(reposiliteUrl)

                credentials {
                    username = providers.environmentVariable("REPOSILITE_USERNAME").orNull
                    password = providers.environmentVariable("REPOSILITE_PASSWORD").orNull
                }

                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }
    }
}
