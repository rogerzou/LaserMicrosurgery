/* Name: Calibrate_Rough.java
 * Project: Laser microdissection of dorsal closure
 * Version: 2.0
 * Author: Shane Hutson
 * Maintained by: Albert Mao
 * Date: 11/19/2004
 * Description: This plugin uses the microbeam to target six spots for ablation.
 * The relative positions of these spots are then used to calibrate the mirror controller motors of the microbeam.
 * The plugin should be run twice: first to ablate the pattern, then again to enter the coordinates of the ablated spots on the image.
 */

import ij.*;
import ij.process.*;
import ij.process.ImageStatistics.*;
import ij.gui.*;
import java.awt.*;
import java.awt.event.*;
import ij.plugin.*;
import java.io.*;
import java.util.*;
import javax.comm.*;
import plugins.LaserMicrosurgery.laserj.*;

public class Calibrate_Rough implements PlugIn {

	double[][] cal = new double[2][2];

    public void run(String arg) {
		IJ.setColumnHeadings("");

		// Displays a dialog box for the user to specify the parameters of the calibration pattern
		int reprate = 10;
		double ds = 0.2;
		GenericDialog gd = new GenericDialog("Calibration Parameters");
		gd.addNumericField("Laser Repetition Rate (Hz) :",reprate,0);
		gd.addNumericField("Calibration Range, DS, (mm travel on actuators) :",ds,2);
		gd.addMessage("");
		gd.addMessage("Program will cut a plus sign in the positive quadrant and then");
		gd.addMessage("make 6 spot ablations on sample at:");
		gd.addMessage("(+DS, 0);  (-DS, 0);   (0, +DS);  (0, +0.9*DS);  (0, -DS); and (0, -0.9*DS)");
		gd.addMessage("So single shots on axis #1 and double shots on axis #2.");
		gd.addCheckbox("Check here to cut pattern (leave unchecked to simply enter spot positions from previous run.)", true);
		gd.showDialog();
		if (gd.wasCanceled()) {
			IJ.error("PlugIn canceled!");
			return;
		}
	   	reprate = (int)gd.getNextNumber();
		ds = gd.getNextNumber();
		boolean cut_pattern = gd.getNextBoolean();
		double period = (1000/reprate);
		int opentime = (int)(period*(1+0.05 ));

		// Initializes the microbeam
		Microbeam microbeam = new Microbeam(Microbeam.CONFIG_FILENAME);
		if(!microbeam.isSetupOK()) { return;}

		if(cut_pattern) {

			// Label positive quadrant
			microbeam.moveToMM(ds/2, ds/4);
			microbeam.openShutter();
			microbeam.moveToMM(ds/2, 3*ds/4);
			microbeam.closeShutter();
			microbeam.moveToMM(ds/4, ds/2);
			microbeam.openShutter();
			microbeam.moveToMM(3*ds/4, ds/2);
			microbeam.closeShutter();

			// Spot 1
			microbeam.moveToMM(ds, 0);
			for(int j=0; j<3; j++) {
				IJ.beep();
				IJ.wait(500);
			}
			microbeam.openShutter();
			IJ.wait( opentime );
			microbeam.closeShutter();

			// Spot 2
			microbeam.moveToMM(-ds, 0);
			for(int j=0; j<3; j++) {
				IJ.beep();
				IJ.wait(500);
			}
			microbeam.openShutter();
			IJ.wait( opentime );
			microbeam.closeShutter();

			// Spots 3 and 4
			microbeam.moveToMM(0, ds);
			for(int j=0; j<3; j++) {
				IJ.beep();
				IJ.wait(500);
			}
			microbeam.openShutter();
			IJ.wait( opentime );
			microbeam.closeShutter();
			microbeam.moveToMM(0, 0.9*ds);
			microbeam.openShutter();
			IJ.wait( opentime );
			microbeam.closeShutter();

			// Spots 5 and 6
			microbeam.moveToMM(0, -ds);
			for(int j=0; j<3; j++) {
				IJ.beep();
				IJ.wait(500);
			}
			microbeam.openShutter();
			IJ.wait( opentime );
			microbeam.closeShutter();
			microbeam.moveToMM(0, -0.9*ds);
			microbeam.openShutter();
			IJ.wait( opentime );
			microbeam.closeShutter();

			microbeam.moveToMM(0.0, 0.0);	//return home
		} else {
			if(measureCalibration(ds)) {
				microbeam.set_calibration(cal[0][0], cal[0][1], cal[1][0], cal[1][1]);
				gd = new GenericDialog("REPLACE CALIBRATION?");
				gd.addMessage("OK will save the newly calculated microbeam calibration to " + Microbeam.CONFIG_FILENAME);
				gd.addMessage("CANCEL will end program without overwriting previous calibration.");
    			gd.showDialog();
				if (!gd.wasCanceled()) {
					microbeam.saveConfig(Microbeam.CONFIG_FILENAME);
				}
			}
		}

		// Releases the microbeam
		microbeam.off();
		IJ.write("\nDONE\n");
	}


