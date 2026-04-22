/*
 * Created by Muhamad Syafii
 * 21/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlinAndroid)
}

android {
  namespace = "com.kupil.apexfetch.xml"
  compileSdk = 34

  defaultConfig {
    minSdk = 21
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    viewBinding = true
  }

  kotlinOptions {
    jvmTarget = "17"
  }

}

dependencies {
  api(project(":apexfetch-core"))
  implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
}