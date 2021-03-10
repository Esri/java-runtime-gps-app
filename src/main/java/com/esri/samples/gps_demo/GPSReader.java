/*
 * Copyright 2021 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.esri.samples.gps_demo;
import com.esri.arcgisruntime.location.NmeaLocationDataSource;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * A class for connecting to a serial port for reading NMEA data and sending the data received
 * from the GPS device to a specified {@link NmeaLocationDataSource}.
 **/
public class GPSReader {
    private final NmeaLocationDataSource nmeaLocationDataSource;

    private SerialPort gpsSerialPort = null;

    /**
     * Constructor for the class which takes in the {@link NmeaLocationDataSource} which will be used to
     * display the location obtained from the GPS device
     * @param source the location data source
     */
    public GPSReader(NmeaLocationDataSource source) {
        nmeaLocationDataSource = source;

        // try each available port in turn
        for (SerialPort serialPort : SerialPort.getCommPorts()) {
            System.out.println("detected port name " + serialPort.getSystemPortName());
            PortChecker portChecker = new PortChecker(serialPort);
            portChecker.start();
        }
    }

    /**
     * Returns the serial port which is connected to the GPS device.  This {@link SerialPort} can be
     * used for closing the port when it is no longer needed or the application closes.
     * @return serial port
     */
    public SerialPort getGpsSerialPort() {
        return gpsSerialPort;
    }

    /**
     * Class used to check if a GPS device is connected to it.  If a GPS device is found then this will be used
     * to read in the NMEA sentences and pass them to the location data source for processing.
     */
    private class PortChecker extends Thread {
        private String nmeaSentence = "";
        private boolean foundGPS = false;
        private final SerialPort serialPort;
        // list of baud rates to try.  4800 baud is most commonly used so trying that first
        private final List<Integer> baudRates = Arrays.asList(4800, 9600, 19200, 1200, 2400);

        /**
         * Constructor takes in the serial port to check if it has a GPS device connected to it
         * @param serialPort the serial port
         */
        public PortChecker(SerialPort serialPort) {
            this.serialPort = serialPort;
        }

        public void run(){
            System.out.println("Checking " + serialPort.getSystemPortName());
            // loop through the possible baud rates
            for (int baudRate : baudRates) {
                // check if we have found the port already on another tread
                if (gpsSerialPort == null) {
                    System.out.println("trying " + serialPort.getSystemPortName() + " with baud rate of " + baudRate);
                    // open serial port
                    serialPort.setComPortParameters(baudRate, 8, 1, 0);
                    serialPort.openPort();

                    // set up a listen for new data
                    serialPort.addDataListener(new SerialPortDataListener() {
                        @Override
                        public int getListeningEvents() {
                            return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
                        }

                        @Override
                        public void serialEvent(SerialPortEvent event) {
                            // return if anything other than new data
                            if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
                                return;
                            // read what has come in
                            byte[] newData = new byte[serialPort.bytesAvailable()];
                            serialPort.readBytes(newData, newData.length);
                            // convert byte array to string
                            String s = new String(newData, StandardCharsets.UTF_8);
                            // as it comes in 1 byte at a time build up the sentence...
                            nmeaSentence = nmeaSentence + s;
                            // are we reading from a verified GPS unit?
                            if (foundGPS) {
                                // see if we have come up to the end of the sentence
                                if (s.contains("\n")) {
                                    // send the sentence to the location data source for parsing.
                                    System.out.println(nmeaSentence);
                                    nmeaLocationDataSource.pushData(nmeaSentence.getBytes());
                                    // clear the way for a new sentence
                                    nmeaSentence = "";
                                }
                            }
                        }
                    });

                    // give the port a while to collect some data before we start checking for $GP
                    try {
                        System.out.println("sleeping for 1 sec");
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // see if we have a sentence containing $GP which indicates we are reading from a GPS at correct baud rate
                    System.out.println("checking: " + nmeaSentence);
                    if (nmeaSentence.contains("$GP")) {
                        System.out.println("------------------------------------GPS FOUND on " + serialPort.getSystemPortName());
                        foundGPS = true;
                        nmeaSentence = "";
                        // record the serial port reference so we can close it on app closure.
                        gpsSerialPort = serialPort;
                        break;
                    }

                    // close the port as we've not found a GPS device here
                    serialPort.closePort();

                    // clear sentence ready to try out another baud rate
                    nmeaSentence = "";
                }
            }
            System.out.println("thread stopping for " + serialPort.getSystemPortName());
        }
    }
}