	boolean measureCalibration(double ds) {
		double[][] spots = new double[4][2];
		double[][] calinv = new double[2][2];
		int zoom = 1;

		// Displays a dialog box for the user to enter the coordinates of the ablation spots
		GenericDialog gd = new GenericDialog("Calibration Spot Positions");
		gd.addNumericField("Digital Zoom of Image :",zoom,0);
		gd.addMessage("");
		gd.addMessage("Coordinates of ablated points on AXIS #1");
		gd.addNumericField("Spot #1 (+ on AXIS #1) - X :",83,0);
		gd.addNumericField("Spot #1 (+ on AXIS #1) - Y :",331,0);
		gd.addNumericField("Spot #2 (- on AXIS #1) - X :",323,0);
		gd.addNumericField("Spot #2 (- on AXIS #1) - Y :",331,0);
		gd.addMessage("Use coordinates of extreme points on AXIS #2");
		gd.addNumericField("Spot #3 (+ on AXIS #2) - X :",200,0);
		gd.addNumericField("Spot #3 (+ on AXIS #2) - Y :",506,0);
		gd.addNumericField("Spot #4 (- on AXIS #1) - X :",206,0);
		gd.addNumericField("Spot #4 (- on AXIS #1) - Y :",161,0);
		gd.addMessage("");
		gd.showDialog();
		if (gd.wasCanceled()) {
			IJ.error("Calibration canceled!");
			return false;
		}
	   	zoom = (int) gd.getNextNumber();
	   	for(int i = 0; i < 4; i++) {
			spots[i][0] = gd.getNextNumber();
		   	spots[i][1] = gd.getNextNumber();
			if((spots[i][0]<=0) | (spots[i][1]<=0)) return false;
		}
		calinv[0][0] = (spots[0][0] - spots[1][0])/(2*ds);
		calinv[1][0] = (spots[0][1] - spots[1][1])/(2*ds);
		calinv[0][1] = (spots[2][0] - spots[3][0])/(2*ds);
		calinv[1][1] = (spots[2][1] - spots[3][1])/(2*ds);

		// Inverts the matrix to get calibration
		double det = calinv[0][0]*calinv[1][1] - calinv[0][1]*calinv[1][0];
		if(det != 0) {
			cal[0][0] = zoom*(1/det)*calinv[1][1];
			cal[0][1] = zoom*(-1/det)*calinv[0][1];
			cal[1][0] = zoom*(-1/det)*calinv[1][0];
			cal[1][1] = zoom*(1/det)*calinv[0][0];
		} else {
			IJ.write("Singular Matrix.");
			return false;
		}
		String p4 = "Conversion - mm travel (axis 1) per pixel (X):     ";
		String p5 = "Conversion - mm travel (axis 1) per pixel (Y):     ";
		String p6 = "Conversion - mm travel (axis 2) per pixel (X):     ";
		String p7 = "Conversion - mm travel (axis 2) per pixel (Y):     ";

		IJ.write("\nNEW CALIBRATION\n\n");
		IJ.write(p4 +"\t"+IJ.d2s(cal[0][0],8));
		IJ.write(p5 +"\t"+IJ.d2s(cal[0][1],8));
		IJ.write(p6 +"\t"+IJ.d2s(cal[1][0],8));
		IJ.write(p7 +"\t"+IJ.d2s(cal[1][1],8));
		IJ.write("");
		return true;
	}
}
