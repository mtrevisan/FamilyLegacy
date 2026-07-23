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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
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
 * Dialog for editing a {@code NOTE_RECORD} according to FLEF 0.1.0.
 * <p>
 * Structure:
 * <pre>
 * NOTE_RECORD :=
 * n @<XREF:NOTE>@ NOTE    {1:1}
 *   +1 TITLE <TEXT>    {0:1}
 *   +1 VALUE <SUBMITTER_TEXT>    {1:1}
 *   +1 MIME <MIME_TYPE>    {0:1}
 *   +1 LOCALE <LOCALE_CODE>    {0:1}
 *   +1 TRANSLATION    {0:M}
 *     +2 VALUE <TEXT>    {1:1}
 *     +2 LOCALE <LOCALE_CODE>    {0:1}
 *   +1 <<SOURCE_CITATION>>    {0:M}
 *   +1 <<RESTRICTION_STRUCTURE>>    {0:1}
 *   +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class NoteDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -4670126000119212975L;


	// Handlers
	static{
		HandlerRegistry.register(new SourceHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	// UI components
	private final BoundTextField titleField = new BoundTextField("TITLE", 30);
	private final BoundTextArea valueArea = new BoundTextArea("VALUE", 10, 30);
	private final BoundComboBox<String> mimeCombo = new BoundComboBox<>("MIME", new String[]{StringUtils.EMPTY, "text/plain", "text/html", "text/markdown"});
	private final BoundComboBox<String> localeCombo = new BoundComboBox<>("LOCALE", new String[]{StringUtils.EMPTY, "en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"});

	// Translations
	private final DefaultListModel<TranslationEntry> translationModel = new DefaultListModel<>();
	private final JList<TranslationEntry> translationList = new JList<>(translationModel);
	private final List<TranslationEntry> translationEntries = new ArrayList<>();

	// Source Citations
	private final DefaultListModel<String> sourceListModel = new DefaultListModel<>();
	private final JList<String> sourceList = new JList<>(sourceListModel);
	private final List<FLEFRecord> sourceCitations = new ArrayList<>();

	// Panels
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]5[]5[]"));
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;

	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	/**
	 * Internal representation of a TRANSLATION entry.
	 */
	private static class TranslationEntry{
		private final String locale;
		private final String value;

		TranslationEntry(String locale, String value){
			this.locale = (locale != null? locale: StringUtils.EMPTY);
			this.value = (value != null? value: StringUtils.EMPTY);
		}

		@Override
		public String toString(){
			final StringBuilder sb = new StringBuilder();
			if(!locale.isEmpty())
				sb.append("[").append(locale).append("] ");
			String display = value;
			if(display.length() > 50)
				display = display.substring(0, 47) + "...";
			sb.append(display);
			return sb.toString();
		}
	}


	public static NoteDialog createNew(final Frame parent, final FLEFModel model){
		return new NoteDialog(parent, model, null);
	}

	public static NoteDialog createEdit(final Frame parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new NoteDialog(parent, model, record);
	}


	private NoteDialog(final Frame parent, final FLEFModel model, final FLEFRecord record){
		super(parent, buildTitle(model, record), model, record);

		restrictionPanel = new RestrictionPanel(this);
		modificationPanel = new ModificationPanel(this);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	private static String buildTitle(final FLEFModel model, final FLEFRecord record){
		return (record == null
			? "New Note"
			: "Edit Note - " + record.getId());
	}

	@Override
	protected void initComponents(){
		// Register bound components
		bindingManager.bind(titleField);
		bindingManager.bind(valueArea);
		bindingManager.bind(mimeCombo);
		bindingManager.bind(localeCombo);

		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());

		final JPanel restrictionContainer = new JPanel(new MigLayout("top", "[grow]", "[grow]"));
		restrictionContainer.add(restrictionPanel, "grow");
		tabbedPane.addTab("Restriction", restrictionContainer);

		final JPanel modificationContainer = new JPanel(new MigLayout("top", "[grow]", "[grow]"));
		modificationContainer.add(modificationPanel, "grow");
		tabbedPane.addTab("Modification", modificationContainer);

		setLayout(new MigLayout("fillx,top"));
		add(tabbedPane, "growx");

		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		saveButton.addActionListener(e -> save());
		cancelButton.addActionListener(e -> dispose());
	}

	private JPanel createMainPanel(){
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// TITLE
		mainPanel.add(new JLabel("Title:"), "align label");
		mainPanel.add(titleField, "growx,wrap");

		// VALUE
		mainPanel.add(new JLabel("Value*:"), "align label,top");
		valueArea.setLineWrap(true);
		valueArea.setWrapStyleWord(true);
		valueArea.setToolTipText("Markdown supported. Use [text](@<XREF:ID>@) for references, [text](confidential) for confidential data.");
		final JScrollPane valueScrollPane = GUIHelper.createScrollPane(valueArea);
		mainPanel.add(valueScrollPane, "growx, growy, wrap");

		// MIME
		mainPanel.add(new JLabel("MIME:"), "align label");
		mainPanel.add(mimeCombo, "growx,wrap");

		// LOCALE
		mainPanel.add(new JLabel("Locale:"), "align label");
		mainPanel.add(localeCombo, "growx,wrap");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 5,fillx,top,wrap 1", "[grow]", "[]5[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		panel.add(createTranslationsPanel(), "growx");
		panel.add(createSourceCitationsPanel(), "growx");

		return panel;
	}

	private JPanel createTranslationsPanel(){
		final JPanel panel = new JPanel(new MigLayout("fillx,top"));
		panel.setBorder(new TitledBorder("Translations"));

		translationList.setVisibleRowCount(4);
		translationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		GUIHelper.installBehavior(translationList,
			() -> translationList.getSelectedIndex() >= 0,
			this::editTranslation,
			this::addTranslation,
			this::removeTranslation,
			builder -> {
				builder.item("Add...", this::addTranslation);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editTranslation);
				builder.selectionSensitiveItem("Remove", this::removeTranslation);
			});

		final JScrollPane scrollPane = GUIHelper.createScrollPane(translationList);
		panel.add(scrollPane, "growx,wrap");

		return panel;
	}

	private JPanel createSourceCitationsPanel(){
		final JPanel panel = new JPanel(new MigLayout("fillx,top"));
		panel.setBorder(new TitledBorder("Source Citations"));

		sourceList.setVisibleRowCount(4);
		sourceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		GUIHelper.installBehavior(sourceList,
			() -> sourceList.getSelectedIndex() >= 0,
			this::editSourceCitation,
			() -> {},	// INSERT key – not used, we have explicit menu items
			this::removeSourceCitation,
			builder -> {
				builder.item("New...", this::createNewSourceAndAddCitation);
				builder.item("Add Existing...", this::addSourceCitation);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editSource);
				builder.selectionSensitiveItem("Edit Citation...", this::editSourceCitation);
				builder.selectionSensitiveItem("Remove", this::removeSourceCitation);
			});

		final JScrollPane scrollPane = GUIHelper.createScrollPane(sourceList);
		panel.add(scrollPane, "growx,wrap");

		return panel;
	}


	private void addTranslation(){
		final TranslationEntry newEntry = showTranslationDialog(null);
		if(newEntry != null){
			translationEntries.add(newEntry);
			translationModel.addElement(newEntry);
		}
	}

	private void editTranslation(){
		final int idx = translationList.getSelectedIndex();
		if(idx == -1)
			return;

		final TranslationEntry current = translationEntries.get(idx);
		final TranslationEntry updated = showTranslationDialog(current);
		if(updated != null){
			translationEntries.set(idx, updated);
			translationModel.set(idx, updated);
		}
	}

	private void removeTranslation(){
		final int idx = translationList.getSelectedIndex();
		if(idx == -1)
			return;

		if(JOptionPane.showConfirmDialog(this, "Remove this translation?", "Confirm",
			JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
			translationEntries.remove(idx);
			translationModel.remove(idx);
		}
	}

	/**
	 * Shows a dialog to create or edit a translation entry.
	 *
	 * @param initial the existing entry, or {@code null} for a new one
	 * @return the updated entry, or {@code null} if canceled
	 */
	private TranslationEntry showTranslationDialog(final TranslationEntry initial){
		final JDialog dialog = new JDialog(this, initial == null? "Add Translation": "Edit Translation", true);
		dialog.setLayout(new MigLayout("ins 10,fillx", "[right]rel[grow]", "[]10[]"));


		// VALUE
		final BoundTextArea valueArea = new BoundTextArea("VALUE", 5, 25);
		valueArea.setLineWrap(true);
		valueArea.setWrapStyleWord(true);
		if(initial != null)
			valueArea.setText(initial.value);
		final JScrollPane valueScroll = GUIHelper.createScrollPane(valueArea);

		dialog.add(new JLabel("Value*:"), "align label,top");
		dialog.add(valueScroll, "growx,wrap");


		// LOCALE
		final BoundComboBox<String> localeCombo = new BoundComboBox<>("LOCALE", new String[]{StringUtils.EMPTY, "en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"});
		localeCombo.setEditable(true);
		if(initial != null && !initial.locale.isEmpty())
			localeCombo.setSelectedItem(initial.locale);

		dialog.add(new JLabel("Locale:"), "align label");
		dialog.add(localeCombo, "growx,wrap");


		final JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		final JButton okBtn = new JButton("OK");
		final JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		dialog.add(btnPanel, "span 2,growx");


		final TranslationEntry[] result = {null};
		okBtn.addActionListener(e -> {
			final String locale = (String)localeCombo.getSelectedItem();
			final String value = valueArea.getText().trim();

			if(value.isEmpty()){
				JOptionPane.showMessageDialog(dialog, "Translation value cannot be empty.",
					"Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			result[0] = new TranslationEntry(
				(locale != null && !locale.isEmpty()? locale: null),
				value);
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
		return result[0];
	}


	/**
	 * Adds an existing source citation by selecting a source from the model.
	 */
	private void addSourceCitation(){
		final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
		final GenericSelectionDialog<?> selDialog = new GenericSelectionDialog<>(
			getParentFrame(), model, sourceHandler, selectedId -> {
			if(selectedId != null){
				final FLEFRecord citation = FLEFRecord.createChildWithValue(1, "SOURCE", selectedId);
				sourceCitations.add(citation);
				sourceListModel.addElement(getSourceCitationDisplay(citation));
			}
		});
		selDialog.setVisible(true);
	}

	/**
	 * Creates a new source using SourceDialog, then automatically adds a citation for it.
	 */
	/**
	 * Creates a new source using SourceDialog, then opens SourceCitationDialog
	 * to add a citation for it with full details (location, crop, notes, etc.).
	 */
	private void createNewSourceAndAddCitation(){
		// Remember existing source IDs before opening the dialog
		final Set<String> before = new HashSet<>();
		for(final FLEFRecord rec : model.getRecordsByType("SOURCE")){
			final String id = rec.getId();
			if(id != null)
				before.add(id);
		}

		// Open SourceDialog to create a new source
		final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
		final JDialog dialog = sourceHandler.createNewDialog(getParentFrame(), model);
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
			// Create a citation record with the source ID pre-filled
			final FLEFRecord citationRecord = FLEFRecord.createChildWithValue(1, "SOURCE", newSourceId);

			// Open SourceCitationDialog in edit mode (with pre-filled source)
			final SourceCitationDialog citationDialog = new SourceCitationDialog(getParentFrame(), model, citationRecord);
			citationDialog.setVisible(true);

			if(citationDialog.isSaved()){
				final FLEFRecord savedCitation = citationDialog.getCitationRecord();
				if(savedCitation != null){
					savedCitation.setLevel(1);
					savedCitation.setTag("SOURCE");
					sourceCitations.add(savedCitation);
					sourceListModel.addElement(getSourceCitationDisplay(savedCitation));
				}
			}
			// If the user cancels the citation dialog, we don't add the citation
		}
	}

	/**
	 * Edits the source record associated with the selected citation.
	 * Opens SourceDialog in edit mode for the referenced source.
	 */
	private void editSource(){
		final int idx = sourceList.getSelectedIndex();
		if(idx == -1)
			return;

		final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);

		final FLEFRecord citation = sourceCitations.get(idx);
		final String id = citation.getValue();
		final FLEFRecord record = model.getRecordById(id);
		if(record == null){
			JOptionPane.showMessageDialog(this, "Source record not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		final JDialog dialog = sourceHandler.createEditDialog(getParentFrame(), model, record);
		dialog.setVisible(true);

		// After editing, update the display name of the citation (the source name might have changed)
		sourceListModel.set(idx, getSourceCitationDisplay(citation));
	}

	private void editSourceCitation(){
		final int idx = sourceList.getSelectedIndex();
		if(idx == -1)
			return;

		final FLEFRecord existing = sourceCitations.get(idx);
		final SourceCitationDialog dialog = new SourceCitationDialog(getParentFrame(), model, existing);
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
		if(idx == -1)
			return;

		if(JOptionPane.showConfirmDialog(this, "Remove this source citation?", "Confirm",
			JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
			sourceCitations.remove(idx);
			sourceListModel.remove(idx);
		}
	}

	private String getSourceCitationDisplay(final FLEFRecord citation){
		final String sourceId = citation.getValue();
		if(sourceId != null){
			final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
			final FLEFRecord record = model.getRecordById(sourceId);
			return (record != null
				? sourceHandler.getDisplayName(record)
				: sourceId);
		}
		return "[empty]";
	}

	@Override
	protected void loadData(){
		setTitle(buildTitle(model, record));

		// ---- Simple fields: load via binding manager ----
		bindingManager.loadFromRecord(record);

		// ---- Complex fields: manual load ----

		// TRANSLATION
		translationEntries.clear();
		translationModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("TRANSLATION".equals(child.getTag())){
				String translationLocale = FLEFRecordUtils.getChildValue(child, "LOCALE");
				String translationValue = FLEFRecordUtils.getChildValue(child, "VALUE");
				if(translationValue == null){
					// Try to get from child (in case of nested structure)
					translationValue = FLEFRecordUtils.getChildValue(child, "VALUE");
				}
				if(translationValue != null && !translationValue.isEmpty()){
					TranslationEntry entry = new TranslationEntry(translationLocale, translationValue);
					translationEntries.add(entry);
					translationModel.addElement(entry);
				}
			}
		}

		// SOURCE_CITATION
		sourceCitations.clear();
		sourceListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("SOURCE".equals(child.getTag())){
				sourceCitations.add(child);
				sourceListModel.addElement(getSourceCitationDisplay(child));
			}
		}

		// RESTRICTION
		FLEFRecord restrictionStruct = FLEFRecordUtils.findChild(record, "RESTRICTION");
		restrictionPanel.loadFromRecord(restrictionStruct);

		// MODIFICATION
		modificationPanel.loadFromRecord(record);
	}

	@Override
	protected boolean validateData(){
		if(valueArea.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(this,
				"Note VALUE is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			// Ensure the tab containing valueArea is visible
			tabbedPane.setSelectedComponent(mainPanel);
			SwingUtilities.invokeLater(valueArea::requestFocusInWindow);

			return false;
		}

		if(restrictionPanel.hasData() && !restrictionPanel.validateRequiredFields())
			return false;

		return true;
	}

	@Override
	protected void saveRecord(){
		FLEFRecordUtils.removeAllChildren(record);

		// ---- Save simple fields via binding manager ----
		bindingManager.saveToRecord(record);

		// ---- Complex fields: manual save ----

		// TRANSLATION
		FLEFRecordUtils.removeChildren(record, "TRANSLATION");
		for(int i = 0, length = translationEntries.size(); i < length; i ++){
			final TranslationEntry entry = translationEntries.get(i);

			FLEFRecordUtils.addChild(record, "TRANSLATION[" + i + "].VALUE", entry.value);
			FLEFRecordUtils.addChild(record, "TRANSLATION[" + i + "].LOCALE", entry.locale);
		}

		// SOURCE
		for(final FLEFRecord citation : sourceCitations){
			citation.setLevel(1);
			citation.setTag("SOURCE");
			record.addChild(citation);
		}

		// RESTRICTION
		if(restrictionPanel.hasData())
			restrictionPanel.saveToRecord(record);

		// MODIFICATION
		modificationPanel.saveToRecord(record);

		if(isNew){
			model.addRecord(record);
		}

		dispose();
	}


	@Override
	protected FLEFRecord createNewRecord(){
		return FLEFRecord.createMainRecord(generateNewId(), NoteHandler.TYPE);
	}

	@Override
	protected String generateNewId(){
		return FLEFRecordUtils.generateNewId(model, NoteHandler.TYPE, NoteHandler.ID_PREFIX);
	}


	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			NoteDialog dialog = NoteDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
