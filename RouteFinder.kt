package edu.vassar.cmpu203.myfirstapplication.Controller

import android.accounts.NetworkErrorException
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import edu.vassar.cmpu203.myfirstapplication.Model.BestRoute
import edu.vassar.cmpu203.myfirstapplication.Model.ClockTime
import edu.vassar.cmpu203.myfirstapplication.Model.Coordinates
import edu.vassar.cmpu203.myfirstapplication.Model.Destination
import edu.vassar.cmpu203.myfirstapplication.Model.GTFSData
import edu.vassar.cmpu203.myfirstapplication.Model.StationDetails
import edu.vassar.cmpu203.myfirstapplication.Model.TransitRoute
import edu.vassar.cmpu203.myfirstapplication.Model.TransitTrip
import edu.vassar.cmpu203.myfirstapplication.Model.TripCollection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.lang.reflect.Type
import java.net.URLEncoder
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.function.Consumer

/**
 * A data class that contains useful fields from the response of the API
 * From: https://github.com/graphhopper/graphhopper/blob/master/docs/web/api-doc.md?plain=1
 */
private class RouteAPIRequest(
    /**
     * The profile to be used for the route calculation.
     */
    val profile: String = "pt",
    /**
     * Specify multiple points for which the route should be calculated.
     * The order is important. Specify at least two points.
     */
    val points: List<Coordinates>,
    /**
     * The locale of the resulting turn instructions. E.g. `pt_PT` for Portuguese or `de` for
     * German.
     */
    val locale: String = "en",
    /**
     * Specify the earliest departure time of the itineraries. In ISO-8601 format
     * `yyyy-MM-ddTHH:mm:ssZ` e.g. `2020-12-30T12:56:00Z`.
     */
    val departureTime: ClockTime,
    /**
     * If true the `pt.earliest_departure_time` parameter is used to define the latest time of
     * arrival of the itineraries.
     */
    val arriveBy: Boolean = false,
    /**
     * If true you request a list of all itineraries where each one is the best way to get from A to
     * B, for some departure time within a specified time window. This profile query is also called
     * "range query". The time window is specified via `pt.profile_duration`. Limited to 50 by
     * default, change this via `pt.limit_solutions`.
     */
    val ptProfile: Boolean = false,
    /**
     * The time window for a profile query and so only applicable if `pt.profile` is `true`.
     * Duration string e.g. `PT200S`.
     */
    val profileDuration: String = "PT60M",
    /**
     * Maximum duration on street for access or egress of public transit i.e. time outside of public
     * transit. Duration string e.g. `PT30M`.
     */
    val limitStreetTime: String? = null,
    /**
     * Specifies if transfers as criterion should be ignored.
     */
    val ignoreTransfers: Boolean,
    /**
     * The number of maximum solutions that should be searched.
     */
    val limitSolutions: String? = null
) {
    constructor(pointA: Coordinates, pointB: Coordinates, time: ClockTime) : this(
        "pt",
        listOf(pointA, pointB),
        "en",
        time,
        false,
        false,
        "PT60M",
        null,
        false,
        "5"
    )

    fun buildQueryParams(): String {
        val profile = URLEncoder.encode(this.profile, "UTF-8")

        // Points are a list of `point` params which are floating-point coordinates separated by a
        // comma ('%2C')
        val pointsStr = this.points.joinToString("&") { point ->
            "point=${point.latitude}%2C${point.longitude}"
        }

        // Format ClockTime as local ISO date string.
        val deptTimeDate = LocalDateTime.now()
            .withHour(departureTime.hours)
            .withMinute(departureTime.minutes)
            .withSecond(departureTime.seconds)
        val deptTimeISO8601 = deptTimeDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
        val deptTime = URLEncoder.encode(deptTimeISO8601, "UTF-8")

        val profileDuration = URLEncoder.encode(this.profileDuration, "UTF-8")

        val paramLimitStreetTime = this.limitStreetTime?.let {
            "pt.limit_street_time=" + URLEncoder.encode(it, "UTF-8")
        } ?: ""
        val paramLimitSolutions = this.limitSolutions?.let {
            "pt.limit_solutions=" + URLEncoder.encode(it, "UTF-8")
        } ?: ""

        return "profile=$profile&$pointsStr&locale=$locale&pt.earliest_departure_time=$deptTime&" +
                "pt.arrive_by=$arriveBy&pt.profile=$ptProfile&pt.profile_duration=$profileDuration&" +
                "$paramLimitStreetTime&pt.ignore_transfers=$ignoreTransfers&" +
                paramLimitSolutions
    }
}


