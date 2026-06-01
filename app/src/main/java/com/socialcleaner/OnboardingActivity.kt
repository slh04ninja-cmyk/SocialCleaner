package com.socialcleaner

import android.content.Context
import android.content.Intent
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

class OnboardingActivity : AppCompatActivity() {

    private var currentPage = 0

    private lateinit var ivImage: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var btnNext: Button
    private lateinit var btnSkip: TextView
    private lateinit var pageIndicator: TextView

    private val pages = listOf(
        OnboardingPage(
            title = "Analysez vos fichiers",
            description = "Appuyez sur Scanner pour analyser les fichiers multimédias de vos applications de réseaux sociaux. L'application détecte les images, vidéos, documents et audio.",
            steps = listOf(
                "1. Sélectionnez une année ou toutes",
                "2. Appuyez sur Scanner",
                "3. Consultez les résultats par catégorie"
            )
        ),
        OnboardingPage(
            title = "Supprimez en toute sécurité",
            description = "Sélectionnez les catégories que vous souhaitez supprimer, puis appuyez sur Supprimer. Vous gardez le contrôle total sur chaque fichier.",
            steps = listOf(
                "1. Cochez les catégories souhaitées",
                "2. Vérifiez la sélection en haut",
                "3. Appuyez sur Supprimer"
            )
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply Material You dynamic colors
        DynamicColors.applyToActivityIfAvailable(this)

        // Follow system dark/light theme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        super.onCreate(savedInstanceState)

        // Activity transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        // Check if already seen
        val prefs = getSharedPreferences("social_cleaner", Context.MODE_PRIVATE)
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

        showPage(0)

        btnNext.setOnClickListener {
            if (currentPage < pages.size - 1) {
                currentPage++
                showPage(currentPage)
            } else {
                // Done
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
        val page = pages[index]

        // Fade out current content
        val fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
        fadeOut.duration = 150

        // Fade in new content
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 200

        // Apply fade transition to content
        tvTitle.startAnimation(fadeOut)
        tvDescription.startAnimation(fadeOut)
        ivImage.startAnimation(fadeOut)

        tvTitle.text = page.title
        tvDescription.text = page.description

        // Build steps text
        val stepsText = page.steps.joinToString("\n")
        findViewById<TextView>(R.id.tvOnboardingSteps).text = stepsText

        // Set image
        if (index == 0) {
            ivImage.setImageResource(R.drawable.onboarding_scan)
        } else {
            ivImage.setImageResource(R.drawable.onboarding_delete)
        }

        // Fade in new content
        tvTitle.startAnimation(fadeIn)
        tvDescription.startAnimation(fadeIn)
        ivImage.startAnimation(fadeIn)

        // Update button text
        if (index == pages.size - 1) {
            btnNext.text = "Commencer"
        } else {
            btnNext.text = "Suivant"
        }

        // Page indicator
        pageIndicator.text = "${index + 1} / ${pages.size}"

        // Hide skip on last page
        btnSkip.visibility = if (index == pages.size - 1) View.GONE else View.VISIBLE
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val steps: List<String>
)
