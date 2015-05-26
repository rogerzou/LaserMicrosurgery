package laserj;

/* Name: Shutter.java
 * Project: Laser microdissection of dorsal closure
 * Version: 3.0
 * Inspired by: Shane Hutson (2003), Albert Mao (2004)
 * Author: Roger Zou
 * Date: 05/22/2015
 * Description: This class represents a shutter. 
 * The shutter can opened and closed at will.
 * Compatible with 64 bit machine running Microsoft Windows 7.
 * See the documentation for the UniBlitz D122 Shutter Driver and the open-source 
 * java-simple-serial-connector (jSSC) https://code.google.com/p/java-simple-serial-connector/
 * for details on the implementation of the communication between the host computer and the shutter driver.
*/

import jssc.SerialPort;
import jssc.SerialPortException;

import ij.*;

public class Shutter {
	
    private static SerialPort shutterPort;

    private static final String openString = "@\n";
    private static final String closeString = "A\n";

    public Shutter(String portname) {
        openShutterPort(portname);
    }

    public void open() {
        writeShutter(openString);
    }

    public void close() {
        writeShutter(closeString);
    }

    protected void off() {
    	this.close();
    	try { shutterPort.closePort(); } catch (SerialPortException e1) { }
    }

    void openShutterPort(String portname) {
    	IJ.log("Opening "+portname+" as Shutter Port");
    	shutterPort = new SerialPort(portname);
    	try {
        	shutterPort.openPort();
			shutterPort.setParams(	SerialPort.BAUDRATE_300,	// see user manual to verify parameters
			            		 	SerialPort.DATABITS_8,
			            		 	SerialPort.STOPBITS_1,
			            		 	SerialPort.PARITY_NONE);
		} catch (SerialPortException e) {
			String error = "Shutter.openShutterPort() failed.";
			IJ.log(error);
			try { shutterPort.closePort(); } catch (SerialPortException e1) { }
			throw new IllegalArgumentException(error);
		}
    }

    void writeShutter(String msg) {
        try {
			shutterPort.writeString(msg);
		} catch (SerialPortException e) {
			String error = "Shutter.writeShutter() failed.";
			IJ.log(error);
			try { shutterPort.closePort(); } catch (SerialPortException e1) { }
			throw new IllegalArgumentException(error);
		}
    }
    
}
