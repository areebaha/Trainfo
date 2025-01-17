package edu.vassar.cmpu203.myfirstapplication.Model;

/** TransitRoute provides information about a route, which is a
* grouping created by public transportation agencies. An example of
* route is MTA's 1 subway line. Corresponds to a GTFS "route."
* `Model.TransitRoute` has an id, human-readable name, color. Unlike
* a GTFS route, `Model.TransitRoute` is associated with a set of trips
* Note that trips are scheduled trips of a public-transportation
* vehicle that are associated with a specific shape and have
* concrete arrival times.
 */
public class TransitRoute {
    private final String id;
    private final String displayName;
    private final String longDisplayName;
    /// A 6-character HEX color code.
    private final String color;
    private final TripCollection trips;

    /**
     * Constructor for TransitRoute.
     * @param id
     * @param displayName
     * @param longDisplayName
     * @param color
     * @param trips
     */
    public TransitRoute(String id, String displayName, String longDisplayName, String color, TripCollection trips) {
        this.id = id;
        this.displayName = displayName;
        this.longDisplayName = longDisplayName;
        this.color = color;
        this.trips = trips;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLongDisplayName() {
        return longDisplayName;
    }

    public String getColor() {
        return color;
    }

    public TripCollection getTrips() {
        return trips;
    }

    /**
     * method that adds a trip to the trip collection.
     * @param trip
     */
    public void addTrip(TransitTrip trip) {
        trips.addTrip(trip);
    }

    /**
     * toString method for TransitRoute.
     * @return
     */
    @Override
    public String toString() {
        return "Model.TransitRoute{" +
                "id=" + id +
                ", displayName='" + displayName + '\'' +
                ", longDisplayName='" + longDisplayName + '\'' +
                ", color='" + color + '\'' +
                ", trips=" + trips +
                '}';
    }
}
