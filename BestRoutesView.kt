package edu.vassar.cmpu203.myfirstapplication.View

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.vassar.cmpu203.myfirstapplication.Controller.Controller
import edu.vassar.cmpu203.myfirstapplication.Controller.Controller.GotBestRoutes
import edu.vassar.cmpu203.myfirstapplication.Controller.Controller.RequestedBestRoutes
import edu.vassar.cmpu203.myfirstapplication.Controller.State
import edu.vassar.cmpu203.myfirstapplication.Controller.StateEvent.FoundBestRoutes
import edu.vassar.cmpu203.myfirstapplication.Model.BestRoute
import edu.vassar.cmpu203.myfirstapplication.Model.ClockTime
import edu.vassar.cmpu203.myfirstapplication.Model.Coordinates
import edu.vassar.cmpu203.myfirstapplication.Model.Destination
import edu.vassar.cmpu203.myfirstapplication.Model.StationDetails
import edu.vassar.cmpu203.myfirstapplication.Model.TransitRoute
import edu.vassar.cmpu203.myfirstapplication.Model.TransitTrip
import edu.vassar.cmpu203.myfirstapplication.Model.TripCollection
import edu.vassar.cmpu203.myfirstapplication.View.ui.theme.BlueTransit
import edu.vassar.cmpu203.myfirstapplication.View.ui.theme.MyFirstApplicationTheme

/**
 * Extract the `finalDestination` state present in both `RequestedBestRoutes` and `GotBestRoutes`.
 */
fun getFinalDestination(state: State): Destination? {
    return when (state) {
        is RequestedBestRoutes -> state.finalDestination
        is GotBestRoutes -> state.finalDestination
        else -> null
    }
}

/**
 * The root view for the best routes screen/fragment.
 */
@Composable
fun BestRoutesView(liveState: LiveData<State>) {
    // Get the state from the LiveData
    val state by liveState.observeAsState()
    // Extract the final destination from the state
    val destinationName = state?.let { getFinalDestination(it)?.name }

    if (destinationName != null) {
        // Use Column layout to arrange the title and found routes vertically.
        Column(
            modifier = Modifier.fillMaxSize(), // Fill the available space
            verticalArrangement = Arrangement.Center, // Center vertically
            horizontalAlignment = Alignment.CenterHorizontally // Center horizontally
        ) {
            // Display the title ("Route to" + destination name).
            // We add modifiers for the title to appear large.
            Text(
                text = "Route to ${destinationName}!",
                maxLines = 2, // Specify max lines to truncate the text
                overflow = TextOverflow.Ellipsis, // When truncating, use ellipsis
                fontSize = 24.sp, // Set the font size to 24.dp which should be large enough.
                fontWeight = FontWeight.W900, // Set the font weight to bold
                modifier = Modifier.padding(16.dp) // Add padding to the title
            )

            // Organize the loading text + indicator in a column; otherwise, display the routes.
            Column(modifier=Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                when (state) {
                    is RequestedBestRoutes -> {
                        // Show loading text + indicator
                        Text("Loading...")
                        // We don't specify a progress to make this indicator indefinite (always
                        // spinning)
                        LinearProgressIndicator(
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                    is GotBestRoutes -> {
                        // Cast the state to `GotBestRoutes`.
                        val bestRoutes = (state as GotBestRoutes).bestRoutes
                        // List the best routes in compact mode.
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            // Each route instance is unique; use that as id (we don't expect routes
                            // to change so this simple solution works).
                            items(bestRoutes, key={ route -> System.identityHashCode(route)}) { route ->
                                // Present the route in compact mode.
                                BestRouteCompactView(route)
                            }
                        }
                    }
                }
            }

        }
    }
}

/**
 * Present a single best route in compact mode.
 */
@Composable
fun BestRouteCompactView(route: BestRoute) {
    // Group the route information in a box that takes up the entire width of the screen,
    // except for some padding on all sides.
    Box(
        modifier = Modifier
            .fillMaxWidth() // Fill the entire width
            .padding(16.dp) // Add padding
    ) {
        // Group the route information in a column that takes up the full box width.
        Column(modifier = Modifier.fillMaxWidth()) {
            // Display the trip length and arrival time in the top row.
            Row(verticalAlignment = Alignment.Bottom) {
                // Calculate the duration of the route.
                // Note that any route that's only a couple seconds long will have a minute duration
                // of 0, so we default to 1 minute.
                val minuteDuration = ClockTime.minuteDuration(route.departureTime, route.arrivalTime)
                        .coerceAtLeast(1)

                // Display the trip length; use large, bold font to emphasize the length..
                Text(
                    "Trip Length: $minuteDuration min",
                    fontWeight = FontWeight.Bold, fontSize = 18.sp)
                // Add as much space as possible between the trip length and arrival time.
                Spacer(Modifier.weight(1f))
                // Display the arrival time; use bold (yet small) font to emphasize the time.
                Text("Arrive at ${route.arrivalTime.toMilitaryTime()}", fontWeight = FontWeight.Bold)
            }

            // Display the route steps in the bottom row (that takes up the entire width).
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                // Display each step in the route.
                route.steps.forEachIndexed { index, step ->
                    // Calculate the duration of the walk step.
                    val walkDuration =
                        ClockTime.minuteDuration(step.departureTime, step.arrivalTime)

                    // Calculate if the step is the last step in the route.
                    // In this case, we won't need to add the ">" icon that only goes between steps.
                    val isLast = index == route.steps.size - 1

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (step) {
                            is BestRoute.WalkStep -> {
                                // If it's a walk step, display a walk icon and the walk duration.
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // The walk icon in black tint and some padding.
                                    Icon(
                                        Icons.Filled.DirectionsWalk, "Walk",
                                        modifier = Modifier.padding(5.dp, 5.dp), tint = Color.Black
                                    )

                                    // If the minute duration is 0, don't display it cause 0' looks
                                    // confusing.
                                    if (walkDuration > 0) {
                                        Text("$walkDuration min")
                                    }
                                }
                            }
                            is BestRoute.TransitStep -> {
                                // Display the route step

                                // Try to calculate the color if it's not blank.
                                // If that fails, use the default BlueTransit color.
                                val routeColor = if (step.route.color != "")
                                    Color(step.route.color) else BlueTransit

                                // Display the route color and name.
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp)) // Rounded corners
                                        .background(routeColor) // Add the route color as background
                                        .padding(10.dp, 5.dp) // Add padding
                                ) {
                                    // Display the train icon in white tint..
                                    Icon(Icons.Filled.Train, "Train",
                                        tint = Color.White)
                                    // Display the route name in white, bold text.
                                    Text(
                                        step.route.displayName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // If this isn't the last step, add the connecting icon ">"
                        // Use the "and" description for accessibility.
                        if (!isLast) {
                            Icon(Icons.Filled.KeyboardArrowRight, "and")
                        }

                    }

                }
            }
        }
    }
}

