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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.events;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.NormalizedDate;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalAxis;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Life map of a single individual.
 * <p>
 * Draws:
 * <ul>
 *   <li>a horizontal lifespan bar from the earliest to the latest dated
 *       event of the individual;</li>
 *   <li>one marker per event, colored by event type, placed at its date;</li>
 *   <li>a small axis at the top with the year labels.</li>
 * </ul>
 * If the individual has no dated events, the bar and the markers are
 * omitted and only the axis is shown.
 */
@Deprecated
public class LifeMapPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 3391083356201378012L;


	private static final int PADDING = 20;
	private static final int AXIS_HEIGHT = 28;
	private static final int TITLE_HEIGHT = 22;
	private static final int BAR_Y_OFFSET = 70;
	private static final int BAR_HEIGHT = 8;
	private static final int MARKER_RADIUS = 6;

	private static final Font AXIS_FONT = new Font("Tahoma", Font.PLAIN, 11);
	private static final Font TITLE_FONT = new Font("Tahoma", Font.BOLD, 13);

	private static final Color BACKGROUND = new Color(250, 249, 245);
	private static final Color AXIS_BACKGROUND = new Color(248, 246, 240);
	private static final Color AXIS_LINE = new Color(140, 130, 110);
	private static final Color LABEL = new Color(50, 40, 30);
	private static final Color BAR_COLOR = new Color(180, 180, 180, 180);
	private static final Color BAR_BORDER = new Color(120, 120, 120);
	private static final Color SELECTED = new Color(220, 100, 60);


	private final EventIndex index;
	private final String individualId;
	private final String individualName;
	private final TemporalAxis axis;
	private final Canvas canvas;

	private EventIndex.EventDatum hovered;
	private EventIndex.EventDatum selected;


	public LifeMapPanel(final FLEFModel model, final String individualId){
		this.index = EventIndex.build(model);
		this.individualId = individualId;

		final FLEFRecord record = model.getRecordById(individualId);
		this.individualName = (record != null
			? IndividualHandler.getInstance().getDisplayText(record, model)
			: individualId);

		// Compute the axis domain from the dated events of this individual.
		NormalizedDate min = null;
		NormalizedDate max = null;
		for(final EventIndex.EventDatum e : index.eventsOf(individualId)){
			if(!e.hasDate())
				continue;
			if(min == null || e.date().compareTo(min) < 0)
				min = e.date();
			if(max == null || e.date().compareTo(max) > 0)
				max = e.date();
		}
		this.axis = new TemporalAxis(min, max);

		this.canvas = new Canvas();
		setLayout(new BorderLayout());
		add(canvas, BorderLayout.CENTER);
		setPreferredSize(new Dimension(900, 240));

		ToolTipManager.sharedInstance().registerComponent(canvas);
		installListeners();
	}


	private void installListeners(){
		final MouseAdapter adapter = new MouseAdapter(){
			@Override public void mouseMoved(final MouseEvent e){
				hovered = eventAt(e.getX(), e.getY());
				canvas.repaint();
			}

			@Override public void mouseExited(final MouseEvent e){
				hovered = null;
				canvas.repaint();
			}

			@Override public void mouseClicked(final MouseEvent e){
				if(!SwingUtilities.isLeftMouseButton(e))
					return;
				final EventIndex.EventDatum hit = eventAt(e.getX(), e.getY());
				selected = (hit != null && selected != null && hit.id().equals(selected.id())? null: hit);
				canvas.repaint();
			}
		};
		canvas.addMouseListener(adapter);
		canvas.addMouseMotionListener(adapter);
	}

	private EventIndex.EventDatum eventAt(final int x, final int y){
		final List<EventIndex.EventDatum> events = index.eventsOf(individualId);
		EventIndex.EventDatum nearest = null;
		int best = MARKER_RADIUS + 6;
		final int barY = TITLE_HEIGHT + AXIS_HEIGHT + BAR_Y_OFFSET + BAR_HEIGHT / 2;
		for(final EventIndex.EventDatum e : events){
			if(!e.hasDate())
				continue;
			final int ex = PADDING + axis.jdnToX(e.date().jdn());
			if(Math.abs(ex - x) < best && Math.abs(y - barY) < 30){
				best = Math.abs(ex - x);
				nearest = e;
			}
		}
		return nearest;
	}


	private final class Canvas extends JPanel{

		@Serial
		private static final long serialVersionUID = 8431902748490191183L;


		Canvas(){
			setBackground(BACKGROUND);
		}

		@Override
		protected void paintComponent(final Graphics g){
			super.paintComponent(g);
			if(!(g instanceof Graphics2D g2))
				return;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			final int width = getWidth();
			final int height = getHeight();
			g2.setColor(BACKGROUND);
			g2.fillRect(0, 0, width, height);

			final Rectangle axisBounds = new Rectangle(PADDING, TITLE_HEIGHT, width - 2 * PADDING, AXIS_HEIGHT);
			final Rectangle contentBounds = new Rectangle(PADDING, TITLE_HEIGHT + AXIS_HEIGHT,
				width - 2 * PADDING, height - TITLE_HEIGHT - AXIS_HEIGHT);

			// Tell the axis how wide the temporal area is. The visible
			// window has already been fitted to the domain in the
			// constructor and is not touched here.
			axis.setViewportWidth(contentBounds.width);

			// Title.
			g2.setFont(TITLE_FONT);
			g2.setColor(LABEL);
			g2.drawString(individualName, PADDING, TITLE_HEIGHT + 14);

			// Axis.
			paintAxis(g2, axisBounds, contentBounds);

			// Bar and markers.
			paintBar(g2, contentBounds);
			paintMarkers(g2, contentBounds);
		}

		private void paintAxis(final Graphics2D g2, final Rectangle axisBounds, final Rectangle contentBounds){
			g2.setColor(AXIS_BACKGROUND);
			g2.fillRect(axisBounds.x, axisBounds.y, axisBounds.width, axisBounds.height);
			g2.setColor(AXIS_LINE);
			g2.drawLine(axisBounds.x, axisBounds.y + axisBounds.height - 1,
				axisBounds.x + axisBounds.width - 1, axisBounds.y + axisBounds.height - 1);

			g2.setFont(AXIS_FONT);
			final FontMetrics fm = g2.getFontMetrics();
			for(final TemporalAxis.Tick tick : axis.computeTicks()){
				final int x = contentBounds.x + tick.x();
				g2.setColor(AXIS_LINE);
				g2.drawLine(x, axisBounds.y + axisBounds.height - 6, x, axisBounds.y + axisBounds.height);
				g2.setColor(LABEL);
				final int tw = fm.stringWidth(tick.label());
				g2.drawString(tick.label(), x - tw / 2, axisBounds.y + fm.getAscent() + 2);
			}
		}

		private void paintBar(final Graphics2D g2, final Rectangle contentBounds){
			long minJdn = Long.MAX_VALUE;
			long maxJdn = Long.MIN_VALUE;
			for(final EventIndex.EventDatum e : index.eventsOf(individualId)){
				if(!e.hasDate())
					continue;
				minJdn = Math.min(minJdn, e.date().jdn());
				maxJdn = Math.max(maxJdn, e.date().jdn());
			}
			if(minJdn > maxJdn)
				return;

			final int x1 = contentBounds.x + axis.jdnToX(minJdn);
			final int x2 = contentBounds.x + axis.jdnToX(maxJdn);
			final int y = contentBounds.y + BAR_Y_OFFSET;

			g2.setColor(BAR_COLOR);
			g2.fillRoundRect(x1, y, Math.max(2, x2 - x1), BAR_HEIGHT, BAR_HEIGHT, BAR_HEIGHT);
			g2.setColor(BAR_BORDER);
			g2.drawRoundRect(x1, y, Math.max(2, x2 - x1), BAR_HEIGHT, BAR_HEIGHT, BAR_HEIGHT);
		}

		private void paintMarkers(final Graphics2D g2, final Rectangle contentBounds){
			final int y = contentBounds.y + BAR_Y_OFFSET + BAR_HEIGHT / 2;
			for(final EventIndex.EventDatum e : index.eventsOf(individualId)){
				if(!e.hasDate())
					continue;
				final int x = contentBounds.x + axis.jdnToX(e.date().jdn());
				final boolean isSel = (selected != null && selected.id().equals(e.id()));
				final boolean isHov = (hovered != null && hovered.id().equals(e.id()));
				final int r = MARKER_RADIUS + (isSel || isHov? 2: 0);

				g2.setColor(isSel? SELECTED: typeColor(e.type()));
				g2.fillOval(x - r, y - r, 2 * r, 2 * r);
				g2.setColor(Color.WHITE);
				g2.drawOval(x - r, y - r, 2 * r, 2 * r);
			}
		}
	}


	private static Color typeColor(final String type){
		if(type == null)
			return new Color(120, 120, 120);
		final int h = Math.abs(type.hashCode());
		return Color.getHSBColor((h % 360) / 360f, 0.55f, 0.85f);
	}


	public static void main(final String[] args) throws IOException{
		try{ UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch(final Exception ignored){}

		final String content;
		try(final InputStream is = LifeMapPanel.class.getResourceAsStream("/tests/TGMZ.flef")){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}
		final FLEFModel model = new FLEFParser().parse(content);
		SwingUtilities.invokeLater(() -> {
			final LifeMapPanel panel = new LifeMapPanel(model, "I1");
			final JFrame frame = new JFrame("Life Map");
			frame.add(panel);
			frame.setSize(1000, 280);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
