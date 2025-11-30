package com.appcontrol.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.appcontrol.data.model.AppInfo
import com.appcontrol.databinding.ItemAppBinding

class AppListAdapter(
    private val onItemClick: (AppInfo) -> Unit
) : ListAdapter<AppInfo, AppListAdapter.AppViewHolder>(AppDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AppViewHolder(
        private val binding: ItemAppBinding,
        private val onItemClick: (AppInfo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(appInfo: AppInfo) {
            binding.textAppName.text = appInfo.appName
            binding.textPackageName.text = appInfo.packageName
            binding.imageAppIcon.setImageDrawable(appInfo.icon)

            if (appInfo.isBlocked) {
                binding.chipStatus.text = "Bloccata"
                binding.chipStatus.setChipBackgroundColorResource(android.R.color.holo_red_light)
            } else {
                binding.chipStatus.text = "Non bloccata"
                binding.chipStatus.setChipBackgroundColorResource(android.R.color.darker_gray)
            }

            binding.root.setOnClickListener {
                onItemClick(appInfo)
            }
        }
    }

    private class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem == newItem
        }
    }
}

