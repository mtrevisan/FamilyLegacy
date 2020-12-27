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

	private static final int MIN_ZOOM_LEVEL = -20;
	private static final int MAX_ZOOM_LEVEL = 10;
	private static final double ZOOM_MULTIPLICATION_FACTOR = 1.2;
	private static final double INITIAL_ZOOM = 1.;

	private boolean initialized = false;
	private final AffineTransform coordTransform = new AffineTransform();
	private Point cutoutStartPoint;
	private Point cutoutEndPoint;
	private Point dragStartPoint;
	private Point dragEndPoint;
	private int zoomLevel = 0;


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
//			final Component parent = (getSize().width < getParent().getSize().width? this: getParent());
//			ImageDrawer.drawScaledImage(icon.getImage(), parent, g, alignmentX, alignmentY, zoom);

			if(g instanceof Graphics2D){
				final Graphics2D graphics2D = (Graphics2D)g.create();

				//image:
				final Image image = icon.getImage();
				if(!initialized){
					final int x = (int)(getWidth() - image.getWidth(null) * INITIAL_ZOOM) / 2;
					final int y = (int)(getHeight() - image.getHeight(null) * INITIAL_ZOOM) / 2;
					coordTransform.translate(x, y);
					coordTransform.scale(INITIAL_ZOOM, INITIAL_ZOOM);

					initialized = true;
				}
				graphics2D.setTransform(coordTransform);
				graphics2D.drawImage(image, 0, 0, this);

				//cutout box:
				if(cutoutEndPoint != null){
					graphics2D.setColor(Color.RED);
					final double zoomFactor = 1. / coordTransform.getScaleX();
					cutoutEndPoint.x = limit(cutoutEndPoint.x, (int)(image.getWidth(null) / zoomFactor));
					cutoutEndPoint.y = limit(cutoutEndPoint.y, (int)(image.getHeight(null) / zoomFactor));
					final int x = (int)((Math.min(cutoutStartPoint.x, cutoutEndPoint.x)) * zoomFactor);
					final int y = (int)((Math.min(cutoutStartPoint.y, cutoutEndPoint.y)) * zoomFactor);
					final int width = (int)(Math.abs(cutoutEndPoint.x - cutoutStartPoint.x) * zoomFactor);
					final int height = (int)(Math.abs(cutoutEndPoint.y - cutoutStartPoint.y) * zoomFactor);
					graphics2D.drawRect(x, y, width, height);
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
					dragStartPoint = evt.getPoint();
					dragEndPoint = null;
				}
				else{
					final int x = (int)(evt.getX() - coordTransform.getTranslateX());
					final int y = (int)(evt.getY() - coordTransform.getTranslateY());
					cutoutStartPoint = new Point(x, y);
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
						final AffineTransform inverse = coordTransform.createInverse();
						final Point2D dragStart = inverse.transform(dragStartPoint, null);
						final Point2D dragEnd = inverse.transform(dragEndPoint, null);

						final double dx = dragEnd.getX() - dragStart.getX();
						final double dy = dragEnd.getY() - dragStart.getY();
						coordTransform.translate(dx, dy);
						dragStartPoint = dragEndPoint;
						dragEndPoint = null;
					}
					catch(final NoninvertibleTransformException ignored){}
				}
				else{
					//cutout start point
//					final double zoomFactor = coordTransform.getScaleX();
					final int x = (int)(evt.getX() - coordTransform.getTranslateX());
					final int y = (int)(evt.getY() - coordTransform.getTranslateY());
					cutoutEndPoint = new Point(x, y);
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
						final Point2D p1 = coordTransform.createInverse()
							.transform(p, null);
						coordTransform.scale(zoomFactor, zoomFactor);
						final Point2D p2 = coordTransform.createInverse()
							.transform(p, null);
						coordTransform.translate(p2.getX() - p1.getX(), p2.getY() - p1.getY());

						repaint();
					}
					catch(final NoninvertibleTransformException ignored){}
				}
			}
		}

	}

}
