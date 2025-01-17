package edu.vassar.cmpu203.myfirstapplication.View;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import edu.vassar.cmpu203.myfirstapplication.Model.Station;
import edu.vassar.cmpu203.myfirstapplication.Model.StationDetails;
import edu.vassar.cmpu203.myfirstapplication.Model.TransitRoute;
import edu.vassar.cmpu203.myfirstapplication.R;
import edu.vassar.cmpu203.myfirstapplication.databinding.FragmentStationsMapBinding;
import edu.vassar.cmpu203.myfirstapplication.databinding.FragmentViewFavoritesBinding;
import edu.vassar.cmpu203.myfirstapplication.databinding.FragmentViewStationDetailsBinding;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ViewStationDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ViewStationDetailsFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
   FragmentViewStationDetailsBinding binding;
   Context ctx;
   Station station;

    public ViewStationDetailsFragment(Context ctx, Station station) {
        this.ctx = ctx;
        this.station = station;
    }


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ViewStationDetailsFragment.
     */
    public ViewStationDetailsFragment newInstance() {
        ViewStationDetailsFragment fragment = new ViewStationDetailsFragment(ctx, station);
        return fragment;
    }

    /**
     * Takes in loaded station details and displays them in the view.
     * @param stationDetails
     */
    public void showStationDetails(StationDetails stationDetails) {
        // Gather loaded data
        List<TransitRoute> transitRoutes = stationDetails.getRecursiveRoutes();

        // Join route.displayName with commas and spaces.
        String routesString = transitRoutes.stream()
                .map(TransitRoute::getDisplayName)
                .collect(Collectors.joining(", "));


        // Remove loading indicator
        binding.loadingText.setVisibility(View.GONE);
        binding.loadingBar.setVisibility(View.GONE);

        // Populate data
        binding.stationLines.setText(getString(R.string.stationLinesLabel, routesString));
        binding.stationLines.setVisibility(View.VISIBLE);
    }

    /**
     * Displays Failure in Builder if we couldn't load station details.
     * @param error
     */
    public void showFailureToGetStationDetails(String error) {
        System.out.println("Getting station details for final location failed: " + error);
        new AlertDialog.Builder(ctx)
                .setTitle("Problem loading station details")
                .setMessage("Couldn't look up station details: " + error)
                .setPositiveButton("OK", (dialog, which) -> {
                    // OK button action
                })
                .show();
    }

    /**
     * onCreateView updates the view with the station details.
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return the view for view station details fragment with station name, station location,
     * and accessibility.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentViewStationDetailsBinding.inflate(inflater);
        //binding.coordinates.setText(this.station.getCoords().toString());
        binding.stationLocation.setText(this.station.getBorough());
        //binding.stationLines.setText(this.station.getLines());
        binding.accessible.setText(this.station.isAccessibleString());
//        getString(R.string.accessibleLabel,
        if (Objects.equals(this.station.isAccessibleString(), "Accessible")) {
            binding.accessible.setTextColor(getResources().getColor(R.color.green));
        } else if (Objects.equals(this.station.isAccessibleString(), "Partially Accessible: ")) {
            {
                String accessibleText = this.station.isAccessibleString() + this.station.accessibilityNote;
                binding.accessible.setText(accessibleText);
                binding.accessible.setTextColor(getResources().getColor(R.color.yellow));
            }
            } else {
            binding.accessible.setTextColor(getResources().getColor(R.color.red));
        }
        binding.stationName.setText(this.station.getName());
        // Inflate the layout for this fragment
        return binding.getRoot();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}