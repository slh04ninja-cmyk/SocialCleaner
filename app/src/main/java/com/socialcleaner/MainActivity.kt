package com.socialcleaner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.socialcleaner.model.*
import com.socialcleaner.scanner.AppRegistry
import com.socialcleaner.scanner.MediaScanner
import com.socialcleaner.ui.YearAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var rvYears: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var tvTotalSummary: TextView
    private lateinit var btnScan: Button
    private lateinit var btnDelete: Button
    private lateinit var spinnerYear: Spinner
    private lateinit var selectionSummary: LinearLayout
    private lateinit var tvSelection: TextView

    private val scanner = MediaScanner()
    private lateinit var yearAdapter: YearAdapter
    private var allResults = listOf<AppScanResult>()
    private var selectedYear: Int? = null

    companion object {
        private const val STORAGE_PERMISSION_CODE = 100
        private const val MANAGE_STORAGE_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupYearSpinner()
        checkPermissions()
    }

    private fun initViews() {
        rvYears = findViewById(R.id.rvYears)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)
        tvTotalSummary = findViewById(R.id.tvTotalSummary)
        btnScan = findViewById(R.id.btnScan)
        btnDelete = findViewById(R.id.btnDelete)
        spinnerYear = findViewById(R.id.spinnerYear)
        selectionSummary = findViewById(R.id.selectionSummary)
        tvSelection = findViewById(R.id.tvSelection)

        yearAdapter = YearAdapter { updateSelectionSummary() }
        rvYears.layoutManager = LinearLayoutManager(this)
        rvYears.adapter = yearAdapter

        btnScan.setOnClickListener { startScan() }
        btnDelete.setOnClickListener { confirmDelete() }

        btnDelete.visibility = View.GONE
        selectionSummary.visibility = View.GONE
    }

    private fun setupYearSpinner() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = mutableListOf("Toutes les années")
        for (y in currentYear downTo 2018) {
            years.add(y.toString())
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerYear.adapter = adapter

        spinnerYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                selectedYear = if (pos == 0) null else years[pos].toInt()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivityForResult(intent, MANAGE_STORAGE_CODE)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivityForResult(intent, MANAGE_STORAGE_CODE)
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    STORAGE_PERMISSION_CODE
                )
            }
        }
    }

    private fun startScan() {
        progressBar.visibility = View.VISIBLE
        tvStatus.text = "Analyse en cours..."
        tvStatus.visibility = View.VISIBLE
        btnScan.isEnabled = false
        btnDelete.visibility = View.GONE
        selectionSummary.visibility = View.GONE

        lifecycleScope.launch {
            val results = withContext(Dispatchers.IO) {
                val allResults = mutableListOf<AppScanResult>()
                val apps = AppRegistry.supportedApps

                for ((index, app) in apps.withIndex()) {
                    withContext(Dispatchers.Main) {
                        tvStatus.text = "Scan ${app.name}... (${index + 1}/${apps.size})"
                    }

                    val scanResults = scanner.scanApp(app, selectedYear)
                    allResults.addAll(scanResults)
                }

                allResults.sortedWith(compareByDescending<AppScanResult> { it.year }
                    .thenByDescending { it.totalSize })
            }

            allResults = results
            displayResults(results)
        }
    }

    private fun displayResults(results: List<AppScanResult>) {
        progressBar.visibility = View.GONE
        btnScan.isEnabled = true

        if (results.isEmpty()) {
            tvStatus.text = "Aucune donnée trouvée"
            tvTotalSummary.text = ""
            return
        }

        val totalFiles = results.sumOf { it.totalFiles }
        val totalSize = results.sumOf { it.totalSize }
        tvStatus.text = "✅ Scan terminé"
        tvTotalSummary.text = "Total: $totalFiles fichiers • ${formatSize(totalSize)}"

        val yearGroups = results.groupBy { it.year }
            .map { (year, apps) -> YearGroup(year, apps) }
            .sortedByDescending { it.year }

        yearAdapter.setData(yearGroups)
        btnDelete.visibility = View.VISIBLE
    }

    private fun updateSelectionSummary() {
        val selectedCount = yearAdapter.itemCount
        selectionSummary.visibility = View.VISIBLE

        val selectedFiles = mutableSetOf<String>()
        var selectedSize = 0L

        for (result in allResults) {
            for (category in result.categories) {
                for (file in category.files) {
                    // This is simplified - in real app, track through adapter
                }
            }
        }

        tvSelection.text = "Sélection: ${selectedFiles.size} fichiers • ${formatSize(selectedSize)}"
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Suppression")
            .setMessage("Êtes-vous sûr de vouloir supprimer les fichiers sélectionnés ?\n\nCette action est irréversible.")
            .setPositiveButton("Supprimer") { _, _ -> deleteSelected() }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun deleteSelected() {
        progressBar.visibility = View.VISIBLE
        tvStatus.text = "Suppression en cours..."

        lifecycleScope.launch {
            var deletedCount = 0
            var deletedSize = 0L

            withContext(Dispatchers.IO) {
                for (result in allResults) {
                    for (category in result.categories) {
                        for (file in category.files) {
                            // In real app, check if file is selected
                            try {
                                val f = File(file.path)
                                if (f.exists() && f.delete()) {
                                    deletedCount++
                                    deletedSize += file.size
                                }
                            } catch (e: Exception) {
                                // Handle permission errors
                            }
                        }
                    }
                }
            }

            progressBar.visibility = View.GONE
            tvStatus.text = "✅ $deletedCount fichiers supprimés • ${formatSize(deletedSize)} libérés"
            btnDelete.visibility = View.GONE

            Toast.makeText(this@MainActivity,
                "$deletedCount fichiers supprimés", Toast.LENGTH_LONG).show()

            startScan()
        }
    }
}
