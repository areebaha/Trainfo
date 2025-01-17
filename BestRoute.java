package edu.vassar.cmpu203.myfirstapplication.Model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Set;

/**
 * BestRoutes is a class that represents the route returned from the finding Accessible Routes algorithm.
 * Hasn't been implemented yet but will be used in View Favorites and to return the route to the user.
 */
public class BestRoute implements java.io.Serializable {
    /**
     * A record representing a step in the route. Each step can be a walk step or a transit step.
     * Usually, routes have a combination of walk and transit steps.
     */
    public static class Step {
        /**
         * The geometry of the step. This is a list of coordinates that represent the path of the
         * step.
         */
        private final List<Coordinates> geometry;
        /**
         * The departure time of the step. This is the time at which the user will depart.
         * E.g. The user might already be at the station, but may need to wait until the next train
         * departs.
         */
        private final ClockTime departureTime;
        /**
         * The arrival time of the step. This is the time at which the user will arrive (at the end
         * of this step).
         */
        private final ClockTime arrivalTime;

        /**
         * Constructor for the Step class.
         */
        public Step(List<Coordinates> geometry,
                    ClockTime departureTime, ClockTime arrivalTime) {
            this.geometry = geometry;
            this.departureTime = departureTime;
            this.arrivalTime = arrivalTime;
        }

        /**
         * Getter for the geometry of the step. This is a list of coordinates that represent the path of the
         * step.
         */
        public List<Coordinates> getGeometry() {
            return geometry;
        }

        /**
         * Getter for the step's departure time. This is the time at which the user will depart.
         * E.g. The user might already be at the station, but may need to wait until the next train
         * departs.
         */
        public ClockTime getDepartureTime() {
            return departureTime;
        }

        /**
         * Getter for the step's arrival time. This is the time at which the user will arrive (at the end
         * of this step).
         */
        public ClockTime getArrivalTime() {
            return arrivalTime;
        }
    }

    /**
     * A record representing a walk step. A walk step is a collection of walk instructions
     * that is either standalone or connects the user between their current location / public-transit
     * station.
     */
    public static class WalkStep extends Step {
        /**
         * The instructions for the walk step.
         */
        private final List<WalkInstruction> instructions;

        public WalkStep(List<Coordinates> geometry,
                        ClockTime departureTime, ClockTime arrivalTime,
                        List<WalkInstruction> instructions) {
            super(geometry, departureTime, arrivalTime);
            this.instructions = instructions;
        }

        /**
         * Getter for the instructions for the walk step.
         */
        public List<WalkInstruction> getInstructions() {
            return instructions;
        }
    }

    /**
     * A record representing a walk instruction. This is an individual instruction
     * in a larger walk step.
     * @param text The text of the instruction, e.g. "Turn left."
     * @param streetName The name of the street the instruction is for, e.g. "Broadway"
     * @param sign The sign corresponding to this instruction, e.g. to turn right.
     */
    public record WalkInstruction(String text, String streetName, InstructionSign sign) {
        /**
         * Converts the instruction to a string.
         */
        @NonNull
        @Override
        public String toString() {
            return "Instruction{" +
                    "text='" + text + '\'' +
                    ", streetName='" + streetName + '\'' +
                    ", sign=" + sign +
                    '}';
        }
    }

    /**
     * The walk sign based on this
     * <a href="https://github.com/graphhopper/graphhopper/blob/master/docs/web/api-doc.md">reference</a>
     */
    public enum InstructionSign {
        U_TURN_UNKNOWN(-98),
        U_TURN_LEFT(-8),
        KEEP_LEFT(-7),
        LEAVE_ROUNDABOUT(-6),
        TURN_SHARP_LEFT(-3),
        TURN_LEFT(-2),
        TURN_SLIGHT_LEFT(-1),
        CONTINUE_ON_STREET(0),
        TURN_SLIGHT_RIGHT(1),
        TURN_RIGHT(2),
        TURN_SHARP_RIGHT(3),

        FINISH_INSTRUCTION_BEFORE_LAST_POINT(4),
        INSTRUCTION_BEFORE_VIA_POINT(5),
        INSTRUCTION_BEFORE_ENTERING_ROUNDABOUT(6),
        KEEP_RIGHT(7),
        U_TURN_RIGHT(8);

        private final int sign;

        /**
         * Constructor for the InstructionSign enum.
         * @param sign
         */
        InstructionSign(int sign) {
            this.sign = sign;
        }

        /**
         * Getter for the sign.
         */
        public int getSign() {
            return sign;
        }

        /**
         * Converts an integer to an InstructionSign.
         */
        @Nullable
        public static InstructionSign fromSign(int sign) {
            for (InstructionSign instructionSign : values()) {
                if (instructionSign.sign == sign) {
                    return instructionSign;
                }
            }
            return null; // Or throw an exception if an unknown sign is not allowed
        }
    }

    public static class TransitStep extends Step {
        private final List<StationDetails> stops;
        private final TransitTrip trip;
        private final TransitRoute route;

        public TransitStep(List<Coordinates> geometry,
                           ClockTime departureTime, ClockTime arrivalTime,
                           List<StationDetails> stops, TransitTrip trip,
                           TransitRoute route) {
            super(geometry, departureTime, arrivalTime);
            this.stops = stops;
            this.trip = trip;
            this.route = route;
        }

        public List<StationDetails> getStops() {
            return stops;
        }

        public TransitTrip getTrip() {
            return trip;
        }

        public TransitRoute getRoute() {
            return route;
        }
    }

    /**
     * The route's overall departure time, i.e. when the user needs to start walking/commuting.
     */
    private final ClockTime departureTime;
    /**
     * The route's overall arrival time, i.e. the estimated time of arrival.
     */
    private final ClockTime arrivalTime;
    /**
     * The route's steps.
     */
    private final List<Step> steps;

    /**
     * Constructor for the BestRoute class.
     */
    public BestRoute(ClockTime departureTime, ClockTime arrivalTime, List<Step> steps) {
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.steps = steps;
    }

    /**
     * Getter for the route's departure time, i.e. when the user needs to start walking/commuting.
     */
    public ClockTime getDepartureTime() {
        return departureTime;
    }

    /**
     * Getter for the route's arrival time, i.e. the estimated time of arrival.
     */
    public ClockTime getArrivalTime() {
        return arrivalTime;
    }

    /**
     * Getter for the route's steps.
     */
    public List<Step> getSteps() {
        return steps;
    }

    /**
     * Converts the route to a string.
     */
    @NonNull
    @Override
    public String toString() {
        return "BestRoute{" +
                "departureTime=" + departureTime +
                ", arrivalTime=" + arrivalTime +
                ", steps=" + steps +
                '}';
    }
}
