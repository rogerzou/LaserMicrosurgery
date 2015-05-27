/* Name: Reset_Microbeam.java
 * Project: Laser microdissection of dorsal closure
 * Version: 3.0
 * Inspired by: Shane Hutson (2003), Albert Mao (2004)
 * Author: Roger Zou
 * Date: 05/22/2015
 * Description: This plugin closes the shutter and returns the microbeam position to (0,0).
 */

import ij.*;
import ij.plugin.*;

import laserj.*;

public class Reset_Microbeam implements PlugIn {

	public void run(String arg) {
		IJ.setColumnHeadings("");
		IJ.log("\n\n\n@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n\nRESET_MICROBEAM"
				+ "\n\n@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n\n\n");
		
		//Initializes the microbeam
		Microbeam microbeam = new Microbeam(Microbeam.CONFIG_FILENAME);
		if (!microbeam.isSetupOK())
			return;

		// close shutter, move to home, close microbeam.
		IJ.wait(200);
		microbeam.closeShutter();
		IJ.wait(200);
		microbeam.moveToMM(0.00,0.00);
		IJ.wait(200);
		microbeam.off();
		IJ.log("\nDONE\n");
	}

}
