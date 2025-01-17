# CMPU-203 F24 - Team 2H

## Construction Phase Description - 12/15/24

During the construction phase, we worked on implementing our third use case, Find Accessible Route. We set up an AWS EC2 server, whose setup we've documented in `server-docs\Setup.md`. Now, using the server, our application finds the best routes in the city. Furthermore, we worked hard on fixing rare bugs that would crash the app and added plenty of tests and documentation.

### Addressing Former Feedback

1. We updated the class diagram, but split it into 4 smaller diagrams to ensure everything is legible. We also updated the sequence diagrams.
2. We added extensive tests and javadoc documentation, including tests for `GTFSService` and `getRecursiveRoutes()`. We also deleted the example unit tests.
3. In terms of usability, we fixed issues with overlapping text, added a "Next" button equivalent to the keyboard's ENTER key, and fixed the back button. We considered the other usability suggestions, but we thought that long-pressing might make the app less intuitive if we don't carefully think through the design. Also, showing a list of stations is a good suggestion but would need significant resources to implement intuitively (debouncing input, handling the back button, not hiding other UI, etc.)
4. Deleted unused/commented out code.

### Running the App
To run the app, open the `astudio` folder (root of the android repository) in the "Android Studio" IDE. Wait for gradle to sync and then select "app" as the target and click the run button.

Once the app opens for the first time, you'll be asked for location permission. If you deny this permission, type a valid location (any place in NYC) in the Current Location Text Field, and press ENTER on keyboard or click the "Next" button. If the location cannot be found, an alert will appear. Otherwise, if location services are on, it will automatically display the location in the Current Location field.

Then, the stations by the current location will automatically pop up with markers. Different markers indicate different things: the green marker with a person inside indicates the start location; the other markers indicate stations which are either accessible (green markers) or partially/not accessible (red markers).

There are 2 things that you can do at this point:

1) Upon clicking a station, the view takes you to the view station details use case. Station details that are loaded from the old station_list csv file are loaded automatically, while the extra station details received from the static GTFS data take time to load. While the GTFS data is loading, a "Loading..." text and loading indicator appear. Upon pressing back, you will be taken to the first screen.
2) After getting the nearby stations, you are prompted to enter the final location. To type, click the Final Location text field and type a location in NYC and press ENTER on the keyboard or click "Next". If the location cannot be found, an alert will appear. Otherwise, the final destination and start location now have markers and the map zooms out to display both. The "Go" buttom will then activate. Upon clicking it, you're taken to the Best Routes screen. While the app is loading the full GTFS data and making a route request to the server, a loading indicator appears. Once loaded, the routes will show up in a compact list. For each route, information appears in two rows. The first row shows the route duration in minutes in big letters, along with the estimated arrival time in military time. In the second row, we show the instructions for the route, which includes walk instructions, or transit instructions, shown as a train wrapped in the color of the respective transit line.

### Limitations and Simplifying Assumptions
Some limitations to this app are: 

1) The routes screen only shows a compact view of the routes which isn't very useful. Due to time constraints in the construction phase, we didn't have time to implement a secondary, expansive screen that shows step-by-step instructions and the route on the map. However, we do have that data from the server and a future version of this app could definitely show that information to the user.
2) Changing the source/start location is still not possible. We assume that most users will simply enable location services and thus only need to change the final destination. However, this is definitely a good quality-of-life improvement for a future version of this app.
3) The android tests must be run individually and the developer must click "Don't Allow" when the app prompts them for the device's location. Unfortunately, we couldn't find a way around this; we were only able to *grant* permission but not deny it.
4) Stations can only be found within NYC area.

## Prototype II Description - 11/23/24

During iteration 2, we implemented our first use case, Find Accessible Stations, and our third use
case, View Station Details. Our original plan was to implement our second use case, Find Accessible Routes,
but we ran into an issue with GraphHopper which we are unable to use. We will implement Find Accessible Routes
in our next use case, where we will use a server, and write the code for Manage Favorites, which allows users to add
routes from the Find Accessible Routes use case. 

### Addressing Former Feedback
1. We tried renaming the package names to lowercase but got weird compiler errors.
2. We're still working on decoupling GTFSLoader/StationFinder and thus didn't test them in the current iteration.
3. We had looked at the `.gitignore` extensively but couldn't figure out why git was tracking `.class` files; we 
thought we had excluded those. Nevertheless, this doesn't appear to be a problem with the `astudio` project.

