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
package io.github.mtrevisan.familylegacy.ui.helpers;


import javax.swing.Icon;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;


/**
 * The symbols of the interface (cross, warning, information, tick, arrows, ...) as vector shapes, instead of characters of a font: a
 * font may not have them (Java draws an empty box, but for the logical fonts), a drawing always looks the same. They are drawn at
 * the size and the resolution of the screen (no image is scaled, so nothing gets blurred) and with the strokes aligned to the
 * pixels, to be as sharp as the text.
 * <p>They are painted in a colour: the one of the component the icon is on
 * (the foreground, the disabled one when the component is disabled), so it follows the theme, the selection of a list and the
 * blinking; give it a colour only when the symbol has a meaning of its own (e.g. red for an error). Its size follows the size of
 * the font of the interface.</p>
 */
public final class SymbolIcon implements Icon{

		/** The symbols that can be drawn. */
	public enum Symbol{
		/** ✖ and ✕ */
		CROSS,
		/** ✓ */
		CHECK,
		/** ⚠ */
		WARNING,
		/** ℹ */
		INFORMATION,
		/** ▶ and ► */
		PLAY_RIGHT,
		/** ◀ and ◄ */
		PLAY_LEFT,
		/** ▲ */
		UP,
		/** ▼ */
		DOWN,
		/** ▸ (a heading that can be opened) */
		SMALL_RIGHT,
		/** ▾ (a heading that is open) */
		SMALL_DOWN,
		/** → */
		ARROW_RIGHT,
		/** ← */
		ARROW_LEFT,
		/** ⊕ */
		PLUS_CIRCLE,
		/** ↺ */
		RESET,
		/** 📅 */
		CALENDAR,
		/** ⇱ */
		DOCK
	}


	private static final float STROKE = 0.14f;
	private static final float SIZE_FACTOR = 1.15f;
	private static final int MINIMUM_SIZE = 10;

	private final Symbol symbol;
	private final Color color;


	private SymbolIcon(final Symbol symbol, final Color color){
		this.symbol = symbol;
		this.color = color;
	}

	/** @return	The icon, in the colour of the component it is painted on. */
	public static SymbolIcon of(final Symbol symbol){
		return new SymbolIcon(symbol, null);
	}

	/** @return	The icon, always in the given colour. */
	public static SymbolIcon of(final Symbol symbol, final Color color){
		return new SymbolIcon(symbol, color);
	}

	/** @return	The symbol the icon draws. */
	public Symbol symbol(){
		return symbol;
	}


	/** @return	The side of the icons, that follows the size of the font of the interface. */
	public static int defaultSize(){
		final Font font = UIManager.getFont("Label.font");
		return Math.max(MINIMUM_SIZE, Math.round((font != null? font.getSize(): 12) * SIZE_FACTOR));
	}

	@Override
	public int getIconWidth(){
		return defaultSize();
	}

	@Override
	public int getIconHeight(){
		return defaultSize();
	}

	@Override
	public void paintIcon(final Component c, final Graphics g, final int x, final int y){
		paint(g, x, y, getIconWidth(), colorOn(c));
	}

	private Color colorOn(final Component c){
		if(color != null)
			return color;
		if(c == null)
			return Color.BLACK;
		if(!c.isEnabled()){
			final Color disabled = UIManager.getColor("Label.disabledForeground");
			if(disabled != null)
				return disabled;
		}
		return c.getForeground();
	}

	/** Paints the symbol in the square of the given side with the top left corner in ({@code x}, {@code y}). */
	public void paint(final Graphics g, final int x, final int y, final int size, final Color paint){
		draw(symbol, g, x, y, size, paint);
	}


