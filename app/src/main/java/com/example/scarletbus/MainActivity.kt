package com.example.scarletbus

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.location.LocationListener



class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Inflate views
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            val fragment = NearbyFragment()
            supportFragmentManager.beginTransaction().add(R.id.container, fragment, "nearby_fragment").commit()
        }


        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            val fm = supportFragmentManager
            val currentFragment = fm.findFragmentById(R.id.container)
            if (currentFragment != null) {
                fm.beginTransaction()
                    .detach(currentFragment)
                    .commit()
            }
            when (item.itemId) {
                R.id.navigation_nearby -> {
                    var nearbyFragment = fm.findFragmentByTag("nearby_fragment")


                    if (nearbyFragment == null) {
                        nearbyFragment = NearbyFragment()
                        fm.beginTransaction()
                            .add(R.id.container, nearbyFragment, "nearby_fragment")
                            .commit()
                    } else {
                        fm.beginTransaction()
                            .attach(nearbyFragment)
                            .commit()
                    }
                }
                R.id.navigation_routes -> {
                    var routesFragment = fm.findFragmentByTag("routes_fragment")

                    if (routesFragment == null) {
                        routesFragment = NearbyFragment()
                        fm.beginTransaction()
                            .add(R.id.container, routesFragment, "routes_fragment")
                            .commit()
                    } else {
                        fm.beginTransaction()
                            .attach(routesFragment)
                            .commit()
                    }


                    val fragment = FragmentRoutes()
                    fm.beginTransaction()
                        .replace(R.id.container, fragment, fragment.javaClass.getSimpleName())
                        .commit()
                }
                R.id.navigation_stops -> {
                    val fragment = FragmentStops()
                    fm.beginTransaction()
                        .replace(R.id.container, fragment, fragment.javaClass.getSimpleName())
                        .commit()
                }
            }
            true
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 200)
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                             permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            200 -> {

                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i("Debugsz", "Permission has been denied by user")
                } else {
                    try {
                        // Request location updates
                        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?
                        locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener);
                    } catch(ex: SecurityException) {
                        Log.d("myTag", "Security Exception, no location available");
                    }
                }
            }
        }
    }

    //define the listener
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.d("myTag", "" + location.longitude + ":" + location.latitude);
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
}
