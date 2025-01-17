package edu.vassar.cmpu203.myfirstapplication.View;

import androidx.lifecycle.MutableLiveData;

import edu.vassar.cmpu203.myfirstapplication.Controller.State;
import edu.vassar.cmpu203.myfirstapplication.Model.BestRoute;
import edu.vassar.cmpu203.myfirstapplication.Model.Destination;
import edu.vassar.cmpu203.myfirstapplication.Model.Station;
import edu.vassar.cmpu203.myfirstapplication.Model.StationDetails;

import java.util.List;
import java.util.function.Consumer;

public interface UIDelegate {
    /**
     * Show to the user that the initial destination is still loading.
     * @param isGeocoding: Whether we're waiting for location services (`false`) or
     *                   geocoding (`true`) to complete.
     */
    void showInitialDestLoading(boolean isGeocoding);

    /**
     * Show the initial destination to the user.
     */
    void showInitialDestination(Destination initialDestination);

    /**
     * Get the user to type in their initial location.
     * Note: Should call `onEnter` **only** once and then disable the text field.
     */
    void requestInitialLocationText(Consumer<String> onEnter);

    /**
     * This is called after `requestInitialLocationText` when the user clicks enter.
     * We should still show the text they typed but **disable** the text input.
     * We need to lock the text input because geocoding is in progress.
     */
    void showInitialLocationText(String initialLocation);

    /**
     * Let the user know that geocoding failed for the `initialDestination` input field.
     */
    void showFailureToGeocodeInitial();
    /**
     * Let the user know that geocoding failed for the `finalDestination` input field.
     */
    void showFailureToGeocodeFinal();

    /**
     * Show nearby stations on a map in the form of pins. Call `onStationSelection`
     * with the respective station when the user clicks one such pin.
     */
    void showNearbyStations(Destination initialDestination, List<Station> nearbyStations, Consumer<Station> onStationSelection);

    /**
     * Show that the details of the selected station are loading in a new screen.
     * Call `onCancel` when the user exits that screen.
     */
    void showLoadingStationDetails(Station selectedStation, Runnable onCancel);

    /**
     * Show the details of the selected station in a new screen.
     * Call `onCancel` when the user exits that screen.
     * Note: You can assume that this is called after `showLoadingStationDetails`.
     */
    void showStationDetails(StationDetails stationDetails, Runnable onCancel);

    /**
     * Let the user know that we couldn't find the details for the selected station.
     * Note: You can assume that this is called after `showLoadingStationDetails`.
     */
    void showFailureToGetStationDetails(String error);

    /**
     * Get the user to type in their final location.
     * Note: Should call `onEnter` **only** once and then disable the text field.
     */
    void requestFinalLocationText(Consumer<String> onEnter);

    /**
     * This is called after `requestFinalLocationText` when the user clicks enter.
     * We should still show the text they typed but **disable** the text input.
     * We need to lock the text input because geocoding is in progress.
     */
    void showFinalLocationText(String finalLocation);

    /**
     * Show the final destination to the user.
     * Keep the text input editable so that the user can go back and change the final destination.
     */
    void showFinalDestination(Destination initialDestination, Destination finalDestination, Consumer<String> onTextUpdate);

    /**
     * Adjusts the zoom level and expected center of the map widget to fit the
     * initial and final destination pins.
     * @param initialDestination: The initial destination.
     * @param finalDestination: The final destination.
     */
    void resetBoundingBox(Destination initialDestination, Destination finalDestination);

    /**
     * Since we now have a start and final destination, we can compute a route.
     * So activate the `Go` button so the user can start a route with the current
     * `initial` and `final` location.
     */
    void activateRouteButton(Runnable onStartRoute);

    /**
     * Show that we're loading routes in a new screen. This is called after `activateRouteButton`
     * Call `onCancel` when the user exits that screen.
     */
    void showRoutesLoading(MutableLiveData<State> state, Runnable onCancel);

    /**
     * Show the best routes.
     * Call `onCancel` when the user exits that screen.
     */
    void showRoutes(List<BestRoute> bestRoutes, Runnable onCancel);

//    void showFavorites(Runnable onCancel);
}
