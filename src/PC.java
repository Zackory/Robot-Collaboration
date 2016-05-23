import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import uwl.aruco.Aruco;

public class PC {

	public static void main(String[] args) throws IOException {
		String ip = "10.0.1.1"; // BT
		if(args.length > 0)
			ip = args[0];
		Socket sock = new Socket(ip, Robot.port);
		System.out.println("Connected");
		
		Aruco.getPredefinedDictionary();
		
		/*InputStream in = sock.getInputStream();
		DataInputStream dIn = new DataInputStream(in);
		String str = dIn.readUTF();
		System.out.println(str);
		
		OutputStream out = sock.getOutputStream();
		DataOutputStream dOut = new DataOutputStream(out);
		dOut.writeDouble(Math.PI);
		dOut.flush();*/
		
		/*OutputStream out = sock.getOutputStream();
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out));
		w.write("");*/

		// Setup new q-learning world
		
		// Read starting locations of robots and verify they are correct (wait until correct)
		
		// Get next action for both robots
		// Take action, using camera to adjust (action should be guaranteed with camera)
		// while (not within 1 inch of center of grid square)
		// if not within 5 degrees of movement direction, then send command to turn in proper direction
		// else send command to move forward
		// Determine new state and collect any reward
		
		sock.close();
	}
}