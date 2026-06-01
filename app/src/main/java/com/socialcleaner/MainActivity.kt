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
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
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
    private lateinit var progressBar: View
    private lateinit var lottieScan: LottieAnimationView
    private lateinit var tvStatus: TextView
    private lateinit var tvStatFiles: TextView
    private lateinit var tvStatSize: TextView
    private lateinit var tvStatApps: TextView
    private lateinit var btnScan: MaterialButton
    private lateinit var btnDelete: MaterialButton
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
        // Apply Material You dynamic colors
        DynamicColors.applyToActivityIfAvailable(this)

        // Follow system dark/light theme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Activity transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        initViews()
        setupYearSpinner()
        checkPermissions()
    }

    private fun initViews() {
        rvYears = findViewById(R.id.rvYears)
        progressBar = findViewById(R.id.progressBar)
        lottieScan = findViewById(R.id.lottieScan)
        tvStatus = findViewById(R.id.tvStatus)
        tvStatFiles = findViewById(R.id.tvStatFiles)
        tvStatSize = findViewById(R.id.tvStatSize)
        tvStatApps = findViewById(R.id.tvStatApps)
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

        val adapter = ArrayAdapter(this, R.layout.spinner_item, years)
        adapter.setDropDownViewResource(R.layout.spinner_item)
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
        // Show Lottie scan animation
        lottieScan.visibility = View.VISIBLE
        lottieScan.playAnimation()
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
        // Hide Lottie animation with fade out
        lottieScan.cancelAnimation()
        lottieScan.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                lottieScan.visibility = View.GONE
                lottieScan.alpha = 1f
            }
            .start()

        progressBar.visibility = View.GONE
        btnScan.isEnabled = true

        if (results.isEmpty()) {
            tvStatus.text = "Aucune donnée trouvée"
            tvStatFiles.text = "0"
            tvStatSize.text = "0"
            tvStatApps.text = "0"
            yearAdapter.setData(emptyList())
            btnDelete.visibility = View.GONE
            selectionSummary.visibility = View.GONE
            return
        }

        val totalFiles = results.sumOf { it.totalFiles }
        val totalSize = results.sumOf { it.totalSize }
        val appCount = results.map { it.appName }.distinct().size

        tvStatus.text = "Scan terminé"
        tvStatus.setTextColor(resources.getColor(R.color.success, theme))
        tvStatFiles.text = totalFiles.toString()
        tvStatSize.text = formatSize(totalSize)
        tvStatApps.text = appCount.toString()

        val yearGroups = results.groupBy { it.year }
            .map { (year, apps) -> YearGroup(year, apps) }
            .sortedByDescending { it.year }

        yearAdapter.setData(yearGroups)

        // Animate results appearance
        btnDelete.alpha = 0f
        btnDelete.visibility = View.VISIBLE
        btnDelete.animate().alpha(1f).setDuration(400).start()
    }

    private fun updateSelectionSummary() {
        val selectedFiles = yearAdapter.getAllSelectedFiles()
        val count = selectedFiles.size
        var totalSize = 0L

        for (result in allResults) {
            for (category in result.categories) {
                for (file in category.files) {
                    if (selectedFiles.contains(file.path)) {
                        totalSize += file.size
                    }
                }
            }
        }

        if (count > 0) {
            selectionSummary.visibility = View.VISIBLE
            tvSelection.text = "$count fichiers • ${formatSize(totalSize)}"
        } else {
            selectionSummary.visibility = View.GONE
        }
    }

    private fun confirmDelete() {
        val selectedFiles = yearAdapter.getAllSelectedFiles()

        if (selectedFiles.isEmpty()) {
            Toast.makeText(this, "Sélectionnez d'abord des fichiers", Toast.LENGTH_SHORT).show()
            return
        }

        var deleteSize = 0L
        val appNames = mutableSetOf<String>()
        val years = mutableSetOf<Int>()

        for (result in allResults) {
            for (category in result.categories) {
                for (file in category.files) {
                    if (selectedFiles.contains(file.path)) {
                        deleteSize += file.size
                        appNames.add(result.appName)
                        years.add(result.year)
                    }
                }
            }
        }

        val yearList = years.sorted().joinToString(", ")
        val appList = appNames.joinToString(", ")

        AlertDialog.Builder(this)
            .setTitle("Confirmation")
            .setMessage(
                "Apps: $appList\n" +
                "Année(s): $yearList\n" +
                "Fichiers: ${selectedFiles.size}\n" +
                "Taille: ${formatSize(deleteSize)}\n\n" +
                "Cette action est irréversible !"
            )
            .setPositiveButton("Supprimer") { _, _ -> deleteSelected(selectedFiles) }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun deleteSelected(filesToDelete: Set<String>) {
        lottieScan.visibility = View.VISIBLE
        lottieScan.playAnimation()
        progressBar.visibility = View.VISIBLE
        tvStatus.text = "Suppression en cours..."

        lifecycleScope.launch {
            var deletedCount = 0
            var deletedSize = 0L

            withContext(Dispatchers.IO) {
                for (filePath in filesToDelete) {
                    try {
                        val f = File(filePath)
                        if (f.exists() && f.delete()) {
                            deletedCount++
                            deletedSize += f.length()
                        }
                    } catch (e: Exception) {
                        // Handle permission errors
                    }
                }
            }

            lottieScan.cancelAnimation()
            lottieScan.visibility = View.GONE
            progressBar.visibility = View.GONE
            tvStatus.text = "$deletedCount supprimés • ${formatSize(deletedSize)} libérés"
            tvStatus.setTextColor(resources.getColor(R.color.success, theme))
            btnDelete.visibility = View.GONE
            selectionSummary.visibility = View.GONE

            Toast.makeText(this@MainActivity,
                "$deletedCount fichiers supprimés", Toast.LENGTH_LONG).show()

            startScan()
        }
    }
}
