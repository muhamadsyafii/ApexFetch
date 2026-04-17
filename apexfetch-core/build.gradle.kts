plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidLibrary)
}

kotlin {
  androidTarget {
    publishLibraryVariants("release")
  }
  jvm()

  sourceSets.all {
    languageSettings.optIn("kotlin.ExperimentalMultiplatform")
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.ktor.client.core)
        api(libs.okio)
      }
    }

    val androidMain by getting {
      dependencies {
        implementation(libs.ktor.client.android)
      }
    }

    val jvmMain by getting {
      dependencies {
        implementation(libs.ktor.client.cio)
      }
    }
  }
}

android {
  namespace = "dev.kupil.apexfetch.core"
  compileSdk = 34

  defaultConfig {
    minSdk = 21
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
}