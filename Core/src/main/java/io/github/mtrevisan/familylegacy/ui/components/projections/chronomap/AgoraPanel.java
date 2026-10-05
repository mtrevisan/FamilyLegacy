package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.ui.components.projections.chronomap.ChronomapIndex.GeoAnchor;
import io.github.mtrevisan.familylegacy.ui.components.projections.chronomap.ChronomapIndex.GeoCoordinate;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.tools.events.CalendarConverterDialog;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;


/**
 * "Agorà" view.
 * <p>
 * Displays a population census of all individuals alive on a specific date.
 * If spatial coordinates are available via {@link ChronomapIndex}, the table
 * shows the interpolated location and reason; otherwise, the person is still
 * listed with their known state or as unlocated.
 *
 * <p>The panel can be used standalone (it owns its own timeline) or as part
 * of a {@link io.github.mtrevisan.familylegacy.ui.components.projections.chronomap.ChronomapWorkspace}
 * (it receives a shared timeline and a shared {@link WorkspaceSelection}).
 * In the workspace the two views stay in sync: moving the timeline updates
 * both, and selecting a row highlights the marker on the map.</p>
 */
public final class AgoraPanel extends JPanel{

	private static final int MAX_PLAUSIBLE_AGE_YEARS = 110;
	private static final double DAYS_PER_YEAR = 365.2425;
	private static final int REFRESH_DEBOUNCE_MS = 150;

	private static final Color HEADER_BACKGROUND = new Color(240, 236, 228);
	private static final Color SUBTITLE_COLOR = new Color(120, 115, 100);
	private static final Font DATE_FONT = new Font("Tahoma", Font.BOLD, 18);
	private static final Font COUNT_FONT = new Font("Tahoma", Font.PLAIN, 12);

	private static final String[] MONTH_NAMES = {
		"Jan", "Feb", "Mar", "Apr", "May", "Jun",
		"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
	};

	public record AgoraRow(String id, String name, String place, double latitude, double longitude,
								  String reason, Integer age){
	}

	private final FLEFModel model;
	private final ChronomapIndex index;
	private final ChronomapTimeline timeline;
	private final WorkspaceSelection selection;
	private final boolean ownsTimeline;

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
	/** Suppresses the listener feedback loop when the table selection is
	 *  being updated programmatically from the shared selection. */
	private boolean updatingSelection;


	/* ======================================================================
	 *                          Factories
	 * ====================================================================== */

	/**
	 * Creates a standalone panel with its own timeline and no shared
	 * selection. The timeline is embedded at the bottom of the panel.
	 */
	public static AgoraPanel create(final FLEFModel model){
		final Path cacheFile = Path.of(System.getProperty("user.home"), ".familylegacy", "geocoding.properties");
		final PlaceCoordinateResolver resolver = new PlaceCoordinateResolver(model, cacheFile);
		final ChronomapIndex index = new ChronomapIndex(model, resolver);
		return new AgoraPanel(model, index, null, null);
	}


