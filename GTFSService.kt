package edu.vassar.cmpu203.myfirstapplication.Controller

import android.content.Context
import edu.vassar.cmpu203.myfirstapplication.Model.GTFSData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer

/**
 * GTFSLoader loads all GTFS data required by our application asynchronously
 * and keeps tracks of gtfs-data requests while we're still loading the data.
 */
class GTFSService(private val scope: CoroutineScope, context: Context) {
    internal sealed class State {
        /**
         * The gtfs data has not yet been loaded.
         * We're keeping track of requests as callbacks that we'll invoke when the data is loaded.
         */
        data class Uninitialized(val callbacks: ArrayList<Consumer<GTFSData>>) : State()

        /**
         * The gtfs data has been loaded.
         */
        data class Loaded(val data: GTFSData) : State()
    }

    private var state: State = State.Uninitialized(ArrayList())
    private val stateLock = ReentrantLock()

    init {
        load(context)
    }

    fun getGTFSData(onSuccess: Consumer<GTFSData>) {
        // Acquiring a lock, either invoke the callback immediately if we're already loaded,
        // or add the callback to the list of callbacks if uninitialized.
        stateLock.lock()
        when (val state = state) {
            is State.Uninitialized -> {
                // Add the callback to the list of callbacks.
                state.callbacks.add(onSuccess)
            }
            is State.Loaded -> {
                // Invoke the callback immediately.
                onSuccess.accept(state.data)
            }
        }
        // Release the lock.
        stateLock.unlock()
    }

    /**
     * The main loading function. Can be called on any thread.
     * Loads all GTFS data required by our application.
     * @param context: The application context used to open the resources containing the GTFS data.
     */
    private fun load(context: Context) {
        scope.launch {
            try {
                // Load the gtfs data in the background
                val gtfsData = withContext(Dispatchers.IO) {
                    GTFSLoaderSync.loadSync(context)
                }
                println("Finished loading gtfs data!")

                // Acquire the lock, invoke the callbacks and save the new state, then release
                // the lock.
                stateLock.lock()
                // Invoke the previously stored callbacks.
                when (val state = state) {
                    is State.Uninitialized -> {
                        // Invoke the callbacks that we stored when the data hadn't yet loaded.
                        state.callbacks.forEach { it.accept(gtfsData) }
                    }
                    is State.Loaded -> {
                        // We're already loaded, but the load function should have only been called
                        // once.
                        throw IllegalStateException ("Unexpectedly loaded gtfs data twice.")
                    }
                }
                // Save the new state.
                state = State.Loaded(gtfsData)
                stateLock.unlock()
            } catch (e: Exception) {
                println("Loading gtfs data failed.")
                throw e
            }
        }
    }
}
