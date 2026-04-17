/*
 * Created by Muhamad Syafii
 * 17/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.io.readByteArray
import okio.FileSystem
import okio.Path
import okio.buffer

class ApexFetcher(
  private val client: HttpClient = HttpClientFactory.create(),
  private val fileSystem: FileSystem = FileSystem.SYSTEM
) {

  fun download(url: String, destinationPath: Path): Flow<DownloadState> = flow {
    emit(DownloadState.Connecting)

    try {
      // 1. Cek apakah file sudah ada sebelumnya (untuk fitur Resume)
      val isResume = fileSystem.exists(destinationPath)
      val downloadedBytes = if (isResume) {
        fileSystem.metadata(destinationPath).size ?: 0L
      } else {
        0L
      }

      // 2. Siapkan Request dengan header 'Range' jika melanjutkan unduhan
      client.prepareGet(url) {
        if (downloadedBytes > 0) {
          header(HttpHeaders.Range, "bytes=$downloadedBytes-")
        }
      }.execute { response: HttpResponse ->

        if (!response.status.isSuccess()) {
          emit(DownloadState.Error(Exception("HTTP Error: ${response.status}")))
          return@execute
        }

        // 3. Hitung total bytes (yang sudah ada + yang akan diunduh)
        val contentLength = response.contentLength() ?: -1L
        val totalBytes = if (contentLength != -1L) downloadedBytes + contentLength else -1L
        var currentBytes = downloadedBytes

        val channel: ByteReadChannel = response.body()

        val sinkMode = if (isResume) fileSystem.appendingSink(destinationPath) else fileSystem.sink(destinationPath)

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

        emit(DownloadState.Success(destinationPath.toString()))
      }
    } catch (e: Exception) {
      emit(DownloadState.Error(e))
    }
  }
}