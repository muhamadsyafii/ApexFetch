/*
 * Created by Muhamad Syafii
 * 21/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.kupil.apexfetch.core.ApexFetcher
import com.kupil.apexfetch.model.DownloadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okio.Path

/**
 * A Compose state holder that manages multiple concurrent downloads.
 *
 * Each download is keyed by its URL, allowing independent progress tracking
 * for list-based UIs (e.g. a download queue screen).
 *
 * Usage:
 * ```kotlin
 * val multiDownload = rememberMultiDownloadState(fetcher, scope)
 *
 * LazyColumn {
 *     items(files) { file ->
 *         val state = multiDownload.stateOf(file.url)
 *         DownloadItem(
 *             name = file.name,
 *             progress = state.progress,
 *             onDownload = { multiDownload.start(file.url, file.path, scope) },
 *             onPause = { multiDownload.pause(file.url) },
 *             onCancel = { multiDownload.cancel(file.url, file.path) }
 *         )
 *     }
 * }
 * ```
 *
 * @param fetcher The [ApexFetcher] instance from apexfetch-core.
 * @param scope The [CoroutineScope] used to launch each download coroutine.
 */
class MultiDownloadStateHolder(
  private val fetcher: ApexFetcher,
  private val scope: CoroutineScope
) {
  /**
   * Snapshot-backed map so Compose recomposes only the item whose state changed.
   * Key: URL, Value: [DownloadUiState]
   */
  val states: SnapshotStateMap<String, DownloadUiState> = mutableStateMapOf()

  /**
   * Returns the current [DownloadUiState] for the given [url].
   * Returns [DownloadUiState] with [DownloadState.Idle] if not yet started.
   */
  fun stateOf(url: String): DownloadUiState = states[url] ?: DownloadUiState()

  /**
   * Starts or resumes a download for the given [url].
   * Launches in the provided [CoroutineScope] so the download survives recomposition.
   */
  fun start(url: String, destinationPath: Path) {
    scope.launch {
      fetcher.download(url, destinationPath).collect { state ->
        states[url] = DownloadUiState(state)
      }
    }
  }

  /** Pauses the download for the given [url]. */
  fun pause(url: String) {
    fetcher.pause(url)
    states[url] = DownloadUiState(DownloadState.Idle)
  }

  /** Cancels the download for [url] and deletes the partial file. */
  fun cancel(url: String, destinationPath: Path) {
    fetcher.cancel(url, destinationPath)
    states.remove(url)
  }

  /** Pauses all active downloads. */
  fun pauseAll() {
    fetcher.pauseAll()
    states.keys.forEach { states[it] = DownloadUiState(DownloadState.Idle) }
  }

  /** Cancels all active downloads and clears all state entries. */
  fun cancelAll(paths: Map<String, Path>) {
    fetcher.cancelAll(paths)
    states.clear()
  }
}

/**
 * Remembers a [MultiDownloadStateHolder] tied to the composition lifecycle.
 *
 * @param fetcher The [ApexFetcher] instance. Should be provided via DI.
 * @param scope The [CoroutineScope] for launching download coroutines.
 *              Typically [rememberCoroutineScope] from the calling composable.
 */
@Composable
fun rememberMultiDownloadState(
  fetcher: ApexFetcher,
  scope: CoroutineScope
): MultiDownloadStateHolder {
  return remember(fetcher) { MultiDownloadStateHolder(fetcher, scope) }
}
