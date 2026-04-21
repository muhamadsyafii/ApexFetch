/*
 * Created by Muhamad Syafii
 * 21/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch.xml

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kupil.apexfetch.core.ApexFetcher
import com.kupil.apexfetch.model.DownloadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okio.Path

/**
 * LiveData-backed manager for multiple concurrent downloads in XML / View System.
 *
 * Each download is independently tracked by its URL key.
 * Designed for use in a RecyclerView adapter or list-based download queue screen.
 *
 * Usage in ViewModel:
 * ```kotlin
 * class QueueViewModel(
 *     fetcher: ApexFetcher
 * ) : ViewModel() {
 *
 *     private val multiManager = ApexMultiDownloadManager(fetcher, viewModelScope)
 *
 *     fun stateOf(url: String): LiveData<DownloadState> = multiManager.stateOf(url)
 *     fun download(url: String, path: Path) = multiManager.start(url, path)
 *     fun pause(url: String) = multiManager.pause(url)
 *     fun cancel(url: String, path: Path) = multiManager.cancel(url, path)
 * }
 * ```
 *
 * Usage in RecyclerView ViewHolder:
 * ```kotlin
 * viewModel.stateOf(item.url).observe(viewLifecycleOwner) { state ->
 *     when (state) {
 *         is DownloadState.Downloading -> progressBar.progress = state.progress
 *         is DownloadState.Success     -> showSuccess()
 *         else                         -> showIdle()
 *     }
 * }
 * ```
 */
class ApexMultiDownloadManager(
  private val fetcher: ApexFetcher,
  private val scope: CoroutineScope
) {
  private val liveDataMap = mutableMapOf<String, MutableLiveData<DownloadState>>()
  private val jobMap = mutableMapOf<String, Job>()

  /**
   * Returns a [LiveData] for the given [url].
   * Creates a new one with [DownloadState.Idle] if not yet tracked.
   */
  fun stateOf(url: String): LiveData<DownloadState> {
    return liveDataMap.getOrPut(url) { MutableLiveData(DownloadState.Idle) }
  }

  /**
   * Starts or resumes a download for the given [url].
   * Cancels the previous job for the same URL if it exists.
   */
  fun start(url: String, destinationPath: Path) {
    jobMap[url]?.cancel()
    val liveData = liveDataMap.getOrPut(url) { MutableLiveData(DownloadState.Idle) }
    jobMap[url] = scope.launch {
      fetcher.download(url, destinationPath).collect { state ->
        liveData.postValue(state)
      }
    }
  }

  /**
   * Pauses the download for the given [url].
   * Resets its [LiveData] to [DownloadState.Idle].
   */
  fun pause(url: String) {
    fetcher.pause(url)
    liveDataMap[url]?.postValue(DownloadState.Idle)
  }

  /**
   * Cancels the download for [url] and deletes the partial file.
   * Removes its [LiveData] entry from the map.
   */
  fun cancel(url: String, destinationPath: Path) {
    jobMap[url]?.cancel()
    fetcher.cancel(url, destinationPath)
    liveDataMap[url]?.postValue(DownloadState.Idle)
    liveDataMap.remove(url)
    jobMap.remove(url)
  }

  /**
   * Pauses all active downloads.
   */
  fun pauseAll() {
    fetcher.pauseAll()
    liveDataMap.values.forEach { it.postValue(DownloadState.Idle) }
  }

  /**
   * Cancels all active downloads and clears all tracked state.
   */
  fun cancelAll(paths: Map<String, Path>) {
    fetcher.cancelAll(paths)
    jobMap.values.forEach { it.cancel() }
    jobMap.clear()
    liveDataMap.values.forEach { it.postValue(DownloadState.Idle) }
    liveDataMap.clear()
  }
}