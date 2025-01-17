package edu.vassar.cmpu203.myfirstapplication.Controller;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import edu.vassar.cmpu203.myfirstapplication.MainActivity;
import edu.vassar.cmpu203.myfirstapplication.Model.BestRoute;
import edu.vassar.cmpu203.myfirstapplication.Model.ClockTime;
import edu.vassar.cmpu203.myfirstapplication.Model.Coordinates;
import edu.vassar.cmpu203.myfirstapplication.Model.Destination;
import edu.vassar.cmpu203.myfirstapplication.Model.Station;
import edu.vassar.cmpu203.myfirstapplication.Model.StationDetails;
import edu.vassar.cmpu203.myfirstapplication.View.UIDelegate;
import kotlin.Unit;
import kotlinx.coroutines.CoroutineScope;

/**
 * Controller carries out functions and communicates between the Model and UIDelegate.
 * See `doc/Trainfo App State Machine.png` for a diagram of the state and the state transitions.
 */
public class Controller {

    private CoroutineScope scope = CoroutineScopeFactory.Companion.getMainScope();
    private final GeocodingService geocodingService;
    private GTFSService gtfsService;
    private final StationFinder stationFinder;
    private final LocationServices locationServices;
    private final RouteFinder routeFinder;
    private final UIDelegate uiDelegate;

    private State state;
    // We use a lock to control access to the state since `updateState`
    // can be called from multiple threads (due to API calls, background
    // processing threads, etc.)
    private final ReentrantLock stateLock = new ReentrantLock();

    /**
     * A live data instance for state.
     * Used for Jetpack Compose compatibility
     */
    private final MutableLiveData<State> stateFlow;

    /**
     * Constructor for Controller.
     * @param activity
     */
    public Controller(MainActivity activity) {
        this.geocodingService = new GeocodingService(scope);
        this.gtfsService = new GTFSService(scope, activity);
        this.stationFinder = new StationFinder(scope, activity, gtfsService);
        this.locationServices = new LocationServices(activity);
        this.routeFinder = new RouteFinder(scope);
        this.uiDelegate = activity;

        this.state = new Uninitialized();
        this.stateFlow = new MutableLiveData<State>(state);
    }

    public void onPermissionsResponse() {
        System.out.println("Permissions response in controller.");
        locationServices.onPermissionsResponse();
    }

    /**
     * Throws an IllegalStateException indicating an illegal state transition.
     *
     * @param event the StateEvent that caused the illegal transition
     * @throws IllegalStateException always thrown to indicate an illegal state transition
     */
    protected void illegalStateTransitionWith(StateEvent event) throws IllegalStateException {
        throw new IllegalStateException(
                "Illegal state transition in state " + state.getClass().getSimpleName() +
                        " for event: " + event.getClass().getSimpleName());
    }

    /**
     * Gets the current time.
     * @return
     */
    private ClockTime getCurrentTime() {
        // Get NYC time zone
        ZoneId newYorkZoneId = ZoneId.of("America/New_York");

        // Get the current date and time in New York time zone
        ZonedDateTime newYorkTime = ZonedDateTime.now(newYorkZoneId);

        // Extract hours, minutes, and seconds
        int hours = newYorkTime.getHour();
        int minutes = newYorkTime.getMinute();
        int seconds = newYorkTime.getSecond();

        return new ClockTime(hours, minutes, seconds);
    }

    /**
     * Sets the current state.
     * @param newState
     */
    private void setState(State newState) {
        System.out.println("New state: " + newState);
        this.state = newState;
        // Update this value for Jetpack Compose compatibility
        this.stateFlow.postValue(newState);
    }

