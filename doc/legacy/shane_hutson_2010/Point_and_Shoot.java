/* Name: Point_and_Shoot.java
 * Project: Laser microdissection of dorsal closure
 * Version: 3.0
 * Author: Shane Hutson
 * Maintained by: Albert Mao
 * Date: 11/19/2004
 * Description: This plugin targets one or more individual points with the microbeam.
 * The user specifies the points using the crosshair tool and the experimental parameters using a dialog box.
 * The plugin then moves the microbeam to each point and opens the shutter to allow the specified number of laser pulses to pass through.
 */

import ij.*;
import ij.process.*;
import ij.process.ImageStatistics.*;
import ij.gui.*;
import ij.text.TextPanel;
import java.awt.*;
import java.awt.event.*;
import ij.plugin.filter.*;
import java.io.*;
import java.util.*;
import javax.comm.*;
import plugins.LaserMicrosurgery.laserj.*;

public class Point_and_Shoot implements PlugInFilter {

	String ls;
	ImagePlus imp;
	ImageWindow win;
	ImageCanvas canvas;
	int numpoints;
	double[] x;
	double[] y;

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
		ip.snapshot();

		// Obtains the coordinates of the points from the Results window
		TextPanel resultsWindow = IJ.getTextPanel();
		try {
			numpoints = resultsWindow.getLineCount();
			if (numpoints <= 0) throw new IllegalArgumentException();
			x = new double[numpoints];
			y = new double[numpoints];
			for (int i = 0; i < numpoints; i++) {
				StringTokenizer line = new StringTokenizer(resultsWindow.getLine(i), "\t");
				line.nextToken();
				x[i] = Double.parseDouble(line.nextToken());
				y[i] = Double.parseDouble(line.nextToken());
			}
		} catch (RuntimeException e) {
			IJ.showMessage("Invalid point selection",
			"Use the crosshair tool to mark all desired points of ablation before executing Point and Shoot.\n" +
			"This plugin will ablate all points listed in the Results window.\n" +
			"Clear the window beforehand if necessary.");
			return;
		}

		// Displays a dialog box to allow the user to configure the experimental parameters
	    double reprate = 10;
	   	int npulses = 1;
		int zoom = 1;
		GenericDialog gd = new GenericDialog("Experimental Parameters");
		gd.addNumericField("Laser Repetition Rate (Hz): ", reprate, 0);
		gd.addNumericField("Number of Ablation Pulses: ", npulses, 0);
		gd.addNumericField("Digital Zoom of Image: ", zoom, 0);
		gd.addMessage("");
		gd.addMessage("Check targeting on image. Cancel plugin if incorrect.");
		gd.showDialog();
		if (gd.wasCanceled()) {
			IJ.error("PlugIn canceled!");
			ip.reset();
			imp.updateAndDraw();
			return;
		}
		reprate = gd.getNextNumber();
	   	npulses = (int) gd.getNextNumber();
	   	zoom = (int) gd.getNextNumber();
	    double period = (1000/reprate);	//period in ms
	    int opentime = (int) (period*(npulses-0.05 ));
		IJ.setColumnHeadings("");

		// Initializes the microbeam
		Microbeam microbeam = new Microbeam(Microbeam.CONFIG_FILENAME);
		if(!microbeam.isSetupOK()) return;

		// Moves to each point and exposes it for the specified number of pulses
		for(int j = 0; j < 3; j++) {
			IJ.beep();
			IJ.wait(500);
		}
		ip.setLineWidth(3);
		ip.setColor(Color.white);
		for (int i = 0; i < numpoints; i++) {
			microbeam.moveToPIXELS(x[i], y[i], ip, zoom);
			if (!win.running) break;
			microbeam.openShutter();
			IJ.wait(opentime);
			microbeam.closeShutter();
			ip.drawDot((int) x[i], (int) y[i]);
			imp.updateAndDraw();
		}

		// Resets the microbeam
		microbeam.moveToMM(0.0, 0.0);
		microbeam.off();
		IJ.write("\nDONE\n");
	}

}
