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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.kupil.apexfetch.core.ApexFetcher
import com.kupil.apexfetch.model.DownloadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okio.Path

/**
 * A Compose state holder that manages a single download lifecycle.
 *
 * Designed for screens where [url] and [destinationPath] are fixed —
 * bind them once via [bind], then call [start], [pause], [cancel] freely
 * from any onClick without needing a coroutine scope at the call site.
 *
 * Usage:
 * ```kotlin
 * val controller = rememberDownloadState(fetcher)
 * controller.bind(url, path)
 *
 * Button(onClick = { controller.start() }) { Text("Download") }
 * LinearProgressIndicator(progress = controller.uiState.progress / 100f)
 * ```
 *
 * @param fetcher The [ApexFetcher] instance from apexfetch-core.
 * @param scope The [CoroutineScope] used to launch the download coroutine.
 */
class DownloadStateHolder(
  private val fetcher: ApexFetcher,
  private val scope: CoroutineScope
) {
  var uiState by mutableStateOf(DownloadUiState())
    private set

  private var currentUrl: String? = null
  private var currentPath: Path? = null
  private var activeJob: Job? = null

  /**
   * Binds a [url] and [destinationPath] to this holder.
   * Call this once before invoking [start].
   */
  fun bind(url: String, destinationPath: Path) {
    currentUrl = url
    currentPath = destinationPath
  }

  /**
   * Starts or resumes the download.
   * Safe to call directly from onClick — launches internally via [scope].
   * No-op if [bind] has not been called.
   */
  fun start() {
    val url = currentUrl ?: return
    val path = currentPath ?: return
    activeJob?.cancel()
    activeJob = scope.launch {
      fetcher.download(url, path).collect { state ->
        uiState = DownloadUiState(state)
      }
    }
  }

  /**
   * Starts or resumes a download with explicit [url] and [destinationPath].
   * Also updates the bound target for subsequent [pause] and [cancel] calls.
   */
  fun start(url: String, destinationPath: Path) {
    bind(url, destinationPath)
    start()
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
    activeJob?.cancel()
    fetcher.cancel(url, path)
    uiState = DownloadUiState(DownloadState.Idle)
  }
}

/**
 * Remembers a [DownloadStateHolder] tied to the composition lifecycle.
 *
 * Internally uses [rememberCoroutineScope] so the download coroutine is
 * automatically cancelled when the composable leaves the composition.
 *
 * @param fetcher The [ApexFetcher] instance. Should be provided via DI (Hilt/Koin).
 */
@Composable
fun rememberDownloadState(fetcher: ApexFetcher): DownloadStateHolder {
  val scope = rememberCoroutineScope()
  return remember(fetcher) { DownloadStateHolder(fetcher, scope) }
}

/**
 * A variant that immediately starts collecting a download on first composition.
 *
 * Useful for screens that auto-start a download on open.
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
  val scope = rememberCoroutineScope()
  val holder = remember(fetcher) { DownloadStateHolder(fetcher, scope) }
  LaunchedEffect(url, destinationPath) {
    holder.start(url, destinationPath)
  }
  return holder
}