	/** Draws the symbol, as vector shapes, in the square of the given side with the top left corner in ({@code x}, {@code y}). */
	private static void draw(final Symbol symbol, final Graphics g, final int x, final int y, final int size, final Color paint){
		final Graphics2D g2 = (Graphics2D)g.create();
		try{
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
			g2.setColor(paint);
			g2.translate(x, y);
			g2.scale(size, size);
			g2.setStroke(new BasicStroke(STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

			switch(symbol){
				case CROSS -> {
					g2.draw(new Line2D.Float(0.22f, 0.22f, 0.78f, 0.78f));
					g2.draw(new Line2D.Float(0.78f, 0.22f, 0.22f, 0.78f));
				}
				case CHECK -> g2.draw(polyline(0.15f, 0.55f, 0.4f, 0.8f, 0.85f, 0.25f));
				case WARNING -> g2.fill(warning());
				case INFORMATION -> g2.fill(information());
				case PLAY_RIGHT -> g2.fill(polygon(0.25f, 0.1f, 0.9f, 0.5f, 0.25f, 0.9f));
				case PLAY_LEFT -> g2.fill(polygon(0.75f, 0.1f, 0.1f, 0.5f, 0.75f, 0.9f));
				case UP -> g2.fill(polygon(0.1f, 0.78f, 0.5f, 0.2f, 0.9f, 0.78f));
				case DOWN -> g2.fill(polygon(0.1f, 0.22f, 0.5f, 0.8f, 0.9f, 0.22f));
				case SMALL_RIGHT -> g2.fill(polygon(0.32f, 0.2f, 0.78f, 0.5f, 0.32f, 0.8f));
				case SMALL_DOWN -> g2.fill(polygon(0.2f, 0.32f, 0.5f, 0.78f, 0.8f, 0.32f));
				case ARROW_RIGHT -> arrow(g2, true);
				case ARROW_LEFT -> arrow(g2, false);
				case PLUS_CIRCLE -> {
					g2.setStroke(new BasicStroke(STROKE * 0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					g2.draw(new Ellipse2D.Float(0.1f, 0.1f, 0.8f, 0.8f));
					g2.draw(new Line2D.Float(0.5f, 0.28f, 0.5f, 0.72f));
					g2.draw(new Line2D.Float(0.28f, 0.5f, 0.72f, 0.5f));
				}
				case RESET -> {
					g2.draw(new Arc2D.Float(0.16f, 0.16f, 0.68f, 0.68f, 100f, 260f, Arc2D.OPEN));
					g2.fill(polygon(0.64f, 0.5f, 1.0f, 0.5f, 0.82f, 0.22f));
				}
				case CALENDAR -> calendar(g2);
				case DOCK -> {
					g2.draw(polyline(0.15f, 0.5f, 0.15f, 0.15f, 0.5f, 0.15f));
					g2.draw(new Line2D.Float(0.15f, 0.15f, 0.85f, 0.85f));
				}
			}
		}
		finally{
			g2.dispose();
		}
	}


	private static void arrow(final Graphics2D g2, final boolean right){
		final float tip = (right? 0.9f: 0.1f);
		final float tail = (right? 0.1f: 0.9f);
		final float head = (right? -0.28f: 0.28f);
		g2.draw(new Line2D.Float(tail, 0.5f, tip, 0.5f));
		g2.draw(polyline(tip + head, 0.5f - 0.26f, tip, 0.5f, tip + head, 0.5f + 0.26f));
	}

	private static void calendar(final Graphics2D g2){
		g2.setStroke(new BasicStroke(STROKE * 0.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2.draw(new RoundRectangle2D.Float(0.1f, 0.18f, 0.8f, 0.72f, 0.12f, 0.12f));
		g2.fill(new RoundRectangle2D.Float(0.1f, 0.18f, 0.8f, 0.2f, 0.12f, 0.12f));
		g2.draw(new Line2D.Float(0.3f, 0.06f, 0.3f, 0.26f));
		g2.draw(new Line2D.Float(0.7f, 0.06f, 0.7f, 0.26f));
		for(int row = 0; row < 2; row ++)
			for(int column = 0; column < 3; column ++)
				g2.fill(new Rectangle2DFloat(0.22f + column * 0.22f, 0.5f + row * 0.18f, 0.12f, 0.1f));
	}

	/** A triangle with an exclamation mark cut out of it. */
	private static Shape warning(){
		final Path2D path = new Path2D.Float(Path2D.WIND_EVEN_ODD);
		path.moveTo(0.5f, 0.06f);
		path.lineTo(0.97f, 0.9f);
		path.lineTo(0.03f, 0.9f);
		path.closePath();
		path.append(new Rectangle2DFloat(0.44f, 0.34f, 0.12f, 0.31f), false);
		path.append(new Rectangle2DFloat(0.44f, 0.71f, 0.12f, 0.12f), false);
		return path;
	}

	/** A disc with an "i" cut out of it. */
	private static Shape information(){
		final Path2D path = new Path2D.Float(Path2D.WIND_EVEN_ODD);
		path.append(new Ellipse2D.Float(0.04f, 0.04f, 0.92f, 0.92f), false);
		path.append(new Rectangle2DFloat(0.44f, 0.43f, 0.12f, 0.35f), false);
		path.append(new Rectangle2DFloat(0.44f, 0.22f, 0.12f, 0.13f), false);
		return path;
	}

	private static Path2D polygon(final float... points){
		final Path2D path = polyline(points);
		path.closePath();
		return path;
	}

	private static Path2D polyline(final float... points){
		final Path2D path = new Path2D.Float();
		path.moveTo(points[0], points[1]);
		for(int i = 2; i < points.length; i += 2)
			path.lineTo(points[i], points[i + 1]);
		return path;
	}



	/** A float rectangle with the constructor of the other shapes, to keep the drawing code short. */
	private static final class Rectangle2DFloat extends java.awt.geom.Rectangle2D.Float{

		private Rectangle2DFloat(final float x, final float y, final float width, final float height){
			super(x, y, width, height);
		}

	}

}
