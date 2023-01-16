package com.sefa.nearatm

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.sefa.nearatm.databinding.ActivityMapsBinding
import org.json.JSONException
import org.json.JSONObject
import java.text.DecimalFormat


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var izinKontrol=0
    private lateinit var fused: FusedLocationProviderClient
    private  lateinit var locationTask: Task<Location>
    lateinit var konum :Location
    private lateinit var currentLatLong:LatLng
    private val API_KEY="AIzaSyATGMGc25BNLlAzllIQLZULVtFt59IQ10E"
    val URL_PART1="https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="
    val URL_PART2forATM="&radius=3000&type=atm&key="
    val URL_PART2forBANK="&radius=3000&type=bank&key="
    private val decimalFormat=DecimalFormat("0000")
    private val decimalFormatter=DecimalFormat("0.000")

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fused= LocationServices.getFusedLocationProviderClient(this)

        Log.e("fused",fused.toString())

        getPermission()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.buttonLocation.setOnClickListener {

        getATM()
        getBANK()
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong,15f))

        }

        Log.e("oncreate","oncreate")
    }

    override fun onMapReady(googleMap: GoogleMap)
    {
        Log.e("onmapready1","onmapready1")
        mMap=googleMap
        statusCheck()
        mMap.uiSettings.isZoomControlsEnabled=true
        mMap.isTrafficEnabled = false;
        mMap.uiSettings.isMapToolbarEnabled = true;
        mMap.uiSettings.isCompassEnabled = true;
        mMap.uiSettings.isZoomGesturesEnabled = true;
        mMap.uiSettings.isScrollGesturesEnabled = true;
        mMap.uiSettings.isRotateGesturesEnabled = true;
        setUpMap()
        Log.e("onmapready2","onmapready2")

        mMap.setOnMapClickListener {
            Log.e("MAP","TIKLANDI")
            binding.cLATMs.visibility=View.INVISIBLE
        }


    }

    private fun setUpMap()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED)
            {

            return
        }
        mMap.isMyLocationEnabled=true
        fused.lastLocation.addOnSuccessListener { location->
            if (location !=null)
            {
                currentLatLong=LatLng(location.latitude,location.longitude)

               // placeMarkerOnMap(currentLatLong)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong,15f))
                getATM()
                getBANK()
            }
        }
    }

    fun statusCheck()
    {
        Log.e("statuscheck","statuscheck")

        val manager = getSystemService (Context.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
            val center=LatLng(39.69187658611501, 34.99914654755137)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center,5f))

        }
    }

    private fun buildAlertMessageNoGps()
    {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialog, id ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))

            })
            .setNegativeButton("No",
                DialogInterface.OnClickListener { dialog, id -> dialog.cancel() })
        val alert: AlertDialog = builder.create()
        alert.show()
    }

    private fun placeMarkerOnMap(currentLatLong: LatLng,title:String)
    {
        val markerOptions=MarkerOptions().position(currentLatLong).icon((BitmapDescriptorFactory
            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))
        markerOptions.title(title)
        mMap.addMarker(markerOptions)

        mMap.setOnMarkerClickListener { marker ->

            Log.e("marker",marker.title.toString())
            Log.e("marker",marker.position.toString())
            marker.showInfoWindow()

            //getDistance("${currentLatLong.latitude},${currentLatLong.longitude}","${marker.position.latitude},${marker.position.longitude}")
           // Log.e("dist",(calculateDist(currentLatLong.latitude,currentLatLong.longitude,marker.position.latitude,marker.position.longitude).toString()))
            //binding.textViewDistance.text="${decimalFormat.format(calculateDist(currentLatLong.latitude,currentLatLong.longitude,marker.position.latitude,marker.position.longitude))} km"

           // binding.textViewDistance.text=calculateDist(currentLatLong.latitude,currentLatLong.longitude,marker.position.latitude,marker.position.longitude).toString()


            binding.textViewTitle.text=marker.title
            binding.cLATMs.visibility=View.VISIBLE

            getDistance("${currentLatLong.latitude},${currentLatLong.longitude}","${marker.position.latitude},${marker.position.longitude}")
            Log.e("dist",(calcDist(currentLatLong.latitude,currentLatLong.longitude,marker.position.latitude,marker.position.longitude).toString()))
            binding.textViewDistance.text="${decimalFormatter.format(calcDist(currentLatLong.latitude,currentLatLong.longitude,marker.position.latitude,marker.position.longitude))} mt."

            true
        }

    }

    private fun getLocation()
    {
        locationTask.addOnSuccessListener {
            if (it !=null)
            {
                konum= it
                currentLatLong= LatLng(it.latitude,it.longitude)
                //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong,17f))
                Log.e("konum",("Lat :"+it.latitude.toString()+" Long : "+it.longitude.toString()))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong,15f))
                getATM()
                getBANK()

            }
            else
            {
                Log.e("konum","alınamadı")
                getPermission()

            }
        }
    }

    private fun getPermission()
    {

        izinKontrol= ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)

        if (izinKontrol != PackageManager.PERMISSION_GRANTED) //izin onaylanmamışsa
        {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", DialogInterface.OnClickListener { dialog, id ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),100)
                    Log.e("izin","evet tıklandı")

                    if (fused!=null)
                    {
                        Log.e("fused",fused.toString())
                        locationTask=fused.lastLocation
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong,15f))
                        getATM()
                        getBANK()

                    }



                })
                .setNegativeButton("No",
                    DialogInterface.OnClickListener { dialog, id -> dialog.cancel() })
            val alert: AlertDialog = builder.create()
            alert.show()


        }
        else //izin onaylanmışsa
        {
            locationTask=fused.lastLocation
            Log.e("izin","onaylandı")
            getLocation()
        }


    }
    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        if (requestCode==100)
        {
            izinKontrol= ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)

            if (grantResults.size>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED)
            {
                Snackbar.make(binding.buttonLocation,"İzin kabul edildi", Snackbar.LENGTH_LONG).show()

                locationTask=fused.lastLocation
            }
            else
            {
                Snackbar.make(binding.buttonLocation,"İzin reddedildi", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    fun getATM()//Volley get api
    {
        val URL=URL_PART1+currentLatLong.latitude+","+currentLatLong.longitude+URL_PART2forATM+API_KEY
        val request=StringRequest(Request.Method.GET,URL, { response ->

        try {

            val jsonObject=JSONObject(response)
            val results=jsonObject.getJSONArray("results")


            for (i in 0 until results.length())
            {
                val atm=results.getJSONObject(i)
                val geometry=atm.getJSONObject("geometry")
                val location=geometry.getJSONObject("location")
                val lat=location.getDouble("lat")
                val lng=location.getDouble("lng")

                val name=atm.getString("name")

                Log.e("atm$i",lat.toString())
                Log.e("atm$i",lng.toString())
                Log.e("atm$i",name.toString())
                Log.e("*****","******")

                placeMarkerOnMap(LatLng(lat,lng),name)
            }

        }   catch (e:JSONException)
        {
            e.printStackTrace()
        }

        }, { e->e.printStackTrace()})

        Volley.newRequestQueue(this@MapsActivity).add(request)
    }

    fun getBANK()//Volley get api
    {
        val URL=URL_PART1+currentLatLong.latitude+","+currentLatLong.longitude+URL_PART2forBANK+API_KEY
        val request=StringRequest(Request.Method.GET,URL, { response ->

            Log.e("gelen veri",response.toString())

            try {

                val jsonObject=JSONObject(response)
                val results=jsonObject.getJSONArray("results")


                for (i in 0 until results.length())
                {
                    val atm=results.getJSONObject(i)
                    val geometry=atm.getJSONObject("geometry")
                    val location=geometry.getJSONObject("location")
                    val lat=location.getDouble("lat")
                    val lng=location.getDouble("lng")

                    val name=atm.getString("name")

                    Log.e("atm$i",lat.toString())
                    Log.e("atm$i",lng.toString())
                    Log.e("atm$i",name.toString())
                    Log.e("*****","******")

                    placeMarkerOnMap(LatLng(lat,lng),name)
                }

            }   catch (e:JSONException)
            {
                e.printStackTrace()
            }

        }, { e->e.printStackTrace()})

        Volley.newRequestQueue(this@MapsActivity).add(request)
    }

    fun getDistance(destination: String, origin: String)//Volley get api
    {
        Log.e("destination",destination)
        Log.e("origin",origin)
//https://maps.googleapis.com/maps/api/directions/json?destination=39.96756200000001,32.7915691&origin=39.9706638,32.7795806&key=AIzaSyCZs3vnvP8MC0U5bCUQd0fkdatM546SCKQ
        val URL=
            "https://maps.googleapis.com/maps/api/directions/json?destination=$destination&mode=walking&origin=$origin&key=AIzaSyCZs3vnvP8MC0U5bCUQd0fkdatM546SCKQ"
        val request=StringRequest(Request.Method.GET,URL, { response ->

            Log.e("gelen veri",response.toString())

            try {

                val jsonObject=JSONObject(response)
                val routes = jsonObject.getJSONArray("routes")
                val route1 = routes.getJSONObject(0)

                val legs = route1.getJSONArray("legs")
                val leg1 = legs.getJSONObject(0)
                val distance = leg1.getJSONObject("distance")

                Log.e("distance",distance.toString())
               /* val distance = leg1.getJSONObject("distance")
                markerDistanceTextToOrigin = distance.getString("text")
                markerDistanceValueToOrigin = distance.getString("value")

                val duration = leg1.getJSONObject("duration")
                markerDurationTextToOrigin = duration.getString("text")
                markerDurationValueToOrigin = duration.getString("value")

*/            }catch(e:JSONException)
            {
                e.printStackTrace()
            }

        }, { e->e.printStackTrace()})

        Volley.newRequestQueue(this@MapsActivity).add(request)
    }

    fun calculateDist(start: LatLng, end: LatLng): Double
    {
        val startPoint = Location("locationA")
        startPoint.latitude = start.latitude
        startPoint.longitude = start.longitude

        val endPoint = Location("locationA")
        endPoint.latitude = end.latitude
        endPoint.longitude = end.longitude

        return startPoint.distanceTo(endPoint).toDouble()
    }

    fun calcDist(lat1:Double, lon1:Double, lat2:Double, lon2:Double):Double
    {
        val theta: Double = lon1 - lon2
        var dist = (Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + (Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta))))
        dist = Math.acos(dist)
        dist = rad2deg(dist)
        dist = dist * 60 * 1.1515
        return dist
    }

    private fun deg2rad(deg: Double): Double {
        return deg * Math.PI / 180.0
    }

    private fun rad2deg(rad: Double): Double {
        return rad * 180.0 / Math.PI
    }





}