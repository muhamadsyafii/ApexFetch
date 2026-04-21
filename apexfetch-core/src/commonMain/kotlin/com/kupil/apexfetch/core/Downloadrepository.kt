/*
 * Created by Muhamad Syafii
 * 21/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch.core

import com.kupil.apexfetch.db.DownloadHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Defines the contract for persisting and querying download history.
 *
 * Separating this from [DownloadExecutor] respects Interface Segregation —
 * callers that only need history queries don't need to know about execution.
 */
internal interface DownloadRepository {
  fun initRecord(url: String, fileName: String, savedPath: String, existingTotalBytes: Long): Long?
  fun updateStatus(url: String, status: String)
  fun updateRecord(id: Long?, url: String, fileName: String, savedPath: String, totalBytes: Long, status: String)
  fun getAllHistory(): Flow<List<DownloadHistoryEntity>>
  fun deleteById(id: Long)
  suspend fun getByUrl(url: String): DownloadHistoryEntity?
}
