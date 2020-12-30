package com.example.oddjob

import android.app.Activity
import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.w3c.dom.Text
import java.io.IOException
import java.util.*


class CustomDialogFragment : DialogFragment(){

    lateinit var tempLatitude:String
    lateinit var tempLongitude:String
    lateinit var tempPlaceId:String


    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog!!)
        val activity: Activity? = activity
        if (activity is DialogInterface.OnDismissListener) {
            (activity as DialogInterface.OnDismissListener).onDismiss(dialog)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        var rootView : View = inflater.inflate(R.layout.layout_dialog, container, false)

        val exit : ImageButton = rootView.findViewById(R.id.fullscreen_dialog_close)

        exit.setOnClickListener{
            dismiss()
        }

        if (!Places.isInitialized()) {
            activity?.let { Places.initialize(it, "AIzaSyBWO2k96oeSm0tsm49V-pfObbXAgzXLC0I", Locale.US) };
        }
        // Initialize the AutocompleteSupportFragment.
        val autocompleteFragment =
            activity?.supportFragmentManager?.findFragmentById(R.id.autocomplete_fragment)
                    as AutocompleteSupportFragment

        val autocompleteView = autocompleteFragment.view?.findViewById(R.id.places_autocomplete_search_input) as EditText


        autocompleteFragment.view?.setBackgroundColor(Color.WHITE)

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME))

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val thread = Thread( Runnable {
                    val client = OkHttpClient()
                    val url = "https://maps.googleapis.com/maps/api/geocode/json?place_id=${place.id}" +
                            "&key=AIzaSyBWO2k96oeSm0tsm49V-pfObbXAgzXLC0I"

                    val request: okhttp3.Request = okhttp3.Request.Builder()
                            .url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        val reader = JSONObject(response.body()!!.string().toString())
                        val jsonArray = reader.getJSONArray("results")
                        val jsonObject: JSONObject = jsonArray.getJSONObject(0)
                        val jsonObjectGeometry: JSONObject = jsonObject.getJSONObject("geometry")
                        val jsonObjectLocation: JSONObject = jsonObjectGeometry.getJSONObject("location")
                        tempLatitude = jsonObjectLocation.getString("lat")
                        tempLongitude =  jsonObjectLocation.getString("lng")
                        tempPlaceId = place.id.toString()
                    }
                })
                thread.start()
            }

            override fun onError(status: Status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: $status")
            }
        })

        val postOddJob : TextView = rootView.findViewById(R.id.postJob)
        val jobTitle : TextView = rootView.findViewById(R.id.enterTitle)
        val jobPrice : TextView = rootView.findViewById(R.id.enterPrice)
        val jobDescription : TextView = rootView.findViewById(R.id.enterDescription)


        postOddJob.setOnClickListener{
            latitudeForMarker = tempLatitude.toDouble()
            longitudeForMarker = tempLongitude.toDouble()
            currentPlaceId = tempPlaceId
            currentTitle = jobTitle.text.toString()
            currentPrice = jobPrice.text.toString()
            currentDescription = jobDescription.text.toString()
            currentPlaceAddress = autocompleteView.text.toString()
            userPressedSubmitButton = true
            this.dismiss()
        }


        return rootView
    }
    interface Listener {
        fun onDismiss()
    }

    // properly destroys place autocomplete fragment so user can reopen it
    override fun onDestroy() {
        super.onDestroy()
        val fragManager = this.fragmentManager
        val fragment = fragManager!!.findFragmentById(R.id.autocomplete_fragment)
        if (fragment != null) {
            fragManager.beginTransaction().remove(fragment).commit()
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog: Dialog? = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.getWindow()?.setLayout(width, height)
        }
    }
}