package com.example.artify.ui.language

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.artify.R
import com.example.artify.model.Language
import com.example.common.gradiant4.LinearGradientBorder

class LanguageAdapter(
    private var languages: List<Language>,
    private val onLanguageSelected: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_language, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(languages[position])
    }

    override fun getItemCount(): Int = languages.size


    inner class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val containerView: LinearGradientBorder =
            itemView.findViewById(R.id.languageItemContainer)
        private val flagImageView: ImageView = itemView.findViewById(R.id.ivLanguageFlag)
        private val languageNameTextView: TextView = itemView.findViewById(R.id.tvLanguageName)

        fun bind(language: Language) {
            // Set language name from resource
            languageNameTextView.setText(language.nameResId)

            // Set flag image
            flagImageView.setImageResource(language.flagResId)

            // Show/hide selected indicator
            if (language.isSelected) {
                containerView.isSelected = true
            }else{
                containerView.isSelected = false
            }

            // Set click listener
            containerView.setOnClickListener {
                onLanguageSelected(language)
            }
        }
    }
} 