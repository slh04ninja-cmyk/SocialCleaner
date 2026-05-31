package com.socialcleaner.ui

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.socialcleaner.R
import com.socialcleaner.model.YearGroup

class YearAdapter(
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<YearAdapter.ViewHolder>() {

    private var items = listOf<YearGroup>()
    private val expandedYears = mutableSetOf<Int>()

    fun setData(data: List<YearGroup>) {
        items = data
        if (expandedYears.isEmpty() && data.isNotEmpty()) {
            expandedYears.add(data.first().year)
        }
        notifyDataSetChanged()
    }

    fun getAllAdapter(): AppResultAdapter? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_year_group, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvYear: TextView = itemView.findViewById(R.id.tvYear)
        private val tvYearSummary: TextView = itemView.findViewById(R.id.tvYearSummary)
        private val ivExpand: ImageView = itemView.findViewById(R.id.ivExpand)
        private val rvApps: RecyclerView = itemView.findViewById(R.id.rvApps)
        private val headerLayout: View = itemView.findViewById(R.id.yearHeaderLayout)

        fun bind(yearGroup: YearGroup) {
            tvYear.text = "📅 ${yearGroup.year}"
            tvYearSummary.text = "${yearGroup.totalFiles} fichiers • ${com.socialcleaner.model.formatSize(yearGroup.totalSize)}"

            val isExpanded = expandedYears.contains(yearGroup.year)
            rvApps.visibility = if (isExpanded) View.VISIBLE else View.GONE

            val rotation = if (isExpanded) 180f else 0f
            ivExpand.rotation = rotation

            headerLayout.setOnClickListener {
                if (expandedYears.contains(yearGroup.year)) {
                    expandedYears.remove(yearGroup.year)
                } else {
                    expandedYears.add(yearGroup.year)
                }
                notifyItemChanged(adapterPosition)
            }

            if (isExpanded) {
                val appAdapter = AppResultAdapter(onSelectionChanged)
                rvApps.layoutManager = LinearLayoutManager(itemView.context)
                rvApps.adapter = appAdapter
                appAdapter.setData(yearGroup.apps)
            }
        }
    }
}