    /**
     * Updates the state of the controller.
     * @param event
     */
    public void updateState(StateEvent event) {
        stateLock.lock();
        if (state instanceof Uninitialized) {
            if (event instanceof StateEvent.AppStarted) {
                // The AppStarted event is sent out when MainActivity has set up the basic views.
                setState(new RequestedLocation());
                uiDelegate.showInitialDestLoading(false);
                locationServices.requestLocation(
                        coordinates -> updateState(new StateEvent.GotUserLocation(coordinates)),
                        error -> updateState(new StateEvent.GotUserLocation(null)));
            }
            else {
                illegalStateTransitionWith(event);
            }
        } else if (state instanceof RequestedLocation) {
            if (event instanceof StateEvent.GotUserLocation castEvent) {
                // We've already requested the location and now location services has come back
                // with the current location (or null).

                @Nullable Coordinates coords = castEvent.getCoordinates();

                if (coords != null) {
                    // We successfully got the user's current coords; now reverse geocode it
                    // so that we can present a human-readable text.
                    setState(new GotInitialDestinationCoords(coords));
                    uiDelegate.showInitialDestLoading(true);
                    geocodingService.lookupCoordsAsync(
                            coords,
                            destination -> {
                                updateState(new StateEvent.ReverseGeocodedInitialLocation(destination));
                                return Unit.INSTANCE;
                            },
                            error -> {
                                updateState(new StateEvent.ReverseGeocodedInitialLocation(
                                        new Destination(coords, "Unable to Find Location Name")));
                                return Unit.INSTANCE;
                            });
                } else {
                    // We couldn't get the user's location; ask the user to type in the initial
                    // destination/location.
                    setState(new RequestedTextForInitialDestination());
                    uiDelegate.requestInitialLocationText(
                            initialLocation -> updateState(
                                    new StateEvent.EnteredInitialLocation(initialLocation)));
                }

            } else {
                illegalStateTransitionWith(event);
            }
        } else if (state instanceof GotInitialDestinationCoords) {
            if (event instanceof StateEvent.ReverseGeocodedInitialLocation castEvent) {
                // We've got the coords for the initial destination and the reverse geocoding
                // we requested has successfully returned the destination name.

                Destination initialDestination = castEvent.getInitialDestination();

                // We've got the initial destination, now find the nearby stations.
                setState(new GotInitialDestination(initialDestination));
                uiDelegate.showInitialDestination(initialDestination);
                stationFinder.findNearbyStations(
                        initialDestination,
                        stations -> updateState(new StateEvent.FoundNearbyStations(stations)));
            }
            else {
                illegalStateTransitionWith(event);
            }
        } else if (state instanceof RequestedTextForInitialDestination) {
            if (event instanceof StateEvent.EnteredInitialLocation castEvent) {
                // Here, we couldn't get the initial destination by the user current location,
                // so we've requested that the user types their initial destination.

                String initialLocation = castEvent.getInitialLocation();

                // Now that we have the user's initial destination text, geocode it to get the
                // associated coordinates.
                setState(new GotInitialDestinationText(initialLocation));
                uiDelegate.showInitialLocationText(initialLocation);
                geocodingService.lookupNameAsync(
                        initialLocation,
                        destinations -> {
                            updateState(new StateEvent.GeocodedInitialLocation(destinations.get(0)));
                            return Unit.INSTANCE;
                        },
                        error -> {
                            updateState(new StateEvent.FailedToGeocodeInitialLocation());
                            return Unit.INSTANCE;
                        });
            } else {
                illegalStateTransitionWith(event);
            }
        } else if (state instanceof GotInitialDestinationText) {
            if (event instanceof StateEvent.GeocodedInitialLocation castEvent) {
                // The user has typed in their initial location and the geocoding has successfully
                // come back with the associated coords.

                Destination initialDestination = castEvent.getInitialDestination();

                // We've got the initial destination, now find the nearby stations.
                setState(new GotInitialDestination(initialDestination));
                uiDelegate.showInitialDestination(initialDestination);
                stationFinder.findNearbyStations(
                        initialDestination,
                        stations -> updateState(new StateEvent.FoundNearbyStations(stations)));
            } else if (event instanceof StateEvent.FailedToGeocodeInitialLocation) {
                // The user has typed in their initial location but geocoding has failed.

                // Display error message and ask the user to retype their location.
                setState(new RequestedTextForInitialDestination());
                uiDelegate.showFailureToGeocodeInitial();
                uiDelegate.requestInitialLocationText(
                        initialLocation -> updateState(
                                new StateEvent.EnteredInitialLocation(initialLocation)));
            } else {
                illegalStateTransitionWith(event);
            }
        } else if (state instanceof GotInitialDestination castState) {
            if (event instanceof StateEvent.FoundNearbyStations castEvent) {
                // We've got the initial destination and we just found the stations near the
                // initial destination/location.

                List<Station> nearbyStations = castEvent.getNearbyStations();

                // Display the nearby stations. The user has two options moving forward:
                // they can either tap on a station marker to expand it or they can type in the
                // final destination which they need to start a route.
                setState(new GotNearbyStations(castState.initialDestination, nearbyStations));
                uiDelegate.showNearbyStations(
                        castState.initialDestination,
                        nearbyStations,
                        selectedStation -> updateState(
                                new StateEvent.SelectedNearbyStation(selectedStation)
                        ));
                uiDelegate.requestFinalLocationText(
                        finalLocation -> updateState(new StateEvent.EnteredFinalLocation(finalLocation)));
            } else {
                illegalStateTransitionWith(event);
            }
        } else if (state instanceof GotNearbyStations castState) {
            if (event instanceof StateEvent.SelectedNearbyStation castEvent) {
                // We've got the nearby stations and the user has selected a station from that list.

                Station selectedStation = castEvent.getSelectedStation();

                // Open a new screen containing the current information on the station and indicate
                // that the details are loading. Offer the user the ability to click the back button.
                // Start loading the station details.
                setState(new RequestedStationDetails(castState, selectedStation));
                uiDelegate.showLoadingStationDetails(
                        selectedStation,
                        () -> updateState(new StateEvent.CanceledNearbyStationSelection()));
                stationFinder.findStationDetails(
                        selectedStation,
                        stationDetails -> updateState(new StateEvent.FoundStationDetails(selectedStation, stationDetails)),
                        error -> updateState(new StateEvent.FailedToFindStationDetails(selectedStation, error)));
            } else if (event instanceof StateEvent.EnteredFinalLocation castEvent) {
                // We've got the nearby stations but the user has now typed their final destination.

                String finalLocation = castEvent.getFinalLocation();

                // Don't allow the user to make any changes to the final destination but still
                // display the text they typed in. We do this because we don't want any changes
                // while we're geocoding the final destination text.
                setState(new GotFinalDestinationText(castState, finalLocation));
                uiDelegate.showFinalLocationText(finalLocation);
                geocodingService.lookupNameAsync(
                        finalLocation,
                        destinations -> {
                            updateState(new StateEvent.GeocodedFinalLocation(destinations.get(0)));
                            return Unit.INSTANCE;
                        },
                        error -> {
                            updateState(new StateEvent.FailedToGeocodeFinalLocation());
                            return Unit.INSTANCE;
                        });
            } else if (event instanceof StateEvent.FoundStationDetails) {
                // We've gone back to GotNearbyStations but the station details
                // just loaded; so just ignore this event, it's irrelevant.
            } else {
                illegalStateTransitionWith(event);
            }
        } else if (state instanceof GotFinalDestinationText castState) {
            if (event instanceof StateEvent.GeocodedFinalLocation castEvent) {
                // The user has typed in their final destination and the requested geocoding has
                // successfully yielded the associated coordinates.

                Destination finalDestination = castEvent.getFinalLocation();

                // Show the final destination name that geocoding returned. Offer the user the
                // ability to start a route or type in a different final destination.
                setState(new GotFinalDestination(castState.nearbyStationsState, finalDestination));
                uiDelegate.showFinalDestination(
                        castState.getNearbyStationsState().initialDestination,
                        finalDestination,
                        newFinalLocation -> updateState(new StateEvent.EnteredFinalLocation(newFinalLocation)));
                uiDelegate.resetBoundingBox(
                        castState.getNearbyStationsState().initialDestination,
                        finalDestination);
                uiDelegate.activateRouteButton(
                        () -> updateState(new StateEvent.InitiatedRoute()));
            } else if (event instanceof StateEvent.FailedToGeocodeFinalLocation) {
                // The user has typed in their final destination but the requested geocoding has
                // failed.

                List<Station> nearbyStations =  castState.getNearbyStationsState().nearbyStations;

                // Show the user an alert informing them of the failure and go back to showing
                // nearby stations.
                setState(new GotNearbyStations(
                        castState.getNearbyStationsState().initialDestination,
                        nearbyStations));
                uiDelegate.showFailureToGeocodeFinal();
                uiDelegate.showNearbyStations(
                        castState.getNearbyStationsState().initialDestination,
                        nearbyStations,
                        selectedStation -> updateState(
                                new StateEvent.SelectedNearbyStation(selectedStation)
                        ));
                uiDelegate.requestFinalLocationText(
                        finalLocation -> updateState(new StateEvent.EnteredFinalLocation(finalLocation)));
            } else if (event instanceof StateEvent.SelectedNearbyStation) {
                // Ignore SelectedNearbyStation since we're in the process of geocoding the final
                // destination text.
            } else {
                illegalStateTransitionWith(event);
            }
        } else if (state instanceof GotFinalDestination castState) {
            if (event instanceof StateEvent.InitiatedRoute) {
                // We've got the final destination and the user has requested to find a route.

                Destination initialDestination = castState.getNearbyStationsState().initialDestination;
                Destination finalDestination = castState.finalDestination;
                ClockTime currentTime = getCurrentTime();

                // Show that the route is loading in a new screen and actually request the new route.
                // Offer the user the ability to cancel the loading of the route.
                setState(new RequestedBestRoutes(castState.getNearbyStationsState(), castState.finalDestination));
                uiDelegate.showRoutesLoading(
                        stateFlow,
                        () -> updateState(new StateEvent.CanceledRouteSelection()));
                // TODO: Add proper error handling (show alert in delegate and go back).
                gtfsService.getGTFSData(gtfsData -> {
                    routeFinder.findBestRoutes(
                            initialDestination, finalDestination, currentTime,
                            gtfsData,
                            bestRoutes -> updateState(new StateEvent.FoundBestRoutes(bestRoutes)),
                            error -> System.err.println("Error finding best routes: " + error));
                });
            } else if (event instanceof StateEvent.EnteredFinalLocation castEvent) {
                // We've got the final destination but the user typed in a new final destination.

                String finalLocation = castEvent.getFinalLocation();

                // Go back to the GotFinalDestinationText state.
                setState(new GotFinalDestinationText(castState.getNearbyStationsState(), finalLocation));
                uiDelegate.showFinalLocationText(finalLocation);
                geocodingService.lookupNameAsync(
                        finalLocation,
                        destinations -> {
                            updateState(new StateEvent.GeocodedFinalLocation(destinations.get(0)));
                            return Unit.INSTANCE;
                        },
                        error -> {
                            updateState(new StateEvent.FailedToGeocodeFinalLocation());
                            return Unit.INSTANCE;
                        });
            } else {
                illegalStateTransitionWith(event);
            }
        } else if (state instanceof RequestedBestRoutes castState) {
            if (event instanceof StateEvent.FoundBestRoutes castEvent) {
                // We've requested the best routes in a new screen and we've actually found them.

                List<BestRoute> bestRoutes = castEvent.getBestRoutes();

                // Show the best routes and offer the user the ability to press the back button to cancel.
                setState(new GotBestRoutes(castState.getNearbyStationsState(), castState.finalDestination, bestRoutes));
                uiDelegate.showRoutes(
                        bestRoutes,
                        () -> updateState(new StateEvent.CanceledRouteSelection()));
            } else if (event instanceof StateEvent.CanceledRouteSelection) {
                // We've requested the best routes but the user has pressed the back button.

                Destination finalDestination = castState.getFinalDestination();

                // Go back to the GotFinalDestination state.
                setState(new GotFinalDestination(castState.getNearbyStationsState(), castState.finalDestination));
                uiDelegate.showFinalDestination(
                        castState.getNearbyStationsState().initialDestination,
                        finalDestination,
                        newFinalLocation -> updateState(new StateEvent.EnteredFinalLocation(newFinalLocation)));
                uiDelegate.activateRouteButton(
                        () -> updateState(new StateEvent.InitiatedRoute()));
            } else if (event instanceof StateEvent.EnteredFinalLocation) {
                // We've selected a station to view its details.
                // Ignore EnteredFinalLocation; we might get this as the FinalDestination text view
                // is dismissed
            } else {
                illegalStateTransitionWith(event);
            }
        } else if (state instanceof GotBestRoutes castState) {
            if (event instanceof StateEvent.CanceledRouteSelection) {
                // We've got the best routes but the user pressed the back button.

                Destination finalDestination = castState.getFinalDestination();

                // Go back to the GotFinalDestination state.
                setState(new GotFinalDestination(castState.getNearbyStationsState(), castState.finalDestination));
                uiDelegate.showFinalDestination(
                        castState.getNearbyStationsState().initialDestination,
                        finalDestination,
                        newFinalLocation -> updateState(new StateEvent.EnteredFinalLocation(newFinalLocation)));
                uiDelegate.activateRouteButton(
                        () -> updateState(new StateEvent.InitiatedRoute()));
            } else if (event instanceof StateEvent.EnteredFinalLocation) {
                // We've selected a station to view its details.
                // Ignore EnteredFinalLocation; we might get this as the FinalDestination text view
                // is dismissed
            } else {
                illegalStateTransitionWith(event);
            }
        } else if (state instanceof RequestedStationDetails castState) {
            if (event instanceof StateEvent.CanceledNearbyStationSelection) {
                // We've got the nearby stations and have selected a station to view its details
                // in a new screen, but the user pressed the back button.

                // Go back to the `GotNearbyStations` state.
                setState(new GotNearbyStations(
                        castState.getNearbyStationsState().initialDestination,
                        castState.getNearbyStationsState().nearbyStations));
                uiDelegate.showNearbyStations(
                        castState.getNearbyStationsState().initialDestination,
                        castState.getNearbyStationsState().nearbyStations,
                        selectedStation -> updateState(
                                new StateEvent.SelectedNearbyStation(selectedStation)
                        ));
                uiDelegate.requestFinalLocationText(
                        finalLocation -> updateState(new StateEvent.EnteredFinalLocation(finalLocation)));
            } else if (event instanceof StateEvent.FoundStationDetails castEvent) {
                // We've got the nearby stations and have selected a station to view its details,
                // and the station details have loaded.

                StationDetails stationDetails = castEvent.getStationDetails();

                // Check that the event's requested station is the same as the state's requested station.
                // We should only process a GotStationDetails event if it matches our current request.
                //
                // E.g. If we click at two events and one loads later, we should only work with the event
                // that fits our currently requested station.
                if (castEvent.getRequestedStation().getCoords().equals(
                        castState.getRequestedStation().getCoords())) {
                    // Show the details of the selected station and offer the option to go back.
                    setState(new GotStationDetails(castState.getNearbyStationsState(), stationDetails));
                    uiDelegate.showStationDetails(
                            stationDetails,
                            () -> updateState(new StateEvent.CanceledNearbyStationSelection()));
                }
            } else if (event instanceof StateEvent.FailedToFindStationDetails castEvent) {
                // The user has selected a station to view its details, but we couldn't find the
                // station details.

                String error = castEvent.getError();

                // Check that the event's requested station is the same as the state's requested station.
                // E.g. If we click at two events and one loads later, we should only work with the event
                // that fits our currently requested station.
                if (castEvent.getRequestedStation().getCoords().equals(
                        castState.getRequestedStation().getCoords())) {
                    // If we failed to find station details, alert the user of the failure and
                    // go back to the GotNearbyStations state.
                    setState(new GotNearbyStations(
                            castState.getNearbyStationsState().initialDestination,
                            castState.getNearbyStationsState().nearbyStations));
                    uiDelegate.showFailureToGetStationDetails(error);
                    uiDelegate.showNearbyStations(
                            castState.getNearbyStationsState().initialDestination,
                            castState.getNearbyStationsState().nearbyStations,
                            selectedStation -> updateState(
                                    new StateEvent.SelectedNearbyStation(selectedStation)
                            ));
                    uiDelegate.requestFinalLocationText(
                            finalLocation -> updateState(new StateEvent.EnteredFinalLocation(finalLocation)));
                }
            } else if (event instanceof StateEvent.EnteredFinalLocation) {
                // We've got the nearby stations and have selected a station to view its details.

                // Ignore EnteredFinalLocation; we might get this as the FinalDestination text view
                // is dismissed
            } else {
                illegalStateTransitionWith(event);
            }
        } else if (state instanceof GotStationDetails castState) {
            if (event instanceof StateEvent.CanceledNearbyStationSelection) {
                // The user has selected a station to view its details and we got the details, but
                // they pressed the back button.

                // Alert the user of the failure to find station details and go back to the
                // GotNearbyStations state.
                setState(new GotNearbyStations(
                        castState.getNearbyStationsState().initialDestination,
                        castState.getNearbyStationsState().nearbyStations));
                uiDelegate.showNearbyStations(
                        castState.getNearbyStationsState().initialDestination,
                        castState.getNearbyStationsState().nearbyStations,
                        selectedStation -> updateState(
                                new StateEvent.SelectedNearbyStation(selectedStation)
                        ));
                uiDelegate.requestFinalLocationText(
                        finalLocation -> updateState(new StateEvent.EnteredFinalLocation(finalLocation)));
            } else if (event instanceof StateEvent.EnteredFinalLocation) {
                // We've selected a station to view its details.
                // Ignore EnteredFinalLocation; we might get this as the FinalDestination text view
                // is dismissed
            } else {
                illegalStateTransitionWith(event);
            }
        }
        stateLock.unlock();
    }

