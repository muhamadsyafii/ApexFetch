/*
 * Created by Muhamad Syafii
 * 21/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kupil.apexfetch.databinding.ItemDownloadHistoryBinding
import com.kupil.apexfetch.db.DownloadHistoryEntity
import com.kupil.apexfetch.model.DownloadState

data class DownloadItemUI(
  val entity: DownloadHistoryEntity,
  val liveState: DownloadState?
)

class DownloadHistoryAdapter(
  private val onPause: (String) -> Unit,
  private val onResume: (DownloadHistoryEntity) -> Unit,
  private val onCancel: (DownloadHistoryEntity) -> Unit,
  private val onDelete: (Long) -> Unit
) : ListAdapter<DownloadItemUI, DownloadHistoryAdapter.ViewHolder>(DiffCallback) {

  // 👇 Gunakan parameter Binding di ViewHolder
  inner class ViewHolder(private val binding: ItemDownloadHistoryBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: DownloadItemUI) {
      val entity = item.entity
      val state = item.liveState

      // ✨ Akses view langsung lewat 'binding.' (Lebih aman dari null!)
      binding.tvFileName.text = entity.fileName
      binding.tvStatusBadge.text = entity.status.uppercase()
      binding.tvTimestamp.text = "File ID: ${entity.id}"

      val isDownloading = entity.status == "DOWNLOADING"
      val isPaused = entity.status == "PAUSED"

      // Logika Progress
      if (state is DownloadState.Downloading) {
        binding.progressBar.progress = state.progress
        val currentMB = state.downloadedBytes / 1024 / 1024
        val totalMB = state.totalBytes / 1024 / 1024
        binding.tvProgressText.text = "${state.progress}%  ($currentMB MB / $totalMB MB)"
      } else {
        if (entity.totalBytes > 0) {
          val currentMB = entity.totalBytes / 1024 / 1024
          val totalMB = entity.totalBytes / 1024 / 1024
          val percent = ((entity.totalBytes.toDouble() / entity.totalBytes) * 100).toInt()
          binding.progressBar.progress = percent
          binding.tvProgressText.text = "$percent%  ($currentMB MB / $totalMB MB)"
        } else {
          binding.progressBar.progress = 0
          binding.tvProgressText.text = "Preparing..."
        }
      }

      // Logika Tombol Play/Pause
      if (isDownloading) {
        binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        binding.btnPlayPause.setOnClickListener { onPause(entity.url) }
      } else {
        binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
        binding.btnPlayPause.setOnClickListener { onResume(entity) }
      }

      // Logika Tombol Cancel
      if (entity.status == "SUCCESS" || entity.status == "CANCELED") {
        binding.btnCancel.visibility = View.GONE
        binding.btnPlayPause.visibility = View.GONE
      } else {
        binding.btnCancel.visibility = View.VISIBLE
        binding.btnPlayPause.visibility = View.VISIBLE
        binding.btnCancel.setOnClickListener { onCancel(entity) }
      }

      // Logika Tombol Delete
      binding.btnDelete.setOnClickListener { onDelete(entity.id) }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    // 👇 Inflate menggunakan Binding
    val binding = ItemDownloadHistoryBinding.inflate(
      LayoutInflater.from(parent.context), parent, false
    )
    return ViewHolder(binding)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(getItem(position))
  }

  object DiffCallback : DiffUtil.ItemCallback<DownloadItemUI>() {
    override fun areItemsTheSame(oldItem: DownloadItemUI, newItem: DownloadItemUI) =
      oldItem.entity.id == newItem.entity.id

    override fun areContentsTheSame(oldItem: DownloadItemUI, newItem: DownloadItemUI) =
      oldItem == newItem
  }
}
