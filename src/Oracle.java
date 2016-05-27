import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import uwl.learning.QLearningSimple;
import uwl.learning.QLearningSimple.State;

public class Oracle {
	private QLearningSimple qlearn;
	private State state1, state2;
	private CameraDetector camera;
	
	private double startX, endX, gridDim, startY, endY;
	
	private BufferedReader robot1In, robot2In;
	private BufferedWriter robot1Out, robot2Out;
	
	private double range = Math.PI / 36.0;
	
	private Imshow imshow;

	public static void main(String[] args) {
		// load the native OpenCV library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		try {
			new Oracle();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Oracle() throws UnknownHostException, IOException {
		// Set up QLearning (this will train the Q matrix for each agent automatically)
		// TODO: Save trained Q matrices for each agent
		qlearn = new QLearningSimple("world.txt", "trainedQ.txt");
		state1 = qlearn.start1;
		state2 = qlearn.start2;
		camera = new CameraDetector(1, null);
		
		// Test actions
		// System.out.println("(" + state1 + ") | ");
		// for (int i = 0; i < 20; i++) {
		// 	takeNextAction();
		// 	System.out.println("(" + state1 + ") | ");
		// }
		// System.exit(0);

		imshow = new Imshow("Video Preview", camera.width / 2, camera.height / 2);
		imshow.Window.setResizable(true);
		
		// Setup grid dimensions
		startX = camera.width / 8.0;
		endX = camera.width * 7.0 / 8.0;
		gridDim = (endX - startX) / qlearn.width;
		startY = camera.height/2.0 - (gridDim * qlearn.height)/2.0;
		endY = camera.height/2.0 + (gridDim * qlearn.height)/2.0;
		
		System.out.println(camera.width + ", " + camera.height + ", " + startX + ", " + endX + ", " + gridDim);
		
		for (int i = 0; i < 1000000; i++) {
			imshow.showImage(drawGrid(camera.readFrame().clone()));
		}

		imshow.showImage(drawGrid(camera.frame.clone()));
		
		// Establish connections to robots
		String ip = "10.0.1.1"; // Bluetooth IP
		Socket socket1 = new Socket(ip, Robot.port1);
		System.out.println("Connected to Robot 1.");
		Socket socket2 = new Socket(ip, Robot.port2);
		System.out.println("Connected to Robot 2.");

		// Setup input and output streams for first robot
		robot1In = new BufferedReader(new InputStreamReader(socket1.getInputStream()));
		robot1Out = new BufferedWriter(new OutputStreamWriter(socket1.getOutputStream()));
		// Setup input and output streams for second robot
		robot2In = new BufferedReader(new InputStreamReader(socket2.getInputStream()));
		robot2Out = new BufferedWriter(new OutputStreamWriter(socket2.getOutputStream()));

		System.out.println("Buffered readers and writers established for both robots.");
		
		// Loop for x number of grid movements
		for (int i = 0; i < 100; i++) {
			// Determine next action for each robot
			takeNextAction();
			
			boolean robot1Moved = false;
			boolean robot2Moved = false;
			// Loop until each robot has moved into their next grid space
			while (!robot1Moved && !robot2Moved) {
				if (!robot1Moved) {
					// Move robot 1
					if (moveRobotToState(1, state1, robot1Out)) {
						robot1Moved = true;
						System.out.println("Robot 1 has successfully reached grid state: (" + state1.toString() + ")");
					}
				}
				
				if (!robot2Moved) {
					// Move robot 2
					if (moveRobotToState(2, state2, robot2Out)) {
						robot2Moved = true;
						System.out.println("Robot 2 has successfully reached grid state: (" + state2.toString() + ")");
					}
				}
				imshow.showImage(drawGrid(camera.frame.clone()));
			}
		}
		
		// Close everything
		robot1Out.write("exit\n");
		robot2Out.write("exit\n");
		robot1In.close();
		robot1Out.close();
		robot2In.close();
		robot2Out.close();
		socket1.close();
		socket2.close();
		camera.closeCamera();
	}
	
	public Mat drawGrid(Mat frame) {
		// Draw rows on image
		for (int i = 0; i <= qlearn.height; i++)
			Imgproc.line(frame, new Point(startX, startY + i*gridDim), new Point(endX, startY + i*gridDim), new Scalar(7, 193, 255), 5);
		// Draw columns on image
		for (int i = 0; i <= qlearn.width; i++)
			Imgproc.line(frame, new Point(startX + i*gridDim, startY), new Point(startX + i*gridDim, endY), new Scalar(7, 193, 255), 5);
		return frame;
	}

	public void takeNextAction() {
		State s1 = qlearn.updateState(qlearn.agent1, state1);
		State s2 = qlearn.updateState(qlearn.agent2, state2);
		
		// Make sure new states are not null and do not conflict
		// TODO: May also need to verify that the robots do not run into another robot's boxes
		System.out.println(s1 + " | " + s2);
		if (s1 != null && s2 != null && s1.equals(s2))
			return;
		if (s1 != null)
			state1 = s1;
		if (s2 != null)
			state2 = s2;
	}
	
	// Returns true if the robot has reached the center of the grid state, else false
	public boolean moveRobotToState(int robotId, State robotState, BufferedWriter writer) throws IOException {
		// Use OpenCV to determine direction and compare to direction of (state1 - robotCurrentState)
		
		CameraDetector.DirectionPosition dirPos = camera.getDirection(robotId);
		double robotDir = dirPos.direction;
		// Determine the camera (x, y) position for the center of this grid state.
		double camStateX = robotState.x * gridDim + startX + gridDim/2;
		double camStateY = robotState.y * gridDim + startY + gridDim/2;
		// Determine angle (-180 to 180 deg) from robot's (x, y) position to state's center (x, y) position
		double dirToState = Math.atan2(camStateY - dirPos.centerY, camStateX - dirPos.centerX);
		
		// Check if robot is close to center of grid (are x and y positions within a range of 15?)
		if (Math.abs(camStateX - robotState.x) < 15 && Math.abs(camStateY - robotState.y) < 15) {
			// Robot has reached the center of next grid state
			writer.write("stop\n");
			return true;
		} else if ((robotDir >= dirToState - range && robotDir <= dirToState + range) || 
				(robotDir + 360 >= dirToState - range && robotDir + 360 <= dirToState + range) ||
				(robotDir >= dirToState + 360 - range && robotDir <= dirToState + 360 + range)) {
			// Move Forward
			writer.write("forward\n");
		} else {
			// Either turn left or right, depending on whichever is closest
			// Just turn left, this is a crude solution
			writer.write("left\n");
		}
		return false;
	}

}
