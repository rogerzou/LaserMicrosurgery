/* Name: Move_In_Circle.java
 * Project: Laser microdissection of dorsal closure
 * Version: 2.0
 * Author: Shane Hutson
 * Maintained by: Albert Mao
 * Date: 11/19/2004
 * Description: This plugin sweeps the microbeam in a circle centered at (0,0).
 * The user specifies the radius of the circle, and the number of revolutions to make.
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

public class Move_In_Circle implements PlugIn {

    public void run(String arg) {
		IJ.setColumnHeadings("");
	    double radius = 1.0;
		int nrot = 10;
		GenericDialog gd = new GenericDialog("Move In Circle Parameters");
		gd.addNumericField("Radius of circle (mm): ",radius, 2);
		gd.addNumericField("How many rotations before prompting for more: ",nrot, 0);
		gd.showDialog();
		if (gd.wasCanceled()) {
			IJ.error("PlugIn canceled!");
			return;
		}
		radius = gd.getNextNumber();
	   	nrot = (int) gd.getNextNumber();

	   	// Initializes the microbeam
		Microbeam microbeam = new Microbeam(Microbeam.CONFIG_FILENAME);
		if(!microbeam.isSetupOK()) return;

		// Sweeps the circle until the user quits
		boolean continue_rotating = true;
		microbeam.moveToMM(0, radius);
		while(continue_rotating) {
			microbeam.openShutter();
			microbeam.arcmoveToMM(0, 0, nrot*360.0);
			microbeam.closeShutter();
			IJ.beep();
    		GenericDialog cut = new GenericDialog("REPEAT?");
			cut.addMessage("OK will cut the sample again with the same trajectory.");
			cut.addMessage("CANCEL to end cutting and return mirrors to 0, 0.");
	    	cut.showDialog();
			continue_rotating = !cut.wasCanceled();

		}
		microbeam.moveToMM(0.0, 0.0);

		// Releases the microbeam
		microbeam.off();
		IJ.write("\nDONE\n");
	}

}
