/*
 * Created by Muhamad Syafii
 * 21/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch.core

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.kupil.apexfetch.db.DownloadHistoryEntity
import com.kupil.apexfetch.db.DownloadHistoryQueries
import com.kupil.apexfetch.model.DownloadStatus
import com.kupil.apexfetch.util.getCurrentTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/**
 * SQLDelight-backed implementation of [DownloadRepository].
 *
 * Single Responsibility — only handles DB read/write operations.
 * If the DB engine changes (e.g. Room), only this class needs to change.
 */
internal class DownloadRepositoryImpl(
  private val queries: DownloadHistoryQueries
) : DownloadRepository {

  override fun initRecord(
    url: String,
    fileName: String,
    savedPath: String,
    existingTotalBytes: Long
  ): Long? {
    val existing = queries.getByUrl(url).executeAsOneOrNull()

    queries.insertOrReplace(
      id = existing?.id,
      url = url,
      fileName = fileName,
      savedPath = savedPath,
      totalBytes = existing?.totalBytes ?: existingTotalBytes,
      status = DownloadStatus.CONNECTING,
      timestamp = getCurrentTimeMillis()
    )

    return existing?.id ?: queries.getByUrl(url).executeAsOneOrNull()?.id
  }

  override fun updateStatus(url: String, status: String) {
    queries.updateStatusByUrl(status, url)
  }

  override fun updateRecord(
    id: Long?,
    url: String,
    fileName: String,
    savedPath: String,
    totalBytes: Long,
    status: String
  ) {
    queries.insertOrReplace(
      id = id,
      url = url,
      fileName = fileName,
      savedPath = savedPath,
      totalBytes = totalBytes,
      status = status,
      timestamp = getCurrentTimeMillis()
    )
  }

  override fun getAllHistory(): Flow<List<DownloadHistoryEntity>> {
    return queries.getAllHistory()
      .asFlow()
      .mapToList(Dispatchers.Default)
  }

  override fun deleteById(id: Long) {
    queries.deleteById(id)
  }

  override suspend fun getByUrl(url: String): DownloadHistoryEntity? {
    return queries.getByUrl(url).executeAsOneOrNull()
  }
}
