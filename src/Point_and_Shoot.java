/* Name: Point_and_Shoot.java
 * Project: Laser microdissection of dorsal closure
 * Version: 3.0
 * Inspired by: Shane Hutson (2003), Albert Mao (2004)
 * Author: Roger Zou
 * Date: 05/22/2015
 * Description: This plugin targets one or more individual points with the microbeam.
 * The user specifies the points using the crosshair tool and the experimental parameters using a dialog box.
 * The plugin then moves the microbeam to each point and opens the shutter to allow the specified number of 
 * laser pulses to pass through.
 */

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.text.TextPanel;
import ij.plugin.filter.*;

import java.awt.*;
import java.util.*;

import laserj.*;

public class Point_and_Shoot implements PlugInFilter {

	private ImagePlus imp;
	private ImageWindow win;

	public int setup(String arg, ImagePlus imp) {
		IJ.log("\n\n\n@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n\nPOINT_AND_SHOOT"
				+ "\n\n@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n\n\n");
		this.imp = imp;
		if (imp != null) {
			win = imp.getWindow();
			win.running = true;
		}
		return DOES_ALL + NO_CHANGES;
	}

    public void run(ImageProcessor ip) {
		int npts;
		double[] x, y;
		
		// Obtains the coordinates of the points from the Results window
		TextPanel resultsWindow = IJ.getTextPanel();
		npts = resultsWindow.getLineCount();
		if (npts <= 0) {
			IJ.log("ERROR: no point coordinates found.");
			IJ.log("INSTRUCTIONS: Use the crosshair tool to mark all desired points of ablation"
					+ "before executing Point and Shoot.\n This plugin will ablate "
					+ "all points listed in the Results window.\n Clear the window "
					+ "beforehand if necessary.");
			IJ.log("\nDONE\n");
			return;
		}
		x = new double[npts];
		y = new double[npts];
		for (int i = 0; i < npts; i++) {
			StringTokenizer line = new StringTokenizer(resultsWindow.getLine(i), "\t");
			line.nextToken();
			x[i] = Double.parseDouble(line.nextToken());
			y[i] = Double.parseDouble(line.nextToken());
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
			IJ.error("Point_and_Shoot PlugIn canceled!");
			return;
		}
		reprate = gd.getNextNumber();
	   	npulses = (int) gd.getNextNumber();
	   	zoom = (int) gd.getNextNumber();
	    double period = 1000 / reprate;	//period in ms
	    int opentime = (int) ( period * (npulses-0.05) );

		// Initializes the microbeam
		Microbeam microbeam = new Microbeam(Microbeam.CONFIG_FILENAME);
		if (!microbeam.isSetupOK()) return;

		/** LASER INCISION **/
		// Iterate over each point, and make a point ablation
		IJ.wait(500);
		for (int i = 0; i < npts; i++) {
			if (!win.running) break;
			
			// move microbeam to position i
			microbeam.moveToPIXELS(x[i], y[i], ip, zoom);
			microbeam.openShutter();
			IJ.wait(opentime);
			microbeam.closeShutter();
			
			// draw point of ablation on display image
			ip.setColor(Color.white); ip.setLineWidth(3);		// set line width and color
			ip.drawDot((int) x[i], (int) y[i]);
			imp.updateAndDraw();
		}

		// Moves microbeam to home and turns it off
		microbeam.moveToMM(0.0, 0.0);
		microbeam.off();
		IJ.log("\nDONE\n");
	}

}
