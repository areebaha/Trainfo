package edu.vassar.cmpu203.myfirstapplication.View;

public interface IStationsMapView {
    /**
     * Interface that classes interested in being notified of events happening
     * to the view should implement.
     */
    interface Listener{
        /**
         * Called when user clicks on the favorites button.
         */
        void showFavorites();
//        void activateRouteButton();
    }
}
