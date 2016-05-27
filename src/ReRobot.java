import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import lejos.hardware.Battery;
import lejos.hardware.Button;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;

public class ReRobot {
	public static EV3LargeRegulatedMotor right, left;
	public static final int port1 = 1111, port2 = 2222;
	public static int port;
	
	public static void main(String[] args) throws IOException {
		// Setup motors
		right = new EV3LargeRegulatedMotor(MotorPort.A);
		left = new EV3LargeRegulatedMotor(MotorPort.B);
		right.setSpeed(25 * Battery.getVoltage());
		left.setSpeed(25 * Battery.getVoltage());
		System.out.println("Motors set up");
		System.out.println("Battery: " + Battery.getVoltage() + "\n");

		// Let user select which robot this is (which port the robot will have)
		System.out.println("UP for Robot 1, Down for Robot 2");
		System.out.println("Robot 1 is placed on left. Robot 2 on the right.");
		while (true) {
			if (Button.UP.isDown()) {
				port = port1;
				break;
			} else if (Button.DOWN.isDown()) {
				port = port2;
				break;
			}
		}
		
		// Setup wireless communication with PC
		ServerSocket server = new ServerSocket(port);
		System.out.println("Awaiting client..");
		Socket client = server.accept();
		System.out.println("Connected");
		
		// Setup input stream
		InputStream in = client.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		// Read first line
		String line = reader.readLine();
		

		// Loop to move robot based on received commands (stop once "exit" is received).
		while (line != null && !line.equals("exit")) {
			line = line.toLowerCase();
			System.out.println("Command received: " + line);
			if (line.contains("forward")) {
				right.forward();
				left.forward();
			} else if (line.contains("right")) {
				right.backward();
				left.forward();
			} else if (line.contains("left")) {
				right.forward();
				left.backward();
			} else if (line.contains("stop")) {
				right.stop(true);
				left.stop(true);
			}
			// Read next line
			line = reader.readLine();
		}
		
		right.stop(true);
		left.stop(true);
		System.out.println("Exit command has been received.");
		System.out.println("Press any button to exit robot client.");
		Button.waitForAnyPress();

		// Close all streams and sockets
		reader.close();
		in.close();
		client.close();
		server.close();
		right.close();
		left.close();

		/*OutputStream out = client.getOutputStream();
		DataOutputStream dOut = new DataOutputStream(out);
		dOut.writeUTF("Battery: " + Battery.getVoltage());
		dOut.flush();

		InputStream in = client.getInputStream();
		DataInputStream dIn = new DataInputStream(in);
		double d = dIn.readDouble();
		System.out.println(d);*/
	}
}
