package uwl.aruco;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;  
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import wrapper.CameraParameters;
import wrapper.CvDrawingUtils;
import wrapper.Marker;
import wrapper.MarkerDetector;

class DetectionPanel extends JPanel{  
	private static final long serialVersionUID = 1L;  
	private BufferedImage image;  
	// Create a constructor method  
	public DetectionPanel(){  
		super();   
	}  

	public void matToImage(Mat frame) {
		// create a temporary buffer
		MatOfByte buffer = new MatOfByte();
		// encode the frame in the buffer
		Imgcodecs.imencode(".png", frame, buffer);
		try {  
            this.image = ImageIO.read(new ByteArrayInputStream(buffer.toArray()));  
       } catch (IOException e) {  
            e.printStackTrace();  
       }
	}
	
	@Override
	public void paintComponent(Graphics g){  
		super.paintComponent(g);   
		if (this.image==null) return;         
		g.drawImage(this.image, 10, 10, (int) this.image.getWidth(), (int) this.image.getHeight(), null);
	}

}  
public class arucoSimple {  
	public static void main(String arg[]) throws InterruptedException, Exception{  
		// load the native OpenCV library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME); 

		//make the JFrame
		JFrame frame = new JFrame("WebCam Capture - Panel detection");  
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
		//JARUCO VARIABLES
		CameraParameters CamParam = new CameraParameters();
		CvDrawingUtils drawUtil = new CvDrawingUtils();
		MarkerDetector MDetector = new MarkerDetector();
		ArrayList <Marker> Markers;
		Markers = new ArrayList<>();
		float MarkerSize = 150;


		DetectionPanel detectionPanel = new DetectionPanel();  
		frame.setSize(400,400); //give the frame some arbitrary size 
		frame.setBackground(Color.BLUE);
		frame.add(detectionPanel,BorderLayout.CENTER);       
		frame.setVisible(true);       

		//Open and Read from the video stream  
		Mat webcam_image = new Mat();  
		VideoCapture webCam = new VideoCapture(0);   

		if( webCam.isOpened())  
		{  
			Thread.sleep(200); /// This one-time delay allows the Webcam to initialize itself  
			while( true )  
			{  
				webCam.read(webcam_image);  
				if( !webcam_image.empty() )  
				{   
					Markers.clear();

					MDetector.detect(webcam_image,Markers,CamParam,MarkerSize);

					for (int i=0;i<Markers.size();i++) {
						System.out.println(i+": "+Markers.get(i));
						Markers.get(i).draw(webcam_image,new Scalar(0,0,255),2);
						System.out.println(Markers.get(i).getArea() + ", " + Markers.get(i).getPerimeter() + ", " +
								Markers.get(i).getSSize() + ", " + Markers.get(i).get(0) + ", " + Markers.get(i).getCenter() +
								", " + Markers.get(i).getRvec().dump() + ", " + Markers.get(i).getTvec().dump());
					}

					Thread.sleep(50); /// This delay eases the computational load .. with little performance leakage
					frame.setSize(webcam_image.width()+40,webcam_image.height()+60);  

					detectionPanel.matToImage(webcam_image);  
					detectionPanel.repaint(); 
				}  
				else  
				{   
					System.out.println(" --(!) No captured frame from webcam !");   
					break;   
				}  
			}  
		}
		// Release the webcam
		webCam.release();

	} 

	static{
		System.load("/home/zackory/Downloads/aruco-1.2.4/build/src/libaruco.so.1.2");
		System.load("/home/zackory/workspace/EV3 Wifi/libWrapperCPP.so");
	}
}