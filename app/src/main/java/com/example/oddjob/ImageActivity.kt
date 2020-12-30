package com.example.oddjob

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.io.ByteArrayOutputStream


class ImageActivity : AppCompatActivity() {
    private val CAMERA = 0;
    private val GALLERY = 1;
    private lateinit var yourImage : ImageView
    private lateinit var newUserPhotoUrl : String
    var b = ByteArray(0)
    private var imageLoaded : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
        val cameraUpload : TextView = findViewById(R.id.camera_upload)
        val galleryUpload : TextView = findViewById(R.id.gallery_upload)
        yourImage = findViewById(R.id.iv_profileImage)
        val imageUpload : TextView = findViewById(R.id.image_upload)

        cameraUpload.setOnClickListener {
            takePhotoFromCamera()
        }
        galleryUpload.setOnClickListener {
            choosePhotoFromGallery()
        }

        imageUpload.setOnClickListener{
            if (imageLoaded == 0){
                Toast.makeText(this,  "Please provide an image.", Toast.LENGTH_LONG).show()
            } else {
                imageLoaded = 0
                savePhotoToFirebase()
            }
        }

    }
    private fun savePhotoToFirebase(){
        // Step 1: Save image to cloud storage
        var getUser = FirebaseAuth.getInstance().currentUser
        val storageRef = FirebaseStorage.getInstance().reference.child("users")
        val fileRef = storageRef!!.child(getUser?.uid.toString() + ".jpg")
        val uploadTask = fileRef.putBytes(b)

        // Step 2: Get URL of file and save profile to database
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