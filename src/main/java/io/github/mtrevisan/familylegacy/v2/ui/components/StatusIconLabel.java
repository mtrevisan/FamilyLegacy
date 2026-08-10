package io.github.mtrevisan.familylegacy.v2.ui.components;

import javax.swing.JLabel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;


/**
 * A JLabel that draws a status icon based on the current status.
 * Can be overridden by subclasses for custom drawing.
 */
class StatusIconLabel extends JLabel{

	private String status = "open";


	public void setStatus(final String status){
		this.status = (status != null? status: "open");
	}


	@Override
	protected void paintComponent(final Graphics g){
		super.paintComponent(g);

		final Graphics2D g2 = (Graphics2D)g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		final int size = Math.min(getWidth(), getHeight());
		final int x = (getWidth() - size) >> 1;
		final int y = (getHeight() - size) >> 1;

		// Delegate to status-specific drawing
		drawStatusIcon(g2, x, y, size);

		g2.dispose();
	}

	/**
	 * Draws the status icon. Can be overridden by subclasses to customize appearance.
	 *
	 * @param g2   the Graphics2D context
	 * @param x    the x coordinate of the icon
	 * @param y    the y coordinate of the icon
	 * @param size the size of the icon (width = height)
	 */
	protected void drawStatusIcon(final Graphics2D g2, final int x, final int y, final int size){
		final int margin = size / 6;
		final int drawSize = size - 2 * margin;
		final int drawX = x + margin;
		final int drawY = y + margin;

		switch(status){
			case "open" -> {
				// Red circle
				g2.setColor(Color.RED);
				g2.fillOval(drawX, drawY, drawSize, drawSize);
				g2.setColor(Color.BLACK);
				g2.drawOval(drawX, drawY, drawSize, drawSize);
			}
			case "on_hold" -> {
				// Orange/yellow circle
				g2.setColor(new Color(255, 165, 0)); // Orange
				g2.fillOval(drawX, drawY, drawSize, drawSize);
				g2.setColor(Color.BLACK);
				g2.drawOval(drawX, drawY, drawSize, drawSize);
				// Small horizontal line inside (pause icon)
				g2.setColor(Color.WHITE);
				final int lineWidth = drawSize >> 2;
				final int lineHeight = drawSize >> 1;
				final int lineX = drawX + drawSize >> 1 - lineWidth >> 1;
				final int lineY = drawY + drawSize >> 1 - lineHeight >> 1;
				g2.fillRect(lineX, lineY, lineWidth, lineHeight);
			}
			case "resolved" -> {
				// Green square
				g2.setColor(new Color(0, 180, 0));
				g2.fillRect(drawX, drawY, drawSize, drawSize);
				g2.setColor(Color.BLACK);
				g2.drawRect(drawX, drawY, drawSize, drawSize);
				// White checkmark inside
				g2.setColor(Color.WHITE);
				g2.setStroke(new BasicStroke(2f));
				final int cx = drawX + drawSize >> 1;
				final int cy = drawY + drawSize >> 1;
				g2.drawLine(cx - drawSize >> 2, cy, cx - drawSize >> 3, cy + drawSize >> 2);
				g2.drawLine(cx - drawSize >> 3, cy + drawSize >> 2, cx + drawSize / 3, cy - drawSize >> 2);
			}
			case "disproven" -> {
				// White square with red X
				g2.setColor(Color.WHITE);
				g2.fillRect(drawX, drawY, drawSize, drawSize);
				g2.setColor(Color.BLACK);
				g2.drawRect(drawX, drawY, drawSize, drawSize);
				// Red X
				g2.setColor(Color.RED);
				g2.setStroke(new BasicStroke(2.5f));
				int marginCross = drawSize >> 2;
				g2.drawLine(drawX + marginCross, drawY + marginCross,
					drawX + drawSize - marginCross, drawY + drawSize - marginCross);
				g2.drawLine(drawX + drawSize - marginCross, drawY + marginCross,
					drawX + marginCross, drawY + drawSize - marginCross);
			}
			default -> {
				// Gray circle for unknown
				g2.setColor(Color.GRAY);
				g2.fillOval(drawX, drawY, drawSize, drawSize);
				g2.setColor(Color.BLACK);
				g2.drawOval(drawX, drawY, drawSize, drawSize);
			}
		}
	}

}
