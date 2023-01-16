package com.sefa.nearatm

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.sefa.nearatm.databinding.ActivityMainBinding
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var izinKontrol=0
    private lateinit var fused:FusedLocationProviderClient
    private  lateinit var locationTask:Task<Location>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fused=LocationServices.getFusedLocationProviderClient(this)

        binding.button.setOnClickListener {
            getPermission()
        }

    }

    private fun getLocation()
    {
        locationTask.addOnSuccessListener {
            if (it !=null)
            {
                Log.e("konum",("Lat :"+it.latitude.toString()+" Long : "+it.longitude.toString()))
            }
            else
            {
                Log.e("konum","alınamadı")

            }
        }
    }

    private fun getPermission() {

        izinKontrol=ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)

        if (izinKontrol !=PackageManager.PERMISSION_GRANTED) //izin onaylanmamışsa
        {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),100)

        }
        else //izin onaylanmışsa
        {
            locationTask=fused.lastLocation
            getLocation()
        }


    }
    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        if (requestCode==100)
        {
            izinKontrol=ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)

            if (grantResults.size>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED)
            {
                Snackbar.make(binding.button,"İzin kabul edildi",Snackbar.LENGTH_LONG).show()

                locationTask=fused.lastLocation
                getLocation()
            }
            else
            {
                Snackbar.make(binding.button,"İzin reddedildi",Snackbar.LENGTH_LONG).show()
            }
        }
    }
}