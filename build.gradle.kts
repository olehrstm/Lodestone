plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

group = "de.ole101.lodestone"
version = providers.gradleProperty("lodestone_version")
    .orElse(
        providers.fileContents(layout.projectDirectory.file("version.txt"))
            .asText
            .map { "${it.trim()}-development" }
    )
    .get()

repositories {
    mavenCentral()
}

dependencies {
    api(libs.minestom)
    api(libs.adventure.text.minimessage)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
}

tasks {
    jar {
        val projectName = project.name
        inputs.property("projectName", projectName)

        from("LICENSE") {
            rename { "${it}_$projectName" }
        }
    }

    test {
        useJUnitPlatform()
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
