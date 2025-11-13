package com.example.nyssewidget

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this).apply {
            text = """
                Nysse Widget
                
                To use this widget:
                
                1. Long-press on your home screen
                2. Select "Widgets"
                3. Find "Nysse Widget"
                4. Drag it to your home screen
                
            """.trimIndent()
            textSize = 16f
            setPadding(32, 32, 32, 32)
        }

        setContentView(textView)
    }
}