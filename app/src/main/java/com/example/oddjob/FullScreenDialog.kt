package com.example.oddjob

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList


class FullScreenDialog : DialogFragment(){

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val placeId = arguments?.getString("string")
        val jobDataReference = FirebaseDatabase.getInstance().getReference("jobs")
        val jobList: ArrayList<Job> = ArrayList()
        var rootView : View = inflater.inflate(R.layout.fullscreen_dialog, container, false)
        setStyle(STYLE_NO_TITLE, R.style.FullscreenDialogTheme)
        val addressTitle : TextView = rootView.findViewById(R.id.addressTitle)

        //get all the jobs that are associated with the address of the marker clicked and put them into an arraylist
        placeId?.let { jobDataReference!!.child(it) }?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (placeIdSnapshot in snapshot.children) {
                    val map : Map<String, String> = placeIdSnapshot.value as Map<String, String>
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
                    addressTitle.text = map.get("jobAddress")
                    jobList.add(newJob)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context,"An error occurred. Try again.", Toast.LENGTH_LONG).show()
            }
        })

        //set up recycler view and feed it the arraylist of jobs
        val recyclerView: RecyclerView = rootView.findViewById(R.id.job_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        val adapter = context?.let { JobAdapter(it, jobList, R.layout.job_item) }
        recyclerView.adapter = adapter
        adapter!!.notifyDataSetChanged()

        val exit : ImageButton = rootView.findViewById(R.id.fullscreen_dialog_close)

        exit.setOnClickListener{
            dismiss()
        }
        return rootView
    }

    // changes size of dialog fragment to match the screen size
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