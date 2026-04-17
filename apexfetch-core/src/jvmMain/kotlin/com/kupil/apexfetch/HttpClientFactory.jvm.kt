/*
 * Created by Muhamad Syafii
 * 17/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual object HttpClientFactory {
  actual fun create(): HttpClient {
    return HttpClient(CIO) {
    }
  }
}