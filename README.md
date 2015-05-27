# LaserMicrosurgery
##### Laser microdissection for Drosophila dorsal closure.
___
This program uses serial ports (jSSC library) to communicate with a shutter and mirror device to perform laser microdissection.

### Setup Instructions (for USERS using MicroManager)
1. Download from GitHub as a zip file. 
2. Rename 'LaserMicrosurgery-master' to just 'LaserMicrosurgery'
3. Load the folder as an existing Eclipse project (all the necessary project files like .project and .classpath should be included).
4. Compile all source files using Eclipse (a 'bin' folder should be created).
5. Make a new folder 'LaserMicrosurgeryEXE'.
6. Copy the contents of 'doc/Microbeam.txt' into LaserMicrosurgeryEXE/.
7. Copy the contents of 'bin' into LaserMicrosurgeryEXE/ (some '.class' files and 'laserj','helpers' folders).
8. Copy the contents of 'lib' into LaserMicrosurgeryEXE/ (should be 3 '.jar' files).
9. Copy 'LaserMicrosurgeryEXE' to the plugins folder of the MicroManager program.
