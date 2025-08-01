package com.example.kueku // Sesuaikan dengan nama package Anda

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        // Penting: Pastikan tema splash diterapkan sebelum super.onCreate
        setTheme(R.style.Theme_App_Starting) // Terapkan tema splash screen

        super.onCreate(savedInstanceState)
        // Tidak perlu setContentView(R.layout.activity_splash), karena tema sudah mengatur backgroundnya

        mAuth = FirebaseAuth.getInstance()

        // Delay beberapa detik sebelum pindah ke Activity berikutnya
        Handler(Looper.getMainLooper()).postDelayed({
            // Periksa apakah pengguna sudah login
            val currentUser = mAuth.currentUser
            if (currentUser != null) {
                // Jika sudah login, langsung ke MainActivity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else {
                // Jika belum login, pergi ke AuthActivity
                val intent = Intent(this, AuthActivity::class.java)
                startActivity(intent)
            }
            finish() // Tutup SplashActivity agar tidak bisa kembali dengan tombol back
        }, 3000) // Delay 3 detik (3000 milidetik)
    }
}