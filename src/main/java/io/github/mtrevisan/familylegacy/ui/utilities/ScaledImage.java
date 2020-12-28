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

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;


public class ScaledImage extends JLabel{

	private static final double ZOOM_MULTIPLICATION_FACTOR = 1.2;
	private static final double MAX_ABSOLUTE_ZOOM = 3.;
	private static final double MIN_ABSOLUTE_ZOOM = 0.5;

	private Image image;
	private double minZoom;
	private double maxZoom;
	private int imageWidth;
	private int imageHeight;
	private boolean initialized = false;
	private final AffineTransform transformation = new AffineTransform();


	private Point cutoutStartPoint;
	private Point cutoutEndPoint;
	private Point dragStartPoint;
	private Point dragEndPoint;


	public ScaledImage(){
		super();

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
		if(icon != null)
			setImage(((ImageIcon)icon).getImage());
	}

	public void setImage(final Image image){
		this.image = image;
		if(image != null){
			imageWidth = image.getWidth(null);
			imageHeight = image.getHeight(null);
		}
	}

	@Override
	protected void paintComponent(final Graphics g){
		if(image == null)
			super.paintComponent(g);
		else if(g instanceof Graphics2D){
			final Graphics2D graphics2D = (Graphics2D)g.create();
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			//image:
			if(!initialized){
				zoomToFitAndCenter();

				initialized = true;
			}
			//NOTE: if the transformer is used here, like in the example below, there is a problem on the second repaint with insets
			//top and left not zero (the image shifts down and right by the insets amount).
			//`graphics2D.setTransform(transformation);`
			//`graphics2D.drawImage(image, 0, 0, null);`
			graphics2D.drawImage(image,
				(int)transformation.getTranslateX(), (int)transformation.getTranslateY(),
				(int)(transformation.getTranslateX() + imageWidth * transformation.getScaleX()),
				(int)(transformation.getTranslateY() + imageHeight * transformation.getScaleY()),
				0, 0, imageWidth, imageHeight, null);

			//cutout rectangle:
			if(cutoutStartPoint != null && cutoutEndPoint != null){
				graphics2D.setColor(Color.RED);
				drawCutoutRectangle(graphics2D);
			}

			graphics2D.dispose();
		}
	}

	private void zoomToFitAndCenter(){
		final int parentWidth = getWidth();
		final int parentHeight = getHeight();

		final double current = Math.min((double)parentWidth / imageWidth, (double)parentHeight / imageHeight);
		minZoom = Math.min(current / 2., MIN_ABSOLUTE_ZOOM);
		maxZoom = Math.max(current * 2., MAX_ABSOLUTE_ZOOM);

		//zoom to fit
		final double initialZoom = Math.min(current, 1.);
		//center image
		final double x = (parentWidth - imageWidth * initialZoom) / 2.;
		final double y = (parentHeight - imageHeight * initialZoom) / 2.;

		transformation.translate(x, y);
		transformation.scale(initialZoom, initialZoom);
	}

	private void drawCutoutRectangle(final Graphics2D g){
		final int x1 = Math.min(cutoutStartPoint.x, cutoutEndPoint.x);
		final int y1 = Math.min(cutoutStartPoint.y, cutoutEndPoint.y);
		final int x2 = Math.max(cutoutStartPoint.x, cutoutEndPoint.x);
		final int y2 = Math.max(cutoutStartPoint.y, cutoutEndPoint.y);
		final int width = Math.abs(x2 - x1);
		final int height = Math.abs(y2 - y1);
		g.setTransform(transformation);
		g.drawRect(x1, y1, width, height);
	}

	public Point getCutoutStartPoint(){
		final int x = Math.min(cutoutStartPoint.x, cutoutEndPoint.x);
		final int y = Math.min(cutoutStartPoint.y, cutoutEndPoint.y);
		return new Point(x, y);
	}

	public Point getCutoutEndPoint(){
		final int x = Math.max(cutoutStartPoint.x, cutoutEndPoint.x);
		final int y = Math.max(cutoutStartPoint.y, cutoutEndPoint.y);
		return new Point(x, y);
	}


	private class ImageMouseListener extends MouseAdapter implements MouseWheelListener{

		private static final double CUTOUT_MOVE_WIDTH = 5.;

		@Override
		public void mouseMoved(final MouseEvent evt){
			//if point is near a cutout edge, change cursor shape
			if(cutoutStartPoint != null && cutoutEndPoint != null){
				try{
					final Point2D p = transformation.createInverse()
						.transform(evt.getPoint(), null);
					int cursorType = Cursor.DEFAULT_CURSOR;
					//top edge
					if(nearTopEdge(p))
						cursorType = Cursor.N_RESIZE_CURSOR;
					//right edge
					else if(nearRightEdge(p))
						cursorType = Cursor.E_RESIZE_CURSOR;
					//bottom edge
					else if(nearBottomEdge(p))
						cursorType = Cursor.S_RESIZE_CURSOR;
					//left edge
					else if(nearLeftEdge(p))
						cursorType = Cursor.W_RESIZE_CURSOR;
					setCursor(Cursor.getPredefinedCursor(cursorType));
				}
				catch(final NoninvertibleTransformException ignored){}
			}
		}

		private boolean nearTopEdge(final Point2D point){
			final int x1 = Math.min(cutoutStartPoint.x, cutoutEndPoint.x);
			final int y1 = Math.min(cutoutStartPoint.y, cutoutEndPoint.y);
			final int x2 = Math.max(cutoutStartPoint.x, cutoutEndPoint.x);
			final int y2 = Math.max(cutoutStartPoint.y, cutoutEndPoint.y);
			return (distanceFromEdge(point, new Point(x1, y1), new Point(x2, y1)) < CUTOUT_MOVE_WIDTH);
		}

