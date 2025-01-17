package edu.vassar.cmpu203.myfirstapplication.Controller;

import android.content.Context;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import edu.vassar.cmpu203.myfirstapplication.Model.ClockTime;
import edu.vassar.cmpu203.myfirstapplication.Model.Coordinates;
import edu.vassar.cmpu203.myfirstapplication.Model.GTFSData;
import edu.vassar.cmpu203.myfirstapplication.Model.StationDetails;
import edu.vassar.cmpu203.myfirstapplication.Model.TransitRoute;
import edu.vassar.cmpu203.myfirstapplication.Model.TransitTrip;
import edu.vassar.cmpu203.myfirstapplication.Model.TripCollection;
import edu.vassar.cmpu203.myfirstapplication.Model.TripShape;
import edu.vassar.cmpu203.myfirstapplication.R;

/**
 * GTFSLoaderSync loads all GTFS data required by our application.
 */
public class GTFSLoaderSync {
    /**
     * Load all GTFS data required by our application in this thread. It's called by `load` in a
     * background thread.
     * @param context: The application context used to open the resources containing the GTFS data.
     */
    public static GTFSData loadSync(Context context) {
        // === Load (parse and store) all CSV data -----
        // The following static methods are precisely for parsing CSV data.
        // Though they're long functions, they follow the same pattern:
        // 1. Set up the Java data structure where we'll be collecting the information
        // 2. Open a buffered reader and (ignoring the first line), start reading each line.
        // 3. For each line, split it by commas and access the related fields.
        // 4. Convert from strings to more convenient datatypes (such as "accessible" -> boolean).
        // 5. Construct the GTFS datatype, e.g. "stops.txt" is converted to `StationDetails`.
        // 6. Store the GTFS datatype in the collecting type, like a `Map<String, StationDetails>`.
        //
        // Note that the order in which we parse data is important, because we might have
        // to link trip departures to e.g. stations.

        // Load stops/stations
        Tuple2<Map<String, StationDetails>, Map<Coordinates, StationDetails>>  stationsTuple = loadAllStops(context);
        Map<String, StationDetails> stations = stationsTuple.first;
        Map<Coordinates, StationDetails> stationsByCoords = stationsTuple.second;

        // Load trip departures
        Map<String, Map<ClockTime, StationDetails>> tripDepartures = loadAllTripDepartures(context, stations);

        // Load shapes
        Map<String, TripShape> shapes = loadAllShapes(context);

        // Load trip services
        Map<String, TransitTrip.TripService> tripServices = loadAllTripServices(context);

        // Load routes
        Map<String, TransitRoute> routes = loadAllRoutes(context);

        // Load all trips and store them into `routes`. Note that we need to link trips with
        // `tripServices`, `shapes` and `tripDepartures`.
        Map<String, TransitTrip> trips = loadAllTripsIntoRoutes(context, routes, tripServices, shapes, tripDepartures);

        // === Store data -----
        return new GTFSData(stationsByCoords, routes, stations, trips);
    }

    /**
     * A simple tuple class we use when we want to return two objects.
     */
    public static class Tuple2<K, V> {
        K first;
        V second;

        public Tuple2(K first, V second){
            this.first = first;
            this.second = second;
        }

        // getters and setters
    }

    /**
     * Load all stops/stations and return them mapped by both their string identifiers and
     * coordinates.
     */
    private static Tuple2<
            Map<String, StationDetails>,
            Map<Coordinates, StationDetails>
        > loadAllStops(Context context) {
        HashMap<String, StationDetails> stationsByID = new HashMap<>();
        HashMap<Coordinates, StationDetails> stationsByCoords = new HashMap<>();

        // Open stops.txt
        InputStreamReader inputStream = new InputStreamReader(
                context.getResources().openRawResource(R.raw.gtfs_stops));

        // Parse as CSV
        try (CSVReader reader = new CSVReader(inputStream)) {
            // Consume header row:
            // [0: stop_id, 1: stop_name, 2: stop_lat, 3: stop_lon, 4: location_type, 5: parent_station]
            String[] stationDetails = reader.readNext();
            while ((stationDetails = reader.readNext()) != null) {
                String stationID = stationDetails[0];
                String stationName = stationDetails[1];
                double latitude = Double.parseDouble(stationDetails[2]);
                double longitude = Double.parseDouble(stationDetails[3]);

                // Skip 'child' stations; only load parent stations.
                String parentStationID = null;
                if (stationDetails.length > 5 && !stationDetails[5].isEmpty()) {
                    parentStationID = stationDetails[5];
                }

                Coordinates coordinates = new Coordinates(latitude, longitude);

                // TODO: rn i put "Not Accessible"" for accessible but we could also pull it from
                //  the data set
                StationDetails newStation = new StationDetails(
                        stationID, stationName, coordinates, new HashSet<>(), "Not Accessible");

                stationsByID.put(stationID, newStation);
                if (parentStationID != null) {
                    // Link this station to its parent
                    StationDetails parent = stationsByID.get(parentStationID);
                    if (parent == null) {
                        throw new RuntimeException("Unexpectedly found child station before parent station when loading GTFS data");
                    }
                    newStation.linkParent(parent);
                } else {
                    // Only add the parent station to the coords map.
                    stationsByCoords.put(coordinates, newStation);
                }
            }
        } catch (IOException | CsvValidationException e) {
            System.out.println("Failure parsing stops.txt: " + e);
        }

        return new Tuple2<>(stationsByID, stationsByCoords);
    }

