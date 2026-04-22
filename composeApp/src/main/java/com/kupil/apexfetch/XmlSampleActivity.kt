package com.kupil.apexfetch

/*
 * Created by Muhamad Syafii
 * 21/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.kupil.apexfetch.adapter.DownloadHistoryAdapter
import com.kupil.apexfetch.adapter.DownloadItemUI
import com.kupil.apexfetch.core.ApexFetcher
import com.kupil.apexfetch.databinding.ActivityXmlSampleBinding
import com.kupil.apexfetch.db.ApexDatabase
import com.kupil.apexfetch.db.DownloadHistoryEntity
import com.kupil.apexfetch.model.DownloadState
import com.kupil.apexfetch.xml.ApexDownloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath

class XmlSampleActivity : AppCompatActivity() {

  private val binding by lazy { ActivityXmlSampleBinding.inflate(layoutInflater) }

  private lateinit var historyAdapter: DownloadHistoryAdapter
  private val activeDownloads = mutableMapOf<String, DownloadState>()
  private var historyList = listOf<DownloadHistoryEntity>()

  private val basePath by lazy {
    applicationContext.filesDir.absolutePath
  }

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
    if (isGranted) executeDownload()
    else Toast.makeText(this, "Izin penyimpanan ditolak!", Toast.LENGTH_SHORT).show()
  }

  // 4. KMP Dependencies (Database & Fetcher)
  private val database by lazy {
    val driver = AndroidSqliteDriver(
      schema = ApexDatabase.Schema,
      context = applicationContext,
      name = "apexfetch_history.db"
    )
    ApexDatabase(driver)
  }

  private val fetcher by lazy { ApexFetcher(database = database) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)

    initView()
    observeDatabase()
  }

  private fun initView() {
    with(binding) {
      // ✨ A. Setup RecyclerView (Ini yang sebelumnya hilang!)
      historyAdapter = DownloadHistoryAdapter(
        onPause = { url -> fetcher.pause(url) },
        onResume = { entity -> startDownloadTask(entity.url, entity.savedPath.toPath()) },
        onCancel = { entity ->
          fetcher.cancel(entity.url, entity.savedPath.toPath())
          activeDownloads.remove(entity.url)
          updateAdapter()
        },
        onDelete = { id -> fetcher.delete(id) }
      )
      rvHistory.layoutManager = LinearLayoutManager(this@XmlSampleActivity)
      rvHistory.adapter = historyAdapter

      // ✨ B. Setup Buttons
      btn100mb.setOnClickListener { etUrl.setText("https://nbg1-speed.hetzner.com/100MB.bin") }
      btn1gb.setOnClickListener { etUrl.setText("https://nbg1-speed.hetzner.com/1GB.bin") }
      btn10gb.setOnClickListener { etUrl.setText("https://nbg1-speed.hetzner.com/10GB.bin") }

      btnStart.setOnClickListener { checkPermissionAndDownload() }

      btnPause.setOnClickListener {
        val currentUrl = etUrl.text.toString().trim()
        if (currentUrl.isNotEmpty()) fetcher.pause(currentUrl)
      }
    }
  }


  private fun observeDatabase() {
    lifecycleScope.launch {
      fetcher.getAllHistoryStream().collect { list ->
        historyList = list
        updateAdapter()
      }
    }
  }

  private fun checkPermissionAndDownload() {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
      val isGranted = ContextCompat.checkSelfPermission(
        this, Manifest.permission.WRITE_EXTERNAL_STORAGE
      ) == PackageManager.PERMISSION_GRANTED

      if (isGranted) executeDownload()
      else requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    } else {
      executeDownload() // Android 10+ bisa langsung gas
    }
  }

  private fun executeDownload() {
    val currentUrl = binding.etUrl.text.toString().trim()
    if (currentUrl.isNotEmpty()) {
      val fileName = currentUrl.substringAfterLast("/")
      val destPath = "$basePath/$fileName".toPath()
      startDownloadTask(currentUrl, destPath)
    }
  }

  private fun startDownloadTask(url: String, destPath: okio.Path) {
    lifecycleScope.launch {
      fetcher.download(url, destPath).collect { state ->
        activeDownloads[url] = state
        updateAdapter()
      }
    }
  }

  private fun updateAdapter() {
    val uiItems = historyList.map { entity ->
      DownloadItemUI(
        entity = entity,
        liveState = activeDownloads[entity.url]
      )
    }
    historyAdapter.submitList(uiItems)
  }
}