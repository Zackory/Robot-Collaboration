package uwl.aruco;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import wrapper.*;

public class arucoCreateMarker {
	public static void main(String[] args) throws Exception {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		// Select a marker ID between 0 to 1023
		int markerId = 750;
		// Name of output file for created marker
		String filename = "marker_750id_150.jpg";
		// Size of marker in pixels
		int pixelSize = 150;
		
		FiducidalMarkers fm = new FiducidalMarkers();
		Mat marker = fm.createMarkerImage(markerId, pixelSize);
	    Imgcodecs.imwrite(filename, marker);
	}

	static{
		System.load("/home/zackory/Downloads/aruco-1.2.4/build/src/libaruco.so.1.2");
		System.load("/home/zackory/workspace/EV3 Wifi/libWrapperCPP.so");
	}
}