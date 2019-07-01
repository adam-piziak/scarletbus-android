package com.example.scarletbus

import android.Manifest
import android.app.Activity
import android.app.ActivityOptions
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat.checkSelfPermission
import android.support.v4.content.ContextCompat.getSystemService
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.example.scarletbus.models.ServedRoute
import com.example.scarletbus.models.Stop
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.fragment_nearby.view.*
import kotlinx.android.synthetic.main.inner_item.view.*
import kotlinx.android.synthetic.main.nearby_list_item.view.*
import okhttp3.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.IOException
import kotlinx.serialization.*
import kotlinx.serialization.json.JSON
import kotlin.math.round
import kotlin.math.roundToInt

import android.support.v4.util.Pair as UtilPair

class NearbyFragment : Fragment() {
    private val client = OkHttpClient()
    private var stops = ArrayList<com.example.scarletbus.models.Stop>()
    private var mHandler: Handler? = null
    private var currentLocation: Location = Location("")
    private lateinit var recyclerView: RecyclerView
    private lateinit var nearbyStopAdapter: RecyclerView.Adapter<*>
    private lateinit var nearbyStopLayoutManager: RecyclerView.LayoutManager
    private var loaded = false
    private lateinit var progressBar: ProgressBar
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        currentLocation.longitude = 0.0
        currentLocation.latitude = 0.0
        // Inflate the layout
        val locationManager = (activity as Activity).getSystemService(LOCATION_SERVICE) as LocationManager?


        val root =  inflater.inflate(R.layout.fragment_nearby, container, false)
        progressBar = root.progressBar
        mHandler = Handler(Looper.getMainLooper())

        nearbyStopLayoutManager = LinearLayoutManager(getActivity())
        nearbyStopAdapter = MyAdapter(stops, currentLocation, activity as Activity)
        recyclerView = root.findViewById<RecyclerView>(R.id.my_recycler_view).apply {
            layoutManager = nearbyStopLayoutManager
            adapter = nearbyStopAdapter
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity as Activity)

