import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class FXController {
	@FXML
	private Button button;
	@FXML
	private ImageView currentFrame;
	
	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that realizes the video capture
	private VideoCapture capture = new VideoCapture();
	// a flag to change the button behavior
	private boolean cameraActive = false;

	private CameraDetector cameraDetector;

	// Orange Working
	private Scalar lowerOrange = new Scalar(40, 80, 80); // HSV
	private Scalar upperOrange = new Scalar(60, 255, 255); // HSV

	private Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2));
	
	int camInt = 0;
	
	/**
	 * The action triggered by pushing the button on the GUI
	 * 
	 * @param event
	 *            the push button event 
	 */
	@FXML
	protected void startCamera(ActionEvent event) {	
		if (!this.cameraActive)	{
			// start the video capture
			this.capture.open(camInt);
			cameraDetector = new CameraDetector(camInt, capture);
			
			// is the video stream available?
			if (this.capture.isOpened()) {
				this.cameraActive = true;
				
				if (camInt == 1) {
					capture.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, 1920);
					capture.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, 1080);
				}
				
				// grab a frame every 33 ms (30 frames/sec)
				Runnable frameGrabber = new Runnable() {
					@Override
					public void run() {
						Image imageToShow = grabFrame();
						if (imageToShow != null)
							currentFrame.setImage(imageToShow);
					}
				};

				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 500, TimeUnit.MILLISECONDS);
				
				this.button.setText("Stop Camera");
			} else {
				System.err.println("Impossible to open the camera connection...");
			}
		} else {
			// the camera is not active at this point
			this.cameraActive = false;
			this.button.setText("Start Camera");
			
			// stop the timer
			try	{
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
			
			// release the camera and clean frame
			this.capture.release();
			this.currentFrame.setImage(null);
		}
	}
	
	/**
	 * Get a frame from the opened video stream (if any)
	 * 
	 * @return the {@link Image} to show
	 */
	private Image grabFrame() {
		Image imageToShow = null;
		Mat frame = new Mat();
		
		// check if the capture is open
		if (this.capture.isOpened()) {
			try {
				// read the current frame
				this.capture.read(frame);
				// System.out.println("Frame: " + frame.width() + ", " + frame.height());
				
				// if the frame is not empty, process it
				if (!frame.empty())	{
					// convert the image to gray scale
					// Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
					
//					frame = detectColors(frame);
					// frame = detectARContours(frame);
					
					CameraDetector.DirectionPosition dirPos = cameraDetector.getDirection(2, frame.clone());
					if (dirPos == null)
						return mat2Image(frame);
					System.out.println(dirPos.direction + ", " + dirPos.x + ", " + dirPos.y);
					// System.out.println(dirPos.x1y1[0] + ", " + dirPos.x1y1[1] + ", " + dirPos.x2y2[0] + ", " + dirPos.x2y2[1]);
					Imgproc.line(frame, new Point(dirPos.centerX, dirPos.centerY), new Point(dirPos.centerX + dirPos.x, dirPos.centerY + dirPos.y), new Scalar(33, 150, 243), 10);
					Imgproc.line(frame, new Point(dirPos.x1y1[0], dirPos.x1y1[1]), new Point(dirPos.x2y2[0], dirPos.x2y2[1]), new Scalar(33, 150, 243), 10);
					
					double width = 10;
					double height = 5;
					
					double startX = frame.width() / 8.0;
					double endX = frame.width() * 7.0 / 8.0;
					double gridDim = (endX - startX) / width;
					double startY = frame.height()/2.0 - (gridDim * height)/2.0;
					double endY = frame.height()/2.0 + (gridDim * height)/2.0;
					
					// Draw rows on image
					// for (int i = 0; i <= height; i++)
					// 	Imgproc.line(frame, new Point(startX, startY + i*gridDim), new Point(endX, startY + i*gridDim), new Scalar(7, 193, 255), 5);
						
					// Draw columns on image
					// for (int i = 0; i <= width; i++)
					// 	Imgproc.line(frame, new Point(startX + i*gridDim, startY), new Point(startX + i*gridDim, endY), new Scalar(7, 193, 255), 5);
					
					// convert the Mat object (OpenCV) to Image (JavaFX)
					imageToShow = mat2Image(frame);
				}
			} catch (Exception e) {
				// log the error
				System.err.println("Exception during the image elaboration: ");
				e.printStackTrace();
			}
		}
		
		return imageToShow;
	}
	
	/**
	 * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
	 * 
	 * @param frame
	 *            the {@link Mat} representing the current frame
	 * @return the {@link Image} to show
	 */
	private Image mat2Image(Mat frame) {
		// create a temporary buffer
		MatOfByte buffer = new MatOfByte();
		// encode the frame in the buffer
		Imgcodecs.imencode(".png", frame, buffer);
		// build and return an Image created from the image encoded in the
		// buffer
		return new Image(new ByteArrayInputStream(buffer.toArray()));
	}
	
	private Mat detectARContours(Mat colorFrame) {
		Mat frame = new Mat();
		// Convert the image to grayscale
		Imgproc.cvtColor(colorFrame.clone(), frame, Imgproc.COLOR_BGR2GRAY);
		
		// Perform thresholding it determine dark (black) areas.
		Imgproc.threshold(frame, frame, 150, 255, Imgproc.THRESH_BINARY);
		// Imgproc.adaptiveThreshold(frame, frame, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);
		// Erode and then dilate to filter noise / small dots
		Imgproc.erode(frame, frame, kernel, new Point(0, 0), 3);
		Imgproc.dilate(frame, frame, kernel, new Point(0, 0), 3);

		// Find contours (bounding shapes) within image
		ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();    
	    Imgproc.findContours(frame, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

	    for (int i = 0; i < contours.size(); i++) { 
			Imgproc.drawContours(colorFrame, contours, i, new Scalar(0, 127, 255), 2);
			// System.out.println(contours.get(0).rows() + ", " + contours.get(0).cols() + ", " + contours.get(0).row(0).dump());
	    }
		return colorFrame;/*
	    System.out.println("Number of contours: " + contours.size());
	    // Find a contour that has two children.
	    // OpenCV represents tree structure with array of array elements: [Next, Previous, First_Child, Parent]
	    MatOfPoint maxContour = null;
	    for (MatOfPoint outside : contours) {
	    	System.out.println(outside.dump());
	    }
	    return frame;*/
	}
	
	private Mat detectColors(Mat frame) {
		Mat colorDetect = new Mat();
		// Detect colors within an image. This will create a binary image. Erode and then dilate to
		// filter noise / small dots
	    Imgproc.cvtColor(frame, colorDetect, Imgproc.COLOR_BGR2HSV);
		Core.inRange(frame, lowerOrange, upperOrange, colorDetect);
		Imgproc.erode(colorDetect, colorDetect, kernel, new Point(0, 0), 3);
		Imgproc.dilate(colorDetect, colorDetect, kernel, new Point(0, 0), 3);
		
		// Find contours (bounding shapes) within image
		ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();    
	    Imgproc.findContours(colorDetect, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
	    
	    // Find largest contour
	    MatOfPoint maxContour = null;
	    for (MatOfPoint contour : contours)
	    	if (maxContour == null || Imgproc.contourArea(contour) > Imgproc.contourArea(maxContour))
	    		maxContour = contour;
	    
	    // Draw contour outline on image
    	if (maxContour != null && Imgproc.contourArea(maxContour) >= 50) {
    		Rect rect = Imgproc.boundingRect(maxContour);
    		Imgproc.circle(frame, new Point(rect.x + rect.width/2, rect.y + rect.height/2), 3, new Scalar(255, 0, 0), 3);
    		// Imgproc.rectangle(frame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0), 3);
    	}
    	return frame;
	}

}