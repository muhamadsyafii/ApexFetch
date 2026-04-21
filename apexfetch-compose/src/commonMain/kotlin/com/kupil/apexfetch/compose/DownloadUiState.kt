/*
 * Created by Muhamad Syafii
 * 21/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch.compose

import androidx.compose.runtime.Immutable
import com.kupil.apexfetch.model.DownloadState

/**
 * A Compose-friendly snapshot of a download's current state.
 *
 * Marked [Immutable] so Compose can skip recomposition when the reference hasn't changed.
 * This is the single source of truth passed to all Composable consumers.
 *
 * @property state The underlying [DownloadState] from apexfetch-core.
 */
@Immutable
data class DownloadUiState(
  val state: DownloadState = DownloadState.Idle
) {
  val isIdle: Boolean get() = state is DownloadState.Idle
  val isConnecting: Boolean get() = state is DownloadState.Connecting
  val isDownloading: Boolean get() = state is DownloadState.Downloading
  val isSuccess: Boolean get() = state is DownloadState.Success
  val isError: Boolean get() = state is DownloadState.Error

  val progress: Int get() = (state as? DownloadState.Downloading)?.progress ?: 0
  val downloadedBytes: Long get() = (state as? DownloadState.Downloading)?.downloadedBytes ?: 0L
  val totalBytes: Long get() = (state as? DownloadState.Downloading)?.totalBytes ?: 0L
  val savedPath: String? get() = (state as? DownloadState.Success)?.filePath
  val error: Throwable? get() = (state as? DownloadState.Error)?.exception
}
