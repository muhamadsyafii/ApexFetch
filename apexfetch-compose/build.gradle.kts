/*
 * Created by Muhamad Syafii
 * 21/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
}

kotlin {
  androidTarget {
    publishLibraryVariants("release")
  }
  jvm()

  sourceSets {
    commonMain.dependencies {
      api(project(":apexfetch-core"))
      implementation(libs.compose.runtime)
      implementation(libs.compose.foundation)
      implementation(libs.compose.ui)
    }
  }
}

android {
  namespace = "com.kupil.apexfetch.compose"
  compileSdk = 34

  defaultConfig {
    minSdk = 21
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}