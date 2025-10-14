package com.koshpal_android.koshpalapp.ui.insights

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.koshpal_android.koshpalapp.R

class InsightsMarkerView(context: Context) : MarkerView(context, R.layout.marker_insight) {
    private val tvTitle: TextView = findViewById(R.id.tvTitle)
    private val tvValue: TextView = findViewById(R.id.tvValue)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e != null) {
            tvTitle.text = "Value"
            tvValue.text = "₹" + String.format("%.2f", e.y)
        }
        super.refreshContent(e, highlight)
    }

    override fun getX(): Float = -(width / 2f)
    override fun getY(): Float = -height.toFloat()
}


