package com.example.oddjob

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.ismaeldivita.chipnavigation.ChipNavigationBar
import com.squareup.picasso.Picasso
import java.util.*
import kotlin.collections.HashMap
import kotlin.properties.Delegates

//global variables that hold the information of a job when it is just created
var latitudeForMarker by Delegates.notNull<Double>()
var longitudeForMarker by Delegates.notNull<Double>()
var currentPlaceId by Delegates.notNull<String>()
var currentTitle by Delegates.notNull<String>()
var currentPrice by Delegates.notNull<String>()
var currentDescription by Delegates.notNull<String>()
var currentPlaceAddress by Delegates.notNull<String>()
val markerMap: HashMap<String, Marker> = HashMap()
var userPressedSubmitButton : Boolean = false

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, DialogInterface.OnDismissListener {
    private val jobList: ArrayList<Job> = ArrayList()
    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val profile : View = findViewById(R.id.profileView)
        val myJobs : View = findViewById(R.id.my_job_recycler_view)
        val emptyRecycler : TextView = findViewById(R.id.emptyRecycler)

        //hide the profile view and the view that shows the user's own jobs
        profile.visibility = View.INVISIBLE
        myJobs.visibility = View.INVISIBLE
        emptyRecycler.visibility = View.INVISIBLE

        // method call to load user's jobs into the recycler view
        loadMyJobs(this, this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        loadProfile(this)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val coordinate = LatLng(39.9526, -82.1652)

        val location: CameraUpdate = CameraUpdateFactory.newLatLngZoom(
                coordinate, 10f)
        mMap.moveCamera(location)


        val profile : View = findViewById(R.id.profileView)
        val logout : TextView = findViewById(R.id.logout)
        val myJobs : View = findViewById(R.id.my_job_recycler_view)
        val dataReference = FirebaseDatabase.getInstance().getReference("jobs")
        val query = dataReference!!.orderByValue()

        //loads all the job markers into the map view
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                markerMap.clear()
                mMap.clear()
                for (placeIdSnapshot in dataSnapshot.children) {
                    for (jobSnapshot in placeIdSnapshot.children) {
                        val map : Map<String, String> = jobSnapshot.value as Map<String, String>

                        val newMarker = LatLng(map.get("lattitude") as Double, map.get("longitude") as Double)
                        val marker = mMap.addMarker(MarkerOptions().position(newMarker).title(map.get("placeId")))
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(newMarker))
                        markerMap.put(map.get("placeId")!!, marker)
                        println(markerMap)
                        val newJob =
                                Job(
                                        map.get("lattitude") as Double,
                                        map.get("longitude") as Double,
                                        map.get("title")!!,
                                        map.get("description")!!,
                                        map.get("placeId")!!,
                                        FirebaseAuth.getInstance().currentUser?.uid.toString(),
                                        map.get("price")!!,
                                        map.get("imageUrl")!!,
                                        map.get("userName")!!,
                                        map.get("userNumber")!!,
                                        map.get("jobAddress")!!,
                                        map.get("jobId")!!
                                )
                        jobList.add(newJob)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(applicationContext,  "An error occurred, please try again.", Toast.LENGTH_LONG).show()
            }
        })

        //click listener for marker that opens up a dialog that shows the jobs posted at that location
        mMap.setOnMarkerClickListener(object: GoogleMap.OnMarkerClickListener {

            override fun onMarkerClick(m: Marker): Boolean {

                var bigDialog = FullScreenDialog()
                val args = Bundle()
                args.putString("string", m.title)
                bigDialog.arguments = args
                bigDialog.show(supportFragmentManager, "bigDialog")
                return true
            }
        })

        val fab : FloatingActionButton = findViewById(R.id.fab)
        val edit : TextView = findViewById(R.id.editProfile)
        val mapSection : View = findViewById(R.id.mapSection)
        val navBar : ChipNavigationBar = findViewById(R.id.navBar)
        val emptyRecycler : TextView = findViewById(R.id.emptyRecycler)
        val pfp : ImageView = findViewById(R.id.pfp)

        fab.setOnClickListener{
            var dialog = CustomDialogFragment()
            dialog.show(supportFragmentManager, "customDialog")
        }

        Picasso.get()
                .load(FirebaseAuth.getInstance().currentUser?.photoUrl) // load the image
                .into(pfp)

        logout.setOnClickListener{
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        edit.setOnClickListener {
            val intent = Intent(this, EditActivity::class.java)
            startActivity(intent)
        }

        //click listeners for bottom nav bar
        navBar.setOnItemSelectedListener { id ->
            when (id) {
                R.id.myProfile -> {
                    myJobs.visibility = View.INVISIBLE
                    mapSection.visibility = View.INVISIBLE
                    profile.visibility = View.VISIBLE
                    emptyRecycler.visibility = View.INVISIBLE
                    loadProfile(this)
                }
                R.id.map ->{
                    myJobs.visibility = View.INVISIBLE
                    profile.visibility = View.INVISIBLE
                    mapSection.visibility = View.VISIBLE
                    emptyRecycler.visibility = View.INVISIBLE
                }
                else -> {
                    myJobs.visibility = View.VISIBLE
                    profile.visibility = View.INVISIBLE
                    mapSection.visibility = View.INVISIBLE
                    if (jobList.size == 0){
                        emptyRecycler.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    //method that gets the user's profile from the database and loads it into the profile view
    private fun loadProfile(activity: Activity){
        val database = FirebaseDatabase.getInstance().reference
        val ref: DatabaseReference = database.child("users")
        val userQuery = ref.orderByChild("uuid").equalTo(FirebaseAuth.getInstance().currentUser?.uid.toString())
        userQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val myName : TextView = activity.findViewById(R.id.myName)
                val myPhone : TextView = activity.findViewById(R.id.myNumber)
                val myEmail : TextView = activity.findViewById(R.id.myEmail)
                for (singleSnapshot in dataSnapshot.children) {
                    val map : Map<String, String> = singleSnapshot.value as Map<String, String>
                    myName.text = "Name: " + map.get("firstName")
                    myPhone.text = "Contact #: " + map.get("phoneNumber")
                    myEmail.text = "Email: " + map.get("email")
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(applicationContext,  "An error occurred, please try again.", Toast.LENGTH_LONG).show()
            }
        })
    }

    //method that gets the current user's own posted jobs from the database and posts them into a recycler view
    private fun loadMyJobs(context : Context, activity: Activity) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.toString()
        val dataReference = FirebaseDatabase.getInstance().getReference("jobs")
        val query = dataReference!!.orderByValue()
        val myJobList: ArrayList<Job> = ArrayList()

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                myJobList.clear()
                for (placeIdSnapshot in dataSnapshot.children) {
                    for (jobSnapshot in placeIdSnapshot.children) {
                        val map : Map<String, String> = jobSnapshot.value as Map<String, String>
                        if (map.get("userId") == currentUserId){
                            val newJob =
                                    Job(
                                            map.get("lattitude") as Double,
                                            map.get("longitude") as Double,
                                            map.get("title")!!,
                                            map.get("description")!!,
                                            map.get("placeId")!!,
                                            currentUserId,
                                            map.get("price")!!,
                                            map.get("imageUrl")!!,
                                            map.get("userName")!!,
                                            map.get("userNumber")!!,
                                            map.get("jobAddress")!!,
                                            map.get("jobId")!!
                                    )
                            myJobList.add(newJob)
                        }
                    }
                }
                val recyclerView: RecyclerView = activity.findViewById(R.id.my_job_recycler_view)
                recyclerView.layoutManager = LinearLayoutManager(context)
                recyclerView.adapter = null
                val adapter = MyJobAdapter(myJobList, R.layout.my_job_item)
                recyclerView.adapter = adapter
                adapter!!.notifyDataSetChanged()
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(applicationContext,  "An error occurred, please try again.", Toast.LENGTH_LONG).show()
            }
        })

    }

    //method that submits a new job to the database when the dialog fragment closes
    override fun onDismiss(dialog: DialogInterface?) {
        //a check to see if the dialog closed because the user pressed the submit button
        if (userPressedSubmitButton){
            userPressedSubmitButton = false
            val database = FirebaseDatabase.getInstance().reference
            val ref: DatabaseReference = database.child("users")
            val userQuery = ref.orderByChild("uuid").equalTo(FirebaseAuth.getInstance().currentUser?.uid.toString())
            userQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (singleSnapshot in dataSnapshot.children) {
                        val map : Map<String, String> = singleSnapshot.value as Map<String, String>
                        val newJob =
                                Job(
                                        latitudeForMarker,
                                        longitudeForMarker,
                                        currentTitle,
                                        currentDescription,
                                        currentPlaceId,
                                        FirebaseAuth.getInstance().currentUser?.uid.toString(),
                                        currentPrice,
                                        FirebaseAuth.getInstance().currentUser?.photoUrl.toString(),
                                        map.get("firstName")!!,
                                        map.get("phoneNumber")!!,
                                        currentPlaceAddress,
                                        ""
                                )

                        val mDatabase = FirebaseDatabase.getInstance().getReference()
                        val key = mDatabase.child("jobs/$currentPlaceId").push().key!!
                        newJob.jobId = key
                        mDatabase.child("jobs/$currentPlaceId").child(key).setValue(newJob)
                    }
                }
                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(applicationContext,  "An error occurred, please try again.", Toast.LENGTH_LONG).show()
                }
            })

        }

    }


}



