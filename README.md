# ApexFetch

[![Maven Central](https://img.shields.io/maven-central/v/io.github.muhamadsyafii/apexfetch-core.svg?label=Maven%20Central)](https://central.sonatype.com/namespace/io.github.muhamadsyafii)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform](https://img.shields.io/badge/platform-Android%20%7C%20JVM%20%7C%20Linux%20%7C%20Windows-lightgrey.svg)](#)

**ApexFetch** is an enterprise-grade download manager for **Kotlin Multiplatform (KMP)**.
It is designed to handle large-scale file downloads with built-in local database
persistence, automatic resume capabilities, and modern UI support for **Android**
(Jetpack Compose & XML View Systems) and **Desktop** (Windows & Linux).

---
<p align="center">
  <img src="https://github.com/user-attachments/assets/b65e28fe-98c7-4908-887e-73a149ef4221" width="32%" alt="androidfromcmp" />
  <img src="https://github.com/user-attachments/assets/5100554e-0eba-4953-87e4-9e453cfe6896" width="32%" alt="androidcompose"/>
  <img src="https://github.com/user-attachments/assets/631ca20a-2069-4432-92cb-c03022b583c5" width="32%" alt="androidnativexml" />
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/5cd41f78-a1a7-46a6-866b-84c490d831ae" width="80%" alt="apexfetchdesktop" />
</p>

---

## Project Architecture

ApexFetch is divided into modular components for maximum flexibility:

- **`apexfetch-core`** — The pure KMP logic core (Ktor, Okio, SQLDelight).
- **`apexfetch-compose`** — UI wrapper for Compose Multiplatform (`rememberApexDownload`).
- **`apexfetch-android-xml`** — Extensions for Android XML View Systems (LiveData & ViewModel).

---

## Features

- **Smart Resume** — Automatically resumes downloads from the last downloaded byte using HTTP Range headers.
- **Local Persistence** — Stores download history and real-time states in a local SQLite database.
- **Multiplatform** — Write once, run on Android, Windows, and Linux.
- **Lifecycle Aware** — Safely manages download scopes, specifically tailored for UI modules.
- **Disk-Efficient** — Writes streams directly to storage via Okio to prevent memory overhead and OOM crashes.

---

## Prerequisites

Ensure your development environment meets the following specifications:

| Requirement | Version |
|---|---|
| Kotlin | `2.1.0` |
| Android Gradle Plugin (AGP) | `8.11.2` |
| SQLDelight | `2.0.1` |
| Android `minSdk` | `21` (Android 5.0 Lollipop) |
| Android `compileSdk` | `34` (Android 14) |
| Java Source/Target | Java 8 (`VERSION_1_8`) |
| JDK (to run AGP 8.x) | `17+` |

**Supported Targets:** Android, JVM (Desktop)

---

## Installation

### Step 1 — Add the Repository

**Kotlin DSL (`settings.gradle.kts`)**

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

**Groovy (`settings.gradle`)**

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

### Step 2 — Add the Dependencies

Add the required modules to your app or module-level `build.gradle.kts` or `build.gradle`:

**Kotlin DSL**

```kotlin
dependencies {
    val apexVersion = "1.0.0"

    // Core engine (required)
    implementation("io.github.muhamadsyafii:apexfetch-core:$apexVersion")

    // For Jetpack Compose / Compose Multiplatform
    implementation("io.github.muhamadsyafii:apexfetch-compose:$apexVersion")

    // For Android XML (LiveData support)
    implementation("io.github.muhamadsyafii:apexfetch-android-xml:$apexVersion")
}
```

**Groovy**

```groovy
dependencies {
    def apexVersion = "1.0.0"

    // Core engine (required)
    implementation "io.github.muhamadsyafii:apexfetch-core:$apexVersion"
    
    // For Jetpack Compose / Compose Multiplatform
    implementation "io.github.muhamadsyafii:apexfetch-compose:$apexVersion"
    
    // For Android XML (LiveData support)
    implementation "io.github.muhamadsyafii:apexfetch-android-xml:$apexVersion"
}
```

---

## Usage

### Initialization

```kotlin
val fetcher = ApexFetcher(database = mySqlDelightDb)
```

### A. Jetpack Compose

```kotlin
val download = rememberApexDownload(fetcher, url, path)

Button(onClick = { download.start() }) {
    Text("Download Now")
}

if (download.state.value is DownloadState.Downloading) {
    val progress = (download.state.value as DownloadState.Downloading).progress
    LinearProgressIndicator(progress = progress / 100f)
}
```

### B. Android XML (LiveData)

```kotlin
fetcher.downloadAsLiveData(url, path).observe(viewLifecycleOwner) { state ->
    when (state) {
        is DownloadState.Downloading -> binding.progressBar.progress = state.progress
        is DownloadState.Success     -> binding.statusText.text = "Completed!"
        // Handle other states: Paused, Error, etc.
    }
}
```

---

## Contributing

Contributions are welcome! Please read our [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a pull request.

By contributing to this project, you agree that your contributions will be licensed under the [Apache License 2.0](LICENSE).

---


## License

```
Copyright 2026 Muhamad Syafii

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
