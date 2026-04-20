plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.sqldelight)
}

kotlin {
  androidTarget {
    publishLibraryVariants("release")
  }
  jvm()

  sourceSets.all {
    languageSettings.optIn("kotlin.ExperimentalMultiplatform")
  }

  targets.all {
    compilations.all {
      kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xexpect-actual-classes"
      }
    }
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.kotlinx.coroutines.core)
        api(libs.ktor.client.core)
        implementation(libs.ktor.client.logging)
        implementation(libs.ktor.client.okhttp)
        api(libs.okio)

        implementation(libs.sqldelight.coroutines)
      }
    }

    val androidMain by getting {
      dependencies {
        implementation(libs.ktor.client.android)
        implementation(libs.sqldelight.android)
      }
    }

    val jvmMain by getting {
      dependencies {
        implementation(libs.ktor.client.cio)
        implementation(libs.sqldelight.jvm)
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

sqldelight {
  databases {
    create("ApexDatabase") {
      packageName.set("com.kupil.apexfetch.db")
    }
  }
}