    public boolean stateIsUninitialized() {
        return state instanceof Uninitialized;
    }

    /**
     * Uninitialized state.
     */
    public static class Uninitialized extends State {
        public Uninitialized() {}
    }

    /**
     * Requested location state.
     */
    public static class RequestedLocation extends State {
        public RequestedLocation() {}
    }

    /**
     * Got initial destination coordinates state.
     */
    public static class GotInitialDestinationCoords extends State {
        private final Coordinates initialDestinationCoords;

        public GotInitialDestinationCoords(Coordinates initialDestinationCoords) {
            this.initialDestinationCoords = initialDestinationCoords;
        }

        public Coordinates getInitialDestinationCoords() {
            return initialDestinationCoords;
        }
    }

    /**
     * Requested text for the initial destination state.
     */
    public static class RequestedTextForInitialDestination extends State {
        public RequestedTextForInitialDestination() {}
    }

    /**
     * Got initial destination state.
     */
    public static class GotInitialDestination extends State {
        private final Destination initialDestination;

        public GotInitialDestination(Destination initialDestination) {
            this.initialDestination = initialDestination;
        }

        public Destination getInitialDestination() {
            return initialDestination;
        }
    }

    /**
     * Got initial destination text state.
     */
    public static class GotInitialDestinationText extends State {
        private final String initialLocation;

