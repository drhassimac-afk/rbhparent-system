package com.rbh.parental.activities

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rbh.parental.R
import com.rbh.parental.adapters.AppListAdapter
import com.rbh.parental.models.AppInfo

class BlockAppsActivity : AppCompatActivity() {

    private lateinit var adapter: AppListAdapter
    private val allApps = mutableListOf<AppInfo>()
    private val filteredApps = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_apps)

        val rv = findViewById<RecyclerView>(R.id.rvApps)
        val etSearch = findViewById<EditText>(R.id.etSearch)

        adapter = AppListAdapter(filteredApps) { app, isBlocked ->
            app.isBlocked = isBlocked
            getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
                .edit()
                .apply {
                    if (isBlocked) putBoolean(app.packageName, true)
                    else remove(app.packageName)
                }
                .apply()
        }

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        loadApps()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterApps(s.toString())
            }
        })
    }

    private fun loadApps() {
        val pm = packageManager
        val blocked = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)

        allApps.clear()
        pm.getInstalledApplications(0)
            .filter { it.packageName != packageName }
            .sortedBy { pm.getApplicationLabel(it).toString() }
            .forEach { app ->
                val icon = try { pm.getApplicationIcon(app) } catch (e: Exception) { null }
                allApps.add(AppInfo(
                    packageName = app.packageName,
                    appName = pm.getApplicationLabel(app).toString(),
                    icon = icon,
                    isBlocked = blocked.getBoolean(app.packageName, false)
                ))
            }

        filteredApps.clear()
        filteredApps.addAll(allApps)
        adapter.notifyDataSetChanged()
    }

    private fun filterApps(q: String) {
        filteredApps.clear()
        if (q.isEmpty()) {
            filteredApps.addAll(allApps)
        } else {
            filteredApps.addAll(allApps.filter {
                it.appName.contains(q, true) || it.packageName.contains(q, true)
            })
        }
        adapter.notifyDataSetChanged()
    }
}
