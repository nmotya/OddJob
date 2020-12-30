 package com.example.oddjob

import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.util.Patterns
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

 class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
        val register : TextView = findViewById(R.id.register)
        val email : EditText = findViewById(R.id.email)
        val firstName : EditText = findViewById(R.id.first_name)
        val password : EditText = findViewById(R.id.password)
        val confirmPassword : EditText = findViewById(R.id.confirm_password)
        val cancel : TextView = findViewById(R.id.cancel)
        val phoneNumber : EditText = findViewById(R.id.phoneNumber)

        cancel.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


        register.setOnClickListener{
            val emailString = email.text.toString().trim()
            val firstNameString = firstName.text.toString().trim()
            val passwordString = password.text.toString().trim()
            val confirmPasswordString = confirmPassword.text.toString().trim()
            val userNumber = phoneNumber.text.toString().trim()

            if (emailString.isEmpty()){
                email.setError("Email is required!")
                email.requestFocus()
                return@setOnClickListener
            }

            if (firstNameString.isEmpty()){
                firstName.setError("First Name is required!")
                firstName.requestFocus()
                return@setOnClickListener
            }
            if (passwordString.isEmpty()){
                password.setError("Password is required!")
                password.requestFocus()
                return@setOnClickListener
            }

            if (passwordString.length < 6){
                password.setError("Password must be at least 6 characters!")
                password.requestFocus()
                return@setOnClickListener
            }

            if (confirmPasswordString.isEmpty()){
                confirmPassword.setError("Confirm your password!")
                confirmPassword.requestFocus()
                return@setOnClickListener
            }
            if (confirmPasswordString != passwordString){
                confirmPassword.setError("Passwords don't match!")
                confirmPassword.requestFocus()
                return@setOnClickListener
            }

            if(!Patterns.EMAIL_ADDRESS.matcher(emailString).matches()){
                email.setError("Enter a valid email!")
                email.requestFocus()
                return@setOnClickListener
            }

            mAuth.createUserWithEmailAndPassword(emailString, passwordString).addOnCompleteListener { task: Task<AuthResult> ->
                if (task.isSuccessful) {
                    val userId : String? = FirebaseAuth.getInstance().currentUser?.uid
                    val newUser = userId?.let { it1 ->
                        User(
                                emailString,
                                firstNameString,
                                it1,
                                PhoneNumberUtils.formatNumber(userNumber).toString()
                        )

                    }
                    userId?.let { it1 ->
                        FirebaseDatabase.getInstance()
                                .getReference("users").child(it1).setValue(newUser).addOnCompleteListener { task: Task<Void> ->
                                    if (task.isSuccessful) {
                                        val user = FirebaseAuth.getInstance().currentUser
                                        user!!.sendEmailVerification()
                                        Toast.makeText(this,  "Please check your email to verify account.", Toast.LENGTH_LONG).show()
                                        val intent = Intent(this, MainActivity::class.java)
                                        startActivity(intent)
                                    } else {
                                        Toast.makeText(this,  "User not created, try again.", Toast.LENGTH_LONG).show()
                                    }
                                }
                    }
                } else {
                    Toast.makeText(this,  "User not created, try again.", Toast.LENGTH_LONG).show()
                }
            }
        }

    }

 }



