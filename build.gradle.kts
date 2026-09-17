import sun.tools.jar.resources.jar

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
    implementation(libs.minestom)
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
