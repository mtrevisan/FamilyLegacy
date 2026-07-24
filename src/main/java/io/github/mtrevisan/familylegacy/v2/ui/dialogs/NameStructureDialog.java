/*
 * Copyright (c) 2026 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.DatePanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Dialog for editing a {@code NAME_STRUCTURE} according to FLEF 0.1.0.
 * <p>
 * Structure:
 * <pre>
 * NAME_STRUCTURE :=
 * n NAME    {1:1}
 *   +1 <<TEXT_VALUE>>    {1:1}
 *     +2 VALUE <TEXT>    {1:1}
 *       +3 <<TEXT_VALUE_VARIANT>>    {0:M}
 *       +3 LOCALE <LOCALE_CODE>    {0:1}
 *       +3 VALID_FROM    {0:1}
 *         +4 <<DATE_STRUCTURE>>    {1:1}
 *       +3 VALID_TO    {0:1}
 *         +4 <<DATE_STRUCTURE>>    {1:1}
 *       +3 NOTE @<XREF:NOTE>@    {0:M}
 *       +3 <<SOURCE_CITATION>>    {0:M}
 *   +1 TYPE <NAME_TYPE>    {0:1}
 * </pre>
 */
public class NameStructureDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 7526263144620538539L;

	static{
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
	}

	private final BindingManager bindingManager = new BindingManager();

	private final FLEFModel model;
	private final FLEFRecord nameRecord;
	private boolean saved;

	// Simple bound fields (using dot notation to access children)
	private final BoundTextField valueField;           // maps to "VALUE.VALUE"
	private final BoundComboBox<String> typeCombo;     // maps to "TYPE"
	private final BoundComboBox<String> localeCombo;   // maps to "VALUE.LOCALE"

	// Validity Dates (using DatePanel)
	private DatePanel validFromDatePanel;
	private DatePanel validToDatePanel;

	// Variants (TEXT_VALUE_VARIANT) – list of PHONETIC / TRANSCRIPTION records
	private final DefaultListModel<String> variantListModel = new DefaultListModel<>();
	private final JList<String> variantList = new JList<>(variantListModel);
	private final List<FLEFRecord> variantRecords = new ArrayList<>();

	// Notes (NOTE references) – list of note IDs
	private final DefaultListModel<String> noteListModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteListModel);
	private final List<String> noteIds = new ArrayList<>();

	// Source Citations (SOURCE) – list of citation records
	private final DefaultListModel<String> sourceListModel = new DefaultListModel<>();
	private final JList<String> sourceList = new JList<>(sourceListModel);
	private final List<FLEFRecord> sourceCitations = new ArrayList<>();

	private final JTabbedPane tabbedPane = new JTabbedPane();

	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	// ==================== Constructors ====================

	/**
	 * Creates a new NameStructureDialog.
	 *
	 * @param parent         the parent window
	 * @param model          the FLEF model
	 * @param existingRecord an existing NAME record to edit, or {@code null} to create a new one
	 */
	public NameStructureDialog(final Window parent, final FLEFModel model, final FLEFRecord existingRecord){
		super(parent, existingRecord == null? "Add Name": "Edit Name", ModalityType.APPLICATION_MODAL);

		this.model = model;
		this.nameRecord = (existingRecord != null)? copyRecord(existingRecord): FLEFRecord.createChild(1, "NAME");

		// Initialize bound components with proper paths
		valueField = new BoundTextField("VALUE", 30);
		typeCombo = new BoundComboBox<>("TYPE", new String[]{StringUtils.EMPTY, "official", "colonial", "indigenous"});
		localeCombo = new BoundComboBox<>("VALUE.LOCALE", new String[]{StringUtils.EMPTY, "en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"});

		initComponents();
		loadData();
		pack();
		setLocationRelativeTo(parent);
	}

	private static FLEFRecord copyRecord(final FLEFRecord original){
		final FLEFRecord copy = FLEFRecord.createChildWithValue(original.getLevel(), original.getTag(), original.getValue());
		for(final FLEFRecord child : original.getChildren()){
			copy.addChild(child);
		}
		return copy;
	}

	// ==================== UI Initialization ====================

	private void initComponents(){
		// Register bound components
		bindingManager.bind(valueField);
		bindingManager.bind(typeCombo);
		bindingManager.bind(localeCombo);

		// Tabs
		tabbedPane.addTab("General", createGeneralPanel());
		tabbedPane.addTab("Validity", createValidityPanel());
		tabbedPane.addTab("Variants", createVariantsPanel());
		tabbedPane.addTab("References", createReferencesPanel());

		setLayout(new MigLayout("ins 10, fillx, top", "[grow]", "[]10[]"));
		add(tabbedPane, "growx, wrap");

		// Buttons
		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, "growx");

		saveButton.addActionListener(e -> save());
		cancelButton.addActionListener(e -> dispose());
	}

	private JPanel createGeneralPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10, fillx, top", "[right]rel[grow]", "[]5[]5[]5"));

		// VALUE (text)
		panel.add(new JLabel("Name Value*:"), "align label");
		panel.add(valueField, "growx, wrap");

		// TYPE
		typeCombo.setEditable(true);
		panel.add(new JLabel("Type:"), "align label");
		panel.add(typeCombo, "growx, wrap");

		// LOCALE
		localeCombo.setEditable(true);
		panel.add(new JLabel("Locale:"), "align label");
		panel.add(localeCombo, "growx, wrap");

		return panel;
	}

	private JPanel createValidityPanel() {
		final JPanel panel = new JPanel(new MigLayout("ins 10, fillx, top", "[grow]", "[]"));
		panel.setBorder(new TitledBorder("Validity Dates"));

		validFromDatePanel = new DatePanel(this, model);
		validToDatePanel = new DatePanel(this, model);

		// Wrap each DatePanel to have consistent padding and titled border
		JPanel fromWrapper = wrapDatePanel(validFromDatePanel, "Valid From");
		JPanel toWrapper = wrapDatePanel(validToDatePanel, "Valid To");

		// Use JTabbedPane to save space
		JTabbedPane validityTabbedPane = new JTabbedPane();
		validityTabbedPane.addTab("Valid From", fromWrapper);
		validityTabbedPane.addTab("Valid To", toWrapper);

		panel.add(validityTabbedPane, "growx");
		return panel;
	}

	/**
	 * Wraps a DatePanel in a container with consistent margins and TitledBorder
	 * to match the layout of Bounded and Spanning tabs inside DatePanel.
	 */
	private JPanel wrapDatePanel(DatePanel datePanel, String title){
		JPanel outer = new JPanel(new MigLayout("ins 0, fillx"));
		outer.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

		JPanel wrapper = new JPanel(new MigLayout("ins 0, fillx", "[right]rel[grow]"));
		wrapper.setBorder(BorderFactory.createTitledBorder(title));
		wrapper.add(datePanel, "growx");

		outer.add(wrapper, "growx");
		return outer;
	}

	private JPanel createVariantsPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10, fillx, top"));
		panel.setBorder(new TitledBorder("Text Value Variants"));

		variantList.setVisibleRowCount(4);
		GUIHelper.installBehavior(variantList,
			() -> variantList.getSelectedIndex() >= 0,
			this::editVariant,
			this::addVariant,
			this::removeVariant,
			builder -> {
				builder.item("Add Variant...", this::addVariant);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editVariant);
				builder.selectionSensitiveItem("Remove", this::removeVariant);
			});

		panel.add(GUIHelper.createScrollPane(variantList), "growx");
		return panel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 5, fillx, top, wrap 1", "[grow]", "[]5[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		panel.add(createNotesPanel(), "growx");
		panel.add(createSourceCitationsPanel(), "growx");

		return panel;
	}

	private JPanel createNotesPanel(){
		final JPanel panel = new JPanel(new MigLayout("fillx, top"));
		panel.setBorder(new TitledBorder("Notes"));

		noteList.setVisibleRowCount(3);
		GUIHelper.installBehavior(noteList,
			() -> noteList.getSelectedIndex() >= 0,
			this::editNote,
			this::addNote,
			this::removeNote,
			builder -> {
				builder.item("Add Note...", this::addNote);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editNote);
				builder.selectionSensitiveItem("Remove", this::removeNote);
			});

		panel.add(GUIHelper.createScrollPane(noteList), "growx, wrap");
		return panel;
	}

	private JPanel createSourceCitationsPanel(){
		final JPanel panel = new JPanel(new MigLayout("fillx, top"));
		panel.setBorder(new TitledBorder("Source Citations"));

		sourceList.setVisibleRowCount(4);
		GUIHelper.installBehavior(sourceList,
			() -> sourceList.getSelectedIndex() >= 0,
			this::editSourceCitation,
			() -> {
			}, // INSERT not used
			this::removeSourceCitation,
			builder -> {
				builder.item("New...", this::createNewSourceAndAddCitation);
				builder.item("Add Existing...", this::addSourceCitation);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editSource);
				builder.selectionSensitiveItem("Edit Citation...", this::editSourceCitation);
				builder.selectionSensitiveItem("Remove", this::removeSourceCitation);
			});

		panel.add(GUIHelper.createScrollPane(sourceList), "growx, wrap");
		return panel;
	}

	// ==================== Variant Management ====================

	private void addVariant(){
		final TextValueVariantDialog dialog = new TextValueVariantDialog(this, null);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			final FLEFRecord record = dialog.getVariantRecord();
			if(record != null){
				variantRecords.add(record);
				variantListModel.addElement(getVariantDisplay(record));
			}
		}
	}

	private void editVariant(){
		final int idx = variantList.getSelectedIndex();
		if(idx == -1) return;

		final FLEFRecord existing = variantRecords.get(idx);
		final TextValueVariantDialog dialog = new TextValueVariantDialog(this, existing);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			final FLEFRecord updated = dialog.getVariantRecord();
			if(updated != null){
				variantRecords.set(idx, updated);
				variantListModel.set(idx, getVariantDisplay(updated));
			}
		}
	}

	private void removeVariant(){
		final int idx = variantList.getSelectedIndex();
		if(idx >= 0){
			variantRecords.remove(idx);
			variantListModel.remove(idx);
		}
	}

	private String getVariantDisplay(final FLEFRecord record){
		if("PHONETIC".equals(record.getTag())){
			final String val = FLEFRecordUtils.getChildValue(record, "VALUE");
			return "[Phonetic - " + record.getValue() + "]: " + (val != null? val: "");
		}
		else if("TRANSCRIPTION".equals(record.getTag())){
			final String type = FLEFRecordUtils.getChildValue(record, "TYPE");
			final String val = FLEFRecordUtils.getChildValue(record, "VALUE");
			return "[Transcription - " + record.getValue() + (StringUtils.isNotBlank(type)? " (" + type + ")": "") + "]: " + (val != null? val: "");
		}
		return record.getTag();
	}

	// ==================== Note Management ====================

	private void addNote(){
		final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);
		final GenericSelectionDialog<?> selDialog = new GenericSelectionDialog<>(
			(Frame)SwingUtilities.getWindowAncestor(this), model, noteHandler, selectedId -> {
			if(selectedId != null){
				noteIds.add(selectedId);
				noteListModel.addElement(getNoteDisplay(selectedId));
			}
		});
		selDialog.setVisible(true);
	}

	private void editNote(){
		final int idx = noteList.getSelectedIndex();
		if(idx == -1) return;

		final String noteId = noteIds.get(idx);
		final FLEFRecord noteRec = model.getRecordById(noteId);
		if(noteRec != null){
			final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);
			final JDialog dialog = noteHandler.createEditDialog((Frame)SwingUtilities.getWindowAncestor(this), model, noteRec);
			dialog.setVisible(true);
			// Update display (title might have changed)
			noteListModel.set(idx, getNoteDisplay(noteId));
		}
	}

	private void removeNote(){
		final int idx = noteList.getSelectedIndex();
		if(idx >= 0){
			noteIds.remove(idx);
			noteListModel.remove(idx);
		}
	}

	private String getNoteDisplay(final String noteId){
		final FLEFRecord rec = model.getRecordById(noteId);
		if(rec != null){
			final String title = FLEFRecordUtils.getChildValue(rec, "TITLE");
			return StringUtils.isNotBlank(title)? title + " (" + noteId + ")": noteId;
		}
		return noteId;
	}

	// ==================== Source Citation Management ====================

	private void addSourceCitation(){
		final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
		final GenericSelectionDialog<?> selDialog = new GenericSelectionDialog<>(
			(Frame)SwingUtilities.getWindowAncestor(this), model, sourceHandler, selectedId -> {
			if(selectedId != null){
				final FLEFRecord citation = FLEFRecord.createChildWithValue(3, "SOURCE", FLEFRecordUtils.formatXRef(selectedId));
				sourceCitations.add(citation);
				sourceListModel.addElement(getSourceCitationDisplay(citation));
			}
		});
		selDialog.setVisible(true);
	}

	private void createNewSourceAndAddCitation(){
		// Remember existing source IDs before opening the dialog
		final Set<String> before = new HashSet<>();
		for(final FLEFRecord rec : model.getRecordsByType("SOURCE")){
			final String id = rec.getId();
			if(id != null) before.add(id);
		}

		// Open SourceDialog to create a new source
		final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
		final JDialog dialog = sourceHandler.createNewDialog((Frame)SwingUtilities.getWindowAncestor(this), model);
		dialog.setVisible(true);

		// After dialog closes, check for a newly added source
		String newSourceId = null;
		for(final FLEFRecord rec : model.getRecordsByType("SOURCE")){
			final String id = rec.getId();
			if(id != null && !before.contains(id)){
				newSourceId = id;
				break;
			}
		}

		// If a new source was created, open SourceCitationDialog to add a citation
		if(newSourceId != null){
			final FLEFRecord citationRecord = FLEFRecord.createChildWithValue(3, "SOURCE", FLEFRecordUtils.formatXRef(newSourceId));
			final SourceCitationDialog citationDialog = new SourceCitationDialog((Frame)SwingUtilities.getWindowAncestor(this), model, citationRecord);
			citationDialog.setVisible(true);

			if(citationDialog.isSaved()){
				final FLEFRecord savedCitation = citationDialog.getCitationRecord();
				if(savedCitation != null){
					savedCitation.setLevel(3);
					savedCitation.setTag("SOURCE");
					sourceCitations.add(savedCitation);
					sourceListModel.addElement(getSourceCitationDisplay(savedCitation));
				}
			}
		}
	}

	private void editSource(){
		final int idx = sourceList.getSelectedIndex();
		if(idx == -1) return;

		final FLEFRecord citation = sourceCitations.get(idx);
		final String rawId = FLEFRecordUtils.extractXRef(citation.getValue());
		final FLEFRecord rec = model.getRecordById(rawId);
		if(rec != null){
			final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
			final JDialog dialog = sourceHandler.createEditDialog((Frame)SwingUtilities.getWindowAncestor(this), model, rec);
			dialog.setVisible(true);
			// Update display name (source title might have changed)
			sourceListModel.set(idx, getSourceCitationDisplay(citation));
		}
	}

	private void editSourceCitation(){
		final int idx = sourceList.getSelectedIndex();
		if(idx == -1) return;

		final FLEFRecord existing = sourceCitations.get(idx);
		final SourceCitationDialog dialog = new SourceCitationDialog((Frame)SwingUtilities.getWindowAncestor(this), model, existing);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			final FLEFRecord updated = dialog.getCitationRecord();
			if(updated != null){
				sourceCitations.set(idx, updated);
				sourceListModel.set(idx, getSourceCitationDisplay(updated));
			}
		}
	}

	private void removeSourceCitation(){
		final int idx = sourceList.getSelectedIndex();
		if(idx >= 0){
			sourceCitations.remove(idx);
			sourceListModel.remove(idx);
		}
	}

	private String getSourceCitationDisplay(final FLEFRecord citation){
		final String rawSourceId = FLEFRecordUtils.extractXRef(citation.getValue());
		if(rawSourceId != null){
			final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
			final FLEFRecord rec = model.getRecordById(rawSourceId);
			return (rec != null && sourceHandler != null)? sourceHandler.getDisplayName(rec): rawSourceId;
		}
		return "[empty]";
	}

	// ==================== Data Loading & Saving ====================

	private void loadData(){
		// Load simple fields via BindingManager
		bindingManager.loadFromRecord(nameRecord);

		// Find the VALUE child (may be missing)
		final FLEFRecord valueRec = FLEFRecordUtils.findChild(nameRecord, "VALUE");
		if(valueRec == null){
			// No VALUE child: clear all complex fields
			validFromDatePanel.clear();
			validToDatePanel.clear();
			variantRecords.clear();
			variantListModel.clear();
			noteIds.clear();
			noteListModel.clear();
			sourceCitations.clear();
			sourceListModel.clear();
			return;
		}

		// Load VALID_FROM
		final FLEFRecord validFrom = FLEFRecordUtils.findChild(valueRec, "VALID_FROM");
		if(validFrom != null){
			final FLEFRecord dateRec = FLEFRecordUtils.findChild(validFrom, "DATE");
			if(dateRec != null){
				validFromDatePanel.loadFromRecord(dateRec);
			}
		}
		else{
			validFromDatePanel.clear();
		}

		// Load VALID_TO
		final FLEFRecord validTo = FLEFRecordUtils.findChild(valueRec, "VALID_TO");
		if(validTo != null){
			final FLEFRecord dateRec = FLEFRecordUtils.findChild(validTo, "DATE");
			if(dateRec != null){
				validToDatePanel.loadFromRecord(dateRec);
			}
		}
		else{
			validToDatePanel.clear();
		}

		// Load variants
		variantRecords.clear();
		variantListModel.clear();
		for(final FLEFRecord child : valueRec.getChildren()){
			if("PHONETIC".equals(child.getTag()) || "TRANSCRIPTION".equals(child.getTag())){
				variantRecords.add(child);
				variantListModel.addElement(getVariantDisplay(child));
			}
		}

		// Load notes
		noteIds.clear();
		noteListModel.clear();
		for(final FLEFRecord child : valueRec.getChildren()){
			if("NOTE".equals(child.getTag())){
				final String rawId = FLEFRecordUtils.extractXRef(child.getValue());
				if(rawId != null){
					noteIds.add(rawId);
					noteListModel.addElement(getNoteDisplay(rawId));
				}
			}
		}

		// Load source citations
		sourceCitations.clear();
		sourceListModel.clear();
		for(final FLEFRecord child : valueRec.getChildren()){
			if("SOURCE".equals(child.getTag())){
				sourceCitations.add(child);
				sourceListModel.addElement(getSourceCitationDisplay(child));
			}
		}
	}

	private boolean validateData(){
		final String text = valueField.getText().trim();
		if(text.isEmpty()){
			JOptionPane.showMessageDialog(this, "Name value cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		return true;
	}

	private void save(){
		if(!validateData())
			return;

		// Clear all children of nameRecord (we will rebuild)
		FLEFRecordUtils.removeAllChildren(nameRecord);

		// Let BindingManager save simple fields (TYPE, VALUE.VALUE, VALUE.LOCALE)
		bindingManager.saveToRecord(nameRecord);

		FLEFRecord valueRec = FLEFRecordUtils.findChild(nameRecord, "VALUE");

		// Remove any existing children that we will rebuild (except those managed by BindingManager?)
		// BindingManager may have added LOCALE, but we want to manage LOCALE ourselves?
		// In this design, we let BindingManager handle LOCALE as "VALUE.LOCALE", so we should not remove it.
		// However, we need to remove old VALID_FROM, VALID_TO, variants, notes, sources that might have been there.
		// We'll remove them explicitly.
		FLEFRecordUtils.removeChildren(valueRec, "VALID_FROM", "VALID_TO", "PHONETIC", "TRANSCRIPTION", "NOTE", "SOURCE");

		// Add VALID_FROM
		if(validFromDatePanel.hasData()){
			final FLEFRecord dateRecord = validFromDatePanel.saveToRecord(null);
			if(dateRecord != null){
				final FLEFRecord validFrom = FLEFRecord.createChild(3, "VALID_FROM");
				final FLEFRecord dateCopy = FLEFRecordUtils.copyRecordWithLevel(dateRecord, 4);
				validFrom.addChild(dateCopy);
				valueRec.addChild(validFrom);
			}
		}

		// Add VALID_TO
		if(validToDatePanel.hasData()){
			final FLEFRecord dateRecord = validToDatePanel.saveToRecord(null);
			if(dateRecord != null){
				final FLEFRecord validTo = FLEFRecord.createChild(3, "VALID_TO");
				final FLEFRecord dateCopy = FLEFRecordUtils.copyRecordWithLevel(dateRecord, 4);
				validTo.addChild(dateCopy);
				valueRec.addChild(validTo);
			}
		}

		// Add variants
		for(final FLEFRecord variant : variantRecords){
			variant.setLevel(3);
			valueRec.addChild(variant);
		}

		// Add notes
		for(final String noteId : noteIds){
			FLEFRecordUtils.addReferenceChild(valueRec, "NOTE", noteId);
		}

		// Add source citations
		for(final FLEFRecord citation : sourceCitations){
			citation.setLevel(3);
			citation.setTag("SOURCE");
			valueRec.addChild(citation);
		}

		// Note: BindingManager already saved TYPE as a direct child.
		// We are done.

		saved = true;
		dispose();
	}

	public boolean isSaved(){
		return saved;
	}

	public FLEFRecord getNameRecord(){
		return nameRecord;
	}

}
