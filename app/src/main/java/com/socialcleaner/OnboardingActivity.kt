package com.socialcleaner

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.google.android.material.color.DynamicColors
import java.util.Locale

class OnboardingActivity : AppCompatActivity() {

    private var currentPage = 0

    private lateinit var ivImage: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var btnNext: Button
    private lateinit var btnSkip: TextView
    private lateinit var pageIndicator: TextView
    private lateinit var tvSteps: TextView

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
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        if (prefs.getBoolean("onboarding_done", false)) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_onboarding)

        ivImage = findViewById(R.id.ivOnboardingImage)
        tvTitle = findViewById(R.id.tvOnboardingTitle)
        tvDescription = findViewById(R.id.tvOnboardingDescription)
        btnNext = findViewById(R.id.btnNext)
        btnSkip = findViewById(R.id.btnSkip)
        pageIndicator = findViewById(R.id.tvPageIndicator)
        tvSteps = findViewById(R.id.tvOnboardingSteps)

        showPage(0)

        btnNext.setOnClickListener {
            if (currentPage < 1) {
                currentPage++
                showPage(currentPage)
            } else {
                prefs.edit { putBoolean("onboarding_done", true) }
                goToMain()
            }
        }

        btnSkip.setOnClickListener {
            prefs.edit { putBoolean("onboarding_done", true) }
            goToMain()
        }
    }

    private fun showPage(index: Int) {
        val fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
        fadeOut.duration = 150
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 200

        tvTitle.startAnimation(fadeOut)
        tvDescription.startAnimation(fadeOut)
        ivImage.startAnimation(fadeOut)

        if (index == 0) {
            tvTitle.text = getString(R.string.onboarding_analyze_title)
            tvDescription.text = getString(R.string.onboarding_analyze_desc)
            tvSteps.text = getString(R.string.onboarding_analyze_step1) + "\n" +
                    getString(R.string.onboarding_analyze_step2) + "\n" +
                    getString(R.string.onboarding_analyze_step3)
            ivImage.setImageResource(R.drawable.onboarding_scan)
            btnNext.text = getString(R.string.next)
        } else {
            tvTitle.text = getString(R.string.onboarding_delete_title)
            tvDescription.text = getString(R.string.onboarding_delete_desc)
            tvSteps.text = getString(R.string.onboarding_delete_step1) + "\n" +
                    getString(R.string.onboarding_delete_step2) + "\n" +
                    getString(R.string.onboarding_delete_step3)
            ivImage.setImageResource(R.drawable.onboarding_delete)
            btnNext.text = getString(R.string.start)
        }

        tvTitle.startAnimation(fadeIn)
        tvDescription.startAnimation(fadeIn)
        ivImage.startAnimation(fadeIn)

        pageIndicator.text = getString(R.string.page_indicator, index + 1, 2)
        btnSkip.visibility = if (index == 1) View.GONE else View.VISIBLE
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
