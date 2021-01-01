package com.example.oddjob

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.telephony.PhoneNumberUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.io.ByteArrayOutputStream


class EditActivity : AppCompatActivity() {
    private val CAMERA = 0;
    private val GALLERY = 1;
    private lateinit var yourImage : ImageView
    private lateinit var newUserPhotoUrl : String
    var b = ByteArray(0)
    private var imageLoaded : Int = 0
    private var isProblem : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        yourImage = findViewById(R.id.editImage)
        val cameraUpload : TextView = findViewById(R.id.editCamera)
        val galleryUpload : TextView = findViewById(R.id.editGallery)
        val editSubmit : TextView = findViewById(R.id.editSubmit)
        val cancelEdit : TextView = findViewById(R.id.editCancel)
        val editName : EditText = findViewById(R.id.editName)
        val editPhone : EditText = findViewById(R.id.editPhone)
        val dataReference = FirebaseDatabase.getInstance().getReference("users")

        cancelEdit.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }

        cameraUpload.setOnClickListener {
            takePhotoFromCamera()
        }

        galleryUpload.setOnClickListener {
            choosePhotoFromGallery()
        }

        editSubmit.setOnClickListener{

            val dataJobReference = FirebaseDatabase.getInstance().getReference("jobs")
            val queryJob = dataJobReference!!.orderByValue()

            //checks if a photo was taken or chosen from gallery
            if (imageLoaded == 1){
                imageLoaded = 0
                savePhotoToFirebase()
            }

            if (!editName.text.isEmpty()){
                //update user's name
                dataReference.child(FirebaseAuth.getInstance().currentUser?.uid.toString()).child("firstName")
                        .setValue(editName.text.trim().toString())

                //update the name listed on all of the user's jobs
                queryJob.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (placeIdSnapshot in dataSnapshot.children) {
                            for (jobSnapshot in placeIdSnapshot.children) {
                                val map : Map<String, String> = jobSnapshot.value as Map<String, String>
                                if (map.get("userId") == FirebaseAuth.getInstance().currentUser?.uid.toString()){
                                    dataJobReference.child(map.get("placeId")!!).child(map.get("jobId")!!).child("userName").setValue(editName.text.trim().toString())
                                }
                            }
                        }
                    }
                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(this@EditActivity,"An error occurred. Try again.", Toast.LENGTH_LONG).show()
                    }
                })
            }

            if (editPhone.text.matches("^(1-)?[0-9]{3}-?[0-9]{3}-?[0-9]{4}$".toRegex())){
                //update user's phone number
                dataReference.child(FirebaseAuth.getInstance().currentUser?.uid.toString()).child("phoneNumber")
                        .setValue(PhoneNumberUtils.formatNumber(editPhone.text.trim().toString()).toString())

                //update the phone number listed on all of the user's jobs
                queryJob.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (placeIdSnapshot in dataSnapshot.children) {
                            for (jobSnapshot in placeIdSnapshot.children) {
                                val map : Map<String, String> = jobSnapshot.value as Map<String, String>
                                if (map.get("userId") == FirebaseAuth.getInstance().currentUser?.uid.toString()){
                                    dataJobReference.child(map.get("placeId")!!).child(map.get("jobId")!!).child("userNumber").setValue(editPhone.text.trim().toString())
                                }
                            }
                        }
                    }
                    override fun onCancelled(databaseError: DatabaseError) {
                    }
                })
            }

            //if a user's phone number is not a proper number then notify the user
            if (!isProblem){
                Toast.makeText(this,"Profile edited successfully",Toast.LENGTH_LONG).show()
                val intent = Intent(this, MapsActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this,"Please enter a valid phone number",Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun savePhotoToFirebase(){
        // Save image to cloud storage
        var getUser = FirebaseAuth.getInstance().currentUser
        val storageRef = FirebaseStorage.getInstance().reference.child("users")
        val fileRef = storageRef!!.child(getUser?.uid.toString() + ".jpg")
        val uploadTask = fileRef.putBytes(b)

        // Get URL of file and save profile to database
        uploadTask.continueWithTask(Continuation < UploadTask.TaskSnapshot, Task<Uri>>{
                task ->
            if (!task.isSuccessful) {
                task.exception?.let{
                    throw it
                }
            }
            return@Continuation fileRef.downloadUrl
        }).addOnCompleteListener {
                task ->
            if (task.isSuccessful) {

                //set current user's image URI to the URI of the photo just taken
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setPhotoUri(Uri.parse(task.result.toString()))
                    .build()
                getUser!!.updateProfile(profileUpdates)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val intent = Intent(this, MapsActivity::class.java)
                            startActivity(intent)
                        }
                    }
            }
        }
    }

    private fun takePhotoFromCamera() {
        if (!checkPermission()) {
            requestPermission(CAMERA)
        } else {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            // CAMERA is the result code
            startActivityForResult(cameraIntent, CAMERA)
        }
    }

    private fun choosePhotoFromGallery() {
        if (!checkPermission()) {
            requestPermission(GALLERY)
        } else {
            val galleryIntent = Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            // GALLERY is the result code
            startActivityForResult(galleryIntent, GALLERY)
        }
    }

    private fun checkPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return true
    }

    private fun requestPermission(code: Int) {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            code)
        //request code will let us know what was requested; will be useful in onRequestPermissionsResult
    }

    // if we receive permission to use camera, then run takePhotoFromCamera again
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA) {
            takePhotoFromCamera()
        } else if (requestCode == GALLERY) {
            choosePhotoFromGallery()
        }
    }

    // this gets run after you return from the gallery or camera
    public override fun onActivityResult(requestCode:Int, resultCode:Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA || requestCode == GALLERY) {
            val imageURI = data?.data
            val thumbnail = data!!.extras!!.get("data") as Bitmap
            yourImage!!.setImageBitmap(thumbnail)
            val baos = ByteArrayOutputStream()
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            b = baos.toByteArray()
            imageLoaded = 1
        }
    }
}