        public GotInitialDestinationText(String initialLocation){
            this.initialLocation = initialLocation;
        }

        public String getInitialLocation() {
            return initialLocation;
        }
    }

    /**
     * Got nearby stations state.
     */
    public static class GotNearbyStations extends State {
        private final Destination initialDestination;
        private final List<Station> nearbyStations;

        /**
         * Constructor for GotNearbyStations.
         * @param initialDestination
         * @param nearbyStations
         */
        public GotNearbyStations(Destination initialDestination, List<Station> nearbyStations) {
            this.initialDestination = initialDestination;
            this.nearbyStations = nearbyStations;
        }

        /**
         * Gets the initial destination.
         * @return Destination
         */
        public Destination getInitialDestination() {
            return initialDestination;
        }

        /**
         * Gets the nearby stations.
         * @return List<Station>
         */
        public List<Station> getNearbyStations() {
            return nearbyStations;
        }
    }

    /**
     * State for when final destination text is received.
     */
    public static class GotFinalDestinationText extends State {
        private final GotNearbyStations nearbyStationsState;
        private final String finalLocation;

        /**
         * Constructor for GotFinalDestinationText.
         * @param nearbyStationsState
         * @param finalLocation
         */
        public GotFinalDestinationText(GotNearbyStations nearbyStationsState, String finalLocation){
            this.nearbyStationsState = nearbyStationsState;
            this.finalLocation = finalLocation;
        }

