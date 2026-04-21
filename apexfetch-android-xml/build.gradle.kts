/*
 * Created by Muhamad Syafii
 * 21/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "com.kupil.apexfetch.xml"
  compileSdk = 34

  defaultConfig {
    minSdk = 21
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  // Aktifkan dukungan untuk menggunakan Custom View XML
  buildFeatures {
    viewBinding = true
  }
}

dependencies {
  api(project(":apexfetch-core"))
  implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
}