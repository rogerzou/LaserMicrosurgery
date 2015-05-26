# LaserMicrosurgery
##### Laser microdissection for Drosophila dorsal closure.
___
Heavily inspired by legacy code from Shane Hutson. This program uses serial ports (jSSC library) to communicate with a shutter and mirror device to perform laser microdissection. 

### Setup Instructions
1. Clone from GitHub.
2. Load the folder as an existing Eclipse project (all the necessary project files like .project and .classpath should be included).
3. Compile all source files using Eclipse (a bin/ folder should be created).
4. Make a new folder 'LaserMicrosurgeryEXE'.
5. Copy the contents of bin/ into LaserMicrosurgeryEXE/ (some .class files and laserj/ folder).
6. Copy the contents of lib/ into LaserMicrosurgeryEXE/ (should be 3 .jar files).
7. Copy LaserMicrosurgeryEXE/ to the plugins folder of MicroManager.
