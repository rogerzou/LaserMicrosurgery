package plugins.LaserMicrosurgery.laserj;

/* Name: Microbeam.java
 * Project: Laser microdissection of dorsal closure
 * Version: 2.2
 * Author: Shane Hutson
 * Maintained by: Albert Mao
 * Date: 11/19/2004
 * Description: This class represents a laser microbeam.
 * This abstraction of a microbeam uses the concept of a current (x,y) position.
 * This position can be moved in real coordinates, or in coordinates scaled to correspond to the pixels of an image.
 * The microbeam also has a shutter that can be open and closed.
 */

import java.awt.*;
import java.util.*;
import java.io.*;
import ij.*;
import ij.gui.*;
import ij.io.*;
import ij.process.*;
import ij.text.*;
import ij.plugin.*;


public class Microbeam implements Serializable {

    public static final String CONFIG_FILENAME = "plugins\\LaserMicrosurgery\\Microbeam.txt";

    String mr_port, sh_port;
    long timeOut = 60000L;      // 60 seconds
    double microns_per_pixel;
    double cal1X;
    double cal1Y;
    double cal2X;
    double cal2Y;
    transient Mirror mirror;
    transient Shutter shutter;
    boolean setupOK;

	// Initializes using a dialog box
    public Microbeam() {
        if(this.dialogConfig()) {
            this.printConfig();
            this.shutter = new Shutter(this.sh_port);
            this.mirror = new Mirror(this.mr_port, this.timeOut);
            IJ.write("\nMICROBEAM INITIALIZATION COMPLETE\n\n");
        }
        else {
            IJ.write("\nMICROBEAM INITIALIZATION CANCELED\n\n");
            setupOK = false;
        }
    }

	// Initializes using a text file
    public Microbeam(String textfile) {
		setupOK = true;
		BufferedReader config;
		try {
			config = new BufferedReader(new FileReader(textfile));
		} catch (FileNotFoundException e) {
			setupOK = false;
			throw new IllegalArgumentException("Microbeam config file not found");
		}
		try {
			mr_port = new StringTokenizer(config.readLine()).nextToken();
			mirror = new Mirror(mr_port, timeOut);
			sh_port = new StringTokenizer(config.readLine()).nextToken();
			shutter = new Shutter(sh_port);
			microns_per_pixel = Double.parseDouble(new StringTokenizer(config.readLine()).nextToken());
			cal1X = Double.parseDouble(new StringTokenizer(config.readLine()).nextToken());
			cal1Y = Double.parseDouble(new StringTokenizer(config.readLine()).nextToken());
			cal2X = Double.parseDouble(new StringTokenizer(config.readLine()).nextToken());
			cal2Y = Double.parseDouble(new StringTokenizer(config.readLine()).nextToken());
			config.close();
		} catch (IOException e) {
			setupOK = false;
			throw new IllegalArgumentException("Error reading microbeam config file");
		} catch (NumberFormatException e) {
			setupOK = false;
			throw new IllegalArgumentException("Microbeam config file is not formatted properly");
		}
    }

    protected void finalize() {
        mirror.mirrorPort.close();
        shutter.shutterPort.close();
    }

    public void moveToPIXELS(double xpix, double ypix, ImageProcessor ip, int zoom) {
		IJ.write("MOVING MICROBEAM TO POSITION (PIXELS): "+IJ.d2s(xpix,0)+", "+IJ.d2s(ypix,0));
		int w = ip.getWidth();
		int h = ip.getHeight();
		double axis1 = (xpix - w/2)*cal1X / zoom + (ypix - h/2)*cal1Y / zoom;
		double axis2 = (xpix - w/2)*cal2X / zoom + (ypix - h/2)*cal2Y / zoom;
		mirror.moveTo(axis1, axis2);
    }

    public void off() {
        IJ.write("\nRELEASE MICROBEAM CONTROLS");
        mirror.off();
        mirror.mirrorPort.close();
        shutter.shutterPort.close();
    }

    public void moveToMM(double x, double y) {
        IJ.write("MOVING MICROBEAM TO POSITION (MM): "+IJ.d2s(x,4)+", "+IJ.d2s(y,4));
        mirror.moveTo(x,y);
    }

    public void arcmoveToMM(double x0, double y0, double degrees) {
        IJ.write("MOVING MICROBEAM ALONG ARC CENTERED AT (MM): "+IJ.d2s(x0,4)+", "+IJ.d2s(y0,4));
        mirror.arcmoveTo(x0,y0,degrees);
    }
    public void setMirrorVelocity(double v) {
        IJ.write("SET MIRROR VELOCITY TO "+IJ.d2s(v,4));
        mirror.setVelocity(v);
    }

    public void defineMirrorHome() {
        boolean sure = IJ.showMessageWithCancel("ARE YOU SURE?","Current mirror position will be set to home (0,0).");
        if(sure) {
            IJ.write("SET CURRENT MIRROR POSITION TO HOME (0, 0) ");
            mirror.defineHome();
        }
        else IJ.write("CANCELED: CURRENT MIRROR POSITION NOT RESET TO HOME (0, 0) ");
    }

    public void openShutter() {
        shutter.open();
        IJ.write("OPEN SHUTTER");
    }

    public void closeShutter() {
        shutter.close();
        IJ.write("CLOSE SHUTTER");
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
        IJ.write("CURRENT MICROBEAM CONFIGURATION\n\n");
        IJ.write(p1+"\t"+ this.mr_port);
        IJ.write(p2+"\t"+ this.sh_port);
        IJ.write( p3 +"\t"+ IJ.d2s(this.microns_per_pixel,6));
        IJ.write( p4 +"\t"+ IJ.d2s(this.cal1X,6));
        IJ.write( p5 +"\t"+ IJ.d2s(this.cal1Y,6));
        IJ.write( p6 +"\t"+ IJ.d2s(this.cal2X,6));
        IJ.write( p7 +"\t"+ IJ.d2s(this.cal2Y,6));
        IJ.write("");
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
