/* Name: Define_Home.java
 * Project: Laser microdissection of dorsal closure
 * Version: 3.0
 * Author: Roger Zou
 * Date: 06/30/2015
 * Description: Defines coordinates of new home for Mirror.
 */

import laserj.Microbeam;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class Define_Home implements PlugInFilter {

    public int setup(String arg, ImagePlus imp) {
		IJ.log("\n\n\n@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n\nDEFINE NEW HOME"
				+ "\n\n@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n\n\n");
        return DOES_ALL + NO_CHANGES;
    }

	public void run(ImageProcessor ip) {

		// Displays a dialog box for the user to enter the coordinates of new home
		int cx = 0;
		int cy = 0;
		GenericDialog gd = new GenericDialog("Define New Home");
		gd.addMessage("");
		gd.addNumericField("Desired Home Coordinates - X (mm): ", cx, 0);
		gd.addNumericField("Desired Home Coordinates - Y (mm): ", cy, 0);
		gd.showDialog();
		if (gd.wasCanceled()) {
			IJ.error("PlugIn canceled! NEW HOME NOT DEFINED.");
			return;
		}
		cx = (int) gd.getNextNumber();
		cy = (int) gd.getNextNumber();
		
		// Initializes the microbeam
		Microbeam microbeam = new Microbeam(Microbeam.CONFIG_FILENAME);
		if (!microbeam.isSetupOK()) return;
		
		// set new home, and turn off mirror
		microbeam.setNewMirrorHome(cx, cy);
		microbeam.off();
	}

}
