package uwl.aruco;
import org.opencv.core.Mat;

public class Aruco {
	static { System.loadLibrary("aruco"); }
    public static native Mat getPredefinedDictionary();
}