    /**
     * Load all departures as a map of `trip_id` to a map of departure times linked to stations.
     * Additionally, map `trip_id`s to `Model.Station`s to link stations to trips.
     */
    private static Map<String, Map<ClockTime, StationDetails>> loadAllTripDepartures(
            Context context,
            Map<String, StationDetails> stations) {
        Map<String, Map<ClockTime, StationDetails>> departures = new HashMap<>();

        // Open stop_times.txt
        InputStreamReader inputStream = new InputStreamReader(
                context.getResources().openRawResource(R.raw.gtfs_stop_times));

        // Parse as CSV
        try (CSVReader reader = new CSVReader(inputStream)) {
            // Consume header row:
            //  [0: trip_id, 1: stop_id, 2: arrival_time, 3: departure_time, 4: stop_sequence]
            String[] departureDetails = reader.readNext();
            while ((departureDetails = reader.readNext()) != null) {
                String tripID = departureDetails[0];
                String stopID = departureDetails[1];
                String[] arrivalTimeComponents = departureDetails[2].split(":");
                String[] departureTimeComponents = departureDetails[3].split(":");

                ClockTime departureTime = new ClockTime(Integer.parseInt(departureTimeComponents[0]),
                        Integer.parseInt(departureTimeComponents[1]),
                        Integer.parseInt(departureTimeComponents[2]));

                // Create inner hash map if it doesn't exist.
                if (!departures.containsKey(tripID)) departures.put(tripID, new HashMap<>());
                // Add to map by linking departure time with station.
                departures.get(tripID).put(departureTime, stations.get(stopID));
            }
        } catch (IOException | CsvValidationException e) {
            System.out.println("Failure parsing stop_times.txt: " + e);
        }

        return departures;
    }

    /**
     * Load all shapes and map them by shape_id.
     * <p>
     * Note: This assumes that `shape_pt_sequence` are continuous integers
     * that start from 0 and end in `shapes.size()-1`. We can make that
     * assumption for the NYC/MTA subway-station dataset.
     */
    private static Map<String, TripShape> loadAllShapes(Context context) {
        Map<String, TripShape> shapes = new HashMap<>();

        // Open shapes.txt
        InputStreamReader inputStream = new InputStreamReader(
                context.getResources().openRawResource(R.raw.gtfs_shapes));

        // Parse as CSV
        try (CSVReader reader = new CSVReader(inputStream)) {
            // Consume header row:
            // [0: shape_id, 1: shape_pt_sequence, 2: shape_pt_lat, 3: shape_pt_lon]
            String[] shapeDetails = reader.readNext();
            while ((shapeDetails = reader.readNext()) != null) {
                String shapeID = shapeDetails[0];
                int shapePointSequence = Integer.parseInt(shapeDetails[1]);
                double shapePointLatitude = Double.parseDouble(shapeDetails[2]);
                double shapePointLongitude = Double.parseDouble(shapeDetails[3]);

                Coordinates shapeCoordinates = new Coordinates(shapePointLatitude, shapePointLongitude);

                // Add shape to map if it doesn't exist
                if (!shapes.containsKey(shapeID)) {
                    shapes.put(shapeID, new TripShape(shapeID, new ArrayList<>()));
                }
                // Add point to the shape.
                shapes.get(shapeID).addPoint(shapeCoordinates, shapePointSequence);
            }
        } catch (IOException | CsvValidationException e) {
            System.out.println("Failure parsing shapes.txt: " + e);
        }

        return shapes;
    }