        /**
         * Gets the nearby stations state.
         * @return nearbyStationsState
         */
        public GotNearbyStations getNearbyStationsState() {
            return nearbyStationsState;
        }

        /**
         * Gets the final location.
         * @return finalLocation
         */
        public String getFinalLocation() {
            return finalLocation;
        }
    }

    /**
     * State for when final destination is received.
     */
    public static class GotFinalDestination extends State {
        private final GotNearbyStations nearbyStationsState;
        private final Destination finalDestination;

        /**
         * Constructor for GotFinalDestination.
         * @param nearbyStationsState
         * @param finalDestination
         */
        public GotFinalDestination(GotNearbyStations nearbyStationsState, Destination finalDestination){
            this.nearbyStationsState = nearbyStationsState;
            this.finalDestination = finalDestination;
        }

        /**
         * Gets the nearby stations state.
         * @return nearbyStationsState
         */
        public GotNearbyStations getNearbyStationsState() {
            return nearbyStationsState;
        }

        /**
         * Gets the final destination.
         * @return Destination
         */
        public Destination getFinalDestination() {
            return finalDestination;
        }
    }

    /**
     * State for when best routes are requested.
     */
    public static class RequestedBestRoutes extends State {
        private final GotNearbyStations nearbyStationsState;
        private final Destination finalDestination;

