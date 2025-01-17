package edu.vassar.cmpu203.myfirstapplication;

//import static androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.Table.map;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.MutableLiveData;

import org.osmdroid.config.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

//import edu.vassar.cmpu203.myfirstapplication.Controller.BackButtonHandler;
import edu.vassar.cmpu203.myfirstapplication.Controller.BackButtonHandler;
import edu.vassar.cmpu203.myfirstapplication.Controller.Controller;
import edu.vassar.cmpu203.myfirstapplication.Controller.State;
import edu.vassar.cmpu203.myfirstapplication.Model.BestRoute;
import edu.vassar.cmpu203.myfirstapplication.Model.Destination;
import edu.vassar.cmpu203.myfirstapplication.Model.Station;
import edu.vassar.cmpu203.myfirstapplication.Model.StationDetails;
import edu.vassar.cmpu203.myfirstapplication.View.BestRoutesFragment;
import edu.vassar.cmpu203.myfirstapplication.View.IStationsMapView;
import edu.vassar.cmpu203.myfirstapplication.View.MainView;
import edu.vassar.cmpu203.myfirstapplication.View.StationsMapFragment;
import edu.vassar.cmpu203.myfirstapplication.View.UIDelegate;
import edu.vassar.cmpu203.myfirstapplication.View.ViewFavoritesFragment;
import edu.vassar.cmpu203.myfirstapplication.View.ViewStationDetailsFragment;

/**
 * MainActivity is in charge of switching between the different fragments and calling to the controller.
 * It acts as a controller for the fragments, while our controller class updates states within the fragment.
 */
public class MainActivity extends AppCompatActivity implements UIDelegate {
        /*,
        implements IFavoritesView.Listener, IStationsMapView.Listener, IStationDetailsVew.Listener
         {
         */
    private FragmentManager fmanager;
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;

    private Controller controller;

    private MainView mainView;

    private StationsMapFragment stationsMapFragment;
    private ViewStationDetailsFragment  stationDetailsFragment;
    private ViewStationDetailsFragment viewStationDetailsFragment;

    BackButtonHandler backButtonHandler = new BackButtonHandler();

    /**
     * Constructor for MainActivity. Takes in a savedInstanceState and checks the necessary permissions
     * for the OSMDroid. It sets the first fragment as the mainView which contains the fragment container,
     * and then switches to StationsMapFragment. It also handles the backButton.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fmanager = getSupportFragmentManager();
        // Get permissions necessary for OSMDroid
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        requestPermissionsIfNecessary(new String[]{
                // WRITE_EXTERNAL_STORAGE is required in order to show the map
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                // We need fine/coarse location to get the initial destination
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
        });

        // Create main screen template
        this.mainView = new MainView(this, this);
        setContentView(this.mainView.getRootView()); // must be called from controller

        // Setup controller and delegate to manage our state.
        controller = new Controller(this);

        // display first screen
        this.stationsMapFragment = new StationsMapFragment(this, controller, backButtonHandler);
        this.mainView.displayFragment(stationsMapFragment, null);
    }

    @Override
    public void onBackPressed() {
        if (fmanager.getBackStackEntryCount() > 0) {
            backButtonHandler.handleBack();
        } else {
            // No fragments in the back stack, perform the default back action
            super.onBackPressed();
        }
    }

    /**
     * Requests the necessary permissions for the OSMDroid.
     * @param permissions
     */
    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            permissionsToRequest.add(permission);
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    // Convert list to array
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Handles the result of the permissions request.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            controller.onPermissionsResponse();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Run the given runnable on the next view update.
     * This is used to allow views to be created before calling methods on them.
     */
    private static void runOnNextUpdate(Runnable runnable){
        final Handler UIHandler = new Handler(Looper.getMainLooper());
        UIHandler.post(runnable);
    }

    // The following are methods for UIDelegate. They call methods on the Stations Map fragment.
    @Override
    public void showInitialDestLoading(boolean isGeocoding) {
        stationsMapFragment.showInitialDestLoading(isGeocoding);
    }

    @Override
    public void requestInitialLocationText(Consumer<String> onEnter) {
        stationsMapFragment.requestInitialLocationText(onEnter);
    }

    @Override
    public void requestFinalLocationText(Consumer<String> onEnter) {
        stationsMapFragment.requestFinalLocationText(onEnter);
    }

    @Override
    public void showInitialLocationText(String initialLocation) {
        stationsMapFragment.showInitialLocationText(initialLocation);
    }

