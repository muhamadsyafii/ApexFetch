/*
 * Created by Muhamad Syafii
 * 17/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.engine.okhttp.OkHttp

actual object HttpClientFactory {
  actual fun create(): HttpClient = HttpClient(OkHttp) {
    install(HttpTimeout) {
      requestTimeoutMillis = 15000
      connectTimeoutMillis = 15000
      socketTimeoutMillis = 15000
    }
  }
}