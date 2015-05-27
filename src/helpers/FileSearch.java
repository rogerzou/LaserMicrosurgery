package helpers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileSearch {

	public static List<String> searchDirectory(File directory, String searchFile) {
		List<String> foundFiles = new ArrayList<String>();
		if (directory.isDirectory()) {
			search(directory, searchFile, foundFiles);
		} else {
			System.out.println(directory.getAbsoluteFile() + " is not a directory.");
		}
		return foundFiles;
	}

	private static void search(File curDir, String searchFile, List<String> foundFiles) {
		if (curDir.isDirectory() && curDir.canRead()) {
			for (File tmp : curDir.listFiles()) {
				if (tmp.isDirectory()) {
					search(tmp, searchFile, foundFiles);
				} else if (searchFile.toLowerCase().equals(tmp.getName().toLowerCase())) {			
					foundFiles.add(tmp.getAbsoluteFile().toString());
				}
			}
		} else {
			System.out.println(curDir.getAbsoluteFile() + ": Permission Denied");
		}
	}

}
