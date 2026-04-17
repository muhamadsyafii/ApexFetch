/*
 * Created by Muhamad Syafii
 * 17/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch

sealed class DownloadState {
  object Idle : DownloadState()
  object Connecting : DownloadState()
  data class Downloading(val progress: Int, val downloadedBytes: Long, val totalBytes: Long) : DownloadState()
  data class Success(val filePath: String) : DownloadState()
  data class Error(val exception: Throwable) : DownloadState()
}