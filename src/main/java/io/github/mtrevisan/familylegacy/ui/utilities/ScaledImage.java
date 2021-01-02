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
import java.awt.event.MouseWheelListener;


public class ScaledImage extends JLabel{

	private static final double ZOOM_MULTIPLICATION_FACTOR = 1.2;
	private static final double MAX_ABSOLUTE_ZOOM = 3.;
	private static final double MIN_ABSOLUTE_ZOOM = 0.5;

	private final CutoutListenerInterface listener;
	private Image image;
	private int imageWidth;
	private int imageHeight;
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
		super();

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
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			//image:
			if(!initialized){
				zoomToFitAndCenter();

				initialized = true;
			}
			graphics2D.drawImage(image,
				(int)transformation.getTranslateX(), (int)transformation.getTranslateY(),
				transformation.transformX(imageWidth),
				transformation.transformY(imageHeight),
				0, 0, imageWidth, imageHeight, null);

			//cutout rectangle:
			if(cutoutStartPointX >= 0){
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

		//scale to fit
		final double scale = Math.min(current, 1.);
		//center image
		final double x = (parentWidth - imageWidth * scale) / 2.;
		final double y = (parentHeight - imageHeight * scale) / 2.;

		transformation.setScale(scale);
		transformation.setTranslation(x, y);
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


	private class ImageMouseListener extends MouseAdapter implements MouseWheelListener{

		@Override
		public void mousePressed(final MouseEvent evt){
			if(SwingUtilities.isRightMouseButton(evt)){
				//right click with left button resets zoom and translation
				zoomToFitAndCenter();

				repaint();
			}
			else if(SwingUtilities.isLeftMouseButton(evt)){
				if(evt.isControlDown()){
					dragStartPointX = evt.getX();
					dragStartPointY = evt.getY();
				}
				else{
					//cutout start point
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
					//pan
					transformation.addTranslation(evt.getX() - dragStartPointX, evt.getY() - dragStartPointY);

					dragStartPointX = evt.getX();
					dragStartPointY = evt.getY();
				}
				else if(cutoutDefinition && cutoutStartPointX >= 0){
					//cutout end point
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
				//zoom
				final double zoomFactor = Math.pow(ZOOM_MULTIPLICATION_FACTOR, evt.getPreciseWheelRotation());
				if(transformation.addZoom(zoomFactor, minZoom, maxZoom, evt.getX(), evt.getY()))
					repaint();
			}
		}

	}

}
