package edu.vassar.cmpu203.myfirstapplication.Controller;

import static android.content.Context.LOCATION_SERVICE;
import static androidx.core.content.ContextCompat.getSystemService;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import edu.vassar.cmpu203.myfirstapplication.Model.Coordinates;

/**
 * Provides access to the user's current location and handle
 * requesting permission and potential problems with acquiring
 * the user's location.
 */
public class LocationServices {
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;

    private final Activity activity;
    private final LocationManager locationManager;

    private final ReentrantLock stateLock = new ReentrantLock();
    private boolean requestedPermission = false;
    private final ArrayList<Consumer<Coordinates>> onSuccessListeners = new ArrayList<>();
    private final ArrayList<Consumer<String>> onFailureListeners = new ArrayList<>();

    /**
     * Before initializing `LocationServices`, we should have requested permission.
     * @param activity: An activity object.
     */
    public LocationServices(Activity activity) {
        this.activity = activity;
        this.locationManager = (LocationManager) activity.getSystemService(LOCATION_SERVICE);

        // Check if we already have the location and thus don't need to wait
        // for the permission results to come back.
        requestedPermission = (ActivityCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED);
    }

    public void onPermissionsResponse() {
        stateLock.lock();
        System.out.println("Permissions response received.");
        // Remember that the user responded to permissions.
        requestedPermission = true;

        // Run all the saved requests now that the user responded to our permission request.
        // Important: We pass copies because we then clear these lists.
        requestLocationImplementation(
                List.copyOf(onSuccessListeners),
                List.copyOf(onFailureListeners));
        // Clear all requests
        onSuccessListeners.clear();
        onFailureListeners.clear();

        stateLock.unlock();
    }

    /**
     * Requests the user's current location.
     * @param onSuccess: A consumer that accepts a Coordinates object.
     * @param onFailure: A consumer that accepts a String.
     */
    public void requestLocation(Consumer<Coordinates> onSuccess, Consumer<String> onFailure) {
        stateLock.lock();
        if (!requestedPermission) {
            // Permissions haven't been requested yet; save the requests to be run when
            // we have permission.
            onSuccessListeners.add(onSuccess);
            onFailureListeners.add(onFailure);
        } else {
            // Since we have permission, run the requests now.
            requestLocationImplementation(List.of(onSuccess), List.of(onFailure));
        }
        stateLock.unlock();
    }

    /**
     * Requests the user's current location.
     * @param onSuccessHandlers: The consumers called that accept a Coordinates object representing
     *                         location.
     * @param onFailureHandlers: The consumers called on failure that accept a String.
     */
    public void requestLocationImplementation(List<Consumer<Coordinates>> onSuccessHandlers,
                                              List<Consumer<String>> onFailureHandlers) {
        // Check if location services are enabled; if not, issue an error.
        if (ActivityCompat.checkSelfPermission(
                    activity,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                    activity,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            System.out.println("Location permission denied");
            onFailureHandlers.forEach(consumer -> consumer.accept("Permission for location services denied."));
            return;
        } else if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            System.out.println("GPS provider is not enabled");
            onFailureHandlers.forEach(consumer -> consumer.accept("GPS provider is not enabled."));
            return;
        }

        locationManager.getCurrentLocation(
                LocationManager.GPS_PROVIDER, null, activity.getMainExecutor(),
                location -> {
                    if (location == null) {
                        System.out.println("Location is null");
                        onFailureHandlers.forEach(consumer -> consumer.accept("Location is null."));
                        return;
                    }
                    onSuccessHandlers.forEach(consumer -> consumer.accept(new Coordinates(
                            location.getLatitude(),
                            location.getLongitude()
                    )));
                });
    }
}
