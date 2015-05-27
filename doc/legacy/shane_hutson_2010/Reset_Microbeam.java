/* Name: Reset_Microbeam.java
 * Project: Laser microdissection of dorsal closure
 * Version: 2.0
 * Author: Shane Hutson
 * Maintained by: Albert Mao
 * Date: 11/19/2004
 * Description: This plugin closes the shutter and returns the microbeam position to (0,0).
 */

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.awt.event.*;
import ij.plugin.*;
import java.io.*;
import java.util.*;
import javax.comm.*;
import plugins.LaserMicrosurgery.laserj.*;

public class Reset_Microbeam implements PlugIn {

	public void run(String arg) {
		IJ.setColumnHeadings("");

		//Initializes the microbeam
		Microbeam microbeam = new Microbeam(Microbeam.CONFIG_FILENAME);
		if(!microbeam.isSetupOK()) { return;}

		IJ.wait(200);
		microbeam.closeShutter();
		IJ.wait(200);
		microbeam.moveToMM(0.00,0.00);
		IJ.wait(200);
		microbeam.off();
		IJ.write("\nDONE\n");
	}

}
