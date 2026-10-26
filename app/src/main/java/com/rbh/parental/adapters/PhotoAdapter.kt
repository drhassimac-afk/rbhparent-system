package com.rbh.parental.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rbh.parental.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PhotoAdapter(
    private val photos: List<File>,
    private val onClick: (File) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val photo: ImageView = v.findViewById(R.id.ivPhoto)
        val name: TextView = v.findViewById(R.id.tvPhotoName)
        val delete: ImageButton = v.findViewById(R.id.btnDeletePhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val file = photos[position]

        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, opts)
        holder.photo.setImageBitmap(bitmap)

        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        holder.name.text = "${sdf.format(Date(file.lastModified()))} (${file.length() / 1024}KB)"

        holder.photo.setOnClickListener { onClick(file) }
        holder.delete.setOnClickListener {
            file.delete()
            notifyItemRemoved(holder.adapterPosition)
        }
    }

    override fun getItemCount() = photos.size
}
