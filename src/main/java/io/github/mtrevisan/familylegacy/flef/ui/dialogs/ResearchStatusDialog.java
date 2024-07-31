/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.dialogs;

import io.github.mtrevisan.familylegacy.flef.db.DatabaseManager;
import io.github.mtrevisan.familylegacy.flef.db.DatabaseManagerInterface;
import io.github.mtrevisan.familylegacy.flef.helpers.DependencyInjector;
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.StringHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TableHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TextPreviewPane;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.naming.OperationNotSupportedException;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.EventQueue;
import java.awt.Frame;
import java.io.IOException;
import java.io.Serial;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;


public final class ResearchStatusDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = 6258734190218776466L;

	private static final int TABLE_INDEX_RECORD_IDENTIFIER = 1;

	private static final String TABLE_NAME = "research_status";


	private JLabel identifierLabel;
	private JTextField identifierField;
	private JLabel descriptionLabel;
	private TextPreviewPane descriptionTextPreview;
	private JLabel statusLabel;
	private JComboBox<String> statusComboBox;
	private JLabel priorityLabel;
	private JTextField priorityField;


	public static ResearchStatusDialog create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		return new ResearchStatusDialog(store, parent);
	}


	private ResearchStatusDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public ResearchStatusDialog withOnCloseGracefully(final Consumer<Map<String, Object>> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	@Override
	protected String getTableName(){
		return TABLE_NAME;
	}

	@Override
	protected DefaultTableModel getDefaultTableModel(){
		return new RecordTableModel();
	}

	@Override
	protected void initStoreComponents(){
		setTitle(StringUtils.capitalize(StringHelper.pluralize(getTableName())));

		super.initStoreComponents();


		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_IDENTIFIER, Comparator.naturalOrder());
	}

	@Override
	protected void initRecordComponents(){
		identifierLabel = new JLabel("Identifier:");
		identifierField = new JTextField();

		descriptionLabel = new JLabel("Description:");
		descriptionTextPreview = TextPreviewPane.createWithoutPreview();
		descriptionTextPreview.setTextViewFont(identifierField.getFont());

		statusLabel = new JLabel("Type:");
		statusComboBox = new JComboBox<>(new String[]{"open", "active", "ended"});

		priorityLabel = new JLabel("Priority:");
		priorityField = new JTextField();
		((AbstractDocument)priorityField.getDocument()).setDocumentFilter(new PositiveIntegerFilter());


		GUIHelper.bindLabelTextChangeUndo(identifierLabel, identifierField, this::saveData);
		addMandatoryField(identifierField);

		GUIHelper.bindLabelTextChange(descriptionLabel, descriptionTextPreview, this::saveData);

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(statusLabel, statusComboBox, this::saveData);

		GUIHelper.bindLabelTextChangeUndo(priorityLabel, priorityField, this::saveData);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(identifierLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(identifierField, "grow,wrap paragraph");
		recordPanelBase.add(descriptionLabel, "align label,top,sizegroup lbl,split 2");
		recordPanelBase.add(descriptionTextPreview, "grow,wrap paragraph");
		recordPanelBase.add(statusLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(statusComboBox, "grow,wrap paragraph");
		recordPanelBase.add(priorityLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(priorityField, "grow");

		recordTabbedPane.add("base", recordPanelBase);
	}

	@Override
	public void loadData(){
		final Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME);

		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final Map<String, Object> container = record.getValue();

			final String identifier = extractRecordIdentifier(container);

			model.setValueAt(key, row, TABLE_INDEX_RECORD_ID);
			model.setValueAt(identifier, row, TABLE_INDEX_RECORD_IDENTIFIER);

			row ++;
		}
	}

	@Override
	protected void filterTableBy(final JDialog panel){
		final String title = GUIHelper.getTextTrimmed(filterField);
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RECORD_ID,
			TABLE_INDEX_RECORD_IDENTIFIER);

		@SuppressWarnings("unchecked")
		final TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)recordTable.getRowSorter();
		sorter.setRowFilter(filter);
	}

	@Override
	protected void fillData(){
		final Integer referenceID = extractRecordReferenceID(selectedRecord);
		final String identifier = extractRecordIdentifier(selectedRecord);
		final String description = extractRecordDescription(selectedRecord);
		final String status = extractRecordStatus(selectedRecord);
		final Integer priority = extractRecordPriority(selectedRecord);

		identifierField.setText(identifier);
		descriptionTextPreview.setText("Research status " + extractRecordID(selectedRecord), description, null);
		statusComboBox.setSelectedItem(status);
		priorityField.setText(String.valueOf(priority));
	}

	@Override
	protected void clearData(){
		identifierField.setText(null);
		descriptionTextPreview.clear();
		statusComboBox.setSelectedItem(null);
		priorityField.setText(null);
	}

	@Override
	protected boolean validateData(){
		if(selectedRecord != null){
			final String identifier = extractRecordIdentifier(selectedRecord);
			if(!validData(identifier)){
				JOptionPane.showMessageDialog(getParent(), "Identifier field is required", "Error",
					JOptionPane.ERROR_MESSAGE);
				identifierField.requestFocusInWindow();

				return false;
			}
		}
		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null)
			return false;

		//read record panel:
		final String identifier = GUIHelper.getTextTrimmed(identifierField);
		final String description = descriptionTextPreview.getTextTrimmed();
		final String status = GUIHelper.getTextTrimmed(statusComboBox);
		final String priorityAsString = GUIHelper.getTextTrimmed(priorityField);
		final Integer priority = (priorityAsString != null
			? Integer.valueOf(priorityAsString)
			: null);

		//update table:
		if(!Objects.equals(identifier, extractRecordIdentifier(selectedRecord))){
			final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
			final Integer recordID = extractRecordID(selectedRecord);
			for(int row = 0, length = model.getRowCount(); row < length; row ++)
				if(model.getValueAt(row, TABLE_INDEX_RECORD_ID).equals(recordID)){
					final int viewRowIndex = recordTable.convertRowIndexToView(row);
					final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

					model.setValueAt(identifier, modelRowIndex, TABLE_INDEX_RECORD_IDENTIFIER);

					break;
				}
		}

		selectedRecord.put("identifier", identifier);
		selectedRecord.put("description", description);
		selectedRecord.put("status", status);
		selectedRecord.put("priority", priority);

		return true;
	}


	private static String extractRecordIdentifier(final Map<String, Object> record){
		return (String)record.get("identifier");
	}

	private static String extractRecordDescription(final Map<String, Object> record){
		return (String)record.get("description");
	}

	private static String extractRecordStatus(final Map<String, Object> record){
		return (String)record.get("status");
	}

	private static Integer extractRecordPriority(final Map<String, Object> record){
		return (Integer)record.get("priority");
	}


	private static class RecordTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = -4786221445323184600L;


		RecordTableModel(){
			super(new String[]{"ID", "Identifier"}, 0);
		}

		@Override
		public final Class<?> getColumnClass(final int column){
			return String.class;
		}

		@Override
		public final boolean isCellEditable(final int row, final int column){
			return false;
		}
	}

	private static class PositiveIntegerFilter extends DocumentFilter{
		@Override
		public final void insertString(final FilterBypass fb, final int offset, final String text, final AttributeSet attr)
				throws BadLocationException{
			if(text != null && isValidInput(text))
				super.insertString(fb, offset, text, attr);
		}

		@Override
		public final void replace(final FilterBypass fb, final int offset, final int length, final String text, final AttributeSet attr)
				throws BadLocationException{
			if(text != null && isValidInput(text))
				super.replace(fb, offset, length, text, attr);
		}

		@Override
		public final void remove(final FilterBypass fb, final int offset, final int length) throws BadLocationException{
			super.remove(fb, offset, length);
		}

		private static boolean isValidInput(final String text){
			try{
				return (Integer.parseInt(text) >= 0);
			}
			catch(final NumberFormatException ignored){
				return false;
			}
		}
	}


	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> researchStatuses = new TreeMap<>();
		store.put(TABLE_NAME, researchStatuses);
		final Map<String, Object> researchStatus = new HashMap<>();
		researchStatus.put("id", 1);
		researchStatus.put("reference_table", "date");
		researchStatus.put("reference_id", 1);
		researchStatus.put("identifier", "research 1");
		researchStatus.put("description", "see people, do things");
		researchStatus.put("status", "open");
		researchStatus.put("priority", 2);
		researchStatuses.put((Integer)researchStatus.get("id"), researchStatus);

		final TreeMap<Integer, Map<String, Object>> dates = new TreeMap<>();
		store.put("historic_date", dates);
		final Map<String, Object> date1 = new HashMap<>();
		date1.put("id", 1);
		date1.put("date", "18 OCT 2000");
		dates.put((Integer)date1.get("id"), date1);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public static void refresh(final EditEvent editCommand) throws OperationNotSupportedException{
					switch(editCommand.getType()){
						case REFERENCE -> {
							throw new OperationNotSupportedException();
							//TODO single reference
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			final DependencyInjector injector = new DependencyInjector();
			final DatabaseManager dbManager = new DatabaseManager("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
			try{
				final String grammarFile = "src/main/resources/gedg/treebard/FLeF.sql";
				dbManager.initialize(grammarFile);

				dbManager.insertDatabase(store);
			}
			catch(final SQLException | IOException e){
				throw new RuntimeException(e);
			}
			injector.register(DatabaseManagerInterface.class, dbManager);

			final ResearchStatusDialog dialog = create(store, parent);
			injector.injectDependencies(dialog);
			dialog.initComponents();
			dialog.loadData();
			if(!dialog.selectData(extractRecordID(researchStatus)))
				dialog.showNewRecord();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.out.println(store);
					System.exit(0);
				}
			});
			dialog.setLocationRelativeTo(null);
			dialog.addComponentListener(new java.awt.event.ComponentAdapter() {
				@Override
				public void componentResized(final java.awt.event.ComponentEvent e) {
					System.out.println("Resized to " + e.getComponent().getSize());
				}
			});
			dialog.setVisible(true);
		});
	}

}
