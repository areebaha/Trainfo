package edu.vassar.cmpu203.myfirstapplication.Model;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/** A trips correspond to one trip made by a public-transportation
* vehicles. E.g. For MTA, it could be the 1 subway line going
* downtown at 12 pm. Corresponds to GTFS's "trip."
* Contains an id, refers back to the parent route. Also includes
* the shape of the trip, the service, and the headsign (what's displayed
* on the display, e.g. "Outbound"). Unlike a GTFS "trip," `Model.TransitTrip`
* refers to all the stations it passes by, organized by departure times.
 */
public class TransitTrip {
    /// Corresponds to GTFS's "calendar.txt"
    ///
    /// @param startDate TODO: Pick a more appropriate format.
    public record TripService(String id, String startDate, String endDate, boolean onMonday,
                              boolean onTuesday, boolean onWednesday, boolean onThursday,
                              boolean onFriday, boolean onSaturday, boolean onSunday) {
        /**
         * Constructor for TripService.
         *
         * @param id
         * @param startDate
         * @param endDate
         * @param onMonday
         * @param onTuesday
         * @param onWednesday
         * @param onThursday
         * @param onFriday
         * @param onSaturday
         * @param onSunday
         */
        public TripService {}

        /**
         * toString method for TripService.
         *
         * @return
         */
        @NonNull
        @Override
        public String toString() {
            String daysString = "";
            if (onMonday) daysString += "M";
            if (onTuesday) daysString += "T";
            if (onWednesday) daysString += "W";
            if (onThursday) daysString += "R";
            if (onFriday) daysString += "F";
            if (onSaturday) daysString += "S";
            if (onSunday) daysString += "D";

            return "TripService{" +
                    "id='" + id + '\'' +
                    ", days=" + daysString +
                    ", startDate='" + startDate + '\'' +
                    ", endDate='" + endDate + '\'' +
                    '}';
        }
    }


    /**
     * Describes the direction (e.g. inbound vs outbound) in
     * the `direction_id` property of the GTFS `trips` specification.
     */
    public enum TripDirection {
        ZERO(0), ONE(1);

        private final int id;

        TripDirection(int id) {
            this.id = id;
        }

        public static TripDirection createWithID(int id) {
            if (id == ZERO.id) return ZERO;
            if (id == ONE.id) return ONE;

            throw new IllegalArgumentException("Model.TransitTrip.RouteDirection can only have id values of :" +
                    Arrays.toString(TripDirection.values()));
        }
    }

    private final String id;
    private final TransitRoute parentRoute;
    private final Optional<TripShape> shape;
    /// The days when this trip runs.
    private final TripService service;
    private final String headsign;
    private final TripDirection direction;
    private final Map<ClockTime, StationDetails> departures;

    /**
     * Constructor for TransitTrip.
     * @param id
     * @param parentRoute
     * @param shape
     * @param service
     * @param headsign
     * @param direction
     * @param departures
     */
    public TransitTrip(String id, TransitRoute parentRoute, Optional<TripShape> shape, TripService service,
                       String headsign, TripDirection direction, Map<ClockTime, StationDetails> departures) {
        this.id = id;
        this.parentRoute = parentRoute;
        this.shape = shape;
        this.service = service;
        this.headsign = headsign;
        this.direction = direction;
        this.departures = departures;
    }

    public String getId() {
        return id;
    }

    public TransitRoute getParentRoute() {
        return parentRoute;
    }

    public Optional<TripShape> getShape() {
        return shape;
    }

    public TripService getService() {
        return service;
    }

    public String getHeadsign() {
        return headsign;
    }

    public TripDirection getDirection() {
        return direction;
    }

    public Map<ClockTime, StationDetails> getDepartures() {
        return departures;
    }

    @NonNull
    @Override
    public String toString() {
        return "Model.TransitTrip{" +
                "id='" + id + '\'' +
                ", parentRoute=" + parentRoute +
                ", shape=" + shape +
                ", service=" + service +
                ", headsign='" + headsign + '\'' +
                ", direction=" + direction +
                ", departures=" + departures +
                '}';
    }
}
