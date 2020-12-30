package com.example.oddjob

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

lateinit var privateParent : ViewGroup

class MyJobAdapter(
        var List:List<Job>,
        private val layout: Int) : RecyclerView.Adapter<MyJobViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyJobViewHolder {
        privateParent = parent
        val v = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MyJobViewHolder(v)
    }

    override fun onBindViewHolder(holder: MyJobViewHolder, position: Int) {
        holder.bindItems(List[position])
    }

    override fun getItemCount() = List.size


}

class MyJobViewHolder(view: View) : RecyclerView.ViewHolder(view) {

      private var myTitle: TextView = view.findViewById(R.id.myTitle)
      private var myAddress: TextView = view.findViewById(R.id.myAddress)
      private var myPrice: TextView = view.findViewById(R.id.myPrice)
      private var myDescription: TextView = view.findViewById(R.id.myDescription)
      private var myJobId: TextView = view.findViewById(R.id.myJobId)
      private var delete: TextView = view.findViewById(R.id.myDelete)

    fun bindItems (item : Job) {
        myTitle.text = item.title
        myPrice.text = "$" + item.price
        myDescription.text = item.description
        myJobId.text = item.jobId
        myAddress.text = item.jobAddress

        delete.setOnClickListener {
            val mDatabase = FirebaseDatabase.getInstance().getReference()
            mDatabase.child("jobs/${item.placeId}").child(myJobId.text.toString()).removeValue()
        }
        //placeId.text = item.placeId
    }
}