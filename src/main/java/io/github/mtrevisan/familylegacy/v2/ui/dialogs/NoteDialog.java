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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ScrollableContainerHost;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


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
 *   +1 TRANSLATION <SUBMITTER_TRANSLATED_TEXT>    {0:M}
 *     +2 LOCALE <LOCALE_CODE>    {0:1}
 *   +1 <<SOURCE_CITATION>>    {0:M}
 *   +1 <<RESTRICTION_STRUCTURE>>    {0:1}
 *   +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class NoteDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -4670126000119212975L;

	private final BindingManager bindingManager = new BindingManager();

	// Handlers
	private final SourceHandler sourceHandler = new SourceHandler();

	// UI components – now bound
	private final BoundTextField titleField = new BoundTextField("TITLE", 30);;
	private final BoundTextArea valueArea = new BoundTextArea("VALUE", 10, 30);
	private final JScrollPane valueScrollPane = new JScrollPane(valueArea);
	private final BoundComboBox<String> mimeCombo = new BoundComboBox<>("MIME", new String[]{"", "text/plain", "text/html", "text/markdown"});
	private final BoundComboBox<String> localeCombo = new BoundComboBox<>("LOCALE", new String[]{"", "en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"});

	// Translations (0:M) – manual
	private final DefaultListModel<TranslationEntry> translationModel = new DefaultListModel<>();
	private final JList<TranslationEntry> translationList = new JList<>(translationModel);
	private final List<TranslationEntry> translationEntries = new ArrayList<>();

	// Source Citations (0:M) – manual
	private final DefaultListModel<String> sourceListModel = new DefaultListModel<>();
	private final JList<String> sourceList = new JList<>(sourceListModel);
	private final List<FLEFRecord> sourceCitations = new ArrayList<>();

	// Panels
	private RestrictionPanel restrictionPanel;
	private ModificationPanel modificationPanel;

	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	/**
	 * Internal representation of a TRANSLATION entry.
	 */
	private static class TranslationEntry{
		private final String locale;
		private final String value;

		TranslationEntry(String locale, String value){
			this.locale = locale != null? locale: "";
			this.value = value != null? value: "";
		}

		@Override
		public String toString(){
			StringBuilder sb = new StringBuilder();
			if(!locale.isEmpty()){
				sb.append("[").append(locale).append("] ");
			}
			String display = value;
			if(display.length() > 50){
				display = display.substring(0, 47) + "...";
			}
			sb.append(display);
			return sb.toString();
		}
	}

	// ----- Factory methods -----
	public static NoteDialog createNew(Frame parent, FLEFModel model){
		return new NoteDialog(parent, model, null);
	}

	public static NoteDialog createEdit(Frame parent, FLEFModel model, FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");
		return new NoteDialog(parent, model, record);
	}

	// ----- Constructor -----
	private NoteDialog(Frame parent, FLEFModel model, FLEFRecord record){
		super(parent, buildTitle(model, record), model, record);

		initComponents();
		loadData();
		setMinimumSize(new Dimension(550, 600));
		pack();
		setLocationRelativeTo(parent);
	}

	private static String buildTitle(FLEFModel model, FLEFRecord record){
		return (record == null
					  ? "New Note"
					  : "Edit Note - " + record.getId());
	}

	// ----- Initialisation -----
	@Override
	protected void initComponents(){
		// Register bound components
		bindingManager.bind(titleField);
		bindingManager.bind(mimeCombo);
		bindingManager.bind(localeCombo);

		restrictionPanel = new RestrictionPanel(this);
		modificationPanel = new ModificationPanel(model, this);

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());

		JPanel restrictionContainer = new JPanel(new MigLayout("top", "[grow]", "[grow]"));
		restrictionContainer.add(restrictionPanel, "grow");
		tabbedPane.addTab("Restriction", restrictionContainer);

		JPanel modificationContainer = new JPanel(new MigLayout("top", "[grow]", "[grow]"));
		modificationContainer.add(modificationPanel, "grow");
		tabbedPane.addTab("Modification", modificationContainer);

		setLayout(new MigLayout("fillx"));
		add(tabbedPane, "growx,push");

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		saveButton.addActionListener(e -> save());
		cancelButton.addActionListener(e -> dispose());
	}

	// ==================== Main Panel ====================
	private JPanel createMainPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10, fillx, top", "[right]rel[grow]", "[]5[]5[]5[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// TITLE (optional) – bound field
		panel.add(new JLabel("Title:"), "align label");
		panel.add(titleField, "growx,wrap");

		// VALUE (required) – multi-line (manual because it's a JTextArea)
		panel.add(new JLabel("Value:"), "align label,top");
		valueArea.setLineWrap(true);
		valueArea.setWrapStyleWord(true);
		valueScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		valueArea.setToolTipText("Markdown supported. Use [text](@<XREF:ID>@) for references, [text](confidential) for confidential data.");
		panel.add(valueScrollPane, "growx, growy, wrap");

		// MIME (optional) – bound combo
		panel.add(new JLabel("MIME:"), "align label");
		panel.add(mimeCombo, "growx,wrap");

		// LOCALE (optional) – bound combo
		panel.add(new JLabel("Locale:"), "align label");
		panel.add(localeCombo, "growx,wrap");

		return panel;
	}

	// ==================== References Panel ====================
	private JPanel createReferencesPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 5, fillx, top, wrap 1", "[grow]", "[]5[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		panel.add(createTranslationsPanel(), "growx");
		panel.add(createSourceCitationsPanel(), "growx");

		return panel;
	}

	// ==================== Translations Panel ====================
	private JPanel createTranslationsPanel(){
		JPanel panel = new JPanel(new MigLayout("fillx"));
		panel.setBorder(new TitledBorder("Translations"));

		translationList.setVisibleRowCount(4);
		translationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		GUIHelper.installBehaviour(translationList,
			() -> translationList.getSelectedIndex() >= 0,
			this::editTranslation,
			this::addTranslation,
			this::removeTranslation,
			builder -> {
				builder.item("Add Translation...", this::addTranslation);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editTranslation);
				builder.selectionSensitiveItem("Remove", this::removeTranslation);
			});

		JScrollPane scrollPane = createScrollPane(translationList);
		scrollPane.setPreferredSize(translationList.getPreferredScrollableViewportSize());
		panel.add(scrollPane, "growx,wrap");

		return panel;
	}

	// ==================== Source Citations Panel ====================
	private JPanel createSourceCitationsPanel(){
		JPanel panel = new JPanel(new MigLayout("fillx"));
		panel.setBorder(new TitledBorder("Source Citations"));

		sourceList.setVisibleRowCount(4);
		sourceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		GUIHelper.installBehaviour(sourceList,
			() -> sourceList.getSelectedIndex() >= 0,
			this::editSourceCitation,
			this::addSourceCitation,
			this::removeSourceCitation,
			builder -> {
				builder.item("Add Source...", this::addSourceCitation);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editSourceCitation);
				builder.selectionSensitiveItem("Remove", this::removeSourceCitation);
			});

		JScrollPane scrollPane = createScrollPane(sourceList);
		scrollPane.setPreferredSize(sourceList.getPreferredScrollableViewportSize());
		panel.add(scrollPane, "growx,wrap");

		return panel;
	}

	// ==================== Translation Methods ====================

	private void addTranslation(){
		TranslationEntry newEntry = showTranslationDialog(null);
		if(newEntry != null){
			translationEntries.add(newEntry);
			translationModel.addElement(newEntry);
		}
	}

	private void editTranslation(){
		int idx = translationList.getSelectedIndex();
		if(idx == -1) return;

		TranslationEntry current = translationEntries.get(idx);
		TranslationEntry updated = showTranslationDialog(current);
		if(updated != null){
			translationEntries.set(idx, updated);
			translationModel.set(idx, updated);
		}
	}

	private void removeTranslation(){
		int idx = translationList.getSelectedIndex();
		if(idx == -1) return;

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
	 * @return the updated entry, or {@code null} if cancelled
	 */
	private TranslationEntry showTranslationDialog(TranslationEntry initial){
		JDialog dialog = new JDialog(this, initial == null? "Add Translation": "Edit Translation", true);
		dialog.setLayout(new MigLayout("ins 10, fillx", "[right]rel[grow]", "[]10[]"));

		// LOCALE
		BoundComboBox<String> localeCombo = new BoundComboBox<>("LOCALE", new String[]{"", "en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"});
		localeCombo.setEditable(true);
		if(initial != null && !initial.locale.isEmpty()){
			localeCombo.setSelectedItem(initial.locale);
		}

		dialog.add(new JLabel("Locale:"), "align label");
		dialog.add(localeCombo, "growx,wrap");

		// VALUE (multi-line)
		BoundTextArea valueArea = new BoundTextArea("VALUE", 5, 25);
		valueArea.setLineWrap(true);
		valueArea.setWrapStyleWord(true);
		if(initial != null){
			valueArea.setText(initial.value);
		}
		JScrollPane valueScroll = new JScrollPane(valueArea);
		valueScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		valueScroll.setPreferredSize(new Dimension(300, 80));

		dialog.add(new JLabel("Value:"), "align label,top");
		dialog.add(valueScroll, "growx,wrap");

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		dialog.add(btnPanel, "span 2,growx");

		final TranslationEntry[] result = {null};
		okBtn.addActionListener(e -> {
			String locale = (String)localeCombo.getSelectedItem();
			String value = valueArea.getText().trim();

			if(value.isEmpty()){
				JOptionPane.showMessageDialog(dialog, "Translation value cannot be empty.",
					"Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			result[0] = new TranslationEntry(
				locale != null && !locale.isEmpty()? locale: null,
				value);
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
		return result[0];
	}

	private JScrollPane createScrollPane(final JList<?> list){
		JScrollPane scrollPane = new JScrollPane(new ScrollableContainerHost(list,
			ScrollableContainerHost.ScrollType.VERTICAL));
		scrollPane.setPreferredSize(list.getPreferredScrollableViewportSize());
		return scrollPane;
	}

	// ==================== Source Citation Methods ====================

	private void addSourceCitation(){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(this, "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		GenericSelectionDialog<?> selDialog = new GenericSelectionDialog<>(
			getParentFrame(), model, sourceHandler, selectedId -> {
			if(selectedId != null){
				FLEFRecord citation = FLEFRecord.createChildWithValue(1, "SOURCE_CITATION", selectedId);
				sourceCitations.add(citation);
				sourceListModel.addElement(getSourceCitationDisplay(citation));
			}
		});
		selDialog.setVisible(true);
	}

	private void editSourceCitation(){
		int idx = sourceList.getSelectedIndex();
		if(idx == -1) return;

		FLEFRecord existing = sourceCitations.get(idx);
		SourceCitationDialog dialog = new SourceCitationDialog(getParentFrame(), model, existing);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			FLEFRecord updated = dialog.getCitationRecord();
			if(updated != null){
				sourceCitations.set(idx, updated);
				sourceListModel.set(idx, getSourceCitationDisplay(updated));
			}
		}
	}

	private void removeSourceCitation(){
		int idx = sourceList.getSelectedIndex();
		if(idx == -1) return;

		if(JOptionPane.showConfirmDialog(this, "Remove this source citation?", "Confirm",
			JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
			sourceCitations.remove(idx);
			sourceListModel.remove(idx);
		}
	}

	private String getSourceCitationDisplay(FLEFRecord citation){
		String sourceId = citation.getValue();
		if(sourceId != null){
			FLEFRecord rec = model.getRecordById(sourceId);
			if(rec != null && sourceHandler != null){
				return sourceHandler.getDisplayName(rec);
			}
			return sourceId;
		}
		return "[empty]";
	}

	// ==================== Load Data ====================
	@Override
	protected void loadData(){
		setTitle(buildTitle(model, record));

		// ---- Simple fields: load via binding manager ----
		bindingManager.loadFromRecord(record);

		// ---- Complex fields: manual load ----

		// VALUE (required) – manual because it's a JTextArea
		String value = FLEFRecordUtils.getChildValue(record, "VALUE");
		valueArea.setText(value != null? value: "");

		// TRANSLATION
		translationEntries.clear();
		translationModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("TRANSLATION".equals(child.getTag())){
				String translationLocale = FLEFRecordUtils.getChildValue(child, "LOCALE");
				String translationValue = FLEFRecordUtils.getChildValue(child, "VALUE");
				if(translationValue == null){
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
			if("SOURCE_CITATION".equals(child.getTag())){
				sourceCitations.add(child);
				sourceListModel.addElement(getSourceCitationDisplay(child));
			}
		}

		// RESTRICTION_STRUCTURE
		FLEFRecord restrictionStruct = FLEFRecordUtils.findChild(record, "RESTRICTION");
		restrictionPanel.loadFromRecord(restrictionStruct);

		// MODIFICATION_STRUCTURE
		modificationPanel.loadFromRecord(record);
	}

	// ==================== Validation ====================
	@Override
	protected boolean validateData(){
		// VALUE is required
		if(valueArea.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(this,
				"Note VALUE is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		if(!modificationPanel.hasData()){
			JOptionPane.showMessageDialog(this,
				"Modification is required for a note.\nPlease add a CREATION date.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		if(!modificationPanel.validateRequiredFields()){
			return false;
		}

		if(restrictionPanel.hasData() && !restrictionPanel.validateRequiredFields()){
			return false;
		}

		return true;
	}

	// ==================== Save ====================
	@Override
	protected void saveRecord(){
		record.getChildren().clear();

		// ---- Save simple fields via binding manager ----
		bindingManager.saveToRecord(record);

		// ---- Complex fields: manual save ----

		// VALUE (required)
		String value = valueArea.getText().trim();
		if(!value.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "VALUE", value);
		}

		// TRANSLATION
		FLEFRecordUtils.removeChildren(record, "TRANSLATION");
		for(TranslationEntry entry : translationEntries){
			FLEFRecord translation = FLEFRecord.createChild(1, "TRANSLATION");

			if(entry.locale != null && !entry.locale.isEmpty()){
				FLEFRecord localeChild = FLEFRecord.createChildWithValue(2, "LOCALE", entry.locale);
				translation.addChild(localeChild);
			}

			if(entry.value != null && !entry.value.isEmpty()){
				FLEFRecord valueChild = FLEFRecord.createChildWithValue(2, "VALUE", entry.value);
				translation.addChild(valueChild);
			}

			record.addChild(translation);
		}

		// SOURCE_CITATION
		for(FLEFRecord citation : sourceCitations){
			citation.setLevel(1);
			citation.setTag("SOURCE_CITATION");
			record.addChild(citation);
		}

		// RESTRICTION_STRUCTURE
		if(restrictionPanel.hasData()){
			FLEFRecord restriction = restrictionPanel.saveToRecord(null);
			if(restriction != null){
				restriction.setLevel(1);
				restriction.setTag("RESTRICTION");
				record.addChild(restriction);
			}
		}

		// MODIFICATION_STRUCTURE
		modificationPanel.saveToRecord(record);

		if(isNew){
			model.addRecord(record);
		}

		dispose();
	}

	// ==================== Overrides ====================
	@Override
	protected FLEFRecord createNewRecord(){
		return FLEFRecord.createMainRecord(generateNewId(), NoteHandler.TYPE);
	}

	@Override
	protected String generateNewId(){
		return FLEFRecordUtils.generateNewId(model, NoteHandler.TYPE, NoteHandler.ID_PREFIX);
	}

	// ==================== Main test ====================
	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Note Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New Note");
			btn.addActionListener(e -> {
				NoteDialog dialog = NoteDialog.createNew(frame, model);
				dialog.setVisible(true);
				System.out.println("Note saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
