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


public class ScaledImageLabel extends JLabel{

	private static final double ZOOM_MULTIPLICATION_FACTOR = 1.2;
	private static final double MAX_ZOOM = 2.;

	private static final int MIN_ZOOM_LEVEL;
	private static final int MAX_ZOOM_LEVEL;
	static{
		MAX_ZOOM_LEVEL = (int)Math.round(Math.log(MAX_ZOOM) / Math.log(ZOOM_MULTIPLICATION_FACTOR));
		MIN_ZOOM_LEVEL = -MAX_ZOOM_LEVEL;
	}

	private boolean initialized = false;
	private final AffineTransform transformation = new AffineTransform();

	private Point cutoutStartPoint;
	private Point cutoutEndPoint;
	private Point dragStartPoint;
	private Point dragEndPoint;
	private int zoomLevel;


	public ScaledImageLabel(){
		super();

		initComponents();
	}

	private void initComponents(){
		final MyMouseListener listener = new MyMouseListener();
		addMouseListener(listener);
		addMouseMotionListener(listener);
		addMouseWheelListener(listener);
	}

	@Override
	protected void paintComponent(final Graphics g){
		final ImageIcon icon = (ImageIcon)getIcon();
		if(icon != null){
			if(g instanceof Graphics2D){
				final Graphics2D graphics2D = (Graphics2D)g.create();

				//image:
				final Image image = icon.getImage();
				if(!initialized){
					final int parentWidth = getWidth();
					final int parentHeight = getHeight();
					final double imageWidth = image.getWidth(null);
					final double imageHeight = image.getHeight(null);

					//zoom to fit
					final double initialZoom = Math.min(Math.min(parentWidth / imageWidth, parentHeight / imageHeight), 1.);
					//center image
					final int x = (int)(parentWidth - imageWidth * initialZoom) / 2;
					final int y = (int)(parentHeight - imageHeight * initialZoom) / 2;

					transformation.translate(x, y);
					transformation.scale(initialZoom, initialZoom);

					initialized = true;
				}
				graphics2D.setTransform(transformation);
				graphics2D.drawImage(image, 0, 0, this);

				//cutout box:
				if(cutoutEndPoint != null){
					graphics2D.setColor(Color.RED);
					final int x2 = limit(cutoutEndPoint.x, image.getWidth(null));
					final int y2 = limit(cutoutEndPoint.y, image.getHeight(null));
					final int x1 = Math.min(cutoutStartPoint.x, x2);
					final int y1 = Math.min(cutoutStartPoint.y, y2);
					final int width = Math.abs(x2 - cutoutStartPoint.x);
					final int height = Math.abs(y2 - cutoutStartPoint.y);
					graphics2D.drawRect(x1, y1, width, height);
				}

				graphics2D.dispose();
			}
		}
		else
			super.paintComponent(g);
	}

	private int limit(final int value, final int max){
		return Math.min(Math.max(value, 0), max);
	}

	public Point getCutoutStartPoint(){
		return cutoutStartPoint;
	}

	public Point getCutoutEndPoint(){
		return cutoutEndPoint;
	}


	private class MyMouseListener extends MouseAdapter implements MouseWheelListener{

		@Override
		public void mousePressed(final MouseEvent evt){
			if(SwingUtilities.isLeftMouseButton(evt)){
				if(evt.isControlDown()){
					//TODO change cursor shape if near a pre-existing line, plus, change only the edge if clicked
					dragStartPoint = evt.getPoint();
					dragEndPoint = null;
				}
				else{
					//cutout start point
					try{
						final Point2D p = transformation.createInverse()
							.transform(evt.getPoint(), null);
						cutoutStartPoint = new Point((int)p.getX(), (int)p.getY());
					}
					catch(final NoninvertibleTransformException ignored){}
				}
			}
		}

		@Override
		public void mouseDragged(final MouseEvent evt){
			if(SwingUtilities.isLeftMouseButton(evt)){
				if(evt.isControlDown()){
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
				else{
					//cutout end point
					try{
						final Point2D p = transformation.createInverse()
							.transform(evt.getPoint(), null);
						cutoutEndPoint = new Point((int)p.getX(), (int)p.getY());
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
				final int wheelRotation = evt.getWheelRotation();
				double zoomFactor = 0.;
				if(wheelRotation > 0 && zoomLevel < MAX_ZOOM_LEVEL){
					zoomLevel ++;
					zoomFactor = 1. / ZOOM_MULTIPLICATION_FACTOR;
				}
				else if(wheelRotation < 0 && zoomLevel > MIN_ZOOM_LEVEL){
					zoomLevel --;
					zoomFactor = ZOOM_MULTIPLICATION_FACTOR;
				}

				if(zoomFactor != 0.){
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
