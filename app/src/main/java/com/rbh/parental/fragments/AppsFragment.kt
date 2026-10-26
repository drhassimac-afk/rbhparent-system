package com.rbh.parental.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rbh.parental.R
import com.rbh.parental.adapters.AppListAdapter
import com.rbh.parental.models.AppInfo

class AppsFragment : Fragment() {

    private lateinit var adapter: AppListAdapter
    private val allApps = mutableListOf<AppInfo>()
    private val filtered = mutableListOf<AppInfo>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_apps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvApps)
        val etSearch = view.findViewById<EditText>(R.id.etSearch)

        adapter = AppListAdapter(filtered) { app, blocked ->
            app.isBlocked = blocked
            requireContext().getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
                .edit()
                .apply {
                    if (blocked) putBoolean(app.packageName, true)
                    else remove(app.packageName)
                }
                .apply()
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
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
        val pm = requireContext().packageManager
        val blocked = requireContext()
            .getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)

        allApps.clear()
        pm.getInstalledApplications(0)
            .filter { it.packageName != requireContext().packageName }
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

        filtered.clear()
        filtered.addAll(allApps)
        adapter.notifyDataSetChanged()
    }

    private fun filterApps(q: String) {
        filtered.clear()
        if (q.isEmpty()) {
            filtered.addAll(allApps)
        } else {
            filtered.addAll(allApps.filter {
                it.appName.contains(q, true) || it.packageName.contains(q, true)
            })
        }
        adapter.notifyDataSetChanged()
    }
}
