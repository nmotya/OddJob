package com.example.oddjob

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

class JobAdapter(
        var context: Context,
        var List:List<Job>,
        private val layout: Int) : RecyclerView.Adapter<JobViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return JobViewHolder(v)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        holder.bindItems(List[position])
    }

    override fun getItemCount() = List.size
}

class JobViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private var jobPerson: TextView = view.findViewById(R.id.jobPerson)
    private var jobPhone: TextView = view.findViewById(R.id.jobPhone)
    private var jobTitle: TextView = view.findViewById(R.id.jobTitle)
    private var jobAddress : TextView = view.findViewById(R.id.jobAddress)
    private var jobPrice : TextView = view.findViewById(R.id.jobPrice)
    private var jobDescription : TextView = view.findViewById(R.id.jobDescription)
    private var jobImage: ImageView = view.findViewById(R.id.jobPfp)

    fun bindItems (item : Job) {
        jobPerson.text = item.userName
        jobPhone.text = item.userNumber
        jobTitle.text = item.title
        jobAddress.text = item.jobAddress
        jobPrice.text = "$" + item.price
        jobDescription.text = item.description
        Picasso.get()
                .load(item.imageUrl) // load the image
                .into(jobImage)
    }
}