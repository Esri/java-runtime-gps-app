/*
  Copyright 2021 Esri

  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy
  of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  License for the specific language governing permissions and limitations
  under the License.
 */

package com.esri.samples.gps_demo;

import com.esri.arcgisruntime.location.NmeaLocationDataSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.view.MapView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class GPS_demo extends Application {

    private MapView mapView;
    private NmeaLocationDataSource nmeaLocationDataSource;
    private GPSReader gpsReader;

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) {

        // set the title and size of the stage and show it
        stage.setTitle("GPS demo app");
        stage.setWidth(800);
        stage.setHeight(700);
        stage.show();

        // create a JavaFX scene with a stack pane as the root node and add it to the scene
        StackPane stackPane = new StackPane();
        Scene scene = new Scene(stackPane);
        stage.setScene(scene);

        // create a MapView to display the map and add it to the stack pane
        mapView = new MapView();
        stackPane.getChildren().add(mapView);

        // create an ArcGISMap with an imagery basemap
        ArcGISMap map = new ArcGISMap(Basemap.createStreets());

        // display the map by setting the map on the map view
        mapView.setMap(map);

        // make location data source and link to Location Display
        nmeaLocationDataSource = new NmeaLocationDataSource();
        mapView.getLocationDisplay().setLocationDataSource(nmeaLocationDataSource);

        // start location data source and wait for it to be ready
        nmeaLocationDataSource.startAsync();
        nmeaLocationDataSource.addStartedListener(()-> {
            gpsReader = new GPSReader(nmeaLocationDataSource);
        });
    }

    /**
     * Stops and releases all resources used in application.
     */
    @Override
    public void stop() {
        if (gpsReader != null) gpsReader.close();
        if (mapView != null) mapView.dispose();
    }
}