	/**
	 * Creates a panel that shares the given timeline and selection with
	 * other views. The timeline is not embedded, because the enclosing
	 * workspace installs it below the split pane.
	 */
	public AgoraPanel(final FLEFModel model, final ChronomapIndex index,
		final ChronomapTimeline timeline, final WorkspaceSelection selection){
		if(index == null)
			throw new IllegalArgumentException("ChronomapIndex must not be null");

		this.model = model;
		this.index = index;
		this.ownsTimeline = (timeline == null);
		this.timeline = (timeline != null? timeline: new ChronomapTimeline());
		this.selection = selection;

		this.refreshTimer = new Timer(REFRESH_DEBOUNCE_MS, e -> refresh());
		this.refreshTimer.setRepeats(false);

		buildUI();
		configureTimeline();
		wireSelection();

		final long[] range = index.computeGlobalDateRange();
		if(range != null && range[0] < range[1]){
			this.timeline.setDomain(range[0], range[1]);
			final long mid = range[0] + (range[1] - range[0]) / 2;
			this.timeline.setCurrentTime(mid);
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

	public void setIndividuals(final Collection<String> ids){
		this.currentIds = (ids != null ? List.copyOf(ids) : List.of());
		refresh();
	}

	public void setDate(final double jdn){
		timeline.setCurrentTime(jdn);
		currentTime = timeline.getCurrentTime();
		dateLabel.setText(formatDate((long)currentTime));
		refreshTimer.restart();
	}

	public void withSelectionCallback(final Consumer<String> callback){
		this.selectionCallback = callback;
	}


	/* ======================================================================
	 *                          Selection wiring
	 * ====================================================================== */

	/**
	 * Wires the shared selection so that:
	 * <ul>
	 *   <li>a selection made elsewhere (e.g. on the map) selects the
	 *       corresponding row in the table;</li>
	 *   <li>a row selection in the table updates the shared selection.</li>
	 * </ul>
	 * Does nothing when no shared selection has been provided.
	 */
	private void wireSelection(){
		if(selection == null)
			return;

		selection.addListener(id -> {
			updatingSelection = true;
			try{
				if(id == null)
					table.clearSelection();
				else
					selectRowById(id);
			}
			finally{
				updatingSelection = false;
			}
		});

		table.getSelectionModel().addListSelectionListener(e -> {
			if(e.getValueIsAdjusting() || updatingSelection || selection == null)
				return;

			final int viewRow = table.getSelectedRow();
			if(viewRow < 0)
				return;

			final int modelRow = table.convertRowIndexToModel(viewRow);
			final AgoraRow row = tableModel.getRow(modelRow);
			if(row != null)
				selection.select(row.id());
		});
	}


	/**
	 * Selects the table row whose underlying identifier matches the
	 * given id. Uses {@link JTable#convertRowIndexToView(int)} so that
	 * the selection is applied to the correct visual row even when the
	 * sorter has reordered the table.
	 */
	private void selectRowById(final String id){
		for(int modelRow = 0; modelRow < tableModel.getRowCount(); modelRow ++){
			final AgoraRow row = tableModel.getRow(modelRow);
			if(row != null && id.equals(row.id())){
				final int viewRow = table.convertRowIndexToView(modelRow);
				table.setRowSelectionInterval(viewRow, viewRow);
				table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));

				return;
			}
		}
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
		final JButton clear = new JButton(I18N.t("button.clear"));
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
					if(viewRow < 0){
						return;
					}
					final int modelRow = table.convertRowIndexToModel(viewRow);
					final AgoraRow row = tableModel.getRow(modelRow);
					if(row != null && selectionCallback != null){
						selectionCallback.accept(row.id());
					}
				}
			}
		});

		final JScrollPane scroll = new JScrollPane(table);
		add(scroll, BorderLayout.CENTER);

		// Embed the timeline only when this panel owns it. In the
		// workspace the timeline is installed by the workspace.
		if(ownsTimeline)
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
			dateLabel.setText(formatDate(jdn.longValue()));
			refreshTimer.restart();
		});
	}

	private void applyFilter(){
		final String text = searchField.getText();
		if(text == null || text.isBlank()){
			sorter.setRowFilter(null);
		}
		else{
			sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text.trim())));
		}
	}

	private void refresh(){
		final List<AgoraRow> rows = new ArrayList<>();
		final long t = (long)currentTime;

		for(final String id : currentIds){
			final List<GeoAnchor> anchors = index.anchorsOf(id);
			final AgoraRow row = evaluate(id, anchors, t);
			if(row != null){
				rows.add(row);
			}
		}

		rows.sort(Comparator.comparing(AgoraRow::name, String.CASE_INSENSITIVE_ORDER));
		tableModel.setRows(rows);
		countLabel.setText(rows.size() + (rows.size() == 1 ? " person alive" : " people alive"));
	}

	private AgoraRow evaluate(final String id, final List<GeoAnchor> anchors, final long t){
		Long bornJdn = getBirthJdn(id, anchors);
		Long deathJdn = getDeathJdn(id, anchors);

		if(bornJdn == null && !anchors.isEmpty()){
			long firstAnchor = Long.MAX_VALUE;
			for(final GeoAnchor a : anchors)
				if(a.startJdn() != Long.MIN_VALUE && a.startJdn() < firstAnchor)
					firstAnchor = a.startJdn();
			if(firstAnchor != Long.MAX_VALUE)
				bornJdn = firstAnchor;
		}

		if(bornJdn == null || bornJdn > t)
			return null;

		if(deathJdn != null && deathJdn <= t)
			return null;

		final long maxAgeJdn = bornJdn + (long)(MAX_PLAUSIBLE_AGE_YEARS * DAYS_PER_YEAR);
		if(t > maxAgeJdn){
			return null;
		}

		// Retrieve interpolated position state and extract coordinate
		final ChronomapOverlayPainter.InterpolatedPosition state = ChronomapOverlayPainter.interpolatePosition(null, anchors, t);
		final GeoCoordinate pos = (state != null ? state.coordinate() : null);
		final double lat = (pos != null ? pos.latitude() : Double.NaN);
		final double lon = (pos != null ? pos.longitude() : Double.NaN);

		String reason = StringUtils.EMPTY;
		String place = StringUtils.EMPTY;

		if(!anchors.isEmpty()){
			for(final GeoAnchor a : anchors){
				if(a.startJdn() == a.endJdn() && a.startJdn() == t){
					reason = describeKind(a.kind());
					place = (a.placeName() != null ? a.placeName() : StringUtils.EMPTY);

					break;
				}
			}

			if(reason.isEmpty())
				for(final GeoAnchor a : anchors)
					if(a.startJdn() < a.endJdn() && t >= a.startJdn() && t <= a.endJdn()){
						reason = describeKind(a.kind());
						place = (a.placeName() != null ? a.placeName() : StringUtils.EMPTY);

						break;
					}

			if(reason.isEmpty()){
				GeoAnchor last = null;
				for(final GeoAnchor a : anchors)
					if(a.startJdn() <= t && (last == null || a.startJdn() > last.startJdn()))
						last = a;

				if(last != null){
					reason = "last: " + describeKind(last.kind());
					place = (last.placeName() != null ? last.placeName() : StringUtils.EMPTY);
				}
				else{
					reason = "in transit";
					place = "between " + describeGap(anchors, t);
				}
			}
		}
		else{
			reason = "alive";
			place = "--";
		}

		final Integer age = (int)Math.floor((t - bornJdn) / DAYS_PER_YEAR);
		if(age < 0)
			return null;

		return new AgoraRow(id, resolveName(id), place, lat, lon, reason, age);
	}

	private Long getBirthJdn(final String id, final List<GeoAnchor> anchors){
		// Fast path: the anchor list may already carry a birth event with a
		// geographic position, in which case no further lookup is needed.
		for(final GeoAnchor a : anchors)
			if("event:birth".equals(a.kind()))
				return a.startJdn();

		// Otherwise consult the pre-computed life-event index, which is built
		// once per rebuild and answers in O(1).
		return index.birthJdnOf(id);
	}

	private Long getDeathJdn(final String id, final List<GeoAnchor> anchors){
		for(final GeoAnchor a : anchors)
			if("event:death".equals(a.kind()))
				return a.startJdn();

		return index.deathJdnOf(id);
	}

	private static String describeGap(final List<GeoAnchor> anchors, final long t){
		GeoAnchor before = null;
		GeoAnchor after = null;
		for(final GeoAnchor a : anchors){
			if(a.endJdn() < t && (before == null || a.endJdn() > before.endJdn())){
				before = a;
			}
			if(a.startJdn() > t && (after == null || a.startJdn() < after.startJdn())){
				after = a;
			}
		}
		final String b = (before != null && before.placeName() != null ? before.placeName() : "?");
		final String a = (after != null && after.placeName() != null ? after.placeName() : "?");
		return b + " and " + a;
	}

	private static String describeKind(final String kind){
		if(kind.startsWith("event:")){
			return kind.substring("event:".length());
		}
		if(kind.startsWith("attribute:")){
			return kind.substring("attribute:".length());
		}
		return kind;
	}

	private String resolveName(final String id){
		final FLEFRecord record = model.getRecordById(id);
		if(record == null){
			return id;
		}

		try{
			final String text = IndividualHandler.getInstance().getDisplayText(record, model);
			return (text != null && !text.isBlank() ? text : id);
		}
		catch(final RuntimeException ignored){
			return id;
		}
	}

	private static String formatDate(final long jdn){
		final int[] ymd = CalendarConverterDialog.jdnToGregorian(jdn);
		return ymd[2] + StringUtils.SPACE + MONTH_NAMES[ymd[1] - 1] + StringUtils.SPACE + ymd[0];
	}

	private static final class AgoraTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {"Name", "Place", "Reason", "Age"};

		private List<AgoraRow> rows = List.of();

		void setRows(final List<AgoraRow> rows){
			this.rows = List.copyOf(rows);
			fireTableDataChanged();
		}

		AgoraRow getRow(final int index){
			return (index >= 0 && index < rows.size() ? rows.get(index) : null);
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
			return (columnIndex == 3 ? Integer.class : String.class);
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
	 *                          Standalone demo
	 * ====================================================================== */

	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){
		}

		final String content;
		try(final InputStream is = AgoraPanel.class.getResourceAsStream("/tests/TGMZ.flef")){
			content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
		final FLEFModel model = new FLEFParser().parse(content);

		final Path cacheFile = Path.of(System.getProperty("user.home"), ".familylegacy", "geocoding.properties");
		final PlaceCoordinateResolver resolver = new PlaceCoordinateResolver(model, cacheFile);
		final ChronomapIndex index = new ChronomapIndex(model, resolver);

		SwingUtilities.invokeLater(() -> {
			final AgoraPanel panel = new AgoraPanel(model, index, null, null);
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
