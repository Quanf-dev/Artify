package com.example.artify.model

/**
 * Model class representing a language option
 */
data class Language(
    val code: String,
    val nameResId: Int,
    val flagResId: Int,
    var isSelected: Boolean = false
) 