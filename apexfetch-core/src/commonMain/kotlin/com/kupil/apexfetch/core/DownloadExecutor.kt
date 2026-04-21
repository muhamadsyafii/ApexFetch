/*
 * Created by Muhamad Syafii
 * 21/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch.core

import com.kupil.apexfetch.model.DownloadState

import kotlinx.coroutines.flow.FlowCollector
import okio.Path

/**
 * Defines the contract for executing a single HTTP download request.
 *
 * Abstracted as an interface to support:
 * - Dependency Inversion (ApexFetcher depends on this, not the concrete impl)
 * - Easy swapping/mocking in tests
 */
internal interface DownloadExecutor {
  suspend fun FlowCollector<DownloadState>.execute(
    url: String,
    destinationPath: Path,
    recordId: Long?
  )
}
