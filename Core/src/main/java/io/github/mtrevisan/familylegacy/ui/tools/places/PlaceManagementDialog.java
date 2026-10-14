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
package io.github.mtrevisan.familylegacy.ui.tools.places;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.ui.bindings.Debouncer;
import io.github.mtrevisan.familylegacy.ui.bindings.FilteredComboBox;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.ui.tools.ToolDialogs;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

	/** [ms] */
	private static final int DEBOUNCE_TIME = 400;


	private final ToolContext context;
	private final PlaceTableModel tableModel = new PlaceTableModel();
	private final TableRowSorter<PlaceTableModel> tableSorter = new TableRowSorter<>(tableModel);
	private final JTable table = new JTable(tableModel);
	private final JTextField searchField = new JTextField(24);
	private final JLabel statusLabel = new JLabel(StringUtils.SPACE);

	// Debouncer to delay filtering execution while typing in the search field
	private final Debouncer<String> searchDebouncer = new Debouncer<>(key -> applyFilter(), DEBOUNCE_TIME);


	public PlaceManagementDialog(final ToolContext context){
		super(context.owner(), I18N.t("dialog.place.management.title"), ModalityType.APPLICATION_MODAL);

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

		toolbar.add(new JLabel(I18N.t("dialog.place.management.search.field.label") + ":"));
		toolbar.add(searchField);
		searchField.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void insertUpdate(final DocumentEvent e){
				scheduleSearch();
			}

			@Override
			public void removeUpdate(final DocumentEvent e){
				scheduleSearch();
			}

			@Override
			public void changedUpdate(final DocumentEvent e){
				scheduleSearch();
			}
		});

		final JButton newButton = new JButton(I18N.t("button.new"));
		newButton.addActionListener(e -> openEditor(null));
		toolbar.add(newButton);

		final JButton editButton = new JButton(I18N.t("button.edit"));
		editButton.addActionListener(e -> openEditor(selectedPlace()));
		toolbar.add(editButton);

		final JButton deleteButton = new JButton(I18N.t("button.delete"));
		deleteButton.addActionListener(e -> deleteSelected());
		toolbar.add(deleteButton);

		return toolbar;
	}

	private void scheduleSearch(){
		searchDebouncer.call(FilteredComboBox.PROPERTY_DEBOUNCER);
	}

	private JScrollPane createTable(){
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(22);
		table.setRowSorter(tableSorter);
		tableSorter.setComparator(0, String.CASE_INSENSITIVE_ORDER);
		tableSorter.setComparator(1, String.CASE_INSENSITIVE_ORDER);
		tableSorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
		final TableColumnModel columnModel = table.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(220);
		columnModel.getColumn(1).setPreferredWidth(120);
		columnModel.getColumn(2).setPreferredWidth(180);
		columnModel.getColumn(3).setPreferredWidth(180);
		columnModel.getColumn(4).setPreferredWidth(150);

		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
					openEditor(selectedPlace());
			}
		});

		final JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createTitledBorder(I18N.t("dialog.place.management.search.result.label")));
		return scroll;
	}

	private JPanel createFooter(){
		final JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		footer.setBorder(BorderFactory.createEmptyBorder(2, 6, 6, 6));
		footer.add(statusLabel);
		final JButton close = new JButton(I18N.t("button.close"));
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
		if(text == null || text.isBlank())
			tableSorter.setRowFilter(null);
		else{
			final String needle = text.trim().toLowerCase(Locale.ROOT);
			tableSorter.setRowFilter(new RowFilter<>(){
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
		return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
	}

	private FLEFRecord selectedPlace(){
		final int viewRow = table.getSelectedRow();
		if(viewRow < 0)
			return null;

		final int modelRow = table.convertRowIndexToModel(viewRow);
		final String rowId = tableModel.getRow(modelRow)
			.id();
		final FLEFModel model = context.model();
		return model.getRecordById(rowId);
	}

	private void openEditor(final FLEFRecord place){
		final Window owner = this;
		final PlaceHandler handler = PlaceHandler.getInstance();
		final BaseRecordDialog dialog;
		final FLEFModel model = context.model();
		if(place == null)
			dialog = handler.createNewDialog(owner, model);
		else
			dialog = handler.createEditDialog(owner, model, place);
		dialog.setVisible(true);

		if(dialog.isSaved())
			reload();
	}

	private void deleteSelected(){
		final FLEFRecord place = selectedPlace();
		if(place == null)
			return;

		final int confirm = JOptionPane.showConfirmDialog(this,
			I18N.tf("confirmation.delete.place.message", PlaceReader.extractPrimaryName(place)),
			I18N.t("confirmation.delete.place.title"),
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if(confirm != JOptionPane.YES_OPTION)
			return;

		final FLEFModel model = context.model();
		model.removeRecord(place.getId());

		reload();
	}

	private void updateStatus(final int count){
		statusLabel.setText(I18N.tf("search.record.found", count));
	}


	/* ======================================================================
	 *                          Table model
	 * ====================================================================== */

	private static final class PlaceTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {
			I18N.t("dialog.place.management.column.name"),
			I18N.t("dialog.place.management.column.type"),
			I18N.t("dialog.place.management.column.parents"),
			I18N.t("dialog.place.management.column.children"),
			I18N.t("dialog.place.management.column.coordinates")
		};

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
				default -> StringUtils.EMPTY;
			};
		}
	}

}
