# Lodestone

An extensive open-source Kotlin library for [Minestom](https://minestom.net) 26.2.

## Installation
![GitHub Release](https://img.shields.io/github/v/release/olehrstm/Lodestone)

`build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
    maven("https://repo.ole101.de/public")
}

dependencies {
    implementation("de.ole101.lodestone:lodestone:<version>")
}
```

Or with a version catalog - `gradle/libs.versions.toml`:

```toml
[versions]
lodestone = "<version>"

[libraries]
lodestone = { module = "de.ole101.lodestone:lodestone", version.ref = "lodestone" }
```
