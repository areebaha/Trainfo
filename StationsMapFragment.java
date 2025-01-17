package edu.vassar.cmpu203.myfirstapplication.View;

import static java.lang.Double.max;
import static java.lang.Double.min;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.material.icons.Icons;
import androidx.fragment.app.Fragment;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.List;
import java.util.function.Consumer;

import edu.vassar.cmpu203.myfirstapplication.Controller.BackButtonHandler;
import edu.vassar.cmpu203.myfirstapplication.Controller.Controller;
import edu.vassar.cmpu203.myfirstapplication.Controller.StateEvent;
import edu.vassar.cmpu203.myfirstapplication.MainActivity;
import edu.vassar.cmpu203.myfirstapplication.Model.Coordinates;
import edu.vassar.cmpu203.myfirstapplication.Model.Destination;
import edu.vassar.cmpu203.myfirstapplication.Model.Station;
import edu.vassar.cmpu203.myfirstapplication.Model.StationDetails;
import edu.vassar.cmpu203.myfirstapplication.databinding.FragmentStationsMapBinding;
//import edu.vassar.cmpu203.myfirstapplication.Controller.StationFinder;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import edu.vassar.cmpu203.myfirstapplication.R;

/**
 * A Fragment subclass that implements the first screen, StationsMapFragment.
 */
public class StationsMapFragment extends Fragment implements IStationsMapView, java.io.Serializable {
    private @NonNull FragmentStationsMapBinding binding;
    private MapView map = null;
    private EditText startDestInput = null;
    private EditText finalDestInput = null;
    private Button goButton = null;
    private Button viewFavoritesButton = null;
    private Button nextButton = null;
    private AutoCompleteTextView autoCompleteTextView = null;
    private Context ctx;
    private final Controller controller;
    private ViewStationDetailsFragment viewStationDetailsFragment;
    private ViewFavoritesFragment viewFavoritesFragment;
    Listener listener;
    BackButtonHandler handler;
    private double savedZoom = 0;
    private GeoPoint savedCenter = null;

    private GeoPoint initialDestColor = null;

    private GeoPoint finalDestColor = null;

    /**
     * Constructor for StationsMapFragment. Initializes ctx and controller.
     *
     * @param ctx
     * @param controller
     */
    public StationsMapFragment(Context ctx, Controller controller, BackButtonHandler handler) {
        // Required empty public constructor
        this.ctx = ctx;
        this.controller = controller;
        this.handler = handler;
    }

