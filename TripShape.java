package edu.vassar.cmpu203.myfirstapplication.Model;

import java.util.ArrayList;

/** A shape associated with a specific trip. Corresponds to
* a GTFS "shape."
* E.g. A bus going inbound (which is different from an outbound trip).
 */
public class TripShape {
    private final String id;
    private final ArrayList<Coordinates> points;

    /**
     * Constructor for TripShape.
     * @param id
     * @param points
     */
    public TripShape(String id, ArrayList<Coordinates> points) {
        this.id = id;
        this.points = points;
    }

    public String getId() {
        return id;
    }

    public ArrayList<Coordinates> getPoints() {
        return points;
    }

    /**
     * Add a point to the shape.
     * @param point
     * @param index
     */
    public void addPoint(Coordinates point, int index) {
        if (index != points.size()) {
            throw new IndexOutOfBoundsException();
        }
        this.points.add(point);
    }
}
