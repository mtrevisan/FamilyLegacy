package io.github.mtrevisan.familylegacy.ui.utilities;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;


//https://github.com/leonardo-ono/Java3DSphereImageViewer
//https://en.wikipedia.org/wiki/UV_mapping
public class SphericalView extends Canvas{

	private static final double FOV = Math.toRadians(110.);
	private static final double ACCURACY_FACTOR = 2048;
	private static final int REQUIRED_SIZE = (int)(2. * ACCURACY_FACTOR);
	private static final double INV_PI = 1. / Math.PI;
	private static final double INV_2PI = 1. / (2. * Math.PI);

	private static final int imageWidth = 800;
	private static final int imageHeight = 600;

	private final BufferedImage image;
	private final BufferedImage offscreenImage;
	private final int[] imageBuffer;
	private final int[] offscreenImageBuffer;
	private final double cameraPlaneDistance;
	private final double[] asinTable = new double[REQUIRED_SIZE];
	private final double[] atan2Table = new double[REQUIRED_SIZE * REQUIRED_SIZE];
	private double[][][] rayVectors;
	private double currentRotationX;
	private double currentRotationY;
	private int dragStartPointX;
	private int dragStartPointY;
	private BufferStrategy bs;


	public SphericalView() throws IOException{
		final File f = new File("D:\\\\Mauro\\FamilyLegacy\\src\\test\\resources\\factory.jpg");
		final BufferedImage sphereTmpImage = ImageIO.read(f);
		image = new BufferedImage(sphereTmpImage.getWidth(), sphereTmpImage.getHeight(), BufferedImage.TYPE_INT_RGB);
		image.getGraphics().drawImage(sphereTmpImage, 0, 0, null);
		imageBuffer = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();

		offscreenImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		offscreenImageBuffer = ((DataBufferInt)offscreenImage.getRaster().getDataBuffer()).getData();
		cameraPlaneDistance = (offscreenImage.getWidth() / 2.) / Math.tan(FOV / 2.);
		createRayVectors();
		precalculateAsinAtan2();

		addMouseMotionListener(new MouseMotionAdapter(){
			@Override
			public void mouseDragged(final MouseEvent evt){
				if(SwingUtilities.isLeftMouseButton(evt)){
					dragStartPointX = evt.getX();
					dragStartPointY = evt.getY();
				}
			}
		});
	}

	private void createRayVectors(){
		rayVectors = new double[offscreenImage.getWidth()][offscreenImage.getHeight()][3];
		for(int y = 0; y < offscreenImage.getHeight(); y ++){
			for(int x = 0; x < offscreenImage.getWidth(); x ++){
				final double vecX = x - offscreenImage.getWidth() / 2.;
				final double vecY = y - offscreenImage.getHeight() / 2.;
				final double vecZ = cameraPlaneDistance;
				final double invVecLength = 1. / Math.sqrt(vecX * vecX + vecY * vecY + vecZ * vecZ);
				rayVectors[x][y][0] = vecX * invVecLength;
				rayVectors[x][y][1] = vecY * invVecLength;
				rayVectors[x][y][2] = vecZ * invVecLength;
			}
		}
	}

	private void precalculateAsinAtan2(){
		for(int i = 0; i < 2 * ACCURACY_FACTOR; i ++){
			asinTable[i] = Math.asin((i - ACCURACY_FACTOR) * 1 / ACCURACY_FACTOR);
			for(int j = 0; j < 2 * ACCURACY_FACTOR; j ++){
				final double y = (i - ACCURACY_FACTOR) / ACCURACY_FACTOR;
				final double x = (j - ACCURACY_FACTOR) / ACCURACY_FACTOR;
				atan2Table[i + j * REQUIRED_SIZE] = Math.atan2(y, x);
			}
		}
	}

	public void start(){
		createBufferStrategy(2);
		bs = getBufferStrategy();
		new Thread(() -> {
			while(true){
				final Graphics2D g = (Graphics2D)bs.getDrawGraphics();
				draw(g);
				g.dispose();
				bs.show();
			}
		}).start();
	}

	private void draw(final Graphics2D g){
		final double targetRotationX = (dragStartPointY - (offscreenImage.getHeight() / 2.)) * 0.025;
		final double targetRotationY = (dragStartPointX - (offscreenImage.getWidth() / 2.)) * 0.025;
		currentRotationX += (targetRotationX - currentRotationX) * 0.25;
		currentRotationY += (targetRotationY - currentRotationY) * 0.25;
		final double sinRotationX = Math.sin(currentRotationX);
		final double cosRotationX = Math.cos(currentRotationX);
		final double sinRotationY = Math.sin(currentRotationY);
		final double cosRotationY = Math.cos(currentRotationY);
		double tmpVecX, tmpVecY, tmpVecZ;
		for(int y = 0; y < offscreenImage.getHeight(); y ++){
			for(int x = 0; x < offscreenImage.getWidth(); x ++){
				double vecX = rayVectors[x][y][0];
				double vecY = rayVectors[x][y][1];
				double vecZ = rayVectors[x][y][2];
				//rotate x
				tmpVecZ = vecZ * cosRotationX - vecY * sinRotationX;
				tmpVecY = vecZ * sinRotationX + vecY * cosRotationX;
				vecZ = tmpVecZ;
				vecY = tmpVecY;
				//rotate y
				tmpVecZ = vecZ * cosRotationY - vecX * sinRotationY;
				tmpVecX = vecZ * sinRotationY + vecX * cosRotationY;
				vecZ = tmpVecZ;
				vecX = tmpVecX;
				final int iX = (int)((vecX + 1) * ACCURACY_FACTOR);
				final int iY = (int)((vecY + 1) * ACCURACY_FACTOR);
				final int iZ = (int)((vecZ + 1) * ACCURACY_FACTOR);
				//https://en.wikipedia.org/wiki/UV_mapping
				final double u = 0.5 + (atan2Table[iZ + iX * REQUIRED_SIZE] * INV_2PI);
				final double v = 0.5 - (asinTable[iY] * INV_PI);
				final int tx = (int)(image.getWidth() * u);
				final int ty = (int)(image.getHeight() * (1 - v));
				final int color = imageBuffer[ty * image.getWidth() + tx];
				offscreenImageBuffer[y * offscreenImage.getWidth() + x] = color;
			}
		}
		g.drawImage(offscreenImage, 0, 0, getWidth(), getHeight(), null);
	}


	public static void main(final String[] args){
		SwingUtilities.invokeLater(() -> {
			try{
				final SphericalView view = new SphericalView();
				final JFrame frame = new JFrame();
				frame.setTitle("Java 360 Sphere Image Viewer");
				frame.setSize(imageWidth, imageHeight);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setLocationRelativeTo(null);
				frame.getContentPane().add(view);
				frame.setResizable(false);
				frame.setVisible(true);
				view.requestFocus();
				view.start();
			}
			catch(final IOException e){
				e.printStackTrace();
				System.exit(-1);
			}
		});
	}

}
