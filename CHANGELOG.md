# CHANGELOG
___

##### Date: May 27, 2015 | Author: Roger S. Zou
We switched from a 32-bit Windows XP machine to a 64-bit Windows 7 machine for laser wounding. The legacy code written by Shane Hutson in 2003 that controls the laser guidance hardware is deprecated: the Java Communications API that provides the interface between hardware and software is no longer supported by Oracle, the maintainers of Java. So, we switched to a new open-source API called java-simple-serial-connector (jSSC). However, it meant changing significant portions of the legacy code, and it was significantly easier to simply create a new plugin modeled after the legacy code. I am in the process of adding documentation. Four of the most commonly used functions currently work with the laser microscopy system: Point_and_Shoot, Center_Microbeam, Cut_A_Curve, and Reset_Microbeam.
