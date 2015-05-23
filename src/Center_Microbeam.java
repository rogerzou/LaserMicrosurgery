/* Name: Center_Microbeam.java
 * Project: Laser microdissection of dorsal closure
 * Version: 3.0
 * Inspired by: Shane Hutson (2003), Albert Mao (2004)
 * Author: Roger Zou
 * Date: 05/22/2015
 * Description: This plugin calibrates the home position of the mirror (0,0) to correspond to the center of the 
 * microscope field of view.
 * With the microbeam at (0,0), use the manual switch on the shutter controller to expose a gel to the beam for one pulse.
 * Save the acquired image and open it in ImageJ.
 * Use the crosshair tool to determine the coordinates of the ablated spot.
 * Execute the plugin, and enter the coordinates of the ablated spot.
 * The plugin will steer the microbeam to the calculated center of the image and fire one shot.
 * If the user specifies that the new ablation spot is centered correctly, the plugin resets the mirror controller 
 * to use the current position as the new home (0,0) position.
 */

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.*;

import laserj.*;

public class Center_Microbeam implements PlugInFilter {

    public int setup(String arg, ImagePlus imp) {
        return DOES_ALL + NO_CHANGES;
    }

	public void run(ImageProcessor ip) {
		double maxVelocity = 0.2;

		// Displays a dialog box for the user to enter the coordinates of the ablated spot
		int cx = (int) (ip.getWidth()/2);
		int cy = (int) (ip.getHeight()/2);
		int zoom = 1;
		GenericDialog gd = new GenericDialog("Center Microbeam");
		gd.addMessage("CENTER MICROBEAM AT SAME ZOOM YOU WILL USE FOR CUTTING!");
		gd.addMessage("");
		gd.addNumericField("Current Coordinates of Ablated Zone - X (pixels): ", cx, 0);
		gd.addNumericField("Current Coordinates of Ablated Zone - Y (pixels): ", cy, 0);
		gd.addNumericField("Digital Zoom of Image: ", zoom, 0);
		gd.showDialog();
		if (gd.wasCanceled()) {
			IJ.error("PlugIn canceled! MICROBEAM NOT CENTERED.");
			return;
		}
		cx = (int) gd.getNextNumber();
		cy = (int) gd.getNextNumber();
		zoom = (int) gd.getNextNumber();

		// Initializes the microbeam
		Microbeam microbeam = new Microbeam(Microbeam.CONFIG_FILENAME);
		if (!microbeam.isSetupOK()) return;

		/** LASER INCISION (CENTERING) **/
		// Prepare
		ip.snapshot();
		
		// Move to computed center
		microbeam.setMirrorVelocity(maxVelocity);
		microbeam.moveToPIXELS(ip.getWidth() - cx, ip.getHeight() - cy, ip, zoom);
		IJ.wait(200);
		
		// Point shot at new center
		microbeam.openShutter();
		IJ.wait(100);
		microbeam.closeShutter();

		// Ask the user if the newly ablated spot is correctly centered. if so, designate the current position as the origin.
		GenericDialog spotcheck = new GenericDialog("IS THE NEW SPOT CENTERED?");
		spotcheck.addCheckbox("Check to set new spot position to microbeam home (0, 0)?", true);
		spotcheck.showDialog();
		// if dialog cancelled, move microbeam to home and turn it off
		if (spotcheck.wasCanceled()) {
			microbeam.moveToMM(0, 0);
			microbeam.off();
			IJ.error("PlugIn canceled! Mirror returned to original 0, 0 position.");
			return;
		}
		// if verified to be new center, save the center location
		boolean centerOK = spotcheck.getNextBoolean();
		if (centerOK)
			microbeam.defineMirrorHome();
		else {
			microbeam.moveToMM(0, 0);
			IJ.showMessage("MICROBEAM NOT CENTERED!", "Mirror returned to original 0, 0 position.");
		}

		// turn microbeam off and clean up
		microbeam.off();
		IJ.log("\nDONE\n");
    }

}
