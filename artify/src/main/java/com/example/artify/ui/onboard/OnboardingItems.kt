package com.example.artify.ui.onboard

import android.content.Context
import android.content.res.Configuration
import com.example.artify.R
import com.example.artify.model.Onboarding


fun getOnboardingItems(context: Context): List<Onboarding> {
    val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    return listOf(
        Onboarding("Easy one-click photo editing!", "Lorem ipsum dolor sit amet consectetur \n" +
                "odigisang elit, sed do eiusmod tempor Lorem ipsum dolor sit amet consectetur ", if (isLandscape) R.drawable.step1_land else R.drawable.step1),
        Onboarding("\"Unleash creativity with AI!\"", "Lorem ipsum dolor sit amet consectetur \n" +
                "odigisang elit, sed do eiusmod tempor Lorem ipsum dolor sit amet consectetur ", if (isLandscape) R.drawable.step2 else R.drawable.step2),
        Onboarding("\"Access all benefits with pro!\"", "Lorem ipsum dolor sit amet consectetur \n" +
                "odigisang elit, sed do eiusmod tempor Lorem ipsum dolor sit amet consectetur ", if (isLandscape) R.drawable.step3 else R.drawable.step3),
    )
}