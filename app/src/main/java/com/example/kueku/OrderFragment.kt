package com.example.kueku

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderFragment : Fragment() {

    private lateinit var spinnerKue: Spinner
    private lateinit var tvHargaSatuan: TextView
    private lateinit var etJumlahPesanan: EditText
    private lateinit var tvTotalHarga: TextView
    private lateinit var btnTambahPesanan: Button
    private lateinit var tvRingkasanPesananHariIni: TextView

    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val hargaKue = mapOf(
        "Opaa Coin Original" to 15000,
        "Opaa Coin Coklat" to 18000,
        "Opaa Coin Keju" to 18000
    )

    private var selectedKueHarga: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_order, container, false)

        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        spinnerKue = view.findViewById(R.id.spinnerKue)
        tvHargaSatuan = view.findViewById(R.id.tvHargaSatuan)
        etJumlahPesanan = view.findViewById(R.id.etJumlahPesanan)
        tvTotalHarga = view.findViewById(R.id.tvTotalHarga)
        btnTambahPesanan = view.findViewById(R.id.btnTambahPesanan)
        tvRingkasanPesananHariIni = view.findViewById(R.id.tvRingkasanPesananHariIni)

        setupSpinner()
        setupJumlahPesananChangeListener()
        setupTambahPesananButton()
        loadRingkasanPesananHariIni()

        return view
    }

    private fun setupSpinner() {
        spinnerKue.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedKue = parent?.getItemAtPosition(position).toString()
                selectedKueHarga = hargaKue[selectedKue] ?: 0
                tvHargaSatuan.text = formatRupiah(selectedKueHarga.toDouble())
                calculateTotalPrice()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
        // Set default selection to trigger initial price update
        spinnerKue.setSelection(0)
    }

    private fun setupJumlahPesananChangeListener() {
        etJumlahPesanan.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateTotalPrice()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun calculateTotalPrice() {
        val jumlah = etJumlahPesanan.text.toString().toIntOrNull() ?: 0
        val total = selectedKueHarga * jumlah
        tvTotalHarga.text = formatRupiah(total.toDouble())
    }

    private fun setupTambahPesananButton() {
        btnTambahPesanan.setOnClickListener {
            val selectedKue = spinnerKue.selectedItem.toString()
            val jumlah = etJumlahPesanan.text.toString().toIntOrNull()
            val totalHarga = selectedKueHarga * (jumlah ?: 0)

            if (jumlah == null || jumlah <= 0) {
                Toast.makeText(context, "Jumlah pesanan tidak valid!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = mAuth.currentUser?.uid
            if (userId != null) {
                val order = hashMapOf(
                    "kue" to selectedKue,
                    "jumlah" to jumlah,
                    "harga_satuan" to selectedKueHarga,
                    "total_harga" to totalHarga,
                    "tanggal" to Date(), // Timestamp
                    "tipe" to "pemasukan", // Untuk laporan keuangan
                    "userId" to userId
                )

                db.collection("transactions")
                    .add(order)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Pemesanan sudah dibuat!", Toast.LENGTH_SHORT).show()
                        // Reset form
                        etJumlahPesanan.setText("1")
                        spinnerKue.setSelection(0)
                        loadRingkasanPesananHariIni() // Perbarui ringkasan
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Gagal menambahkan pesanan: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(context, "Pengguna tidak terautentikasi.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadRingkasanPesananHariIni() {
        val userId = mAuth.currentUser?.uid ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("transactions")
            .whereEqualTo("userId", userId)
            .whereEqualTo("tipe", "pemasukan")
            .get()
            .addOnSuccessListener { documents ->
                var totalPesananHariIni = 0
                var jumlahKueTerjual = 0

                for (document in documents) {
                    val date = document.getDate("tanggal")
                    if (date != null && SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date) == today) {
                        val totalHarga = document.getLong("total_harga")?.toInt() ?: 0
                        val jumlah = document.getLong("jumlah")?.toInt() ?: 0
                        totalPesananHariIni += totalHarga
                        jumlahKueTerjual += jumlah
                    }
                }

                if (jumlahKueTerjual > 0) {
                    tvRingkasanPesananHariIni.text = "Hari ini: ${jumlahKueTerjual} kue terjual dengan total ${formatRupiah(totalPesananHariIni.toDouble())}"
                } else {
                    tvRingkasanPesananHariIni.text = "Belum ada pesanan hari ini."
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Gagal memuat ringkasan pesanan: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun formatRupiah(number: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return format.format(number).replace("Rp", "Rp ").replace(",00", "")
    }
}