package edu.vassar.cmpu203.myfirstapplication.View;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * Interface for the main view. Used to display fragments.
 */
public interface IMainView {
    public View getRootView();
    public void displayFragment(@NonNull final Fragment fragment);

    public void displayFragment(@NonNull final Fragment fragment, String transName);
}
