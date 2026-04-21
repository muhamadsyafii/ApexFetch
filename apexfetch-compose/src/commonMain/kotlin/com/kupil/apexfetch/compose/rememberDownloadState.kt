/*
 * Created by Muhamad Syafii
 * 21/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kupil.apexfetch.core.ApexFetcher
import com.kupil.apexfetch.model.DownloadState
import okio.Path

/**
 * A Compose state holder that manages a single download lifecycle.
 *
 * Provides reactive [DownloadUiState] that automatically updates as the download progresses.
 * Cancels the download collection when the composable leaves the composition.
 *
 * Usage:
 * ```kotlin
 * val downloadState = rememberDownloadState(fetcher = fetcher)
 *
 * Button(onClick = { downloadState.start(url, path) }) { Text("Download") }
 * LinearProgressIndicator(progress = downloadState.uiState.progress / 100f)
 * ```
 *
 * @param fetcher The [ApexFetcher] instance from apexfetch-core.
 */
class DownloadStateHolder(
  private val fetcher: ApexFetcher
) {
  var uiState by mutableStateOf(DownloadUiState())
    private set

  private var currentUrl: String? = null
  private var currentPath: Path? = null

  /**
   * Starts or resumes a download.
   * Call this from a button click or LaunchedEffect.
   */
  suspend fun start(url: String, destinationPath: Path) {
    currentUrl = url
    currentPath = destinationPath
    fetcher.download(url, destinationPath).collect { state ->
      uiState = DownloadUiState(state)
    }
  }

  /** Pauses the current download. */
  fun pause() {
    currentUrl?.let { fetcher.pause(it) }
    uiState = DownloadUiState(DownloadState.Idle)
  }

  /** Cancels the current download and deletes the partial file. */
  fun cancel() {
    val url = currentUrl ?: return
    val path = currentPath ?: return
    fetcher.cancel(url, path)
    uiState = DownloadUiState(DownloadState.Idle)
  }
}

/**
 * Remembers a [DownloadStateHolder] tied to the composition lifecycle.
 *
 * The holder is stable across recompositions as long as [fetcher] doesn't change.
 *
 * @param fetcher The [ApexFetcher] instance. Should be provided via DI (Hilt/Koin).
 */
@Composable
fun rememberDownloadState(fetcher: ApexFetcher): DownloadStateHolder {
  return remember(fetcher) { DownloadStateHolder(fetcher) }
}

/**
 * A variant that immediately starts collecting a download on first composition.
 *
 * Useful for screens that auto-start a download on open.
 *
 * Usage:
 * ```kotlin
 * val downloadState = rememberAutoDownloadState(
 *     fetcher = fetcher,
 *     url = fileUrl,
 *     destinationPath = savePath
 * )
 * ```
 *
 * @param fetcher The [ApexFetcher] instance.
 * @param url The URL to download immediately.
 * @param destinationPath The path to save the file.
 */
@Composable
fun rememberAutoDownloadState(
  fetcher: ApexFetcher,
  url: String,
  destinationPath: Path
): DownloadStateHolder {
  val holder = remember(fetcher) { DownloadStateHolder(fetcher) }
  LaunchedEffect(url, destinationPath) {
    holder.start(url, destinationPath)
  }
  return holder
}