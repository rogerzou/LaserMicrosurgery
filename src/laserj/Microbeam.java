package laserj;

/* Name: Microbeam.java
 * Project: Laser microdissection of dorsal closure
 * Version: 3.0
 * Inspired by: Shane Hutson (2003), Albert Mao (2004)
 * Author: Roger Zou
 * Date: 05/22/2015
 * Description: This class represents a laser microbeam.
 * This abstraction of a microbeam uses the concept of a current (x,y) position.
 * This position can be moved in real coordinates, or in coordinates scaled to correspond to the pixels of an image.
 * The microbeam also has a shutter that can be open and closed.
 */

import java.util.*;
import java.io.*;

import helpers.FileSearch;
import ij.*;
import ij.gui.*;
import ij.process.*;

public class Microbeam {

	/** SETTINGS TO CONFIGURE **/
    public static final String CONFIG_FILENAME = "Microbeam.txt";
    private static final long timeOut = 30000L;	// 30 second delay
    
    private String mr_port, sh_port;
    private double microns_per_pixel, cal1X, cal1Y, cal2X, cal2Y;
    private transient Mirror mirror;
    private transient Shutter shutter;
    private boolean setupOK;

	// Initializes using the name of Microbeam config file.
    public Microbeam(String configfilename) {
		setupOK = true;
		
		// Search for microbeam config file inside current directory
    	File curdir = new File(System.getProperty("user.dir"));
		List<String> results = FileSearch.searchDirectory(curdir, configfilename);
		String configfilepath;
		if (results.size() > 0) {
			configfilepath = results.get(0);
		} else {
			setupOK = false;
			throw new IllegalArgumentException("Microbeam config file not found.");
		}

		// Load the file into file reader to parse
		BufferedReader config;
		try {
			config = new BufferedReader(new FileReader(configfilepath));
		} catch (FileNotFoundException e) {
			setupOK = false;
			throw new IllegalArgumentException("Microbeam config file not found.");
		}
		try {
			mr_port = new StringTokenizer(config.readLine()).nextToken();
			sh_port = new StringTokenizer(config.readLine()).nextToken();
			microns_per_pixel = Double.parseDouble(new StringTokenizer(config.readLine()).nextToken());
			cal1X = Double.parseDouble(new StringTokenizer(config.readLine()).nextToken());
			cal1Y = Double.parseDouble(new StringTokenizer(config.readLine()).nextToken());
			cal2X = Double.parseDouble(new StringTokenizer(config.readLine()).nextToken());
			cal2Y = Double.parseDouble(new StringTokenizer(config.readLine()).nextToken());
			config.close();
		} catch (IOException e) {
			setupOK = false;
			throw new IllegalArgumentException("Error reading microbeam config file.");
		} catch (NumberFormatException e) {
			setupOK = false;
			throw new IllegalArgumentException("Microbeam config file is not formatted properly.");
		}
		
		// Initialize shutter and mirror
		shutter = new Shutter(sh_port);
		mirror = new Mirror(mr_port, timeOut);
    }

    public void moveToPIXELS(double xpix, double ypix, ImageProcessor ip, int zoom) {
		IJ.log("MOVING MICROBEAM TO POSITION (PIXELS): "+IJ.d2s(xpix,0)+", "+IJ.d2s(ypix,0));
		int w = ip.getWidth();
		int h = ip.getHeight();
		double axis1 = (xpix - w/2)*cal1X / zoom + (ypix - h/2)*cal1Y / zoom;
		double axis2 = (xpix - w/2)*cal2X / zoom + (ypix - h/2)*cal2Y / zoom;
		mirror.moveTo(axis1, axis2);
    }

    public void off() {
        IJ.log("\nMICROBEAM OFF");
        mirror.off();
        shutter.off();
    }

    public void moveToMM(double x, double y) {
        IJ.log("MOVING MICROBEAM TO POSITION (MM): "+IJ.d2s(x,4)+", "+IJ.d2s(y,4));
        mirror.moveTo(x,y);
    }

    public void arcmoveToMM(double x0, double y0, double degrees) {
        IJ.log("MOVING MICROBEAM ALONG ARC CENTERED AT (MM): "+IJ.d2s(x0,4)+", "+IJ.d2s(y0,4));
        mirror.arcmoveTo(x0,y0,degrees);
    }
    public void setMirrorVelocity(double v) {
        IJ.log("SET MIRROR VELOCITY TO "+IJ.d2s(v,4));
        mirror.setVelocity(v);
    }

    public void defineMirrorHome() {
        mirror.defineHome();
    }

    public void setNewMirrorHome(double x, double y) {
    	mirror.setNewHome(x, y);
    }
    
    public void openShutter() {
        shutter.open();
        IJ.log("OPEN SHUTTER");
    }