    @Override
    public void showFinalLocationText(String finalLocation) {
        stationsMapFragment.showFinalLocationText(finalLocation);
    }

    @Override
    public void showInitialDestination(Destination initialDestination) {
        stationsMapFragment.showInitialDestination(initialDestination);
    }

    @Override
    public void showFinalDestination(Destination initialDestination, Destination finalDestination, Consumer<String> onTextUpdate) {
        stationsMapFragment.showFinalDestination(initialDestination, finalDestination, onTextUpdate);
    }

    @Override
    public void resetBoundingBox(Destination initialDestination, Destination finalDestination) {
        stationsMapFragment.resetBoundingBox(initialDestination, finalDestination);
    }
    @Override
    public void showFailureToGeocodeInitial() {
        stationsMapFragment.showFailureToGeocodeInitial();
    }

    @Override
    public void showFailureToGeocodeFinal() {
        stationsMapFragment.showFailureToGeocodeFinal();
    }

    @Override
    public void showNearbyStations(Destination initialDestination, List<Station> nearbyStations, Consumer<Station> onStationSelection) {
        stationsMapFragment.showNearbyStations(initialDestination, nearbyStations, onStationSelection);
    }

    /**
     * showLoadingStationDetails is our first method for the view station details fragment.
     * Shows the basic station details in the ViewStationDetailsFragment like the station name,
     * location, and accessibility.
     * @param selectedStation
     * @param onCancel
     */
    @Override
    public void showLoadingStationDetails(Station selectedStation, Runnable onCancel) {
        this.stationDetailsFragment = new ViewStationDetailsFragment(this, selectedStation);
        this.mainView.displayFragment(stationDetailsFragment, "stationDetails");

        runOnNextUpdate(() -> {
            backButtonHandler.addCallback(cancelButtonCallback -> {
                backButtonHandler.clearCallbacks();

                // There are fragments in the back stack, pop the last one
                fmanager.popBackStack();

                runOnNextUpdate(() -> {
                    System.out.println("Calling cancel");
                    onCancel.run();
                });
            });
        });
    }

    /**
     * Method that uses our GTFS data, which takes more time to load. After showStationDetails is
     * run, showLoadingStationDetails displays the extra details like the station Lines.
     * @param stationDetails
     * @param onCancel
     */
    @Override
    public void showStationDetails(StationDetails stationDetails, Runnable onCancel) {
        // Allow time for `stationDetailsFragment` to be created before giving it an update.
        runOnNextUpdate(() -> {
            stationDetailsFragment.showStationDetails(stationDetails);
            backButtonHandler.addCallback(cancelButtonCallback -> {
                backButtonHandler.clearCallbacks();

                // There are fragments in the back stack, pop the last one
                fmanager.popBackStack();

                runOnNextUpdate(() -> {
                    System.out.println("Calling cancel");
                    onCancel.run();
                });
            });
        });
    }

    /**
     * Shows the failure to get station details in the ViewStationDetailsFragment.
     * @param error
     */
    @Override
    public void showFailureToGetStationDetails(String error) {
        // Allow time for `stationDetailsFragment` to be created before giving it an update.
        runOnNextUpdate(() -> stationDetailsFragment.showFailureToGetStationDetails(error));
    }


    /**
     * Method to activate the goButton, that will activate our Finding Routes algorithm.
     * @param onStartRoute
     */
    @Override
    public void activateRouteButton(Runnable onStartRoute) {
        stationsMapFragment.activateRouteButton(onStartRoute);
    }

    /**
     * Displays the routes that take longer to load in the StationsMapFragment.
     */
    @Override
    public void showRoutesLoading(MutableLiveData<State> state, Runnable onCancel) {
        BestRoutesFragment bestRoutesFragment = new BestRoutesFragment(state);
        this.mainView.displayFragment(bestRoutesFragment, "routesLoading");

        runOnNextUpdate(() -> {
            backButtonHandler.addCallback(cancelButtonCallback -> {
                backButtonHandler.clearCallbacks();

                // There are fragments in the back stack, pop the last one
                fmanager.popBackStack();

                runOnNextUpdate(() -> {
                    System.out.println("Calling cancel");
                    onCancel.run();
                });
            });
        });
    }

    /**
     * Displays the routes in the StationsMapFragment.
     * @param bestRoutes
     * @param onCancel
     */
    @Override
    public void showRoutes(List<BestRoute> bestRoutes, Runnable onCancel) {
        // TODO: Implement
    }
}