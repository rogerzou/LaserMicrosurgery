package plugins.LaserMicrosurgery.laserj;

/* Name: Mirror.java
 * Project: Laser microdissection of dorsal closure
 * Version: 2.0
 * Author: Shane Hutson
 * Maintained by: Albert Mao
 * Date: 11/19/2004
 * Description: This class represents a tiltable mirror.
 * The piezoelectric actuators motors that direct the mirror are controlled by a Newport ESP300 Universal Motion Controller unit.
 * This abstraction uses the concept of a current position, which corresponds to the location of a laser spot that is reflected off the mirror.
 * Several methods for moving the current position to a new location through either a straight line or an arc are provided.
 * See the documentation for the ESP300 and the Java Communications API for details on the implementation of the communication between the host computer and the microcontroller over a serial port.
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

public class Mirror implements SerialPortEventListener {
	static Enumeration portList;
	static CommPortIdentifier portId;
	static SerialPort mirrorPort;
	static OutputStream mirrorOutStream;
	static InputStream mirrorInStream;
	static String returnedString = "No response yet";
	static double MAXVELOCITY = 0.2;
	static double MINVELOCITY = 0.01;
	String whichport;
	long startTime, timeOut;
	double microns_per_pixel;
	double calX;
	double calY;
	ImageWindow win;
	boolean using_window_for_esc;
	String ls = System.getProperty("line.separator");
	boolean replyReceived = false;
	boolean done;
	boolean handshake = false;	//Controller is supposed to work with CTS/RTS, but I can't make it work

	public Mirror(String arg, long t) {
		this.whichport = arg;
		this.timeOut = t;
		ImagePlus imp1 = WindowManager.getCurrentImage();
		if (imp1!=null) {
			this.win = imp1.getWindow();
			win.running = true;
			this.using_window_for_esc = true;
		} else {
			this.using_window_for_esc = false;
		}
		this.openMirrorPort(whichport);
		this.openMirrorPortListener();
		this.initializeMirror();
	}

	public Mirror() {
		this( "COM1", 60000L);
		IJ.showMessage("Serial Port Not Specified!", "Program will attempt to use default port (COM1) to communicate with mirror actuators.");
	}

	protected void finalize() {
		mirrorPort.close();
	}

	void off() {
		this.finalizeMirror();
		mirrorPort.close();
	}

	void setVelocity(double velocity) {
		if (velocity>MAXVELOCITY) velocity = MAXVELOCITY;
		if (velocity<MINVELOCITY) velocity = MINVELOCITY;
		writeMirror("1HV" + IJ.d2s(velocity) + ";1HA0.1;1HD0.1");	// Set vectorial velocity, acceleration & deceleration for Group 1
		IJ.wait(200);
	}

	void moveTo(double x, double y) {
		boolean sure = true;
		if((Math.abs(x)>2.0)||(Math.abs(y)>2.0)) sure = IJ.showMessageWithCancel("ARE YOU SURE?","Mirror position "+IJ.d2s(x,2)+", "+IJ.d2s(y,2)+" will be well off the visible area.");
		if(sure) {
			writeMirror("1HL"+IJ.d2s(x,6)+","+IJ.d2s(y,6)+";1HW;1HQ8;1HS?");
			char stopped = waitForMirrorReply().charAt(0);
			IJ.write("\tReceived :\t "+stopped);
		} else {
			IJ.write("CANCELED MOVE TO POSITION "+IJ.d2s(x,2)+", "+IJ.d2s(y,2)+" mm.");
		}
	}

	void arcmoveTo(double x0, double y0, double degrees) {
		writeMirror("1HC"+IJ.d2s(x0,6)+","+IJ.d2s(y0,6)+","+IJ.d2s(degrees,6)+";1HW;1HQ8;1HS?");
		char stopped = waitForMirrorReply().charAt(0);
		IJ.write("\tReceived :\t "+stopped);
	}

	void defineHome() {
		writeMirror("1DH;2DH");			// Assign current position to 0,0
		IJ.wait(500);
	}

	void initializeMirror() {
		writeMirror("1MO;2MO;1HN1,2");			// Power on to motors 1&2, Assign motors 1&2 to Group 1
		IJ.wait(200);
		this.setVelocity(MAXVELOCITY);
	}

	void finalizeMirror() {
		writeMirror("1HW;1HX;1MF;2MF");		// Delete Group 1, Power off to motors 1&2
		IJ.wait(200);
	}

    void writeMirror(String msg) {
        boolean echo = true;
		boolean successful = true;
		try {
			if (echo) IJ.write("\tSend: \t"+msg);
			msg += "\r";
			if (mirrorPort.isCTS()) IJ.write("CTS is true");
			char[] msgchar = msg.toCharArray();
			for(int i = 0; i < msg.length(); i++) {
				mirrorOutStream.write((int) msgchar[i]);
			}
		} catch (IOException e) {
			IJ.showMessage("ERROR", "Error Writing to Mirror-Drive Port: "+ e.getMessage());
			mirrorPort.close();
			successful = false;
		}
	}

	// Uses an idle loop to wait until the microcontroller responds, the ESC key is pressed, or the timeout is reached
	String waitForMirrorReply() {
		startTime = System.currentTimeMillis();
		String reply;
		while (true) {
			long time = System.currentTimeMillis();
			long elapsedTime = time-startTime;
			IJ.showStatus("Waiting for reply: "+(time-startTime)/1000 + " seconds (press ESC to abort)");
			if (elapsedTime >= timeOut)	{
				IJ.beep();
				IJ.write("Program has reached timeout without Reply");
				reply="None received";
				break;
			}
			if (using_window_for_esc) {
				if (!win.running) {
					IJ.beep();
					IJ.write("Program was aborted with ESC");
					reply="None received";
					break;
				}
			}
			if (replyReceived) {
				reply=returnedString;
				break;
			}
		}
		replyReceived = false;
		return reply;
	}

	void openMirrorPort(String portname) {
		portList = CommPortIdentifier.getPortIdentifiers();
		while (portList.hasMoreElements()) {
			portId = (CommPortIdentifier) portList.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				if (portId.getName().equals(portname)) {
					try {
						IJ.write("Open "+portname+" as Mirror-Drive Port");
						mirrorPort = (SerialPort) portId.open("SimpleWriteApp", 2000);
					} catch (PortInUseException e) {
						IJ.showMessage("Opening "+portname+ " as Mirror-Drive Port", e.getMessage());
						mirrorPort.close();
					}
					try {
						mirrorPort.setSerialPortParams(19200,
							SerialPort.DATABITS_8,
							SerialPort.STOPBITS_1,
							SerialPort.PARITY_NONE);
					} catch (UnsupportedCommOperationException e) {
						IJ.showMessage("Opening "+portname+ " as Mirror-Drive Port", e.getMessage());
						mirrorPort.close();
					}
					if (handshake) {
						try {
							mirrorPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_OUT | SerialPort.FLOWCONTROL_RTSCTS_IN );
							IJ.write("Hardware Flow Control is being used.");
						} catch (UnsupportedCommOperationException e) {
							IJ.showMessage("Unsupported Flow Control for "+portname+ " as Mirror-Drive Port", e.getMessage());
							mirrorPort.close();
						}
					}
					try {
						mirrorOutStream = mirrorPort.getOutputStream();
					} catch (IOException e) {
						IJ.showMessage("Error Opening "+portname+ " as Mirror-Drive Port", e.getMessage());
						mirrorPort.close();
					}
				}
			}
		}
	}

    void openMirrorPortListener() {
       	try {
			mirrorInStream = mirrorPort.getInputStream();
		} catch (IOException e)  {
			IJ.showMessage("Setting-up Mirror-Drive Port Listener", e.getMessage());
			mirrorPort.close();
		}
		try {
			mirrorPort.addEventListener(this);
		} catch (TooManyListenersException e) {
			IJ.showMessage("Setting-up Mirror-Drive Port Listener", e.getMessage());
			mirrorPort.close();
		}
		mirrorPort.notifyOnDataAvailable(true);
		if (handshake) mirrorPort.notifyOnCTS(true);
	}

	public void serialEvent(SerialPortEvent event) {
		switch(event.getEventType()) {
			case SerialPortEvent.BI:
			case SerialPortEvent.OE:
			case SerialPortEvent.FE:
			case SerialPortEvent.PE:
			case SerialPortEvent.CD:
			case SerialPortEvent.CTS:
				IJ.write("CTS has changed to "+ mirrorPort.isCTS());
			case SerialPortEvent.DSR:
			case SerialPortEvent.RI:
			case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
				break;
			case SerialPortEvent.DATA_AVAILABLE:
				byte[] readBuffer = new byte[20];
				try {
					while (mirrorInStream.available() > 0) {
						int numBytes = mirrorInStream.read(readBuffer);
					}
					returnedString = new String(readBuffer);
					replyReceived = true;
				} catch (IOException e) {}
				break;
		}
    }

	boolean mirrorStopped() {
		writeMirror("1HQ8;1HS?");  		//Query if motors are stopped
		char stopped = waitForMirrorReply().charAt(0);
		IJ.write("\tReceived : \t"+stopped);
		return (stopped=='1');
	}

}
