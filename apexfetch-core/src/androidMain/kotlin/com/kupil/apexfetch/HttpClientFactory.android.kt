/*
 * Created by Muhamad Syafii
 * 17/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android

actual object HttpClientFactory {
  actual fun create(): HttpClient {
    return HttpClient(Android) {
      // Bisa tambah konfigurasi timeout di sini nanti
    }
  }
}