        /**
         * Constructor for RequestedBestRoutes.
         * @param nearbyStationsState
         * @param finalDestination
         */
        public RequestedBestRoutes(GotNearbyStations nearbyStationsState,
                                   Destination finalDestination) {
            this.nearbyStationsState = nearbyStationsState;
            this.finalDestination = finalDestination;
        }

        /**
         * Gets the nearby stations state.
         * @return nearbyStationsState
         */
        public GotNearbyStations getNearbyStationsState() {
            return nearbyStationsState;
        }

        /**
         * Gets the final destination.
         * @return finalDestination
         */
        public Destination getFinalDestination() {
            return finalDestination;
            }
    }

    /**
     * State for when best routes are received.
     */
    public static class GotBestRoutes extends State {
        private final GotNearbyStations nearbyStationsState;
        private final Destination finalDestination;
        private final List<BestRoute> bestRoutes;

        /**
         * Constructor for GotBestRoutes.
         * @param nearbyStationsState
         * @param finalDestination
         * @param bestRoutes
         */
        public GotBestRoutes(GotNearbyStations nearbyStationsState,
                             Destination finalDestination,
                             List<BestRoute> bestRoutes) {
            this.nearbyStationsState = nearbyStationsState;
            this.finalDestination = finalDestination;
            this.bestRoutes = bestRoutes;
        }