    public void closeShutter() {
        shutter.close();
        IJ.log("CLOSE SHUTTER");
    }

    public double get_microns_per_pixel() {
        return this.microns_per_pixel;
    }

    public String get_mirror_port() {
        return this.mr_port;
    }

    public String get_shutter_port() {
        return this.sh_port;
    }

    public double[] get_calibration () {
        double[] cal = new double[4];
        cal[0] = cal1X; cal[1] = cal1Y; cal[2] = cal2X; cal[3] = cal2Y;
        return cal;
    }

    public long get_timeout() {
        return timeOut;
    }

    public boolean isSetupOK() {
        return setupOK;
    }

    public void set_microns_per_pixel(double mpp) {
        this.microns_per_pixel = mpp;
    }

    public void set_calibration (double c1X, double c1Y, double c2X, double c2Y) {
        this.cal1X = c1X; this.cal1Y=c1Y; this.cal2X=c2X; this.cal2Y=c2Y;
    }

    public void saveConfig(String textfile) {
        try {
			PrintWriter config = new PrintWriter(new BufferedWriter(new FileWriter(textfile, false)));
			config.println(mr_port + "\t\tSerial Port - Mirror Scanner");
			config.println(sh_port + "\t\tSerial Port - Shutter");
			config.println(microns_per_pixel + "\t\tConversion: microns per pixel");
			config.println(cal1X + "\tConversion: mm travel(axis ONE) per pix(X)");
			config.println(cal1Y + "\tConversion: mm travel(axis ONE) per pix(Y)");
			config.println(cal2X + "\tConversion: mm travel(axis TWO) per pix(X)");
			config.println(cal2Y + "\tConversion: mm travel(axis TWO) per pix(Y)");
            config.close();
            this.printConfig();
        }
        catch (IOException e) {
			IJ.showMessage("ERROR", "Error Writing Microbeam Configuration to Disk: "+ e.getMessage());
		}
    }

    void printConfig() {
        String p1 = "Serial Port - Mirror Scanner:             ";
        String p2 = "Serial Port - Shutter:                    ";
        String p3 = "Conversion - microns per pixel:           ";
        String p4 = "Conversion - mm travel (axis 1) per pixel (X):     ";
        String p5 = "Conversion - mm travel (axis 1) per pixel (Y):     ";
        String p6 = "Conversion - mm travel (axis 2) per pixel (X):     ";
        String p7 = "Conversion - mm travel (axis 2) per pixel (Y):     ";
        IJ.log("CURRENT MICROBEAM CONFIGURATION\n\n");
        IJ.log(p1+"\t"+ this.mr_port);
        IJ.log(p2+"\t"+ this.sh_port);
        IJ.log( p3 +"\t"+ IJ.d2s(this.microns_per_pixel,6));
        IJ.log( p4 +"\t"+ IJ.d2s(this.cal1X,6));
        IJ.log( p5 +"\t"+ IJ.d2s(this.cal1Y,6));
        IJ.log( p6 +"\t"+ IJ.d2s(this.cal2X,6));
        IJ.log( p7 +"\t"+ IJ.d2s(this.cal2Y,6));
        IJ.log("");
    }

    boolean dialogConfig() {
		int nports = 8;
		String[] ports = new String[nports];
		for(int i =1;i<(nports+1);i++) {
			ports[i-1] = "COM"+i;
		}
		GenericDialog gd = new GenericDialog("Microbeam Configuration");
		gd.addMessage("SERIAL PORTS FOR MICROBEAM COMMUNICATIONS ");
		gd.addChoice("Mirror Controller: ",ports, this.mr_port);
		gd.addChoice("Shutter: ",ports, this.sh_port);
		gd.addMessage("______________________________________________");
		gd.addNumericField("Image scale (microns per pixel): ", this.microns_per_pixel, 3);
		gd.addMessage("______________________________________________");
		gd.addMessage("MIRROR ACTUATOR CALIBRATION");
		gd.addNumericField("mm travel on axis 1 per pixel (X): ",this.cal1X, 8);
		gd.addNumericField("mm travel on axis 1 per pixel (Y): ",this.cal1Y, 8);
		gd.addNumericField("mm travel on axis 2 per pixel (X): ",this.cal2X, 8);
		gd.addNumericField("mm travel on axis 2 per pixel (Y): ",this.cal2Y, 8);
		gd.showDialog();
		if (gd.wasCanceled()) {
			IJ.error("Plugin Canceled!");
			return false;
		}
		this.mr_port = gd.getNextChoice();
		this.sh_port = gd.getNextChoice();
		this.microns_per_pixel = gd.getNextNumber();
		this.cal1X = gd.getNextNumber();
		this.cal1Y = gd.getNextNumber();
		this.cal2X = gd.getNextNumber();
		this.cal2Y = gd.getNextNumber();
		return true;
    }


}
