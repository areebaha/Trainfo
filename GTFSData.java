package edu.vassar.cmpu203.myfirstapplication.Model;

import java.util.Map;

public class GTFSData {
    /**
     * Only parent stations mapped in terms of their coordinates.
     */
    private final Map<Coordinates, StationDetails> stationsByCoords;
    /**
     * All routes mapped by their string identifier.
     */
    private final Map<String, TransitRoute> routesByID;
    /**
     * All trips mapped by their string identifier.
     */
    private final Map<String, TransitTrip> tripsByID;
    /**
     * All stations mapped by their string identifier.
     */
    private final Map<String, StationDetails> stationsByID;

    public GTFSData(Map<Coordinates, StationDetails> stationsByCoords,
                    Map<String, TransitRoute> routesByID,
                    Map<String, StationDetails> stationsByID,
                    Map<String, TransitTrip> tripsByID) {
        this.stationsByCoords = stationsByCoords;
        this.routesByID = routesByID;
        this.stationsByID = stationsByID;
        this.tripsByID = tripsByID;
    }

    /**
     * Get all stations mapped by their coordinates.
     */
    public Map<Coordinates, StationDetails> getStationsByCoords() {
        return stationsByCoords;
    }

    /**
     * Get all routes mapped by their string identifier.
     */
    public Map<String, TransitRoute> getRoutesByID() {
        return routesByID;
    }

    /**
     * Get all stations mapped by their string identifier.
     */
    public Map<String, StationDetails> getStationsByID() {
        return stationsByID;
    }

    /**
     * Get all trips mapped by their string identifier.
     */
    public Map<String, TransitTrip> getTripsByID() {
        return tripsByID;
    }
}
