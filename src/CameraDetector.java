import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class CameraDetector {
	private VideoCapture capture = new VideoCapture();
	private Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2));
	public int width;
	public int height;

	public boolean verbose = false;
	
	public Mat frame = new Mat();

	public static void main(String[] args) {
		// load the native OpenCV library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		CameraDetector camera = new CameraDetector(1, null);
		camera.getDirection(1);
	}

	public CameraDetector(int cameraInt, VideoCapture capture) {
		if (capture != null)
			this.capture = capture;

		if (!this.capture.isOpened()) {
			this.capture.open(cameraInt);
			this.capture.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, 1920);
			this.capture.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, 1080);
		}

		if (!this.capture.isOpened()) {
			System.err.println("Unable to open the camera connection.");
		} else {
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
		public double x, y, centerX, centerY;
		public double[] x1y1, x2y2;
		public DirectionPosition(double[] edge, double[] x1y1, double[] x2y2, double centerX, double centerY) {
			this.x = edge[0];
			this.y = edge[1];
			this.x1y1 = x1y1;
			this.x2y2 = x2y2;
			this.centerX = centerX;
			this.centerY = centerY;
			this.direction = Math.atan2(this.y, this.x);
		}
	}
	
	public Mat readFrame() {
		// Read the current frame
		this.capture.read(frame);
		return frame;
	}

	// Returns an angle value between 0 and 360
	public DirectionPosition getDirection(int robot) {
		// Read the current frame
		this.capture.read(frame);
		return getDirection(robot, frame);
	}

	// Returns an angle value between 0 and 360
	public DirectionPosition getDirection(int arTag, Mat origFrame) {
		// Should use opencv to grab camera frame and parse a robot's direction.
		if (!this.capture.isOpened()) {
			System.out.println("Camera is not yet opened!");
			return null;
		}

		Mat frame = origFrame.clone();
		// Return null if the camera frame is empty
		if (frame == null || frame.empty()) {
			System.out.println("Camera frame is empty. getDirection() in CameraDetector is returning null.");
			return null;
		}

		// If the frame is not empty, process it
		// colorFrame = Imgcodecs.imread("/home/zackory/workspace/Robot Collaboration/tree_hierarchy.png");
		if (verbose) System.out.println(frame.width() + ", " + frame.height());
		// Convert the image to grayscale
		if (frame.channels() > 1)
			Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);

		// Perform thresholding to determine dark (black) areas.
		// Imgproc.threshold(frame, frame, 150, 255, Imgproc.THRESH_BINARY);
		Imgproc.threshold(frame, frame, 50, 255, Imgproc.THRESH_BINARY_INV);
		// Imgproc.adaptiveThreshold(frame, frame, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);
		// Erode and then dilate to filter noise / small dots
		Imgproc.erode(frame, frame, kernel, new Point(0, 0), 3);
		Imgproc.dilate(frame, frame, kernel, new Point(0, 0), 3);
		Imgcodecs.imwrite("/home/zackory/workspace/Robot Collaboration/view.png", frame);

		if (verbose) System.out.println("1");
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

		if (verbose) System.out.println("2");
		// Find the proper AR tag by finding a contour that contains two children contours.
		// OpenCV represents tree structure with array of array elements: [Next, Previous, First_Child, Parent]
		for (int c = 0; c < hierarchy.cols(); c++) {
			// Check if parent has children (are there contours nested inside this one)
			double childId = hierarchy.get(0, c)[2];
			if (verbose) System.out.println("childId: " + childId);
			if (childId == -1)
				continue;
			// Loop through all children. There should be two children. None of the children should have children.
			double[] child1 = hierarchy.get(0, (int) childId);
			if (verbose) System.out.println("child1: " + child1[0]);
			// Continue to next contour if there is only one child or if first child has any children
			if (child1[0] == -1 || child1[2] != -1)
				continue;
			double[] child2 = hierarchy.get(0, (int) child1[0]);
			if (verbose) System.out.println("child2: " + child2[0]);
			// Continue to next contour if there are more than two children (only for robot 1) or if second child has any children
			if (arTag == 1 && (child2[0] != -1 || child2[2] != -1) || (arTag == 2 && (child2[0] == -1 || child1[2] != -1)))
				continue;
			double[] child3 = null;
			if (arTag == 2) {
				child3 = hierarchy.get(0, (int) child2[0]);
				// Continue to next contour if there are more than three children (only for robot 2) or if third child has any children
				if (child3[0] != -1 || child3[2] != -1)
					continue;
			}

			if (verbose) System.out.println("Number of contour points in first contour: " + contours.get((int) childId).rows());

			// This simplifies all contours to a polygon (rather than a bunch of points along a shape)
			MatOfPoint2f mMOP2f1 = new MatOfPoint2f(); 
			MatOfPoint2f mMOP2f2 = new MatOfPoint2f(); 
			for (int i = 0; i < contours.size(); i++) {
				// Convert contours(i) from MatOfPoint to MatOfPoint2f
				contours.get(i).convertTo(mMOP2f1, CvType.CV_32FC2);
				//Processing on mMOP2f1 which is in type MatOfPoint2f
				Imgproc.approxPolyDP(mMOP2f1, mMOP2f2, 3, true); 
				//Convert back to MatOfPoint and put the new values back into the contours list
				mMOP2f2.convertTo(contours.get(i), CvType.CV_32S);
			}				

			if (verbose) System.out.println("Number of contour points in first contour after polygon approximation: " + contours.get((int) childId).rows());

			if (verbose) System.out.println("3");
			// We have found the proper AR tag.
			// Find longest edge of both children.
			MatOfPoint contour1 = contours.get((int) childId);
			//				MatOfPoint2f approxContour1 = new MatOfPoint2f(contour1.toArray());
			//				Imgproc.approxPolyDP(contour1, approxContour1, 3, true);
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

			if (verbose) System.out.println("4");
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

			Mat baseEdge = null;
			Mat directionEdge = null, directionEdge2 = null;
			Mat dirEdgePoint1 = null, dirEdgePoint2 = null, pointOnBase = null;
			if (arTag == 1) {
				// Make sure we did not find a small little anomaly.
				if (maxLength1 < 20 || maxLength2 < 20)
					continue;

				if (verbose) System.out.println(maxLength1 + ", " + maxLength2);

				// Find the child contour with the longest edge (this is the bottom child)
				// The other child gives us the direction (angle) of the robot
				if (maxLength1 > maxLength2) {
					// First contour is the bottom child
					baseEdge = maxEdge1;
					directionEdge = maxEdge2;
					dirEdgePoint1 = edge2Point1;
					dirEdgePoint2 = edge2Point2;
					pointOnBase = edge1Point1;
				} else {
					// Second contour is the bottom child
					baseEdge = maxEdge2;
					directionEdge = maxEdge1;
					dirEdgePoint1 = edge1Point1;
					dirEdgePoint2 = edge1Point2;
					pointOnBase = edge2Point1;
				}
			} else if (arTag == 2) {
				MatOfPoint contour3 = contours.get((int) child2[0]);
				Mat maxEdge3 = null;
				Mat edge3Point1 = null, edge3Point2 = null;
				double maxLength3 = 0;
				for (int i = 1; i < contour3.rows(); i++) {
					Mat temp = new Mat();
					Core.subtract(contour3.row(i), contour3.row(i-1), temp);
					double distance = Core.norm(temp, Core.NORM_L2);
					if (distance > maxLength3) {
						maxEdge3 = temp;
						maxLength3 = distance;
						edge3Point1 = contour3.row(i);
						edge3Point2 = contour3.row(i-1);
					}
				}

				// Make sure we did not find a small little anomaly.
				if (maxLength1 < 20 || maxLength2 < 20 || maxLength3 < 20)
					continue;

				if (verbose) System.out.println(maxLength1 + ", " + maxLength2 + ", " + maxLength3);

				// Find the child contour with the longest edge (this is the bottom child)
				// The other children gives us the direction (angle) of the robot
				if (maxLength1 > maxLength2 && maxLength1 > maxLength3) {
					// First contour is the bottom child
					baseEdge = maxEdge1;
					directionEdge = maxEdge2;
					directionEdge2 = maxEdge3;
					dirEdgePoint1 = edge2Point1;
					dirEdgePoint2 = edge2Point2;
					pointOnBase = edge1Point1;
				} else if (maxLength2 > maxLength1 && maxLength2 > maxLength3) {
					// Second contour is the bottom child
					baseEdge = maxEdge2;
					directionEdge = maxEdge1;
					directionEdge2 = maxEdge3;
					dirEdgePoint1 = edge1Point1;
					dirEdgePoint2 = edge1Point2;
					pointOnBase = edge2Point1;
				} else {
					// Third contour is the bottom child
					baseEdge = maxEdge3;
					directionEdge = maxEdge1;
					directionEdge2 = maxEdge2;
					dirEdgePoint1 = edge1Point1;
					dirEdgePoint2 = edge1Point2;
					pointOnBase = edge3Point1;
				}
			}
			
			if (dirEdgePoint1 == null || dirEdgePoint2 == null || pointOnBase == null) {
				System.out.println("dirEdgePoint1, dirEdgePoint2, or pointOnBase was null!");
				return null;
			}

			// Use the longest edge in this other child to determine the direction (angle) of the robot
			// Direction should point opposite of the bottom child

			if (verbose) System.out.println("5");
			// Check if direction of angle vector should be flipped 180 (i.e. pointing in the wrong direction)
			Mat temp1 = new Mat();
			Mat temp2 = new Mat();
			Core.subtract(dirEdgePoint1, pointOnBase, temp1);
			Core.subtract(dirEdgePoint2, pointOnBase, temp2);
			double dist1 = Core.norm(temp1, Core.NORM_L2);
			double dist2 = Core.norm(temp2, Core.NORM_L2);

			if (dist2 > dist1) {
				// Flip direction of vector
				Core.subtract(dirEdgePoint2, dirEdgePoint1, directionEdge);
			}

			// Determine center of robot
			MatOfPoint parentContour = contours.get(c);
			Moments moment = Imgproc.moments(parentContour);
			
			// Return direction vector
			return new DirectionPosition(directionEdge.get(0, 0), dirEdgePoint1.get(0, 0), dirEdgePoint2.get(0, 0), moment.m10/moment.m00, moment.m01/moment.m00);
		}

		System.out.println("Camera detector has returned null!");
		return null;
	}

	public void closeCamera() {
		this.capture.release();
	}

}
