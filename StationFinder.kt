package edu.vassar.cmpu203.myfirstapplication.Controller

import android.content.Context
import edu.vassar.cmpu203.myfirstapplication.Model.Coordinates
import edu.vassar.cmpu203.myfirstapplication.Model.Destination
import edu.vassar.cmpu203.myfirstapplication.Model.Station
import edu.vassar.cmpu203.myfirstapplication.Model.StationDetails
import edu.vassar.cmpu203.myfirstapplication.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import kotlin.concurrent.withLock

/**
 * A station finder is responsible for loading datasets containing station information.
 * This information is used to find nearby stations and to find station details.
 */
class StationFinder(private val scope: CoroutineScope, context: Context, gtfsService: GTFSService) {
    /**
     * The state of the station finder.
     * Keeps track of what data is loaded and the data requests that are pending.
     */
    sealed class State {
        /**
        * The station finder is currently loading data.
        */
        data object Loading : State()

        /**
         * There's been a request to get nearby stations but we're still loading the station data set.
         */
        data class RequestedNearbyStation(val requestedDest: Destination,
                                          val onSuccess: Consumer<List<Station>>) : State()
        /**
         * The station finder has loaded the station list.
         */
        data class LoadedStationList(val stationMap: Map<Coordinates, Station>) : State()
    }

    /**
     * The events that can be sent to the station finder to modify state.
     */
    private sealed class Event {
        data class LoadedStationList(val stations: Map<Coordinates, Station>) : Event()
        data class RequestNearbyStations(val dest: Destination, val onSuccess: Consumer<List<Station>>) : Event()
        data class RequestStationDetails(val station: Station, val onSuccess: Consumer<StationDetails>, val onFailure: Consumer<String>) : Event()
    }

    // We need a lock to control concurrent access to shared mutable state.
    // This is necessary since we're leveraging background threads for processing-
    // intensive tasks.
    private val stateLock = ReentrantLock()
    /**
     * The current state of the station finder.
     */
    private var state: State
    private val gtfsService: GTFSService

    init {
        state = State.Loading
        this.gtfsService = gtfsService

        // Kick off the processing by loading the first-line `stationList` data which should load
        // relatively fast.
        loadStationList(context, Consumer { stationList ->
            updateState(Event.LoadedStationList(stationList))
        })
    }

    /**
     * Throw an exception if the state transition is invalid.
     */
    private fun invalidStateTransition(state: State, event: Event) {
        println("Invalid state transition: $state -> $event")
        throw RuntimeException("Invalid state transition: $state -> $event")
    }

    private fun setState(state: State) {
        println("StationFinder new state: $state (from ${this.state})")
        this.state = state
    }

    /**
     * Update the state of the station finder.
     *
     * There are some general rules for state transitions:
     * 1. Always lock/unlock the state to avoid race conditions
     * 2. When the first data-loading task finishes, start the second one. Also, when data-loading
     *      tasks finish, any pending requests should be taken care of.
     * 3. When a request is made, either save it as a state or fulfill it if we have the necessary.
     * 4. Any undefined state transitions should throw an exception.
     */
    private fun updateState(event: Event) {
        stateLock.withLock {
            when (state) {
                is State.Loading -> {
                    if (event is Event.LoadedStationList) {
                        // Update the state and try to load the big data set
                        setState(State.LoadedStationList(event.stations))
                    } else if (event is Event.RequestNearbyStations) {
                        setState(State.RequestedNearbyStation(event.dest, event.onSuccess))
                    } else {
                        invalidStateTransition(state, event)
                    }
                }
                is State.RequestedNearbyStation -> {
                    val castState = state as State.RequestedNearbyStation
                    if (event is Event.LoadedStationList) {
                        // Complete the request now that we have the data
                        val nearbyStations = findStationsNear(castState.requestedDest, event.stations.values)
                        castState.onSuccess.accept(nearbyStations)

                        // Update the state and try to load the big data set
                        setState(State.LoadedStationList(event.stations))
                    } else if (event is Event.RequestNearbyStations) {
                        // Overwrite if old request if new one comes in.
                        setState(State.RequestedNearbyStation(castState.requestedDest, event.onSuccess))
                    } else {
                        invalidStateTransition(state, event)
                    }
                }
                is State.LoadedStationList -> {
                    val castState = state as State.LoadedStationList
                    if (event is Event.RequestNearbyStations) {
                        // Handle request
                        val nearbyStations = findStationsNear(event.dest, castState.stationMap.values)
                        event.onSuccess.accept(nearbyStations)
                        // State doesn't change, since we only handle the request
                    } else if (event is Event.RequestStationDetails) {
                        // Use the GTFS loader to load the station details.
                        gtfsService.getGTFSData({ gtfsData ->
                            // Lookup the station details by the requested station
                            val stationDetails = getStationDetails(
                                event.station, gtfsData.stationsByCoords)

                            // Handle failure and success by invoking the event's callbacks.
                            if (stationDetails == null) {
                                event.onFailure.accept("Station details not found")
                            } else {
                                event.onSuccess.accept(stationDetails)
                            }
                        })
                    } else {
                        invalidStateTransition(state, event)
                    }
                }
            }
        }
    }

