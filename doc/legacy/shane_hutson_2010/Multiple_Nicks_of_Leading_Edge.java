/* Name: Microbeam_Example.java
 * Project: Laser microdissection of dorsal closure
 * Version: 2.0
 * Author: Shane Hutson
 * Maintained by: Albert Mao
 * Date: 11/19/2004
 * Description: This plugin uses the microbeam to make multiple incisions perpendicular to an arbitrary trajectory.
 * The user defines the trajectory using the polygon, freehand, segmented line, or freehand line selection tools.
 */

//=====================================================
//      Name:          	Multiple_Nicks_of_Leading_Edge.java
//      Project:        	Laser microdissection of dorsal closure
//      Version:         	2.0
//
//      Author:           	Shane Hutson
//      Date:               	7/29/2003
//      Comment:       	Program designed to control shutter and mirror
//		of microbeam through serial ports. Cuts a multiple
//		nicks along a user-defined (with polyline or polygon tools)
//		incision curve
//		on the sample.
//=====================================================

//===========imports===================================

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

public class Multiple_Nicks_of_Leading_Edge implements PlugInFilter {

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

		// Displays a dialog box for the user to configure the experimental parameters
		double velocity = 0.05;
		int ncuts=6;
		int zoom =1;
		int mag = 40;
	    double reprate = 10;
	   	int npulses = 1;
		GenericDialog gd = new GenericDialog("Experimental Parameters");
		// gd.addNumericField("Laser Repetition Rate (Hz) :",reprate,0);
		// gd.addNumericField("Number of Pulses per Nick :",npulses,0);
		gd.addNumericField("Number of Nicks :",ncuts,0);
		gd.addNumericField("Vectorial Velocity of Mirror Drive during Cuts:",velocity,3);
		gd.addMessage("");
		gd.addNumericField("Magnification Factor of Objective :",mag,0);
		gd.addNumericField("Zoom Factor of Image :",zoom,0);
		gd.addMessage("");
		gd.addMessage("After pressing OK,\n shutter will open 0.5 s after audible warning.");
		gd.showDialog();
		if (gd.wasCanceled()) {
			IJ.error("No incision made. PlugIn canceled!");
			return;
		}
		// reprate=gd.getNextNumber();
		// npulses=(int)gd.getNextNumber();
	    ncuts = (int)gd.getNextNumber();
		velocity = gd.getNextNumber();
		if (velocity>maxVelocity) { velocity = maxVelocity; IJ.write("Mirror Vectorial Velocity too High. Setting to Max Velocity = "+IJ.d2s(maxVelocity));}
		if (velocity<minVelocity) { velocity = minVelocity; IJ.write("Mirror Vectorial Velocity too Low. Setting to Min Velocity = "+IJ.d2s(minVelocity));}
		mag = (int)gd.getNextNumber();
		zoom = (int)gd.getNextNumber();
	    // double period = (1000/reprate);	//period in ms
	    // int opentime = (int)(period*(npulses-0.05 ));

	    // Checks if the selected trajectory is valid.  If so, precalculates the endpoints of the incisions and stores them in xpath and ypath
		try {
			getXYCoordinates(imp);
		} catch (IllegalArgumentException e) {
			IJ.showMessage("Invalid selection", e.getMessage());
			return;
		}
		if(pathLength()<(ncuts*2)) {
			IJ.error("Defined Path ("+IJ.d2s(pathLength(),1) +" pixels) must be >"+IJ.d2s(ncuts*2.0,1)+" pixels. No incision made. PlugIn canceled!");
			return;
		}
		multipleNickPath(ncuts);

		// Initializes the microbeam
		Microbeam microbeam = new Microbeam(Microbeam.CONFIG_FILENAME);
		if(!microbeam.isSetupOK()) return;