### Running the App
To run the app, open the `astudio` folder (root of the android repository) in the "Android Studio" IDE.
Wait for gradle to sync and then select "app" as the target and click the run button..
Type a valid location(any place in NYC) in the Current Location Text Field,
and press ENTER on keyboard. If the Android Phone's location services are on, 
it will automatically display the location in the Current Location field and the nearby stations. 
The stations by the current location will automatically pop up with markers. There are 2 things that you can do at this point:
1) Upon clicking a station, the view takes you to the view station details use case. Station details that are loaded 
from the old station_list csv file are loaded automatically while the extra statin details received from the static 
GTFS data take time to load. Upon pressing back, you will be taken to the first screen, but the screen won't save changes.
2) After getting the nearby stations, you are prompted to enter the final location. To type, click the Final Location
text field and type a location in NYC and press ENTER on the keyboard. If the location cannot be found, an alert will appear.
Otherwise, the final destination and start location now have markers and the map zooms out to display both. The go button
isn't implemented yet but in our next iteration, we will allow the go button to run the Find Accessible Routes use case.

### Limitations and Simplifying Assumptions
Some limitations to this prototype are: 
1) Back button doesn't save changes to station map fragment and you can't re-enter current location.
2) Go Button isn't implemented as well as our 2nd highest priority use case.
3) Favorites Button isn't implemented. 
4) Stations can only be found within NYC area. 
5) Setting a final location that's very close to the initial location will result in Trainfo zooming too far into the pins.

## Prototype I Description - Phase 1
We implemented a simple prototype for viewing nearby stations. We only support 
searching for stations near a coordinate (as opposed to typing in a name and then 
finding the respective coordinates). 

### Running the App
To run the app, open the `team-2h` folder (root of the repository) in the "IntelliJ IDEA" IDE.
Then, press the run button on the top right. Alternatively, navigate to `intellij/src/Main.java`
and click the run button next to the "Main" class declaration or the "main" method.

### Limitations and Simplifying Assumptions
We assume that the user can input their coordinates. We don't have access to the device of the user's location
and neither have we implemented the feature where the user types in a description of their location, and we look up the 
coordinates. It'll be easier to implement such a feature when we move to Android Studio and have access to geocoding 
libraries in the Java ecosystem.

Further, we're using a smaller dataset published by the MTA as opposed to their actual GTFS dataset which greatly 
simplifies the amount of files and data we need to parse. We plan to implement more comprehensive parsing of GTFS
data when it's time to implement the "View Station Details" use case.

Finally, this is evidently a CLI app and not an actual Android app.

### Usage Guide
When running the app, a console window will pop up and the app will greet the user "Welcome to the Trainfo app! What
would you like to do today?" The user should then type in a number for the option they wish to select and press enter.
Currently, only option `1: Find Stations Near Me` and `4: Exit` are implemented.

### Using `1: Find Stations Near Me`
After typing `1` and pressing enter, the user will be prompted for coordinates. These coordinates should be within the 
**NYC** metropolitan area. If they exceed broad min/max longitude or latitude values, the app will reject them, 
prompting the user to retype a different set of coordinates. Example coordinates of `longitude=-73.909831` and 
`latitude=40.874561` (Western Harlem) are displayed. To test different coordinates, one can open Google Maps on their 
phone, find a place on the map, and type the coordinates into our app. So after typing in the longitude and pressing enter, the app will prompt the user to retype a valid value or move on
to requesting the latitude. If the latitude is correct, the app will display nearby stations.

The app returns an ordered list of stations. For each station, we show the display name (e.g. "215 St"), the coordinates
of the station as a latitude and longitude pair, the train lines that pass through that station, and finally
whether the station is accessible or not. Note that for distant areas in the city, which are more than 1 km from any 
station, no nearby stations will be displayed. After showing the results, the app will show the start menu to the user 
and the user can continue by selecting `1: Find Stations Near Me` and typing in different coordinates or by clicking `4`
to exit.

## Authors and Acknowledgment
Areebah Aziz and Filippos Sakellariou.
