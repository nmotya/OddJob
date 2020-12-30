package com.example.oddjob

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth


class  MainActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null && user?.isEmailVerified!! && user.photoUrl != null){
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        } else if (user != null && user?.isEmailVerified!! && user.photoUrl == null){
            val intent = Intent(this, ImageActivity::class.java)
            startActivity(intent)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val registerText : TextView = findViewById(R.id.registerText)
        val forgotPass : TextView = findViewById(R.id.forgotPassword)

        forgotPass.setOnClickListener{
            Toast.makeText(this,  user?.photoUrl.toString(), Toast.LENGTH_LONG).show()
        }


        registerText.setOnClickListener{
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        val login : TextView = findViewById(R.id.login)
        val email : EditText = findViewById(R.id.email)
        val password : EditText = findViewById(R.id.password)

        val mAuth = FirebaseAuth.getInstance()

        login.setOnClickListener{
            val emailString = email.text.toString().trim()
            val passwordString = password.text.toString().trim()

            if (emailString.isEmpty()){
                email.setError("Email is required!")
                email.requestFocus()
                return@setOnClickListener
            }

            if(!Patterns.EMAIL_ADDRESS.matcher(emailString).matches()){
                email.setError("Enter a valid email!")
                email.requestFocus()
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

            mAuth.signInWithEmailAndPassword(emailString, passwordString).addOnCompleteListener { task: Task<AuthResult> ->
                if (task.isSuccessful) {
                    val newUser = FirebaseAuth.getInstance().currentUser
                    if(newUser?.isEmailVerified!!){
                        Toast.makeText(this,  "Login Successful!", Toast.LENGTH_LONG).show()
                        if (newUser.photoUrl != null){
                            val intent = Intent(this, MapsActivity::class.java)
                            startActivity(intent)
                        } else {
                            val intent = Intent(this, ImageActivity::class.java)
                            startActivity(intent)
                        }

                    } else {
                        newUser.sendEmailVerification()
                        Toast.makeText(this,  "Please check email to verify account", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this,  "Login unsuccessful, please check your login info", Toast.LENGTH_LONG).show()
                }
            }

        }
    }
}