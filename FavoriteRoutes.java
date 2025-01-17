package edu.vassar.cmpu203.myfirstapplication.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that represents a list of favorite routes.
 */
public class FavoriteRoutes {
    private final List<BestRoute> bestRoute;
    private final List<BestRoute> favoriteRoutes;

    /**
     * Constructor for FavoriteRoutes.
     * @param bestRoute
     * @param favoriteRoutes
     */
    public FavoriteRoutes(List<BestRoute> bestRoute, List<BestRoute> favoriteRoutes) {
        this.bestRoute = bestRoute;
        this.favoriteRoutes = new ArrayList<>();
    }

    /**
     * Returns the best route.
     * @return
     */
    public List<BestRoute> getBestRoute() {
        return bestRoute;
    }

    /**
     * Returns the list of favorite routes.
     * @return
     */
    public List<BestRoute> getFavoriteRoutes() {
        return favoriteRoutes;
    }

    /**
     * Add a route to the list of favorite routes.
     * @param route
     */
    public void addToFavorites(BestRoute route) {
        favoriteRoutes.add(route);
    }

    /**
     * Delete a route from the list of favorite routes.
     * @param route
     */
    public void deleteFromFavorites(BestRoute route) {
        favoriteRoutes.remove(route);
    }

    /**
     * Returns a list of favorite routes.
     * @param bestRoute
     * @return
     */
    public String toString(List<BestRoute> bestRoute) {
        String retString = "My Favorite Routes:\n";
        for (BestRoute route : favoriteRoutes) {
            retString += route.toString() + "\n";
        }
        return retString;
    }
}
