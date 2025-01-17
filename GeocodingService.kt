package edu.vassar.cmpu203.myfirstapplication.Controller

import android.accounts.NetworkErrorException
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import edu.vassar.cmpu203.myfirstapplication.Model.Coordinates
import edu.vassar.cmpu203.myfirstapplication.Model.Destination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException
import java.net.URLEncoder

/**
 * A data class that contains useful fields from the response of the Nominatim
 * Geocoding API.
 */
private data class GeocodingAPIResponse(
    val lon: Double,
    val lat: Double,
    @SerializedName("display_name")
    val displayName: String
)

/**
 * A data class that contains useful fields from the response of the Nominatim
 * Reverse Geocoding API.
 */
private data class ReverseGeocodingAPIResponse(
    @SerializedName("display_name")
    val displayName: String
)

/**
 * A class that implements the business logic for geocoding (human-readable text -> coordinates)
 * and reverse geocoding (coordinates -> human-readable text). We do that by making
 * calls to the Nominatim API.
 */
class GeocodingService(private val scope: CoroutineScope) {
    // Gson shared object for decoding API responses
    private val gson = Gson()

    /**
     * Given a human-readble text describing a location, returns a list of `Destination`.
     * A `Destination` includes the coordinates of the location and the human-readable text.
     * This is otherwise known as (forward) geocoding.
     */
    // Java-friendly method that takes a callback
    @JvmOverloads
    fun lookupNameAsync(
        query: String,
        onSuccess: (List<Destination>) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        scope.launch {
            try {
                // Do the background work
                val result = withContext(Dispatchers.IO) {
                    lookupName(query)
                }
                // Deliver result on main thread
                onSuccess(result)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    /**
     * Given a set of coordinates, returns a `Destination`.
     * A `Destination` includes the coordinates of the location and the human-readable text.
     * This is otherwise known as reverse geocoding.
     */
    // Java-friendly method that takes a callback
    @JvmOverloads
    fun lookupCoordsAsync(
        coords: Coordinates,
        onSuccess: (Destination) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        scope.launch {
            try {
                // Do the background work
                val result = withContext(Dispatchers.IO) {
                    lookupCoords(coords)
                }
                // Deliver result on main thread
                onSuccess(result)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    /**
     * A blocking call to the Nominatim Geocoding API.
     */
    @Throws(NetworkErrorException::class)
    private fun lookupName(query: String): List<Destination> {
        // Check that the inputs are valid (the query is not too long).
        if (query.length > 300) {
            throw NetworkErrorException("Query is too long.");
        }

        // Construct a request
        val localizedQuery = "$query, New York City, NY";
        val encodedQuery = URLEncoder.encode(localizedQuery, "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=10"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Kotlin Nominatim Client")
            .get()
            .build()

        // Make the API call and throw in case of a failure
        val response: Response = try {
            client.newCall(request).execute()
        } catch (e: Exception) {
            throw NetworkErrorException("Network error: ${e.message}")
        }

        // Check the response is successful and valid
        if (!response.isSuccessful) {
            throw NetworkErrorException("GET request failed. Response Code: ${response.code}")
        }
        val responseBody = response.body?.string()
            ?: throw NetworkErrorException("Empty response body in call to geocoding service.")

        // Decode the response and construct a list of Destination using the response data.
        val listType = object : TypeToken<List<GeocodingAPIResponse>>() {}.type
        val apiResponse: List<GeocodingAPIResponse> = gson.fromJson(responseBody, listType)
        val foundDestinations = apiResponse.map { res -> Destination(Coordinates(res.lat, res.lon), res.displayName) }

        return foundDestinations
    }

    /**
     * A blocking call to the Nominatim Reverse Geocoding API.
     */
    @Throws(NetworkErrorException::class)
    private fun lookupCoords(coords: Coordinates): Destination {
        // Construct a request
        // We use a zoom level of 10 to search only within the city
        val url = "https://nominatim.openstreetmap.org/reverse?lat=${coords.latitude}&lon=${coords.longitude}&format=json&zoom=10"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Kotlin Nominatim Client")
            .get()
            .build()

        // Make the API call and throw in case of a failure
        val response: Response = try {
            client.newCall(request).execute()
        } catch (e: Exception) {
            throw NetworkErrorException("Network error: ${e.message}")
        }

        // Check the response is successful and valid
        if (!response.isSuccessful) {
            throw NetworkErrorException("GET request failed. Response Code: ${response.code}")
        }
        val responseBody = response.body?.string()
            ?: throw NetworkErrorException("Empty response body in call to geocoding service.")

        // Decode the response and construct a list of Destination using the response data.
        val listType = object : TypeToken<GeocodingAPIResponse>() {}.type
        val apiResponse: GeocodingAPIResponse = gson.fromJson(responseBody, listType)

        return Destination(coords, apiResponse.displayName)
    }

    /**
     * Cleanup resources acquired by the `GeocodingService`.
     */
    fun cleanup() {
        scope.cancel()
    }
}
