package com.rbh.parental.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rbh.parental.R
import com.rbh.parental.models.ToolItem

class ToolsAdapter(
    private val tools: List<ToolItem>,
    private val onClick: (ToolItem) -> Unit
) : RecyclerView.Adapter<ToolsAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val card: View = v.findViewById(R.id.cardTool)
        val icon: ImageView = v.findViewById(R.id.ivToolIcon)
        val title: TextView = v.findViewById(R.id.tvToolTitle)
        val desc: TextView = v.findViewById(R.id.tvToolDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tool, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = tools[position]
        holder.icon.setImageResource(t.iconRes)
        holder.title.text = t.title
        holder.desc.text = t.description
        holder.icon.setBackgroundColor(t.bgColor)
        holder.card.setOnClickListener { onClick(t) }
    }

    override fun getItemCount(): Int = tools.size
}