    /**
     * When StationsMapFragment is called, onCreateView initializes a map and sets the location to
     * NYC. All fragment inputs are initialized and controller state is updated.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment,
     * @param container          parent view that the fragments can switch between.
     * @param savedInstanceState fragment is re-constructed
     *                           from a previous saved state as given here.
     * @return View
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentStationsMapBinding.inflate(inflater);
        map = binding.map;
        setUpMapWidget(map);

        // Set up the `startDestInput` text field.
        startDestInput = binding.startDestination;
        // Disable at the beginning until we load the start location.
        startDestInput.setEnabled(false);
        // Make sure to clear the focus when the user hits done.
        startDestInput.setOnEditorActionListener((v, actionId, event) -> {
            // Check for the IME action or the Enter key press
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {

                // Clear focus from the EditText
                startDestInput.clearFocus();

                return true;
            }
            return false;
        });
        // Set up the `finalDestInput` text field.
        finalDestInput = binding.finalDestination;
        // Disable at the beginning until we get the initial location.
        // Then we'll move onto the final location.
        finalDestInput.setEnabled(false);
        // Make sure to clear the focus when the user hits done.
        finalDestInput.setOnEditorActionListener((v, actionId, event) -> {
            // Check for the IME action or the Enter key press
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {

                // Clear focus from the EditText
                finalDestInput.clearFocus();

                return true;
            }
            return false;
        });

        // go button
        this.goButton = binding.goButton;
        goButton.setEnabled(false);

        // favorites button, on click takes user to view favorites fragment
        this.viewFavoritesButton = binding.favoritesButton;
        viewFavoritesButton.setEnabled(false);
        // viewFavoritesButton.setOnClickListener(v -> listener.showFavorites());

        // next button
        this.nextButton = binding.nextButton;
        nextButton.setEnabled(false);

        // Tell controller to start the app if it's uninitialized.
        if (controller.stateIsUninitialized()) {
            controller.updateState(new StateEvent.AppStarted());
        } else {
            handler.handleBack();
        }

        return binding.getRoot();
    }

    /**
     * Sets up the map widget. Sets center to Empire State Building.
     *
     * @param map
     */
    private static void setUpMapWidget(MapView map) {
        // Configure the MapView
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setHorizontalMapRepetitionEnabled(false);
        map.setVerticalMapRepetitionEnabled(false);
        map.setZoomLevel(15);
        map.setMinZoomLevel(2.0);
        map.setScrollableAreaLimitLatitude(MapView.getTileSystem().getMaxLatitude(), MapView.getTileSystem().getMinLatitude(), 0);
        // TODO: Change this to set the expected center at the current location geopoint
        GeoPoint empireStateBuildingLoc = new GeoPoint(40.748438, -73.985687);
        map.setExpectedCenter(empireStateBuildingLoc);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        // This will refresh the osmdroid configuration on resuming.
        if (savedCenter != null && savedZoom > 0) {
            map.getController().setCenter(savedCenter);
            map.getController().setZoom(savedZoom);
        }
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        // This will refresh the osmdroid configuration on resuming.
        savedZoom = map.getZoomLevelDouble();
        savedCenter = (GeoPoint) map.getMapCenter();
        map.onPause();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        map.onDetach();
    }

    public Marker addMarker(@DrawableRes int drawable, String name, Coordinates coords, Context context, MapView map) {
        Marker marker = new Marker(map);
        marker.setTitle(name);
        marker.setPosition(new GeoPoint(coords.getLatitude(), coords.getLongitude()));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        Drawable markerIcon = ContextCompat.getDrawable(context, drawable);
        if (markerIcon != null) {
            markerIcon = DrawableCompat.wrap(markerIcon);
//            DrawableCompat.setTint(markerIcon, ContextCompat.getColor(context, color));
            marker.setIcon(markerIcon);
        }

        map.getOverlays().add(marker);
        map.invalidate();

        return marker;
    }

    // The following are methods for UIDelegate
    public void showInitialDestLoading(boolean isGeocoding) {
        if (!isGeocoding) {
            startDestInput.setText(R.string.loadingCurrentLocation);
        } else {
            startDestInput.setText(R.string.lookingUpCurrentLocation);
        }
    }

    public void requestInitialLocationText(Consumer<String> onEnter) {
        startDestInput.setHint(R.string.currentLocationHint);
        startDestInput.setEnabled(true);
        startDestInput.setText("");
        nextButton.setEnabled(true);

        // Call `onEnter` when the user submits their answer
        View.OnFocusChangeListener listener = (textView, hasFocus) -> {
            if (!hasFocus) {
                // Remove listener after first call
                textView.setOnFocusChangeListener(null);
                // Call consumer
                EditText editTextView = (EditText) textView;
                String inputText = editTextView.getText().toString();
                onEnter.accept(inputText);
            }
        };
        startDestInput.setOnFocusChangeListener(listener);
        // When user submits their answer, they should be able to click the next Button.
        nextButton.setOnClickListener(v -> {
            // Call consumer
            EditText textView = startDestInput;
            EditText editTextView = (EditText) textView;
            String inputText = editTextView.getText().toString();
            showInitialLocationText(inputText);
            nextButton.setEnabled(false);
        });
    }

