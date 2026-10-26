package com.rbh.parental.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import com.rbh.parental.R
import com.rbh.parental.models.AppInfo

class AppListAdapter(
    private val apps: List<AppInfo>,
    private val onBlockChanged: (AppInfo, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivIcon: ImageView = v.findViewById(R.id.ivAppIcon)
        val tvName: TextView = v.findViewById(R.id.tvAppName)
        val tvPkg: TextView = v.findViewById(R.id.tvPackage)
        val tvUsage: TextView = v.findViewById(R.id.tvUsage)
        val tvNotif: TextView = v.findViewById(R.id.tvNotifications)
        val sw: MaterialSwitch = v.findViewById(R.id.switchBlock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = apps[position]
        holder.ivIcon.setImageDrawable(app.icon)
        holder.tvName.text = app.appName
        holder.tvPkg.text = app.packageName
        holder.tvUsage.text = "Usage: ${app.getFormattedUsage()}"
        holder.tvNotif.text = if (app.notificationCount > 0) "${app.notificationCount}" else ""

        holder.sw.setOnCheckedChangeListener(null)
        holder.sw.isChecked = app.isBlocked
        holder.sw.setOnCheckedChangeListener { _, b ->
            onBlockChanged(app, b)
        }
    }

    override fun getItemCount(): Int = apps.size
}
