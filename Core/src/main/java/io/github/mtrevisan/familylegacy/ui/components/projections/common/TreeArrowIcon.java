/**
 * Copyright (c) 2026 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.components.projections.common;

import javax.swing.Icon;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;


/**
 * The arrow of the navigation of the tree (a chevron on a thick stem, with round ends), drawn as a vector shape in the colour of
 * the component it is on (the disabled colour, if the component is disabled): at any size and resolution it is as sharp as the
 * text, because no image is scaled.
 * <p>The arrow fills the area of the icon.</p>
 */
final class TreeArrowIcon implements Icon{

	/** Where the arrow points. */
	enum Direction{
		UP(0),
		RIGHT(90),
		DOWN(180),
		LEFT(270);


		private final int degrees;


		Direction(final int degrees){
			this.degrees = degrees;
		}
	}


	/** Height of the arrow, in the unit where its width is 1. */
	private static final double HEIGHT = 3501. / 2662.;
	/** Thickness of the stem and of the arms, in the unit where the width of the arrow is 1. */
	private static final double THICKNESS = 0.24;

	private final Direction direction;
	private final Dimension size;


	/**
	 * @param direction	The way the arrow points.
	 * @param size	The area of the icon (it is the same for all the directions, so that the layouts do not change).
	 */
	TreeArrowIcon(final Direction direction, final Dimension size){
		this.direction = direction;
		this.size = new Dimension(size);
	}


	@Override
	public int getIconWidth(){
		return size.width;
	}

	@Override
	public int getIconHeight(){
		return size.height;
	}

	/** @return	The way the arrow points. */
	Direction direction(){
		return direction;
	}

	@Override
	public void paintIcon(final Component c, final Graphics g, final int x, final int y){
		final boolean sideways = (direction == Direction.LEFT || direction == Direction.RIGHT);

		//the arrow pointing up (1 wide and HEIGHT tall), turned, then stretched to fill the area of the icon exactly (the same
		//way the image it replaces was resized), so that its stem is where the layouts expect it
		final double cap = THICKNESS / 2.;
		final Path2D path = new Path2D.Double();
		path.moveTo(cap, 0.5);
		path.lineTo(0.5, cap);
		path.lineTo(1. - cap, 0.5);
		path.moveTo(0.5, cap);
		path.lineTo(0.5, HEIGHT - cap);
		final Shape shape = new BasicStroke((float)THICKNESS, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
			.createStrokedShape(path);

		final AffineTransform transform = new AffineTransform();
		transform.translate(x, y);
		transform.scale(size.width / (sideways? HEIGHT: 1.), size.height / (sideways? 1.: HEIGHT));
		transform.translate((sideways? HEIGHT: 1.) / 2., (sideways? 1.: HEIGHT) / 2.);
		transform.rotate(Math.toRadians(direction.degrees));
		transform.translate(-0.5, -HEIGHT / 2.);

		final Graphics2D g2 = (Graphics2D)g.create();
		try{
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(colorOn(c));
			g2.fill(transform.createTransformedShape(shape));
		}
		finally{
			g2.dispose();
		}
	}

	private static Color colorOn(final Component c){
		if(c == null)
			return Color.BLACK;
		if(!c.isEnabled()){
			final Color disabled = UIManager.getColor("Label.disabledForeground");
			if(disabled != null)
				return disabled;
		}
		return c.getForeground();
	}

}
