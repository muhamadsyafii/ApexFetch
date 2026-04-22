/*
 * Created by Muhamad Syafii
 * 21/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch.core

import com.kupil.apexfetch.model.DownloadState
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.io.readByteArray
import okio.FileSystem
import okio.Path
import okio.buffer

/**
 * Handles all disk I/O operations for downloads.
 *
 * Single Responsibility — this class only knows about reading from a network
 * channel and writing bytes to disk. It has no knowledge of HTTP, retries, or DB.
 */
internal class DiskManager(
  private val fileSystem: FileSystem,
  private val bufferSize: Long
) {

  /**
   * Returns bytes already written to [path], or 0 if file does not exist.
   */
  fun resolveDownloadedBytes(path: Path): Long {
    return if (fileSystem.exists(path)) fileSystem.metadata(path).size ?: 0L else 0L
  }

  /**
   * Deletes [path] from disk if it exists.
   */
  fun deleteIfExists(path: Path) {
    if (fileSystem.exists(path)) fileSystem.delete(path)
  }

  /**
   * Streams bytes from [channel] to [destinationPath], emitting [DownloadState.Downloading]
   * progress updates along the way.
   *
   * Opens the sink in append mode when [isResume] is true.
   *
   * @param startBytes Bytes already on disk — used as the baseline for progress calculation.
   * @param onProgress Optional callback that receives the latest [currentBytes] written.
   *                   Used by [DownloadExecutorImpl] to persist final downloadedBytes to DB.
   */
  suspend fun FlowCollector<DownloadState>.streamToDisk(
    channel: ByteReadChannel,
    destinationPath: Path,
    isResume: Boolean,
    startBytes: Long,
    totalBytes: Long,
    onProgress: ((currentBytes: Long) -> Unit)? = null
  ) {
    val sink = if (isResume) {
      fileSystem.appendingSink(destinationPath)
    } else {
      fileSystem.sink(destinationPath)
    }

    var currentBytes = startBytes

    sink.buffer().use { buffered ->
      while (!channel.isClosedForRead) {
        val packet = channel.readRemaining(bufferSize)
        while (!packet.exhausted()) {
          val bytes = packet.readByteArray()
          buffered.write(bytes)
          currentBytes += bytes.size
          onProgress?.invoke(currentBytes)
          emit(DownloadState.Downloading(
            progress = calculateProgress(currentBytes, totalBytes),
            downloadedBytes = currentBytes,
            totalBytes = totalBytes
          ))
        }
      }
    }
  }

  private fun calculateProgress(currentBytes: Long, totalBytes: Long): Int {
    if (totalBytes <= 0L) return 0
    return ((currentBytes.toDouble() / totalBytes) * 100).toInt()
  }
}