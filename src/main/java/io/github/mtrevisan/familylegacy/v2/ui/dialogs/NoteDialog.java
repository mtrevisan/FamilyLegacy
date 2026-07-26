package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/**
 * Dialog for editing a {@code NOTE_RECORD} according to FLEF 0.1.0.
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

	// Source Citations (using SourceCitationListPanel)
	private final SourceCitationListPanel sourceCitationPanel;

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
			if(!locale.isEmpty()) sb.append("[").append(locale).append("] ");
			String display = value;
			if(display.length() > 50) display = display.substring(0, 47) + "...";
			sb.append(display);
			return sb.toString();
		}
	}

	// ==================== Constructors ====================

	public static NoteDialog createNew(final Frame parent, final FLEFModel model){
		return new NoteDialog(parent, model, null);
	}

	public static NoteDialog createEdit(final Frame parent, final FLEFModel model, final FLEFRecord record){
		if(record == null) throw new IllegalArgumentException("Record cannot be null");
		return new NoteDialog(parent, model, record);
	}


	private NoteDialog(final Frame parent, final FLEFModel model, final FLEFRecord record){
		super(parent, buildTitle(model, record), model, record);

		restrictionPanel = new RestrictionPanel(this);
		modificationPanel = new ModificationPanel(this);
		sourceCitationPanel = new SourceCitationListPanel(model, this);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	private static String buildTitle(final FLEFModel model, final FLEFRecord record){
		return (record == null? "New Note": "Edit Note - " + record.getId());
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

		JPanel restrictionContainer = new JPanel(new MigLayout("top", "[grow]", "[grow]"));
		restrictionContainer.add(restrictionPanel, "grow");
		tabbedPane.addTab("Restriction", restrictionContainer);

		JPanel modificationContainer = new JPanel(new MigLayout("top", "[grow]", "[grow]"));
		modificationContainer.add(modificationPanel, "grow");
		tabbedPane.addTab("Modification", modificationContainer);

		setLayout(new MigLayout("fillx,top"));
		add(tabbedPane, "growx");

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
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
		mainPanel.add(GUIHelper.createScrollPane(valueArea), "growx, growy, wrap");

		// MIME
		mainPanel.add(new JLabel("MIME:"), "align label");
		mainPanel.add(mimeCombo, "growx,wrap");

		// LOCALE
		mainPanel.add(new JLabel("Locale:"), "align label");
		mainPanel.add(localeCombo, "growx,wrap");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 5,fillx,top,wrap 1", "[grow]", "[]5[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		panel.add(createTranslationsPanel(), "growx");

		// Source Citations panel
		panel.add(sourceCitationPanel, "growx");

		return panel;
	}

	private JPanel createTranslationsPanel(){
		JPanel panel = new JPanel(new MigLayout("fillx,top"));
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

		panel.add(GUIHelper.createScrollPane(translationList), "growx,wrap");
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
	 * @return the updated entry, or {@code null} if canceled
	 */
	private TranslationEntry showTranslationDialog(final TranslationEntry initial){
		JDialog dialog = new JDialog(this, initial == null? "Add Translation": "Edit Translation", true);
		dialog.setLayout(new MigLayout("ins 10,fillx", "[right]rel[grow]", "[]10[]"));

		BoundTextArea valueArea = new BoundTextArea("VALUE", 5, 25);
		valueArea.setLineWrap(true);
		valueArea.setWrapStyleWord(true);
		if(initial != null) valueArea.setText(initial.value);
		dialog.add(new JLabel("Value*:"), "align label,top");
		dialog.add(GUIHelper.createScrollPane(valueArea), "growx,wrap");

		BoundComboBox<String> localeCombo = new BoundComboBox<>("LOCALE", new String[]{StringUtils.EMPTY, "en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"});
		localeCombo.setEditable(true);
		if(initial != null && !initial.locale.isEmpty()) localeCombo.setSelectedItem(initial.locale);
		dialog.add(new JLabel("Locale:"), "align label");
		dialog.add(localeCombo, "growx,wrap");

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

	// ==================== Load / Save ====================

	@Override
	protected void loadData(){
		setTitle(buildTitle(model, record));

		// ---- Simple fields: load via binding manager ----
		bindingManager.loadFromRecord(record);

		// Translations
		translationEntries.clear();
		translationModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("TRANSLATION".equals(child.getTag())){
				String translationLocale = FLEFRecordUtils.getChildValue(child, "LOCALE");
				String translationValue = FLEFRecordUtils.getChildValue(child, "VALUE");
				if(translationValue != null && !translationValue.isEmpty()){
					translationEntries.add(new TranslationEntry(translationLocale, translationValue));
					translationModel.addElement(translationEntries.get(translationEntries.size() - 1));
				}
			}
		}

		// Source Citations
		List<FLEFRecord> citations = new ArrayList<>();
		for(FLEFRecord child : record.getChildren()){
			if("SOURCE".equals(child.getTag())){
				citations.add(child);
			}
		}
		sourceCitationPanel.loadFromCitations(citations);

		// Restriction & Modification
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

		// Translations
		FLEFRecordUtils.removeChildren(record, "TRANSLATION");
		for(int i = 0; i < translationEntries.size(); i++){
			TranslationEntry entry = translationEntries.get(i);
			FLEFRecordUtils.addChild(record, "TRANSLATION[" + i + "].VALUE", entry.value);
			FLEFRecordUtils.addChild(record, "TRANSLATION[" + i + "].LOCALE", entry.locale);
		}

		// Source Citations
		for(FLEFRecord citation : sourceCitationPanel.getCitations()){
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
