package com.example.camera.ui.camera.adapter

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.camera.R
import com.example.camera.domain.model.FilterType
import timber.log.Timber

class FilterAdapter(
    private val onFilterSelected: (FilterType) -> Unit
) : ListAdapter<FilterType, FilterAdapter.FilterViewHolder>(FilterDiffCallback()) {

    private var selectedFilter: FilterType = FilterType.NONE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val view = try {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_filter, parent, false)
        } catch (e: Exception) {
            Timber.e(e, "Failed to inflate item_filter layout, creating programmatically")
            createFilterItemView(parent.context)
        }
        return FilterViewHolder(view)
    }

    private fun createFilterItemView(context: Context): View {
        val container = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                (60 * context.resources.displayMetrics.density).toInt(),
                (60 * context.resources.displayMetrics.density).toInt()
            )
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setBackgroundColor(Color.parseColor("#40FFFFFF"))
            setPadding(8, 8, 8, 8)
        }

        val imageView = ImageView(context).apply {
            id = R.id.ivFilterIcon
            layoutParams = LinearLayout.LayoutParams(
                (28 * context.resources.displayMetrics.density).toInt(),
                (28 * context.resources.displayMetrics.density).toInt()
            ).apply {
                bottomMargin = (2 * context.resources.displayMetrics.density).toInt()
            }
            setColorFilter(Color.WHITE)
            setImageResource(R.drawable.ic_filter_none)
        }

        val textView = TextView(context).apply {
            id = R.id.tvFilterName
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = "None"
            setTextColor(Color.WHITE)
            textSize = 9f
        }

        container.addView(imageView)
        container.addView(textView)
        
        return container
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val filter = getItem(position)
        holder.bind(filter, filter == selectedFilter) { selectedFilter ->
            this.selectedFilter = selectedFilter
            onFilterSelected(selectedFilter)
            notifyDataSetChanged() // Update selection state
        }
    }

    class FilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        private val ivFilterIcon: ImageView? by lazy { 
            try {
                itemView.findViewById<ImageView>(R.id.ivFilterIcon)
            } catch (e: Exception) {
                Timber.e(e, "Failed to find ivFilterIcon")
                null
            }
        }
        
        private val tvFilterName: TextView? by lazy { 
            try {
                itemView.findViewById<TextView>(R.id.tvFilterName)
            } catch (e: Exception) {
                Timber.e(e, "Failed to find tvFilterName")
                null
            }
        }

        fun bind(
            filter: FilterType,
            isSelected: Boolean,
            onFilterClick: (FilterType) -> Unit
        ) {
            try {
                // Set filter name
                tvFilterName?.text = getFilterDisplayName(filter)
                
                // Set filter icon
                ivFilterIcon?.setImageResource(getFilterIcon(filter))
                
                // Set selection state
                itemView.isSelected = isSelected
                itemView.alpha = if (isSelected) 1.0f else 0.7f
                
                // Handle click
                itemView.setOnClickListener {
                    onFilterClick(filter)
                }
                
                // Log if views are null for debugging
                if (ivFilterIcon == null) {
                    Timber.w("ivFilterIcon is null for filter: $filter")
                }
                if (tvFilterName == null) {
                    Timber.w("tvFilterName is null for filter: $filter")
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Error binding filter: $filter")
            }
        }

        private fun getFilterDisplayName(filter: FilterType): String {
            return when (filter) {
                FilterType.NONE -> "None"
                FilterType.SEPIA -> "Sepia"
                FilterType.BLACK_WHITE -> "B&W"
                FilterType.CINEMATIC -> "Cinema"
                FilterType.VINTAGE -> "Vintage"
                FilterType.COLD -> "Cold"
                FilterType.WARM -> "Warm"
            }
        }

        private fun getFilterIcon(filter: FilterType): Int {
            return when (filter) {
                FilterType.NONE -> R.drawable.ic_filter_none
                FilterType.SEPIA -> R.drawable.ic_filter_sepia
                FilterType.BLACK_WHITE -> R.drawable.ic_filter_bw
                FilterType.CINEMATIC -> R.drawable.ic_filter_cinematic
                FilterType.VINTAGE -> R.drawable.ic_filter_vintage
                FilterType.COLD -> R.drawable.ic_filter_cold
                FilterType.WARM -> R.drawable.ic_filter_warm
            }
        }
    }
}

class FilterDiffCallback : DiffUtil.ItemCallback<FilterType>() {
    override fun areItemsTheSame(oldItem: FilterType, newItem: FilterType): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: FilterType, newItem: FilterType): Boolean {
        return oldItem == newItem
    }
} 