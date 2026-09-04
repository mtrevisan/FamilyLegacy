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
package io.github.mtrevisan.familylegacy.v2.ui.images;

import io.github.mtrevisan.familylegacy.v2.ui.helpers.ZeroException;

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
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.Serial;


public class ScaledImage extends JLabel{

	@Serial
	private static final long serialVersionUID = -2951121956660972171L;


	private static final int NO_CROP_COORD = -1;

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

	private static final int RESIZE_EDGE_THRESHOLD = 8;
	private static final Color OVERLAY_COLOR = new Color(0, 0, 0, 120);

	private Image image;
	private int imageWidth;
	private int imageHeight;
	private int viewportWidth;
	private int viewportHeight;

	// spherical (UV mapped) image data:
	private int[] curvedImageBuffer;
	private BufferedImage viewportImage;
	private int[] viewportImageBuffer;
	private final double[] asinTable = new double[REQUIRED_SIZE];
	private final double[] atan2Table = new double[REQUIRED_SIZE * REQUIRED_SIZE];
	private double[][][] rayVectors;

	// cylindrical (equirectangular horizontal/vertical mapped) image data:
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
	private int cropStartPointX = NO_CROP_COORD;
	private int cropStartPointY;
	private int cropEndPointX;
	private int cropEndPointY;
	private int dragStartPointX;
	private int dragStartPointY;

	// Handle type: 0, 'N', 'S', 'E', 'W', '1' (NW), '2' (NE), '3' (SW), '4' (SE)
	private volatile char resizingCropEdge;

	private volatile boolean viewOnly;


	public static ScaledImage create(){
		return new ScaledImage();
	}

	public static ScaledImage createViewOnly(){
		final ScaledImage si = new ScaledImage();
		si.viewOnly = true;
		return si;
	}


