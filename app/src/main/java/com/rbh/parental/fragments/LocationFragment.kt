package com.rbh.parental.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.rbh.parental.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationFragment : Fragment() {

    private val permReq = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { getAndShowLocation() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_location, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // زر تحديث
        view.findViewById<View>(R.id.fabRefresh)?.setOnClickListener {
            if (hasPerm()) getAndShowLocation()
            else permReq.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }

        // زر فتح في خريطة Google
        view.findViewById<TextView>(R.id.tvMapPlaceholder)?.apply {
            setOnClickListener { openInMaps() }
        }

        getAndShowLocation()
    }

    private fun hasPerm() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun getAndShowLocation() {
        try {
            val lm = requireContext()
                .getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // قراءة آخر موقع محفوظ أولاً
            val savedPrefs = requireContext()
                .getSharedPreferences("location", Context.MODE_PRIVATE)

            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            val tv = view?.findViewById<TextView>(R.id.tvMapPlaceholder) ?: return

            if (loc != null) {
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                tv.text = "📍 الموقع الحالي\n\n" +
                    "خط العرض: ${loc.latitude}\n" +
                    "خط الطول: ${loc.longitude}\n" +
                    "الدقة: ${loc.accuracy}m\n" +
                    "الوقت: ${sdf.format(Date(loc.time))}\n\n" +
                    "👆 اضغط لفتح على الخريطة"
            } else {
                val lat = savedPrefs.getFloat("lat", 0f)
                val lng = savedPrefs.getFloat("lng", 0f)
                if (lat != 0f || lng != 0f) {
                    tv.text = "📍 آخر موقع محفوظ\n\n" +
                        "خط العرض: $lat\n" +
                        "خط الطول: $lng\n\n" +
                        "👆 اضغط لفتح على الخريطة"
                } else {
                    tv.text = "📍 لا يوجد موقع متاح\n\n" +
                        "اضغط زر التحديث للحصول على الموقع"
                }
            }
        } catch (e: SecurityException) { }
    }

    private fun openInMaps() {
        val prefs = requireContext()
            .getSharedPreferences("location", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("lat", 0f)
        val lng = prefs.getFloat("lng", 0f)

        if (lat != 0f || lng != 0f) {
            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                // فتح في المتصفح
                val webUri = Uri.parse("https://maps.google.com/?q=$lat,$lng")
                startActivity(Intent(Intent.ACTION_VIEW, webUri))
            }
        } else {
            Toast.makeText(requireContext(), "لا يوجد موقع متاح", Toast.LENGTH_SHORT).show()
        }
    }
}
