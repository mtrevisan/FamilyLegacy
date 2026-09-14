package io.github.mtrevisan.familylegacy.v2.ui.tools.places;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDialogs;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Modal dialog that lists every {@code PlaceRecord} and allows the user
 * to create, edit, and delete places.
 * <p>
 * The table is filterable by substring on any column (name, type,
 * parents, children, coordinates) and sortable by clicking the column
 * header. A double-click on a row opens the record editor.
 */
public final class PlaceManagementDialog extends JDialog{

	private final ToolContext context;
	private final PlaceTableModel tableModel = new PlaceTableModel();
	private final JTable table = new JTable(tableModel);
	private final JTextField searchField = new JTextField(24);
	private final JLabel statusLabel = new JLabel(" ");


	public PlaceManagementDialog(final ToolContext context){
		super(context.owner(), "Manage Places", ModalityType.APPLICATION_MODAL);
		this.context = context;

		setLayout(new BorderLayout(6, 6));
		add(createToolbar(), BorderLayout.NORTH);
		add(createTable(), BorderLayout.CENTER);
		add(createFooter(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(900, 560));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());

		reload();
	}


	private JPanel createToolbar(){
		final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
		toolbar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

		toolbar.add(new JLabel("Search:"));
		toolbar.add(searchField);
		searchField.getDocument().addDocumentListener(new DocumentListener(){
			@Override public void insertUpdate(final DocumentEvent e){ applyFilter(); }
			@Override public void removeUpdate(final DocumentEvent e){ applyFilter(); }
			@Override public void changedUpdate(final DocumentEvent e){ applyFilter(); }
		});

		final JButton newButton = new JButton("New…");
		newButton.addActionListener(e -> openEditor(null));
		toolbar.add(newButton);

		final JButton editButton = new JButton("Edit…");
		editButton.addActionListener(e -> openEditor(selectedPlaceId()));
		toolbar.add(editButton);

		final JButton deleteButton = new JButton("Delete");
		deleteButton.addActionListener(e -> deleteSelected());
		toolbar.add(deleteButton);

		return toolbar;
	}

	private JScrollPane createTable(){
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(22);
		table.setAutoCreateRowSorter(true);
		table.getColumnModel().getColumn(0).setPreferredWidth(220);
		table.getColumnModel().getColumn(1).setPreferredWidth(120);
		table.getColumnModel().getColumn(2).setPreferredWidth(180);
		table.getColumnModel().getColumn(3).setPreferredWidth(180);
		table.getColumnModel().getColumn(4).setPreferredWidth(150);

		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
					openEditor(selectedPlaceId());
			}
		});

		final JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createTitledBorder("Places"));
		return scroll;
	}

	private JPanel createFooter(){
		final JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		footer.setBorder(BorderFactory.createEmptyBorder(2, 6, 6, 6));
		footer.add(statusLabel);
		final JButton close = new JButton("Close");
		close.addActionListener(e -> dispose());
		footer.add(close);
		return footer;
	}


	/* ======================================================================
	 *                          Actions
	 * ====================================================================== */

	private void reload(){
		final FLEFModel model = context.model();
		final Map<String, FLEFRecord> placesById = model.getPlacesById();
		final List<PlaceHelper.PlaceRow> rows = new ArrayList<>();
		for(final FLEFRecord place : PlaceHelper.listAllPlaces(model))
			rows.add(PlaceHelper.toRow(place, model, placesById));

		tableModel.setRows(rows);
		updateStatus(rows.size());
	}

	private void applyFilter(){
		final String text = searchField.getText();
		@SuppressWarnings("unchecked")
		final TableRowSorter<PlaceTableModel> sorter =
			(TableRowSorter<PlaceTableModel>)table.getRowSorter();
		if(text == null || text.isBlank())
			sorter.setRowFilter(null);
		else{
			final String needle = text.trim().toLowerCase(java.util.Locale.ROOT);
			sorter.setRowFilter(new RowFilter<>(){
				@Override
				public boolean include(final Entry<? extends PlaceTableModel, ? extends Integer> entry){
					final PlaceHelper.PlaceRow row = tableModel.getRow(entry.getIdentifier());
					return contains(row.name(), needle)
						|| contains(row.type(), needle)
						|| contains(row.parents(), needle)
						|| contains(row.children(), needle)
						|| contains(row.coordinates(), needle);
				}
			});
		}
		updateStatus(table.getRowCount());
	}

	private static boolean contains(final String haystack, final String needle){
		return haystack != null && haystack.toLowerCase(java.util.Locale.ROOT).contains(needle);
	}

	private String selectedPlaceId(){
		final int viewRow = table.getSelectedRow();
		if(viewRow < 0)
			return null;
		final int modelRow = table.convertRowIndexToModel(viewRow);
		return tableModel.getRow(modelRow).id();
	}

	private void openEditor(final String placeId){
		final Window owner = this;
		final PlaceHandler handler = PlaceHandler.getInstance();
		final BaseRecordDialog dialog;
		if(placeId == null)
			dialog = handler.createNewDialog(owner, context.model());
		else{
			final FLEFRecord record = context.model().getRecordById(placeId);
			if(record == null)
				return;
			dialog = handler.createEditDialog(owner, context.model(), record);
		}
		dialog.setVisible(true);
		if(dialog.isSaved())
			reload();
	}

	private void deleteSelected(){
		final String placeId = selectedPlaceId();
		if(placeId == null)
			return;
		final int confirm = javax.swing.JOptionPane.showConfirmDialog(this,
			"Delete place " + placeId + "?",
			"Confirm Deletion",
			javax.swing.JOptionPane.YES_NO_OPTION,
			javax.swing.JOptionPane.WARNING_MESSAGE);
		if(confirm != javax.swing.JOptionPane.YES_OPTION)
			return;
		context.model().removeRecord(placeId);
		reload();
	}

	private void updateStatus(final int count){
		statusLabel.setText(count + (count == 1? " place": " places"));
	}


	/* ======================================================================
	 *                          Table model
	 * ====================================================================== */

	private static final class PlaceTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {"Name", "Type", "Parents", "Children", "Coordinates"};

		private final List<PlaceHelper.PlaceRow> rows = new ArrayList<>();

		void setRows(final List<PlaceHelper.PlaceRow> rows){
			this.rows.clear();
			this.rows.addAll(rows);
			fireTableDataChanged();
		}

		PlaceHelper.PlaceRow getRow(final int index){
			return rows.get(index);
		}

		@Override public int getRowCount(){ return rows.size(); }
		@Override public int getColumnCount(){ return COLUMNS.length; }
		@Override public String getColumnName(final int column){ return COLUMNS[column]; }

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex){
			final PlaceHelper.PlaceRow row = rows.get(rowIndex);
			return switch(columnIndex){
				case 0 -> row.name();
				case 1 -> row.type();
				case 2 -> row.parents();
				case 3 -> row.children();
				case 4 -> row.coordinates();
				default -> "";
			};
		}
	}

}
