package com.example.scarletbus.models

import android.location.Location
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Stop(val name: String, val area: String, val location: Location, var routes: ArrayList<ServedRoute>?): Parcelable

@Parcelize
data class ServedRoute(val name: String, var times: List<Double>?): Parcelable