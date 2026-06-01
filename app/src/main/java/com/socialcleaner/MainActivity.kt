package com.socialcleaner

import android.Manifest
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.google.android.material.switchmaterial.SwitchMaterial
import com.socialcleaner.model.*
import com.socialcleaner.scanner.AppRegistry
import com.socialcleaner.scanner.MediaScanner
import com.socialcleaner.ui.YearAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnMenu: ImageView
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

    // Drawer menu
    private lateinit var menuLanguage: LinearLayout
    private lateinit var menuTheme: LinearLayout
    private lateinit var menuNotifications: LinearLayout
    private lateinit var switchDarkMode: SwitchMaterial
    private lateinit var switchNotifications: SwitchMaterial
    private lateinit var tvCurrentLang: TextView

    private val scanner = MediaScanner()
    private lateinit var yearAdapter: YearAdapter
    private lateinit var notifHelper: NotificationHelper
    private var allResults = listOf<AppScanResult>()
    private var selectedYear: Int? = null
    private var isDarkMode = false

    // Scan service
    private var scanService: ScanService? = null
    private var serviceBound = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ScanService.ScanBinder
            scanService = binder.getService()
            serviceBound = true

            scanService?.onProgress = { appName, current, total ->
                tvStatus.text = getString(R.string.scan_app, appName, current, total)
                progressBar.visibility = View.VISIBLE
            }

            scanService?.onResult = { results ->
                allResults = results
                displayResults(results)
            }

            scanService?.onCancelled = {
                resetScanUI()
                tvStatus.text = getString(R.string.cancel)
            }

            // Start scan after binding
            scanService?.startScan(selectedYear)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            scanService = null
            serviceBound = false
        }
    }

    companion object {
        private const val STORAGE_PERMISSION_CODE = 100
        private const val MANAGE_STORAGE_CODE = 101
        private const val NOTIF_PERMISSION_CODE = 102
        private const val STATE_RESULTS = "scan_results"
        private const val STATE_YEAR = "selected_year"
    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("social_cleaner", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "fr") ?: "fr"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)

        val prefs = getSharedPreferences("social_cleaner", Context.MODE_PRIVATE)
        isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        notifHelper = NotificationHelper(this)

        initViews()
        setupDrawer()
        setupYearSpinner()
        checkPermissions()

        // Restore saved state
        if (savedInstanceState != null) {
            selectedYear = savedInstanceState.getInt(STATE_YEAR, -1).let {
                if (it == -1) null else it
            }
        }

        // Handle notification cleanup action
        if (intent.getBooleanExtra("auto_cleanup", false)) {
            startScan()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_YEAR, selectedYear ?: -1)
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        btnMenu = findViewById(R.id.btnMenu)
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

        // Drawer menu views
        menuLanguage = findViewById(R.id.menuLanguage)
        menuTheme = findViewById(R.id.menuTheme)
        menuNotifications = findViewById(R.id.menuNotifications)
        switchDarkMode = findViewById(R.id.switchDarkMode)
        switchNotifications = findViewById(R.id.switchNotifications)
        tvCurrentLang = findViewById(R.id.tvCurrentLang)

        yearAdapter = YearAdapter { updateSelectionSummary() }
        rvYears.layoutManager = LinearLayoutManager(this)
        rvYears.adapter = yearAdapter

        btnScan.setOnClickListener { startScan() }
        btnDelete.setOnClickListener { confirmDelete() }

        btnDelete.visibility = View.GONE
        selectionSummary.visibility = View.GONE
    }

    private fun setupDrawer() {
        // Set drawer width to 70% of screen
        val params = findViewById<View>(R.id.drawerMenu).layoutParams as DrawerLayout.LayoutParams
        params.width = (resources.displayMetrics.widthPixels * 0.70).toInt()
        findViewById<View>(R.id.drawerMenu).layoutParams = params

        // Hamburger menu click
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Update current language display
        updateLanguageDisplay()

        // Language switcher
        menuLanguage.setOnClickListener {
            showLanguageDialog()
        }

        // Dark mode toggle
        switchDarkMode.isChecked = isDarkMode

        menuTheme.setOnClickListener {
            switchDarkMode.toggle()
        }
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != isDarkMode) {
                isDarkMode = isChecked
                val prefs = getSharedPreferences("social_cleaner", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("dark_mode", isChecked).apply()
                AppCompatDelegate.setDefaultNightMode(
                    if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
                // recreate() is called automatically by AppCompatDelegate
            }
        }

        // Notifications toggle
        val prefs = getSharedPreferences("social_cleaner", Context.MODE_PRIVATE)
        val notifEnabled = prefs.getBoolean("notifications_enabled", true)
        switchNotifications.isChecked = notifEnabled

        menuNotifications.setOnClickListener {
            switchNotifications.toggle()
        }
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            toggleNotifications(isChecked)
        }
    }

    private fun updateLanguageDisplay() {
        val locale = resources.configuration.locales[0]
        val langName = when (locale.language) {
            "en" -> "English"
            "es" -> "Español"
            "ar" -> "العربية"
            else -> "Français"
        }
        tvCurrentLang.text = langName
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("Français", "English", "Español", "العربية")
        val codes = arrayOf("fr", "en", "es", "ar")

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_language))
            .setItems(languages) { _, which ->
                setLocale(codes[which])
            }
            .show()
    }

    private fun setLocale(languageCode: String) {
        val prefs = getSharedPreferences("social_cleaner", Context.MODE_PRIVATE)
        val currentLang = prefs.getString("language", "fr") ?: "fr"

        if (currentLang == languageCode) return // Already set

        prefs.edit().putString("language", languageCode).apply()

        // Apply locale
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        // Recreate with new locale
        recreate()
    }

    private fun toggleNotifications(enabled: Boolean) {
        val prefs = getSharedPreferences("social_cleaner", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()

        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS")
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf("android.permission.POST_NOTIFICATIONS"),
                    NOTIF_PERMISSION_CODE
                )
            }
        }

        Toast.makeText(this,
            if (enabled) getString(R.string.notif_enabled) else getString(R.string.notif_disabled),
            Toast.LENGTH_SHORT).show()
    }

    private fun setupYearSpinner() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = mutableListOf(getString(R.string.all_years))
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
        lottieScan.visibility = View.VISIBLE
        lottieScan.playAnimation()
        progressBar.visibility = View.VISIBLE
        tvStatus.text = getString(R.string.scanning)
        tvStatus.visibility = View.VISIBLE
        btnScan.isEnabled = false
        btnDelete.visibility = View.GONE
        selectionSummary.visibility = View.GONE

        // Start and bind to foreground service
        val intent = Intent(this, ScanService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun resetScanUI() {
        lottieScan.cancelAnimation()
        lottieScan.visibility = View.GONE
        progressBar.visibility = View.GONE
        btnScan.isEnabled = true
    }

    private fun displayResults(results: List<AppScanResult>) {
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
            tvStatus.text = getString(R.string.no_data)
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

        tvStatus.text = getString(R.string.scan_complete)
        tvStatus.setTextColor(resources.getColor(R.color.success, theme))
        tvStatFiles.text = totalFiles.toString()
        tvStatSize.text = formatSize(this, totalSize)
        tvStatApps.text = appCount.toString()

        val yearGroups = results.groupBy { it.year }
            .map { (year, apps) -> YearGroup(year, apps) }
            .sortedByDescending { it.year }

        yearAdapter.setData(yearGroups)

        btnDelete.alpha = 0f
        btnDelete.visibility = View.VISIBLE
        btnDelete.animate().alpha(1f).setDuration(400).start()

        // Show notification for scan results
        if (totalSize > 0) {
            notifHelper.showCleanupNotification(totalSize)
        }
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
            tvSelection.text = getString(R.string.selection_summary, count, formatSize(this, totalSize))
        } else {
            selectionSummary.visibility = View.GONE
        }
    }

    private fun confirmDelete() {
        val selectedFiles = yearAdapter.getAllSelectedFiles()

        if (selectedFiles.isEmpty()) {
            Toast.makeText(this, getString(R.string.select_files_first), Toast.LENGTH_SHORT).show()
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
            .setTitle(getString(R.string.confirm_title))
            .setMessage(getString(R.string.confirm_message, appList, yearList, selectedFiles.size, formatSize(this, deleteSize)))
            .setPositiveButton(getString(R.string.confirm)) { _, _ -> deleteSelected(selectedFiles) }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteSelected(filesToDelete: Set<String>) {
        lottieScan.visibility = View.VISIBLE
        lottieScan.playAnimation()
        progressBar.visibility = View.VISIBLE
        tvStatus.text = getString(R.string.delete_progress)

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
            tvStatus.text = getString(R.string.delete_complete, deletedCount, formatSize(this@MainActivity, deletedSize))
            tvStatus.setTextColor(resources.getColor(R.color.success, theme))
            btnDelete.visibility = View.GONE
            selectionSummary.visibility = View.GONE

            Toast.makeText(this@MainActivity,
                getString(R.string.delete_toast, deletedCount), Toast.LENGTH_LONG).show()

            startScan()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
