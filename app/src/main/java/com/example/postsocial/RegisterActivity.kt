package com.example.postsocial

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.postsocial.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val btn_register = findViewById<Button>(R.id.btn_register)
        val loading = findViewById<ProgressBar>(R.id.loading_progress_bar)
        val tvLogin = findViewById<TextView>(R.id.tv_login)
        val email_register = findViewById<EditText>(R.id.ed_email_register)
        val password_register = findViewById<EditText>(R.id.ed_password_register)
        val name_register = findViewById<EditText>(R.id.ed_name_register)

        btn_register.setOnClickListener {
            val email = email_register.text.toString()
            val password = password_register.text.toString()
            val name = name_register.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty()) {
                registerUser(email, password, name)
                loading.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                loading.visibility = View.INVISIBLE
            }
        }
        tvLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

    }
    private fun registerUser(email: String, password: String, name: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // register berhasil
                    val user = auth.currentUser
                    val userId = user?.uid
                    // Simpan data pengguna ke dalam database
                    userId?.let {
                        val database = Firebase.database
                        val myRef = database.getReference("users").child(it)
                        val newUser = User(userId, email, name)
                        myRef.setValue(newUser)
                    }

                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish() // Tutup halaman registrasi
                } else {
                    // Login gagal
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

}