        public GotNearbyStations getNearbyStationsState() {
            return nearbyStationsState;
        }

        public Destination getFinalDestination() {
            return finalDestination;
        }

        public List<BestRoute> getBestRoutes() {
            return bestRoutes;
        }
    }

    /**
     * State for when station details are requested.
     */
    public static class RequestedStationDetails extends State {
        private final GotNearbyStations nearbyStationsState;
        private final Station requestedStation;

        /**
         * Constructor for RequestedStationDetails.
         * @param nearbyStationsState
         * @param requestedStation
         */
        public RequestedStationDetails(GotNearbyStations nearbyStationsState, Station requestedStation) {
            this.nearbyStationsState = nearbyStationsState;
            this.requestedStation = requestedStation;
        }

        public GotNearbyStations getNearbyStationsState() {
            return nearbyStationsState;
        }

        public Station getRequestedStation() {
            return requestedStation;
        }
    }

    /**
     * State for when station details are received.
     */
    public static class GotStationDetails extends State {
        private final GotNearbyStations nearbyStationsState;
        private final StationDetails station;

        /**
         * Constructor for GotStationDetails.
         * @param nearbyStationsState
         * @param station
         */
        public GotStationDetails(GotNearbyStations nearbyStationsState, StationDetails station) {
            this.nearbyStationsState = nearbyStationsState;
            this.station = station;
        }

        public GotNearbyStations getNearbyStationsState() {
            return nearbyStationsState;
        }

        /**
         * gets a station with type stationDetails
         * @return station
         */
        public StationDetails getStation() {
            return station;
        }
    }
}