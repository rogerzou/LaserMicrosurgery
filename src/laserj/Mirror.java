package laserj;

/* Name: Mirror.java
 * Project: Laser microdissection of dorsal closure
 * Version: 3.0
 * Inspired by: Shane Hutson (2003), Albert Mao (2004), Adam Sokolow (2007)
 * Author: Roger Zou
 * Date: 05/22/2015
 * Description: This class represents a tiltable mirror.
 * Compatible with 64 bit machine running Microsoft Windows 7.
 * The piezoelectric actuators motors that direct the mirror are controlled by a Newport ESP300 
 * Universal Motion Controller unit. This abstraction uses the concept of a current position, 
 * which corresponds to the location of a laser spot that is reflected off the mirror.
 * Several methods for moving the current position to a new location through either a straight
 * line or an arc are provided.
 * See the documentation for the ESP300 and the open-source java-simple-serial-connector (jSSC) 
 * https://code.google.com/p/java-simple-serial-connector/ for details on the implementation 
 * of the communication between the host computer and the microcontroller over a serial port.
 */

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

import ij.*;

public class Mirror implements SerialPortEventListener {
	
	private static SerialPort mirrorPort;
	
	private static final double MAXVELOCITY = 0.2;
	private static final double MINVELOCITY = 0.01;
	
	private String returnedString = "No response yet";
	
	private long startTime, timeOut;
	private boolean replyReceived = false;

	public Mirror(String portname, long t) {
		this.timeOut = t;
		this.openMirrorPort(portname);
		this.initializeMirror();
	}

	void off() {
		this.finalizeMirror();
		try { mirrorPort.closePort(); } catch (SerialPortException e1) { }
	}

	void setVelocity(double velocity) {
		if (velocity>MAXVELOCITY) velocity = MAXVELOCITY;
		if (velocity<MINVELOCITY) velocity = MINVELOCITY;
		writeMirror("1HV" + IJ.d2s(velocity) + ";1HA0.1;1HD0.1");	// Set vectorial velocity, acceleration & deceleration for Group 1
		IJ.wait(200);
	}

	void moveTo(double x, double y) {
		boolean sure = true;
		if ((Math.abs(x)>2.0)||(Math.abs(y)>2.0)) sure = IJ.showMessageWithCancel("ARE YOU SURE?","Mirror position "+IJ.d2s(x,2)+", "+IJ.d2s(y,2)+" will be well off the visible area.");
		if (sure) {
			writeMirror("1HL"+IJ.d2s(x,6)+","+IJ.d2s(y,6)+";1HW;1HQ8;1HS?");
			char stopped = waitForMirrorReply().charAt(0);
			IJ.log("\tReceived :\t "+stopped);
		} else {
			IJ.log("CANCELED MOVE TO POSITION "+IJ.d2s(x,2)+", "+IJ.d2s(y,2)+" mm.");
		}
	}

	void arcmoveTo(double x0, double y0, double degrees) {
		writeMirror("1HC"+IJ.d2s(x0,6)+","+IJ.d2s(y0,6)+","+IJ.d2s(degrees,6)+";1HW;1HQ8;1HS?");
		char stopped = waitForMirrorReply().charAt(0);
		IJ.log("\tReceived :\t "+stopped);
	}

	void defineHome() {
		writeMirror("1DH;2DH");			// Assign current position to 0,0
		IJ.wait(500);
	}

	void initializeMirror() {

		IJ.log("Initializing Mirrors");
		writeMirror("1MO;2MO;1HN1,2");			// Power on to motors 1&2, Assign motors 1&2 to Group 1
		IJ.wait(200);
		this.setVelocity(MAXVELOCITY);
	}

	void finalizeMirror() {
		writeMirror("1HW;1HX;1MF;2MF");		// Delete Group 1, Power off to motors 1&2
		IJ.wait(200);
	}

	/**
	 * Sends message to mirror
	 * @param msg message to be written
	 */
    void writeMirror(String msg) {
		try {
			IJ.log("\tSend: \t" + msg);
			msg += "\r";	// add carriage return to indicate termination of command
			mirrorPort.writeString(msg);
		} catch (SerialPortException e) {
			String error = "Mirror.openMirrorPort() failed.";
			IJ.log(error);
			try { mirrorPort.closePort(); } catch (SerialPortException e1) { }
			throw new IllegalArgumentException(error);
		}
	}

	/**
	 * Uses an idle loop to wait until the microcontroller responds, or the timeout is reached.
	 * @return the message from the device as a string, or "None received" if timeout without reply.
	 */
    String waitForMirrorReply() {
		startTime = System.currentTimeMillis();
		String reply;
		while (true) {
			long time = System.currentTimeMillis();
			long elapsedTime = time-startTime;
			IJ.showStatus("Waiting for reply: "+(time-startTime)/1000 + " seconds");
			if (elapsedTime >= timeOut)	{
				IJ.beep();
				IJ.log("Program has reached timeout without Reply");
				reply = "None received";
				break;
			}
			if (replyReceived) {
				reply = returnedString;
				break;
			}
		}
		replyReceived = false;
		return reply;
	}

	/**
	 * Opens mirror port and its listener
	 * @param portname 
	 */
	void openMirrorPort(String portname) {
		IJ.log("Opening "+portname+" as Mirror Port");
		mirrorPort = new SerialPort(portname);
		try {
			mirrorPort.openPort();
			mirrorPort.setParams(	SerialPort.BAUDRATE_19200,	// see user manual to verify parameters
					 				SerialPort.DATABITS_8,
					 				SerialPort.STOPBITS_1,
					 				SerialPort.PARITY_NONE);
			mirrorPort.setEventsMask(SerialPort.MASK_RXCHAR);
			mirrorPort.addEventListener(this);
		} catch (SerialPortException e) {
			String error = "Mirror.openMirrorPort() failed.";
			IJ.log(error);
			try { mirrorPort.closePort(); } catch (SerialPortException e1) { }
			throw new IllegalArgumentException(error);
		}
	}

    public void serialEvent(SerialPortEvent event) {
        if (event.isRXCHAR() && event.getEventValue() > 0) { // If data is available and has bits...
            try {
                returnedString = mirrorPort.readString();
                replyReceived = true;
            } catch (SerialPortException ex) {
				String error = "Mirror.serialEvent() failed.";
				IJ.log(error);
				try { mirrorPort.closePort(); } catch (SerialPortException e1) { }
				throw new IllegalArgumentException(error);
            }
        }
    }
    
	boolean mirrorStopped() {
		writeMirror("1HQ8;1HS?");  		//Query if motors are stopped
		char stopped = waitForMirrorReply().charAt(0);
		IJ.log("\tReceived : \t"+stopped);
		return (stopped=='1');
	}

}
