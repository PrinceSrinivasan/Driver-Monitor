package com.drowsiness.app.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.drowsiness.app.databinding.ItemHistoryBinding
import com.drowsiness.app.db.HistoryEntity

class HistoryAdapter : ListAdapter<HistoryEntity, HistoryAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HistoryEntity) {
            binding.tvDate.text = item.date
            binding.tvTime.text = item.time
            binding.tvEar.text = String.format("EAR: %.3f", item.ear)
            binding.tvStatus.text = item.status
            binding.tvStatus.setTextColor(
                when (item.status) {
                    "DROWSY" -> Color.parseColor("#FF3B5C")
                    "AWAKE" -> Color.parseColor("#00FF99")
                    else -> Color.parseColor("#00D4FF")
                }
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<HistoryEntity>() {
            override fun areItemsTheSame(oldItem: HistoryEntity, newItem: HistoryEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: HistoryEntity, newItem: HistoryEntity) =
                oldItem == newItem
        }
    }
}