	private ScaledImage(){
		initComponents();
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
			final Graphics2D g2 = (Graphics2D)g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

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

					g2.drawImage(viewportImage,
						0, 0,
						viewportWidth, viewportHeight,
						null);
				}
				catch(final ZeroException ze){
					ze.printStackTrace();
				}
			}
			else
				g2.drawImage(image,
					(int)transformation.getTranslateX(), (int)transformation.getTranslateY(),
					transformation.transformX(imageWidth), transformation.transformY(imageHeight),
					0, 0,
					imageWidth, imageHeight,
					null);

			// Crop selection & external dimming overlay:
			if(cropStartPointX >= 0)
				drawCropRectangle(g2);

			g2.dispose();
		}
	}

	/**
	 * @see <a href="https://en.wikipedia.org/wiki/UV_mapping">UV mapping</a>
	 * @see <a href="https://github.com/leonardo-ono/Java3DSphereImageViewer">Java3DSphereImageViewer</a>
	 */
	private void rotateCurvedImage() throws ZeroException{
		final double xAngle = (!cylindrical || !cylindricalHorizontal
			? transformation.getTranslateY() * ROTATION_FACTOR
			: 0.);
		final double yAngle = (!cylindrical || cylindricalHorizontal
			? transformation.getTranslateX() * ROTATION_FACTOR
			: 0.);
		final Quaternion rotation = Quaternion.fromAngles(-xAngle, yAngle, 0.).getInverse();
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
				// Check if mouse is within rendered image bounds using real scale
				final int imgScreenX = (int)transformation.getTranslateX();
				final int imgScreenY = (int)transformation.getTranslateY();
				final int imgScreenWidth = (int)Math.round(imageWidth * transformation.getScale());
				final int imgScreenHeight = (int)Math.round(imageHeight * transformation.getScale());

				final boolean insideImage = (mousePoint.x >= imgScreenX && mousePoint.x <= imgScreenX + imgScreenWidth
					&& mousePoint.y >= imgScreenY && mousePoint.y <= imgScreenY + imgScreenHeight);

				if(!insideImage){
					setCursor(Cursor.getDefaultCursor());

					return;
				}

				final char handle = getCropHandleAt(mousePoint);
				switch(handle){
					case '1' -> setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
					case '2' -> setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
					case '3' -> setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
					case '4' -> setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
					case 'N' -> setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
					case 'S' -> setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
					case 'E' -> setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
					case 'W' -> setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
					case 'M' -> setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
					default ->
						setCursor(viewOnly? Cursor.getDefaultCursor(): Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
				}
			}
		}
	}

	private char getCropHandleAt(final Point p){
		final int x1 = getWestCoordinate();
		final int y1 = getNorthCoordinate();
		final int x2 = getEastCoordinate();
		final int y2 = getSouthCoordinate();

		final boolean nearWest = Math.abs(p.x - x1) <= RESIZE_EDGE_THRESHOLD;
		final boolean nearEast = Math.abs(p.x - x2) <= RESIZE_EDGE_THRESHOLD;
		final boolean nearNorth = Math.abs(p.y - y1) <= RESIZE_EDGE_THRESHOLD;
		final boolean nearSouth = Math.abs(p.y - y2) <= RESIZE_EDGE_THRESHOLD;

		final boolean inXRange = p.x >= x1 - RESIZE_EDGE_THRESHOLD && p.x <= x2 + RESIZE_EDGE_THRESHOLD;
		final boolean inYRange = p.y >= y1 - RESIZE_EDGE_THRESHOLD && p.y <= y2 + RESIZE_EDGE_THRESHOLD;

		if(nearNorth && nearWest)
			return '1'; // NW
		if(nearNorth && nearEast)
			return '2'; // NE
		if(nearSouth && nearWest)
			return '3'; // SW
		if(nearSouth && nearEast)
			return '4'; // SE

		if(nearNorth && inXRange)
			return 'N';
		if(nearSouth && inXRange)
			return 'S';
		if(nearEast && inYRange)
			return 'E';
		if(nearWest && inYRange)
			return 'W';

		// Inside crop rectangle check -> Move handle ('M')
		if(p.x > x1 && p.x < x2 && p.y > y1 && p.y < y2)
			return 'M';

		return 0;
	}

	private double[][][] createRayVectors(){
		final double halfViewportWidth = viewportWidth / 2.;
		final double halfViewportHeight = viewportHeight / 2.;
		final double fov = Math.toRadians(Math.clamp(transformation.getScale() * 140., MIN_FOV, MAX_FOV));
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
			asinTable[i] = StrictMath.asin((i - ACCURACY_FACTOR) / ACCURACY_FACTOR);
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

		// Calculate actual rendered image screen bounds correctly
		final int imgScreenX = (int)transformation.getTranslateX();
		final int imgScreenY = (int)transformation.getTranslateY();
		final int imgScreenWidth = (int)Math.round(imageWidth * transformation.getScale());
		final int imgScreenHeight = (int)Math.round(imageHeight * transformation.getScale());

		// 2. Shadow overlay (only within image bounds)
		g.setColor(OVERLAY_COLOR);

		// Clip graphics to the exact image bounds
		final Shape oldClip = g.getClip();
		g.clipRect(imgScreenX, imgScreenY, imgScreenWidth, imgScreenHeight);

		// Top
		g.fillRect(imgScreenX, imgScreenY, imgScreenWidth, Math.max(0, y1 - imgScreenY));
		// Bottom
		g.fillRect(imgScreenX, y2, imgScreenWidth, Math.max(0, (imgScreenY + imgScreenHeight) - y2));
		// Left
		g.fillRect(imgScreenX, y1, Math.max(0, x1 - imgScreenX), height);
		// Right
		g.fillRect(x2, y1, Math.max(0, (imgScreenX + imgScreenWidth) - x2), height);

		// Restore original clip
		g.setClip(oldClip);

		// 3. Crop border
		g.setColor(Color.RED);
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
		windowStartPointX = Math.clamp(startX, 0, imageWidth);
		windowStartPointY = Math.clamp(startY, 0, imageHeight);
		windowEndPointX = Math.clamp(endX, windowStartPointX, imageWidth);
		windowEndPointY = Math.clamp(endY, windowStartPointY, imageHeight);
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

	public void setCrop(Rectangle crop){
		if(crop == null)
			crop = new Rectangle(NO_CROP_COORD, 0, 0, 0);

		cropStartPointX = crop.x;
		cropStartPointY = crop.y;
		cropEndPointX = crop.x + crop.width;
		cropEndPointY = crop.y + crop.height;
	}


	private class ImageMouseListener extends MouseAdapter{

		private static final int DRAG_THRESHOLD = 10;

		private int potentialCropStartX;
		private int potentialCropStartY;
		private Point pressPoint;

		private int cropDragStartX;
		private int cropDragStartY;


		@Override
		public void mouseMoved(final MouseEvent evt){
			if(viewOnly)
				return;

			if(!cropDefinition && cropStartPointX >= 0)
				setCropCursor();
		}

		@Override
		public void mouseEntered(final MouseEvent evt){
			setCropCursor();
		}

		@Override
		public void mouseExited(final MouseEvent evt){
			setCursor(Cursor.getDefaultCursor());
		}

		@Override
		public final void mousePressed(final MouseEvent evt){
			if(SwingUtilities.isRightMouseButton(evt)){
				//right click with left button resets zoom and translation:
				zoomToFitAndCenter();

				repaint();
			}
			else if(SwingUtilities.isLeftMouseButton(evt)){
				if(evt.isControlDown()){
					dragStartPointX = evt.getX();
					dragStartPointY = evt.getY();

					return;
				}
				if(viewOnly)
					return;

				pressPoint = evt.getPoint();
				final char handle = getCropHandleAt(pressPoint);
				if(handle != 0){
					resizingCropEdge = handle;

					// Save initial coordinates for moving/resizing
					cropDragStartX = getInverseX(evt.getX());
					cropDragStartY = getInverseY(evt.getY());
				}
				else{
					// Memorize the potential point, but do NOT start selecting until there is a drag
					potentialCropStartX = Math.clamp(getInverseX(evt.getX()), 0, imageWidth);
					potentialCropStartY = Math.clamp(getInverseY(evt.getY()), 0, imageHeight);
				}
			}
		}

		@Override
		public final void mouseDragged(final MouseEvent evt){
			if(SwingUtilities.isLeftMouseButton(evt)){
				if(evt.isControlDown()){
					//pan:
					transformation.addTranslation(evt.getX() - dragStartPointX, evt.getY() - dragStartPointY);

					dragStartPointX = evt.getX();
					dragStartPointY = evt.getY();
				}

				if(!viewOnly){
					final int x = Math.clamp(getInverseX(evt.getX()), 0, imageWidth);
					final int y = Math.clamp(getInverseY(evt.getY()), 0, imageHeight);

					if(resizingCropEdge == 'M'){
						// Calculate delta offset based on previous drag position
						final int deltaX = x - cropDragStartX;
						final int deltaY = y - cropDragStartY;

						final int minX = Math.min(cropStartPointX, cropEndPointX);
						final int maxX = Math.max(cropStartPointX, cropEndPointX);
						final int minY = Math.min(cropStartPointY, cropEndPointY);
						final int maxY = Math.max(cropStartPointY, cropEndPointY);

						final int width = maxX - minX;
						final int height = maxY - minY;

						// Clamp translation within image boundaries
						final int newMinX = Math.clamp(minX + deltaX, 0, imageWidth - width);
						final int newMinY = Math.clamp(minY + deltaY, 0, imageHeight - height);

						cropStartPointX = newMinX;
						cropEndPointX = newMinX + width;
						cropStartPointY = newMinY;
						cropEndPointY = newMinY + height;

						cropDragStartX = x;
						cropDragStartY = y;
					}
					else if(resizingCropEdge != 0){
						final int minX = Math.min(cropStartPointX, cropEndPointX);
						final int maxX = Math.max(cropStartPointX, cropEndPointX);
						final int minY = Math.min(cropStartPointY, cropEndPointY);
						final int maxY = Math.max(cropStartPointY, cropEndPointY);

						switch(resizingCropEdge){
							// NW
							case '1' -> { cropStartPointX = x; cropEndPointX = maxX; cropStartPointY = y; cropEndPointY = maxY; }
							// NE
							case '2' -> { cropStartPointX = minX; cropEndPointX = x; cropStartPointY = y; cropEndPointY = maxY; }
							// SW
							case '3' -> { cropStartPointX = x; cropEndPointX = maxX; cropStartPointY = minY; cropEndPointY = y; }
							// SE
							case '4' -> { cropStartPointX = minX; cropEndPointX = x; cropStartPointY = minY; cropEndPointY = y; }
							case 'N' -> { cropStartPointY = y; cropEndPointY = maxY; }
							case 'S' -> { cropStartPointY = minY; cropEndPointY = y; }
							case 'W' -> { cropStartPointX = x; cropEndPointX = maxX; }
							case 'E' -> { cropStartPointX = minX; cropEndPointX = x; }
						}
					}
					else{
						// If we are not scaling/moving, check if the distance exceeds the threshold
						if(!cropDefinition && pressPoint != null && pressPoint.distance(evt.getPoint()) >= DRAG_THRESHOLD){
							cropStartPointX = potentialCropStartX;
							cropStartPointY = potentialCropStartY;
							cropDefinition = true;
						}

						if(cropDefinition){
							cropEndPointX = x;
							cropEndPointY = y;
						}
					}
				}

				repaint();
			}
		}

		@Override
		public final void mouseReleased(final MouseEvent evt){
			if(viewOnly)
				return;

			if(resizingCropEdge != 0)
				resizingCropEdge = 0;
			else if(cropDefinition){
				cropDefinition = false;

				// If the created area has zero width or height, cancel the selection
				if(cropStartPointX == cropEndPointX || cropStartPointY == cropEndPointY)
					cropStartPointX = NO_CROP_COORD;

				repaint();
			}

			pressPoint = null;
			setCropCursor();
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
