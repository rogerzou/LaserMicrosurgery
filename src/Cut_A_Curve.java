/* Name: Cut_A_Curve.java
 * Project: Laser microdissection of dorsal closure
 * Version: 3.0
 * Inspired by: Shane Hutson (2003), Albert Mao (2004)
 * Author: Roger Zou
 * Date: 05/22/2015
 * Description: This plugin makes an incision in the shape of an arbitrary curve using a laser microbeam system.
 * The user defines the curve by making a polygon, freehand, segmented line, or freehand line selection on an 
 * image, and specifies the experimental parameters in a dialog box. This plugin will then command the microbeam
 * to trace the user-defined curve.
 */

import ij.*;
import ij.process.*;
import ij.gui.*;

import java.awt.*;

import ij.plugin.filter.*;
import laserj.*;

public class Cut_A_Curve implements PlugInFilter {

	private ImagePlus imp;
	private ImageWindow win;

	private double[] xpath;
	private double[] ypath;
	
	public int setup(String arg, ImagePlus imp) {
		IJ.log("\n\n\n@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n\nCUT_A_CURVE"
				+ "\n\n@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n\n\n");
		this.imp = imp;
		if (imp != null) {
			win = imp.getWindow();
			win.running = true;
		}
		return DOES_ALL + ROI_REQUIRED + NO_CHANGES;
	}

    public void run(ImageProcessor ip) {
		IJ.setColumnHeadings("");
		double maxVelocity = 0.2;
		double minVelocity = 0.01;

		// Gets the coordinates points that define the curve
		try {
			getXYCoordinates(imp);
		} catch (IllegalArgumentException e) {
			IJ.showMessage("Invalid selection", e.getMessage());
			return;
		}

		// Queries the user for experimental parameters
		double repdelay = 10;
		int ncuts = 1;
		double velocity = 0.2;
		int mag = 40;
		int zoom = 1;
		boolean resetPosition = true;
		GenericDialog gd = new GenericDialog("Experimental Parameters");
		gd.addNumericField("Total number of repeated incisions:", ncuts, 0);
		gd.addNumericField("Delay between repeated incisions (s):", repdelay, 1);
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
	   	repdelay = gd.getNextNumber() * 1000;
	   	resetPosition = gd.getNextBoolean();
		velocity = gd.getNextNumber();
		if (velocity > maxVelocity) {
			velocity = maxVelocity;
			IJ.log("Mirror Vectorial Velocity too High. Setting to Max Velocity = "+IJ.d2s(maxVelocity));
		}
		if (velocity < minVelocity) {
			velocity = minVelocity;
			IJ.log("Mirror Vectorial Velocity too Low. Setting to Min Velocity = "+IJ.d2s(minVelocity));
		}
		mag = (int) gd.getNextNumber();
		zoom = (int) gd.getNextNumber();

		// Initializes the microbeam system, stop if setup failed
		Microbeam microbeam = new Microbeam(Microbeam.CONFIG_FILENAME);
		if (!microbeam.isSetupOK()) return;

		/** LASER INCISION **/
		// store original image
		ip.snapshot();
		
		// Move microbeam to correct starting location
		microbeam.setMirrorVelocity(maxVelocity);
		microbeam.moveToPIXELS(xpath[0], ypath[0], ip, zoom);
		ip.moveTo((int) xpath[0], (int) ypath[0]);
		
		// Cut a curve by turning on shutter and moving microbeam to destination location
		microbeam.setMirrorVelocity(velocity);
		long t1 = System.currentTimeMillis();
		for(int i = 1; i <= ncuts; i++) {	// iterate over each repeated cut
			// make audio beep, wait 0.5s, open shutter, wait 0.1s.
			IJ.beep();
			IJ.wait(500);
			microbeam.openShutter();	
			IJ.wait(100);
			// compute traversal path
			int startIndex = 1;
			int endIndex = xpath.length-1;
			int dir = 1;
			if (!resetPosition) {
				if (i%2 == 1) { // Cut forward
					dir = 1;
					startIndex = 1;
					endIndex = xpath.length-1;
				} else {        // Cut backward
					dir = -1;
					startIndex = Math.max(0, xpath.length - 2);
					endIndex = -1;
				}
			}
			// traverse path
			for(int j = startIndex; j <= endIndex; j+=dir)  {	// traverse each segment of path
				if (!win.running) break;
				// make cut on segment
				microbeam.moveToPIXELS(xpath[j], ypath[j], ip, zoom);
				// draw physical line on display image that denotes cut
				ip.setColor(Color.white); ip.setLineWidth(3);
				ip.lineTo((int)xpath[j], (int)ypath[j]);
				imp.updateAndDraw();
				// wait 0.1s before next segment
				IJ.wait(100);
				// move pencil on display image to next segment
				ip.moveTo((int)xpath[j], (int)ypath[j]);
			}
			// close shutter and wait 0.1s
			microbeam.closeShutter();
			IJ.wait(100);
			// check if there is another cut to perform
			if (!win.running) break;
			if (i < ncuts) {
				if (resetPosition) {
					microbeam.moveToPIXELS(xpath[0], ypath[0], ip, zoom);
					ip.moveTo((int)xpath[0], (int)ypath[0]);
				}
				// delay between repeated incisions
				IJ.wait((int) Math.max(0, repdelay - 500));
				// clear display image for next incision
				ip.reset();
				imp.updateAndDraw();
			}
		}

		// Move microbeam back to home and clean up
		long t2 = System.currentTimeMillis();
		microbeam.setMirrorVelocity(maxVelocity);
		microbeam.moveToMM(0, 0);
		microbeam.off();
		IJ.log("\nDONE\n");
		double exposure = ((double)(t2-t1-1500)) / 1000.0;
		IJ.log("Total Exposure Time  = "+IJ.d2s(exposure,2)+" s");
	}

	public void getXYCoordinates(ImagePlus imp) {
	   	IJ.setColumnHeadings("X" + "Y");
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
		Rectangle r = p.getBounds();
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
