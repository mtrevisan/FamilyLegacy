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
import io.github.mtrevisan.familylegacy.flef.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.helpers.DependencyInjector;
import io.github.mtrevisan.familylegacy.flef.helpers.FileHelper;
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
import io.github.mtrevisan.familylegacy.flef.ui.panels.HistoryPanel;
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
import java.io.IOException;
import java.io.Serial;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;

import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordCertainty;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordCredibility;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordDateEndID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordDateID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordDateStartID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordDescription;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordIdentifier;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordPlaceID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordReferenceID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordReferenceTable;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordCertainty;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordCredibility;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordDescription;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordIdentifier;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordReferenceID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordReferenceTable;


public final class CulturalNormDialog extends CommonListDialog implements TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = -3961030253095528462L;

	private static final int TABLE_INDEX_IDENTIFIER = 2;

	private static final String TABLE_NAME = "cultural_norm";
	private static final String TABLE_NAME_CULTURAL_NORM_JUNCTION = "cultural_norm_junction";
	private static final String TABLE_NAME_ASSERTION = "assertion";
	private static final String TABLE_NAME_EVENT = "event";


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

	private HistoryPanel historyPanel;

	private String filterReferenceTable;
	private int filterReferenceID;


	public static CulturalNormDialog create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final CulturalNormDialog dialog = new CulturalNormDialog(store, parent);
		dialog.initialize();
		return dialog;
	}

	public static CulturalNormDialog createSelectOnly(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final CulturalNormDialog dialog = new CulturalNormDialog(store, parent);
		dialog.selectRecordOnly = true;
		dialog.hideUnselectButton = true;
		dialog.addViewOnlyComponents(dialog.placeButton, dialog.dateStartButton, dialog.dateEndButton, dialog.noteButton, dialog.mediaButton,
			dialog.assertionButton, dialog.eventButton);
		dialog.initialize();
		return dialog;
	}

	public static CulturalNormDialog createRecordOnly(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final CulturalNormDialog dialog = new CulturalNormDialog(store, parent);
		dialog.selectRecordOnly = true;
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}


	private CulturalNormDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public CulturalNormDialog withOnCloseGracefully(final Consumer<Map<String, Object>> onCloseGracefully){
		Consumer<Map<String, Object>> innerOnCloseGracefully = record -> {
			final NavigableMap<Integer, Map<String, Object>> mediaJunctions = getRecords(TABLE_NAME_CULTURAL_NORM_JUNCTION);
			final int mediaJunctionID = extractNextRecordID(mediaJunctions);
			if(selectedRecord == null)
				mediaJunctions.remove(mediaJunctionID);
			else if(filterReferenceTable != null){
				mediaJunctions.put(mediaJunctionID, selectedRecordLink);

				if(onCloseGracefully != null)
					onCloseGracefully.accept(record);
			}
			else if(onCloseGracefully != null)
				onCloseGracefully.accept(record);
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
		return TABLE_NAME;
	}

	@Override
	protected String  getJunctionTableName(){
		return TABLE_NAME_CULTURAL_NORM_JUNCTION;
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
		final Comparator<String> numericComparator = GUIHelper.getNumericComparator();
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
		historyPanel = HistoryPanel.create(store)
			.withLinkListener((table, id) -> EventBusService.publish(EditEvent.create(EditEvent.EditType.MODIFICATION_HISTORY, getTableName(),
				Map.of("id", extractRecordID(selectedRecord), "note_id", id))));


		GUIHelper.bindLabelTextChangeUndo(identifierLabel, identifierField, this::saveData);
		addMandatoryField(identifierField);
		descriptionTextPreview.setTextViewFont(identifierField.getFont());
		descriptionTextPreview.setMinimumSize(MINIMUM_NOTE_TEXT_PREVIEW_SIZE);

		GUIHelper.bindLabelTextChange(descriptionLabel, descriptionTextPreview, this::saveData);

		placeButton.setToolTipText("Place");
		placeButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PLACE, TABLE_NAME, selectedRecord)));

		dateStartButton.setToolTipText("Start date");
		dateStartButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.HISTORIC_DATE, TABLE_NAME, selectedRecord)));

		dateEndButton.setToolTipText("End date");
		dateEndButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.HISTORIC_DATE, TABLE_NAME, selectedRecord)));

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(certaintyLabel, certaintyComboBox, this::saveData);
		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(credibilityLabel, credibilityComboBox, this::saveData);


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, TABLE_NAME, selectedRecord)));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, TABLE_NAME, selectedRecord)));

		assertionButton.setToolTipText("Assertions");
		assertionButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.ASSERTION, TABLE_NAME, selectedRecord)));

		eventButton.setToolTipText("Events");
		eventButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.EVENT, TABLE_NAME, selectedRecord)));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);


		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(linkCertaintyLabel, linkCertaintyComboBox, this::saveData);
		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(linkCredibilityLabel, linkCredibilityComboBox, this::saveData);
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
		recordTabbedPane.add("history", historyPanel);
	}

	@Override
	public void loadData(){
		unselectAction();

		final Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME);

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final Map<String, Object> container = record.getValue();

			final String identifier = extractRecordIdentifier(container);
			final FilterString filter = FilterString.create()
				.add(key)
				.add(identifier);
			final String filterData = filter.toString();

			model.setValueAt(key, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(identifier, row, TABLE_INDEX_IDENTIFIER);

			row ++;
		}

		if(selectRecordOnly)
			selectFirstData();
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
		final Integer placeID = extractRecordPlaceID(selectedRecord);
		final Integer dateStartID = extractRecordDateStartID(selectedRecord);
		final Integer dateEndID = extractRecordDateEndID(selectedRecord);
		final String certainty = extractRecordCertainty(selectedRecord);
		final String credibility = extractRecordCredibility(selectedRecord);
		final boolean hasNotes = (getRecords(TABLE_NAME_NOTE)
			.values().stream()
			.filter(record -> Objects.equals(TABLE_NAME, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(culturalNormID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final boolean hasMedia = (getRecords(TABLE_NAME_MEDIA_JUNCTION)
			.values().stream()
			.filter(record -> Objects.equals(TABLE_NAME, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(culturalNormID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final boolean hasAssertions = (getRecords(TABLE_NAME_ASSERTION)
			.values().stream()
			.filter(record -> Objects.equals(TABLE_NAME, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(culturalNormID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final boolean hasEvents = (getRecords(TABLE_NAME_EVENT)
			.values().stream()
			.filter(record -> Objects.equals(TABLE_NAME, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(culturalNormID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final String restriction = getRecords(TABLE_NAME_RESTRICTION)
			.values().stream()
			.filter(record -> Objects.equals(TABLE_NAME, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(culturalNormID, extractRecordReferenceID(record)))
			.findFirst()
			.map(EntityManager::extractRecordRestriction)
			.orElse(null);

		identifierField.setText(identifier);
		descriptionTextPreview.setText("Cultural norm " + culturalNormID, description, null);
		setButtonEnableAndBorder(placeButton, placeID != null);
		setButtonEnableAndBorder(dateStartButton, dateStartID != null);
		setButtonEnableAndBorder(dateEndButton, dateEndID != null);
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
			final Map<Integer, Map<String, Object>> recordCulturalNormJunction = extractReferences(TABLE_NAME_CULTURAL_NORM_JUNCTION,
				EntityManager::extractRecordCulturalNormID, culturalNormID);
			if(recordCulturalNormJunction.size() > 1)
				throw new IllegalArgumentException("Data integrity error");

			final Iterator<Map<String, Object>> itr = recordCulturalNormJunction.values().iterator();
			if(itr.hasNext()){
				selectedRecordLink = itr.next();

				final String linkCertainty = extractRecordCertainty(selectedRecordLink);
				final String linkCredibility = extractRecordCredibility(selectedRecordLink);

				linkCertaintyComboBox.setSelectedItem(linkCertainty);
				linkCredibilityComboBox.setSelectedItem(linkCredibility);
			}
		}

		historyPanel.withReference(TABLE_NAME, culturalNormID);
		historyPanel.loadData();

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

		if(filterReferenceTable != null){
			//read link panel:
			final String linkCertainty = GUIHelper.getTextTrimmed(linkCertaintyComboBox);
			final String linkCredibility = GUIHelper.getTextTrimmed(linkCredibilityComboBox);

			if(selectedRecordLink == null){
				selectedRecordLink = new HashMap<>(4);
				insertRecordReferenceTable(selectedRecordLink, filterReferenceTable);
				insertRecordReferenceID(selectedRecordLink, extractRecordID(selectedRecord));
			}

			insertRecordCertainty(selectedRecordLink, linkCertainty);
			insertRecordCredibility(selectedRecordLink, linkCredibility);
		}

		//update table:
		if(!Objects.equals(identifier, extractRecordIdentifier(selectedRecord))){
			final DefaultTableModel model = getRecordTableModel();
			final Integer recordID = extractRecordID(selectedRecord);
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

		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> culturalNorms = new TreeMap<>();
		store.put("cultural_norm", culturalNorms);
		final Map<String, Object> culturalNorm = new HashMap<>();
		culturalNorm.put("id", 1);
		culturalNorm.put("identifier", "rule 1 id");
		culturalNorm.put("description", "rule 1");
		culturalNorm.put("place_id", 1);
		culturalNorm.put("date_start_id", 1);
		culturalNorm.put("date_end_id", 1);
		culturalNorm.put("certainty", "certain");
		culturalNorm.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		culturalNorms.put((Integer)culturalNorm.get("id"), culturalNorm);

		final TreeMap<Integer, Map<String, Object>> culturalNormJunctions = new TreeMap<>();
		store.put("cultural_norm_junction", culturalNormJunctions);
		final Map<String, Object> culturalNormJunction1 = new HashMap<>();
		culturalNormJunction1.put("id", 1);
		culturalNormJunction1.put("cultural_norm_id", 1);
		culturalNormJunction1.put("reference_table", "cultural_norm");
		culturalNormJunction1.put("reference_id", 1);
		culturalNormJunction1.put("certainty", "probable");
		culturalNormJunction1.put("credibility", "probable");
		culturalNormJunctions.put((Integer)culturalNormJunction1.get("id"), culturalNormJunction1);

		final TreeMap<Integer, Map<String, Object>> places = new TreeMap<>();
		store.put("place", places);
		final Map<String, Object> place1 = new HashMap<>();
		place1.put("id", 1);
		place1.put("identifier", "place 1");
		place1.put("name", "name of the place");
		place1.put("locale", "en-US");
		places.put((Integer)place1.get("id"), place1);

		final TreeMap<Integer, Map<String, Object>> dates = new TreeMap<>();
		store.put("historic_date", dates);
		final Map<String, Object> date1 = new HashMap<>();
		date1.put("id", 1);
		date1.put("date", "18 OCT 2000");
		dates.put((Integer)date1.get("id"), date1);

		final TreeMap<Integer, Map<String, Object>> notes = new TreeMap<>();
		store.put("note", notes);
		final Map<String, Object> note1 = new HashMap<>();
		note1.put("id", 1);
		note1.put("note", "note 1");
		note1.put("reference_table", "person");
		note1.put("reference_id", 1);
		notes.put((Integer)note1.get("id"), note1);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("id", 2);
		note2.put("note", "note 1");
		note2.put("reference_table", TABLE_NAME);
		note2.put("reference_id", 1);
		notes.put((Integer)note2.get("id"), note2);

		final TreeMap<Integer, Map<String, Object>> restrictions = new TreeMap<>();
		store.put("restriction", restrictions);
		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("id", 1);
		restriction1.put("restriction", "confidential");
		restriction1.put("reference_table", TABLE_NAME);
		restriction1.put("reference_id", 1);
		restrictions.put((Integer)restriction1.get("id"), restriction1);

		final TreeMap<Integer, Map<String, Object>> media = new TreeMap<>();
		store.put("media", media);
		final Map<String, Object> m1 = new HashMap<>();
		m1.put("id", 1);
		m1.put("identifier", "custom media");
		media.put((Integer)m1.get("id"), m1);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
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

//			final CulturalNormDialog dialog = create(store, parent);
			final CulturalNormDialog dialog = createRecordOnly(store, parent)
				.withReference("cultural_norm", 1);
			injector.injectDependencies(dialog);
			dialog.loadData(1);
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
									? AssertionDialog.createSelectOnly(store, parent)
									: AssertionDialog.create(store, parent))
								.withReference(TABLE_NAME, culturalNormID);
							assertionDialog.loadData();

							assertionDialog.showDialog();
						}
						case PLACE -> {
							final PlaceDialog placeDialog = PlaceDialog.create(store, parent);
							placeDialog.loadData();
							final Integer placeID = extractRecordPlaceID(container);
							if(placeID != null)
								placeDialog.selectData(placeID);

							placeDialog.showDialog();
						}
						case HISTORIC_DATE -> {
							final HistoricDateDialog historicDateDialog = HistoricDateDialog.create(store, parent);
							historicDateDialog.loadData();
							final Integer dateID = extractRecordDateID(container);
							if(dateID != null)
								historicDateDialog.selectData(dateID);

							historicDateDialog.showDialog();
						}
						case NOTE -> {
							final NoteDialog noteDialog = (dialog.isViewOnlyComponent(dialog.noteButton)
									? NoteDialog.createSelectOnly(store, parent)
									: NoteDialog.create(store, parent))
								.withReference(TABLE_NAME, culturalNormID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										insertRecordReferenceTable(record, TABLE_NAME);
										insertRecordReferenceID(record, culturalNormID);
									}
								});
							noteDialog.loadData();

							noteDialog.showDialog();
						}
						case MEDIA -> {
							final MediaDialog mediaDialog = (dialog.isViewOnlyComponent(dialog.mediaButton)
									? MediaDialog.createSelectOnlyForMedia(store, parent)
									: MediaDialog.createForMedia(store, parent))
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(TABLE_NAME, culturalNormID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										insertRecordReferenceTable(record, TABLE_NAME);
										insertRecordReferenceID(record, culturalNormID);
									}
								});
							mediaDialog.loadData();

							mediaDialog.showDialog();
						}
						case EVENT -> {
							final EventDialog eventDialog = (dialog.isViewOnlyComponent(dialog.eventButton)
									? EventDialog.createSelectOnly(store, parent)
									: EventDialog.create(store, parent))
								.withReference(TABLE_NAME, culturalNormID);
							eventDialog.loadData();

							eventDialog.showDialog();
						}
						case MODIFICATION_HISTORY -> {
							final String tableName = editCommand.getIdentifier();
							final Integer noteID = (Integer)container.get("note_id");
							final NoteDialog changeNoteDialog = NoteDialog.createModificationRecordOnly(store, parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle("Change modification note for " + title + " " + culturalNormID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(noteID);

							changeNoteDialog.showDialog();
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.out.println(store);
					System.exit(0);
				}
			});
			dialog.showDialog();
		});
	}

}
