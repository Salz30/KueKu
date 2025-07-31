package com.example.kueku

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FinanceFragment : Fragment() {

    private lateinit var tvTotalPenjualanHariIni: TextView
    private lateinit var spinnerKategoriPengeluaran: Spinner
    private lateinit var etJumlahPengeluaran: EditText
    private lateinit var btnTambahPengeluaran: Button
    private lateinit var llDaftarPengeluaran: LinearLayout
    private lateinit var tvRingkasanTotalPenjualan: TextView
    private lateinit var tvRingkasanTotalPengeluaran: TextView

    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_finance, container, false)

        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvTotalPenjualanHariIni = view.findViewById(R.id.tvTotalPenjualanHariIni)
        spinnerKategoriPengeluaran = view.findViewById(R.id.spinnerKategoriPengeluaran)
        etJumlahPengeluaran = view.findViewById(R.id.etJumlahPengeluaran)
        btnTambahPengeluaran = view.findViewById(R.id.btnTambahPengeluaran)
        llDaftarPengeluaran = view.findViewById(R.id.llDaftarPengeluaran)
        tvRingkasanTotalPenjualan = view.findViewById(R.id.tvRingkasanTotalPenjualan)
        tvRingkasanTotalPengeluaran = view.findViewById(R.id.tvRingkasanTotalPengeluaran)

        loadFinanceData()
        setupTambahPengeluaranButton()

        return view
    }

    private fun loadFinanceData() {
        val userId = mAuth.currentUser?.uid ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        var totalPenjualanHariIni = 0
        var totalPenjualanKeseluruhan = 0
        var totalPengeluaranKeseluruhan = 0

        // Hapus semua view pengeluaran sebelumnya
        llDaftarPengeluaran.removeAllViews()
        val noExpenseTextView = TextView(context)
        noExpenseTextView.text = "Belum ada pengeluaran."
        noExpenseTextView.textSize = 16f
        llDaftarPengeluaran.addView(noExpenseTextView)


        db.collection("transactions")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                llDaftarPengeluaran.removeAllViews() // Hapus placeholder "Belum ada pengeluaran"

                if (documents.isEmpty) {
                    val noDataTextView = TextView(context)
                    noDataTextView.text = "Belum ada transaksi."
                    noDataTextView.textSize = 16f
                    llDaftarPengeluaran.addView(noDataTextView)
                }

                for (document in documents) {
                    val tipe = document.getString("tipe")
                    val tanggal = document.getDate("tanggal")
                    val amount = document.getLong("total_harga")?.toDouble() ?: document.getLong("jumlah_pengeluaran")?.toDouble() ?: 0.0

                    if (tipe == "pemasukan") {
                        totalPenjualanKeseluruhan += amount.toInt()
                        if (tanggal != null && SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tanggal) == today) {
                            totalPenjualanHariIni += amount.toInt()
                        }
                    } else if (tipe == "pengeluaran") {
                        totalPengeluaranKeseluruhan += amount.toInt()

                        val kategori = document.getString("kategori")
                        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tanggal ?: Date())
                        val expenseText = "${kategori}: ${formatRupiah(amount)} (${formattedDate})"
                        val textView = TextView(context)
                        textView.text = expenseText
                        textView.textSize = 16f
                        llDaftarPengeluaran.addView(textView)
                    }
                }

                tvTotalPenjualanHariIni.text = formatRupiah(totalPenjualanHariIni.toDouble())
                tvRingkasanTotalPenjualan.text = "Total Penjualan: ${formatRupiah(totalPenjualanKeseluruhan.toDouble())}"
                tvRingkasanTotalPengeluaran.text = "Total Pengeluaran: ${formatRupiah(totalPengeluaranKeseluruhan.toDouble())}"

                if (llDaftarPengeluaran.childCount == 0) {
                    val noDataTextView = TextView(context)
                    noDataTextView.text = "Belum ada pengeluaran."
                    noDataTextView.textSize = 16f
                    llDaftarPengeluaran.addView(noDataTextView)
                }

            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Gagal memuat laporan keuangan: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupTambahPengeluaranButton() {
        btnTambahPengeluaran.setOnClickListener {
            val kategori = spinnerKategoriPengeluaran.selectedItem.toString()
            val jumlah = etJumlahPengeluaran.text.toString().toDoubleOrNull()

            if (jumlah == null || jumlah <= 0) {
                Toast.makeText(context, "Jumlah pengeluaran tidak valid!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = mAuth.currentUser?.uid
            if (userId != null) {
                val expense = hashMapOf(
                    "kategori" to kategori,
                    "jumlah_pengeluaran" to jumlah,
                    "tanggal" to Date(),
                    "tipe" to "pengeluaran", // Untuk laporan keuangan
                    "userId" to userId
                )

                db.collection("transactions")
                    .add(expense)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Pengeluaran berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                        etJumlahPengeluaran.setText("0")
                        loadFinanceData() // Perbarui laporan
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Gagal menambahkan pengeluaran: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(context, "Pengguna tidak terautentikasi.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatRupiah(number: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return format.format(number).replace("Rp", "Rp ").replace(",00", "")
    }
}