/**
 * Initialize Compose Color with hex value.
 */
fun Color(hex: String): Color {
    return try {
        // Try to parse the hex value.
        Color(android.graphics.Color.parseColor("#$hex"))
    } catch (e: NumberFormatException) {
        // If the hex is invalid, use the default BlueTransit color.
        BlueTransit
    }
}

/**
 * Preview the best routes view.
 */
@Preview(showBackground = true)
@Composable
fun GotRoutesPreview() {
    // === Create Fake Data ------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------

    // Create a fake nearby stations state.
    val nearbyStationsState = Controller.GotNearbyStations(
        Destination(Coordinates(40.610780, -73.941350), "Madison Square Garden"),
        emptyList()
    )
    // Create a fake final destination
    val finalDestination = Destination(Coordinates(42.575871, -73.683647), "Empire State Building")

    // Create a fake transit route
    val transitRoute = TransitRoute(
        "1",
        "1",
        "Broadway - 7 Avenue Local",
        "EE352E",
        TripCollection())

    // Create a fake list of stations
    val stations = listOf(
        StationDetails(
            "119S", "103 St", Coordinates(-73.968379, 40.799446),
            setOf(transitRoute), "0"),
        StationDetails(
            "121S", "86 St", Coordinates(-73.976218, 40.788644),
            setOf(transitRoute), "0"),
        StationDetails(
            "122S", "79 St", Coordinates(-73.979917, 40.783934),
            setOf(transitRoute), "0"),
        StationDetails(
            "123S", "72 St", Coordinates(-73.98197, 40.778453),
            setOf(transitRoute), "0"),
        StationDetails(
            "124S", "66 St-Lincoln Center", Coordinates(-73.982209, 40.77344),
            setOf(transitRoute), "0"),
        StationDetails(
            "125S", "59 St-Columbus Circle", Coordinates(-73.981929, 40.768247),
            setOf(transitRoute), "0"),
        StationDetails(
            "126S", "50 St", Coordinates(-73.983849, 40.761728),
            setOf(transitRoute), "0"))
    // Create a fake transit trip
    val transitTrip = TransitTrip(
        "ASP24GEN-1092-Weekday-00_109600_1..S03R", transitRoute, null,
        TransitTrip.TripService(
            "1", "Start date", "End date", true, true, true, true, true, true, true),
        "South Ferry", TransitTrip.TripDirection.ONE, HashMap())

    // Create list of 2 fake transit routes.
    val bestRoutes = listOf(
        BestRoute(
            ClockTime(18, 50, 38),
            ClockTime(18, 51, 30),
            listOf(
                BestRoute.WalkStep(
                    listOf(Coordinates(42.575871, -73.683647)),
                    ClockTime(23, 38, 25),
                    ClockTime(23, 40, 30),
                    listOf(
                        BestRoute.WalkInstruction("Continue", "", BestRoute.InstructionSign.CONTINUE_ON_STREET),
                        BestRoute.WalkInstruction("Turn right onto Broadway", "Broadway", BestRoute.InstructionSign.TURN_RIGHT),
                        BestRoute.WalkInstruction("Arrive at destination", "", BestRoute.InstructionSign.FINISH_INSTRUCTION_BEFORE_LAST_POINT)
                    )
                ),
                BestRoute.TransitStep(
                    listOf(Coordinates(42.575871, -73.683647)),
                    ClockTime(23, 40, 30),
                    ClockTime(23, 52, 0),
                    stations,
                    transitTrip,
                    transitRoute),
                BestRoute.WalkStep(
                    listOf(Coordinates(42.575871, -73.683647)),
                    ClockTime(23, 38, 25),
                    ClockTime(23, 40, 30),
                    listOf(
                        BestRoute.WalkInstruction("Continue onto Broadway", "Broadway", BestRoute.InstructionSign.CONTINUE_ON_STREET),
                        BestRoute.WalkInstruction("Turn right", "", BestRoute.InstructionSign.TURN_RIGHT),
                        BestRoute.WalkInstruction("Turn left", "", BestRoute.InstructionSign.TURN_LEFT),
                        BestRoute.WalkInstruction("Turn right", "", BestRoute.InstructionSign.TURN_RIGHT),
                        BestRoute.WalkInstruction("Turn left onto 6½ Avenue", "6½ Avenue", BestRoute.InstructionSign.TURN_LEFT),
                        BestRoute.WalkInstruction("Turn right", "", BestRoute.InstructionSign.TURN_RIGHT),
                        BestRoute.WalkInstruction("Turn left", "", BestRoute.InstructionSign.TURN_LEFT),
                        BestRoute.WalkInstruction("Turn right", "", BestRoute.InstructionSign.TURN_RIGHT),
                        BestRoute.WalkInstruction("Turn left", "", BestRoute.InstructionSign.TURN_LEFT),
                        BestRoute.WalkInstruction("Turn right onto West 54th Street", "West 54th Street", BestRoute.InstructionSign.TURN_RIGHT),
                        BestRoute.WalkInstruction("Arrive at destination", "", BestRoute.InstructionSign.FINISH_INSTRUCTION_BEFORE_LAST_POINT)
                    )
                )
            )
        )
    )

    // === Set Up State & View ---------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------

    // Set up the fake mutable live data
    val mutableState = MutableLiveData<State>(GotBestRoutes(
        nearbyStationsState,
        finalDestination,
        bestRoutes
    ))
    MyFirstApplicationTheme {
        // Display the view with the fake data.
        BestRoutesView(mutableState)
    }
}

/**
 * Preview the requested best routes view.
 */
@Preview(showBackground = true)
@Composable
fun RequestedRoutesPreview() {
    // Create a fake nearby stations state.
    val mutableState = MutableLiveData<State>(RequestedBestRoutes(
        Controller.GotNearbyStations(
            Destination(Coordinates(40.610780, -73.941350), "Madison Square Garden"),
            emptyList()
        ),
        Destination(Coordinates(42.575871, -73.683647), "Empire State Building")
    ))

    // Set up the fake mutable live data
    MyFirstApplicationTheme {
        BestRoutesView(mutableState)
    }
}
