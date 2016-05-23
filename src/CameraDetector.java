import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import uwl.learning.QLearning;

public class CameraDetector {
	private VideoCapture capture = new VideoCapture();
	private Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2));
	public int width;
	public int height;
	
	public static void main(String[] args) {
		// load the native OpenCV library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		CameraDetector camera = new CameraDetector(0);
		camera.getDirection(0);
	}

	public CameraDetector(int cameraInt) {
		this.capture.open(cameraInt);
		if (!this.capture.isOpened()) {
			System.err.println("Unable to open the camera connection.");
		} else {
			Mat frame = new Mat();
			try {
				// Read the current frame to obtain width and height of camera image
				this.capture.read(frame);
				width = frame.width();
				height = frame.height();
			} catch (Exception e) {
				System.err.println("Exception during the camera detection:");
				e.printStackTrace();
			}
		}
	}
	
	public class DirectionPosition {
		public double direction;
		public double x, y;
		public DirectionPosition(double x, double y, double direction) {
			this.x = x;
			this.y = y;
			this.direction = direction;
		}
	}
	
	// Returns an angle value between 0 and 360
	public DirectionPosition getDirection(int robot) {
		// Should use opencv to grab camera frame and parse a robot's direction.
		if (!this.capture.isOpened()) {
			return null;
		}
		
		Mat colorFrame = new Mat();
		Mat frame = new Mat();
		try {
			// Read the current frame
			this.capture.read(colorFrame);
			
			// if the frame is not empty, process it
			if (!colorFrame.empty()) {
				colorFrame = Imgcodecs.imread("/home/zackory/workspace/EV3 Wifi/tree_hierarchy.png");
				System.out.println(colorFrame.width() + ", " + colorFrame.height());
				// Convert the image to grayscale
				Imgproc.cvtColor(colorFrame, frame, Imgproc.COLOR_BGR2GRAY);
				
				// Perform thresholding to determine dark (black) areas.
				Imgproc.threshold(frame, frame, 50, 255, Imgproc.THRESH_BINARY);
				// Imgproc.adaptiveThreshold(frame, frame, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);
				// Erode and then dilate to filter noise / small dots
				Imgproc.erode(frame, frame, kernel, new Point(0, 0), 3);
				Imgproc.dilate(frame, frame, kernel, new Point(0, 0), 3);
				Imgcodecs.imwrite("/home/zackory/workspace/EV3 Wifi/view.png", frame);

				// Find contours (bounding shapes) within image
				ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>(); 
				Mat hierarchy = new Mat();
			    Imgproc.findContours(frame, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

			    // Used for testing
			    /*System.out.println("Number of contours: " + contours.size());
			    System.out.println(hierarchy.rows() + ", " + hierarchy.cols());
			    for (int r = 0; r < hierarchy.rows(); r++) {
			    	for (int c = 0; c < hierarchy.cols(); c++) {
			    		System.out.println(hierarchy.row(r).col(c).dump());
			    	}
			    }*/

			    // Find the proper AR tag by finding a contour that contains two children contours.
			    // OpenCV represents tree structure with array of array elements: [Next, Previous, First_Child, Parent]
			    for (int c = 0; c < hierarchy.cols(); c++) {
			    	// Check if parent has children (are there contours nested inside this one)
			    	double childId = hierarchy.get(0, c)[2];
			    	if (childId == -1)
			    		continue;
		    		// Loop through all children. There should be two children. None of the children should have children.
		    		double[] child1 = hierarchy.get(0, (int) childId);
		    		// Continue to next contour if there is only one child or if first child has any children
		    		if (child1[0] == -1 || child1[2] != -1)
		    			continue;
		    		double[] child2 = hierarchy.get(0, (int) child1[0]);
		    		// Continue to next contour if there are more than two children or if second child has any children
		    		if (child2[0] != -1 || child2[2] != -1)
		    			continue;
		    		
		    		// We have found the proper AR tag.
		    		// Find longest edge of both children.
		    		MatOfPoint contour1 = contours.get((int) childId);
		    		Mat maxEdge1 = null;
		    		Mat edge1Point1 = null, edge1Point2 = null;
		    		double maxLength1 = 0;
		    		for (int i = 1; i < contour1.rows(); i++) {
		    			Mat temp = new Mat();
		    			Core.subtract(contour1.row(i), contour1.row(i-1), temp);
		    			double distance = Core.norm(temp, Core.NORM_L2);
		    			if (distance > maxLength1) {
		    				maxEdge1 = temp;
		    				maxLength1 = distance;
		    				edge1Point1 = contour1.row(i);
		    				edge1Point2 = contour1.row(i-1);
		    			}
		    		}
		    		
		    		MatOfPoint contour2 = contours.get((int) child1[0]);
		    		Mat maxEdge2 = null;
		    		Mat edge2Point1 = null, edge2Point2 = null;
		    		double maxLength2 = 0;
		    		for (int i = 1; i < contour2.rows(); i++) {
		    			Mat temp = new Mat();
		    			Core.subtract(contour2.row(i), contour2.row(i-1), temp);
		    			double distance = Core.norm(temp, Core.NORM_L2);
		    			if (distance > maxLength2) {
		    				maxEdge2 = temp;
		    				maxLength2 = distance;
		    				edge2Point1 = contour2.row(i);
		    				edge2Point2 = contour2.row(i-1);
		    			}
		    		}
		    		
		    		// Find the child contour with the longest edge (this is the bottom child)
		    		// The other child gives us the direction (angle) of the robot
		    		Mat baseEdge = null;
		    		Mat directionEdge = null;
		    		Mat dirEdgePoint1 = null, dirEdgePoint2 = null, pointOnBase;
		    		if (maxLength1 > maxLength2) {
		    			baseEdge = maxEdge1;
		    			directionEdge = maxEdge2;
			    		dirEdgePoint1 = edge2Point1;
			    		dirEdgePoint2 = edge2Point2;
			    		pointOnBase = edge1Point1;
		    		} else {
		    			baseEdge = maxEdge2;
		    			directionEdge = maxEdge1;
			    		dirEdgePoint1 = edge1Point1;
			    		dirEdgePoint2 = edge1Point2;
			    		pointOnBase = edge2Point1;
		    		}
		    		
		    		// Use the longest edge in this other child to determine the direction (angle) of the robot
		    		// Direction should point opposite of the bottom child
		    		
		    		// Check if direction of angle vector should be flipped 180 (i.e. pointing in the wrong direction)
	    			Mat temp1 = new Mat();
	    			Mat temp2 = new Mat();
	    			Core.subtract(dirEdgePoint1, pointOnBase, temp1);
	    			Core.subtract(dirEdgePoint2, pointOnBase, temp1);
	    			double dist1 = Core.norm(temp1, Core.NORM_L2);
	    			double dist2 = Core.norm(temp2, Core.NORM_L2);
		    		
		    		if (dist2 > dist1) {
		    			// Flip direction of vector
		    			Core.subtract(dirEdgePoint2, dirEdgePoint1, directionEdge);
		    		}
		    		
		    		// Return angle of direction vector
		    		return new DirectionPosition(directionEdge.get(0, 0)[0], directionEdge.get(0, 0)[1], Math.toDegrees(Math.atan2(directionEdge.get(0, 0)[1], directionEdge.get(0, 0)[0])));
			    }

			}
		} catch (Exception e) {
			System.err.println("Exception during the camera detection:");
			e.printStackTrace();
		}

		return null;
	}
	
	public void closeCamera() {
		this.capture.release();
	}
	
}
