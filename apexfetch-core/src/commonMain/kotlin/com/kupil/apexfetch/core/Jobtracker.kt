/*
 * Created by Muhamad Syafii
 * 21/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch.core

import kotlinx.coroutines.Job

/**
 * Tracks active coroutine [Job]s by URL.
 *
 * Single Responsibility — this class only knows about job lifecycle.
 * It does not know about downloads, files, or databases.
 */
internal class JobTracker {

  private val activeJobs = mutableMapOf<String, Job>()

  fun isActive(url: String): Boolean = activeJobs.containsKey(url)

  fun register(url: String, job: Job) {
    activeJobs[url] = job
  }

  fun cancel(url: String) {
    activeJobs[url]?.cancel()
  }

  fun cancelAll() {
    activeJobs.keys.toList().forEach(::cancel)
  }

  fun remove(url: String) {
    activeJobs.remove(url)
  }

  fun allUrls(): List<String> = activeJobs.keys.toList()
}
