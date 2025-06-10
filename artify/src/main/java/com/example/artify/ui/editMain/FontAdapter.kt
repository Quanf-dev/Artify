package com.example.artify.ui.editMain

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.artify.R

class FontAdapter(
    private val fonts: List<String>,
    var selectedIndex: Int,
    private val fontMap: Map<String, Int>,
    val onFontSelected: (Int) -> Unit
) : RecyclerView.Adapter<FontAdapter.FontViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_font_picker, parent, false)
        return FontViewHolder(view as FrameLayout)
    }
    override fun getItemCount() = fonts.size
    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        val fontName = fonts[position]
        val tv = holder.fontName
        tv.text = fontName
        val fontResId = fontMap[fontName] ?: 0
        if (fontResId != 0) {
            try {
                tv.typeface = ResourcesCompat.getFont(tv.context, fontResId)
            } catch (e: Exception) {
                tv.typeface = Typeface.DEFAULT
            }
        } else {
            tv.typeface = Typeface.DEFAULT
        }
        // Set background
        tv.setBackgroundResource(if (position == selectedIndex) R.drawable.bg_font_picker_item_selected else R.drawable.bg_font_picker_item_default)
        tv.setTextColor(if (position == selectedIndex) Color.BLACK else Color.DKGRAY)
        tv.setOnClickListener {
            if (position != selectedIndex) {
                onFontSelected(position)
            }
        }
    }
    class FontViewHolder(view: FrameLayout) : RecyclerView.ViewHolder(view) {
        val fontName: TextView = view.findViewById(R.id.tvFontName)
    }
}
