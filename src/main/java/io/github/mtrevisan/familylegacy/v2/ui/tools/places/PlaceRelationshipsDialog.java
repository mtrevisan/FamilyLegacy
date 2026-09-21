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
package io.github.mtrevisan.familylegacy.v2.ui.tools.places;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDialogs;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;


/**
 * Modal dialog that lists every {@code PlaceRelationshipRecord} and
 * allows the user to create, edit, and delete relationships.
 * <p>
 * The table is filterable by relation type and by substring on any
 * column. The type filter is populated from the declared types plus any
 * custom type found in the data.
 */
public final class PlaceRelationshipsDialog extends JDialog{

	private static final String FILTER_ALL = "All";

	private final ToolContext context;
	private final RelationshipsTableModel tableModel = new RelationshipsTableModel();
	private final JTable table = new JTable(tableModel);
	private final JTextField searchField = new JTextField(20);
	private final JComboBox<String> typeFilter = new JComboBox<>();
	private final JLabel statusLabel = new JLabel(" ");


	public PlaceRelationshipsDialog(final ToolContext context){
		super(context.owner(), "Place Relationships", ModalityType.APPLICATION_MODAL);

		this.context = context;

		setLayout(new BorderLayout(6, 6));
		add(createToolbar(), BorderLayout.NORTH);
		add(createTable(), BorderLayout.CENTER);
		add(createFooter(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(1000, 560));

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

		toolbar.add(new JLabel("Type:"));
		typeFilter.setPreferredSize(new Dimension(200, typeFilter.getPreferredSize().height));
		typeFilter.addActionListener(e -> applyFilter());
		toolbar.add(typeFilter);

		final JButton newButton = new JButton("New…");
		newButton.addActionListener(e -> openEditor(null));
		toolbar.add(newButton);

		final JButton editButton = new JButton("Edit…");
		editButton.addActionListener(e -> openEditor(selectedRelationshipId()));
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
		table.getColumnModel().getColumn(0).setPreferredWidth(200);
		table.getColumnModel().getColumn(1).setPreferredWidth(200);
		table.getColumnModel().getColumn(2).setPreferredWidth(180);
		table.getColumnModel().getColumn(3).setPreferredWidth(200);

		table.addMouseListener(new java.awt.event.MouseAdapter(){
			@Override
			public void mouseClicked(final java.awt.event.MouseEvent e){
				if(e.getClickCount() == 2 && javax.swing.SwingUtilities.isLeftMouseButton(e))
					openEditor(selectedRelationshipId());
			}
		});

		final JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createTitledBorder("Place Relationships"));
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
	 *                          Data and actions
	 * ====================================================================== */

	private void reload(){
		final FLEFModel model = context.model();
		final Map<String, FLEFRecord> placesById = model.getPlacesById();
		final List<RelationshipRow> rows = new ArrayList<>();
		final java.util.Set<String> types = new TreeSet<>();

		for(final FLEFRecord rel : PlaceHelper.listAllRelationships(model)){
			final String parentId = PlaceHelper.endpointPlaceId(rel, PlaceHelper.TAG_SUBJECT);
			final String childId = PlaceHelper.endpointPlaceId(rel, PlaceHelper.TAG_TARGET);
			final String type = io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper
				.getChildValue(rel, PlaceHelper.TAG_TYPE);
			final String from = PlaceHelper.dateValue(rel, PlaceHelper.TAG_VALID_FROM);
			final String to = PlaceHelper.dateValue(rel, PlaceHelper.TAG_VALID_TO);

			final String parentName = parentId != null && placesById.containsKey(parentId)
				? PlaceHelper.displayName(placesById.get(parentId)): parentId;
			final String childName = childId != null && placesById.containsKey(childId)
				? PlaceHelper.displayName(placesById.get(childId)): childId;

			if(type != null && !type.isBlank())
				types.add(type);

			rows.add(new RelationshipRow(rel.getId(), parentName, childName, type, from, to));
		}

		final String previous = (String)typeFilter.getSelectedItem();
		final List<String> items = new ArrayList<>();
		items.add(FILTER_ALL);
		items.addAll(types);
		typeFilter.setModel(new DefaultComboBoxModel<>(items.toArray(new String[0])));
		if(previous != null && items.contains(previous))
			typeFilter.setSelectedItem(previous);
		else
			typeFilter.setSelectedItem(FILTER_ALL);

		tableModel.setRows(rows);
		updateStatus(rows.size());
	}

	private void applyFilter(){
		final String text = searchField.getText();
		final String type = (String)typeFilter.getSelectedItem();
		final boolean hasTypeFilter = (type != null && !FILTER_ALL.equals(type));

		@SuppressWarnings("unchecked")
		final TableRowSorter<RelationshipsTableModel> sorter =
			(TableRowSorter<RelationshipsTableModel>)table.getRowSorter();

		if((text == null || text.isBlank()) && !hasTypeFilter)
			sorter.setRowFilter(null);
		else{
			final String needle = (text == null? "": text.trim().toLowerCase(java.util.Locale.ROOT));
			sorter.setRowFilter(new RowFilter<>(){
				@Override
				public boolean include(final Entry<? extends RelationshipsTableModel, ? extends Integer> entry){
					final RelationshipRow row = tableModel.getRow(entry.getIdentifier());
					if(hasTypeFilter && !type.equals(row.relationType()))
						return false;
					if(needle.isEmpty())
						return true;
					return contains(row.parentName(), needle)
						|| contains(row.childName(), needle)
						|| contains(row.relationType(), needle);
				}
			});
		}
		updateStatus(table.getRowCount());
	}

	private static boolean contains(final String haystack, final String needle){
		return haystack != null && haystack.toLowerCase(java.util.Locale.ROOT).contains(needle);
	}

	private String selectedRelationshipId(){
		final int viewRow = table.getSelectedRow();
		if(viewRow < 0)
			return null;
		final int modelRow = table.convertRowIndexToModel(viewRow);
		return tableModel.getRow(modelRow).id();
	}

	private void openEditor(final String relationshipId){
		final int option = JOptionPane.showConfirmDialog(this,
			"This dialog does not edit individual relationships inline.\n"
				+ "Open the record editor for the selected relationship?",
			"Edit Relationship",
			JOptionPane.YES_NO_OPTION);
		if(option != JOptionPane.YES_OPTION)
			return;
		final String id = (relationshipId != null? relationshipId: "<new>");
		JOptionPane.showMessageDialog(this,
			"Record editor for relationship " + id + " is not wired yet.",
			"Not Implemented",
			JOptionPane.INFORMATION_MESSAGE);
	}

	private void deleteSelected(){
		final String relationshipId = selectedRelationshipId();
		if(relationshipId == null)
			return;
		final int confirm = JOptionPane.showConfirmDialog(this,
			"Delete place relationship " + relationshipId + "?",
			"Confirm Deletion",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if(confirm != JOptionPane.YES_OPTION)
			return;
		context.model().removeRecord(relationshipId);
		reload();
	}

	private void updateStatus(final int count){
		statusLabel.setText(count + (count == 1? " relationship": " relationships"));
	}


	/* ======================================================================
	 *                          Row and model
	 * ====================================================================== */

	private record RelationshipRow(String id, String parentName, String childName,
											 String relationType, String validFrom, String validTo){}


	private static final class RelationshipsTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {"Parent", "Child", "Type", "Validity"};

		private final List<RelationshipRow> rows = new ArrayList<>();

		void setRows(final List<RelationshipRow> rows){
			this.rows.clear();
			this.rows.addAll(rows);
			fireTableDataChanged();
		}

		RelationshipRow getRow(final int index){
			return rows.get(index);
		}

		@Override public int getRowCount(){ return rows.size(); }
		@Override public int getColumnCount(){ return COLUMNS.length; }
		@Override public String getColumnName(final int column){ return COLUMNS[column]; }

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex){
			final RelationshipRow row = rows.get(rowIndex);
			return switch(columnIndex){
				case 0 -> row.parentName();
				case 1 -> row.childName();
				case 2 -> row.relationType();
				case 3 -> formatValidity(row.validFrom(), row.validTo());
				default -> "";
			};
		}

		private static String formatValidity(final String from, final String to){
			if(from == null && to == null)
				return "";
			return (from != null? from: "?") + " – " + (to != null? to: "?");
		}
	}

}
