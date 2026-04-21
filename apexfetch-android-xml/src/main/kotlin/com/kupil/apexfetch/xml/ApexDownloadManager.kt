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
 * LiveData-backed download state for Android XML / View System consumers.
 *
 * Bridges [ApexFetcher]'s [kotlinx.coroutines.flow.Flow] to [LiveData] so that
 * XML-based UIs (Fragment, Activity, custom Views) can observe download progress
 * without any Compose dependency.
 *
 * Usage in ViewModel:
 * ```kotlin
 * class MyViewModel(
 *     private val fetcher: ApexFetcher,
 *     private val scope: CoroutineScope = viewModelScope
 * ) : ViewModel() {
 *
 *     private val downloadManager = ApexDownloadManager(fetcher, viewModelScope)
 *
 *     val downloadState: LiveData<DownloadState> = downloadManager.state
 *
 *     fun download(url: String, path: Path) = downloadManager.start(url, path)
 *     fun pause(url: String) = downloadManager.pause(url)
 *     fun cancel(url: String, path: Path) = downloadManager.cancel(url, path)
 * }
 * ```
 *
 * Usage in Fragment/Activity:
 * ```kotlin
 * viewModel.downloadState.observe(viewLifecycleOwner) { state ->
 *     when (state) {
 *         is DownloadState.Connecting   -> showConnecting()
 *         is DownloadState.Downloading  -> updateProgress(state.progress)
 *         is DownloadState.Success      -> showSuccess(state.savedPath)
 *         is DownloadState.Error        -> showError(state.cause)
 *         is DownloadState.Idle         -> showIdle()
 *     }
 * }
 * ```
 */
class ApexDownloadManager(
  private val fetcher: ApexFetcher,
  private val scope: CoroutineScope
) {
  private val _state = MutableLiveData<DownloadState>(DownloadState.Idle)

  /** Observe this in your Fragment or Activity. */
  val state: LiveData<DownloadState> get() = _state

  private var activeJob: Job? = null

  /**
   * Starts or resumes a download.
   * Cancels any previously running download job before starting a new one.
   *
   * @param url The URL to download.
   * @param destinationPath The Okio [Path] where the file will be saved.
   */
  fun start(url: String, destinationPath: Path) {
    activeJob?.cancel()
    activeJob = scope.launch {
      fetcher.download(url, destinationPath).collect { state ->
        _state.postValue(state)
      }
    }
  }

  /**
   * Pauses the current download.
   * Posts [DownloadState.Idle] to reset the UI.
   *
   * @param url The URL of the download to pause.
   */
  fun pause(url: String) {
    fetcher.pause(url)
    _state.postValue(DownloadState.Idle)
  }

  /**
   * Cancels the current download and deletes the partial file.
   * Posts [DownloadState.Idle] to reset the UI.
   *
   * @param url The URL of the download to cancel.
   * @param destinationPath The path of the partial file to delete.
   */
  fun cancel(url: String, destinationPath: Path) {
    activeJob?.cancel()
    fetcher.cancel(url, destinationPath)
    _state.postValue(DownloadState.Idle)
  }
}