/**
 * A data class that contains useful fields from the response of the API
 */
private data class RouteAPIResponse(
    val paths: List<Path>
) {
    /**
     * Converts the response to a list of `BestRoute`s which is what we use in the app.
     */
    fun formBestRoutes(
        gtfsData: GTFSData
    ): List<BestRoute> {
        return paths.map { path -> path.formBestRoute(gtfsData) }
    }

    companion object {
        /**
         * A path represents one route, maps to one `BestRoute`.
         */
        data class Path(
            /**
             * Duration of the path in seconds.
             */
            val time: Int,
            /**
             * Set of coordinates. Each coordinate is represented
             * as a list of doubles that should contain just two elements:
             * latitude and longitude.
             */
            val points: Geometry,
            /**
             * A list of instructions (walk or pt).
             */
            val instructions: List<Instruction>,
            /**
             * A list of legs.
             */
            val legs: List<Leg>
        ) {
            /**
             * Converts the path to a `BestRoute`.
             */
            fun formBestRoute(
                gtfsData: GTFSData
            ): BestRoute {
                // Convert each `leg` to a `BestRoute.Step`
                val legs = legs.map { leg -> leg.formStep(gtfsData) }

                // Build the `BestRoute` object
                return BestRoute(
                    legs.first().departureTime,
                    legs.last().arrivalTime,
                    legs
                )
            }
        }

        /**
         * A leg. Maps to one `BestRoute.Step`.
         */
        sealed class Leg {
            /**
             * Either `pt` or `foot`.
             */
            abstract val type: String
            /**
             * A string like 2024-11-27T23:40:30.000+00:00
             */
            abstract val departureTime: String
            /**
             * A string like 2024-11-27T23:40:30.000+00:00
             */
            abstract val arrivalTime: String
            /**
             * The geometry of the leg, i.e. the set of coordinates.
             */
            abstract val geometry: Geometry

            /**
             * Converts the leg to a `BestRoute.Step`.
             */
            fun formStep(gtfsData: GTFSData): BestRoute.Step {
                // Get common information
                val geometry = geometry.coordinates.map { coords -> Coordinates(coords[0], coords[1]) }
                val departureTime = ClockTime(departureTime)
                val arrivalTime = ClockTime(arrivalTime)

                // Switch based on type of leg
                when (this) {
                    is WalkingLeg -> {
                        // If the type of leg is `walk`, convert to `BestRoute.WalkStep`
                        return BestRoute.WalkStep(
                            geometry,
                            departureTime,
                            arrivalTime,
                            instructions.map(Instruction::formInstruction))
                    }
                    is TransitLeg -> {
                        // For the transit leg, we need to map several GTFS IDs to our GTFS data.
                        // Link each routes id to `TransitRoute` object.
                        val route = gtfsData.routesByID.get(routeID) ?:
                            TransitRoute(routeID, "", "", "", TripCollection())
                        // Link each trip id to `TransitTrip` object. If we can't find the trip,
                        // we make up a trip with the route (so that our app doesn't crash).
                        val trip = gtfsData.tripsByID[tripID] ?:
                            TransitTrip(
                                // Use the right trip id and link to the right route.
                                tripID, route, null,
                                // Use a fake trip service for all days.
                                TransitTrip.TripService(
                                    "Not Found", "Start Date", "End Date",
                                    true, true, true, true, true, true, true),
                                // Use the given trip headsign
                                tripHeadsign,
                                // Use an arbitrary direction (one) and empty departures.
                                TransitTrip.TripDirection.ONE, HashMap())

                        return BestRoute.TransitStep(
                            geometry,
                            departureTime,
                            arrivalTime,
                            stops.map { stop ->
                                // Map each stop id to a station.
                                // If we can't find a station, make up a station so we don't crash.
                                gtfsData.stationsByID.get(stop.id) ?:
                                    StationDetails(
                                        // Use the given id, name, coordinates/
                                        stop.id, stop.name,
                                        Coordinates(stop.geometry.coordinates[0], stop.geometry.coordinates[1]),
                                        // Use the route we constructed and an arbitrary
                                        // accessibility (not accessible).
                                        setOf(route), "Not Accessible")
                            },
                            trip,
                            route)
                    }
                }
            }
        }

        /**
         * A walking leg. Maps to a `BestRoute.WalkStep`.
         */
        data class WalkingLeg(
            // Inherited fields:
            override val type: String,
            @SerializedName("departure_time")
            override val departureTime: String,
            @SerializedName("arrival_time")
            override val arrivalTime: String,
            override val geometry: Geometry,

            /**
             * A list of instructions.
             */
            val instructions: List<Instruction>
        ) : Leg()

        /**
         * An instruction. Maps to a `BestRoute.WalkInstruction`.
         */
        data class Instruction(
            /**
             * Description of the instruction
             */
            val text: String,
            /**
             * Time for the instruction
             */
            val time: Int,
            @SerializedName("street_name")
            val streetName: String,
            /**
             * The sign of the instruction.
             * See this for <a href="https://github.com/graphhopper/graphhopper/blob/master/docs/web/api-doc.md">reference</a>
             */
            val sign: Int
        ) {
            /**
             * Converts the instruction to a `BestRoute.WalkInstruction`.
             */
            fun formInstruction(): BestRoute.WalkInstruction {
                return BestRoute.WalkInstruction(
                    text,
                    streetName,
                    BestRoute.InstructionSign.fromSign(sign))
            }
        }

        /**
         * A transit leg. Maps to a `BestRoute.TransitStep`.
         */
        data class TransitLeg(
            // Inherited fields:
            override val type: String,
            @SerializedName("departure_time")
            override val departureTime: String,
            @SerializedName("arrival_time")
            override val arrivalTime: String,
            override val geometry: Geometry,

            /**
             * A list of stops with pt.
             */
            val stops: List<PtStop>,

            /**
             * The GTFS trip id.
             */
            @SerializedName("trip_id")
            val tripID: String,
            /**
             * The GTFS route id
             */
            @SerializedName("route_id")
            val routeID: String,
            /**
             * The headsign of the trip.
             */
            @SerializedName("trip_headsign")
            val tripHeadsign: String
        ) : Leg()

        /**
         * The Geometry class represents a set of coordinates.
         */
        data class Geometry(
            /**
             * Set of coordinates. Each coordinate is represented
             * as a list of doubles that should contain just two elements:
             * latitude and longitude.
             */
            @SerializedName("coordinates")
            val coordinates: List<List<Double>>
        )

        /**
         * One public-transit stop. Maps to a `StationDetails`.
         */
        data class PtStop(
            /**
             * The GTFS stop id
             */
            @SerializedName("stop_id")
            val id: String,
            /**
             * The name of the stop
             */
            @SerializedName("stop_name")
            val name: String,
            /**
             * The geometry of the stop.
             */
            val geometry: PtStopGeometry
        )

        /**
         * The geometry of a public-transit stop.
         */
        data class PtStopGeometry(
            /**
             * One set of coordinates. The coordinates are represented
             * as a latitude and longitude pair.
             */
            val coordinates: List<Double>
        )
    }
}

