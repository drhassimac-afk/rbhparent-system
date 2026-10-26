package com.rbh.parental.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rbh.parental.R
import com.rbh.parental.models.AppInfo

class UsageBarAdapter(
    private val apps: List<AppInfo>,
    private val totalMinutes: Long
) : RecyclerView.Adapter<UsageBarAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.ivIcon)
        val name: TextView = v.findViewById(R.id.tvName)
        val time: TextView = v.findViewById(R.id.tvTime)
        val bar: ProgressBar = v.findViewById(R.id.progressBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usage_bar, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = apps[position]
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.appName
        holder.time.text = app.getFormattedUsage()

        val percent = if (totalMinutes > 0)
            ((app.usageMinutesToday * 100) / totalMinutes).toInt() else 0
        holder.bar.progress = percent
    }

    override fun getItemCount() = apps.size
}
