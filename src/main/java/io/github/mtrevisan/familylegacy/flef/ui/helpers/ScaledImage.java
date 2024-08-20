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
package io.github.mtrevisan.familylegacy.flef.ui.helpers;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;


public class ScaledImage extends JLabel{

	private static final double ZOOM_MULTIPLICATION_FACTOR = 1.2;
	private static final double MAX_ZOOM = 3.;
	private static final double MIN_ZOOM = 0.5;
	private static final double ROTATION_FACTOR = 0.005;

	/** Maximum FoV [deg]. */
	private static final double MAX_FOV = 180.;
	/** Minimum FoV [deg]. */
	private static final double MIN_FOV = 10.;
	private static final double ACCURACY_FACTOR = 2048;
	private static final int REQUIRED_SIZE = (int)(2. * ACCURACY_FACTOR);
	private static final double INV_PI = 1. / Math.PI;
	private static final double INV_2PI = 1. / (2. * Math.PI);

	private final int RESIZE_EDGE_THRESHOLD = 5;


	private Image image;
	private int imageWidth;
	private int imageHeight;
	private int viewportWidth;
	private int viewportHeight;
	private CropListenerInterface listener;

	//spherical (UV mapped) image data:
	private int[] curvedImageBuffer;
	private BufferedImage viewportImage;
	private int[] viewportImageBuffer;
	private final double[] asinTable = new double[REQUIRED_SIZE];
	private final double[] atan2Table = new double[REQUIRED_SIZE * REQUIRED_SIZE];
	private double[][][] rayVectors;

	//cylindrical (equirectangular horizontal/vertical mapped) image data:
	private boolean cylindrical;
	private boolean cylindricalHorizontal;

	private double minZoom;
	private double maxZoom;
	private boolean initialized;
	private final AffineTransform transformation = new AffineTransform();

	private int windowStartPointX;
	private int windowStartPointY;
	private int windowEndPointX;
	private int windowEndPointY;
	private volatile boolean cropDefinition;
	private int cropStartPointX = -1;
	private int cropStartPointY;
	private int cropEndPointX;
	private int cropEndPointY;
	private int dragStartPointX;
	private int dragStartPointY;
	private volatile char resizingCropEdge;


	public ScaledImage(){
		initComponents();
	}


	public ScaledImage withListener(final CropListenerInterface listener){
		this.listener = listener;

		return this;
	}

	private void initComponents(){
		final ImageMouseListener listener = new ImageMouseListener();
		addMouseListener(listener);
		addMouseMotionListener(listener);
		addMouseWheelListener(listener);


		//add a component listener to handle resize events
		addComponentListener(new ComponentAdapter(){
			@Override
			public void componentResized(final ComponentEvent evt){
				zoomToFitAndCenter();

				repaint();
			}
		});
	}

	/**
	 * NOTE: `icon` MUST BE an {@link ImageIcon}.
	 */
	@Override
	public final void setIcon(final Icon icon){
		if(icon != null){
			Image img = ((ImageIcon)icon).getImage();
			if(!(img instanceof BufferedImage)){
				final BufferedImage bufferedImage = new BufferedImage(img.getWidth(null), img.getHeight(null),
					BufferedImage.TYPE_INT_RGB);
				bufferedImage.getGraphics().drawImage(img, 0, 0, null);
				img = bufferedImage;
			}
			setRectangularImage((BufferedImage)img);
		}
	}

	public final void setRectangularImage(final BufferedImage image){
		this.image = image;
		if(image != null){
			imageWidth = image.getWidth();
			imageHeight = image.getHeight();

			curvedImageBuffer = null;
			cylindrical = false;
			cylindricalHorizontal = false;
			initialized = false;

			repaint();
		}
	}

	public final void setSphericalImage(final BufferedImage image){
		this.image = null;
		if(image != null){
			imageWidth = image.getWidth();
			imageHeight = image.getHeight();

			this.image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
			this.image.getGraphics().drawImage(image, 0, 0, null);
			curvedImageBuffer = ((DataBufferInt)((BufferedImage)this.image).getRaster().getDataBuffer()).getData();

			cylindrical = false;
			cylindricalHorizontal = false;
			initialized = false;

			repaint();
		}
	}

	public final void setCylindricalHorizontalImage(final BufferedImage image){
		setSphericalImage(image);

		cylindrical = true;
		cylindricalHorizontal = true;
	}

	public final void setCylindricalVerticalImage(final BufferedImage image){
		setSphericalImage(image);

		cylindrical = true;
		cylindricalHorizontal = false;
	}

	/**
	 * @return	Whether the images have a spherical or cylindrical mapping.
	 */
	private boolean isCurved(){
		return (curvedImageBuffer != null);
	}

