/* Name: Test_Patterns.java
 * Project: Laser microdissection of dorsal closure
 * Version: 2.0
 * Author: Shane Hutson
 * Maintained by: Albert Mao
 * Date: 11/19/2004
 * Description: This plugin uses the microbeam to ablate a test pattern.
 * The user can choose between a test pattern of horizontal, vertical, or radial lines.
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
import plugins.LaserMicrosurgery.laserj.*;

public class Test_Patterns implements PlugInFilter {

	static final double maxVelocity = 0.2;
	static final double minVelocity = 0.01;

	String ls;
	ImagePlus imp;
	ImageWindow win;
	ImageCanvas canvas;
	double[] xpath;
	double[] ypath;
	double velocity = 0.2;
	int zoom = 1;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		if (imp != null) {
			win = imp.getWindow();
			win.running = true;
		}
		ls = System.getProperty("line.separator");
		return DOES_ALL + NO_CHANGES;
	}

    public void run(ImageProcessor ip) {
		IJ.setColumnHeadings("");
		ip.setColor(255);
		ip.setLineWidth(1);
    	int ncuts = 1;
		int period = 10;

		// Displays a dialog box for the user to choose the test pattern, precalculates the coordinates of the trajectory, and saves them in xpath and ypath
		String[] patterns =  {"Horizontal Lines","Vertical Lines", "Radial Lines"};
		GenericDialog gd1 = new GenericDialog("Test Patterns");
		gd1.addChoice("Choose a microbeam test pattern: ",patterns, patterns[0]);
		gd1.showDialog();
		if (gd1.wasCanceled()) {
			IJ.error("No incisions made. PlugIn canceled!");
			return;
		}
		int pattern_index = gd1.getNextChoiceIndex();
		if(pattern_index == 0) if(!horizontal_lines(ip)) return;
		if(pattern_index == 1) if(!vertical_lines(ip)) return;
		if(pattern_index == 2) if(!radial_lines(ip)) return;

		// Initializes the microbeam
		Microbeam microbeam = new Microbeam(Microbeam.CONFIG_FILENAME);
		if(!microbeam.isSetupOK()) return;

		// Directs the microbeam to the positions saved in xpath and ypath and draws the trajectory on the image
		ip.snapshot();
		microbeam.setMirrorVelocity(maxVelocity);
		microbeam.moveToPIXELS(xpath[0], ypath[0], ip, zoom);
		ip.moveTo((int)xpath[0], (int)ypath[0]);
		microbeam.setMirrorVelocity(velocity);
		long t1 = System.currentTimeMillis();
		for(int i=1; i<=ncuts;i++) {
			for(int j=0; j<3; j++) {
				IJ.beep();
				IJ.wait(500);
			}
			microbeam.openShutter();
			IJ.wait(100);
			for(int j=1;j<xpath.length;j++)  {
				if (!win.running) break;
				microbeam.moveToPIXELS(xpath[j], ypath[j], ip, zoom);
				IJ.wait(100);
				ip.lineTo((int)xpath[j], (int)ypath[j]);
				imp.updateAndDraw();
				ip.moveTo((int)xpath[j], (int)ypath[j]);
			}
			microbeam.closeShutter();
			IJ.wait(100);
			if (!win.running) break;
			if (i<ncuts) {
				microbeam.moveToPIXELS(xpath[0], ypath[0], ip, zoom);
				ip.moveTo((int)xpath[0], (int)ypath[0]);
				IJ.wait( (int)(period-3*500));
				ip.reset();
				imp.updateAndDraw();
			}
		}
		long t2 = System.currentTimeMillis();
		microbeam.setMirrorVelocity(maxVelocity);

		// Returns to home position (radial pattern already ends at (0,0))
		if(pattern_index != 2) microbeam.moveToMM(0, 0);

		// Resets the microbeam and restores ImageJ's line width setting
		microbeam.off();
		ip.setLineWidth(1);
		IJ.write("\nDONE\n");
		double exposure = ((double)(t2-t1-1500))/1000.0;
		IJ.write("Total Exposure Time  = "+IJ.d2s(exposure,2)+" s");
	}

	boolean horizontal_lines(ImageProcessor ip) {
		int dp = 50;
		int nlines = 11;
		GenericDialog gd = new GenericDialog("Horizontal Lines");
		gd.addNumericField("Vectorial Velocity of Mirror Drive :",velocity,3);
		gd.addNumericField("Digital Zoom of Image :",zoom,0);
		gd.addNumericField("Spacing (pixels) between horizontal lines :",dp,0);
		gd.addNumericField("Number of lines to cut (will add one if even number): ",nlines,0);
		gd.addMessage("After pressing OK,\n shutter will open 0.5 s after audible warning.");
		gd.showDialog();
		if (gd.wasCanceled()) {
			IJ.error("No incision made. PlugIn canceled!");
			return false;
		}
		velocity = gd.getNextNumber();
		zoom = (int)gd.getNextNumber();
	    	dp = (int)gd.getNextNumber();
	   	nlines = (int)gd.getNextNumber();
		if (velocity>maxVelocity) { velocity = maxVelocity; IJ.write("Mirror Vectorial Velocity too High. Setting to Max Velocity = "+IJ.d2s(maxVelocity));}
		if (velocity<minVelocity) { velocity = minVelocity; IJ.write("Mirror Vectorial Velocity too Low. Setting to Min Velocity = "+IJ.d2s(minVelocity));}
		if(dp<5) {IJ.showMessage("Line spacing too small. Canceling Plugin!"); return false;}
		if(nlines<1) {IJ.showMessage("Number of lines less than one. Canceling Plugin!"); return false;}
		if(zoom<=0) {IJ.showMessage("Digital zoom cannot be less than or equal to 0. Canceling Plugin!"); return false;}
		if((nlines%2)==0) {nlines++;}
		xpath = new double[2*nlines];
		ypath = new double[2*nlines];
		for(int j=0;j<nlines;j++)  {
			xpath[2*j]   = ip.getWidth()*(j%2);
			xpath[2*j+1] = ip.getWidth()*((j+1)%2);
			ypath[2*j]      = (j - (nlines-1)/2)*dp + ip.getWidth()/2;
			ypath[2*j+1] = (j - (nlines-1)/2)*dp + ip.getWidth()/2;
		}
		return true;
	}

	boolean vertical_lines(ImageProcessor ip) {
		int dp = 50;
		int nlines = 11;
		GenericDialog gd = new GenericDialog("Vertical Lines");
		gd.addNumericField("Vectorial Velocity of Mirror Drive :",velocity,3);
		gd.addNumericField("Digital Zoom of Image :",zoom,0);
		gd.addNumericField("Spacing (pixels) between vertical lines :",dp,0);
		gd.addNumericField("Number of lines to cut (will add one if even number): ",nlines,0);
		gd.addMessage("After pressing OK,\n shutter will open 0.5 s after audible warning.");
		gd.showDialog();
		if (gd.wasCanceled()) {
			IJ.error("No incision made. PlugIn canceled!");
			return false;
		}
		velocity = gd.getNextNumber();
		zoom = (int)gd.getNextNumber();
	    dp = (int)gd.getNextNumber();
	   	nlines = (int)gd.getNextNumber();
		if (velocity>maxVelocity) { velocity = maxVelocity; IJ.write("Mirror Vectorial Velocity too High. Setting to Max Velocity = "+IJ.d2s(maxVelocity));}
		if (velocity<minVelocity) { velocity = minVelocity; IJ.write("Mirror Vectorial Velocity too Low. Setting to Min Velocity = "+IJ.d2s(minVelocity));}
		if(dp<5) {IJ.showMessage("Line spacing too small. Canceling Plugin!"); return false;}
		if(nlines<1) {IJ.showMessage("Number of lines less than one. Canceling Plugin!"); return false;}
		if(zoom<=0) {IJ.showMessage("Digital zoom cannot be less than or equal to 0. Canceling Plugin!"); return false;}
		if((nlines%2)==0) {nlines++;}
		xpath = new double[2*nlines];
		ypath = new double[2*nlines];
		for(int j=0;j<nlines;j++)  {
			ypath[2*j]   = ip.getHeight()*(j%2);
			ypath[2*j+1] = ip.getHeight()*((j+1)%2);
			xpath[2*j]      = (j - (nlines-1)/2)*dp + ip.getHeight()/2;
			xpath[2*j+1] = (j - (nlines-1)/2)*dp + ip.getHeight()/2;
		}
		return true;
	}

	boolean radial_lines(ImageProcessor ip) {
		int nlines = 4;
		GenericDialog gd = new GenericDialog("Radial Lines");
		gd.addNumericField("Vectorial Velocity of Mirror Drive :",velocity,3);
		gd.addNumericField("Digital Zoom of Image :",zoom,0);
		gd.addNumericField("Number of spokes to cut: ",nlines,0);
		gd.addMessage("Note that 4 spokes is a cross and 8 is an asterisk.");
		gd.addMessage("After pressing OK,\n shutter will open 0.5 s after audible warning.");
		gd.showDialog();
		if (gd.wasCanceled()) {
			IJ.error("No incision made. PlugIn canceled!");
			return false;
		}
		velocity = gd.getNextNumber();
		zoom = (int)gd.getNextNumber();
	   	nlines = (int)gd.getNextNumber();
		if (velocity>maxVelocity) { velocity = maxVelocity; IJ.write("Mirror Vectorial Velocity too High. Setting to Max Velocity = "+IJ.d2s(maxVelocity));}
		if (velocity<minVelocity) { velocity = minVelocity; IJ.write("Mirror Vectorial Velocity too Low. Setting to Min Velocity = "+IJ.d2s(minVelocity));}
		if(nlines<1) {IJ.showMessage("Number of lines less than one. Canceling Plugin!"); return false;}
		if(zoom<=0) {IJ.showMessage("Digital zoom cannot be less than or equal to 0. Canceling Plugin!"); return false;}
		xpath = new double[2*nlines];
		ypath = new double[2*nlines];
		double r;
		if(ip.getWidth()>=ip.getHeight()) r = ip.getWidth()/2;
		else r = ip.getHeight()/2;
		IJ.write("Radius of Incribed Circle: "+IJ.d2s(r));
		for(int j=0;j<nlines;j++)  {
			double theta = j*(2*Math.PI/nlines);
			IJ.write("theta = "+IJ.d2s(theta));
			ypath[2*j]   = (ip.getHeight()/2)+ r*Math.sin(theta);
			ypath[2*j+1] = (ip.getHeight()/2);
			xpath[2*j]      = (ip.getWidth()/2)+ r*Math.cos(theta);
			xpath[2*j+1] =  (ip.getWidth()/2);
		}
		return true;
	}
}
