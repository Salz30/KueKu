package com.example.kueku

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
// Pastikan ini diimpor jika belum
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private lateinit var tvKueTerjualHariIni: TextView
    private lateinit var tvTotalPendapatanHariIni: TextView
    private lateinit var btnLihatDetailPesanan: Button
    private lateinit var tvPendapatanHariIni: TextView
    private lateinit var tvEstimasiLabaKotor: TextView
    private lateinit var btnLihatLaporanKeuangan: Button

    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvKueTerjualHariIni = view.findViewById(R.id.tvKueTerjualHariIni)
        tvTotalPendapatanHariIni = view.findViewById(R.id.tvTotalPendapatanHariIni)
        btnLihatDetailPesanan = view.findViewById(R.id.btnLihatDetailPesanan)
        tvPendapatanHariIni = view.findViewById(R.id.tvPendapatanHariIni)
        tvEstimasiLabaKotor = view.findViewById(R.id.tvEstimasiLabaKotor)
        btnLihatLaporanKeuangan = view.findViewById(R.id.btnLihatLaporanKeuangan)

        loadDashboardData()
        setupButtons()

        return view
    }

    private fun loadDashboardData() {
        val userId = mAuth.currentUser?.uid ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        var totalKueTerjual = 0
        var totalPendapatan = 0
        var totalPengeluaran = 0

        db.collection("transactions")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val tipe = document.getString("tipe")
                    val tanggal = document.getDate("tanggal")

                    if (tanggal != null && SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tanggal) == today) {
                        if (tipe == "pemasukan") {
                            val jumlahKue = document.getLong("jumlah")?.toInt() ?: 0
                            val harga = document.getLong("total_harga")?.toInt() ?: 0
                            totalKueTerjual += jumlahKue
                            totalPendapatan += harga
                        } else if (tipe == "pengeluaran") {
                            val jumlahPengeluaran = document.getLong("jumlah_pengeluaran")?.toInt() ?: 0
                            totalPengeluaran += jumlahPengeluaran
                        }
                    }
                }

                tvKueTerjualHariIni.text = "${totalKueTerjual} Kue Terjual"
                tvTotalPendapatanHariIni.text = "Total pendapatan: ${formatRupiah(totalPendapatan.toDouble())}"
                tvPendapatanHariIni.text = "Pendapatan: ${formatRupiah(totalPendapatan.toDouble())}"

                val labaKotor = totalPendapatan - totalPengeluaran
                tvEstimasiLabaKotor.text = "Estimasi Laba Kotor: ${formatRupiah(labaKotor.toDouble())}"
                if (totalPengeluaran == 0) {
                    tvEstimasiLabaKotor.append(" (Perlu input pengeluaran)")
                }

            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Gagal memuat data dasbor: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupButtons() {
        btnLihatDetailPesanan.setOnClickListener {
            val mainActivity = activity as? MainActivity // Cast activity ke MainActivity
            mainActivity?.loadFragment(OrderFragment()) // Panggil loadFragment dari MainActivity
            mainActivity?.selectBottomNavItem(R.id.nav_order) // Panggil fungsi baru untuk pilih item navigasi
        }

        btnLihatLaporanKeuangan.setOnClickListener {
            val mainActivity = activity as? MainActivity // Cast activity ke MainActivity
            mainActivity?.loadFragment(FinanceFragment()) // Panggil loadFragment dari MainActivity
            mainActivity?.selectBottomNavItem(R.id.nav_finance) // Panggil fungsi baru untuk pilih item navigasi
        }
    }

    private fun formatRupiah(number: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return format.format(number).replace("Rp", "Rp ").replace(",00", "")
    }
}