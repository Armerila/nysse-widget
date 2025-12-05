package com.example.nyssewidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class WidgetConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var stopIdInput: EditText
    private lateinit var stopNameInput: EditText
    private lateinit var confirmButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.widget_config_layout)

        // Set result as canceled initially
        setResult(RESULT_CANCELED)

        // Get the widget ID from the intent
        appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // If invalid widget ID, finish
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Initialize views
        stopIdInput = findViewById(R.id.stopIdInput)
        stopNameInput = findViewById(R.id.stopNameInput)
        confirmButton = findViewById(R.id.confirmButton)

        // Set default value
        stopIdInput.setText("tampere:0805")
        stopNameInput.setText("Keskustori A")

        // Set up confirm button
        confirmButton.setOnClickListener {
            val stopId = stopIdInput.text.toString().trim()
            val stopName = stopNameInput.text.toString().trim()

            if (stopId.isEmpty()) {
                Toast.makeText(this, "Please enter a stop ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save the stop configuration
            saveStopConfig(this, appWidgetId, stopId, stopName)

            // Update the widget
            val appWidgetManager = AppWidgetManager.getInstance(this)
            NysseWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)

            // Return success result
            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }

    companion object {
        private const val PREFS_NAME = "com.example.nyssewidget.WidgetConfig"
        private const val PREF_STOP_ID_PREFIX = "stop_id_"
        private const val PREF_STOP_NAME_PREFIX = "stop_name_"

        fun saveStopConfig(context: Context, appWidgetId: Int, stopId: String, stopName: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString(PREF_STOP_ID_PREFIX + appWidgetId, stopId)
                putString(PREF_STOP_NAME_PREFIX + appWidgetId, stopName)
                apply()
            }
        }

        fun loadStopId(context: Context, appWidgetId: Int): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(PREF_STOP_ID_PREFIX + appWidgetId, "tampere:0805") ?: "tampere:0805"
        }

        fun loadStopName(context: Context, appWidgetId: Int): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(PREF_STOP_NAME_PREFIX + appWidgetId, null)
        }

        fun deleteStopConfig(context: Context, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                remove(PREF_STOP_ID_PREFIX + appWidgetId)
                remove(PREF_STOP_NAME_PREFIX + appWidgetId)
                apply()
            }
        }
    }
}
