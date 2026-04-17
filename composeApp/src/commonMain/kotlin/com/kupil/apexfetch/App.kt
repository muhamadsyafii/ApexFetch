package com.kupil.apexfetch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath

@Composable
fun App(fetcher: ApexFetcher) {
    MaterialTheme {
        var url by remember { mutableStateOf("https://nbg1-speed.hetzner.com/100MB.bin") }
        var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("ApexFetch Tester", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Download URL") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        val fileName = url.substringAfterLast("/")
                        val homeDir = System.getProperty("user.home")
//                        val tempPath = fileName.toPath()
                        val tempPath = "$homeDir/Downloads/$fileName".toPath()

                        fetcher.download(url, tempPath).collect { state ->
                            downloadState = state
                        }
                    }
                },
                enabled = downloadState !is DownloadState.Downloading
            ) {
                Text("Start Download")
            }

            Spacer(modifier = Modifier.height(32.dp))

            when (val state = downloadState) {
                is DownloadState.Idle -> Text("Ready to download.")
                is DownloadState.Connecting -> Text("Connecting to server...")
                is DownloadState.Downloading -> {
                    Text("Downloading... ${state.progress}%")
                    LinearProgressIndicator(
                    progress = { state.progress / 100f },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = ProgressIndicatorDefaults.linearColor,
                    trackColor = ProgressIndicatorDefaults.linearTrackColor,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                    )
                    Text("${state.downloadedBytes} / ${state.totalBytes} bytes")
                }
                is DownloadState.Success -> Text("Success! Saved to:\n${state.filePath}", color = MaterialTheme.colorScheme.primary)
                is DownloadState.Error -> Text("Error: ${state.exception.message}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}