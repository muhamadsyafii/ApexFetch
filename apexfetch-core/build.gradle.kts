import com.vanniktech.maven.publish.SonatypeHost

/*
 * Copyright 2026 Muhamad Syafii
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.sqldelight)
  alias(libs.plugins.dokka)
  id("com.vanniktech.maven.publish")
}

mavenPublishing {
  publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
  signAllPublications()

  configure(com.vanniktech.maven.publish.KotlinMultiplatform(javadocJar = com.vanniktech.maven.publish.JavadocJar.Empty()))

  coordinates(
    groupId    = ApexFetchConfig.GROUP,
    artifactId = ApexFetchConfig.ARTIFACT_CORE,
    version    = ApexFetchConfig.VERSION,
  )

  pom {
    name.set(ApexFetchConfig.LIBRARY_NAME)
    description.set(ApexFetchConfig.LIBRARY_DESCRIPTION)
    url.set(ApexFetchConfig.LIBRARY_URL)

    licenses {
      license {
        name.set(ApexFetchConfig.LICENSE_NAME)
        url.set(ApexFetchConfig.LICENSE_URL)
      }
    }

    developers {
      developer {
        id.set(ApexFetchConfig.DEVELOPER_ID)
        name.set(ApexFetchConfig.DEVELOPER_NAME)
        email.set(ApexFetchConfig.DEVELOPER_EMAIL)
      }
    }

    scm {
      url.set(ApexFetchConfig.SCM_URL)
      connection.set(ApexFetchConfig.SCM_CONNECTION)
      developerConnection.set(ApexFetchConfig.SCM_DEV_CON)
    }
  }
}


kotlin {
  androidTarget {
    publishLibraryVariants("release")
  }
  jvm()

  jvmToolchain(17)

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
  namespace  = "dev.kupil.apexfetch.core"
  compileSdk = ApexFetchConfig.COMPILE_SDK

  defaultConfig {
    minSdk = ApexFetchConfig.MIN_SDK
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