/* Name: Microbeam_test.java
 * Project: Laser microdissection of dorsal closure
 * Version: 2.0
 * Author: Shane Hutson
 * Maintained by: Albert Mao
 * Date: 11/19/2004
 * Description: This plugin demonstrates some simple operations with the microbeam, and can be used to test the microbeam system.
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

public class Microbeam_test implements PlugInFilter {

	String ls;
	ImagePlus imp;
	ImageWindow win;
	ImageCanvas canvas;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		if (imp!=null) {
			win = imp.getWindow();
			win.running = true;
		}
		ls = System.getProperty("line.separator");
		return DOES_ALL+NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		IJ.setColumnHeadings("");

		Microbeam microbeam = new Microbeam(Microbeam.CONFIG_FILENAME);
		if(!microbeam.isSetupOK()) return;

		// Perform the microbeam operations
		microbeam.setMirrorVelocity(0.05);
		microbeam.openShutter();
		microbeam.moveToMM(0.1,0.1);
		IJ.wait(1000);
		microbeam.moveToMM(0.00,0.00);
		microbeam.closeShutter();
		IJ.wait(1000);
		microbeam.openShutter();
		microbeam.moveToPIXELS(200, 200, ip, 1);
		IJ.wait(1000);
		microbeam.moveToPIXELS(256, 256, ip, 1);
		
		microbeam.closeShutter();
			
		


		// Release the microbeam
		microbeam.off();
		IJ.write("\nDONE\n");
	}

}