    public void requestFinalLocationText(Consumer<String> onEnter) {
        finalDestInput.setHint(R.string.finalLocationHint);
        finalDestInput.setEnabled(true);
        finalDestInput.setText("");
        nextButton.setEnabled(true);

        // Call `onEnter` when the user submits their answer
        View.OnFocusChangeListener listener = (textView, hasFocus) -> {
            if (!hasFocus) {
                // Remove listener after first call
                textView.setOnFocusChangeListener(null);
                // Call consumer
                EditText editTextView = (EditText) textView;
                String inputText = editTextView.getText().toString();
                onEnter.accept(inputText);
            }
        };
        finalDestInput.setOnFocusChangeListener(listener);
        // After the user enters their final location, they should be able to click the next Button.
        nextButton.setOnClickListener(v -> {
            // Call consumer
            EditText textView = finalDestInput;
            EditText editTextView = (EditText) textView;
            String inputText = editTextView.getText().toString();
            showFinalLocationText(inputText);
            nextButton.setEnabled(false);
        });

//        // Initialize the AutoCompleteTextView
//        autoCompleteTextView = binding.autoCompleteTextView;
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
//                android.R.layout.simple_dropdown_item_1line, locations);
//        autoCompleteTextView.setAdapter(adapter);
//
//        // Set an item click listener for the AutoCompleteTextView
//        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
//            String selectedLocation = adapter.getItem(position);
//            if (selectedLocation != null) {
//                // Here you can use a geocoding service to get the coordinates
//                // For simplicity, we will use hardcoded coordinates for demonstration
//                GeoPoint geoPoint = getGeoPointForLocation(selectedLocation);
//                if (geoPoint != null) {
//                    mapView.getController().setZoom(10);
//                    mapView.getController().setCenter(geoPoint);
//                }
//            }
//        });
    }

    public void showInitialLocationText(String initialLocation) {
        startDestInput.setText(initialLocation);
        startDestInput.setEnabled(false);
    }

    public void showFinalLocationText(String finalLocation) {
        finalDestInput.setText(finalLocation);
        finalDestInput.setEnabled(false);
        nextButton.setEnabled(false);
        goButton.setEnabled(false);
    }

    public void showInitialDestination(Destination initialDestination) {
        startDestInput.setText(initialDestination.getName());
        // Recenter map and add an marker overlay on the map
        GeoPoint initialLoc = new GeoPoint(
                initialDestination.getCoords().getLatitude(),
                initialDestination.getCoords().getLongitude());
        map.setExpectedCenter(initialLoc);

        // Add marker overlay
        map.getOverlays().clear();
        addMarker(R.drawable.initial_destination_pin, initialDestination.getName(),
                initialDestination.getCoords(),
                ctx, map);
        map.invalidate();
    }

    public void showFinalDestination(Destination initialDestination, Destination finalDestination, Consumer<String> onTextUpdate) {
        finalDestInput.setText(finalDestination.getName());
        finalDestInput.setEnabled(true);

        // Add marker overlay and recenter map to show both
        GeoPoint initialLoc = new GeoPoint(
                initialDestination.getCoords().getLatitude(),
                initialDestination.getCoords().getLongitude());
        GeoPoint finalLoc = new GeoPoint(
                finalDestination.getCoords().getLatitude(),
                finalDestination.getCoords().getLongitude());
        GeoPoint center = new GeoPoint((initialLoc.getLatitude() + finalLoc.getLatitude()) / 2, (initialLoc.getLongitude() + finalLoc.getLongitude()) / 2);

        map.getOverlays().clear();

        // Change color of current and final locations
        addMarker(R.drawable.initial_destination_pin,
                initialDestination.getName(), initialDestination.getCoords(), ctx, map);

        addMarker(R.drawable.final_destination_pin,
                finalDestination.getName(), finalDestination.getCoords(), ctx, map);

        map.setExpectedCenter(center);

        // Refresh the map.
        map.invalidate();

        // Call `onTextUpdate` if the user changes their answer
        View.OnFocusChangeListener listener = (textView, hasFocus) -> {
            if (!hasFocus) {
                // Remove listener after first call
                textView.setOnFocusChangeListener(null);
                // Call consumer
                EditText editTextView = (EditText) textView;
                String inputText = editTextView.getText().toString();
                onTextUpdate.accept(inputText);
            }
        };
        finalDestInput.setOnFocusChangeListener(listener);
    }

