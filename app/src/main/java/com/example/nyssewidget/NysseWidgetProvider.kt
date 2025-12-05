package com.example.nyssewidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.app.PendingIntent
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NysseWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val WORK_NAME = "widget_update_work"
        private const val ACTION_REFRESH = "com.example.nyssewidget.REFRESH_WIDGET"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val stopId = WidgetConfigActivity.loadStopId(context, appWidgetId)
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

            // Set up refresh button click
            val refreshIntent = Intent(context, NysseWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.refreshButton, refreshPendingIntent)

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

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update each widget instance
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        schedulePeriodicUpdates(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )

            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // Clean up stored preferences when widget is deleted
        for (appWidgetId in appWidgetIds) {
            WidgetConfigActivity.deleteStopConfig(context, appWidgetId)
        }
    }

    // Minimum allowed update rate 15min
    private fun schedulePeriodicUpdates(context: Context) {
        val updateRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            updateRequest
        )
    }
}