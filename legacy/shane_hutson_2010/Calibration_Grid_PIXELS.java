/* Name: Calibration_Grid_PIXELS.java
 * Project: Laser microdissection of dorsal closure
 * Version: 2.0
 * Author: Shane Hutson
 * Maintained by: Albert Mao
 * Date: 11/19/2004
 * Description: This plugin uses the microbeam to ablate grid of pixels.
 * The user can specify the grid size and spacing.
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

public class Calibration_Grid_PIXELS implements PlugInFilter {

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
		return DOES_ALL;
	}

    public void run(ImageProcessor ip) {
		double maxVelocity = 0.2;
		double minVelocity = 0.01;
		IJ.setColumnHeadings("");
		ip.setColor(Color.white);
		ip.setLineWidth(1);

		// Displays a dialog box for the user to configure the grid parameters
		double dp = 20;
		int nspots = 25;
		GenericDialog gd = new GenericDialog("Calibration Grid Parameters");
		gd.addNumericField("Spacing (pixels) between points on grid: ", dp, 0);
		gd.addNumericField("Grid Size (number of points along each axis): ", nspots, 0);
		gd.showDialog();
		if (gd.wasCanceled()) {
			IJ.error("PlugIn canceled!");
			return;
		}
		dp = gd.getNextNumber();
	   	nspots = (int) gd.getNextNumber();

		// Precalculates the coordinates of the grid points and stores them in xpath and ypath
		xpath = new double[nspots*nspots];
		ypath = new double[nspots*nspots];
		for(int j=0;j<nspots;j++) {
			for(int k=0;k<nspots;k++) {
				xpath[j + k*nspots]   = (j-(nspots-1)/2)*dp + ip.getWidth()/2;
				ypath[j + k*nspots]   = (k-(nspots-1)/2)*dp + ip.getHeight()/2;
			}
		}

		// Initializes the microbeam
		Microbeam microbeam = new Microbeam(Microbeam.CONFIG_FILENAME);
		if(!microbeam.isSetupOK()) return;

		// ********* MAKE THE CUTS *******************************************************
		ip.snapshot();
		microbeam.setMirrorVelocity(maxVelocity);
		long t1 = System.currentTimeMillis();
		for(int j=0;j<xpath.length;j++)  {
			if (!win.running) break;
			IJ.wait(300);
			microbeam.moveToPIXELS(xpath[j], ypath[j], ip, 1);
			IJ.wait(100);
			microbeam.openShutter();
			IJ.wait(100);
			microbeam.closeShutter();
			ip.drawDot((int)xpath[j], (int)ypath[j]);
			imp.updateAndDraw();
		}
		long t2 = System.currentTimeMillis();
		microbeam.moveToMM(0, 0);

		// Releases the microbeam and restores ImageJ's line width setting
		microbeam.off();
		ip.setLineWidth(1);
		IJ.write("\nDONE\n");
		double exposure = ((double)(t2-t1-1500))/1000.0;
		IJ.write("Total Time  = "+IJ.d2s(exposure,2)+" s");
	}

}
