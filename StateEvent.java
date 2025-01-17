package edu.vassar.cmpu203.myfirstapplication.Controller;

import androidx.annotation.Nullable;

import java.util.List;

import edu.vassar.cmpu203.myfirstapplication.Model.BestRoute;
import edu.vassar.cmpu203.myfirstapplication.Model.Coordinates;
import edu.vassar.cmpu203.myfirstapplication.Model.Destination;
import edu.vassar.cmpu203.myfirstapplication.Model.Station;
import edu.vassar.cmpu203.myfirstapplication.Model.StationDetails;

public abstract class StateEvent {
    /**
     * The app just started.
     */
    public static class AppStarted extends StateEvent {
        public AppStarted() {}
    }

    /**
     * Location services returned either the user's coordinates
     * or couldn't provide the user's location.
     */
    public static class GotUserLocation extends StateEvent {
        @Nullable
        private final Coordinates coordinates;

        public GotUserLocation(@Nullable Coordinates coordinates) {
            this.coordinates = coordinates;
        }

        public @Nullable Coordinates getCoordinates() {
            return coordinates;
        }
    }

    /**
     * The reverse geocoding completed on the initial location coordinates;
     * now we have an initial destination.
     */
    public static class ReverseGeocodedInitialLocation extends StateEvent {
        private final Destination initialDestination;

        public ReverseGeocodedInitialLocation(Destination initialDestination) {
            this.initialDestination = initialDestination;
        }

        public Destination getInitialDestination() {
            return initialDestination;
        }
    }

    /**
     * The user was requested to type their initial location; they've now
     * typed in their location and hit enter.
     */
    public static class EnteredInitialLocation extends StateEvent {
        private final String initialLocation;

        public EnteredInitialLocation(String initialLocation) {
            this.initialLocation = initialLocation;
        }

        public String getInitialLocation() {
            return initialLocation;
        }
    }

    /**
     * The geocoding on the initial location succeeded.
     */
    public static class GeocodedInitialLocation extends StateEvent {
        private final Destination initialDestination;

        public GeocodedInitialLocation(Destination initialDestination) {
            this.initialDestination = initialDestination;
        }

        public Destination getInitialDestination() {
            return initialDestination;
        }
    }

    /**
     * The geocoding on the initial location failed.
     */
    public static class FailedToGeocodeInitialLocation extends StateEvent {
        public FailedToGeocodeInitialLocation() {}
    }

    /**
     * We finished finding the stations near the user's initial location.
     */
    public static class FoundNearbyStations extends StateEvent {
        private final List<Station> nearbyStations;

        public FoundNearbyStations(List<Station> nearbyStations) {
            this.nearbyStations = nearbyStations;
        }

        public List<Station> getNearbyStations() {
            return nearbyStations;
        }
    }

    /**
     * We requested that they user type their final location and they've
     * now hit enter.
     */
    public static class EnteredFinalLocation extends StateEvent {
        private final String finalLocation;

        public EnteredFinalLocation(String finalLocation) {
            this.finalLocation = finalLocation;
        }

        public String getFinalLocation() {
            return finalLocation;
        }
    }

    /**
     * The geocoding on the final location succeeded.
     */
    public static class GeocodedFinalLocation extends StateEvent {
        private final Destination finalLocation;

        public GeocodedFinalLocation(Destination finalLocation) {
            this.finalLocation = finalLocation;
        }

        public Destination getFinalLocation() {
            return finalLocation;
        }
    }

    /**
     * The geocoding on the final location failed.
     */
    public static class FailedToGeocodeFinalLocation extends StateEvent {
        public FailedToGeocodeFinalLocation() {}
    }

    /**
     * The user selected a nearby station from the list of nearby stations.
     */
    public static class SelectedNearbyStation extends StateEvent {
        private final Station selectedStation;

        public SelectedNearbyStation(Station selectedStation) {
            this.selectedStation = selectedStation;
        }

        public Station getSelectedStation() {
            return selectedStation;
        }
    }

    /**
     * The user has selected a station but we were unable to find the
     * station's details.
     */
    public static class FailedToFindStationDetails extends StateEvent {
        private final Station requestedStation;
        private final String error;

        public FailedToFindStationDetails(Station requestedStation, String error) {
            this.requestedStation = requestedStation;
            this.error = error;
        }

        public Station getRequestedStation() {
            return requestedStation;
        }

        public String getError() {
            return error;
        }
    }

    /**
     * The user has selected a station and we were able to find the
     * station's details.
     */
    public static class FoundStationDetails extends StateEvent {
        private final Station requestedStation;
        private final StationDetails stationDetails;

        public FoundStationDetails(Station requestedStation, StationDetails stationDetails) {
            this.requestedStation = requestedStation;
            this.stationDetails = stationDetails;
        }

        public Station getRequestedStation() {
            return requestedStation;
        }

        public StationDetails getStationDetails() {
            return stationDetails;
        }
    }

    /**
     * The user had selected a station from the list of nearby stations
     * but has now canceled their selection and wants to go back to the
     * list of stations.
     */
    public static class CanceledNearbyStationSelection extends StateEvent {
        public CanceledNearbyStationSelection() {}
    }

    /**
     * The user has entered both the initial and final destination
     * and has now clicked the "go" button to start finding routes.
     */
    public static class InitiatedRoute extends StateEvent {
        public InitiatedRoute() {}
    }

    /**
     * We successfully found the best routes.
     */
    public static class FoundBestRoutes extends StateEvent {
        private final List<BestRoute> bestRoutes;

        public FoundBestRoutes(List<BestRoute> bestRoutes) {
            this.bestRoutes = bestRoutes;
        }

        public List<BestRoute> getBestRoutes() {
            return bestRoutes;
        }
    }

    /**
     * The user had clicked "go" to calculate a route but now wants
     * to go back to the main screen.
     */
    public static class CanceledRouteSelection extends StateEvent {
        public CanceledRouteSelection() {}
    }

//    public static class SelectedFavoritesButton extends StateEvent{
//        public SelectedFavoritesButton(){}
//    }
}
