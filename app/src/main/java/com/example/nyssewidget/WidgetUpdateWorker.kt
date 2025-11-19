package com.example.nyssewidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Background worker that updates the widget periodically
 */
class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)

            val widgetComponent = ComponentName(applicationContext, NysseWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)

            if (appWidgetIds.isNotEmpty()) {
                val intent = android.content.Intent(applicationContext, NysseWidgetProvider::class.java)
                intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                applicationContext.sendBroadcast(intent)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}