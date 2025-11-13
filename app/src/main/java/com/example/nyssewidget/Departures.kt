package com.example.nyssewidget

data class Departure(
    val routeNumber: String,
    val destination: String,
    val departureTime: Long,
    val isRealtime: Boolean,
    val delaySeconds: Int
) {

    fun getMinutesUntil(): Int {
        val nowSeconds = System.currentTimeMillis() / 1000
        val diffSeconds = departureTime - nowSeconds
        return (diffSeconds / 60).toInt()
    }

    fun getDisplayTime(): String {
        val minutes = getMinutesUntil()
        return when {
            minutes <= 0 -> "Now"
            minutes == 1 -> "1 min"
            minutes < 60 -> "$minutes min"
            else -> {
                val hours = minutes / 60
                val mins = minutes % 60
                "${hours}h ${mins}m"
            }
        }
    }
}