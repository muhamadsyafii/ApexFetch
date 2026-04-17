/*
 * Created by Muhamad Syafii
 * 17/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch

import com.kupil.apexfetch.db.ApexDatabase
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.io.readByteArray
import okio.FileSystem
import okio.Path
import okio.buffer

class ApexFetcher(
  private val database: ApexDatabase,
  private val client: HttpClient = HttpClientFactory.create(),
  private val fileSystem: FileSystem = FileSystem.SYSTEM,
) {

  private val queries = database.downloadHistoryQueries
  private val activeJobs = mutableMapOf<String, Job>()

  fun download(url: String, destinationPath: Path): Flow<DownloadState> = flow {
    currentCoroutineContext()[Job]?.let { activeJobs[url] = it }
    emit(DownloadState.Connecting)

    try {
      val fileName = destinationPath.name
      val savedPath = destinationPath.toString()
      queries.insertOrReplace(
        id = null,
        url = url,
        fileName = fileName,
        savedPath = savedPath,
        totalBytes = 0L,
        status = DownloadStatus.CONNECTING,
        timestamp = getCurrentTimeMillis()
      )

      val isResume = fileSystem.exists(destinationPath)
      val downloadedBytes = if (isResume) {
        fileSystem.metadata(destinationPath).size ?: 0L
      } else {
        0L
      }

      client.prepareGet(url) {
        if (downloadedBytes > 0) {
          header(HttpHeaders.Range, "bytes=$downloadedBytes-")
        }
      }.execute { response: HttpResponse ->

        if (!response.status.isSuccess()) {
          queries.insertOrReplace(
            id = null,
            url = url,
            fileName = fileName,
            savedPath = savedPath,
            totalBytes = 0L,
            status = DownloadStatus.FAILED,
            timestamp = getCurrentTimeMillis()
          )
          emit(DownloadState.Error(Exception("HTTP Error: ${response.status}")))
          return@execute
        }

        val contentLength = response.contentLength() ?: -1L
        val totalBytes = if (contentLength != -1L) downloadedBytes + contentLength else -1L
        var currentBytes = downloadedBytes
        queries.insertOrReplace(
          id = null,
          url = url,
          fileName = fileName,
          savedPath = savedPath,
          totalBytes = totalBytes,
          status = DownloadStatus.DOWNLOADING,
          timestamp = getCurrentTimeMillis()
        )
        val channel: ByteReadChannel = response.body()
        val sinkMode = if (isResume) fileSystem.appendingSink(destinationPath) else fileSystem.sink(
          destinationPath
        )
        sinkMode.buffer().use { sink ->
          while (!channel.isClosedForRead) {
            val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
            while (!packet.exhausted()) {
              val bytes = packet.readByteArray()
              sink.write(bytes)

              currentBytes += bytes.size
              val progress = if (totalBytes > 0) {
                ((currentBytes.toDouble() / totalBytes) * 100).toInt()
              } else {
                0
              }

              emit(DownloadState.Downloading(progress, currentBytes, totalBytes))
            }
          }
        }
        queries.insertOrReplace(
          id = null,
          url = url,
          fileName = fileName,
          savedPath = savedPath,
          totalBytes = totalBytes,
          status = DownloadStatus.SUCCESS,
          timestamp = getCurrentTimeMillis()
        )

        emit(DownloadState.Success(savedPath))
      }
    } catch (e: Exception) {
      queries.insertOrReplace(
        id = null,
        url = url,
        fileName = destinationPath.name,
        savedPath = destinationPath.toString(),
        totalBytes = 0L,
        status = DownloadStatus.FAILED,
        timestamp = getCurrentTimeMillis()
      )
      emit(DownloadState.Error(e))
    } finally {
      activeJobs.remove(url)
    }
  }

  fun pause(url: String) {
    activeJobs[url]?.cancel()
    queries.updateStatusByUrl(DownloadStatus.PAUSED, url)
  }

  fun pauseAll() {
    activeJobs.keys.toList().forEach { url -> pause(url) }
  }

  fun cancel(url: String, destinationPath: Path) {
    activeJobs[url]?.cancel()
    queries.updateStatusByUrl(DownloadStatus.CANCELED, url)
    if (fileSystem.exists(destinationPath)) fileSystem.delete(destinationPath)
  }

  fun cancelAll(paths: Map<String, Path>) {
    activeJobs.keys.toList().forEach { url ->
      paths[url]?.let { cancel(url, it) }
    }
  }
}