    /**
     * Load nearby stations close to the given destination.
     */
    fun findNearbyStations(dest: Destination, onSuccess: Consumer<List<Station>>) {
        updateState(Event.RequestNearbyStations(dest, onSuccess))
    }

    private fun findStationsNear(dest: Destination, stationList: Collection<Station>): List<Station> {
        val radius = 1.0
        val validStations: MutableList<Station> = ArrayList()

        for (station in stationList) {
            val distance = Coordinates.distanceInKm(dest.coords, station.coords)

            if (distance > radius) continue

            validStations.add(station)
        }

        return validStations
    }

    /**
     * Load details for a station.
     * Precondition: The given station can only be a station vended by `loadNearbyStations`
     */
    fun findStationDetails(station: Station, onSuccess: Consumer<StationDetails>, onFailure: Consumer<String>) {
        updateState(Event.RequestStationDetails(station, onSuccess, onFailure))
    }

    private fun getStationDetails(station: Station, stationDetails: Map<Coordinates, StationDetails>): StationDetails? {
        return stationDetails[station.coords]
    }

    public fun loadStationList(context: Context, onSuccess: Consumer<Map<Coordinates, Station>>) {
        scope.launch {
            try {
                // Do the background work
                val result = withContext(Dispatchers.IO) {
                    loadStationListSync(context)
                }
                onSuccess.accept(result)
            } catch (e: Exception) {
                println("Loading stations CSV failed")
                throw e
            }
        }
    }

    public fun loadStationListSync(context: Context): Map<Coordinates, Station> {
        var line = ""
        val splitBy = ","
        val stations = HashMap<Coordinates, Station>()

        try {
            // Open station_list.csv
            val br = BufferedReader(
                InputStreamReader(
                    context.resources.openRawResource(R.raw.station_list)
                )
            )
            br.readLine()

            // Consume header row:
            // [0: StationID, 1: Line, 2: Stop Name, 3: Borough, 4: Daytime Routes, 5: Structure, 6: GTFS Latitude,
            //      7: GTFS Longitude, 8: North Direction Label, 9: South Direction Label,
            //      10: ADA, 11: ADA Northbound, 12: ADA Southbound, 13: ADA Notes
            while ((br.readLine().also { line = it }) != null) {
                // Parse a single Model.Station
                val stationDetails =
                    line.split(splitBy.toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray() // use comma as separator
                val borough = stationDetails[3]
                val stationName = stationDetails[2]
                val latitude = stationDetails[6].toDouble()
                val longitude = stationDetails[7].toDouble()
                val accessible = stationDetails[10]
                val coordinates = Coordinates(latitude, longitude)
                var accessibilityNote = "";
                if (stationDetails.size > 13) {
                    accessibilityNote = stationDetails[13];
                }
                val newStation = Station(
                        stationName,
                        coordinates,
                        accessible, borough, accessibilityNote)
                stations[coordinates] = newStation
            }
        } catch (e: IOException) {
            println("Error parsing file" + e.message)
        }

        return stations
    }


}



