package com.koshpal_android.koshpalapp.ui.custom

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.koshpal_android.koshpalapp.R

class NotchedBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr) {
    
    init {
        // Set the custom notched background
        background = NotchedBottomNavBackground()
    }
}
