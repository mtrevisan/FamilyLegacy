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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Full-text search panel over the participants of all events.
 * <p>
 * Provides:
 * <ul>
 *   <li>a search field that filters participant names with a
 *       case-insensitive substring match, applied live as the user
 *       types;</li>
 *   <li>a role filter that restricts the results to a single role;</li>
 *   <li>a table with one row per participation, showing participant,
 *       type, role, event type, event date and place;</li>
 *   <li>double-click on a row opens the edit dialog of the
 *       participant;</li>
 *   <li>right-click opens a context menu with "Edit participant" and
 *       "Edit event";</li>
 *   <li>a status line showing the number of results.</li>
 * </ul>
 * After a successful edit the index is rebuilt, so that any name or role
 * change is immediately reflected in the results.
 */
@Deprecated
public class ParticipantSearchPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 6128741529384567428L;


	/** Value of the role combo that disables the role filter. */
	private static final String ALL_ROLES = "(all roles)";


	private static final String[] COLUMN_NAMES = {
		"Participant", "Type", "Role", "Event", "Date", "Place"
	};
	private static final int COL_PARTICIPANT = 0;
	private static final int COL_TYPE = 1;
	private static final int COL_ROLE = 2;
	private static final int COL_EVENT = 3;
	private static final int COL_DATE = 4;
	private static final int COL_PLACE = 5;

	private static final Font LINK_FONT = new Font("Tahoma", Font.PLAIN, 12);
	private static final Color LINK_COLOR = new Color(30, 80, 180);
	private static final Color LINK_COLOR_SELECTED = Color.WHITE;
	private static final Color SELECTION_BG = new Color(200, 220, 245);
	private static final Color EMPTY_MSG_COLOR = new Color(120, 120, 120);

	private static final String[] MONTH_NAMES = {
		"Jan", "Feb", "Mar", "Apr", "May", "Jun",
		"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
	};


	private final FLEFModel model;

	private ParticipantIndex index;
	private final ParticipantTableModel tableModel = new ParticipantTableModel();

	private final JTextField searchField = new JTextField(24);
	private final JComboBox<String> roleCombo = new JComboBox<>();
	private final JButton clearButton = new JButton("Clear");
	private final JButton editParticipantButton = new JButton("Edit participant");
	private final JButton editEventButton = new JButton("Edit event");
	private final JLabel statusLabel = new JLabel();

	private final JTable table = new JTable(tableModel);


	/**
	 * Constructor.
	 *
	 * @param model the FLEF model (must not be {@code null})
	 */
	public ParticipantSearchPanel(final FLEFModel model){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");
		this.model = model;

		this.index = ParticipantIndex.build(model);

		buildUI();
		installListeners();
		refreshRoleCombo();
		applyFilter();
	}


	/* ======================================================================
	 *                          UI construction
	 * ====================================================================== */

	private void buildUI(){
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		add(buildFilterBar(), BorderLayout.NORTH);
		add(buildTableScroll(), BorderLayout.CENTER);
		add(buildStatusBar(), BorderLayout.SOUTH);
	}

	private JPanel buildFilterBar(){
		final JPanel bar = new JPanel(new MigLayout("ins 0,gapx 6", "[][grow,fill][][][]", "[]"));

		bar.add(new JLabel("Search:"));
		bar.add(searchField, "growx");
		bar.add(new JLabel("Role:"));
		bar.add(roleCombo);
		bar.add(clearButton);

		return bar;
	}

	private JScrollPane buildTableScroll(){
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(22);
		table.setShowGrid(false);
		table.setIntercellSpacing(new Dimension(0, 1));
		table.setFillsViewportHeight(true);
		table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		final JTableHeader header = table.getTableHeader();
		header.setReorderingAllowed(false);
		header.setResizingAllowed(true);

		// Column widths.
		table.getColumnModel()
			.getColumn(COL_PARTICIPANT)
			.setPreferredWidth(220);
		table.getColumnModel()
			.getColumn(COL_TYPE)
			.setPreferredWidth(70);
		table.getColumnModel()
			.getColumn(COL_ROLE)
			.setPreferredWidth(110);
		table.getColumnModel()
			.getColumn(COL_EVENT)
			.setPreferredWidth(120);
		table.getColumnModel()
			.getColumn(COL_DATE)
			.setPreferredWidth(110);
		table.getColumnModel()
			.getColumn(COL_PLACE)
			.setPreferredWidth(180);

		// Participant column as a clickable link.
		table.getColumnModel()
			.getColumn(COL_PARTICIPANT)
			.setCellRenderer(new LinkCellRenderer());

		final JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createTitledBorder("Participants"));
		scroll.getVerticalScrollBar()
			.setUnitIncrement(16);
		return scroll;
	}

	private JPanel buildStatusBar(){
		final JPanel bar = new JPanel(new MigLayout("ins 0,gapx 6", "[grow][][]", "[]"));
		bar.add(statusLabel, "growx");
		bar.add(editParticipantButton);
		bar.add(editEventButton);
		return bar;
	}


	/* ======================================================================
	 *                          Listeners
	 * ====================================================================== */

	private void installListeners(){
		// Live search: re-apply the filter on every change.
		searchField.getDocument()
			.addDocumentListener(new DocumentListener(){
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

		roleCombo.addActionListener(e -> applyFilter());

		clearButton.addActionListener(e -> {
			searchField.setText("");
			roleCombo.setSelectedIndex(0);
			applyFilter();
		});

		editParticipantButton.addActionListener(e -> editSelectedParticipant());
		editEventButton.addActionListener(e -> editSelectedEvent());

		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(!SwingUtilities.isLeftMouseButton(e))
					return;
				final int row = table.rowAtPoint(e.getPoint());
				if(row < 0)
					return;
				table.setRowSelectionInterval(row, row);
				if(e.getClickCount() == 2){
					final int col = table.columnAtPoint(e.getPoint());
					if(col == COL_EVENT)
						editSelectedEvent();
					else
						editSelectedParticipant();
				}
			}

			@Override
			public void mousePressed(final MouseEvent e){
				if(e.isPopupTrigger())
					showContextMenu(e);
			}

			@Override
			public void mouseReleased(final MouseEvent e){
				if(e.isPopupTrigger())
					showContextMenu(e);
			}
		});
	}

	private void showContextMenu(final MouseEvent e){
		final int row = table.rowAtPoint(e.getPoint());
		if(row < 0)
			return;
		table.setRowSelectionInterval(row, row);

		final JPopupMenu menu = new JPopupMenu();
		final JMenuItem editPerson = new JMenuItem("Edit participant…");
		editPerson.addActionListener(a -> editSelectedParticipant());
		menu.add(editPerson);

		final JMenuItem editEvent = new JMenuItem("Edit event…");
		editEvent.addActionListener(a -> editSelectedEvent());
		menu.add(editEvent);

		menu.show(table, e.getX(), e.getY());
	}


	/* ======================================================================
	 *                          Filtering
	 * ====================================================================== */

	private void applyFilter(){
		final String query = searchField.getText();
		final String role = selectedRole();

		final List<ParticipantIndex.ParticipantEntry> results = index.search(query, role);
		tableModel.setEntries(results);

		final int total = index.size();
		final int shown = results.size();
		if(shown == total)
			statusLabel.setText(total + (total == 1? " participation": " participations"));
		else
			statusLabel.setText(shown + " of " + total + (total == 1? " participation": " participations"));

		updateButtonsEnabled();
	}

	private String selectedRole(){
		final Object sel = roleCombo.getSelectedItem();
		if(sel == null || ALL_ROLES.equals(sel))
			return null;
		return sel.toString();
	}

	private void refreshRoleCombo(){
		final DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
		model.addElement(ALL_ROLES);
		for(final String role : index.distinctRoles())
			model.addElement(role);
		roleCombo.setModel(model);
	}

	private void updateButtonsEnabled(){
		final boolean hasSelection = (table.getSelectedRow() >= 0);
		editParticipantButton.setEnabled(hasSelection);
		editEventButton.setEnabled(hasSelection);
	}


	/* ======================================================================
	 *                          Edit actions
	 * ====================================================================== */

	private void editSelectedParticipant(){
		final ParticipantIndex.ParticipantEntry entry = selectedEntry();
		if(entry == null)
			return;

		final FLEFRecord record = model.getRecordById(entry.participantId());
		if(record == null)
			return;

		final Window owner = SwingUtilities.getWindowAncestor(this);
		final BaseRecordDialog dialog = (entry.isIndividual()
			? IndividualHandler.getInstance()
			.createEditDialog(owner, model, record)
			: GroupHandler.getInstance()
				.createEditDialog(owner, model, record));
		dialog.setVisible(true);
		if(dialog.isSaved())
			rebuildIndexAndRefresh();
	}

	private void editSelectedEvent(){
		final ParticipantIndex.ParticipantEntry entry = selectedEntry();
		if(entry == null)
			return;

		final FLEFRecord record = model.getRecordById(entry.eventId());
		if(record == null)
			return;

		final Window owner = SwingUtilities.getWindowAncestor(this);
		final BaseRecordDialog dialog = EventHandler.getInstance()
			.createEditDialog(owner, model, record);
		dialog.setVisible(true);
		if(dialog.isSaved())
			rebuildIndexAndRefresh();
	}

	private ParticipantIndex.ParticipantEntry selectedEntry(){
		final int row = table.getSelectedRow();
		if(row < 0)
			return null;
		final int modelRow = table.convertRowIndexToModel(row);
		return tableModel.entryAt(modelRow);
	}

	/**
	 * Rebuilds the index after a model change and re-applies the current
	 * filter, so that the table reflects the updated names and roles.
	 */
	private void rebuildIndexAndRefresh(){
		this.index = ParticipantIndex.build(model);
		refreshRoleCombo();
		applyFilter();
	}


	/* ======================================================================
	 *                          Date formatting
	 * ====================================================================== */

	private static String formatDate(final NormalizedDate date){
		if(date == null)
			return "";
		final int[] ymd = jdnToGregorian(date.jdn());
		return switch(date.precision()){
			case DAY -> ymd[2] + " " + MONTH_NAMES[ymd[1] - 1] + " " + ymd[0];
			case MONTH -> MONTH_NAMES[ymd[1] - 1] + " " + ymd[0];
			case YEAR -> Integer.toString(ymd[0]);
			case DECADE -> (ymd[0] / 10 * 10) + "s";
			case CENTURY -> (ymd[0] / 100 + 1) + "th c.";
		};
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
	 *                          Table model
	 * ====================================================================== */

	private static final class ParticipantTableModel extends AbstractTableModel{

		@java.io.Serial
		private static final long serialVersionUID = -5920194587192830291L;


		private List<ParticipantIndex.ParticipantEntry> entries = new ArrayList<>();


		void setEntries(final List<ParticipantIndex.ParticipantEntry> entries){
			this.entries = (entries != null? new ArrayList<>(entries): new ArrayList<>());
			fireTableDataChanged();
		}

		ParticipantIndex.ParticipantEntry entryAt(final int row){
			if(row < 0 || row >= entries.size())
				return null;
			return entries.get(row);
		}

		@Override
		public int getRowCount(){
			return entries.size();
		}

		@Override
		public int getColumnCount(){
			return COLUMN_NAMES.length;
		}

		@Override
		public String getColumnName(final int column){
			return COLUMN_NAMES[column];
		}

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex){
			final ParticipantIndex.ParticipantEntry e = entries.get(rowIndex);
			return switch(columnIndex){
				case COL_PARTICIPANT -> e.participantName();
				case COL_TYPE -> e.isIndividual()? "person": "group";
				case COL_ROLE -> e.role();
				case COL_EVENT -> e.eventType();
				case COL_DATE -> formatDate(e.eventDate());
				case COL_PLACE -> (e.placeName() != null? e.placeName(): "");
				default -> "";
			};
		}
	}


	/* ======================================================================
	 *                          Cell renderer
	 * ====================================================================== */

	/**
	 * Renders the participant name as a clickable link: colored and
	 * underlined. Falls back to the default rendering when the row is
	 * selected, to preserve the selection highlight.
	 */
	private static final class LinkCellRenderer extends DefaultTableCellRenderer{

		@java.io.Serial
		private static final long serialVersionUID = -1035928461029481039L;


		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
			final boolean hasFocus, final int row, final int column){
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if(isSelected){
				setForeground(LINK_COLOR_SELECTED);
				setBackground(SELECTION_BG);
				setText(value != null? value.toString(): "");
			}
			else{
				setForeground(LINK_COLOR);
				setBackground(table.getBackground());
				setText(value != null? "<html><u>" + escape(value.toString()) + "</u></html>": "");
			}
			return this;
		}

		private static String escape(final String s){
			return s.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;");
		}
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
		try(final InputStream is = ParticipantSearchPanel.class.getResourceAsStream("/tests/TGMZ.flef")){
			content = new String(Objects.requireNonNull(is)
				.readAllBytes(), StandardCharsets.UTF_8);
		}
		final FLEFModel model = new FLEFParser().parse(content);

		SwingUtilities.invokeLater(() -> {
			final ParticipantSearchPanel panel = new ParticipantSearchPanel(model);
			final JFrame frame = new JFrame("Participant Search");
			frame.add(panel);
			frame.setSize(1000, 620);
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
