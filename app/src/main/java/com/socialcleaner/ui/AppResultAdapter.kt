package com.socialcleaner.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.socialcleaner.R
import com.socialcleaner.model.AppScanResult
import com.socialcleaner.model.MediaCategory
import com.socialcleaner.model.formatSize

class AppResultAdapter(
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<AppResultAdapter.ViewHolder>() {

    private var items = listOf<AppScanResult>()
    private val selectedFiles = mutableSetOf<String>()

    fun setData(data: List<AppScanResult>) {
        items = data
        notifyDataSetChanged()
    }

    fun getSelectedFiles(): Set<String> = selectedFiles.toSet()

    fun getSelectedSize(): Long {
        return items.flatMap { it.categories }
            .flatMap { it.files }
            .filter { selectedFiles.contains(it.path) }
            .sumOf { it.size }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvAppSummary: TextView = itemView.findViewById(R.id.tvAppSummary)
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val categoriesContainer: LinearLayout = itemView.findViewById(R.id.categoriesContainer)
        private val cbSelectAll: CheckBox = itemView.findViewById(R.id.cbSelectAll)

        fun bind(result: AppScanResult) {
            val ctx = itemView.context
            tvAppName.text = result.appName
            tvAppSummary.text = ctx.getString(R.string.files_and_size,
                result.totalFiles, formatSize(ctx, result.totalSize))

            val iconRes = when (result.appIcon) {
                "whatsapp" -> R.drawable.ic_whatsapp
                "telegram" -> R.drawable.ic_telegram
                "facebook" -> R.drawable.ic_facebook
                "instagram" -> R.drawable.ic_instagram
                "snapchat" -> R.drawable.ic_snapchat
                "tiktok" -> R.drawable.ic_tiktok
                "messenger" -> R.drawable.ic_messenger
                "signal" -> R.drawable.ic_signal
                else -> R.drawable.ic_app_default
            }
            ivAppIcon.setImageResource(iconRes)

            categoriesContainer.removeAllViews()
            val allPaths = result.categories.flatMap { it.files.map { f -> f.path } }

            cbSelectAll.setOnCheckedChangeListener(null)
            cbSelectAll.isChecked = allPaths.isNotEmpty() && allPaths.all { selectedFiles.contains(it) }
            cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedFiles.addAll(allPaths)
                } else {
                    selectedFiles.removeAll(allPaths.toSet())
                }
                notifyDataSetChanged()
                onSelectionChanged()
            }

            for (category in result.categories) {
                val categoryView = LayoutInflater.from(ctx)
                    .inflate(R.layout.item_category, categoriesContainer, false)

                val tvCategoryIcon = categoryView.findViewById<ImageView>(R.id.tvCategoryIcon)
                val tvCategoryName = categoryView.findViewById<TextView>(R.id.tvCategoryName)
                val tvCategoryInfo = categoryView.findViewById<TextView>(R.id.tvCategoryInfo)
                val cbCategory = categoryView.findViewById<CheckBox>(R.id.cbCategory)

                tvCategoryIcon.setImageResource(getCategoryIcon(category))
                tvCategoryName.text = category.getLocalizedName(ctx)
                tvCategoryInfo.text = ctx.getString(R.string.files_and_size,
                    category.fileCount, formatSize(ctx, category.totalSize))

                val categoryPaths = category.files.map { it.path }
                cbCategory.setOnCheckedChangeListener(null)
                cbCategory.isChecked = categoryPaths.isNotEmpty() && categoryPaths.all { selectedFiles.contains(it) }
                cbCategory.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedFiles.addAll(categoryPaths)
                    } else {
                        selectedFiles.removeAll(categoryPaths.toSet())
                    }
                    notifyDataSetChanged()
                    onSelectionChanged()
                }

                categoriesContainer.addView(categoryView)
            }
        }

        private fun getCategoryIcon(category: MediaCategory): Int {
            return when (category.icon) {
                "image" -> R.drawable.ic_picture
                "video" -> R.drawable.ic_play
                "document" -> R.drawable.ic_files
                "mic" -> R.drawable.ic_mic
                "music" -> R.drawable.ic_mic
                "sticker" -> R.drawable.ic_picture
                "video_note" -> R.drawable.ic_play
                "gif" -> R.drawable.ic_gif
                else -> R.drawable.ic_files
            }
        }
    }
}
