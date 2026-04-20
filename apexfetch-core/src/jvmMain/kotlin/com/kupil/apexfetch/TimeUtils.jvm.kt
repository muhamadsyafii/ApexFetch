/*
 * Created by Muhamad Syafii
 * 17/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
actual fun formatTimestamp(timestamp: Long): String {
  val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
  return formatter.format(Date(timestamp))
}