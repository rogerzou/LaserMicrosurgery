/* Name: Cut_Both_Canthi.java
 * Project: Laser microdissection of dorsal closure
 * Version: 2.1
 * Author: Shane Hutson
 * Maintained by: Albert Mao
 * Date: 11/19/2004
 * Description: This plugin uses the microbeam makes an incision in each canthus of the amnioserosa.
 * The user specifies the position of the amnioserosa by using the straight line selection tool to draw a line from canthus to canthus, and specifies the experimental parameters in a dialog box.
 * This plugin will then command the microbeam to make the appropriate incisions.
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

public class Cut_Both_CanthiNEW implements PlugInFilter {

	String ls;
	ImagePlus imp;
	ImageWindow win;
	ImageCanvas canvas;

	double[] xpath, ypath, linex,liney;

	int zoom =1;
	double ROBJ;
	double sep = 40;
	int ncuts = 1;
	double period = 30;
	boolean resetPosition = true;
	double lcuts = 10;
	double velocity = 0.1;
	int mag = 40;
	boolean parallel;
	double maxVelocity = 0.2;
	double minVelocity = 0.01;
	boolean firstPass=true;

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
		
		IJ.setColumnHeadings("");
		ip.setColor(Color.white);
		ip.setLineWidth(3);

		// Initializes xpath and ypath with the four endpoints of the two line cuts
		try {
			getLineCoordinates(imp);
		} catch (IllegalArgumentException e) {
			IJ.showMessage("Invalid selection", e.getMessage());
			return;
		}

		
		boolean	cut_again = getUserInfo();

		
		
		Microbeam microbeam = new Microbeam(Microbeam.CONFIG_FILENAME);
		if(!microbeam.isSetupOK()) return;


		ROBJ = microbeam.get_microns_per_pixel();


		// Checks if the two incisions would overlap
		if(pathLength()<2*lcuts) {
			IJ.error("Defined Path ("+IJ.d2s(pathLength(),1) +" microns) must be >"+IJ.d2s(2*lcuts,1)+" microns. No incision made. PlugIn canceled!");
			return;
		}
		cutBothEndsPath(lcuts, parallel);

		
		firstPass = false; 
		
		int counter = 1;
		while(cut_again) {


			IJ.write("Counter: "+counter);
			IJ.write("xpath ypath");
			
for (int i=0; i<xpath.length; i++) {
			
			IJ.write(IJ.d2s(xpath[i],2)+"       "+IJ.d2s(ypath[i],2));
		}
			IJ.write("linex liney");
for (int i=0; i<linex.length; i++) {
			
			IJ.write(IJ.d2s(linex[i],2)+"       "+IJ.d2s(liney[i],2));
		}



			long starttime = System.currentTimeMillis();
			ip.snapshot();
			microbeam.setMirrorVelocity(maxVelocity);
			IJ.wait(500);
			
//if (!win.running) break;

			// Start at point 0
			int j = 0;
			if (!resetPosition && counter % 2 == 0) j = 3-j;
			microbeam.moveToPIXELS(xpath[j], ypath[j], ip, zoom);
			ip.moveTo((int)xpath[j], (int)ypath[j]);
			microbeam.setMirrorVelocity(velocity);
			IJ.wait(500);
			for(int i=0; i<3; i++) {
				IJ.beep();
				IJ.wait(500);
			}
			long t1 = System.currentTimeMillis();
			microbeam.openShutter();
			IJ.wait(100);

			// Cut to point 1
			j = 1;
			if (!resetPosition && counter % 2 == 0) j = 3-j;
			microbeam.moveToPIXELS(xpath[j], ypath[j], ip, zoom);
			
//if (!win.running) break;
			IJ.wait(300);
			ip.lineTo((int)xpath[j], (int)ypath[j]);
			imp.updateAndDraw();
			ip.moveTo((int)xpath[j], (int)ypath[j]);
			microbeam.closeShutter();
			t1 = System.currentTimeMillis()- t1;
			long t2 = System.currentTimeMillis();
			IJ.wait(500);
			microbeam.setMirrorVelocity(maxVelocity);
			IJ.wait(500);

			// Move to point 2
			j = 2;
			if (!resetPosition && counter % 2 == 0) j = 3-j;
			microbeam.moveToPIXELS(xpath[j], ypath[j], ip, zoom);
			
//if (!win.running) break;
			ip.moveTo((int)xpath[j], (int)ypath[j]);
			IJ.wait(500);
			microbeam.setMirrorVelocity(velocity);
			IJ.wait(500);
			t2 = System.currentTimeMillis()- t2;
			long t3 = System.currentTimeMillis();
			microbeam.openShutter();
			IJ.wait(100);

			// Cut to point 3
			j = 3;
			if (!resetPosition && counter % 2 == 0) j = 3-j;
			microbeam.moveToPIXELS(xpath[j], ypath[j], ip, zoom);
			
//if (!win.running) break;
			IJ.wait(100);
			ip.lineTo((int)xpath[j], (int)ypath[j]);
			imp.updateAndDraw();
			ip.moveTo((int)xpath[j], (int)ypath[j]);
			microbeam.closeShutter();
			t3 = System.currentTimeMillis()- t3;

			// Display timing information
			double exposure1 = ((double)(t1))/1000.0;
			double exposure2 = ((double)(t3))/1000.0;
			double traveltime = ((double)(t2))/1000.0;
			IJ.write("\n\nIncision Pair #"+counter);
			IJ.write("Exposure Time (Cut #1) = "+IJ.d2s(exposure1,2)+" s");
			IJ.write("Exposure Time (Cut #2) = "+IJ.d2s(exposure2,2)+" s");
			IJ.write("Travel Time Between Cuts = "+IJ.d2s(traveltime,2)+" s\n\n");
			
//if (!win.running) break;

			while((System.currentTimeMillis()- starttime)<(period*1000)) {
				IJ.showStatus("PAUSE BETWEEN CUTS - WAITING");
				IJ.wait(800);
				IJ.showStatus("");
				IJ.wait(200);
			}
			if (counter >= ncuts) {
				IJ.beep();
				GenericDialog cut = new GenericDialog("CONTINUE?");
				cut.addMessage("OK will cut the sample one more time with the same trajectory.");
				cut.addMessage("CANCEL to end cutting and return mirrors to 0, 0.");
				cut.showDialog();
				cut_again = !cut.wasCanceled();
			}
			counter++;

			if (cut_again) {

				getLineCoordinates(imp);
				cutBothEndsPath(lcuts, parallel);

				ip.reset();
				imp.updateAndDraw();
			}
		}

		// Resets and releases the microbeam and restores ImageJ's line width setting
		microbeam.setMirrorVelocity(maxVelocity);
		IJ.wait(500);
		microbeam.moveToMM(0, 0);
		IJ.wait(500);
		microbeam.off();
		ip.setLineWidth(1);
		IJ.write("\nDONE\n");
	}

	public void getLineCoordinates(ImagePlus imp) {
		Roi roi = imp.getRoi();
		
		try{
		Line lroi = (Line)roi;
		linex = new double[2];
		liney = new double[2];
		linex[0] = 1.0*lroi.x1;   liney[0] = 1.0*lroi.y1;
		linex[1] = 1.0*lroi.x2;   liney[1] = 1.0*lroi.y2;

		// Sorts the endpoints from left to right
		if (linex[1] < linex[0]) {
			double temp = linex[1];
			linex[1] = linex[0];
			linex[0] = temp;
			temp = liney[1];
			liney[1] = liney[0];
			liney[0] = temp;
		}
		}catch(ClassCastException e)
		{
			if(firstPass)
				throw new IllegalArgumentException("Straight line selection required.");
		}
	}

	public double pathLength() {
		double L = 0.0;
		for (int i=1; i<linex.length; i++) {
			L += Math.sqrt((linex[i]-linex[i-1])*(linex[i]-linex[i-1])+(liney[i]-liney[i-1])*(liney[i]-liney[i-1]));
		}
		//IJ.write(IJ.d2s(L,3));
		return L*ROBJ/zoom;
	}

	public void cutBothEndsPath(double dL, boolean parallel) {
		dL = dL*zoom/ROBJ;		//convert to pixels
		double dx;
		double dy;
		if (linex[0] != linex[1]) {
			double m = (liney[1]-liney[0])/(linex[1]-linex[0]);
			dx = dL/(Math.sqrt(1 + m*m));
			dy = dx*m;
		} else {
			dx = 0;
			dy = dL;
		}
		 xpath = new double[4];
		 ypath = new double[4];

		if(parallel) {
			xpath[0] = linex[0] ; ypath[0] = liney[0];
			xpath[1] = linex[0] + dx ; ypath[1] = liney[0]+ dy;
			xpath[2] = linex[1] -  dx ; ypath[2] = liney[1] - dy;
			xpath[3] = linex[1] ; ypath[3] = liney[1];
		} else {
			double dxtemp = dx/2; double dytemp=dy/2;
			dx = -dytemp;
			dy = dxtemp;
			xpath[0] = linex[0]  - dx;  ypath[0] = liney[0] - dy;
			xpath[1] = linex[0] + dx ; ypath[1] = liney[0]+ dy;
			xpath[2] = linex[1]  - dx ; ypath[2] = liney[1] - dy;
			xpath[3] = linex[1] + dx;  ypath[3] = liney[1]+ dy;
		}
		IJ.write("ENDPOINTS OF TWO LINES TO BE CUT\n");
		
		for (int i=0; i<xpath.length; i++) {
			
			IJ.write(IJ.d2s(xpath[i],2)+"       "+IJ.d2s(ypath[i],2));
		}
		IJ.write("");
	}
	public boolean getUserInfo()
	{


		// Displays a dialog for the user to configure the experimental parameters
	       
		GenericDialog gd = new GenericDialog("Experimental Parameters");
		gd.addCheckbox("Cut Parallel to Line (unchecked is perpendicular)?",true);
		gd.addNumericField("Number of cuts: ", ncuts, 0);
		gd.addNumericField("Time Between Cuts (s): ", period, 1);
		gd.addCheckbox("Reset position between repeated incisions", resetPosition);
		gd.addNumericField("Length of Cuts (microns): ", lcuts, 1);
		gd.addMessage("");
		gd.addNumericField("Vectorial Velocity of Mirror Drive during Cuts: ",velocity, 3);
		gd.addMessage("");
		gd.addNumericField("Magnification Factor of Objective: ",mag, 0);
		gd.addNumericField("Zoom Factor of Image: ",zoom, 0);
		gd.addMessage("");
		gd.addMessage("After pressing OK,\n shutter will open 0.5 s after audible warning.");
		gd.showDialog();
		if (gd.wasCanceled()) {
			IJ.error("No incision made. PlugIn canceled!");
			return false;
		}
		parallel = gd.getNextBoolean();
		ncuts = (int) gd.getNextNumber();
	    period = gd.getNextNumber();
	    resetPosition = gd.getNextBoolean();
	    lcuts = gd.getNextNumber();
		velocity = gd.getNextNumber();
		if (velocity > maxVelocity) {
			velocity = maxVelocity;
			IJ.write("Mirror Vectorial Velocity too High. Setting to Max Velocity = "+IJ.d2s(maxVelocity));
		}
		if (velocity < minVelocity) {
			velocity = minVelocity;
			IJ.write("Mirror Vectorial Velocity too Low. Setting to Min Velocity = "+IJ.d2s(minVelocity));
		}
		mag = (int) gd.getNextNumber();
		zoom = (int) gd.getNextNumber();

		return true;
	}
}
