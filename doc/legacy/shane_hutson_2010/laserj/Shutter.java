package plugins.LaserMicrosurgery.laserj;

/* Name: Shutter.java
 * Project: Laser microdissection of dorsal closure
 * Version: 2.0
 * Author: Shane Hutson
 * Maintained by: Albert Mao
 * Date: 11/19/2004
 * Description: This class represents a shutter.
 * The shutter can opened and closed at will.
 * See the documentation for the UniBlitz D122 Shutter Driver and the Java Communications API for details on the implementation of the communication betweeh the host computer and the shutter driver.
*/

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.awt.event.*;
import ij.plugin.filter.*;
import java.io.*;
import java.util.*;
import javax.comm.*;

public class Shutter {
    static Enumeration portList;
    static CommPortIdentifier portId;
    static CommPort shutterPort;
    static OutputStream shutterStream;

    String whichport;
    String openString = "@\n";
    String closeString = "A\n";


    public Shutter(String arg) {
        this.whichport = arg;
        openShutterPort(whichport);
    }

    public Shutter() {
        IJ.showMessage("Serial Port Not Specified!", "Program will attempt to use default port (COM2) to communicate with shutter.");
        this.whichport = "COM2";
        this.openShutterPort(whichport);
    }

    protected void finalize() {
        shutterPort.close();
    }

    public void open() {
        writeShutter(openString);
    }

    public void close() {
        writeShutter(closeString);
    }


    void writeShutter(String msg) {
        try {
            shutterStream.write(msg.getBytes());
        } catch (IOException e) {
            IJ.showMessage("ERROR", "Error Writing to Shutter Port: "+ e.getMessage());
            shutterPort.close();
        }
    }

    void openShutterPort(String portname) {
        try {
            portId = CommPortIdentifier.getPortIdentifier(portname);
        } catch(NoSuchPortException e) {
            IJ.showMessage("Port " + portname + " does not exist", e.getMessage());
            return;
        }
        try {
            IJ.write("Opening "+portname+" as Shutter Port");
            shutterPort = portId.open("Microbeam Shutter", 2000);
        } catch (PortInUseException e) {
            IJ.showMessage("Error opening "+portname+ " as Shutter Port", e.getMessage());
            shutterPort.close();
            return;
        }
        try {
            shutterStream = shutterPort.getOutputStream();
        } catch (IOException e) {
            IJ.showMessage("Error opening "+portname+ " as Shutter Port", e.getMessage());
            shutterPort.close();
            return;
        }
        if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
            try {
                ((SerialPort) shutterPort).setSerialPortParams(300,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            } catch (UnsupportedCommOperationException e) {
                IJ.showMessage("Error opening "+portname+ " as Shutter Port", e.getMessage());
                shutterPort.close();
                return;
            }
        }
    }

}