/**
 * A class for deserializing legs based on the type.
 */
private class LegDeserializer : JsonDeserializer<RouteAPIResponse.Companion.Leg> {
    /**
     * Deserialize a json element into a leg.
     * If the type is `pt`, deserialize into a `PtLeg`.
     * Else if the type is `walk`, deserialize into a `WalkingLeg`.
     */
     override fun deserialize(json: JsonElement, typeOfT: Type,
                             context: JsonDeserializationContext): RouteAPIResponse.Companion.Leg {
        val jsonObject = json.asJsonObject
        return when (val type = jsonObject.get("type").asString) {
            "pt" -> context.deserialize<RouteAPIResponse.Companion.TransitLeg>(
                json, RouteAPIResponse.Companion.TransitLeg::class.java)
            "walk" -> context.deserialize<RouteAPIResponse.Companion.WalkingLeg>(
                json, RouteAPIResponse.Companion.WalkingLeg::class.java)
            else -> throw JsonParseException("Unknown leg type: $type")
        }
    }
}

/**
 * A route finder is responsible for finding the best routes between two destinations.
 * This class reaches out to our server.
 */
class RouteFinder(private val scope: CoroutineScope) {
    private val apiURL = "http://ec2-18-220-26-161.us-east-2.compute.amazonaws.com:8989/route"
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(RouteAPIResponse.Companion.Leg::class.java, LegDeserializer())
        .create()

