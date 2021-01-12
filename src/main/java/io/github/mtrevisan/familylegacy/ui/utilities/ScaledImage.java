/**
 * Copyright (c) 2020 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.ui.utilities;

import io.github.mtrevisan.familylegacy.ui.interfaces.CutoutListenerInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;


public class ScaledImage extends JLabel{

	private static final double ZOOM_MULTIPLICATION_FACTOR = 1.2;
	private static final double MAX_ZOOM = 3.;
	private static final double MIN_ZOOM = 0.5;

	private static final double ACCURACY_FACTOR = 2048;
	private static final int REQUIRED_SIZE = (int)(2. * ACCURACY_FACTOR);
	private static final double INV_PI = 1. / Math.PI;
	private static final double INV_2PI = 1. / (2. * Math.PI);


	private final CutoutListenerInterface listener;
	private Image image;
	private double centerX;
	private double centerY;
	private int imageWidth;
	private int imageHeight;
	private int viewportWidth;
	private int viewportHeight;

	//spherical image data:
	private int[] imageBuffer;
	private BufferedImage viewportImage;
	private int[] viewportImageBuffer;
	private double currentRotationX;
	private double currentRotationY;
	private final double[] asinTable = new double[REQUIRED_SIZE];
	private final double[] atan2Table = new double[REQUIRED_SIZE * REQUIRED_SIZE];
	private double[][][] rayVectors;

	private double minZoom;
	private double maxZoom;
	private boolean initialized;
	private final AffineTransform transformation = new AffineTransform();

	private boolean cutoutDefinition;
	private int cutoutStartPointX = -1;
	private int cutoutStartPointY;
	private int cutoutEndPointX;
	private int cutoutEndPointY;
	private int dragStartPointX;
	private int dragStartPointY;


	public ScaledImage(final CutoutListenerInterface listener){
		this.listener = listener;

		initComponents();
	}

	private void initComponents(){
		final ImageMouseListener listener = new ImageMouseListener();
		addMouseListener(listener);
		addMouseMotionListener(listener);
		addMouseWheelListener(listener);
	}

	/**
	 * NOTE: `icon` MUST BE an {@link ImageIcon}.
	 */
	@Override
	public void setIcon(final Icon icon){
		if(icon != null){
			Image img = ((ImageIcon)icon).getImage();
			if(!(img instanceof BufferedImage)){
				final BufferedImage bufferedImage = new BufferedImage(img.getWidth(null), img.getHeight(null),
					BufferedImage.TYPE_INT_RGB);
				bufferedImage.getGraphics().drawImage(img, 0, 0, null);
				img = bufferedImage;
			}
			setImage((BufferedImage)img);
		}
	}

	public void setImage(final BufferedImage image){
		this.image = image;
		if(image != null){
			imageWidth = image.getWidth();
			imageHeight = image.getHeight();

			imageBuffer = null;
			initialized = false;
		}
	}

	public void setSphericalImage(final BufferedImage image){
		this.image = null;
		if(image != null){
			imageWidth = image.getWidth();
			imageHeight = image.getHeight();

			this.image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
			this.image.getGraphics().drawImage(image, 0, 0, null);
			imageBuffer = ((DataBufferInt)((BufferedImage)this.image).getRaster().getDataBuffer()).getData();

			initialized = false;
		}
	}

	private boolean isSpherical(){
		return (imageBuffer != null);
	}

	@Override
	protected void paintComponent(final Graphics g){
		if(image == null)
			super.paintComponent(g);
		else if(g instanceof Graphics2D){
			final Graphics2D graphics2D = (Graphics2D)g.create();
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			if(!initialized){
				zoomToFitAndCenter();

				if(isSpherical()){
					viewportImage = new BufferedImage(viewportWidth, viewportHeight, BufferedImage.TYPE_INT_RGB);
					viewportImageBuffer = ((DataBufferInt)viewportImage.getRaster().getDataBuffer()).getData();

					rayVectors = createRayVectors();
					precalculateAsinAtan2();
				}

				initialized = true;
			}
			if(isSpherical()){
				try{
					rotateSphericalImage();

					graphics2D.drawImage(viewportImage,
						0, 0, viewportWidth, viewportHeight, null);
				}
				catch(final ZeroException e){
					e.printStackTrace();
				}
			}
			else
				graphics2D.drawImage(image,
					(int)transformation.getTranslateX(), (int)transformation.getTranslateY(),
					transformation.transformX(imageWidth), transformation.transformY(imageHeight),
					0, 0, imageWidth, imageHeight, null);

			//cutout rectangle:
			if(cutoutStartPointX >= 0){
				graphics2D.setColor(Color.RED);
				drawCutoutRectangle(graphics2D);
			}

			graphics2D.dispose();
		}
	}

	/**
	 * @see <a href="https://en.wikipedia.org/wiki/UV_mapping">UV mapping</a>
	 * @see <a href="https://github.com/leonardo-ono/Java3DSphereImageViewer">Java3DSphereImageViewer</a>
	 */
	//TODO
	private void rotateSphericalImage() throws ZeroException{
		final double targetRotationX = (dragStartPointY - viewportHeight / 2.) * 0.025;
		final double targetRotationY = (dragStartPointX - viewportWidth / 2.) * 0.025;
		currentRotationX += (targetRotationX - currentRotationX) * 0.25;
		currentRotationY += (targetRotationY - currentRotationY) * 0.25;

//		currentRotationX = transformation.getTranslateY() * 0.005;
//		currentRotationY = transformation.getTranslateX() * 0.005;

		final Quaternion rotation = Quaternion.fromAngles(-currentRotationX, currentRotationY, 0.)
			.getInverse();
		final double[] rotatedVector = new double[3];
		for(int y = 0; y < viewportHeight; y ++)
			for(int x = 0; x < viewportWidth; x ++){
				rotation.applyRotation(rayVectors[x][y], rotatedVector);
				final int iX = (int)((rotatedVector[0] + 1.) * ACCURACY_FACTOR);
				final int iY = (int)((rotatedVector[1] + 1.) * ACCURACY_FACTOR);
				final int iZ = (int)((rotatedVector[2] + 1.) * ACCURACY_FACTOR);
				final double u = 0.5 + atan2Table[iZ + iX * REQUIRED_SIZE] * INV_2PI;
				final double v = 0.5 - asinTable[iY] * INV_PI;
				final int tx = (int)(imageWidth * u);
				final int ty = (int)(imageHeight * (1. - v));
				final int color = imageBuffer[ty * imageWidth + tx];
				viewportImageBuffer[y * viewportWidth + x] = color;
			}
	}

	private void zoomToFitAndCenter(){
		viewportWidth = getWidth();
		viewportHeight = getHeight();

		final double current = Math.min((double)viewportWidth / imageWidth, (double)viewportHeight / imageHeight);
		minZoom = Math.min(current / 2., MIN_ZOOM);
		maxZoom = Math.max(current * 2., MAX_ZOOM);

		//scale to fit
//		final double scale = Math.min(current, 1.);
//FIXME
final double scale = Math.min(current, 1.) * 3.;
		//center image
		centerX = (viewportWidth - imageWidth * scale) / 2.;
		centerY = (viewportHeight - imageHeight * scale) / 2.;

		transformation.setScale(scale);
		transformation.setTranslation(centerX, centerY);
	}

	private double[][][] createRayVectors(){
		final double halfViewportWidth = viewportWidth / 2.;
		final double halfViewportHeight = viewportHeight / 2.;
//		final double fovZoomFactor = transformation.getScale();
//		System.out.println("scale " + transformation.getScale() + ", new fov " + Math.toDegrees(fov / fovZoomFactor));
		final double fov = Math.toRadians(100.);
		final double cameraPlaneDistance = halfViewportWidth / Math.tan(fov / 2.);
System.out.println(cameraPlaneDistance);

		final double[][][] rayVectors = new double[viewportWidth][viewportHeight][3];
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
		return rayVectors;
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

	private void drawCutoutRectangle(final Graphics2D g){
		final int x1 = transformation.transformX(Math.min(cutoutStartPointX, cutoutEndPointX));
		final int y1 = transformation.transformY(Math.min(cutoutStartPointY, cutoutEndPointY));
		final int x2 = transformation.transformX(Math.max(cutoutStartPointX, cutoutEndPointX));
		final int y2 = transformation.transformY(Math.max(cutoutStartPointY, cutoutEndPointY));
		final int width = Math.abs(x2 - x1);
		final int height = Math.abs(y2 - y1);

		g.drawRect(x1, y1, width, height);
	}

	public Point getCutoutStartPoint(){
		final int x = Math.min(cutoutStartPointX, cutoutEndPointX);
		final int y = Math.min(cutoutStartPointY, cutoutEndPointY);
		return new Point(x, y);
	}

	public void setCutoutStartPoint(final int x, final int y){
		cutoutStartPointX = x;
		cutoutStartPointY = y;
	}

	public Point getCutoutEndPoint(){
		final int x = Math.max(cutoutStartPointX, cutoutEndPointX);
		final int y = Math.max(cutoutStartPointY, cutoutEndPointY);
		return new Point(x, y);
	}

	public void setCutoutEndPoint(final int x, final int y){
		cutoutEndPointX = x;
		cutoutEndPointY = y;
	}


	private class ImageMouseListener extends MouseAdapter{

		@Override
		public void mousePressed(final MouseEvent evt){
			if(SwingUtilities.isRightMouseButton(evt)){
				//right click with left button resets zoom and translation:
				zoomToFitAndCenter();

				repaint();
			}
			else if(SwingUtilities.isLeftMouseButton(evt)){
				if(evt.isControlDown()){
					dragStartPointX = evt.getX();
					dragStartPointY = evt.getY();
				}
				else{
					//cutout start point:
					final int x = transformation.transformInverseX(evt.getX());
					final int y = transformation.transformInverseY(evt.getY());
					final boolean insideX = (x >= 0 && x <= imageWidth);
					final boolean insideY = (y >= 0 && y <= imageHeight);
					if(insideX && insideY){
						cutoutStartPointX = x;
						cutoutStartPointY = y;

						cutoutDefinition = true;
					}
				}
			}
		}

		@Override
		public void mouseReleased(final MouseEvent evt){
			if(cutoutDefinition && evt.getClickCount() == 1){
				cutoutDefinition = false;

				//warn listener a selection is made
				if(listener != null)
					listener.cutoutSelected();
			}
		}

		@Override
		public void mouseDragged(final MouseEvent evt){
			if(SwingUtilities.isLeftMouseButton(evt)){
				if(evt.isControlDown()){
					//pan:
					transformation.addTranslation(evt.getX() - dragStartPointX, evt.getY() - dragStartPointY);

					dragStartPointX = evt.getX();
					dragStartPointY = evt.getY();
				}
				else if(cutoutDefinition && cutoutStartPointX >= 0){
					//cutout end point:
					final int x = transformation.transformInverseX(evt.getX());
					final int y = transformation.transformInverseY(evt.getY());
					cutoutEndPointX = Math.max(Math.min(x, imageWidth), 0);
					cutoutEndPointY = Math.max(Math.min(y, imageHeight), 0);
				}

				repaint();
			}
		}

		@Override
		public void mouseWheelMoved(final MouseWheelEvent evt){
			if(evt.isControlDown()){
				//zoom:
				final double zoomFactor = Math.pow(ZOOM_MULTIPLICATION_FACTOR, evt.getPreciseWheelRotation());
				if(transformation.addZoom(zoomFactor, minZoom, maxZoom, evt.getX(), evt.getY())){
					if(isSpherical())
						rayVectors = createRayVectors();

					repaint();
				}
			}
		}

	}

}
