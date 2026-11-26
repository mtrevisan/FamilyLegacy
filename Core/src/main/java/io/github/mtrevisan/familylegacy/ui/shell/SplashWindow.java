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
package io.github.mtrevisan.familylegacy.ui.shell;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.helpers.AppLogo;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;


/**
 * The window shown while the application starts: the logo over a deep blue gradient with a soft glow, the name, a tagline and a
 * thin progress line (cyan to gold, like the logo) that glides toward the real progress instead of jumping.
 * <p>
 * Borderless, with rounded corners over a transparent window.
 */
public final class SplashWindow extends JWindow{

	private static final int WIDTH = 600;
	private static final int HEIGHT = 320;
	private static final int ARC = 32;
	private static final int LOGO_SIZE = 200;
	private static final int LOGO_X = 34;
	private static final int TEXT_X = LOGO_X + LOGO_SIZE + 30;
	private static final int TEXT_RIGHT_MARGIN = 36;
	private static final int BAR_HEIGHT = 4;

	private static final Color BACKGROUND_TOP = new Color(0x13, 0x2A, 0x52);
	private static final Color BACKGROUND_BOTTOM = new Color(0x07, 0x11, 0x26);
	private static final Color BORDER = new Color(0xE8, 0xB9, 0x4A, 70);
	private static final Color GLOW = new Color(0x3C, 0xC8, 0xE6, 90);
	private static final Color CYAN = new Color(0x3C, 0xC8, 0xE6);
	private static final Color GOLD = new Color(0xE8, 0xB9, 0x4A);
	private static final Color TITLE = new Color(0xF4, 0xF8, 0xFF);
	private static final Color TAGLINE = new Color(0x9F, 0xB6, 0xD6);
	private static final Color TRACK = new Color(255, 255, 255, 35);
	private static final Color MESSAGE = new Color(0x7F, 0x97, 0xB8);

	/** The fraction of the remaining distance the bar covers at each tick (the glide). */
	private static final double EASING = 0.14;
	private static final int TICK_MS = 16;


	private final Icon logo = AppLogo.icon(LOGO_SIZE);
	private final Timer glide;
	private int value;
	private int maximum = 1;
	private double displayed;
	private String message = I18N.t("splash.loading");


	public SplashWindow(){
		setBackground(new Color(0, 0, 0, 0));

		final JPanel content = new JPanel(null){
			@Override
			protected void paintComponent(final Graphics g){
				final Graphics2D g2 = (Graphics2D)g.create();
				try{
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
					paintSplash(g2);
				}
				finally{
					g2.dispose();
				}
			}
		};
		content.setOpaque(false);
		content.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setContentPane(content);
		pack();
		setLocationRelativeTo(null);

		glide = new Timer(TICK_MS, event -> {
			final double target = target();
			final double gap = target - displayed;
			displayed = (Math.abs(gap) < 0.0005? target: displayed + gap * EASING);
			repaint();
		});
		glide.start();
	}


	/**
	 * Sets the progress.
	 *
	 * @param value	How many steps are done.
	 * @param maximum	The total number of steps.
	 */
	public void setProgress(final int value, final int maximum){
		this.value = value;
		this.maximum = Math.max(maximum, 1);
	}

	/** Sets the line of text under the progress line. */
	public void setMessage(final String message){
		this.message = message;
		repaint();
	}

	/** The displayed progress, from 0 to 1 (it follows the real one with a delay: the glide). */
	double displayedProgress(){
		return displayed;
	}

	/** Moves the displayed progress to the real one at once, with no glide (the one shown by a still image). */
	void snapToProgress(){
		displayed = target();
	}

	private double target(){
		return Math.clamp((double)value / maximum, 0., 1.);
	}

	@Override
	public void dispose(){
		glide.stop();
		super.dispose();
	}


	private void paintSplash(final Graphics2D g){
		final RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, WIDTH - 1, HEIGHT - 1, ARC, ARC);

		g.setPaint(new LinearGradientPaint(new Point2D.Float(0, 0), new Point2D.Float(WIDTH * 0.35f, HEIGHT),
			new float[]{0.f, 1.f}, new Color[]{BACKGROUND_TOP, BACKGROUND_BOTTOM}));
		g.fill(shape);

