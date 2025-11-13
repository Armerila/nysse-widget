package com.example.nyssewidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class NysseWidgetProvider : AppWidgetProvider() {

    // Pyhällönpuisto B
    private val stopId = "tampere:0873"

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update each widget instance
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = DigitransitApiService()
                val departures = apiService.fetchDepartures(stopId, numberOfDepartures = 5)

                withContext(Dispatchers.Main) {
                    updateWidgetUI(context, appWidgetManager, appWidgetId, departures)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError(context, appWidgetManager, appWidgetId)
                }
            }
        }
    }

    private fun updateWidgetUI(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        stopInfo: StopInfo
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val updateTime = timeFormat.format(Date())
        views.setTextViewText(R.id.updateTimeText, "Updated: $updateTime")

        views.setTextViewText(R.id.stopNameText, "${stopInfo.name} - Next Trams")

        val departuresText = if (stopInfo.departures.isEmpty()) {
            "No tram departures found.\n\nCheck:\n• API key is set\n• Stop ID is correct\n• Internet connection"
        } else {
            stopInfo.departures.take(5).joinToString("\n") { departure ->
                String.format("%-3s → %-15s %s",
                    departure.routeNumber,
                    departure.destination.take(15),
                    departure.getDisplayTime())
            }
        }

        views.setTextViewText(R.id.departuresText, departuresText)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun showError(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        views.setTextViewText(R.id.stopNameText, "Error loading data")
        views.setTextViewText(R.id.updateTimeText, "Check internet connection")
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}