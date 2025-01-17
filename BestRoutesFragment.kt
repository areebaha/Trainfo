package edu.vassar.cmpu203.myfirstapplication.View

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import edu.vassar.cmpu203.myfirstapplication.Controller.BackButtonHandler
import edu.vassar.cmpu203.myfirstapplication.Controller.Controller
import edu.vassar.cmpu203.myfirstapplication.Controller.State
import edu.vassar.cmpu203.myfirstapplication.Controller.StateEvent.AppStarted

/**
 * Fragment for the Best Routes view.
 * This fragment relies on the Composable views defined in `BestRoutesView.kt`
 */
class BestRoutesFragment(private val liveState: LiveData<State>) : Fragment() {
    private val controller: Controller? = null
    private var handler: BackButtonHandler? = null

    /**
     * The onCreate() method is called when the fragment is first created
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BestRoutesView(liveState)
            }
        }
    }
}