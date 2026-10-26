package com.rbh.parental.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.rbh.parental.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecordingAdapter(
    private val recordings: List<File>,
    private val onPlay: (File) -> Unit
) : RecyclerView.Adapter<RecordingAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvFileName)
        val info: TextView = v.findViewById(R.id.tvFileInfo)
        val play: ImageButton = v.findViewById(R.id.btnPlay)
        val share: ImageButton = v.findViewById(R.id.btnShare)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recording, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val file = recordings[position]

        holder.name.text = file.name
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        val date = sdf.format(Date(file.lastModified()))
        val size = "${file.length() / 1024}KB"
        holder.info.text = "$date • $size"

        holder.play.setOnClickListener { onPlay(file) }

        holder.share.setOnClickListener {
            val ctx = holder.share.context
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(Intent.createChooser(shareIntent, "مشاركة التسجيل"))
        }
    }

    override fun getItemCount() = recordings.size
}
