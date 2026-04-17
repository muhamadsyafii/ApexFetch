/*
 * Created by Muhamad Syafii
 * 17/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.kupil.apexfetch.db.ApexDatabase
import com.kupil.apexfetch.db.DownloadHistoryEntity
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.io.readByteArray
import okio.FileSystem
import okio.Path
import okio.buffer

/**
 * The core engine responsible for handling file downloads in the ApexFetch library.
 * * This class operates as a multiplatform downloader (Android, iOS, JVM). It handles
 * network connections, file I/O operations (including resume capabilities via chunking),
 * and automatically synchronizes the download state with a local SQLite database.
 *
 * @property database The SQLDelight [ApexDatabase] instance used for tracking download history.
 * @property client The Ktor [HttpClient] used for network requests. Defaults to [HttpClientFactory.create].
 * @property fileSystem The Okio [FileSystem] used for cross-platform file operations. Defaults to [FileSystem.SYSTEM].
 */
class ApexFetcher(
  private val database: ApexDatabase,
  private val client: HttpClient = HttpClientFactory.create(),
  private val fileSystem: FileSystem = FileSystem.SYSTEM,
) {

  private val queries = database.downloadHistoryQueries

  /**
   * An internal map to track currently active coroutine [Job]s.
   * This allows the fetcher to selectively pause or cancel ongoing downloads based on their URL.
   */
  private val activeJobs = mutableMapOf<String, Job>()

  /**
   * Initiates or resumes a file download from the specified URL.
   *
   * This function returns a cold [Flow]. The download will only begin when the flow is collected.
   * If a file with the same name already exists at the target path, it will attempt to resume
   * the download from the last saved byte.
   *
   * @param url The absolute URL of the file to download.
   * @param destinationPath The absolute Okio [Path] where the file will be saved.
   * @return A [Flow] emitting the real-time [DownloadState] (Connecting, Downloading, Success, Error).
   */
  fun download(url: String, destinationPath: Path): Flow<DownloadState> = flow {
    // Register the current coroutine Job so it can be controlled (paused/canceled) later.
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
      val existingRecord = queries.getByUrl(url).executeAsOneOrNull()
/*      queries.insertOrReplace(
        id = null,
        url = url,
        fileName = destinationPath.name,
        savedPath = destinationPath.toString(),
        totalBytes = 0L,
        status = DownloadStatus.FAILED,
        timestamp = getCurrentTimeMillis()
      )
      emit(DownloadState.Error(e))*/
      queries.insertOrReplace(
        id = existingRecord?.id,
        url = url,
        fileName = destinationPath.name,
        savedPath = destinationPath.toString(),
        totalBytes = 0L,
        status = DownloadStatus.FAILED,
        timestamp = getCurrentTimeMillis()
      )
      emit(DownloadState.Error(e))

    } finally {
      // Ensure the job is removed from the active tracker to prevent memory leaks
      activeJobs.remove(url)
    }
  }

  /**
   * Pauses an active download.
   * * This cancels the underlying coroutine job and updates the database status to PAUSED.
   * The partially downloaded file remains intact on the disk, allowing it to be resumed later.
   *
   * @param url The URL of the download to pause.
   */
  fun pause(url: String) {
    activeJobs[url]?.cancel()
    queries.updateStatusByUrl(DownloadStatus.PAUSED, url)
  }

  /**
   * Pauses all currently active downloads in the application.
   */
  fun pauseAll() {
    activeJobs.keys.toList().forEach { url -> pause(url) }
  }

  /**
   * Cancels an active download and cleans up the partial file.
   * * This cancels the underlying coroutine job, updates the database status to CANCELED,
   * and deletes any partially downloaded data from the disk to free up storage.
   *
   * @param url The URL of the download to cancel.
   * @param destinationPath The path where the file was being saved.
   */
  fun cancel(url: String, destinationPath: Path) {
    activeJobs[url]?.cancel()
    queries.updateStatusByUrl(DownloadStatus.CANCELED, url)
    if (fileSystem.exists(destinationPath)) fileSystem.delete(destinationPath)
  }

  /**
   * Cancels all currently active downloads and deletes their temporary files.
   *
   * @param paths A map associating the download URLs to their respective file paths.
   */
  fun cancelAll(paths: Map<String, Path>) {
    activeJobs.keys.toList().forEach { url ->
      paths[url]?.let { cancel(url, it) }
    }
  }

  /**
   * Provides a reactive stream of the entire download history.
   * * The UI can collect this [Flow] to automatically stay synchronized with the database.
   * Any insertion or status update triggered by the fetcher will immediately emit a new list.
   *
   * @return A [Flow] emitting the list of all recorded [DownloadHistoryEntity] rows.
   */
  fun getAllHistoryStream(): Flow<List<DownloadHistoryEntity>> {
    return queries.getAllHistory()
      .asFlow()
      .mapToList(Dispatchers.Default)
  }

  /**
   * Deletes a specific download record from the database history by its ID.
   * This does not delete the physical file, only the record in the database.
   *
   * @param id The unique database ID of the record to delete.
   */
  fun delete(id: Long) {
    queries.deleteById(id)
  }

  /**
   * Searches for download data based on URL.
   * Useful for checking whether a file is already in the queue.
   */
  suspend fun getHistoryByUrl(url: String): DownloadHistoryEntity? {
    return queries.getByUrl(url).executeAsOneOrNull()
  }

}