		private boolean nearRightEdge(final Point2D point){
			final int x1 = Math.min(cutoutStartPoint.x, cutoutEndPoint.x);
			final int y1 = Math.min(cutoutStartPoint.y, cutoutEndPoint.y);
			final int x2 = Math.max(cutoutStartPoint.x, cutoutEndPoint.x);
			final int y2 = Math.max(cutoutStartPoint.y, cutoutEndPoint.y);
			return (distanceFromEdge(point, new Point(x2, y1), new Point(x2, y2)) < CUTOUT_MOVE_WIDTH);
		}

		private boolean nearBottomEdge(final Point2D point){
			final int x1 = Math.min(cutoutStartPoint.x, cutoutEndPoint.x);
			final int y1 = Math.min(cutoutStartPoint.y, cutoutEndPoint.y);
			final int x2 = Math.max(cutoutStartPoint.x, cutoutEndPoint.x);
			final int y2 = Math.max(cutoutStartPoint.y, cutoutEndPoint.y);
			return (distanceFromEdge(point, new Point(x1, y2), new Point(x2, y2)) < CUTOUT_MOVE_WIDTH);
		}

		private boolean nearLeftEdge(final Point2D point){
			final int x1 = Math.min(cutoutStartPoint.x, cutoutEndPoint.x);
			final int y1 = Math.min(cutoutStartPoint.y, cutoutEndPoint.y);
			final int x2 = Math.max(cutoutStartPoint.x, cutoutEndPoint.x);
			final int y2 = Math.max(cutoutStartPoint.y, cutoutEndPoint.y);
			return (distanceFromEdge(point, new Point(x1, y1), new Point(x1, y2)) < CUTOUT_MOVE_WIDTH);
		}

		private double distanceFromEdge(final Point2D point, final Point vertex1, final Point vertex2){
			final double edgeX = vertex2.x - vertex1.x;
			final double edgeY = vertex2.y - vertex1.y;
			final double u = ((point.getX() - vertex1.x) * edgeX + (point.getY() - vertex1.y) * edgeY) / (edgeX * edgeX + edgeY * edgeY);
			if(u < 0. || u > 1.)
				return Double.MAX_VALUE;

			final double x = vertex1.x + edgeX * u;
			final double y = vertex1.y + edgeY * u;
			final double dx = x - point.getX();
			final double dy = y - point.getY();
			return Math.sqrt(dx * dx + dy * dy);
		}

		@Override
		public void mousePressed(final MouseEvent evt){
			if(SwingUtilities.isLeftMouseButton(evt)){
				if(evt.isControlDown()){
					//TODO change cursor shape if near a pre-existing line, plus, change only the edge if dragged
					dragStartPoint = evt.getPoint();
					dragEndPoint = null;
				}
				}
				else{
					//cutout start point
					try{
						final Point2D p = transformation.createInverse()
							.transform(evt.getPoint(), null);

						final int x = (int)p.getX();
						final int y = (int)p.getY();
						final boolean insideX = (x >= 0. && x < imageWidth);
						final boolean insideY = y >= 0. && y < imageHeight;
						cutoutStartPoint = (insideX && insideY? new Point(x, y): null);
					}
					catch(final NoninvertibleTransformException ignored){}
				}
			}
		}

		@Override
		public void mouseDragged(final MouseEvent evt){
			if(SwingUtilities.isLeftMouseButton(evt)){
				if(!evt.isControlDown()){
					//cutout end point
					try{
						final Point2D p = transformation.createInverse()
							.transform(evt.getPoint(), null);
						final int x = (int)p.getX();
						final int y = (int)p.getY();
						final boolean insideX = (x >= 0. && x < imageWidth);
						final boolean insideY = y >= 0. && y < imageHeight;
						if(cutoutEndPoint != null){
							if(insideX)
								cutoutEndPoint.x = x;
							if(insideY)
								cutoutEndPoint.y = y;
						}
						else if(insideX && insideY)
							cutoutEndPoint = new Point(x, y);
					}
					catch(final NoninvertibleTransformException ignored){}
				}
				else if(cutoutStartPoint != null){
					try{
						//pan
						dragEndPoint = evt.getPoint();
						final AffineTransform inverse = transformation.createInverse();
						final Point2D dragStart = inverse.transform(dragStartPoint, null);
						final Point2D dragEnd = inverse.transform(dragEndPoint, null);

						final double dx = dragEnd.getX() - dragStart.getX();
						final double dy = dragEnd.getY() - dragStart.getY();
						transformation.translate(dx, dy);
						dragStartPoint = dragEndPoint;
						dragEndPoint = null;
					}
					catch(final NoninvertibleTransformException ignored){}
				}

				repaint();
			}
		}

		@Override
		public void mouseWheelMoved(final MouseWheelEvent evt){
			if(evt.isControlDown()){
				//zoom
				final double zoomFactor = (evt.getWheelRotation() > 0? 1. / ZOOM_MULTIPLICATION_FACTOR: ZOOM_MULTIPLICATION_FACTOR);
				final double newZoom = transformation.getScaleX() * zoomFactor;
				if(minZoom <= newZoom && newZoom <= maxZoom){
					try{
						final Point p = evt.getPoint();
						final Point2D p1 = transformation.createInverse()
							.transform(p, null);
						transformation.scale(zoomFactor, zoomFactor);
						final Point2D p2 = transformation.createInverse()
							.transform(p, null);
						transformation.translate(p2.getX() - p1.getX(), p2.getY() - p1.getY());

						repaint();
					}
					catch(final NoninvertibleTransformException ignored){}
				}
			}
		}

	}

}
