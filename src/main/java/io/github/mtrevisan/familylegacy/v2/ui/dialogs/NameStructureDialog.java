package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.DatePanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;


public class NameStructureDialog extends JDialog {

	@Serial
	private static final long serialVersionUID = 7526263144620538539L;

	static {
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
	}

	private final BindingManager bindingManager = new BindingManager();
	private final FLEFModel model;
	private final FLEFRecord nameRecord;
	private boolean saved;

	// Simple bound fields
	private final BoundTextField valueField;
	private final BoundComboBox<String> typeCombo;
	private final BoundComboBox<String> localeCombo;

	// Validity Date Display Fields & Records
	private final JTextField validFromDisplayField = new JTextField(20);
	private final JTextField validToDisplayField = new JTextField(20);
	private FLEFRecord validFromDateRecord;
	private FLEFRecord validToDateRecord;

	// Variants
	private final DefaultListModel<String> variantListModel = new DefaultListModel<>();
	private final JList<String> variantList = new JList<>(variantListModel);
	private final List<FLEFRecord> variantRecords = new ArrayList<>();

	// Notes & Source Citations
	private final NoteListPanel notePanel;
	private final SourceCitationListPanel sourcePanel;

	private final JTabbedPane tabbedPane = new JTabbedPane();

	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	public NameStructureDialog(final Window parent, final FLEFModel model, final FLEFRecord existingRecord) {
		super(parent, existingRecord == null ? "Add Name" : "Edit Name", ModalityType.APPLICATION_MODAL);

		this.model = model;
		this.nameRecord = (existingRecord != null) ? copyRecord(existingRecord) : FLEFRecord.createChild("NAME");

		valueField = new BoundTextField("VALUE", 30);
		typeCombo = new BoundComboBox<>("TYPE", new String[]{StringUtils.EMPTY, "official", "colonial", "indigenous"});
		localeCombo = new BoundComboBox<>("VALUE.LOCALE", new String[]{StringUtils.EMPTY, "en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"});

		notePanel = new NoteListPanel(model, this);
		sourcePanel = new SourceCitationListPanel(model, this);

		initComponents();
		loadData();
		pack();
		setLocationRelativeTo(parent);
	}

	private static FLEFRecord copyRecord(final FLEFRecord original) {
		final FLEFRecord copy = FLEFRecord.createChildWithValue(original.getTag(), original.getValue());
		for (final FLEFRecord child : original.getChildren()) {
			copy.addChild(child);
		}
		return copy;
	}

	private void initComponents() {
		bindingManager.bind(valueField);
		bindingManager.bind(typeCombo);
		bindingManager.bind(localeCombo);

		setupDateField(validFromDisplayField, () -> validFromDateRecord != null, this::editValidFrom, this::clearValidFrom);
		setupDateField(validToDisplayField, () -> validToDateRecord != null, this::editValidTo, this::clearValidTo);

		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("Variants", createVariantsPanel());
		tabbedPane.addTab("References", createReferencesPanel());

		setLayout(new MigLayout("ins 10, fillx, top", "[grow]", "[]10[]"));
		add(tabbedPane, "growx, wrap");

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, "growx");