	@Override
	protected final void paintComponent(final Graphics g){
		if(image == null)
			super.paintComponent(g);
		else if(g instanceof Graphics2D){
			final Graphics2D graphics2D = (Graphics2D)g.create();
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			if(!initialized){
				zoomToFitAndCenter();

				if(isCurved()){
					viewportImage = new BufferedImage(viewportWidth, viewportHeight, BufferedImage.TYPE_INT_RGB);
					viewportImageBuffer = ((DataBufferInt)viewportImage.getRaster().getDataBuffer()).getData();

					rayVectors = createRayVectors();
					precalculateAsinAndAtan2Tables();
				}

				initialized = true;
			}
			if(isCurved()){
				try{
					rotateCurvedImage();

					graphics2D.drawImage(viewportImage,
						0, 0,
						viewportWidth, viewportHeight,
						null);
				}
				catch(final ZeroException ze){
					ze.printStackTrace();
				}
			}
			else
				graphics2D.drawImage(image,
					(int)transformation.getTranslateX(), (int)transformation.getTranslateY(),
					transformation.transformX(imageWidth), transformation.transformY(imageHeight),
					0, 0,
					imageWidth, imageHeight,
					null);

			//crop rectangle:
			if(cropStartPointX >= 0){
				graphics2D.setColor(Color.RED);
				drawCropRectangle(graphics2D);
			}

			graphics2D.dispose();
		}
	}

