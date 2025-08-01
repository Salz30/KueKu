package com.example.kueku

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar // Import Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var toolbar: Toolbar // Deklarasi Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        toolbar = findViewById(R.id.toolbar) // Inisialisasi Toolbar
        setSupportActionBar(toolbar) // SET TOOLBAR SEBAGAI ACTION BAR

        // ... (Kode lainnya seperti setup fragment default dan setOnItemSelectedListener) ...

        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_order -> {
                    loadFragment(OrderFragment())
                    true
                }
                R.id.nav_finance -> {
                    loadFragment(FinanceFragment())
                    true
                }
                else -> false
            }
        }
    }

    // ... (Fungsi loadFragment dan selectBottomNavItem tidak berubah) ...

    public fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun selectBottomNavItem(itemId: Int) {
        if (::bottomNavigationView.isInitialized) {
            bottomNavigationView.selectedItemId = itemId
        }
    }

    // Fungsi ini yang meng-inflate menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                mAuth.signOut()
                Toast.makeText(this, "Anda telah Logout", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, AuthActivity::class.java)
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}