		saveButton.addActionListener(e -> save());
		cancelButton.addActionListener(e -> dispose());
	}

	private void setupDateField(JTextField field, Supplier<Boolean> hasSelection, Runnable editAction, Runnable clearAction) {
		field.setEditable(false);
		field.setBackground(UIManager.getColor("TextField.background"));
		GUIHelper.installBehavior(field,
			hasSelection,
			editAction,
			editAction,
			clearAction,
			builder -> {
				builder.item("Set Date...", editAction);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", editAction);
				builder.selectionSensitiveItem("Clear", clearAction);
			});
	}

	private JPanel createMainPanel() {
		JPanel panel = new JPanel(new MigLayout("ins 10, fillx, top", "[right]rel[grow]", "[]5[]5[]10[]"));

		panel.add(new JLabel("Name Value*:"), "align label");
		panel.add(valueField, "growx, wrap");

		typeCombo.setEditable(true);
		panel.add(new JLabel("Type:"), "align label");
		panel.add(typeCombo, "growx, wrap");

		localeCombo.setEditable(true);
		panel.add(new JLabel("Locale:"), "align label");
		panel.add(localeCombo, "growx, wrap");

		// Validity Range Panel
		JPanel validityPanel = new JPanel(new MigLayout("ins 5, fillx, top", "[right]rel[grow]", "[]5[]"));
		validityPanel.setBorder(BorderFactory.createTitledBorder("Validity Range"));

		validityPanel.add(new JLabel("Valid From:"), "align label");
		validityPanel.add(validFromDisplayField, "growx, wrap");

		validityPanel.add(new JLabel("Valid To:"), "align label");
		validityPanel.add(validToDisplayField, "growx, wrap");

		panel.add(validityPanel, "span 2, growx, wrap");

		return panel;
	}

	// ==================== Date Handlers ====================

	private void editValidFrom() {
		FLEFRecord updated = showDateDialog("Valid From Date", validFromDateRecord);
		if (updated != null) {
			validFromDateRecord = updated;
			updateDateDisplay(validFromDisplayField, validFromDateRecord);
		}
	}

	private void clearValidFrom() {
		validFromDateRecord = null;
		validFromDisplayField.setText(StringUtils.EMPTY);
	}

	private void editValidTo() {
		FLEFRecord updated = showDateDialog("Valid To Date", validToDateRecord);
		if (updated != null) {
			validToDateRecord = updated;
			updateDateDisplay(validToDisplayField, validToDateRecord);
		}
	}

	private void clearValidTo() {
		validToDateRecord = null;
		validToDisplayField.setText(StringUtils.EMPTY);
	}

	private FLEFRecord showDateDialog(String title, FLEFRecord existingRecord) {
		DateDialog dialog = DateDialog.createEdit(this, model, title, existingRecord);
		dialog.setVisible(true);
		return dialog.isSaved() ? dialog.getDateRecord() : existingRecord;
	}

	private void updateDateDisplay(JTextField field, FLEFRecord dateRecord){
		if(dateRecord == null){
			field.setText(StringUtils.EMPTY);
			return;
		}

		field.setText(DatePanel.getDisplayText(dateRecord));
	}

	// ==================== Variants & References ====================

	private JPanel createVariantsPanel() {
		JPanel panel = new JPanel(new MigLayout("ins 10, fillx, top"));
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

	private JPanel createReferencesPanel() {
		JPanel panel = new JPanel(new MigLayout("ins 5, fillx, top, wrap 1", "[grow]", "[]5[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		panel.add(notePanel, "growx");
		panel.add(sourcePanel, "growx");
		return panel;
	}

	private void addVariant() {
		TextValueVariantDialog dialog = new TextValueVariantDialog(this, null);
		dialog.setVisible(true);
		if (dialog.isSaved()) {
			FLEFRecord record = dialog.getVariantRecord();
			if (record != null) {
				variantRecords.add(record);
				variantListModel.addElement(getVariantDisplay(record));
			}
		}
	}

	private void editVariant() {
		int idx = variantList.getSelectedIndex();
		if (idx == -1) return;

		FLEFRecord existing = variantRecords.get(idx);
		TextValueVariantDialog dialog = new TextValueVariantDialog(this, existing);
		dialog.setVisible(true);

		if (dialog.isSaved()) {
			FLEFRecord updated = dialog.getVariantRecord();
			if (updated != null) {
				variantRecords.set(idx, updated);
				variantListModel.set(idx, getVariantDisplay(updated));
			}
		}
	}

	private void removeVariant() {
		int idx = variantList.getSelectedIndex();
		if (idx >= 0) {
			variantRecords.remove(idx);
			variantListModel.remove(idx);
		}
	}

	private String getVariantDisplay(final FLEFRecord record) {
		if ("PHONETIC".equals(record.getTag())) {
			String val = FLEFRecordUtils.getChildValue(record, "VALUE");
			return "[Phonetic - " + record.getValue() + "]: " + (val != null ? val : StringUtils.EMPTY);
		} else if ("TRANSCRIPTION".equals(record.getTag())) {
			String type = FLEFRecordUtils.getChildValue(record, "TYPE");
			String val = FLEFRecordUtils.getChildValue(record, "VALUE");
			return "[Transcription - " + record.getValue() + (StringUtils.isNotBlank(type) ? " (" + type + ")" : StringUtils.EMPTY) + "]: " + (val != null ? val : StringUtils.EMPTY);
		}
		return record.getTag();
	}

	// ==================== Data Loading & Saving ====================

	private void loadData() {
		bindingManager.loadFromRecord(nameRecord);

		FLEFRecord valueRec = FLEFRecordUtils.findChild(nameRecord, "VALUE");
		if (valueRec == null) {
			clearValidFrom();
			clearValidTo();
			variantRecords.clear();
			variantListModel.clear();
			notePanel.clear();
			sourcePanel.clear();
			return;
		}

		// Load VALID_FROM
		FLEFRecord validFrom = FLEFRecordUtils.findChild(valueRec, "VALID_FROM");
		if (validFrom != null) {
			validFromDateRecord = FLEFRecordUtils.findChild(validFrom, "DATE");
			updateDateDisplay(validFromDisplayField, validFromDateRecord);
		} else {
			clearValidFrom();
		}

		// Load VALID_TO
		FLEFRecord validTo = FLEFRecordUtils.findChild(valueRec, "VALID_TO");
		if (validTo != null) {
			validToDateRecord = FLEFRecordUtils.findChild(validTo, "DATE");
			updateDateDisplay(validToDisplayField, validToDateRecord);
		} else {
			clearValidTo();
		}

		// Load variants
		variantRecords.clear();
		variantListModel.clear();
		for (FLEFRecord child : valueRec.getChildren()) {
			if ("PHONETIC".equals(child.getTag()) || "TRANSCRIPTION".equals(child.getTag())) {
				variantRecords.add(child);
				variantListModel.addElement(getVariantDisplay(child));
			}
		}

		// Load notes
		List<String> noteIds = new ArrayList<>();
		for (FLEFRecord child : valueRec.getChildren()) {
			if ("NOTE".equals(child.getTag())) {
				String rawId = FLEFRecordUtils.extractXRef(child.getValue());
				if (rawId != null) noteIds.add(rawId);
			}
		}
		notePanel.loadFromNoteIds(noteIds);

		// Load source citations
		List<FLEFRecord> citations = new ArrayList<>();
		for (FLEFRecord child : valueRec.getChildren()) {
			if ("SOURCE".equals(child.getTag())) {
				citations.add(child);
			}
		}
		sourcePanel.loadFromCitations(citations);
	}

	private void save() {
		String text = valueField.getText().trim();
		if (text.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Name value cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		FLEFRecordUtils.removeAllChildren(nameRecord);
		bindingManager.saveToRecord(nameRecord);

		FLEFRecord valueRec = FLEFRecordUtils.findChild(nameRecord, "VALUE");
		if (valueRec == null) {
			valueRec = FLEFRecord.createChildWithValue("VALUE", text);
			nameRecord.addChild(valueRec);
		} else {
			valueRec.setValue(text);
		}

		FLEFRecordUtils.removeChildren(valueRec, "VALID_FROM", "VALID_TO", "PHONETIC", "TRANSCRIPTION", "NOTE", "SOURCE");

		// VALID_FROM
		if (validFromDateRecord != null) {
			FLEFRecord validFrom = FLEFRecord.createChild("VALID_FROM");
			validFrom.addChild(validFromDateRecord);
			valueRec.addChild(validFrom);
		}

		// VALID_TO
		if (validToDateRecord != null) {
			FLEFRecord validTo = FLEFRecord.createChild("VALID_TO");
			validTo.addChild(validToDateRecord);
			valueRec.addChild(validTo);
		}

		// Variants
		for (FLEFRecord variant : variantRecords) {
			valueRec.addChild(variant);
		}

		// Notes
		for (String noteId : notePanel.getNoteIds()) {
			FLEFRecordUtils.addReferenceChild(valueRec, "NOTE", noteId);
		}

		// Source citations
		for (FLEFRecord citation : sourcePanel.getCitations()) {
			citation.setTag("SOURCE");
			valueRec.addChild(citation);
		}

		saved = true;
		dispose();
	}

	public boolean isSaved() {
		return saved;
	}

	public FLEFRecord getNameRecord() {
		return nameRecord;
	}
}