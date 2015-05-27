/* Name: Cut_A_Curve.java
 * Project: Laser microdissection of dorsal closure
 * Version: 2.1
 * Author: Shane Hutson
 * Maintained by: Albert Mao
 * Date: 11/19/2004
 * Description: This plugin makes an incision in the shape of an arbitrary curve using a laser microbeam system.
 * The user defines the curve by making a polygon, freehand, segmented line, or freehand line selection on an image, and specifies the experimental parameters in a dialog box.
 * This plugin will then command the microbeam to trace the user-defined curve.
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

public class Cut_A_Curve implements PlugInFilter {

	String ls;
	ImagePlus imp;
	ImageWindow win;
	ImageCanvas canvas;

	double[] xpath;
	double[] ypath;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		if (imp!=null) {
			win = imp.getWindow();
			win.running = true;
		}
		ls = System.getProperty("line.separator");
		return DOES_ALL+ROI_REQUIRED+NO_CHANGES;
	}

    public void run(ImageProcessor ip) {
		double maxVelocity = 0.2;
		double minVelocity = 0.01;
		IJ.setColumnHeadings("");
		ip.setColor(Color.white); ip.setLineWidth(3);

		// Gets the coordinates points that define the curve
		try {
			getXYCoordinates(imp);
		} catch (IllegalArgumentException e) {
			IJ.showMessage("Invalid selection", e.getMessage());
			return;
		}

		// Queries the user for experimental parameters
		double period = 10;
		int ncuts = 1;
		double velocity = 0.2;
		int zoom = 1;
		int mag = 40;
		boolean resetPosition = true;
		GenericDialog gd = new GenericDialog("Experimental Parameters");
		gd.addNumericField("Total number of repeated incisions:", ncuts, 0);
		gd.addNumericField("Delay between repeated incisions (s):", period, 1);
		gd.addCheckbox("Reset position between repeated incisions", resetPosition);
		gd.addNumericField("Vectorial velocity of mirror drive:", velocity, 3);
		gd.addMessage("");
		gd.addNumericField("Magnification Factor of Objective:", mag, 0);
		gd.addNumericField("Zoom Factor of Image:", zoom, 0);
		gd.addMessage("");
		gd.addMessage("After pressing OK,\n shutter will open 0.5 s after audible warning.");
		gd.showDialog();
		if (gd.wasCanceled()) {
			IJ.error("No incision made. PlugIn canceled!");
			return;
		}
		ncuts = (int) gd.getNextNumber();
	   	period = gd.getNextNumber() * 1000;
	   	resetPosition = gd.getNextBoolean();
		velocity = gd.getNextNumber();
		if (velocity>maxVelocity) {
			velocity = maxVelocity;
			IJ.write("Mirror Vectorial Velocity too High. Setting to Max Velocity = "+IJ.d2s(maxVelocity));
		}
		if (velocity<minVelocity) {
			velocity = minVelocity;
			IJ.write("Mirror Vectorial Velocity too Low. Setting to Min Velocity = "+IJ.d2s(minVelocity));
		}
		mag = (int) gd.getNextNumber();
		zoom = (int) gd.getNextNumber();

		// Initializes the microbeam system
		Microbeam microbeam = new Microbeam(Microbeam.CONFIG_FILENAME);
		if(!microbeam.isSetupOK()) return;

		// Sends commands to the microbeam to cut the curve
		ip.snapshot();
		microbeam.setMirrorVelocity(maxVelocity);
		microbeam.moveToPIXELS(xpath[0], ypath[0], ip, zoom);
		ip.moveTo((int) xpath[0], (int) ypath[0]);
		microbeam.setMirrorVelocity(velocity);
		long t1 = System.currentTimeMillis();
		for(int i = 1; i <= ncuts; i++) {
			for(int j = 0; j < 3; j++) {
				IJ.beep();
				IJ.wait(500);
			}
			microbeam.openShutter();
			IJ.wait(100);
			int firstIndex = 1;
			int terminateIndex = xpath.length;
			int dir = 1;
			if (!resetPosition) {
				if (i%2 == 1) { // Cut forward
					dir = 1;
					firstIndex = 1;
					terminateIndex = xpath.length;
				} else {        // Cut backward
					dir = -1;
					firstIndex = Math.max(0, xpath.length - 2);
					terminateIndex = -1;
				}
			}
			for(int j = firstIndex; j != terminateIndex; j+=dir)  {
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
				if (resetPosition) {
					microbeam.moveToPIXELS(xpath[0], ypath[0], ip, zoom);
					ip.moveTo((int)xpath[0], (int)ypath[0]);
				}
				IJ.wait((int) Math.max(0, period - 3*500));
				ip.reset();
				imp.updateAndDraw();
			}
		}

		// Releases the microbeam and cleans up
		long t2 = System.currentTimeMillis();
		microbeam.setMirrorVelocity(maxVelocity);
		microbeam.moveToMM(0, 0);
		microbeam.off();
		ip.setLineWidth(1);
		IJ.write("\nDONE\n");
		double exposure = ((double)(t2-t1-1500))/1000.0;
		IJ.write("Total Exposure Time  = "+IJ.d2s(exposure,2)+" s");
	}

	public void getXYCoordinates(ImagePlus imp) {
	   	IJ.setColumnHeadings("X"+"Y");
		Roi roi = imp.getRoi();
		if (roi==null)
			throw new IllegalArgumentException("ROI required");
		if (!(roi instanceof PolygonRoi))
			throw new IllegalArgumentException("Polygon, freehand, segmented line, \nor freehand line selection required.");
		PolygonRoi p = (PolygonRoi)roi;
		getPolygonPath(p);
	}

	void getPolygonPath(PolygonRoi p) {
		int tool = Toolbar.getToolId();
		boolean closed = ((tool==Toolbar.POLYGON) | (tool==Toolbar.FREEROI));
		int n = p.getNCoordinates();
		int[] x = p.getXCoordinates();
		int[] y = p.getYCoordinates();
		Rectangle r = p.getBoundingRect();
		int xbase = r.x;
		int ybase = r.y;
		if (closed) {
			n++;
			xpath = new double[n];
			ypath = new double[n];
			for (int i=0; i<(n-1); i++) {
				xpath[i] = 1.0*(r.x + x[i]);
				ypath[i] =1.0*( r.y + y[i]);
			}
			// Duplicates the first point as the last point to close the curve
			xpath[n-1] = r.x + x[0];  ypath[n-1] = r.y + y[0];
		}
		else {
			xpath = new double[n];
			ypath = new double[n];
			for (int i=0; i<n; i++) {
				xpath[i] = 1.0*(r.x + x[i]);
				ypath[i] = 1.0*(r.y + y[i]);
			}
		}
	}

}