    /**
     * A function that finds the best routes between two destinations.
     * We also need the current time as input along with the `GTFSData` (vended by `GTFSService`).
     * Finally, the result (or error) is returned via the `onSuccess` and `onError` callback
     * functions.
     */
    fun findBestRoutes(
        initialDestination: Destination,
        finalDestination: Destination,
        currentTime: ClockTime,
        gtfsData: GTFSData,
        onSuccess: Consumer<List<BestRoute>>,
        onError: Consumer<Throwable>
    ) {
        scope.launch {
            try {
                // Do the background work
                val result = withContext(Dispatchers.IO) {
                    findBestRoutesSync(
                        initialDestination, finalDestination, currentTime, gtfsData)
                }
                // Deliver result on main thread
                onSuccess.accept(result)
            } catch (e: Exception) {
                // Print error and return error to user.
                System.err.println("Error: $e")
                onError.accept(e)
            }
        }
    }

    /**
     * The implementation function that synchronously makes the server request.
     * This function encodes the parameters, makes the request to the server, and finally
     * decodes the result.
     */
    private fun findBestRoutesSync(
        initialDestination: Destination,
        finalDestination: Destination,
        currentTime: ClockTime,
        gtfsData: GTFSData
    ): List<BestRoute> {
        // === Encode query parameters
        // Build the query parameters through
        val queryParameters = RouteAPIRequest(
            initialDestination.coords,
            finalDestination.coords,
            currentTime
        ).buildQueryParams()
        // The url on which we'll make the request.
        val url = "$apiURL?$queryParameters"
        // Log the url
        println("Calling at : $url")

        // === Build the request
        // Get the client and build the request
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Kotlin Graphhopper Client")
            .get()
            .build()

        // === Execute Request
        // Make the API call and throw in case of a failure
        val response: Response = try {
            client.newCall(request).execute()
        } catch (e: Exception) {
            throw NetworkErrorException("Network error: ${e.message}")
        }

        // === Get Response Body
        // Check the response is successful and valid
        if (!response.isSuccessful) {
            throw NetworkErrorException("GET request failed. Response Code: ${response.code}")
        }
        // Get the response body or throw if empty
        val responseBody = response.body?.string()
            ?: throw NetworkErrorException("Empty response body in call to routing service.")
        // Close the response to avoid leaks.
        response.close()

        // === Decode the request as a `RouteAPIResponse`
        // Decode the response and construct a list of Destination using the response data.
        val responseType = object : TypeToken<RouteAPIResponse>() {}.type
        val apiResponse: RouteAPIResponse
        // Ask gson to decode or throw NetworkErrorException
        try {
            apiResponse = gson.fromJson(responseBody, responseType)
        } catch (e: Exception) {
            throw NetworkErrorException("JSON parsing failed.")
        }
        // Convert the decoded `RouteAPIResponse` to a list of `BestRoute`
        val foundDestinations = apiResponse.formBestRoutes(gtfsData)

        return foundDestinations
    }
}