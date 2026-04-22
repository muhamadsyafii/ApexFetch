/*
 * Created by Muhamad Syafii
 * 21/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kupil.apexfetch.compose.DownloadUiState
import com.kupil.apexfetch.compose.rememberMultiDownloadState
import com.kupil.apexfetch.core.ApexFetcher
import com.kupil.apexfetch.db.DownloadHistoryEntity
import com.kupil.apexfetch.model.DownloadStatus
import okio.Path.Companion.toPath
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Constants ──────────────────────────────────────────────────────────────────

private val QUICK_TEST_FILES = listOf(
  "100 MB" to "https://nbg1-speed.hetzner.com/100MB.bin",
  "1 GB"   to "https://nbg1-speed.hetzner.com/1GB.bin",
  "10 GB"  to "https://nbg1-speed.hetzner.com/10GB.bin",
)


/**
 * Native Compose sample screen with equivalent features to the CMP screen.
 *
 * Features:
 * - Free URL input + quick test buttons (100MB / 1GB / 10GB)
 * - Multiple concurrent downloads
 * - Download queue from history DB (reactive via Flow)
 * - Pause / Resume / Cancel / Delete per item
 *
 * Uses [rememberMultiDownloadState] from the `apexfetch-compose` module.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeNativeSampleScreen(
  fetcher: ApexFetcher,
  basePath: String,
  onBackPressed: () -> Unit
) {
  val scope = rememberCoroutineScope()

  val multiDownload = rememberMultiDownloadState(fetcher, scope)

  var urlInput by remember { mutableStateOf(QUICK_TEST_FILES.first().second) }
  val historyList by fetcher.getAllHistoryStream().collectAsState(initial = emptyList())

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text("Native Compose Test", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onBackPressed) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back"
            )
          }
        }
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .padding(padding)
        .padding(16.dp)
        .fillMaxSize()
    ) {

      // ── Section Label ──────────────────────────────────────────────
      Text(
        text = "Sample Download File Test (apexfetch-compose)",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(8.dp))

      // ── Input Card ─────────────────────────────────────────────────
      DownloadInputCard(
        urlInput = urlInput,
        onUrlChange = { urlInput = it },
        onStartClick = {
          val url = urlInput.trim()
          if (url.isBlank()) return@DownloadInputCard
          val fileName = url.substringAfterLast("/")
          multiDownload.start(url, "$basePath/$fileName".toPath())
        }
      )

      HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

      // ── History Queue ──────────────────────────────────────────────
      Text(
        text = "Download Queue (History)",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(8.dp))

      LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        if (historyList.isEmpty()) {
          item { EmptyHistoryPlaceholder() }
        }

        items(historyList, key = { it.id }) { entity ->
          NativeDownloadItemCard(
            entity = entity,
            liveState = multiDownload.stateOf(entity.url), // ✨ per-URL state
            onPause   = { multiDownload.pause(entity.url) },
            onResume  = { multiDownload.start(entity.url, entity.savedPath.toPath()) },
            onCancel  = { multiDownload.cancel(entity.url, entity.savedPath.toPath()) },
            onDelete  = { fetcher.delete(entity.id) }
          )
        }
      }
    }
  }
}

// ── Input Card ─────────────────────────────────────────────────────────────────

@Composable
private fun DownloadInputCard(
  urlInput: String,
  onUrlChange: (String) -> Unit,
  onStartClick: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    )
  ) {
    Column(modifier = Modifier.padding(16.dp)) {

      Text(
        text = "Quick Test Files:",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        QUICK_TEST_FILES.forEach { (label, url) ->
          OutlinedButton(
            onClick = { onUrlChange(url) },
            modifier = Modifier.weight(1f)
          ) { Text(label) }
        }
      }

      OutlinedTextField(
        value = urlInput,
        onValueChange = onUrlChange,
        label = { Text("URL to Test") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        trailingIcon = {
          if (urlInput.isNotEmpty()) {
            IconButton(onClick = { onUrlChange("") }) {
              Icon(Icons.Default.Close, contentDescription = "Clear URL")
            }
          }
        }
      )

      Spacer(modifier = Modifier.height(8.dp))

      Button(
        onClick = onStartClick,
        modifier = Modifier.align(Alignment.End)
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null)
        Spacer(Modifier.width(4.dp))
        Text("Start Test")
      }
    }
  }
}

// ── Download Item Card ─────────────────────────────────────────────────────────

@Composable
private fun NativeDownloadItemCard(
  entity: DownloadHistoryEntity,
  liveState: DownloadUiState,
  onPause: () -> Unit,
  onResume: () -> Unit,
  onCancel: () -> Unit,
  onDelete: () -> Unit,
) {
  val isActivelyDownloading = entity.status == DownloadStatus.DOWNLOADING
  val isPaused = entity.status == DownloadStatus.PAUSED

  Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {

      // ── Header ────────────────────────────────────────────────────
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = entity.fileName,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        StatusBadge(status = entity.status)
      }

      Text(
        text = formatTimestamp(entity.timestamp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(12.dp))

      if (isActivelyDownloading || isPaused) {
        DownloadProgress(
          liveState = liveState,
          isPaused = isPaused,
          dbDownloadedBytes = entity.downloadedBytes,
          dbTotalBytes = entity.totalBytes
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        when {
          isActivelyDownloading -> {
            IconButton(onClick = onPause) {
              Icon(Icons.Default.Pause, contentDescription = "Pause")
            }
          }
          isPaused || entity.status == DownloadStatus.FAILED -> {
            IconButton(onClick = onResume) {
              Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
            }
          }
        }

        if (entity.status != DownloadStatus.SUCCESS &&
          entity.status != DownloadStatus.CANCELED
        ) {
          IconButton(onClick = onCancel) {
            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.Red)
          }
        }

        IconButton(onClick = onDelete) {
          Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
        }
      }
    }
  }
}

// ── Progress Section ───────────────────────────────────────────────────────────

@Composable
private fun DownloadProgress(
  liveState: DownloadUiState,
  isPaused: Boolean,
  dbDownloadedBytes: Long,
  dbTotalBytes: Long
) {
  when {
    // Live dari hook — sedang download aktif
    liveState.isDownloading -> {
      LinearProgressIndicator(
        progress = { liveState.progress / 100f },
        modifier = Modifier.fillMaxWidth(),
        strokeCap = StrokeCap.Round
      )
      Text(
        text = "${liveState.progress}%  " +
          "(${liveState.downloadedBytes.toMB()} MB / ${liveState.totalBytes.toMB()} MB)",
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 4.dp)
      )
    }

    isPaused && dbTotalBytes > 0 -> {
      val progressFloat = dbDownloadedBytes.toFloat() / dbTotalBytes
      val progressPercent = (progressFloat * 100).toInt()
      LinearProgressIndicator(
        progress = { progressFloat },
        modifier = Modifier.fillMaxWidth(),
        strokeCap = StrokeCap.Round
      )
      Text(
        text = "$progressPercent%  (${dbDownloadedBytes.toMB()} MB / ${dbTotalBytes.toMB()} MB)",
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 4.dp)
      )
    }

    else -> {
      LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth(),
        strokeCap = StrokeCap.Round
      )
      Text(
        text = "Preparing...",
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 4.dp)
      )
    }
  }
}

// ── Status Badge ───────────────────────────────────────────────────────────────

@Composable
private fun StatusBadge(status: String) {
  val color = when (status) {
    DownloadStatus.SUCCESS     -> Color(0xFF4CAF50)
    DownloadStatus.FAILED      -> Color.Red
    DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
    DownloadStatus.PAUSED      -> Color(0xFFFF9800)
    DownloadStatus.CONNECTING  -> Color.Blue
    else                       -> Color.Gray
  }
  Surface(
    color = color.copy(alpha = 0.1f),
    shape = RoundedCornerShape(4.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
  ) {
    Text(
      text = status.uppercase(),
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
      style = MaterialTheme.typography.labelSmall,
      color = color,
      fontWeight = FontWeight.ExtraBold
    )
  }
}

@Composable
private fun EmptyHistoryPlaceholder() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(32.dp),
    contentAlignment = Alignment.Center
  ) {
    Text("No downloads in history.", color = Color.Gray)
  }
}

private fun Long.toMB(): Long = this / 1024 / 1024

private fun formatTimestamp(timestamp: Long): String {
  val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
  return fmt.format(Date(timestamp))
}