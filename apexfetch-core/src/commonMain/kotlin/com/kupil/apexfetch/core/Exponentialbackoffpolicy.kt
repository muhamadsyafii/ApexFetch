/*
 * Created by Muhamad Syafii
 * 21/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch.core

import kotlin.math.pow

/**
 * Exponential backoff retry policy.
 *
 * Delay formula: 2^retryCount seconds
 * - Retry 1 → 2s
 * - Retry 2 → 4s
 * - Retry 3 → 8s
 *
 * Swap this with [LinearBackoffPolicy] or [NoRetryPolicy] without touching [DownloadExecutorImpl].
 */
internal class ExponentialBackoffPolicy(
  override val maxRetries: Int = 3
) : RetryPolicy {
  override fun delayMillisFor(retryCount: Int): Long {
    return (2.0.pow(retryCount).toLong()) * 1000L
  }
}