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
import java.util.List;


/**
 * Dialog for editing a {@code NAME_STRUCTURE} according to FLEF 0.1.0.
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

	// Simple bound fields
	private final BoundTextField valueField;
	private final BoundComboBox<String> typeCombo;
	private final BoundComboBox<String> localeCombo;

	// Validity Dates
	private DatePanel validFromDatePanel;
	private DatePanel validToDatePanel;

	// Variants
	private final DefaultListModel<String> variantListModel = new DefaultListModel<>();
	private final JList<String> variantList = new JList<>(variantListModel);
	private final List<FLEFRecord> variantRecords = new ArrayList<>();

	// Notes & Source Citations (using new panels)
	private final NoteListPanel notePanel;
	private final SourceCitationListPanel sourcePanel;

	private final JTabbedPane tabbedPane = new JTabbedPane();

	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	// ==================== Constructors ====================

	public NameStructureDialog(final Window parent, final FLEFModel model, final FLEFRecord existingRecord){
		super(parent, existingRecord == null? "Add Name": "Edit Name", ModalityType.APPLICATION_MODAL);

		this.model = model;
		this.nameRecord = (existingRecord != null)? copyRecord(existingRecord): FLEFRecord.createChild("NAME");

		valueField = new BoundTextField("VALUE", 30);
		typeCombo = new BoundComboBox<>("TYPE", new String[]{StringUtils.EMPTY, "official", "colonial", "indigenous"});
		localeCombo = new BoundComboBox<>("VALUE.LOCALE", new String[]{StringUtils.EMPTY, "en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"});

		// Initialize sub-panels
		notePanel = new NoteListPanel(model, this);
		sourcePanel = new SourceCitationListPanel(model, this);

		initComponents();
		loadData();
		pack();
		setLocationRelativeTo(parent);
	}

	private static FLEFRecord copyRecord(final FLEFRecord original){
		final FLEFRecord copy = FLEFRecord.createChildWithValue(original.getTag(), original.getValue());
		for(final FLEFRecord child : original.getChildren()){
			copy.addChild(child);
		}
		return copy;
	}

	// ==================== UI Initialization ====================

	private void initComponents(){
		bindingManager.bind(valueField);
		bindingManager.bind(typeCombo);
		bindingManager.bind(localeCombo);

		tabbedPane.addTab("General", createGeneralPanel());
		tabbedPane.addTab("Validity", createValidityPanel());
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

	private JPanel createGeneralPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10, fillx, top", "[right]rel[grow]", "[]5[]5[]5"));

		panel.add(new JLabel("Name Value*:"), "align label");
		panel.add(valueField, "growx, wrap");

		typeCombo.setEditable(true);
		panel.add(new JLabel("Type:"), "align label");
		panel.add(typeCombo, "growx, wrap");

		localeCombo.setEditable(true);
		panel.add(new JLabel("Locale:"), "align label");
		panel.add(localeCombo, "growx, wrap");

		return panel;
	}

	private JPanel createValidityPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10, fillx, top", "[grow]", "[]"));
		panel.setBorder(new TitledBorder("Validity Dates"));

		validFromDatePanel = new DatePanel(this, model);
		validToDatePanel = new DatePanel(this, model);

		JPanel fromWrapper = wrapDatePanel(validFromDatePanel, "Valid From");
		JPanel toWrapper = wrapDatePanel(validToDatePanel, "Valid To");

		JTabbedPane validityTabbedPane = new JTabbedPane();
		validityTabbedPane.addTab("Valid From", fromWrapper);
		validityTabbedPane.addTab("Valid To", toWrapper);

		panel.add(validityTabbedPane, "growx");
		return panel;
	}

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

	private JPanel createReferencesPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 5, fillx, top, wrap 1", "[grow]", "[]5[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// Notes
		panel.add(notePanel, "growx");

		// Source Citations
		panel.add(sourcePanel, "growx");

		return panel;
	}

	// ==================== Variant Management ====================

	private void addVariant(){
		TextValueVariantDialog dialog = new TextValueVariantDialog(this, null);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord record = dialog.getVariantRecord();
			if(record != null){
				variantRecords.add(record);
				variantListModel.addElement(getVariantDisplay(record));
			}
		}
	}

	private void editVariant(){
		int idx = variantList.getSelectedIndex();
		if(idx == -1) return;

		FLEFRecord existing = variantRecords.get(idx);
		TextValueVariantDialog dialog = new TextValueVariantDialog(this, existing);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			FLEFRecord updated = dialog.getVariantRecord();
			if(updated != null){
				variantRecords.set(idx, updated);
				variantListModel.set(idx, getVariantDisplay(updated));
			}
		}
	}

	private void removeVariant(){
		int idx = variantList.getSelectedIndex();
		if(idx >= 0){
			variantRecords.remove(idx);
			variantListModel.remove(idx);
		}
	}

	private String getVariantDisplay(final FLEFRecord record){
		if("PHONETIC".equals(record.getTag())){
			String val = FLEFRecordUtils.getChildValue(record, "VALUE");
			return "[Phonetic - " + record.getValue() + "]: " + (val != null? val: "");
		}
		else if("TRANSCRIPTION".equals(record.getTag())){
			String type = FLEFRecordUtils.getChildValue(record, "TYPE");
			String val = FLEFRecordUtils.getChildValue(record, "VALUE");
			return "[Transcription - " + record.getValue() + (StringUtils.isNotBlank(type)? " (" + type + ")": "") + "]: " + (val != null? val: "");
		}
		return record.getTag();
	}

	// ==================== Data Loading & Saving ====================

	private void loadData(){
		bindingManager.loadFromRecord(nameRecord);

		FLEFRecord valueRec = FLEFRecordUtils.findChild(nameRecord, "VALUE");
		if(valueRec == null){
			validFromDatePanel.clear();
			validToDatePanel.clear();
			variantRecords.clear();
			variantListModel.clear();
			notePanel.clear();
			sourcePanel.clear();
			return;
		}

		// Load VALID_FROM
		FLEFRecord validFrom = FLEFRecordUtils.findChild(valueRec, "VALID_FROM");
		if(validFrom != null){
			FLEFRecord dateRec = FLEFRecordUtils.findChild(validFrom, "DATE");
			if(dateRec != null) validFromDatePanel.loadFromRecord(dateRec);
		}
		else{
			validFromDatePanel.clear();
		}

		// Load VALID_TO
		FLEFRecord validTo = FLEFRecordUtils.findChild(valueRec, "VALID_TO");
		if(validTo != null){
			FLEFRecord dateRec = FLEFRecordUtils.findChild(validTo, "DATE");
			if(dateRec != null) validToDatePanel.loadFromRecord(dateRec);
		}
		else{
			validToDatePanel.clear();
		}

		// Load variants
		variantRecords.clear();
		variantListModel.clear();
		for(FLEFRecord child : valueRec.getChildren()){
			if("PHONETIC".equals(child.getTag()) || "TRANSCRIPTION".equals(child.getTag())){
				variantRecords.add(child);
				variantListModel.addElement(getVariantDisplay(child));
			}
		}

		// Load notes
		List<String> noteIds = new ArrayList<>();
		for(FLEFRecord child : valueRec.getChildren()){
			if("NOTE".equals(child.getTag())){
				String rawId = FLEFRecordUtils.extractXRef(child.getValue());
				if(rawId != null) noteIds.add(rawId);
			}
		}
		notePanel.loadFromNoteIds(noteIds);

		// Load source citations
		List<FLEFRecord> citations = new ArrayList<>();
		for(FLEFRecord child : valueRec.getChildren()){
			if("SOURCE".equals(child.getTag())){
				citations.add(child);
			}
		}
		sourcePanel.loadFromCitations(citations);
	}

	private void save(){
		String text = valueField.getText().trim();
		if(text.isEmpty()){
			JOptionPane.showMessageDialog(this, "Name value cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		FLEFRecordUtils.removeAllChildren(nameRecord);

		// Save simple fields via BindingManager
		bindingManager.saveToRecord(nameRecord);

		// Ensure VALUE child exists
		FLEFRecord valueRec = FLEFRecordUtils.findChild(nameRecord, "VALUE");
		if(valueRec == null){
			valueRec = FLEFRecord.createChildWithValue("VALUE", text);
			nameRecord.addChild(valueRec);
		}
		else{
			valueRec.setValue(text);
		}

		// Remove children we will rebuild
		FLEFRecordUtils.removeChildren(valueRec, "VALID_FROM", "VALID_TO", "PHONETIC", "TRANSCRIPTION", "NOTE", "SOURCE");

		// VALID_FROM
		if(validFromDatePanel.hasData()){
			FLEFRecord dateRecord = validFromDatePanel.saveToRecord(null);
			if(dateRecord != null){
				FLEFRecord validFrom = FLEFRecord.createChild("VALID_FROM");
				validFrom.addChild(dateRecord);
				valueRec.addChild(validFrom);
			}
		}

		// VALID_TO
		if(validToDatePanel.hasData()){
			FLEFRecord dateRecord = validToDatePanel.saveToRecord(null);
			if(dateRecord != null){
				FLEFRecord validTo = FLEFRecord.createChild("VALID_TO");
				validTo.addChild(dateRecord);
				valueRec.addChild(validTo);
			}
		}

		// Variants
		for(FLEFRecord variant : variantRecords){
			valueRec.addChild(variant);
		}

		// Notes
		for(String noteId : notePanel.getNoteIds()){
			FLEFRecordUtils.addReferenceChild(valueRec, "NOTE", noteId);
		}

		// Source citations
		for(FLEFRecord citation : sourcePanel.getCitations()){
			citation.setTag("SOURCE");
			valueRec.addChild(citation);
		}

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
