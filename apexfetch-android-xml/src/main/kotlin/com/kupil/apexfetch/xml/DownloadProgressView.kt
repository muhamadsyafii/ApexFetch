/*
 * Created by Muhamad Syafii
 * 21/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch.xml

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.kupil.apexfetch.model.DownloadState

/**
 * A ready-to-use custom View for displaying download progress in XML layouts.
 *
 * Bind a [DownloadState] directly via [setState] — no manual view wiring needed.
 *
 * XML Usage:
 * ```xml
 * <com.kupil.apexfetch.xml.DownloadProgressView
 *     android:id="@+id/downloadProgressView"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content" />
 * ```
 *
 * Fragment/Activity Usage:
 * ```kotlin
 * viewModel.downloadState.observe(viewLifecycleOwner) { state ->
 *     binding.downloadProgressView.setState(state)
 * }
 * ```
 */
class DownloadProgressView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

  companion object {
    private const val TEXT_IDLE = "Idle"
    private const val TEXT_CONNECTING = "Connecting..."
    private const val TEXT_DOWNLOADING_FORMAT = "Downloading %d%%"
    private const val TEXT_SUCCESS = "Download Complete"
    private const val TEXT_ERROR_FORMAT = "Error: %s"
    private const val TEXT_BYTES_FORMAT = "%s / %s"
  }

  private val progressBar: ProgressBar
  private val tvStatus: TextView
  private val tvBytes: TextView

  init {
    LayoutInflater.from(context).inflate(
      R.layout.view_download_progress,
      this,
      true
    )
    progressBar = findViewById(R.id.progressBar)
    tvStatus = findViewById(R.id.tvStatus)
    tvBytes = findViewById(R.id.tvBytes)
  }

  /**
   * Binds the view to the given [DownloadState].
   * Panggil ini dari Observer di Activity/Fragment.
   */
  fun setState(state: DownloadState) {
    when (state) {
      is DownloadState.Idle -> {
        progressBar.progress = 0
        tvStatus.text = TEXT_IDLE
        tvBytes.text = ""
      }
      is DownloadState.Connecting -> {
        progressBar.isIndeterminate = true
        tvStatus.text = TEXT_CONNECTING
        tvBytes.text = ""
      }
      is DownloadState.Downloading -> {
        progressBar.isIndeterminate = false
        progressBar.progress = state.progress

        tvStatus.text = String.format(TEXT_DOWNLOADING_FORMAT, state.progress)
        tvBytes.text = String.format(
          TEXT_BYTES_FORMAT,
          state.downloadedBytes.toReadableSize(),
          state.totalBytes.toReadableSize()
        )
      }
      is DownloadState.Success -> {
        progressBar.isIndeterminate = false
        progressBar.progress = 100
        tvStatus.text = TEXT_SUCCESS
        tvBytes.text = state.filePath
      }
      is DownloadState.Error -> {
        progressBar.isIndeterminate = false
        progressBar.progress = 0
        val errorMessage = state.exception.message ?: "Unknown error occurred"
        tvStatus.text = String.format(TEXT_ERROR_FORMAT, errorMessage)
        tvBytes.text = ""
      }
    }
  }

  private fun Long.toReadableSize(): String {
    if (this < 0) return "?"
    val kb = this / 1024.0
    val mb = kb / 1024.0
    return when {
      mb >= 1 -> "%.1f MB".format(mb)
      kb >= 1 -> "%.1f KB".format(kb)
      else -> "$this B"
    }
  }
}