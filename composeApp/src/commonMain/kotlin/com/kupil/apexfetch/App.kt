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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kupil.apexfetch.core.ApexFetcher
import com.kupil.apexfetch.db.DownloadHistoryEntity
import com.kupil.apexfetch.model.DownloadState
import com.kupil.apexfetch.model.DownloadStatus
import com.kupil.apexfetch.util.formatTimestamp
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
  fetcher: ApexFetcher,
  basePath: String,
  namePlatform : String,
  onNavigateToXml: (() -> Unit)? = null,
  onNavigateToNativeCompose: (() -> Unit)? = null
) {
  MaterialTheme {
    val scope = rememberCoroutineScope()
    var urlInput by remember { mutableStateOf("https://nbg1-speed.hetzner.com/100MB.bin") }
    val activeDownloads = remember { mutableStateMapOf<String, DownloadState>() }
    val historyList by fetcher.getAllHistoryStream().collectAsState(initial = emptyList())

    Scaffold(
      topBar = {
        CenterAlignedTopAppBar(title = {
          Text(
            "ApexFetch Dashboard",
            fontWeight = FontWeight.Bold
          )
        })
      }
    ) { padding ->
      Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {

        if (onNavigateToXml != null && onNavigateToNativeCompose != null) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = onNavigateToNativeCompose,
              modifier = Modifier.weight(1f),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
              Text("➡️ Native Compose")
            }
            Button(
              onClick = onNavigateToXml,
              modifier = Modifier.weight(1f),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
              Text("➡️ XML View")
            }
          }
        }


        Text(
          "Sample Download File Test $namePlatform",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Card(
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
              alpha = 0.5f
            )
          )
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              "Quick Test Files:",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary
            )
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = { urlInput = "https://nbg1-speed.hetzner.com/100MB.bin" },
                modifier = Modifier.weight(1f)
              ) { Text("100 MB") }

              OutlinedButton(
                onClick = { urlInput = "https://nbg1-speed.hetzner.com/1GB.bin" },
                modifier = Modifier.weight(1f)
              ) { Text("1 GB") }

              OutlinedButton(
                onClick = { urlInput = "https://nbg1-speed.hetzner.com/10GB.bin" },
                modifier = Modifier.weight(1f)
              ) { Text("10 GB") }
            }
            OutlinedTextField(
              value = urlInput,
              onValueChange = { urlInput = it },
              label = { Text("URL to Test") },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              trailingIcon = {
                if (urlInput.isNotEmpty()) {
                  IconButton(onClick = { urlInput = "" }) {
                    Icon(
                      imageVector = Icons.Default.Clear,
                      contentDescription = "Clear URL"
                    )
                  }
                }
              }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
              onClick = {
                val currentUrlToDownload = urlInput
                scope.launch {
                  val fileName = currentUrlToDownload.substringAfterLast("/")
                  val dest = "$basePath/$fileName".toPath()
                  fetcher.download(currentUrlToDownload, dest).collect { state ->
                    activeDownloads[currentUrlToDownload] = state
                  }
                }
              },
              modifier = Modifier.align(Alignment.End)
            ) {
              Icon(Icons.Default.PlayArrow, null)
              Spacer(Modifier.width(4.dp))
              Text("Start Test")
            }
            /*                        val currentTopState = activeDownloads[urlInput] ?: DownloadState.Idle
                                    MonitorProgress(state = currentTopState)*/
          }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text(
          "Download Queue (History)",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          if (historyList.isEmpty()) {
            item {
              Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No downloads in history.", color = Color.Gray)
              }
            }
          }
          items(historyList, key = { it.id }) { entity ->
            DownloadItemCard(
              entity = entity,
              liveState = activeDownloads[entity.url],
              onPause = { fetcher.pause(entity.url) },
              onResume = {
                scope.launch {
                  fetcher.download(entity.url, entity.savedPath.toPath()).collect { state ->
                    activeDownloads[entity.url] = state
                  }
                }
              },
              onCancel = { fetcher.cancel(entity.url, entity.savedPath.toPath()) },
              onDelete = { fetcher.delete(entity.id) }
            )
          }
        }
      }
    }
  }
}

@Composable
fun MonitorProgress(state: DownloadState) {
  Column(modifier = Modifier.padding(top = 8.dp)) {
    when (state) {
      is DownloadState.Downloading -> {
        Text("Progress: ${state.progress}%", style = MaterialTheme.typography.bodySmall)
        LinearProgressIndicator(
          progress = { state.progress / 100f },
          modifier = Modifier.fillMaxWidth()
        )
      }

      is DownloadState.Error -> {
        Text("Failed: ${state.exception.message}", color = MaterialTheme.colorScheme.error)
      }

      is DownloadState.Success -> {
        Text("Status: Success ✅", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
      }

      DownloadState.Connecting -> {
        Text("Connecting to server...", color = MaterialTheme.colorScheme.primary)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      }

      else -> Text("Status: Idle", color = Color.Gray)
    }
  }
}


@Composable
fun DownloadItemCard(
    entity: DownloadHistoryEntity,
    liveState: DownloadState?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
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
      val isDownloading = entity.status == DownloadStatus.DOWNLOADING
      val isPaused = entity.status == DownloadStatus.PAUSED
      if (isDownloading || isPaused) {
        if (liveState is DownloadState.Downloading) {
          LinearProgressIndicator(
            progress = { liveState.progress / 100f },
            modifier = Modifier.fillMaxWidth(),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
          )

          // Tampilkan angka MB-nya
          val currentMB = liveState.downloadedBytes / 1024 / 1024
          val totalMB = liveState.totalBytes / 1024 / 1024
          Text(
            text = "${liveState.progress}%  ($currentMB MB / $totalMB MB)",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp)
          )
        } else {
          var localBytes by remember { mutableStateOf(0L) }
          LaunchedEffect(entity.savedPath) {
            val file = File(entity.savedPath)
            if (file.exists()) {
              localBytes = file.length()
            }
          }

          if (localBytes > 0 && entity.totalBytes > 0) {
            val progressFloat = (localBytes.toDouble() / entity.totalBytes.toDouble()).toFloat()
            val currentMB = localBytes / 1024 / 1024
            val totalMB = entity.totalBytes / 1024 / 1024
            val progressPercent = (progressFloat * 100).toInt()

            LinearProgressIndicator(
              progress = { progressFloat },
              modifier = Modifier.fillMaxWidth(),
              strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Text(
              text = "$progressPercent%  ($currentMB MB / $totalMB MB)",
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.padding(top = 4.dp)
            )
          } else {
            LinearProgressIndicator(
              modifier = Modifier.fillMaxWidth(),
              strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Text(
              text = "Preparing...",
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.padding(top = 4.dp)
            )
          }
        }
      }


      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (isDownloading) {
          IconButton(onClick = onPause) {
            Icon(Icons.Default.Pause, contentDescription = "Pause")
          }
        } else if (isPaused || entity.status == DownloadStatus.FAILED) {
          IconButton(onClick = onResume) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
          }
        }
        if (entity.status != DownloadStatus.SUCCESS && entity.status != DownloadStatus.CANCELED) {
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

@Composable
fun StatusBadge(status: String) {
  val color = when (status) {
    DownloadStatus.SUCCESS -> Color(0xFF4CAF50)
    DownloadStatus.FAILED -> Color.Red
    DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
    DownloadStatus.PAUSED -> Color(0xFFFF9800)
    DownloadStatus.CONNECTING -> Color.Blue
    else -> Color.Gray
  }

  Surface(
    color = color.copy(alpha = 0.1f),
    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
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