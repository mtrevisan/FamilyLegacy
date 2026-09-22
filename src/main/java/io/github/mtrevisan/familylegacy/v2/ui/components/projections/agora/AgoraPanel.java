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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.agora;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.chronomap.ChronomapIndex;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.chronomap.ChronomapIndex.GeoAnchor;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.chronomap.ChronomapIndex.GeoCoordinate;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.chronomap.ChronomapOverlayPainter;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.chronomap.ChronomapTimeline;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.chronomap.PlaceCoordinateResolver;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * "Agorà" view.
 * <p>
 * Shows who was alive on a specific date and where they were, using the
 * same anchor model as the chronomap: events are zero-duration anchors,
 * attributes with {@code valid_from} / {@code valid_to} are anchors with
 * a real duration. The location of each person is the interpolated
 * position between the anchors around the chosen date.
 * <p>
 * A person is considered alive when:
 * <ul>
 *   <li>their earliest anchor (or their birth, when present) is not
 *       after the chosen date;</li>
 *   <li>no death anchor is on or before the chosen date;</li>
 *   <li>the chosen date is not more than 110 years after their birth
 *       (or after their first anchor, when no birth is recorded).</li>
 * </ul>
 * The 110-year cap is a genealogical heuristic: without a death record,
 * we assume the person is no longer alive once they would be older than
 * a plausible human lifespan.
 * <p>
 * The panel does not touch the model. It reuses the {@link ChronomapIndex}
 * shared by the chronomap and its {@link PlaceCoordinateResolver}.
 */
