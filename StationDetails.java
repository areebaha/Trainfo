package edu.vassar.cmpu203.myfirstapplication.Model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A station provides information about a specific station/stop, that is
 * a concrete location from which public transportation services pick up
 * or drop off passengers.
 * This corresponds to what the GTFS specification describes as a "stop."
 * It contains as an id, a human-readable name, the type (e.g. subway, other).
 * Unlike a GTFS stop, a stop is associated with a set of `RouteTrip`s organized
 * by a `Model.TransitRoute`.
 */
public class StationDetails {
    private final String id;
    private final String name;
    private final Coordinates coords;
    private final Set<TransitRoute> transitRoutes;
    private final String accessible;
    private @Nullable StationDetails parent = null;
    private final ArrayList<StationDetails> children = new ArrayList<>();

    /**
     * Constructor for Station.
     * @param id
     * @param name
     * @param coords
     * @param transitRoutes
     * @param accessible
     */
    public StationDetails(
            String id, String name, Coordinates coords, Set<TransitRoute> transitRoutes,
            String accessible) {
        this.id = id;
        this.name = name;
        this.coords = coords;
        this.transitRoutes = transitRoutes;
        this.accessible = accessible;
    }

    /**
     * Get the id of the station.
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * Get the name of the station.
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Get the coordinates of the station.
     * @return
     */
    public Coordinates getCoords() {
        return coords;
    }

    /**
     * Get the routes of the station.
     * @return
     */
    public Set<TransitRoute> getTransitRoutes() {
        return transitRoutes;
    }

    /**
     * Link this station to its parent station.
     * @param parent
     */
    public void linkParent(StationDetails parent) {
        this.parent = parent;
        parent.children.add(this);
    }
    /**
    * Adds a route to the list of routes that pass through this station.
    * Duplicates ignored.
    */
    public void addRoute(TransitRoute transitRoute) {
        transitRoutes.add(transitRoute);
    }

    /**
     * Get the parent of the station.
     * @return
     */
    @Nullable
    public StationDetails getParent() {
        return parent;
    }

    /**
     * Get the children of the station.
     * @return
     */
    public ArrayList<StationDetails> getChildren() {
        return children;
    }

    /**
     * Get the accessibility of the station.
     * @return
     */
    public String getAccessible() {
        return accessible;
    }

    /**
     * Get all the routes of this station and all its children.
     * @return The recursive routes sorted in alphabetical order.
     */
    public List<TransitRoute> getRecursiveRoutes() {
        // Gather all routes without any duplicates.
        Set<TransitRoute> routes = new HashSet<>(transitRoutes);

        for (StationDetails child : children) {
            routes.addAll(child.getRecursiveRoutes());
        }

        // Sort the routes by alphabetical order
        List<TransitRoute> sortedRoutes = new ArrayList<>(routes);
        sortedRoutes.sort(Comparator.comparing(r -> r.getDisplayName()));

        return sortedRoutes;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StationDetails station = (StationDetails) obj;
        return name.equals(station.name) &&
                coords.equals(station.coords) &&
                id.equals(station.id) &&
                transitRoutes.equals(station.transitRoutes) &&
                accessible.equals(station.accessible) &&
                parent.equals(station.parent) &&
                children.equals(station.children);
    }


    /**
     * @return string representation of station.
     */
    @NonNull
    @Override
    public String toString() {
        return "Station{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", coords=" + coords +
                ", transitRoutes=" + transitRoutes +
                ", accessible='" + accessible + '\'' +
                ", parent=" + (parent==null) +
                ", children=" + children +
                '}';
    }
}
