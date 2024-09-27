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

import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.persistence.db.GraphDatabaseManager;
import io.github.mtrevisan.familylegacy.flef.persistence.repositories.Repository;
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.FilterString;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.StringHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TextPreviewPane;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.naming.OperationNotSupportedException;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.EventQueue;
import java.awt.Frame;
import java.io.Serial;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCreationDate;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordDescription;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordIdentifier;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPriority;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordStatus;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCreationDate;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordDescription;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordIdentifier;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordPriority;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordStatus;


public final class ResearchStatusDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = 6258734190218776466L;

	private static final int TABLE_INDEX_IDENTIFIER = 2;


	private final JLabel identifierLabel = new JLabel("Identifier:");
	private final JTextField identifierField = new JTextField();
	private final JLabel descriptionLabel = new JLabel("Description:");
	private final TextPreviewPane descriptionTextPreview = TextPreviewPane.createWithoutPreview();
	private final JLabel statusLabel = new JLabel("Type:");
	private final JComboBox<String> statusComboBox = new JComboBox<>(new String[]{null, "open", "active", "ended"});
	private final JLabel priorityLabel = new JLabel("Priority:");
	private final JTextField priorityField = new JTextField();


	public static ResearchStatusDialog create(final Frame parent){
		final ResearchStatusDialog dialog = new ResearchStatusDialog(parent);
		dialog.showRecordResearchStatus = false;
		dialog.initialize();
		return dialog;
	}

	public static ResearchStatusDialog createSelectOnly(final Frame parent){
		final ResearchStatusDialog dialog = new ResearchStatusDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.showRecordResearchStatus = false;
		dialog.initialize();
		return dialog;
	}

	public static ResearchStatusDialog createShowOnly(final Frame parent){
		final ResearchStatusDialog dialog = new ResearchStatusDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.showRecordOnly = true;
		dialog.showRecordResearchStatus = false;
		dialog.initialize();
		return dialog;
	}

	public static ResearchStatusDialog createEditOnly(final Frame parent){
		final ResearchStatusDialog dialog = new ResearchStatusDialog(parent);
		dialog.showRecordOnly = true;
		dialog.showRecordResearchStatus = false;
		dialog.initialize();
		return dialog;
	}


	private ResearchStatusDialog(final Frame parent){
		super(parent);
	}


	public ResearchStatusDialog withOnCloseGracefully(final BiConsumer<Map<String, Object>, Integer> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	@Override
	protected String getTableName(){
		return EntityManager.NODE_RESEARCH_STATUS;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Identifier"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.LEFT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		final Comparator<Integer> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, textComparator};
	}

	@Override
	protected void initStoreComponents(){
		setTitle(StringUtils.capitalize(StringHelper.pluralize(getTableName())));

		super.initStoreComponents();
	}

	@Override
	protected void initRecordComponents(){
		((AbstractDocument)priorityField.getDocument()).setDocumentFilter(new PositiveIntegerFilter());


		GUIHelper.bindLabelUndo(identifierLabel, identifierField);
		addMandatoryField(identifierField);

		GUIHelper.bindLabel(descriptionLabel, descriptionTextPreview);
		descriptionTextPreview.setTextViewFont(identifierField.getFont());
		descriptionTextPreview.setMinimumSize(MINIMUM_NOTE_TEXT_PREVIEW_SIZE);

		GUIHelper.bindLabelUndoAutoComplete(statusLabel, statusComboBox);

		GUIHelper.bindLabelUndo(priorityLabel, priorityField);
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
		unselectAction();

		final List<Map<String, Object>> records = Repository.findAll(EntityManager.NODE_RESEARCH_STATUS);

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map<String, Object> record : records){
			final Integer recordID = extractRecordID(record);
			final String identifier = extractRecordIdentifier(record);
			final FilterString filter = FilterString.create()
				.add(recordID)
				.add(identifier);
			final String filterData = filter.toString();

			model.setValueAt(recordID, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(identifier, row, TABLE_INDEX_IDENTIFIER);

			row ++;
		}
	}

	@Override
	protected void requestFocusAfterSelect(){
		//set focus on first field
		identifierField.requestFocusInWindow();
	}

	@Override
	protected void fillData(){
		final String identifier = extractRecordIdentifier(selectedRecord);
		final String description = extractRecordDescription(selectedRecord);
		final String status = extractRecordStatus(selectedRecord);
		final Integer priority = extractRecordPriority(selectedRecord);

		identifierField.setText(identifier);
		descriptionTextPreview.setText("Research status " + extractRecordID(selectedRecord), description, null);
		statusComboBox.setSelectedItem(status);
		priorityField.setText(priority != null? String.valueOf(priority): null);
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
		final String now = EntityManager.now();
		final String identifier = GUIHelper.getTextTrimmed(identifierField);
		final String description = descriptionTextPreview.getTextTrimmed();
		final String status = GUIHelper.getTextTrimmed(statusComboBox);
		final String priorityAsString = GUIHelper.getTextTrimmed(priorityField);
		final Integer priority = (priorityAsString != null
			? Integer.valueOf(priorityAsString)
			: null);

		//update table:
		if(!Objects.equals(identifier, extractRecordIdentifier(selectedRecord))){
			final DefaultTableModel model = getRecordTableModel();
			final Integer recordID = extractRecordID(selectedRecord);
			for(int row = 0, length = model.getRowCount(); row < length; row ++)
				if(model.getValueAt(row, TABLE_INDEX_ID).equals(recordID)){
					final int viewRowIndex = recordTable.convertRowIndexToView(row);
					final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

					model.setValueAt(identifier, modelRowIndex, TABLE_INDEX_IDENTIFIER);

					break;
				}
		}

		insertRecordIdentifier(selectedRecord, identifier);
		insertRecordDescription(selectedRecord, description);
		insertRecordStatus(selectedRecord, status);
		insertRecordPriority(selectedRecord, priority);
		if(extractRecordCreationDate(selectedRecord) == null)
			insertRecordCreationDate(selectedRecord, now);

		return true;
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


		GraphDatabaseManager.clearDatabase();

		final Map<String, Object> date1 = new HashMap<>();
		date1.put("date", "18 OCT 2000");
		int date1ID = Repository.upsert(date1, EntityManager.NODE_HISTORIC_DATE);

		final Map<String, Object> researchStatus1 = new HashMap<>();
		researchStatus1.put("identifier", "research 1");
		researchStatus1.put("description", "see people, do things");
		researchStatus1.put("status", "open");
		researchStatus1.put("priority", 2);
		researchStatus1.put("creation_date", EntityManager.now());
		int researchStatus1ID = Repository.upsert(researchStatus1, EntityManager.NODE_RESEARCH_STATUS);
		Repository.upsertRelationship(EntityManager.NODE_RESEARCH_STATUS, researchStatus1ID,
			EntityManager.NODE_HISTORIC_DATE, date1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public static void refresh(final EditEvent editCommand) throws OperationNotSupportedException{}
			};
			EventBusService.subscribe(listener);

			final ResearchStatusDialog dialog = create(parent);
//			final ResearchStatusDialog dialog = createRecordOnly(parent);
			dialog.loadData();
			if(!dialog.selectData(extractRecordID(researchStatus1)))
				dialog.showNewRecord();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.out.println(Repository.logDatabase());

					System.exit(0);
				}
			});
			dialog.showDialog();
		});
	}

}