    /**
     * Load all "trip services" mapped by service_id.
     * <p>
     * "Trip servces" expresses what days the trip operates on. For example, a trip might operate
     * only on Mondays and Tuesdays.
     */
    private static Map<String, TransitTrip.TripService> loadAllTripServices(Context context) {
        Map<String, TransitTrip.TripService> tripServices = new HashMap<>();

        // Open calendar.txt
        InputStreamReader inputStream = new InputStreamReader(
                context.getResources().openRawResource(R.raw.gtfs_calendar));

        // Parse as CSV
        try (CSVReader reader = new CSVReader(inputStream)) {
            // Consume header row:
            // [0: service_id, 1: monday, 2: tuesday, 3: wednesday, 4: thursday, 5: friday,
            //  6: saturday, 7: sunday, 8: start_date, 9: end_date]
            String[] serviceDetails = reader.readNext();
            while ((serviceDetails = reader.readNext()) != null) {
                String serviceID = serviceDetails[0];
                String startDate = serviceDetails[8];
                String endDate = serviceDetails[9];

                boolean onMonday = serviceDetails[1].equals("1");
                boolean onTuesday = serviceDetails[2].equals("1");
                boolean onWednesday = serviceDetails[3].equals("1");
                boolean onThursday = serviceDetails[4].equals("1");
                boolean onFriday = serviceDetails[5].equals("1");
                boolean onSaturday = serviceDetails[6].equals("1");
                boolean onSunday = serviceDetails[7].equals("1");

                TransitTrip.TripService service = new TransitTrip.TripService(
                        serviceID, startDate, endDate,
                        onMonday, onTuesday, onWednesday, onThursday, onFriday, onSaturday, onSunday);
                tripServices.put(serviceID, service);
            }
        } catch (IOException | CsvValidationException e) {
            System.out.println("Failure parsing calendar.txt: " + e);
        }

        return tripServices;
    }

    /**
     * Load all routes mapped by route_id. Note that for each route in the map we return,
     * `route.trips` is an empty `Model.TripCollection` but which is linked to `route.`
     */
    private static Map<String, TransitRoute> loadAllRoutes(Context context) {
        Map<String, TransitRoute> routes = new HashMap<>();

        // Open routes.txt
        InputStreamReader inputStream = new InputStreamReader(
                context.getResources().openRawResource(R.raw.gtfs_routes));

        // Parse as CSV
        try (CSVReader reader = new CSVReader(inputStream)) {
            // Consume header row:
            // [0: agency_id, 1: route_id, 2: route_short_name, 3: route_long_name, 4: route_type, 5: route_desc,
            //  6: route_url, 7: route_color, 8: route_text_color]
            String[] routeDetails = reader.readNext();
            while ((routeDetails = reader.readNext()) != null) {
                String routeID = routeDetails[1];
                String displayName = routeDetails[2];
                String longDisplayName = routeDetails[3];
                String routeColor = routeDetails[7];

                // Create routes and trip collection.
                TripCollection trips = new TripCollection();
                TransitRoute route = new TransitRoute(routeID, displayName, longDisplayName, routeColor, trips);
                // `Model.TripCollection` needs to have access to its parent route.
                trips.linkParentRoute(route);

                routes.put(routeID, route);
            }
        } catch (IOException | CsvValidationException e) {
            System.out.println("Failure parsing routes.txt: " + e);
        }

        return routes;
    }

    /**
     * Load all trips and add them to the `Model.TripCollection` instance `routes.get(route_id).trips`.
     * We obviously need the `routes` to store the trips in. But we also need `tripServices`,
     * `shapes` and `tripDepartures` which are all properties we need for a trip.
     */
    private static Map<String, TransitTrip> loadAllTripsIntoRoutes(Context context,
                                                                   Map<String, TransitRoute> routes,
                                                                   Map<String, TransitTrip.TripService> tripServices,
                                                                   Map<String, TripShape> shapes,
                                                                   Map<String, Map<ClockTime, StationDetails>> tripDepartures) {
        Map<String, TransitTrip> trips = new HashMap<>();

        // Open trips.txt
        InputStreamReader inputStream = new InputStreamReader(
                context.getResources().openRawResource(R.raw.gtfs_trips));

        // Parse as CSV
        try (CSVReader reader = new CSVReader(inputStream)) {
            // Consume header row:
            // [0: route_id, 1: trip_id, 2: service_id, 3: trip_headsign, 4: direction_id, 5: shape_id]
            String[] tripDetails = reader.readNext();
            while ((tripDetails = reader.readNext()) != null) {
                String routeID = tripDetails[0];
                String tripID = tripDetails[1];
                String serviceID = tripDetails[2];
                String headsign = tripDetails[3];
                int directionID = Integer.parseInt(tripDetails[4]);
                String shapeID = tripDetails[5];

                TransitTrip.TripDirection direction = TransitTrip.TripDirection.createWithID(directionID);
                Optional<TripShape> shape = Optional.ofNullable(shapes.get(shapeID));

                // Create trip instance
                TransitRoute parentRoute = routes.get(routeID);
                TransitTrip trip = new TransitTrip(
                        tripID, parentRoute, shape,  tripServices.get(serviceID),
                        headsign, direction, tripDepartures.get(tripID));

                // Store parent route into each station that this trip (and by extension route) traverses.
                for (StationDetails stopStation: tripDepartures.get(tripID).values()) {
                    stopStation.addRoute(parentRoute);
                }

                // Store trip into routes object.
                parentRoute.addTrip(trip);

                // Store trip in hashmap
                trips.put(tripID, trip);
            }
        } catch (IOException | CsvValidationException e) {
            System.out.println("Failure parsing trips.txt: " + e);
        }

        return trips;
    }
}