		//the soft glow behind the logo, and a faint one in the opposite corner, in gold
		final float centerX = LOGO_X + LOGO_SIZE / 2.f;
		final float centerY = HEIGHT / 2.f - 14;
		g.setClip(shape);
		g.setPaint(new RadialGradientPaint(new Point2D.Float(centerX, centerY), 190.f, new float[]{0.f, 1.f},
			new Color[]{GLOW, new Color(GLOW.getRed(), GLOW.getGreen(), GLOW.getBlue(), 0)}));
		g.fillRect(0, 0, WIDTH, HEIGHT);
		g.setPaint(new RadialGradientPaint(new Point2D.Float(WIDTH, HEIGHT), 220.f, new float[]{0.f, 1.f},
			new Color[]{new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 40),
			new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 0)}));
		g.fillRect(0, 0, WIDTH, HEIGHT);
		g.setClip(null);

		g.setColor(BORDER);
		g.setStroke(new BasicStroke(1.f));
		g.draw(shape);

		if(logo != null)
			logo.paintIcon(null, g, LOGO_X, (HEIGHT - LOGO_SIZE) / 2 - 14);

		final int textWidth = WIDTH - TEXT_X - TEXT_RIGHT_MARGIN;
		final Font base = getFont();

		//the name, in two weights: the same look as a wordmark
		final Font light = base.deriveFont(Font.PLAIN, 34.f);
		final Font bold = base.deriveFont(Font.BOLD, 34.f);
		final FontMetrics lightMetrics = g.getFontMetrics(light);
		final FontMetrics boldMetrics = g.getFontMetrics(bold);
		final int titleY = 124;
		g.setFont(light);
		g.setColor(TITLE);
		g.drawString("Family", TEXT_X, titleY);
		g.setFont(bold);
		g.setColor(GOLD);
		g.drawString("Legacy", TEXT_X + lightMetrics.stringWidth("Family"), titleY);

		//the short rule under the name
		final int ruleWidth = lightMetrics.stringWidth("Family") + boldMetrics.stringWidth("Legacy");
		g.setPaint(new LinearGradientPaint(new Point2D.Float(TEXT_X, 0), new Point2D.Float(TEXT_X + ruleWidth, 0),
			new float[]{0.f, 1.f}, new Color[]{CYAN, GOLD}));
		g.fillRoundRect(TEXT_X, titleY + 14, 56, 3, 3, 3);

		//the tagline, wrapped to the room it has
		g.setFont(base.deriveFont(Font.PLAIN, 14.f));
		g.setColor(TAGLINE);
		final FontMetrics taglineMetrics = g.getFontMetrics();
		int y = titleY + 48;
		for(final String line : wrap(I18N.t("splash.tagline"), taglineMetrics, textWidth)){
			g.drawString(line, TEXT_X, y);
			y += taglineMetrics.getHeight() + 2;
		}

		//the progress line, across the bottom
		final int barX = LOGO_X;
		final int barWidth = WIDTH - 2 * LOGO_X;
		final int barY = HEIGHT - 50;
		g.setColor(TRACK);
		g.fillRoundRect(barX, barY, barWidth, BAR_HEIGHT, BAR_HEIGHT, BAR_HEIGHT);
		final int fill = (int)Math.round(barWidth * displayed);
		if(fill > 0){
			g.setPaint(new LinearGradientPaint(new Point2D.Float(barX, 0), new Point2D.Float(barX + barWidth, 0),
				new float[]{0.f, 1.f}, new Color[]{CYAN, GOLD}));
			g.fillRoundRect(barX, barY, Math.max(fill, BAR_HEIGHT), BAR_HEIGHT, BAR_HEIGHT, BAR_HEIGHT);
			//the bright head of the bar
			g.setPaint(new RadialGradientPaint(new Point2D.Float(barX + fill, barY + BAR_HEIGHT / 2.f), 9.f, new float[]{0.f, 1.f},
				new Color[]{new Color(255, 255, 255, 170), new Color(255, 255, 255, 0)}));
			g.fillOval(barX + fill - 9, barY + BAR_HEIGHT / 2 - 9, 18, 18);
		}

		g.setFont(base.deriveFont(Font.PLAIN, 12.f));
		g.setColor(MESSAGE);
		g.drawString(message, barX, barY + 24);
	}

	/** The lines of the text, none wider than the room, broken between words. */
	private static List<String> wrap(final String text, final FontMetrics metrics, final int room){
		final List<String> lines = new ArrayList<>(3);
		final StringBuilder line = new StringBuilder();
		for(final String word : org.apache.commons.lang3.StringUtils.split(text, ' ')){
			if(!line.isEmpty() && metrics.stringWidth(line + " " + word) > room){
				lines.add(line.toString());
				line.setLength(0);
			}
			if(!line.isEmpty())
				line.append(' ');
			line.append(word);
		}
		if(!line.isEmpty())
			lines.add(line.toString());
		return lines;
	}

}
