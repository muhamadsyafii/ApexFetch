/*
 * Created by Muhamad Syafii
 * 21/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch.core

/**
 * Defines the contract for a retry policy.
 *
 * Open/Closed Principle — [DownloadExecutor] is closed for modification,
 * but open for extension by swapping the [RetryPolicy] implementation.
 *
 * Example alternatives: NoRetryPolicy, LinearBackoffPolicy, CustomRetryPolicy.
 */
interface RetryPolicy {
  val maxRetries: Int
  fun delayMillisFor(retryCount: Int): Long
}
