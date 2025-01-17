package edu.vassar.cmpu203.myfirstapplication.Controller;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Keeps track of all the callbacks for the back button. Also includes a
 * `handleBack` method that should be called from MainActivity when the back button is invoked.
 */
public class BackButtonHandler {
    private final CopyOnWriteArrayList<CallbackWrapper> callbacks = new CopyOnWriteArrayList<>();

    public BackButtonHandler() {}

    /**
     * Adds a callback to the list of callbacks.
     * @param callback: A callback that will be invoked when the back button is invoked.
     *                It accepts a Runnable that can be used to cancel the callback.
     */
    public void addCallback(Consumer<Runnable> callback) {
        CallbackWrapper wrapper = new CallbackWrapper(callback);
        callbacks.add(0, wrapper); // Add to front to maintain LIFO order
    }

    /**
     * A method that invokes all stored callbacks.
     * Should be called by MainActivity when the back button is invoked.
     */
    public void handleBack() {
        // Find the first non-cancelled callback
        for (CallbackWrapper wrapper : callbacks) {
            if (!wrapper.isCancelled) {
                // Create a cancellation runnable
                Runnable cancellationRunnable = () -> wrapper.isCancelled = true;

                // Invoke the callback with the cancellation runnable
                wrapper.callback.accept(cancellationRunnable);
                return;
            }
        }
    }

    /**
     * Clears all stored callbacks.
     */
    public void clearCallbacks() {
        callbacks.clear();
    }

    private static class CallbackWrapper {
        final Consumer<Runnable> callback;
        volatile boolean isCancelled;

        CallbackWrapper(Consumer<Runnable> callback) {
            this.callback = callback;
            this.isCancelled = false;
        }
    }
}
