package com.example.nyssewidget

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class StopInfo(
    val name: String,
    val departures: List<Departure>
)

class DigitransitApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private val API_URL = "https://api.digitransit.fi/routing/v2/waltti/gtfs/v1"

    private val API_KEY = BuildConfig.DIGITRANSIT_API_KEY

    fun fetchDepartures(stopId: String, numberOfDepartures: Int = 10): StopInfo {
        try {
            val query = """
                {
                  stop(id: "$stopId") {
                    name
                    stoptimesWithoutPatterns(numberOfDepartures: $numberOfDepartures) {
                      scheduledDeparture
                      realtimeDeparture
                      departureDelay
                      realtime
                      realtimeState
                      serviceDay
                      headsign
                      trip {
                        route {
                          shortName
                          mode
                        }
                      }
                    }
                  }
                }
            """.trimIndent()

            val jsonBody = JsonObject().apply {
                addProperty("query", query)
            }

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Content-Type", "application/json")
                .addHeader("digitransit-subscription-key", API_KEY)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e("NysseWidget", "API request failed: ${response.code}")
                return StopInfo("Error", emptyList())
            }

            return parseDepartures(responseBody)

        } catch (e: Exception) {
            Log.e("NysseWidget", "Error fetching departures", e)
            return StopInfo("Error", emptyList())
        }
    }

    private fun parseDepartures(jsonResponse: String): StopInfo {

        try {
            val departures = mutableListOf<Departure>()

            val jsonObject = gson.fromJson(jsonResponse, JsonObject::class.java)
            val data = jsonObject.getAsJsonObject("data")
            val stop = data?.getAsJsonObject("stop")
            val stopName = stop?.get("name")?.asString ?: "Unknown Stop"
            val stoptimes = stop?.getAsJsonArray("stoptimesWithoutPatterns")

            stoptimes?.forEach { element ->
                val stoptime = element.asJsonObject

                val serviceDay = stoptime.get("serviceDay").asLong
                val realtimeDeparture = stoptime.get("realtimeDeparture").asInt
                val departureDelay = stoptime.get("departureDelay").asInt
                val realtime = stoptime.get("realtime").asBoolean
                val headsign = stoptime.get("headsign").asString

                val trip = stoptime.getAsJsonObject("trip")
                val route = trip.getAsJsonObject("route")
                val routeShortName = route.get("shortName").asString
                val mode = route.get("mode").asString

                if (mode == "TRAM") {
                    // serviceDay is Unix timestamp of the day (in seconds)
                    // realtimeDeparture is seconds since midnight
                    val departureTime = serviceDay + realtimeDeparture

                    val departure = Departure(
                        routeNumber = routeShortName,
                        destination = headsign,
                        departureTime = departureTime,
                        isRealtime = realtime,
                        delaySeconds = departureDelay
                    )

                    departures.add(departure)
                }
            }

            return StopInfo(
                name = stopName,
                departures = departures.sortedBy { it.departureTime }
            )

        } catch (e: Exception) {
            Log.e("NysseWidget", "Error parsing departures", e)
            return StopInfo("Error", emptyList())
        }
    }
}