public final class AgoraPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 6182938471029384756L;


	/** Hard cap on human lifespan, used when no death record exists. */
	private static final int MAX_PLAUSIBLE_AGE_YEARS = 110;
	/** Days per Gregorian year, used for the age computation. */
	private static final double DAYS_PER_YEAR = 365.2425;
	/** Debounce for the table rebuild, in milliseconds. */
	private static final int REFRESH_DEBOUNCE_MS = 80;

	private static final Color HEADER_BACKGROUND = new Color(240, 236, 228);
	private static final Color SUBTITLE_COLOR = new Color(120, 115, 100);
	private static final Font DATE_FONT = new Font("Tahoma", Font.BOLD, 18);
	private static final Font COUNT_FONT = new Font("Tahoma", Font.PLAIN, 12);

	private static final String[] MONTH_NAMES = {
		"Jan", "Feb", "Mar", "Apr", "May", "Jun",
		"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
	};


	/**
	 * A single row of the Agora table: one individual alive at the
	 * chosen date, with the interpolated location and the reason that
	 * justifies that location.
	 *
	 * @param id        the individual id
	 * @param name      the display name
	 * @param place     the place name, or a movement description
	 * @param latitude  the interpolated latitude
	 * @param longitude the interpolated longitude
	 * @param reason    a short description of the anchor that drives the
	 *                  location (e.g. {@code "residence"}, {@code "birth"},
	 *                  {@code "in transit"})
	 * @param age       the age in years at the chosen date, or {@code null}
	 */
	public record AgoraRow(String id, String name, String place, double latitude, double longitude,
								  String reason, Integer age){
	}


	private final FLEFModel model;
	private final ChronomapIndex index;

	private final ChronomapTimeline timeline = new ChronomapTimeline();
	private final JLabel dateLabel = new JLabel();
	private final JLabel countLabel = new JLabel();
	private final JTextField searchField = new JTextField(20);
	private final AgoraTableModel tableModel = new AgoraTableModel();
	private final JTable table = new JTable(tableModel);
	private final TableRowSorter<AgoraTableModel> sorter = new TableRowSorter<>(tableModel);

	private final Timer refreshTimer;

	private Collection<String> currentIds = List.of();
	private double currentTime;

	private Consumer<String> selectionCallback;


	/**
	 * Constructor.
	 *
	 * @param model the FLEF model
	 * @param index the shared chronomap index, built on the same model
	 */
	public AgoraPanel(final FLEFModel model, final ChronomapIndex index){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");
		if(index == null)
			throw new IllegalArgumentException("ChronomapIndex must not be null");

		this.model = model;
		this.index = index;

		this.refreshTimer = new Timer(REFRESH_DEBOUNCE_MS, e -> refresh());
		this.refreshTimer.setRepeats(false);

		buildUI();
		configureTimeline();

		final long[] range = index.computeGlobalDateRange();
		if(range != null && range[0] < range[1]){
			timeline.setDomain(range[0], range[1]);
			// Default position: the midpoint of the range, which is more
			// likely to show a populated Agora than either extreme.
			final long mid = range[0] + (range[1] - range[0]) / 2;
			timeline.setCurrentTime(mid);
			currentTime = mid;
			dateLabel.setText(formatDate(mid));
		}
		else{
			currentTime = 0.;
			dateLabel.setText("—");
		}
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Sets the population to consider. Only these ids are scanned when
	 * the date changes; the rest of the model is ignored.
	 *
	 * @param ids the individual ids; may be {@code null}
	 */
	public void setIndividuals(final Collection<String> ids){
		this.currentIds = (ids != null? List.copyOf(ids): List.of());
		refresh();
	}

	/**
	 * Moves the Agora to the given date.
	 *
	 * @param jdn the date, as a Julian Day Number
	 */
	public void setDate(final double jdn){
		timeline.setCurrentTime(jdn);
		currentTime = timeline.getCurrentTime();
		dateLabel.setText(formatDate((long)currentTime));
		refreshTimer.restart();
	}

	/**
	 * Registers a callback invoked when the user double-clicks a row.
	 * The callback receives the individual id, so the enclosing frame
	 * can open the dossier or re-root a projection.
	 *
	 * @param callback the callback; may be {@code null}
	 */
	public void withSelectionCallback(final Consumer<String> callback){
		this.selectionCallback = callback;
	}


	/* ======================================================================
	 *                          UI
	 * ====================================================================== */

	private void buildUI(){
		setLayout(new BorderLayout());

		final JPanel header = new JPanel(new MigLayout("ins 8,gapx 12,fillx", "[][grow,fill][]", "[]0[]"));
		header.setBackground(HEADER_BACKGROUND);

		dateLabel.setFont(DATE_FONT);
		dateLabel.setForeground(new Color(40, 35, 25));

		countLabel.setFont(COUNT_FONT);
		countLabel.setForeground(SUBTITLE_COLOR);

		header.add(dateLabel, "split 2,span 2");
		header.add(countLabel, "wrap");

		header.add(new JLabel("Filter:"), "right");
		header.add(searchField, "growx");
		final JButton clear = new JButton("Clear");
		clear.addActionListener(e -> searchField.setText(StringUtils.EMPTY));
		header.add(clear);

		add(header, BorderLayout.NORTH);

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowSorter(sorter);
		table.setFillsViewportHeight(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

		table.getColumnModel().getColumn(0).setPreferredWidth(200);
		table.getColumnModel().getColumn(1).setPreferredWidth(320);
		table.getColumnModel().getColumn(2).setPreferredWidth(140);
		table.getColumnModel().getColumn(3).setPreferredWidth(60);

		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)){
					final int viewRow = table.getSelectedRow();
					if(viewRow < 0)
						return;
					final int modelRow = table.convertRowIndexToModel(viewRow);
					final AgoraRow row = tableModel.getRow(modelRow);
					if(row != null && selectionCallback != null)
						selectionCallback.accept(row.id());
				}
			}
		});

		final JScrollPane scroll = new JScrollPane(table);
		add(scroll, BorderLayout.CENTER);

		add(timeline, BorderLayout.SOUTH);

		searchField.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void insertUpdate(final DocumentEvent e){
				applyFilter();
			}

			@Override
			public void removeUpdate(final DocumentEvent e){
				applyFilter();
			}

			@Override
			public void changedUpdate(final DocumentEvent e){
				applyFilter();
			}
		});
	}

	private void configureTimeline(){
		timeline.withTimeListener(jdn -> {
			currentTime = jdn;
			dateLabel.setText(formatDate((long)jdn));
			refreshTimer.restart();
		});
	}

	private void applyFilter(){
		final String text = searchField.getText();
		if(text == null || text.isBlank())
			sorter.setRowFilter(null);
		else
			sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text.trim())));
	}


	/* ======================================================================
	 *                          Data
	 * ====================================================================== */

	private void refresh(){
		final List<AgoraRow> rows = new ArrayList<>();
		final long t = (long)currentTime;

		for(final String id : currentIds){
			final List<GeoAnchor> anchors = index.anchorsOf(id);
			if(anchors.isEmpty())
				continue;

			final AgoraRow row = evaluate(id, anchors, t);
			if(row != null)
				rows.add(row);
		}

		rows.sort(Comparator.comparing(AgoraRow::name, String.CASE_INSENSITIVE_ORDER));
		tableModel.setRows(rows);
		countLabel.setText(rows.size() + (rows.size() == 1? " person alive": " people alive"));
	}

	/**
	 * Decides whether the given individual was alive at time {@code t}
	 * and, if so, builds the corresponding row. Returns {@code null}
	 * when the person should not appear in the Agora.
	 */
	private AgoraRow evaluate(final String id, final List<GeoAnchor> anchors, final long t){
		GeoAnchor birth = null;
		GeoAnchor death = null;
		long firstAnchorJdn = Long.MAX_VALUE;
		for(final GeoAnchor a : anchors){
			if(a.startJdn() != Long.MIN_VALUE && a.startJdn() < firstAnchorJdn)
				firstAnchorJdn = a.startJdn();
			if("event:birth".equals(a.kind()) && (birth == null || a.startJdn() < birth.startJdn()))
				birth = a;
			if("event:death".equals(a.kind()) && (death == null || a.startJdn() < death.startJdn()))
				death = a;
		}

		// Lower bound: the person must be born (or have an anchor) on or
		// before the chosen date.
		final long bornJdn = (birth != null? birth.startJdn(): firstAnchorJdn);
		if(bornJdn == Long.MAX_VALUE || bornJdn > t)
			return null;

		// Upper bound: a recorded death ends the interval.
		if(death != null && death.startJdn() <= t)
			return null;

		// Plausibility cap: without a death record, stop once the person
		// would be older than a plausible human lifespan.
		final long maxAgeJdn = bornJdn + (long)(MAX_PLAUSIBLE_AGE_YEARS * DAYS_PER_YEAR);
		if(t > maxAgeJdn)
			return null;

		// Position: interpolated between the anchors around t.
		final GeoCoordinate pos = ChronomapOverlayPainter.interpolate(anchors, t);
		if(pos == null)
			return null;

		// Reason and place:
		//   1. if t falls exactly on an event, that event is the reason;
		//   2. otherwise, if t is inside an attribute interval, the attribute
		//      is the reason;
		//   3. otherwise, the most recent anchor before t is the reason,
		//      prefixed by its distance ("since 1832", "last: birth 1800").
		String reason = StringUtils.EMPTY;
		String place = StringUtils.EMPTY;

		// 1. Exact event match.
		for(final GeoAnchor a : anchors)
			if(a.startJdn() == a.endJdn() && a.startJdn() == t){
				reason = describeKind(a.kind());
				place = (a.placeName() != null? a.placeName(): StringUtils.EMPTY);
				break;
			}

// 2. Inside an attribute interval.
		if(reason.isEmpty())
			for(final GeoAnchor a : anchors)
				if(a.startJdn() < a.endJdn() && t >= a.startJdn() && t <= a.endJdn()){
					reason = describeKind(a.kind());
					place = (a.placeName() != null? a.placeName(): StringUtils.EMPTY);
					break;
				}

// 3. Last anchor before t: the "last known state" of the person.
		if(reason.isEmpty()){
			GeoAnchor last = null;
			for(final GeoAnchor a : anchors)
				if(a.startJdn() <= t && (last == null || a.startJdn() > last.startJdn()))
					last = a;

			if(last != null){
				final String kind = describeKind(last.kind());
				reason = "last: " + kind;
				place = (last.placeName() != null? last.placeName(): StringUtils.EMPTY);
			}
			else{
				reason = "in transit";
				place = "between " + describeGap(anchors, t);
			}
		}

		final Integer age = (birth != null
			? (int)Math.floor((t - birth.startJdn()) / DAYS_PER_YEAR)
			: null);
		if(age != null && age < 0)
			return null;

		return new AgoraRow(id, resolveName(id), place, pos.latitude(), pos.longitude(), reason, age);
	}

	private static String describeGap(final List<GeoAnchor> anchors, final long t){
		GeoAnchor before = null;
		GeoAnchor after = null;
		for(final GeoAnchor a : anchors){
			if(a.endJdn() < t && (before == null || a.endJdn() > before.endJdn()))
				before = a;
			if(a.startJdn() > t && (after == null || a.startJdn() < after.startJdn()))
				after = a;
		}
		final String b = (before != null && before.placeName() != null? before.placeName(): "?");
		final String a = (after != null && after.placeName() != null? after.placeName(): "?");
		return b + " and " + a;
	}

	private static String describeKind(final String kind){
		if(kind.startsWith("event:"))
			return kind.substring("event:".length());
		if(kind.startsWith("attribute:"))
			return kind.substring("attribute:".length());
		return kind;
	}

	private String resolveName(final String id){
		final FLEFRecord record = model.getRecordById(id);
		if(record == null)
			return id;

		try{
			final String text = IndividualHandler.getInstance().getDisplayText(record, model);
			return (text != null && !text.isBlank()? text: id);
		}
		catch(final RuntimeException ignored){
			return id;
		}
	}

	private static String formatDate(final long jdn){
		final int[] ymd = jdnToGregorian(jdn);
		return ymd[2] + StringUtils.SPACE + MONTH_NAMES[ymd[1] - 1] + StringUtils.SPACE + ymd[0];
	}


	/* ======================================================================
	 *                          Table model
	 * ====================================================================== */

	private static final class AgoraTableModel extends AbstractTableModel{

		@Serial
		private static final long serialVersionUID = -7182938475610293847L;

		private static final String[] COLUMNS = {"Name", "Place", "Reason", "Age"};

		private List<AgoraRow> rows = List.of();


		void setRows(final List<AgoraRow> rows){
			this.rows = List.copyOf(rows);
			fireTableDataChanged();
		}

		AgoraRow getRow(final int index){
			return (index >= 0 && index < rows.size()? rows.get(index): null);
		}

		@Override
		public int getRowCount(){
			return rows.size();
		}

		@Override
		public int getColumnCount(){
			return COLUMNS.length;
		}

		@Override
		public String getColumnName(final int column){
			return COLUMNS[column];
		}

		@Override
		public Class<?> getColumnClass(final int columnIndex){
			return (columnIndex == 3? Integer.class: String.class);
		}

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex){
			final AgoraRow row = rows.get(rowIndex);
			return switch(columnIndex){
				case 0 -> row.name();
				case 1 -> row.place();
				case 2 -> row.reason();
				case 3 -> row.age();
				default -> null;
			};
		}
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

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
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){
		}

		final String content;
		try(final InputStream is = AgoraPanel.class.getResourceAsStream("/tests/TGMZ.flef")){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}
		final FLEFModel model = new FLEFParser().parse(content);

		final Path cacheFile = Path.of(System.getProperty("user.home"),
			".familylegacy", "geocoding.properties");
		final PlaceCoordinateResolver resolver = new PlaceCoordinateResolver(model, cacheFile);
		final ChronomapIndex index = new ChronomapIndex(model, resolver);

		SwingUtilities.invokeLater(() -> {
			final AgoraPanel panel = new AgoraPanel(model, index);
			panel.setIndividuals(model.getRecordsByType(IndividualHandler.TYPE)
				.stream()
				.map(FLEFRecord::getId)
				.toList());

			final JFrame frame = new JFrame("Agorà");
			frame.add(panel, BorderLayout.CENTER);
			frame.setSize(900, 640);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
