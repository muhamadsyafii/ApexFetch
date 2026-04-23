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
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlinAndroid)
  alias(libs.plugins.dokka)
  id("com.vanniktech.maven.publish")
}

mavenPublishing {
  publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
  signAllPublications()

  configure(com.vanniktech.maven.publish.AndroidSingleVariantLibrary(publishJavadocJar = false))

  coordinates(
    groupId    = ApexFetchConfig.GROUP,
    artifactId = ApexFetchConfig.ARTIFACT_XML,
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

android {
  namespace  = "com.kupil.apexfetch.xml"
  compileSdk = ApexFetchConfig.COMPILE_SDK

  defaultConfig {
    minSdk = ApexFetchConfig.MIN_SDK
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