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
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.NormalizedDate;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
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
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Heatmap of event density over the calendar.
 * <p>
 * The grid has one row per year and <b>thirteen columns</b>: the twelve
 * months of the Gregorian calendar, plus a final column for dates that
 * only carry year precision (or century/decade precision). This design
 * solves two problems at once:
 * <ul>
 *   <li>events with full or month-level precision are placed in the
 *       correct month, so the seasonal distribution is preserved;</li>
 *   <li>events with year-only precision are not forced into January,
 *       which would be misleading, but are grouped into the dedicated
 *       column so their count is still visible.</li>
 * </ul>
 * The final column header is {@code "?"}, meaning "month not specified".
 * <p>
 * Hovering a cell shows a tooltip with the events of that month; clicking
 * a cell opens a dialog listing them with type, date, place and
 * participants.
 */
public class EventHeatmapPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 7703492048361283712L;


	/** Number of grid columns: 12 months + 1 unknown-month column. */
	private static final int COLUMNS = 13;

	/** Side of a cell, in pixels. */
	private static final int CELL_SIZE = 18;
	/** Gap between two adjacent cells, in pixels. */
	private static final int CELL_GAP = 1;

	/** Width of the year label column, in pixels. */
	private static final int YEAR_LABEL_WIDTH = 44;
	/** Height of the month header row, in pixels. */
	private static final int MONTH_HEADER_HEIGHT = 16;

	/** Outer margin of the grid, in pixels. */
	private static final int MARGIN = 10;

	private static final Font YEAR_FONT = new Font("Tahoma", Font.PLAIN, 10);
	private static final Font MONTH_FONT = new Font("Tahoma", Font.PLAIN, 9);
	private static final Font REPORT_FONT = new Font("Monospaced", Font.PLAIN, 12);

	private static final Color BACKGROUND = new Color(250, 249, 245);
	private static final Color GRID = new Color(225, 223, 218);
	private static final Color LABEL = new Color(50, 40, 30);
	private static final Color EMPTY = new Color(246, 246, 242);
	private static final Color HEAT_LOW = new Color(190, 215, 190);
	private static final Color HEAT_HIGH = new Color(200, 55, 55);
	private static final Color BORDER = new Color(200, 198, 195);
	private static final Color HEADER_BG = new Color(240, 238, 232);

	private static final String[] MONTH_NAMES = {
		"Jan", "Feb", "Mar", "Apr", "May", "Jun",
		"Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "?"
	};


	/** Aggregation key: a year and a month, where month may be {@code 0} for "unspecified". */
	private record YearMonth(int year, int month){}


	private final EventIndex index;

	private final int minYear;
	private final int maxYear;
	private final Map<YearMonth, List<EventIndex.EventDatum>> eventsByYearMonth;
	private final int maxCellCount;

	private final HeatmapCanvas canvas;


	/**
	 * Constructor.
	 *
	 * @param model the FLEF model (must not be {@code null})
	 */
	public EventHeatmapPanel(final FLEFModel model){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");

		this.index = EventIndex.build(model);

		final Map<YearMonth, List<EventIndex.EventDatum>> byYearMonth = new TreeMap<>(
			java.util.Comparator.comparingInt(YearMonth::year)
				.thenComparingInt(YearMonth::month));
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for(final EventIndex.EventDatum e : index.allEvents()){
			if(!e.hasDate())
				continue;
			final int[] ymd = jdnToGregorian(e.date().jdn());
			final int year = ymd[0];
			// Month is 1..12 for DAY and MONTH precision, 0 for YEAR,
			// DECADE, CENTURY (the normalized date points to January 1st
			// but the precision declares that the month is not known).
			final int month = (e.date().precision().isYearOrFiner() && e.date().precision() !=
				io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.DatePrecision.YEAR
				? ymd[1]: 0);
			final YearMonth key = new YearMonth(year, month);
			byYearMonth.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
			min = Math.min(min, year);
			max = Math.max(max, year);
		}

		this.eventsByYearMonth = byYearMonth;
		this.minYear = (min <= max? min: 0);
		this.maxYear = (min <= max? max: 0);

		int mxc = 1;
		for(final List<EventIndex.EventDatum> list : byYearMonth.values())
			mxc = Math.max(mxc, list.size());
		this.maxCellCount = mxc;

		this.canvas = new HeatmapCanvas();
		final JScrollPane scroll = new JScrollPane(canvas,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);

		setLayout(new BorderLayout());
		add(scroll, BorderLayout.CENTER);
		setPreferredSize(new Dimension(480, 600));

		ToolTipManager.sharedInstance()
			.registerComponent(canvas);
		installListeners();
	}


	/* ======================================================================
	 *                          Interaction
	 * ====================================================================== */

	private void installListeners(){
		final MouseAdapter adapter = new MouseAdapter(){
			@Override public void mouseClicked(final MouseEvent e){
				if(!SwingUtilities.isLeftMouseButton(e))
					return;
				final YearMonth key = cellAt(e.getX(), e.getY());
				if(key != null)
					openReportDialog(key);
			}
		};
		canvas.addMouseListener(adapter);
	}

	/**
	 * Returns the (year, month) cell that contains the given point, or
	 * {@code null} if no cell is hit.
	 */
	private YearMonth cellAt(final int x, final int y){
		final int gridX = x - MARGIN - YEAR_LABEL_WIDTH;
		final int gridY = y - MARGIN - MONTH_HEADER_HEIGHT;
		if(gridX < 0 || gridY < 0)
			return null;

		final int colWidth = CELL_SIZE + CELL_GAP;
		final int rowHeight = CELL_SIZE + CELL_GAP;
		final int col = gridX / colWidth;
		final int row = gridY / rowHeight;
		if(col < 0 || col >= COLUMNS)
			return null;
		if(gridX % colWidth >= CELL_SIZE || gridY % rowHeight >= CELL_SIZE)
			return null;

		final int year = minYear + row;
		if(year > maxYear)
			return null;

		// Column 12 = "unspecified month"; columns 0..11 = Jan..Dec.
		final int month = (col == 12? 0: col + 1);
		return new YearMonth(year, month);
	}


	/* ======================================================================
	 *                          Report dialog
	 * ====================================================================== */

	private void openReportDialog(final YearMonth key){
		final List<EventIndex.EventDatum> events = eventsByYearMonth.get(key);
		final Window owner = SwingUtilities.getWindowAncestor(this);
		final JDialog dialog = new JDialog(owner, "Events of " + formatKey(key),
			JDialog.ModalityType.APPLICATION_MODAL);

		final JTextArea area = new JTextArea(buildReport(key, events));
		area.setEditable(false);
		area.setFont(REPORT_FONT);
		area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		final JScrollPane scroll = new JScrollPane(area);
		scroll.setBorder(BorderFactory.createTitledBorder(
			events != null && !events.isEmpty()
				? events.size() + (events.size() == 1? " event": " events")
				: "No events"));

		final JButton closeBtn = new JButton("Close");
		closeBtn.addActionListener(e -> dialog.dispose());

		final JPanel footer = new JPanel();
		footer.add(closeBtn);

		dialog.setLayout(new BorderLayout());
		dialog.add(scroll, BorderLayout.CENTER);
		dialog.add(footer, BorderLayout.SOUTH);
		dialog.setSize(560, 420);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}

	private static String buildReport(final YearMonth key, final List<EventIndex.EventDatum> events){
		if(events == null || events.isEmpty())
			return "No events recorded in " + formatKey(key) + ".";

		final StringBuilder sb = new StringBuilder();
		sb.append("Events of ").append(formatKey(key))
			.append(" (").append(events.size()).append(")\n\n");
		for(int i = 0; i < events.size(); i ++){
			final EventIndex.EventDatum e = events.get(i);
			sb.append(i + 1).append(". ").append(e.type()).append('\n');
			sb.append("   date:  ").append(formatDate(e.date())).append('\n');
			if(e.hasPlace())
				sb.append("   place: ").append(e.placeName()).append('\n');
			if(!e.participants().isEmpty()){
				sb.append("   participants:\n");
				for(final EventIndex.Participant p : e.participants())
					sb.append("     - ").append(p.name())
						.append(p.isIndividual()? "": " (group)")
						.append('\n');
			}
			if(i < events.size() - 1)
				sb.append('\n');
		}
		return sb.toString();
	}

	private static String formatKey(final YearMonth key){
		if(key.month() == 0)
			return key.year() + " (month unspecified)";
		return MONTH_NAMES[key.month() - 1] + " " + key.year();
	}


	/* ======================================================================
	 *                          Canvas
	 * ====================================================================== */

	private final class HeatmapCanvas extends JPanel{

		@java.io.Serial
		private static final long serialVersionUID = 3309182347194811032L;


		HeatmapCanvas(){
			setBackground(BACKGROUND);
		}

		@Override
		public Dimension getPreferredSize(){
			final int totalYears = Math.max(1, maxYear - minYear + 1);
			final int gridWidth = COLUMNS * (CELL_SIZE + CELL_GAP);
			final int gridHeight = totalYears * (CELL_SIZE + CELL_GAP);
			final int w = 2 * MARGIN + YEAR_LABEL_WIDTH + gridWidth;
			final int h = 2 * MARGIN + MONTH_HEADER_HEIGHT + gridHeight;
			return new Dimension(w, h);
		}

		@Override
		public String getToolTipText(final MouseEvent event){
			final YearMonth key = cellAt(event.getX(), event.getY());
			if(key == null)
				return null;
			final List<EventIndex.EventDatum> events = eventsByYearMonth.get(key);
			if(events == null || events.isEmpty())
				return "<html><b>" + formatKey(key) + "</b><br>No events</html>";

			final StringBuilder sb = new StringBuilder("<html><b>").append(escape(formatKey(key)))
				.append("</b> — ").append(events.size())
				.append(events.size() == 1? " event": " events")
				.append("<br><br>");
			final int limit = Math.min(8, events.size());
			for(int i = 0; i < limit; i ++){
				final EventIndex.EventDatum e = events.get(i);
				sb.append("• ").append(escape(e.type()));
				if(e.hasDate())
					sb.append(" — ").append(escape(formatDate(e.date())));
				if(e.hasPlace())
					sb.append(" @ ").append(escape(e.placeName()));
				sb.append("<br>");
			}
			if(events.size() > limit)
				sb.append("<i>… and ").append(events.size() - limit).append(" more</i><br>");
			sb.append("<br><i>Click to see the full list</i></html>");
			return sb.toString();
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

			if(maxYear < minYear)
				return;

			// Draw the header band behind the month labels.
			final int gridLeft = MARGIN + YEAR_LABEL_WIDTH;
			g2.setColor(HEADER_BG);
			g2.fillRect(MARGIN, MARGIN, width - 2 * MARGIN, MONTH_HEADER_HEIGHT);

			// Month header labels.
			g2.setFont(MONTH_FONT);
			g2.setColor(LABEL);
			final FontMetrics mfm = g2.getFontMetrics();
			for(int m = 0; m < COLUMNS; m ++){
				final String label = MONTH_NAMES[m];
				final int cx = gridLeft + m * (CELL_SIZE + CELL_GAP) + CELL_SIZE / 2;
				final int tw = mfm.stringWidth(label);
				g2.drawString(label, cx - tw / 2, MARGIN + MONTH_HEADER_HEIGHT - 4);
			}

			// Grid.
			final int rowHeight = CELL_SIZE + CELL_GAP;
			final int colWidth = CELL_SIZE + CELL_GAP;
			final int gridTop = MARGIN + MONTH_HEADER_HEIGHT;

			for(int year = minYear; year <= maxYear; year ++){
				final int y = gridTop + (year - minYear) * rowHeight;

				// Year label.
				g2.setFont(YEAR_FONT);
				g2.setColor(LABEL);
				final FontMetrics yfm = g2.getFontMetrics();
				final String yearText = Integer.toString(year);
				final int yw = yfm.stringWidth(yearText);
				g2.drawString(yearText, MARGIN + YEAR_LABEL_WIDTH - yw - 6, y + CELL_SIZE / 2 + 4);

				// Cells.
				for(int m = 0; m < COLUMNS; m ++){
					final int month = (m == 12? 0: m + 1);
					final YearMonth key = new YearMonth(year, month);
					final List<EventIndex.EventDatum> events = eventsByYearMonth.get(key);
					final int count = (events != null? events.size(): 0);
					final Color fill = (count == 0? EMPTY: heatColor(count, maxCellCount));

					final int x = gridLeft + m * colWidth;
					g2.setColor(fill);
					g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
					g2.setColor(BORDER);
					g2.drawRect(x, y, CELL_SIZE, CELL_SIZE);
				}
			}
		}
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private static Color heatColor(final int count, final int maxCount){
		final float t = Math.min(1f, count / (float)Math.max(1, maxCount));
		final int r = (int)(HEAT_LOW.getRed() + t * (HEAT_HIGH.getRed() - HEAT_LOW.getRed()));
		final int gr = (int)(HEAT_LOW.getGreen() + t * (HEAT_HIGH.getGreen() - HEAT_LOW.getGreen()));
		final int b = (int)(HEAT_LOW.getBlue() + t * (HEAT_HIGH.getBlue() - HEAT_LOW.getBlue()));
		return new Color(r, gr, b);
	}

	private static String formatDate(final NormalizedDate date){
		if(date == null)
			return "?";
		final int[] ymd = jdnToGregorian(date.jdn());
		return switch(date.precision()){
			case DAY -> ymd[2] + " " + MONTH_NAMES[ymd[1] - 1] + " " + ymd[0];
			case MONTH -> MONTH_NAMES[ymd[1] - 1] + " " + ymd[0];
			case YEAR -> Integer.toString(ymd[0]);
			case DECADE -> (ymd[0] / 10 * 10) + "s";
			case CENTURY -> (ymd[0] / 100 + 1) + "th century";
		};
	}

	private static String escape(final String s){
		if(s == null)
			return "";
		return s.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}

	private static int[] jdnToGregorian(final long jdn){
		final long a = jdn + 32044L;
		final long b = (4L * a + 3L) / 146097L;
		final long c = a - (146097L * b) / 4L;
		final long d = (4L * c + 3L) / 1461L;
		final long e = c - (1461L * d) / 4L;
		final long m = (5L * e + 2L) / 153L;
		final int day = (int)(e - (153L * m + 2L) / 5L + 1L);
		final int month = (int)(m + 3L - 12L * (m / 10L));
		final int year = (int)(100L * b + d - 4800L + m / 10L);
		return new int[]{year, month, day};
	}


	/* ======================================================================
	 *                          Bootstrap
	 * ====================================================================== */

	public static void main(final String[] args) throws IOException{
		try{ UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch(final Exception ignored){}

		final String content;
		try(final InputStream is = EventHeatmapPanel.class.getResourceAsStream("/tests/TGMZ.flef")){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}
		final FLEFModel model = new FLEFParser().parse(content);
		SwingUtilities.invokeLater(() -> {
			final EventHeatmapPanel panel = new EventHeatmapPanel(model);
			final JFrame frame = new JFrame("Event Heatmap");
			frame.add(panel);
			frame.setSize(520, 640);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