	/**
	 * @see <a href="https://en.wikipedia.org/wiki/UV_mapping">UV mapping</a>
	 * @see <a href="https://github.com/leonardo-ono/Java3DSphereImageViewer">Java3DSphereImageViewer</a>
	 */
	private void rotateCurvedImage() throws ZeroException{
		final double xAngle = (!cylindrical || !cylindricalHorizontal? transformation.getTranslateY() * ROTATION_FACTOR: 0.);
		final double yAngle = (!cylindrical || cylindricalHorizontal? transformation.getTranslateX() * ROTATION_FACTOR: 0.);
		final Quaternion rotation = Quaternion.fromAngles(-xAngle, yAngle, 0.)
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
				final int color = curvedImageBuffer[ty * imageWidth + tx];
				viewportImageBuffer[y * viewportWidth + x] = color;
			}
	}

	private void zoomToFitAndCenter(){
		viewportWidth = getWidth();
		viewportHeight = getHeight();

		int tmpX = windowStartPointX + windowEndPointX;
		int tmpY = windowStartPointY + windowEndPointY;
		int windowWidth = windowEndPointX - windowStartPointX;
		int windowHeight = windowEndPointY - windowStartPointY;
		if(windowWidth <= 0 || windowHeight <= 0){
			tmpX = imageWidth;
			tmpY = imageHeight;
			windowWidth = imageWidth;
			windowHeight = imageHeight;
		}
		final double current = Math.min((double)viewportWidth / windowWidth, (double)viewportHeight / windowHeight);
		minZoom = Math.min(current / 2., MIN_ZOOM);
		maxZoom = Math.max(current * 2., MAX_ZOOM);

		//scale to fit
		final double scale = Math.min(current, 1.);
		//center image
		final double centerX = (viewportWidth - tmpX * scale) / 2.;
		final double centerY = (viewportHeight - tmpY * scale) / 2.;

		transformation.setScale(scale);
		transformation.setTranslation(centerX, centerY);
	}

	private void setCropCursor(){
		if(!cropDefinition && cropStartPointX >= 0){
			final Point mousePoint = getMousePosition();
			if(mousePoint != null){
				//calculate bounds of the crop rectangle
				final int x1 = getWestCoordinate();
				final int y1 = getNorthCoordinate();
				final int x2 = getEastCoordinate();
				final int y2 = getSouthCoordinate();

				//check if mouse is near any edge of the crop rectangle
				if(Math.abs(mousePoint.x - x1) <= RESIZE_EDGE_THRESHOLD || Math.abs(mousePoint.x - x2) <= RESIZE_EDGE_THRESHOLD)
					setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
				else if(Math.abs(mousePoint.y - y1) <= RESIZE_EDGE_THRESHOLD || Math.abs(mousePoint.y - y2) <= RESIZE_EDGE_THRESHOLD)
					setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
				else
					setCursor(Cursor.getDefaultCursor());
			}
		}
	}

	private double[][][] createRayVectors(){
		final double halfViewportWidth = viewportWidth / 2.;
		final double halfViewportHeight = viewportHeight / 2.;
		final double fov = Math.toRadians(Math.max(Math.min(transformation.getScale() * 140., MAX_FOV), MIN_FOV));
		final double cameraPlaneDistance = halfViewportWidth / StrictMath.tan(fov * 0.5);

		final double[][][] rayVectors = new double[viewportWidth][viewportHeight][3];
		for(int y = 0; y < viewportHeight; y ++)
			for(int x = 0; x < viewportWidth; x ++){
				final double vectorX = x - halfViewportWidth;
				final double vectorY = y - halfViewportHeight;
				final double vectorZ = cameraPlaneDistance;
				final double inverseNorm = 1. / Math.sqrt(vectorX * vectorX + vectorY * vectorY + vectorZ * vectorZ);

				rayVectors[x][y][0] = vectorX * inverseNorm;
				rayVectors[x][y][1] = vectorY * inverseNorm;
				rayVectors[x][y][2] = vectorZ * inverseNorm;
			}
		return rayVectors;
	}

	private void precalculateAsinAndAtan2Tables(){
		for(int i = 0; i < 2 * ACCURACY_FACTOR; i ++){
			asinTable[i] = StrictMath.asin((i - ACCURACY_FACTOR) * 1 / ACCURACY_FACTOR);
			for(int j = 0; j < 2 * ACCURACY_FACTOR; j ++){
				final double y = (i - ACCURACY_FACTOR) / ACCURACY_FACTOR;
				final double x = (j - ACCURACY_FACTOR) / ACCURACY_FACTOR;
				atan2Table[i + j * REQUIRED_SIZE] = StrictMath.atan2(y, x);
			}
		}
	}

	private void drawCropRectangle(final Graphics2D g){
		final int x1 = getWestCoordinate();
		final int y1 = getNorthCoordinate();
		final int x2 = getEastCoordinate();
		final int y2 = getSouthCoordinate();
		final int width = Math.abs(x2 - x1);
		final int height = Math.abs(y2 - y1);

		g.drawRect(x1, y1, width, height);
	}

	private int getWestCoordinate(){
		return transformation.transformX(Math.min(cropStartPointX, cropEndPointX));
	}

	private int getEastCoordinate(){
		return transformation.transformX(Math.max(cropStartPointX, cropEndPointX));
	}

	private int getSouthCoordinate(){
		return transformation.transformY(Math.max(cropStartPointY, cropEndPointY));
	}

	private int getNorthCoordinate(){
		return transformation.transformY(Math.min(cropStartPointY, cropEndPointY));
	}

	private int getInverseX(final int x){
		return transformation.transformInverseX(x);
	}

	private int getInverseY(final int y){
		return transformation.transformInverseY(y);
	}

	public final void resetWindow(){
		windowStartPointX = 0;
		windowStartPointY = 0;
		windowEndPointX = 0;
		windowEndPointY = 0;
	}

	public final void setWindow(final int startX, final int startY, final int endX, final int endY){
		windowStartPointX = Math.max(Math.min(startX, imageWidth), 0);
		windowStartPointY = Math.max(Math.min(startY, imageHeight), 0);
		windowEndPointX = Math.max(Math.min(endX, imageWidth), windowStartPointX);
		windowEndPointY = Math.max(Math.min(endY, imageHeight), windowStartPointY);
	}

	public final Point getCropStartPoint(){
		final int x = Math.min(cropStartPointX, cropEndPointX);
		final int y = Math.min(cropStartPointY, cropEndPointY);
		return (x < 0 || y < 0? null: new Point(x, y));
	}

	public final Point getCropEndPoint(){
		final int x = Math.max(cropStartPointX, cropEndPointX);
		final int y = Math.max(cropStartPointY, cropEndPointY);
		return (x < 0 || y < 0? null: new Point(x, y));
	}

	public Rectangle getCrop(){
		final Point start = getCropStartPoint();
		final Point end = getCropEndPoint();
		return (start != null && end != null
			? new Rectangle(start.x, start.y, end.x - start.x, end.y - start.y)
			: null);
	}

	public void setCrop(final Rectangle crop){
		cropStartPointX = crop.x;
		cropStartPointY = crop.y;
		cropEndPointX = crop.x + crop.width;
		cropEndPointY = crop.y + crop.height;
	}


	private class ImageMouseListener extends MouseAdapter{

		@Override
		public void mouseMoved(final MouseEvent evt){
			if(!cropDefinition && cropStartPointX >= 0)
				setCropCursor();
		}

		@Override
		public final void mousePressed(final MouseEvent evt){
			if(SwingUtilities.isRightMouseButton(evt)){
				//right click with left button resets zoom and translation:
				zoomToFitAndCenter();

				repaint();
			}
			else if(SwingUtilities.isLeftMouseButton(evt)){
				if(isNearCropBorder(evt)){
					//calculate bounds of the crop rectangle
					final int x1 = getWestCoordinate();
					final int y1 = getNorthCoordinate();
					final int x2 = getEastCoordinate();
					final int y2 = getSouthCoordinate();

					//check if mouse is near any edge of the crop rectangle
					final Point mousePoint = evt.getPoint();
					if(Math.abs(mousePoint.y - y1) <= RESIZE_EDGE_THRESHOLD)
						resizingCropEdge = 'N';
					else if(Math.abs(mousePoint.y - y2) <= RESIZE_EDGE_THRESHOLD)
						resizingCropEdge = 'S';
					else if(Math.abs(mousePoint.x - x2) <= RESIZE_EDGE_THRESHOLD)
						resizingCropEdge = 'E';
					else if(Math.abs(mousePoint.x - x1) <= RESIZE_EDGE_THRESHOLD)
						resizingCropEdge = 'W';
					else
						resizingCropEdge = 0;
				}
				else if(evt.isControlDown()){
					dragStartPointX = evt.getX();
					dragStartPointY = evt.getY();
				}
				else{
					//crop start point:
					final int x = getInverseX(evt.getX());
					final int y = getInverseY(evt.getY());
					final boolean insideX = (x >= 0 && x <= imageWidth);
					final boolean insideY = (y >= 0 && y <= imageHeight);
					if(insideX && insideY){
						cropStartPointX = x;
						cropStartPointY = y;

						cropDefinition = true;
					}
				}
			}
		}

		private boolean isNearCropBorder(final MouseEvent evt){
			//calculate bounds of the crop rectangle
			final int x1 = getWestCoordinate();
			final int y1 = getNorthCoordinate();
			final int x2 = getEastCoordinate();
			final int y2 = getSouthCoordinate();

			//check if mouse is near any edge of the crop rectangle
			final Point mousePoint = evt.getPoint();
			return (Math.abs(mousePoint.x - x1) <= RESIZE_EDGE_THRESHOLD
				|| Math.abs(mousePoint.x - x2) <= RESIZE_EDGE_THRESHOLD
				|| Math.abs(mousePoint.y - y1) <= RESIZE_EDGE_THRESHOLD
				|| Math.abs(mousePoint.y - y2) <= RESIZE_EDGE_THRESHOLD);
		}

		@Override
		public final void mouseReleased(final MouseEvent evt){
			if(resizingCropEdge != 0)
				resizingCropEdge = 0;
			else if(cropDefinition && evt.getClickCount() == 1){
				cropDefinition = false;

				//warn listener a selection is made
				if(listener != null)
					listener.cropSelected();

				repaint();
			}
		}

		@Override
		public final void mouseDragged(final MouseEvent evt){
			if(SwingUtilities.isLeftMouseButton(evt)){
				if(resizingCropEdge == 'N'){
					final int y = getInverseY(evt.getY());
					final boolean insideY = (y >= 0 && y <= imageHeight);
					if(insideY)
						cropStartPointY = y;
				}
				else if(resizingCropEdge == 'S'){
					final int y = getInverseY(evt.getY());
					final boolean insideY = (y >= 0 && y <= imageHeight);
					if(insideY)
						cropEndPointY = y;
				}
				else if(resizingCropEdge == 'E'){
					final int x = getInverseX(evt.getX());
					final boolean insideX = (x >= 0 && x <= imageWidth);
					if(insideX)
						cropEndPointX = x;
				}
				else if(resizingCropEdge == 'W'){
					final int x = getInverseX(evt.getX());
					final boolean insideX = (x >= 0 && x <= imageWidth);
					if(insideX)
						cropStartPointX = x;
				}
				else if(evt.isControlDown()){
					//pan:
					transformation.addTranslation(evt.getX() - dragStartPointX, evt.getY() - dragStartPointY);

					dragStartPointX = evt.getX();
					dragStartPointY = evt.getY();
				}
				else if(cropDefinition && cropStartPointX >= 0){
					//crop end point:
					final int x = getInverseX(evt.getX());
					final int y = getInverseY(evt.getY());
					cropEndPointX = Math.max(Math.min(x, imageWidth), 0);
					cropEndPointY = Math.max(Math.min(y, imageHeight), 0);
				}

				repaint();
			}
		}

		@Override
		public final void mouseWheelMoved(final MouseWheelEvent evt){
			if(evt.isControlDown()){
				//zoom:
				final double zoomFactor = StrictMath.pow(ZOOM_MULTIPLICATION_FACTOR, evt.getPreciseWheelRotation());
				if(transformation.addZoom(zoomFactor, minZoom, maxZoom, evt.getX(), evt.getY())){
					if(isCurved())
						rayVectors = createRayVectors();

					repaint();
				}
			}
		}

	}

}
