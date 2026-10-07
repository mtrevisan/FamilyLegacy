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
package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.projections.chronomap.ChronomapIndex.GeoAnchor;
import io.github.mtrevisan.familylegacy.ui.components.projections.chronomap.ChronomapIndex.GeoCoordinate;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.tools.events.CalendarConverterDialog;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
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
 * Displays a population census of all individuals alive on a specific
 * date. The life test is delegated to
 * {@link ChronomapIndex#isAliveAt(List, double)}, the same method used
 * by the chronomap marker layer: the set of people listed here and the
 * set of people drawn on the map therefore always coincide.
 * <p>
 * The panel can be used standalone (it owns its own timeline) or as
 * part of a {@link ChronomapWorkspace} (it receives a shared timeline
 * and a shared {@link WorkspaceSelection}). In the workspace the two
 * views stay in sync: moving the timeline updates both, and selecting
 * a row highlights the marker on the map.
 */
public final class AgoraPanel extends JPanel{

	private static final int REFRESH_DEBOUNCE_MS = 150;

	private static final Color HEADER_BACKGROUND = new Color(240, 236, 228);
	private static final Color SUBTITLE_COLOR = new Color(120, 115, 100);
	private static final Font DATE_FONT = new Font("Tahoma", Font.BOLD, 18);
	private static final Font COUNT_FONT = new Font("Tahoma", Font.PLAIN, 12);

	private static final Border CELL_BORDER = BorderFactory.createMatteBorder(
		0, 0, 1, 1, new Color(200, 195, 185));

	private static final class BorderedCellRenderer extends DefaultTableCellRenderer{

		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value,
				final boolean isSelected, final boolean hasFocus, final int row, final int column){
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			setBorder(BorderFactory.createCompoundBorder(
				CELL_BORDER,
				BorderFactory.createEmptyBorder(2, 6, 2, 6)
			));
			return this;
		}
	}

	private static final String[] MONTH_NAMES = {
		"Jan", "Feb", "Mar", "Apr", "May", "Jun",
		"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
	};

	public record AgoraRow(String id, String name, String place, double latitude, double longitude, String reason,
		Integer age){}

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
	/**
	 * Suppresses the listener feedback loop when the table selection is
	 * being updated programmatically from the shared selection.
	 */
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
		this.currentIds = (ids != null? List.copyOf(ids): List.of());

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
			if(e.getValueIsAdjusting() || updatingSelection)
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

		/* ==================================================================
		 *                          Header
		 * ================================================================== */

		final JPanel header = new JPanel(new MigLayout("ins 8,gapx 12,fillx",
			"[][grow,fill][]", "[]0[]"));
		header.setBackground(HEADER_BACKGROUND);
		header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
			new Color(210, 205, 195)));

		dateLabel.setFont(DATE_FONT);
		dateLabel.setForeground(new Color(40, 35, 25));

		countLabel.setFont(COUNT_FONT);
		countLabel.setForeground(SUBTITLE_COLOR);

		header.add(dateLabel, "split 2,span 2");
		header.add(countLabel, "wrap");

		header.add(new JLabel("Filter:"), "right");
		header.add(searchField, "growx");
		final JButton clear = new JButton("Clear");
		clear.addActionListener(e -> searchField.setText(""));
		header.add(clear);

		add(header, BorderLayout.NORTH);

		/* ==================================================================
		 *                          Table
		 * ================================================================== */

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowSorter(sorter);
		table.setFillsViewportHeight(true);
		// Distribute extra horizontal space across all columns proportionally
		// to their preferred width, instead of dumping it all into the last
		// one. With AUTO_RESIZE_LAST_COLUMN the Age column would absorb all
		// the leftover width and look disproportionately large.
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		table.setShowGrid(false);
		table.setGridColor(new Color(230, 226, 218));
		table.setIntercellSpacing(new Dimension(0, 0));
		table.setRowHeight(22);
		final var columnModel = table.getColumnModel();
		for(int i = 0; i < columnModel.getColumnCount(); i ++){
			final var col = columnModel.getColumn(i);
			final var renderer = new BorderedCellRenderer();
			if(i == 3)
				renderer.setHorizontalAlignment(SwingConstants.RIGHT);
			col.setCellRenderer(renderer);
		}

		// Name
		final TableColumn columnName = columnModel.getColumn(0);
		columnName.setPreferredWidth(200);
		columnName.setMinWidth(120);
		// Place
		final TableColumn columnPlace = columnModel.getColumn(1);
		columnPlace.setPreferredWidth(320);
		columnPlace.setMinWidth(180);
		// Reason
		final TableColumn columnReason = columnModel.getColumn(2);
		columnReason.setPreferredWidth(140);
		columnReason.setMinWidth(100);
		// Age: keep it narrow even when the table is resized.
		final TableColumn columnAge = columnModel.getColumn(3);
		columnAge.setPreferredWidth(60);
		columnAge.setMinWidth(50);
		columnAge.setMaxWidth(90);

		// Header: bold, not reorderable, and a subtle border below it so
		// the column titles read as a distinct band.
		final JTableHeader headerBar = table.getTableHeader();
		headerBar.setReorderingAllowed(false);
		headerBar.setFont(headerBar.getFont().deriveFont(Font.BOLD));
		headerBar.setBackground(new Color(240, 236, 228));
		headerBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
			new Color(210, 205, 195)));

		// Double-click on a row: notify the selection callback with the
		// individual id, so the enclosing frame can open the dossier or
		// re-root a projection.
		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(!SwingUtilities.isLeftMouseButton(e))
					return;

				final int viewRow = table.getSelectedRow();
				if(viewRow < 0)
					return;

				final int modelRow = table.convertRowIndexToModel(viewRow);
				final AgoraRow row = tableModel.getRow(modelRow);
				if(row == null)
					return;

				if(e.getClickCount() == 1){
					// Single click: forward the selection to the workspace, so
					// the shared selection highlights the corresponding marker
					// on the chronomap (or whatever panel is listening).
					if(selectionCallback != null)
						selectionCallback.accept(row.id());
				}
				else if(e.getClickCount() == 2)
					// Double click: open the edit dialog for the individual.
					openEditDialog(row.id());
			}
		});

		// Scroll pane with a visible outer border, so the table does not
		// look like it ends abruptly at the last row.
		final JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createLineBorder(new Color(210, 205, 195)));
		scroll.getViewport()
			.setBackground(Color.WHITE);
		add(scroll, BorderLayout.CENTER);

		/* ==================================================================
		 *                          Timeline
		 * ================================================================== */

		add(timeline, BorderLayout.SOUTH);

		/* ==================================================================
		 *                          Filter wiring
		 * ================================================================== */

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
		if(text == null || text.isBlank())
			sorter.setRowFilter(null);
		else
			sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text.trim())));
	}

	private void refresh(){
		final List<AgoraRow> rows = new ArrayList<>();
		final long t = (long)currentTime;
		for(final String id : currentIds){
			final List<GeoAnchor> anchors = index.anchorsOf(id);
			final AgoraRow row = evaluate(id, anchors, t);
			if(row != null)
				rows.add(row);
		}

		rows.sort(Comparator.comparing(AgoraRow::name, String.CASE_INSENSITIVE_ORDER));
		tableModel.setRows(rows);
		countLabel.setText(rows.size() + (rows.size() == 1? " person alive": " people alive"));
	}

	/**
	 * Builds the table row for one individual, or returns {@code null}
	 * when the individual should not appear.
	 * <p>
	 * The life test is delegated to
	 * {@link ChronomapIndex#isAliveAt(List, double)}, the same method
	 * used by the chronomap marker layer. This guarantees that the set
	 * of people shown on the map and the set of people listed here
	 * always coincide: a person who is alive for the map is alive for
	 * the list, and vice versa.
	 * <p>
	 * People with no anchors at all are excluded, because they cannot
	 * be placed on the map. This is a deliberate consequence of the
	 * shared test: the Agora is a spatial census, not a general list
	 * of the living.
	 */
	private AgoraRow evaluate(final String id, final List<GeoAnchor> anchors, final long t){
		if(!ChronomapIndex.isAliveAt(anchors, t))
			return null;

		// Age: computed from the birth event, or from the earliest
		// anchor when no birth is recorded. This mirrors the same
		// fallback used by ChronomapIndex.isAliveAt, so the two values
		// are consistent.
		final Long bornJdn = earliestBirthJdn(anchors);
		final Integer age = (bornJdn != null
			? (int)Math.floor((t - bornJdn) / ChronomapIndex.DAYS_PER_YEAR)
			: null);

		// Position: interpolated between the anchors around t.
		final ChronomapOverlayPainter.InterpolatedPosition state =
			ChronomapOverlayPainter.interpolatePosition(null, anchors, t);
		final GeoCoordinate pos = (state != null? state.coordinate(): null);
		final double lat = (pos != null? pos.latitude(): Double.NaN);
		final double lon = (pos != null? pos.longitude(): Double.NaN);

		// Reason and place.
		String reason = StringUtils.EMPTY;
		String place = StringUtils.EMPTY;

		for(final GeoAnchor a : anchors)
			if(a.startJdn() == a.endJdn() && a.startJdn() == t){
				reason = describeKind(a.kind());
				place = (a.placeName() != null? a.placeName(): StringUtils.EMPTY);

				break;
			}

		if(reason.isEmpty())
			for(final GeoAnchor a : anchors)
				if(a.startJdn() < a.endJdn() && t >= a.startJdn() && t <= a.endJdn()){
					reason = describeKind(a.kind());
					place = (a.placeName() != null? a.placeName(): StringUtils.EMPTY);

					break;
				}

		if(reason.isEmpty()){
			GeoAnchor last = null;
			for(final GeoAnchor a : anchors)
				if(a.startJdn() <= t && (last == null || a.startJdn() > last.startJdn()))
					last = a;

			if(last != null){
				reason = "last: " + describeKind(last.kind());
				place = (last.placeName() != null? last.placeName(): StringUtils.EMPTY);
			}
			else{
				reason = "in transit";
				place = "between " + describeGap(anchors, t);
			}
		}

		return new AgoraRow(id, resolveName(id), place, lat, lon, reason, age);
	}

	/**
	 * Returns the JDN of the birth event, or, when no birth event is
	 * recorded, the JDN of the earliest anchor. Returns {@code null}
	 * when the anchor list is empty or contains only anchors with an
	 * open start date ({@link Long#MIN_VALUE}).
	 */
	private static Long earliestBirthJdn(final List<GeoAnchor> anchors){
		Long earliest = null;
		for(final GeoAnchor a : anchors){
			if("event:birth".equals(a.kind()))
				return a.startJdn();

			if(a.startJdn() != Long.MIN_VALUE && (earliest == null || a.startJdn() < earliest))
				earliest = a.startJdn();
		}
		return earliest;
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
		final int[] ymd = CalendarConverterDialog.jdnToGregorian(jdn);
		return ymd[2] + StringUtils.SPACE + MONTH_NAMES[ymd[1] - 1] + StringUtils.SPACE + ymd[0];
	}

	/**
	 * Opens the standard edit dialog for the given individual. On a
	 * successful edit the shared {@link ChronomapIndex} is rebuilt, because
	 * the change may affect the anchors (a birth date, a place, an event),
	 * and the table is refreshed so the Agora shows the updated data.
	 * <p>
	 * The index is shared with the chronomap and any other view that uses
	 * the same instance, so rebuilding it here also affects those views:
	 * this is intentional, since the edit is a global change to the model.
	 *
	 * @param individualId the individual to edit; must not be {@code null}
	 */
	private void openEditDialog(final String individualId){
		final FLEFRecord record = model.getRecordById(individualId);
		if(record == null)
			return;

		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(record.getTag());
		if(handler == null)
			return;

		final Window owner = SwingUtilities.getWindowAncestor(this);
		final BaseRecordDialog dialog = handler.createEditDialog(owner, model, record);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			index.rebuild();

			refresh();
		}
	}


	private static final class AgoraTableModel extends AbstractTableModel{

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
	 *                          Standalone demo
	 * ====================================================================== */

	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

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
