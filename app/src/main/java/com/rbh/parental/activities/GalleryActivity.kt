package com.rbh.parental.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import androidx.viewpager2.widget.ViewPager2
import com.rbh.parental.R
import com.rbh.parental.adapters.PhotoAdapter
import com.rbh.parental.adapters.RecordingAdapter
import java.io.File

class GalleryActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        tabLayout.addTab(tabLayout.newTab().setText("📷 الصور"))
        tabLayout.addTab(tabLayout.newTab().setText("🎤 التسجيلات"))

        viewPager.adapter = GalleryPagerAdapter(this)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) viewPager.currentItem = tab.position
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val tab = tabLayout.getTabAt(position)
                    if (tab != null) tab.select()
                }
            })
    }

    inner class GalleryPagerAdapter(
        private val activity: GalleryActivity
    ) : RecyclerView.Adapter<GalleryPagerAdapter.VH>() {

        inner class VH(rv: RecyclerView) : RecyclerView.ViewHolder(rv) {
            val recycler: RecyclerView = rv
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val rv = RecyclerView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.MATCH_PARENT
                )
                clipToPadding = false
                setPadding(8, 8, 8, 8)
            }
            return VH(rv)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            if (position == 0) loadPhotos(holder.recycler)
            else loadRecordings(holder.recycler)
        }

        override fun getItemCount(): Int = 2

        private fun loadPhotos(rv: RecyclerView) {
            val dir = File(activity.getExternalFilesDir(null), "RBH_Photos")
            val files: List<File> = if (dir.exists()) {
                (dir.listFiles() ?: arrayOf()).sortedByDescending { it.lastModified() }
            } else {
                emptyList()
            }

            rv.layoutManager = GridLayoutManager(activity, 2)
            rv.adapter = PhotoAdapter(files) { file ->
                try {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        activity, "${activity.packageName}.provider", file
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(activity, "لا يمكن فتح الصورة", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun loadRecordings(rv: RecyclerView) {
            val dir = File(activity.getExternalFilesDir(null), "RBH_Audio")
            val files: List<File> = if (dir.exists()) {
                (dir.listFiles() ?: arrayOf()).sortedByDescending { it.lastModified() }
            } else {
                emptyList()
            }

            rv.layoutManager = LinearLayoutManager(activity)
            rv.adapter = RecordingAdapter(files) { file ->
                try {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        activity, "${activity.packageName}.provider", file
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "audio/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(activity, "لا يمكن تشغيل التسجيل", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
