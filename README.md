# LaserMicrosurgery
##### Laser microdissection for Drosophila dorsal closure.
___
ImageJ plugin that uses serial ports (jSSC library) to communicate with a shutter and mirror device to perform laser microdissection. Compatible with MicroManager, an open source microscopy program.

### Plugin Setup Instructions (for USERS using MicroManager)
###### (Getting the code set up)
1. Download from GitHub as a zip file. 
2. Rename 'LaserMicrosurgery-master' to just 'LaserMicrosurgery'
3. Load the folder as an existing Eclipse project (all the necessary project files like .project and .classpath should be included).
4. Compile all source files using Eclipse (a 'bin' folder should be created).
5. Make a new folder 'LaserMicrosurgeryEXE'.
6. Copy 'doc/Microbeam.txt' into 'LaserMicrosurgeryEXE'.
7. Copy all the contents of 'bin' into 'LaserMicrosurgeryEXE' (some '.class' files and 'laserj', 'helpers' folders).
8. Copy all the contents of 'lib' into 'LaserMicrosurgeryEXE' (should be 3 '.jar' files).
9. Copy 'LaserMicrosurgeryEXE' to the plugins folder of the MicroManager program.

### MicroManager (MM) Setup Instructions (for USERS using MicroManager)
###### (Getting the code to run on MicroManager)
1. Navigate to 'LaserMicrosurgeryEXE' folder.
2. In 'Microbeam.txt', make sure that all information is correct for your system (e.x. COM ports for mirror and shutter are assigned correctly)
3. In general, make sure that all COM ports are correctly assigned in your MicroManager config file.
3. When the MicroManager program is open, go to its ImageJ GUI. Its relevant microdissection functions are under 'Plugins>LaserMicrosurgeryEXE>'. 