        if (checkSelfPermission(this.activity as Activity,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location : Location? ->
                    currentLocation.set(location)
                    nearbyStopAdapter.notifyDataSetChanged()

                    // Got last known location. In some rare situations this can be null.
                }
            locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener);
        }

        Log.i("NETWORK", "start")

        get_stops()

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            val tempStops: ArrayList<Stop>? = savedInstanceState.getParcelableArrayList("stops")
            if (tempStops != null) {
                stops = tempStops
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList("stop_list", stops);
    }

    class InnerAdapter(private val routes: ArrayList<ServedRoute>) : RecyclerView.Adapter<InnerAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name = view.inner_item_name
            val time = view.inner_item_time
        }

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.inner_item, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, i: Int) {
            holder.name.text = routes[i].name
            val times = routes[i].times
            if (times != null && times.isNotEmpty()) {
                val eta = times[0]
                val now = System.currentTimeMillis()
                val min_left = ((eta - now)/(60*1000)).roundToInt()
                if (min_left > 0) {
                    holder.time.text = "${min_left} min"
                } else {
                    holder.time.text = "<1 min"
                }
            }
        }

        override fun getItemCount(): Int {
            return routes.size
        }
    }

    class MyAdapter(private val myDataset: ArrayList<Stop>, private val current_pos: Location?, private val context: Activity) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val root = view
            val name = view.item_nearby_stop
            val area = view.item_nearby_stop_area
            val distance = view.distance
            val routesView  = view.served_routes_rv
            var routes = ArrayList<ServedRoute>()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.nearby_list_item, parent, false) as LinearLayout
            v.served_routes_rv.layoutManager = LinearLayoutManager(parent.context)
            v.served_routes_rv.adapter = null
            return ViewHolder(v)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // On click listener
            holder.root.transitionName = "commonElement ${position}"
            holder.root.setOnClickListener {
                val intent = Intent(context, OpenActivity::class.java)
                intent.putExtra("name", myDataset[position].name)
                val options =
                    ActivityOptionsCompat.makeSceneTransitionAnimation(context,
                        UtilPair.create<View, String>(holder.root, "commonElement"))
                context.startActivity(intent, options.toBundle())
            }

            holder.name.text = myDataset[position].name
            holder.area.text = "Serves ${myDataset[position].area}"

            if (current_pos != null) {
                val distance = current_pos.distanceTo(myDataset[position].location)
                if (distance > 1000) {
                    holder.distance.text = "${round(distance/10)/100} km"
                } else {
                    holder.distance.text = "${round(distance)} m"
                }
            } else {
                holder.distance.text = "m"
            }
            val stopRoutes = myDataset[position].routes
            holder.routesView.visibility = View.GONE
            if (stopRoutes != null) {
                if (stopRoutes.size == 0) {
                    holder.routesView.visibility = View.GONE
                    return
                }
                holder.routesView.visibility = View.VISIBLE
                holder.routes = stopRoutes
                val adapter = holder.routesView.adapter
                if (adapter == null) {
                    holder.routesView.adapter = InnerAdapter(holder.routes)
                } else {
                    adapter.notifyDataSetChanged()
                }
            } else {
                holder.routesView.visibility = View.GONE
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = myDataset.size

        override fun getItemViewType(position: Int): Int {
            if (position > 2) {
                return 0
            }
            return 1
        }
    }



    @Serializable
    data class ScarletBusResponse(
        val data: DataContent?
    )

    @Serializable
    data class DataContent(
        val stops: List<StopObject?>
    )

    @Serializable
    data class StopObject(
        val name: String,
        val area: String,
        val location: List<Double>,
        val routes: List<RouteObject>
    )

    @Serializable
    data class RouteObject(
        val name: String,
        val arrivals: List<Double>?
    )

    /*
    class retrieveTasks() : AsyncTask<Void, Void, String>() {
        override fun doInBackground(vararg params: Void?): String? {
            // ...
        }

        override fun onPreExecute() {
            super.onPreExecute()
            // ...
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
        }
    }
    */
    fun get_stops() {
        doAsync {
            Log.i("Apple", "1")

            // Create request
            val body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                """
                {
                    "query": "{stops { name, area, location, routes(active: true) { name, arrivals} }}"
                }
                """.trimIndent())

            val request = Request.Builder()
                .url("https://api.scarletbus.com/graphql/")
                .post(body)
                .build()

            Log.i("Apple", "2")

            // call
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.i("FAILURE", "Request to scarletbus failed.")
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.i("Apple", "3")


                        Log.i("Apple", "4")
                        val string_res = response.body()?.string();
                        if (string_res != null) {
                            stops.clear()
                            Log.i("Apple", "4.1")
                            val res = JSON.parse(ScarletBusResponse.serializer(), string_res) // b is optional since it has default value
                            Log.i("Apple", "4.2")
                            res.data?.stops?.iterator()?.forEach {
                                Log.i("Apple", "-")
                                if (it != null) {
                                    val servedRoutes: ArrayList<ServedRoute> = ArrayList()
                                    it.routes.iterator().forEach {
                                        servedRoutes.add(ServedRoute(it.name, it.arrivals))
                                    }
                                    val location= Location("")
                                    location.latitude = it.location[1]
                                    location.longitude = it.location[0]
                                    stops.add(Stop(it.name, it.area, location, servedRoutes))
                                }
                            }
                            stops.sortBy { currentLocation.distanceTo(it.location) }
                        }
                    uiThread {
                        Log.i("Apple", "5")
                        if (currentLocation.latitude != 0.0 && currentLocation.longitude != 0.0) {
                            nearbyStopAdapter.notifyDataSetChanged()
                        }
                    }
                }
            })
        }
    }


    //define the listener
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            currentLocation.set(location)
            nearbyStopAdapter.notifyDataSetChanged()
            if (stops.size > 0 && !loaded) {
                loaded = true
                progressBar.visibility = View.GONE
            }
            Log.d("myTagz", "" + location.longitude + ":" + location.latitude);
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
}

