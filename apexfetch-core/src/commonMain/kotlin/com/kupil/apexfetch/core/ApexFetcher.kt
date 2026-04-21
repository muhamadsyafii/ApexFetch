/*
 * Created by Muhamad Syafii
 * 21/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch.core

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.kupil.apexfetch.db.ApexDatabase
import com.kupil.apexfetch.db.DownloadHistoryEntity
import com.kupil.apexfetch.model.DownloadState
import com.kupil.apexfetch.model.DownloadStatus
import com.kupil.apexfetch.network.HttpClientFactory
import com.kupil.apexfetch.util.getCurrentTimeMillis
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.io.readByteArray
import okio.FileSystem
import okio.Path
import okio.buffer
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.pow

/**
* The public-facing facade of the ApexFetch library.
*
* Orchestrates download lifecycle — initiation, pause, cancel, and history —
* by delegating to focused internal components:
* - [JobTracker]        → coroutine job lifecycle
* - [DownloadExecutor]  → HTTP execution + streaming
* - [DownloadRepository] → database persistence
* - [DiskManager]       → file system operations
*
* All internal fields are private. This class depends on abstractions, not concretions (DIP).
*
* @param database The SQLDelight [ApexDatabase] for download history persistence.
* @param client The Ktor [HttpClient] for network requests.
* @param fileSystem The Okio [FileSystem] for cross-platform file operations.
* @param bufferSize Chunk size in bytes for network read / disk write. Defaults to 64 KB.
* @param retryPolicy The [RetryPolicy] used on transient failures. Defaults to [ExponentialBackoffPolicy].
*/
class ApexFetcher(
  database: ApexDatabase,
  client: HttpClient = HttpClientFactory.create(),
  fileSystem: FileSystem = FileSystem.SYSTEM,
  bufferSize: Long = 64 * 1024L,
  retryPolicy: RetryPolicy = ExponentialBackoffPolicy()
) {
  private val repository: DownloadRepository = DownloadRepositoryImpl(
    queries = database.downloadHistoryQueries
  )

  private val diskManager = DiskManager(
    fileSystem = fileSystem,
    bufferSize = bufferSize
  )

  private val executor: DownloadExecutor = DownloadExecutorImpl(
    client = client,
    diskManager = diskManager,
    repository = repository,
    retryPolicy = retryPolicy
  )

  private val jobTracker = JobTracker()

  // ─── Download ────────────────────────────────────────────────────────────

  /**
   * Initiates or resumes a file download from the specified [url].
   *
   * Returns a cold [Flow] — download only begins on collection.
   * If a partial file exists at [destinationPath], download resumes from that byte offset.
   *
   * @param url Absolute URL of the file to download.
   * @param destinationPath Absolute Okio [Path] where the file will be saved.
   * @return A [Flow] emitting real-time [DownloadState].
   */
  fun download(url: String, destinationPath: Path): Flow<DownloadState> = flow {
    if (jobTracker.isActive(url)) return@flow
    currentCoroutineContext()[Job]?.let { jobTracker.register(url, it) }

    emit(DownloadState.Connecting)

    try {
      val recordId = repository.initRecord(
        url = url,
        fileName = destinationPath.name,
        savedPath = destinationPath.toString(),
        existingTotalBytes = 0L
      )
      with(executor) { execute(url, destinationPath, recordId) }
    } catch (e: Exception) {
      if (e is kotlinx.coroutines.CancellationException) throw e
      repository.updateStatus(url, DownloadStatus.FAILED)
      emit(DownloadState.Error(e))
    } finally {
      jobTracker.remove(url)
    }
  }

  // ─── Pause ───────────────────────────────────────────────────────────────

  /**
   * Pauses an active download by [url].
   * The partial file remains on disk for later resumption.
   */
  fun pause(url: String) {
    jobTracker.cancel(url)
    repository.updateStatus(url, DownloadStatus.PAUSED)
  }

  /** Pauses all currently active downloads. */
  fun pauseAll() {
    jobTracker.allUrls().forEach(::pause)
  }

  // ─── Cancel ──────────────────────────────────────────────────────────────

  /**
   * Cancels an active download and deletes the partial file from disk.
   *
   * @param url The URL of the download to cancel.
   * @param destinationPath The path where the file was being saved.
   */
  fun cancel(url: String, destinationPath: Path) {
    jobTracker.cancel(url)
    repository.updateStatus(url, DownloadStatus.CANCELED)
    diskManager.deleteIfExists(destinationPath)
  }

  /**
   * Cancels all active downloads and deletes their partial files.
   *
   * @param paths Map of download URLs to their respective file paths.
   */
  fun cancelAll(paths: Map<String, Path>) {
    jobTracker.allUrls().forEach { url ->
      paths[url]?.let { cancel(url, it) }
    }
  }

  // ─── History ─────────────────────────────────────────────────────────────

  /**
   * Reactive stream of the entire download history.
   * Emits a new list on any insertion or status update.
   */
  fun getAllHistoryStream(): Flow<List<DownloadHistoryEntity>> = repository.getAllHistory()

  /**
   * Deletes a download record from the database by [id].
   * Does not delete the physical file.
   */
  fun delete(id: Long) = repository.deleteById(id)

  /**
   * Retrieves a download record by [url], or null if not found.
   */
  suspend fun getHistoryByUrl(url: String): DownloadHistoryEntity? = repository.getByUrl(url)
}