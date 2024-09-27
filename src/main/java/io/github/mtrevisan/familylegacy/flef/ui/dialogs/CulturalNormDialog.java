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

import io.github.mtrevisan.familylegacy.flef.helpers.FileHelper;
import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.persistence.db.GraphDatabaseManager;
import io.github.mtrevisan.familylegacy.flef.persistence.repositories.Repository;
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.CertaintyComboBoxModel;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.CredibilityComboBoxModel;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.FilterString;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.StringHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TextPreviewListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TextPreviewPane;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JCheckBox;
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

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCertainty;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCredibility;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordDescription;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordIdentifier;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCertainty;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCredibility;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordDescription;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordIdentifier;


public final class CulturalNormDialog extends CommonListDialog implements TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = -3961030253095528462L;

	private static final int TABLE_INDEX_IDENTIFIER = 2;


	private final JLabel identifierLabel = new JLabel("Identifier:");
	private final JTextField identifierField = new JTextField();
	private final JLabel descriptionLabel = new JLabel("Description:");
	private final TextPreviewPane descriptionTextPreview = TextPreviewPane.createWithPreview(CulturalNormDialog.this);
	private final JButton placeButton = new JButton("Place", ICON_PLACE);
	private final JButton dateStartButton = new JButton("Date start", ICON_CALENDAR);
	private final JButton dateEndButton = new JButton("Date end", ICON_CALENDAR);

	private final JLabel certaintyLabel = new JLabel("Certainty:");
	private final JComboBox<String> certaintyComboBox = new JComboBox<>(new CertaintyComboBoxModel());
	private final JLabel credibilityLabel = new JLabel("Credibility:");
	private final JComboBox<String> credibilityComboBox = new JComboBox<>(new CredibilityComboBoxModel());

	private final JButton noteButton = new JButton("Notes", ICON_NOTE);
	private final JButton mediaButton = new JButton("Media", ICON_MEDIA);
	private final JButton assertionButton = new JButton("Assertions", ICON_ASSERTION);
	private final JButton eventButton = new JButton("Events", ICON_EVENT);
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	private final JLabel linkCertaintyLabel = new JLabel("Certainty:");
	private final JComboBox<String> linkCertaintyComboBox = new JComboBox<>(new CertaintyComboBoxModel());
	private final JLabel linkCredibilityLabel = new JLabel("Credibility:");
	private final JComboBox<String> linkCredibilityComboBox = new JComboBox<>(new CredibilityComboBoxModel());

	private String filterReferenceTable;
	private int filterReferenceID;


	public static CulturalNormDialog create(final Frame parent){
		final CulturalNormDialog dialog = new CulturalNormDialog(parent);
		dialog.initialize();
		return dialog;
	}

	public static CulturalNormDialog createSelectOnly(final Frame parent){
		final CulturalNormDialog dialog = new CulturalNormDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.hideUnselectButton = true;
		dialog.addViewOnlyComponents(dialog.placeButton, dialog.dateStartButton, dialog.dateEndButton, dialog.noteButton, dialog.mediaButton,
			dialog.assertionButton, dialog.eventButton);
		dialog.initialize();
		return dialog;
	}

	public static CulturalNormDialog createShowOnly(final Frame parent){
		final CulturalNormDialog dialog = new CulturalNormDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}

	public static CulturalNormDialog createEditOnly(final Frame parent){
		final CulturalNormDialog dialog = new CulturalNormDialog(parent);
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}


	private CulturalNormDialog(final Frame parent){
		super(parent);
	}


	public CulturalNormDialog withOnCloseGracefully(final BiConsumer<Map<String, Object>, Integer> onCloseGracefully){
		BiConsumer<Map<String, Object>, Integer> innerOnCloseGracefully = (record, recordID) -> {
			if(selectedRecord != null)
				Repository.upsertRelationship(EntityManager.NODE_CULTURAL_NORM, recordID,
					filterReferenceTable, filterReferenceID,
					EntityManager.RELATIONSHIP_SUPPORTED_BY, new HashMap<>(selectedRecordLink),
					GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
			else if(selectedRecordID != null)
				Repository.deleteRelationship(EntityManager.NODE_CULTURAL_NORM, recordID,
					filterReferenceTable, filterReferenceID);
		};
		if(onCloseGracefully != null)
			innerOnCloseGracefully = innerOnCloseGracefully.andThen(onCloseGracefully);

		setOnCloseGracefully(innerOnCloseGracefully);

		return this;
	}

	public CulturalNormDialog withReference(final String referenceTable, final int referenceID){
		filterReferenceTable = referenceTable;
		filterReferenceID = referenceID;

		return this;
	}

	@Override
	protected String getTableName(){
		return EntityManager.NODE_CULTURAL_NORM;
	}

	@Override
	protected String getJunctionTableName(){
		return EntityManager.RELATIONSHIP_SUPPORTED_BY;
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
		GUIHelper.bindLabelUndo(identifierLabel, identifierField);
		addMandatoryField(identifierField);
		descriptionTextPreview.setTextViewFont(identifierField.getFont());
		descriptionTextPreview.setMinimumSize(MINIMUM_NOTE_TEXT_PREVIEW_SIZE);

		GUIHelper.bindLabel(descriptionLabel, descriptionTextPreview);

		placeButton.setToolTipText("Place");
		placeButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PLACE, EntityManager.NODE_CULTURAL_NORM, selectedRecord)));

		dateStartButton.setToolTipText("Start date");
		dateStartButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.HISTORIC_DATE_START, EntityManager.NODE_CULTURAL_NORM, selectedRecord)));

		dateEndButton.setToolTipText("End date");
		dateEndButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.HISTORIC_DATE_END, EntityManager.NODE_CULTURAL_NORM, selectedRecord)));

		GUIHelper.bindLabelUndoAutoComplete(certaintyLabel, certaintyComboBox);
		GUIHelper.bindLabelUndoAutoComplete(credibilityLabel, credibilityComboBox);


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, EntityManager.NODE_CULTURAL_NORM, selectedRecord)));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, EntityManager.NODE_CULTURAL_NORM, selectedRecord)));

		assertionButton.setToolTipText("Assertions");
		assertionButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.ASSERTION, EntityManager.NODE_CULTURAL_NORM, selectedRecord)));

		eventButton.setToolTipText("Events");
		eventButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.EVENT, EntityManager.NODE_CULTURAL_NORM, selectedRecord)));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);


		GUIHelper.bindLabelUndoAutoComplete(linkCertaintyLabel, linkCertaintyComboBox);
		GUIHelper.bindLabelUndoAutoComplete(linkCredibilityLabel, linkCredibilityComboBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(identifierLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(identifierField, "grow,wrap paragraph");
		recordPanelBase.add(descriptionLabel, "align label,top,sizegroup lbl,split 2");
		recordPanelBase.add(descriptionTextPreview, "grow,wrap paragraph");
		recordPanelBase.add(placeButton, "sizegroup btn,center,wrap paragraph");
		recordPanelBase.add(dateStartButton, "sizegroup btn,center,split 2");
		recordPanelBase.add(dateEndButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelBase.add(certaintyLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(certaintyComboBox, "wrap related");
		recordPanelBase.add(credibilityLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(credibilityComboBox);

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,split 2");
		recordPanelOther.add(mediaButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(assertionButton, "sizegroup btn,center,split 2");
		recordPanelOther.add(eventButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		final JPanel recordPanelLink = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelLink.add(linkCertaintyLabel, "align label,sizegroup lbl,split 2");
		recordPanelLink.add(linkCertaintyComboBox, "wrap related");
		recordPanelLink.add(linkCredibilityLabel, "align label,sizegroup lbl,split 2");
		recordPanelLink.add(linkCredibilityComboBox);

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
		recordTabbedPane.add("link", recordPanelLink);
	}

	@Override
	public void loadData(){
		unselectAction();

		final List<Map<String, Object>> records = Repository.findAll(EntityManager.NODE_CULTURAL_NORM);

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
		final Integer culturalNormID = extractRecordID(selectedRecord);
		final String identifier = extractRecordIdentifier(selectedRecord);
		final String description = extractRecordDescription(selectedRecord);
		final boolean hasPlace = Repository.hasPlace(EntityManager.NODE_CULTURAL_NORM, culturalNormID);
		final boolean hasDateStart = Repository.hasDateStart(EntityManager.NODE_CULTURAL_NORM, culturalNormID);
		final boolean hasDateEnd = Repository.hasDateEnd(EntityManager.NODE_CULTURAL_NORM, culturalNormID);
		final String certainty = extractRecordCertainty(selectedRecord);
		final String credibility = extractRecordCredibility(selectedRecord);
		final boolean hasNotes = Repository.hasNotes(EntityManager.NODE_CULTURAL_NORM, culturalNormID);
		final boolean hasMedia = Repository.hasMedia(EntityManager.NODE_CULTURAL_NORM, culturalNormID);
		final boolean hasAssertions = Repository.hasAssertions(EntityManager.NODE_CULTURAL_NORM, culturalNormID);
		final boolean hasEvents = Repository.hasEvents(EntityManager.NODE_CULTURAL_NORM, culturalNormID);
		final String restriction = Repository.getRestriction(EntityManager.NODE_CULTURAL_NORM, culturalNormID);

		identifierField.setText(identifier);
		descriptionTextPreview.setText("Cultural norm " + culturalNormID, description, null);
		setButtonEnableAndBorder(placeButton, hasPlace);
		setButtonEnableAndBorder(dateStartButton, hasDateStart);
		setButtonEnableAndBorder(dateEndButton, hasDateEnd);
		certaintyComboBox.setSelectedItem(certainty);
		credibilityComboBox.setSelectedItem(credibility);

		setButtonEnableAndBorder(noteButton, hasNotes);
		setButtonEnableAndBorder(mediaButton, hasMedia);
		setButtonEnableAndBorder(assertionButton, hasAssertions);
		setButtonEnableAndBorder(eventButton, hasEvents);
		setCheckBoxEnableAndBorder(restrictionCheckBox, EntityManager.RESTRICTION_CONFIDENTIAL.equals(restriction));

		linkCertaintyComboBox.setSelectedItem(null);
		linkCredibilityComboBox.setSelectedItem(null);
		if(filterReferenceTable != null){
			final List<Map<String, Object>> recordCulturalNormRelationships = Repository.findRelationships(
				EntityManager.NODE_CULTURAL_NORM, culturalNormID,
				filterReferenceTable, filterReferenceID,
				EntityManager.RELATIONSHIP_SUPPORTED_BY);
			if(recordCulturalNormRelationships.size() > 1)
				throw new IllegalArgumentException("Data integrity error");

			if(!recordCulturalNormRelationships.isEmpty()){
				selectedRecordLink = recordCulturalNormRelationships.getFirst();

				final String linkCertainty = extractRecordCertainty(selectedRecordLink);
				final String linkCredibility = extractRecordCredibility(selectedRecordLink);

				linkCertaintyComboBox.setSelectedItem(linkCertainty);
				linkCredibilityComboBox.setSelectedItem(linkCredibility);
			}
		}

		GUIHelper.enableTabByTitle(recordTabbedPane, "link", (showRecordOnly || filterReferenceTable != null && selectedRecord != null));
	}

	@Override
	protected void clearData(){
		identifierField.setText(null);
		descriptionTextPreview.clear();
		GUIHelper.setDefaultBorder(placeButton);
		GUIHelper.setDefaultBorder(dateStartButton);
		GUIHelper.setDefaultBorder(dateEndButton);
		certaintyComboBox.setSelectedItem(null);
		credibilityComboBox.setSelectedItem(null);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(mediaButton);
		GUIHelper.setDefaultBorder(eventButton);
		GUIHelper.setDefaultBorder(assertionButton);
		restrictionCheckBox.setSelected(false);

		linkCertaintyComboBox.setSelectedItem(null);
		linkCredibilityComboBox.setSelectedItem(null);
	}

	@Override
	protected boolean validateData(){
		final String identifier = GUIHelper.getTextTrimmed(identifierField);
		if(!validData(identifier)){
			JOptionPane.showMessageDialog(getParent(), "Identifier field is required", "Error",
				JOptionPane.ERROR_MESSAGE);
			identifierField.requestFocusInWindow();

			return false;
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
		final String certainty = GUIHelper.getTextTrimmed(certaintyComboBox);
		final String credibility = GUIHelper.getTextTrimmed(credibilityComboBox);

		final Integer recordID = extractRecordID(selectedRecord);
		if(filterReferenceTable != null){
			//read link panel:
			final String linkCertainty = GUIHelper.getTextTrimmed(linkCertaintyComboBox);
			final String linkCredibility = GUIHelper.getTextTrimmed(linkCredibilityComboBox);

			if(selectedRecordLink == null)
				selectedRecordLink = new HashMap<>(2);
			insertRecordCertainty(selectedRecordLink, linkCertainty);
			insertRecordCredibility(selectedRecordLink, linkCredibility);
			Repository.upsertRelationship(EntityManager.NODE_CULTURAL_NORM, recordID,
				filterReferenceTable, filterReferenceID,
				EntityManager.RELATIONSHIP_FOR, selectedRecordLink, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		}

		//update table:
		if(!Objects.equals(identifier, extractRecordIdentifier(selectedRecord))){
			final DefaultTableModel model = getRecordTableModel();
			for(int row = 0, length = model.getRowCount(); row < length; row ++){
				final int viewRowIndex = recordTable.convertRowIndexToView(row);
				final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

				if(model.getValueAt(modelRowIndex, TABLE_INDEX_ID).equals(recordID)){
					model.setValueAt(identifier, modelRowIndex, TABLE_INDEX_IDENTIFIER);

					break;
				}
			}
		}

		insertRecordIdentifier(selectedRecord, identifier);
		insertRecordDescription(selectedRecord, description);
		insertRecordCertainty(selectedRecord, certainty);
		insertRecordCredibility(selectedRecord, credibility);

		return true;
	}

	@Override
	public void onPreviewStateChange(final boolean visible){
		TextPreviewListenerInterface.centerDivider(this, visible);
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();

		final Map<String, Object> culturalNorm1 = new HashMap<>();
		culturalNorm1.put("identifier", "rule 1 id");
		culturalNorm1.put("description", "rule 1");
		culturalNorm1.put("certainty", "certain");
		culturalNorm1.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		int culturalNorm1ID = Repository.upsert(culturalNorm1, EntityManager.NODE_CULTURAL_NORM);

		final Map<String, Object> assertion1 = new HashMap<>();
		assertion1.put("role", "father");
		assertion1.put("certainty", "certain");
		assertion1.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		int assertion1ID = Repository.upsert(assertion1, EntityManager.NODE_ASSERTION);

		final Map<String, Object> culturalNormRelationship1 = new HashMap<>();
		culturalNormRelationship1.put("certainty", "probable");
		culturalNormRelationship1.put("credibility", "probable");
		Repository.upsertRelationship(EntityManager.NODE_CULTURAL_NORM, culturalNorm1ID,
			EntityManager.NODE_ASSERTION, assertion1ID,
			EntityManager.RELATIONSHIP_SUPPORTED_BY, culturalNormRelationship1,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> place1 = new HashMap<>();
		place1.put("identifier", "place 1");
		place1.put("name", "name of the place");
		place1.put("locale", "en-US");
		int place1ID = Repository.upsert(place1, EntityManager.NODE_PLACE);
		Repository.upsertRelationship(EntityManager.NODE_CULTURAL_NORM, culturalNorm1ID,
			EntityManager.NODE_PLACE, place1ID,
			EntityManager.RELATIONSHIP_APPLIES_IN, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> date1 = new HashMap<>();
		date1.put("date", "18 OCT 2000");
		int date1ID = Repository.upsert(date1, EntityManager.NODE_HISTORIC_DATE);
		final Map<String, Object> date2 = new HashMap<>();
		date2.put("date", "18 OCT 2010");
		int date2ID = Repository.upsert(date2, EntityManager.NODE_HISTORIC_DATE);
		Repository.upsertRelationship(EntityManager.NODE_CULTURAL_NORM, culturalNorm1ID,
			EntityManager.NODE_HISTORIC_DATE, date1ID,
			EntityManager.RELATIONSHIP_STARTED_ON, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		Repository.upsertRelationship(EntityManager.NODE_CULTURAL_NORM, culturalNorm1ID,
			EntityManager.NODE_HISTORIC_DATE, date2ID,
			EntityManager.RELATIONSHIP_ENDED_ON, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> note1 = new HashMap<>();
		note1.put("note", "note 1");
		int note1ID = Repository.upsert(note1, EntityManager.NODE_NOTE);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note1ID,
			EntityManager.NODE_CULTURAL_NORM, culturalNorm1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("restriction", "confidential");
		int restriction1ID = Repository.upsert(restriction1, EntityManager.NODE_RESTRICTION);
		Repository.upsertRelationship(EntityManager.NODE_RESTRICTION, restriction1ID,
			EntityManager.NODE_CULTURAL_NORM, culturalNorm1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final CulturalNormDialog dialog = create(parent);
//			final CulturalNormDialog dialog = createShowOnly(parent)
//				.withReference(EntityManager.NODE_CULTURAL_NORM, 1);
			dialog.loadData();
//			dialog.loadData(1);
//			if(!dialog.selectData(extractRecordID(culturalNorm)))
//				dialog.showNewRecord();

			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public void refresh(final EditEvent editCommand){
					final Map<String, Object> container = editCommand.getContainer();
					final int culturalNormID = extractRecordID(container);
					switch(editCommand.getType()){
						case ASSERTION -> {
							final AssertionDialog assertionDialog = (dialog.isViewOnlyComponent(dialog.assertionButton)
									? AssertionDialog.createSelectOnly(parent)
									: AssertionDialog.create(parent))
								.withReference(EntityManager.NODE_CULTURAL_NORM, culturalNormID);
							assertionDialog.loadData();

							assertionDialog.showDialog();
						}
						case PLACE -> {
							final PlaceDialog placeDialog = PlaceDialog.create(parent);
							placeDialog.loadData();
							final Map.Entry<String, Map<String, Object>> placeNode = Repository.findReferencedNode(
								EntityManager.NODE_CULTURAL_NORM, culturalNormID,
								EntityManager.RELATIONSHIP_APPLIES_IN);
							if(placeNode != null && EntityManager.NODE_PLACE.equals(placeNode.getKey()))
								placeDialog.selectData(extractRecordID(placeNode.getValue()));

							placeDialog.showDialog();
						}
						case HISTORIC_DATE_START -> {
							final HistoricDateDialog historicDateDialog = HistoricDateDialog.create(parent);
							historicDateDialog.loadData();
							final Map.Entry<String, Map<String, Object>> dateStartNode = Repository.findReferencedNode(
								EntityManager.NODE_CULTURAL_NORM, culturalNormID,
								EntityManager.RELATIONSHIP_STARTED_ON);
							if(dateStartNode != null && EntityManager.NODE_HISTORIC_DATE.equals(dateStartNode.getKey()))
								historicDateDialog.selectData(extractRecordID(dateStartNode.getValue()));

							historicDateDialog.showDialog();
						}
						case HISTORIC_DATE_END -> {
							final HistoricDateDialog historicDateDialog = HistoricDateDialog.create(parent);
							historicDateDialog.loadData();
							final Map.Entry<String, Map<String, Object>> dateEndNode = Repository.findReferencedNode(
								EntityManager.NODE_CULTURAL_NORM, culturalNormID,
								EntityManager.RELATIONSHIP_ENDED_ON);
							if(dateEndNode != null && EntityManager.NODE_HISTORIC_DATE.equals(dateEndNode.getKey()))
								historicDateDialog.selectData(extractRecordID(dateEndNode.getValue()));

							historicDateDialog.showDialog();
						}
						case NOTE -> {
							final NoteDialog noteDialog = (dialog.isViewOnlyComponent(dialog.noteButton)
									? NoteDialog.createSelectOnly(parent)
									: NoteDialog.create(parent))
								.withReference(EntityManager.NODE_CULTURAL_NORM, culturalNormID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_NOTE, recordID,
											EntityManager.NODE_CULTURAL_NORM, culturalNormID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
								});
							noteDialog.loadData();

							noteDialog.showDialog();
						}
						case MEDIA -> {
							final MediaDialog mediaDialog = (dialog.isViewOnlyComponent(dialog.mediaButton)
									? MediaDialog.createSelectOnlyForMedia(parent)
									: MediaDialog.createForMedia(parent))
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(EntityManager.NODE_CULTURAL_NORM, culturalNormID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_MEDIA, recordID,
											EntityManager.NODE_CULTURAL_NORM, culturalNormID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
								});
							mediaDialog.loadData();

							mediaDialog.showDialog();
						}
						case EVENT -> {
							final EventDialog eventDialog = (dialog.isViewOnlyComponent(dialog.eventButton)
									? EventDialog.createSelectOnly(parent)
									: EventDialog.create(parent))
								.withReference(EntityManager.NODE_CULTURAL_NORM, culturalNormID);
							eventDialog.loadData();

							eventDialog.showDialog();
						}
						case MODIFICATION_HISTORY -> {
							final String tableName = editCommand.getIdentifier();
							final Integer noteID = (Integer)container.get("noteID");
							final Boolean showOnly = (Boolean)container.get("showOnly");
							final NoteDialog changeNoteDialog = (showOnly
								? NoteDialog.createModificationNoteShowOnly(parent)
								: NoteDialog.createModificationNoteEditOnly(parent));
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle((showOnly? "Show": "Edit") + " modification note for " + title + " " + culturalNormID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(noteID);

							changeNoteDialog.showDialog();
						}
						case RESEARCH_STATUS -> {
							final String tableName = editCommand.getIdentifier();
							final Integer researchStatusID = (Integer)container.get("researchStatusID");
							final Boolean showOnly = (Boolean)container.get("showOnly");
							final ResearchStatusDialog researchStatusDialog = (showOnly
								? ResearchStatusDialog.createShowOnly(parent)
								: ResearchStatusDialog.createEditOnly(parent));
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle((showOnly? "Show": "Edit") + " research status for " + title + " " + culturalNormID);
							researchStatusDialog.loadData();
							researchStatusDialog.selectData(researchStatusID);

							researchStatusDialog.showDialog();
						}
					}
				}
			};
			EventBusService.subscribe(listener);

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