    public void resetBoundingBox(Destination initialDestination, Destination finalDestination) {
        GeoPoint initialLoc = new GeoPoint(
                initialDestination.getCoords().getLatitude(),
                initialDestination.getCoords().getLongitude());
        GeoPoint finalLoc = new GeoPoint(
                finalDestination.getCoords().getLatitude(),
                finalDestination.getCoords().getLongitude());
        // Construct the bounding box
        // Readjust the zoom and center
        // Longitude is associated with west/east (west has smaller coord values;
        // east has larger coord values)
        double west = Math.min(initialLoc.getLongitude(), finalLoc.getLongitude());
        double east = Math.max(initialLoc.getLongitude(), finalLoc.getLongitude());
        // Then get the 0.1 difference to use as a margin. This way,
        // the bounding box will not reach the end of the screen but will
        // instead have a margin of 10% of the map in all directions.
        double longitudeMargin = (east - west) * 0.1;
        west -= longitudeMargin;
        east += longitudeMargin;

        // Latitude is associated with south/north (south has smaller coord values;
        // north has larger coord values)
        double south = Math.min(initialLoc.getLatitude(), finalLoc.getLatitude());
        double north = Math.max(initialLoc.getLatitude(), finalLoc.getLatitude());
        // Then get the 0.1 difference to use as a margin, similar to above.
        double latitudeMargin = (north - south) * 0.1;
        south -= latitudeMargin;
        north += latitudeMargin;

        BoundingBox boundingBox = new BoundingBox(north, east, south, west);
        // Zoom to the bounding box but without reaching a zoom level higher than 18.
        map.setMaxZoomLevel(18.0);
        // Zoom & center on the bounding box using an animation.
        map.zoomToBoundingBox(boundingBox, true);
        // Then restore the zoom level so the user can zoom in further if they'd like
        map.setMaxZoomLevel(29.0);
    }

    public void showFailureToGeocodeInitial() {
        System.out.println("Geocoding for initial location failed");
        new AlertDialog.Builder(ctx)
                .setTitle("Problem finding Location")
                .setMessage("Couldn't look up start location. Please try again.")
                .setPositiveButton("OK", (dialog, which) -> {
                    // OK button action
                })
                .show();
    }

    public void showFailureToGeocodeFinal() {
        System.out.println("Geocoding for final location failed");
        new AlertDialog.Builder(ctx)
                .setTitle("Problem finding location")
                .setMessage("Couldn't look up final location. Please try again.")
                .setPositiveButton("OK", (dialog, which) -> {
                    // OK button action
                })
                .show();
    }

    public void showNearbyStations(Destination initialDestination, List<Station> nearbyStations, Consumer<Station> onStationSelection) {
        map.getOverlays().clear();

        GeoPoint initialLocation = new GeoPoint(
                initialDestination.getCoords().getLatitude(),
                initialDestination.getCoords().getLongitude());
        addMarker(R.drawable.initial_destination_pin,
                initialDestination.getName(), initialDestination.getCoords(), ctx, map);
        map.setExpectedCenter(initialLocation);

        for (Station station : nearbyStations) {
            // Guide for marker: https://github.com/MKergall/osmbonuspack/wiki/Tutorial_0
            // Set a marker for each station.
            // We also change the icon based on accessibility.
            Marker stationMarker = addMarker(
                    station.isAccessible() ? R.drawable.accessible_station_pin : R.drawable.station_pin,
                    station.getName(), station.getCoords(), ctx, map);

            // Add listener
            Marker.OnMarkerClickListener listener = (marker, mapView) -> {
                System.out.println("Trying to expand station details for " + station.getName() + station.getCoords());
                onStationSelection.accept(station);
                return true;
            };

            stationMarker.setOnMarkerClickListener(listener);
        }
        // To update map
        map.invalidate();
    }

    public void activateRouteButton(Runnable onStartRoute) {
        goButton.setEnabled(true);

        View.OnClickListener listener = view -> {
            // Remove listener after one click action
            view.setOnClickListener(null);
            // Call consumer
            onStartRoute.run();
        };
        goButton.setOnClickListener(listener);
    }
}

