/*
 * Created by Muhamad Syafii
 * 21/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch.core

import com.kupil.apexfetch.model.DownloadState
import com.kupil.apexfetch.model.DownloadStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import okio.Path

/**
 * Concrete implementation of [DownloadExecutor].
 *
 * Responsibilities:
 * - Build the HTTP request (with optional Range header for resume)
 * - Delegate retry logic to [RetryPolicy]
 * - Delegate disk writing to [DiskManager]
 * - Delegate DB updates to [DownloadRepository]
 *
 * Open/Closed — swap [RetryPolicy] or [DiskManager] without modifying this class.
 */
internal class DownloadExecutorImpl(
  private val client: HttpClient,
  private val diskManager: DiskManager,
  private val repository: DownloadRepository,
  private val retryPolicy: RetryPolicy
) : DownloadExecutor {

  override suspend fun FlowCollector<DownloadState>.execute(
    url: String,
    destinationPath: Path,
    recordId: Long?
  ) {
    val fileName = destinationPath.name
    val savedPath = destinationPath.toString()
    var retryCount = 0

    while (retryCount <= retryPolicy.maxRetries) {
      try {
        performRequest(url, destinationPath, fileName, savedPath, recordId)
        return
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        retryCount++
        if (retryCount > retryPolicy.maxRetries) throw e
        emit(DownloadState.Connecting)
        delay(retryPolicy.delayMillisFor(retryCount))
      }
    }
  }

  private suspend fun FlowCollector<DownloadState>.performRequest(
    url: String,
    destinationPath: Path,
    fileName: String,
    savedPath: String,
    recordId: Long?
  ) {
    val downloadedBytes = diskManager.resolveDownloadedBytes(destinationPath)
    val isResume = downloadedBytes > 0L

    client.prepareGet(url) {
      if (isResume) header(HttpHeaders.Range, "bytes=$downloadedBytes-")
    }.execute { response: HttpResponse ->

      // HTTP 416 = server says file is already fully downloaded
      if (response.status.value == 416) {
        repository.updateStatus(url, DownloadStatus.SUCCESS)
        emit(DownloadState.Success(savedPath))
        return@execute
      }

      if (!response.status.isSuccess()) throw Exception("HTTP Error: ${response.status}")

      val contentLength = response.contentLength() ?: -1L
      val totalBytes = if (contentLength != -1L) downloadedBytes + contentLength else -1L

      repository.updateRecord(
        id = recordId,
        url = url,
        fileName = fileName,
        savedPath = savedPath,
        totalBytes = totalBytes,
        status = DownloadStatus.DOWNLOADING
      )

      with(diskManager) {
        streamToDisk(
          channel = response.body<ByteReadChannel>(),
          destinationPath = destinationPath,
          isResume = isResume,
          startBytes = downloadedBytes,
          totalBytes = totalBytes
        )
      }

      repository.updateRecord(
        id = recordId,
        url = url,
        fileName = fileName,
        savedPath = savedPath,
        totalBytes = totalBytes,
        status = DownloadStatus.SUCCESS
      )

      emit(DownloadState.Success(savedPath))
    }
  }
}
