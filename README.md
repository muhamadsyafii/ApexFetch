# 🚀 ApexFetch

[![Maven Central](https://img.shields.io/maven-central/v/com.github.muhamadsyafii/apexfetch.svg)](https://search.maven.org/)
[![JitPack](https://jitpack.io/v/muhamadsyafii/apexfetch.svg)](https://jitpack.io/#muhamadsyafii/apexfetch)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform](https://img.shields.io/badge/platform-Android%20%7C%20%7C%20JVM%20%7C%20Linux%20%7C%20Windows-lightgrey.svg)](#)

**ApexFetch** is an Enterprise-grade Download Manager for **Kotlin Multiplatform (KMP)**. This library is designed to handle large-scale file downloads with built-in local database persistence, automatic resume capabilities, and modern UI support for both Jetpack Compose and legacy XML View Systems.

---

## 📺 Demo
![ApexFetch Demo](https://raw.githubusercontent.com/muhamadsyafii/apexfetch/main/assets/demo.gif)
> *Check out the full video demonstration here: [YouTube Link]*

---

## 🏗️ Project Architecture

ApexFetch is divided into modular components for maximum flexibility:

- **`apexfetch-core`**: The pure KMP logic core (Ktor, Okio, SQLDelight).
- **`apexfetch-compose`**: UI Wrapper for Compose Multiplatform (`rememberApexDownload`).
- **`apexfetch-android-xml`**: Extensions for Android XML View Systems (LiveData & ViewModel).

---

## ✨ Key Features
* **✅ Smart Resume**: Automatically resumes downloads from the last downloaded byte using HTTP Range headers.
* **✅ Local Persistence**: Permanently stores download history and real-time states in a local SQLite database.
* **✅ Multi-Platform**: Write once, run seamlessly on Android, Windows, Linux.
* **✅ Lifecycle Aware**: Safely manages download scopes (specifically tailored for UI modules).
* **✅ Disk-Efficient**: Writes streams directly to storage using Okio to prevent memory overhead and out-of-memory (OOM) crashes.

---

## 📋 Prerequisites

Before integrating the `apexfetch-core` library into your project, ensure your development environment meets the exact specifications defined in our build system:

* **Kotlin**: Version `2.1.0`
* **Android Gradle Plugin (AGP)**: Version `8.11.2`
* **SQLDelight**: Version `2.0.1`
* **Android SDK**: `minSdk 21` (Android 5.0 Lollipop) and `compileSdk 34` (Android 14).
* **Java**: Source and Target compatibility are set to Java 8 (`VERSION_1_8`). *(Note: You will need JDK 17+ installed on your machine to run AGP 8.x).*
* **Supported Targets**: Android and JVM (Desktop).
---


## 📦 Installation

### 1. Add the Repository

#### **Kotlin DSL (`settings.gradle.kts`)**
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("[https://jitpack.io](https://jitpack.io)") }
    }
}
```

#### **Groovy (`settings.gradle`)**
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("[https://jitpack.io](https://jitpack.io)") }
    }
}
```

### 2. Add the Dependencies
Add the required modules to your app/module level `build.gradle.kts` or `build.gradle`:


#### **Kotlin DSL (`settings.gradle.kts`)**
```kotlin
dependencies {
    val apexVersion = "1.0.0"
    
    // Core Engine (Required)
    implementation("com.github.muhamadsyafii:apexfetch-core:$apexVersion")
    
    // For Jetpack Compose / Compose Multiplatform
    implementation("com.github.muhamadsyafii:apexfetch-compose:$apexVersion")
    
    // For Android XML (LiveData support)
    implementation("com.github.muhamadsyafii:apexfetch-android-xml:$apexVersion")
}
```

#### **Groovy (`settings.gradle`)**
```Groovy
dependencies {
    def apexVersion = "1.0.0"
    
    implementation "com.github.muhamadsyafii:apexfetch-core:$apexVersion"
    implementation "com.github.muhamadsyafii:apexfetch-compose:$apexVersion"
    implementation "com.github.muhamadsyafii:apexfetch-android-xml:$apexVersion"
}
```


## 📦 Installation
### Initialization

```kotlin
val fetcher = ApexFetcher(database = mySqlDelightDb)
```

#### **A. Jetpack Compose**
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
#### **B. Android XML (LiveData)**
```kotlin
fetcher.downloadAsLiveData(url, path).observe(viewLifecycleOwner) { state ->
    when(state) {
        is DownloadState.Downloading -> binding.progressBar.progress = state.progress
        is DownloadState.Success -> binding.statusText.text = "Completed!"
        // Handle other states like Paused, Error, etc.
    }
}
```



