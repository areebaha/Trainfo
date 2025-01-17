package edu.vassar.cmpu203.myfirstapplication.Model;

import java.util.HashMap;
import java.util.Map;

/** A collection organizing `TransitTrip`s associated with a specific route.
 * Trips are organized by the direction in which they're going.
 */
public class TripCollection {
    private TransitRoute parentRoute;
    private final Map<TransitTrip.TripDirection, TransitTrip> tripSet;

    /**
     * Constructor for TripCollection.
     */
    public TripCollection() {
        this.parentRoute = null;
        this.tripSet = new HashMap<>();
    }
    /**
     * Get the parent route of this TripCollection.
     * @return
     */
    public TransitRoute getParentRoute() {
        return parentRoute;
    }
    /**
     * Get the trip for a specific direction.
     * @param direction
     * @return
     */
    public Map<TransitTrip.TripDirection, TransitTrip>
    getTripSet(TransitTrip.TripDirection direction, TransitTrip trip) {
        return tripSet;
    }

    /**
     * Link this `TripCollection` to its parent `TransitRoute`.
     * @param parentRoute
     */
    public void linkParentRoute(TransitRoute parentRoute) {
        // Can only link parent route once; otherwise, throw exception.
        if (this.parentRoute != null) throw new RuntimeException("Already linked to a parent");

        this.parentRoute = parentRoute;
    }

    /**
     * Add a TransitTrip to this TripCollection.
     * @param trip
     */
    public void addTrip(TransitTrip trip) {
        tripSet.put(trip.getDirection(), trip);
    }
}
