package edu.vassar.cmpu203.myfirstapplication.View;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import edu.vassar.cmpu203.myfirstapplication.databinding.MainBinding;

/**
 * The main view for the app. Implements IMainView
 */
public class MainView implements IMainView {
    private MainBinding binding;
    private FragmentManager fmanager;

    /**
     * Constructor for the main view. Initializes the binding and the fragment manager.
     * @param context
     * @param factivity
     */
    public MainView(final Context context, final FragmentActivity factivity) {
        this.binding = MainBinding.inflate(LayoutInflater.from(context));

        // configure app to maximize space usage by drawing of top of system bars
        EdgeToEdge.enable(factivity);
        ViewCompat.setOnApplyWindowInsetsListener(this.binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        this.binding = MainBinding.inflate(factivity.getLayoutInflater());
        this.fmanager = factivity.getSupportFragmentManager();
    }

    /**
     * Get the root view of the main view.
     * @return View
     */
    @Override
    public View getRootView() {
        return this.binding.getRoot();
    }

    /**
     * Display a fragment in the main view.
     * @param fragment
     */
    @Override
    public void displayFragment(@NonNull Fragment fragment) { this.displayFragment(fragment,null);}

    /**
     * Display a fragment in the main view.
     * @param fragment
     * @param transName
     */
    @Override
    public void displayFragment(@NonNull Fragment fragment, String transName) {
        FragmentTransaction ft = fmanager.beginTransaction();
        ft.replace(this.binding.fragmentContainerView.getId(), fragment);
        if (transName != null) {ft.addToBackStack(transName);}
        ft.commit(); // execute transaction

    }
}
