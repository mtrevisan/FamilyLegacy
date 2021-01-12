package io.github.mtrevisan.familylegacy.ui.utilities;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;


//https://github.com/leonardo-ono/Java3DSphereImageViewer
//https://en.wikipedia.org/wiki/UV_mapping
public class SphericalView extends Canvas implements MouseMotionListener{

	private final int viewportWidth = 800;
	private final int viewportHeight = 600;

	private BufferedImage sphereImage;
	private final BufferedImage viewportImage;
	private int[] imageBuffer;
	private final int[] viewportImageBuffer;
	private static final double FOV = Math.toRadians(110);
	private final double cameraPlaneDistance;
	private double[][][] rayVectors;
	private static final double ACCURACY_FACTOR = 2048;
	private static final int REQUIRED_SIZE = (int)(2 * ACCURACY_FACTOR);
	private final double[] asinTable = new double[REQUIRED_SIZE];
	private final double[] atan2Table = new double[REQUIRED_SIZE * REQUIRED_SIZE];
	private static final double INV_PI = 1 / Math.PI;
	private static final double INV_2PI = 1 / (2 * Math.PI);
	private double currentRotationX, currentRotationY;
	private int dragStartPointX, dragStartPointY;
	private BufferStrategy bs;


	public SphericalView(){
		try{
			final File f = new File("D:\\\\Mauro\\FamilyLegacy\\src\\test\\resources\\factory.jpg");
			BufferedImage sphereTmpImage = ImageIO.read(f);
			sphereImage = new BufferedImage(sphereTmpImage.getWidth(), sphereTmpImage.getHeight(), BufferedImage.TYPE_INT_RGB);
			sphereImage.getGraphics().drawImage(sphereTmpImage, 0, 0, null);
			imageBuffer = ((DataBufferInt)sphereImage.getRaster().getDataBuffer()).getData();
		}catch(IOException ex){
			System.exit(-1);
		}
		viewportImage = new BufferedImage(viewportWidth, viewportHeight, BufferedImage.TYPE_INT_RGB);
		viewportImageBuffer = ((DataBufferInt)viewportImage.getRaster().getDataBuffer()).getData();
		cameraPlaneDistance = (viewportImage.getWidth() / 2) / Math.tan(FOV / 2);
		createRayVectors();
		precalculateAsinAtan2();
		addMouseMotionListener(this);
	}

	private void createRayVectors(){
		final double halfViewportWidth = viewportWidth / 2.;
		final double halfViewportHeight = viewportHeight / 2.;

		rayVectors = new double[viewportWidth][viewportHeight][3];
		for(int y = 0; y < viewportHeight; y ++){
			for(int x = 0; x < viewportWidth; x ++){
				final double vectorX = x - halfViewportWidth;
				final double vectorY = y - halfViewportHeight;
				final double vectorZ = cameraPlaneDistance;
				final double inverseNorm = 1. / Math.sqrt(vectorX * vectorX + vectorY * vectorY + vectorZ * vectorZ);

				rayVectors[x][y][0] = vectorX * inverseNorm;
				rayVectors[x][y][1] = vectorY * inverseNorm;
				rayVectors[x][y][2] = vectorZ * inverseNorm;
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
		new Thread(new Runnable(){
			@Override
			public void run(){
				boolean running = true;
				while(running){
					Graphics2D g = (Graphics2D)bs.getDrawGraphics();
					try{
						draw(g);
						g.dispose();
						bs.show();
					}
					catch(ZeroException ignored){}
				}
			}
		}).start();
	}

	private void draw(Graphics2D g) throws ZeroException{
		final int imageWidth = sphereImage.getWidth();
		final int imageHeight = sphereImage.getHeight();

		final double targetRotationX = (dragStartPointY - viewportHeight / 2.) * 0.025;
		final double targetRotationY = (dragStartPointX - viewportWidth / 2.) * 0.025;
		currentRotationX += (targetRotationX - currentRotationX) * 0.25;
		currentRotationY += (targetRotationY - currentRotationY) * 0.25;
//		currentRotationX += (targetRotationX - currentRotationX) * 0.25;
//		currentRotationY += (targetRotationY - currentRotationY) * 0.25;

		final Quaternion rotation = Quaternion.fromAngles(currentRotationX, currentRotationY, 0.);
		final double[] rotatedVector = new double[3];
//		final double sinRotationX = Math.sin(currentRotationX);
//		final double cosRotationX = Math.cos(currentRotationX);
//		final double sinRotationY = Math.sin(currentRotationY);
//		final double cosRotationY = Math.cos(currentRotationY);
		for(int y = 0; y < viewportHeight; y ++){
			for(int x = 0; x < viewportWidth; x ++){
				rotation.applyRotation(rayVectors[x][y], rotatedVector);
//				double vectorX = rayVectors[x][y][0];
//				double vectorY = rayVectors[x][y][1];
//				double vectorZ = rayVectors[x][y][2];
//
//				//rotate x
//				double tmpVecZ = vectorZ * cosRotationX - vectorY * sinRotationX;
//				final double tmpVecY = vectorZ * sinRotationX + vectorY * cosRotationX;
//				vectorZ = tmpVecZ;
//				vectorY = tmpVecY;
//
//				//rotate y
//				tmpVecZ = vectorZ * cosRotationY - vectorX * sinRotationY;
//				final double tmpVecX = vectorZ * sinRotationY + vectorX * cosRotationY;
//				vectorZ = tmpVecZ;
//				vectorX = tmpVecX;

				final int iX = (int)((rotatedVector[0] + 1.) * ACCURACY_FACTOR);
				final int iY = (int)((rotatedVector[1] + 1.) * ACCURACY_FACTOR);
				final int iZ = (int)((rotatedVector[2] + 1.) * ACCURACY_FACTOR);
//				final int iX = (int)((vectorX + 1.) * ACCURACY_FACTOR);
//				final int iY = (int)((vectorY + 1.) * ACCURACY_FACTOR);
//				final int iZ = (int)((vectorZ + 1.) * ACCURACY_FACTOR);
				final double u = 0.5 + atan2Table[iZ + iX * REQUIRED_SIZE] * INV_2PI;
				final double v = 0.5 - asinTable[iY] * INV_PI;
				final int tx = (int)(imageWidth * u);
				final int ty = (int)(imageHeight * (1. - v));
				final int color = imageBuffer[ty * imageWidth + tx];
				viewportImageBuffer[y * viewportWidth + x] = color;
			}
		}
		g.drawImage(viewportImage, 0, 0, getWidth(), getHeight(), null);
	}

	@Override
	public void mouseDragged(MouseEvent e){
		// do nothing
	}

	@Override
	public void mouseMoved(MouseEvent e){
		dragStartPointX = e.getX();
		dragStartPointY = e.getY();
	}

	public static void main(String[] args){
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run(){
				SphericalView view = new SphericalView();
				JFrame frame = new JFrame();
				frame.setTitle("Java 360 Sphere Image Viewer");
				frame.setSize(800, 600);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setLocationRelativeTo(null);
				frame.getContentPane().add(view);
				frame.setResizable(false);
				frame.setVisible(true);
				view.requestFocus();
				view.start();
			}
		});
	}

}
