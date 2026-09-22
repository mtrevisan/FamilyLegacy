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
package io.github.mtrevisan.familylegacy.v2.ui.tools.events;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDialogs;
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
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Modal dialog that lists every {@code EventParticipationRecord} with
 * the event, the participant, and the role, and allows the user to
 * create, edit, and delete participations.
 * <p>
 * The dialog is the natural companion of the event editor: from there,
 * the user defines what happened; from here, the user defines who took
 * part and in which capacity.
 */
public final class EventParticipantsDialog extends JDialog{

	private final ToolContext context;
	private final ParticipationTableModel tableModel = new ParticipationTableModel();
	private final JTable table = new JTable(tableModel);
	private final JTextField searchField = new JTextField(24);
	private final JLabel statusLabel = new JLabel(StringUtils.SPACE);


	public EventParticipantsDialog(final ToolContext context){
		super(context.owner(), "Manage Participants", ModalityType.APPLICATION_MODAL);

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

		final JButton newButton = new JButton("New…");
		newButton.addActionListener(e -> openEditor(null));
		toolbar.add(newButton);

		final JButton editButton = new JButton("Edit…");
		editButton.addActionListener(e -> openEditor(selectedParticipationId()));
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
		table.getColumnModel().getColumn(0).setPreferredWidth(140);
		table.getColumnModel().getColumn(1).setPreferredWidth(260);
		table.getColumnModel().getColumn(2).setPreferredWidth(120);
		table.getColumnModel().getColumn(3).setPreferredWidth(160);

		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
					openEditor(selectedParticipationId());
			}
		});

		final JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createTitledBorder("Event participations"));
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


	private void reload(){
		final FLEFModel model = context.model();
		final Map<String, FLEFRecord> eventsById = model.getEventsById();
		final List<ParticipationRow> rows = new ArrayList<>();
		for(final FLEFRecord participation : EventHelper.listAllParticipations(model)){
			final EventHelper.Participation p = EventHelper.toParticipation(participation);
			final FLEFRecord event = (p.eventId() != null? eventsById.get(p.eventId()): null);
			final String eventLabel = (event != null
				? EventHelper.eventType(event) + " [" + event.getId() + "]"
				: (p.eventId() != null? "[missing event] " + p.eventId(): "[no event]"));
			rows.add(new ParticipationRow(
				p.participationId(),
				eventLabel,
				(p.participantType() != null? p.participantType(): StringUtils.EMPTY),
				(p.participantId() != null? p.participantId(): StringUtils.EMPTY),
				(p.role() != null? p.role(): StringUtils.EMPTY)
			));
		}
		tableModel.setRows(rows);
		updateStatus(rows.size());
	}

	private void applyFilter(){
		final String text = searchField.getText();
		@SuppressWarnings("unchecked")
		final TableRowSorter<ParticipationTableModel> sorter =
			(TableRowSorter<ParticipationTableModel>)table.getRowSorter();
		if(text == null || text.isBlank())
			sorter.setRowFilter(null);
		else{
			final String needle = text.trim().toLowerCase(Locale.ROOT);
			sorter.setRowFilter(new RowFilter<>(){
				@Override
				public boolean include(final Entry<? extends ParticipationTableModel, ? extends Integer> entry){
					final ParticipationRow row = tableModel.getRow(entry.getIdentifier());
					return contains(row.eventLabel(), needle)
						|| contains(row.participantType(), needle)
						|| contains(row.participantId(), needle)
						|| contains(row.role(), needle);
				}
			});
		}
		updateStatus(table.getRowCount());
	}

	private static boolean contains(final String haystack, final String needle){
		return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
	}

	private String selectedParticipationId(){
		final int viewRow = table.getSelectedRow();
		if(viewRow < 0)
			return null;
		return tableModel.getRow(table.convertRowIndexToModel(viewRow)).id();
	}

	private void openEditor(final String participationId){
		final EventParticipationHandler handler = EventParticipationHandler.getInstance();
		final BaseRecordDialog dialog;
		if(participationId == null)
			dialog = handler.createNewDialog(this, context.model());
		else{
			final FLEFRecord record = context.model().getRecordById(participationId);
			if(record == null)
				return;
			dialog = handler.createEditDialog(this, context.model(), record);
		}
		dialog.setVisible(true);
		if(dialog.isSaved())
			reload();
	}

	private void deleteSelected(){
		final String participationId = selectedParticipationId();
		if(participationId == null)
			return;
		final int confirm = JOptionPane.showConfirmDialog(this,
			"Delete participation " + participationId + "?",
			"Confirm Deletion",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if(confirm != JOptionPane.YES_OPTION)
			return;
		context.model().removeRecord(participationId);
		reload();
	}

	private void updateStatus(final int count){
		statusLabel.setText(count + (count == 1? " participation": " participations"));
	}


	private record ParticipationRow(String id, String eventLabel, String participantType, String participantId,
		String role){}


	private static final class ParticipationTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {"Event", "Participant", "Type", "Role"};

		private final List<ParticipationRow> rows = new ArrayList<>();

		void setRows(final List<ParticipationRow> rows){
			this.rows.clear();
			this.rows.addAll(rows);
			fireTableDataChanged();
		}

		ParticipationRow getRow(final int index){
			return rows.get(index);
		}

		@Override public int getRowCount(){ return rows.size(); }
		@Override public int getColumnCount(){ return COLUMNS.length; }
		@Override public String getColumnName(final int column){ return COLUMNS[column]; }

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex){
			final ParticipationRow row = rows.get(rowIndex);
			return switch(columnIndex){
				case 0 -> row.eventLabel();
				case 1 -> row.participantId();
				case 2 -> row.participantType();
				case 3 -> row.role();
				default -> "";
			};
		}
	}

}