		// Commands the microbeam to move to the coordinates stored in xpath and ypath, drawing the cuts on the image
		ip.snapshot();
		microbeam.setMirrorVelocity(velocity);			// Set to max vectorial velocity
		microbeam.moveToPIXELS(xpath[0], ypath[0], ip, zoom);
		ip.moveTo((int)xpath[0], (int)ypath[0]);
		long t1 = 0;
		long t2 = 0;
		for(int i=0; i<ncuts;i++) {
			t1=t1+t2;
			if (!win.running) break;
			microbeam.moveToPIXELS(xpath[i], ypath[i]+10, ip, zoom);
			ip.moveTo((int)xpath[i], (int)ypath[i]+10);

			if(i==0) for(int j=0; j<3; j++) {IJ.beep(); IJ.wait(500);}
			else IJ.beep();

			t2 = System.currentTimeMillis();
			microbeam.openShutter();
			IJ.wait(100);
			microbeam.moveToPIXELS(xpath[i], ypath[i]-10, ip, zoom);
			ip.lineTo((int)xpath[i], (int)ypath[i]-10);
			imp.updateAndDraw();
			ip.moveTo((int)xpath[i], (int)ypath[i]-10);

			microbeam.closeShutter();
			t2 = System.currentTimeMillis()- t2;
		}
		microbeam.moveToMM(0, 0);

		// Resets the microbeam and restores ImageJ's line width setting
		microbeam.off();
		ip.setLineWidth(1);
		IJ.write("\nDONE\n");
		double exposure = ((double)(t1))/1000.0;
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
         		// IJ.write("Closed curve = "+closed);
		if (closed) {
			n = n+1;
			xpath = new double[n];
			ypath = new double[n];
			for (int i=0; i<(n-1); i++) {
				xpath[i] = 1.0*(r.x + x[i]);   ypath[i] =1.0*( r.y + y[i]);
			}
			xpath[n-1] = r.x + x[0];  ypath[n-1] = r.y + y[0];
		}
		else {
			xpath = new double[n];
			ypath = new double[n];
			for (int i=0; i<n; i++) {
				xpath[i] = 1.0*(r.x + x[i]);   ypath[i] = 1.0*(r.y + y[i]);
			}
		}
	}

	void drawTarget(int x, int y, ImageProcessor ip) {
		int color;
		ImageStatistics imstat = imp.getStatistics();
		//IJ.write("ROI Mean = "+IJ.d2s(imstat.mean));
		if(imstat.mean>127)  color = 0;
		else color = 255;
		ip.setColor(color);

		ip.setLineWidth(1);
		ip.moveTo((int)(x-10), (int)y);
		ip.lineTo((int)(x+10), (int)y);
		ip.moveTo((int)x, (int)(y-10));
		ip.lineTo((int)x, (int)(y+10));
		imp.updateAndDraw();
	}

	public double pathLength() {
		double L = 0.0;
		for (int i=1; i<xpath.length; i++) {
			L += Math.sqrt((xpath[i]-xpath[i-1])*(xpath[i]-xpath[i-1])+(ypath[i]-ypath[i-1])*(ypath[i]-ypath[i-1]));
		}
		IJ.write(IJ.d2s(L,3));
		return L;
	}

	public void multipleNickPath(int ncuts) {
		double[] xtemp = new double[ncuts];
		double[] ytemp = new double[ncuts];
		xtemp[0] = xpath[0];
		ytemp[0] = ypath[0];
		double dL=pathLength()/(ncuts-1);

		int f=0;
		for(int j=1; j<ncuts; j++) {
			double L = 0.0;
			for (int i=f+1; i<xpath.length; i++) {
				L += Math.sqrt((xpath[i]-xpath[i-1])*(xpath[i]-xpath[i-1])+(ypath[i]-ypath[i-1])*(ypath[i]-ypath[i-1]));
				f=i;
				if(L>dL) break;
			}
			xtemp[j] = xpath[f];
			ytemp[j] = ypath[f];
		}

		xpath = new double[ncuts];
		ypath = new double[ncuts];
		for (int i=0; i<xpath.length; i++) {
			xpath[i]=xtemp[i]; ypath[i]=ytemp[i];
			IJ.write(IJ.d2s(xpath[i],2)+"       "+IJ.d2s(ypath[i],2));
		}
	}

}
