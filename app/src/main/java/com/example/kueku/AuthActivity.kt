package com.example.kueku

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class AuthActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        // Inisialisasi Firebase Auth dan Firestore
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Inisialisasi View
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)

        // Periksa apakah pengguna sudah login saat aplikasi dibuka
        checkUserSession()

        // Listener untuk tombol Login
        btnLogin.setOnClickListener {
            loginUser()
        }

        // Listener untuk tombol Register
        btnRegister.setOnClickListener {
            registerUser()
        }
    }

    private fun checkUserSession() {
        val currentUser: FirebaseUser? = mAuth.currentUser
        if (currentUser != null) {
            // Pengguna sudah login, arahkan ke MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Tutup AuthActivity agar tidak bisa kembali ke sini
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (TextUtils.isEmpty(email)) {
            etEmail.error = "Email diperlukan!"
            return
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.error = "Password diperlukan!"
            return
        }

        progressBar.visibility = View.VISIBLE

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Login Gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun registerUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (TextUtils.isEmpty(email)) {
            etEmail.error = "Email diperlukan!"
            return
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.error = "Password diperlukan!"
            return
        }
        if (password.length < 6) {
            etPassword.error = "Password minimal 6 karakter!"
            return
        }

        progressBar.visibility = View.VISIBLE

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    val userId = mAuth.currentUser?.uid
                    val userEmail = mAuth.currentUser?.email

                    // Simpan data pengguna ke Firestore
                    if (userId != null && userEmail != null) {
                        val userMap = hashMapOf(
                            "email" to userEmail,
                            "name" to "User Baru" // Anda bisa menambahkan EditText untuk nama jika diperlukan
                        )
                        db.collection("users").document(userId)
                            .set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Registrasi Berhasil!", Toast.LENGTH_SHORT).show()
                                // Langsung arahkan ke MainActivity setelah register
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Gagal menyimpan data user: ${e.message}", Toast.LENGTH_LONG).show()
                                // Hapus user dari Authentication jika gagal menyimpan ke Firestore (opsional)
                                mAuth.currentUser?.delete()
                            }
                    }
                } else {
                    Toast.makeText(this, "Registrasi Gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}