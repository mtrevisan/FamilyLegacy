package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.FLEFFile;
import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
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

	private final FLEFModel model;
	private final FLEFRecord nameRecord;
	private boolean saved;

	// Main / TEXT_VALUE components
	private final BoundTextField valueField = new BoundTextField("VALUE", 30);
	private final BoundComboBox<String> typeCombo = new BoundComboBox<>(
		"TYPE", new String[]{StringUtils.EMPTY, "official", "colonial", "indigenous"}
	);
	private final BoundComboBox<String> localeCombo = new BoundComboBox<>(
		"LOCALE", new String[]{StringUtils.EMPTY, "en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"}
	);

	// Validity Dates using DatePanel
	private DatePanel validFromDatePanel;
	private DatePanel validToDatePanel;
	private JTabbedPane validityTabbedPane;

	// Variants {0:M} (under VALUE)
	private final DefaultListModel<String> variantListModel = new DefaultListModel<>();
	private final JList<String> variantList = new JList<>(variantListModel);
	private final List<FLEFRecord> variantRecords = new ArrayList<>();

	// Notes {0:M} (under VALUE)
	private final DefaultListModel<String> noteListModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteListModel);
	private final List<String> noteIds = new ArrayList<>();

	// Source Citations {0:M} (under VALUE)
	private final DefaultListModel<String> sourceListModel = new DefaultListModel<>();
	private final JList<String> sourceList = new JList<>(sourceListModel);
	private final List<FLEFRecord> sourceCitations = new ArrayList<>();

	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JButton okButton = new JButton("OK");
	private final JButton cancelButton = new JButton("Cancel");

	public NameStructureDialog(final Window parent, final FLEFModel model, final FLEFRecord existingRecord){
		super(parent, existingRecord == null? "Add Name": "Edit Name", ModalityType.APPLICATION_MODAL);
		this.model = model;
		this.nameRecord = (existingRecord != null? copyRecord(existingRecord): FLEFRecord.createChild(1, "NAME"));

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

	private void initComponents(){
		tabbedPane.addTab("General", createGeneralPanel());
		tabbedPane.addTab("Variants", createVariantsPanel());
		tabbedPane.addTab("Validity & Notes", createValidityAndNotesPanel());
		tabbedPane.addTab("Sources", createSourcesPanel());

		setLayout(new MigLayout("ins 10, fillx, top", "[grow]", "[]10[]"));
		add(tabbedPane, "growx, wrap");

		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, "growx");

		okButton.addActionListener(e -> save());
		cancelButton.addActionListener(e -> dispose());
	}

	private JPanel createGeneralPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10, fillx, top", "[right]rel[grow]", "[]5[]5[]"));

		// VALUE <TEXT>
		panel.add(new JLabel("Name Value*:"), "align label");
		panel.add(valueField, "growx, wrap");

		// TYPE <NAME_TYPE>
		typeCombo.setEditable(true);
		panel.add(new JLabel("Type:"), "align label");
		panel.add(typeCombo, "growx, wrap");

		// LOCALE <LOCALE_CODE>
		localeCombo.setEditable(true);
		panel.add(new JLabel("Locale:"), "align label");
		panel.add(localeCombo, "growx, wrap");

		return panel;
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

	private JPanel createValidityAndNotesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10, fillx, top", "[grow]", "[]10[]"));

		// --- Tabs for Valid From / Valid To using DatePanel ---
		validityTabbedPane = new JTabbedPane();

		validFromDatePanel = new DatePanel(model, this);
		validToDatePanel = new DatePanel(model, this);

		JPanel fromWrapper = wrapDatePanel(validFromDatePanel, "Valid From");
		JPanel toWrapper = wrapDatePanel(validToDatePanel, "Valid To");

		validityTabbedPane.addTab("Valid From", fromWrapper);
		validityTabbedPane.addTab("Valid To", toWrapper);

		panel.add(validityTabbedPane, "growx, wrap");

		// --- Notes Panel ---
		final JPanel notesPanel = new JPanel(new MigLayout("fillx, top"));
		notesPanel.setBorder(new TitledBorder("Notes"));

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

		notesPanel.add(GUIHelper.createScrollPane(noteList), "growx, wrap");
		panel.add(notesPanel, "growx");

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

	private JPanel createSourcesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10, fillx, top"));
		panel.setBorder(new TitledBorder("Source Citations"));

		sourceList.setVisibleRowCount(4);
		GUIHelper.installBehavior(sourceList,
			() -> sourceList.getSelectedIndex() >= 0,
			this::editSourceCitation,
			() -> {
			},
			this::removeSourceCitation,
			builder -> {
				builder.item("New...", this::createNewSourceAndAddCitation);
				builder.item("Add Existing...", this::addSourceCitation);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editSource);
				builder.selectionSensitiveItem("Edit Citation...", this::editSourceCitation);
				builder.selectionSensitiveItem("Remove", this::removeSourceCitation);
			});

		panel.add(GUIHelper.createScrollPane(sourceList), "growx");
		return panel;
	}

	// --- Variant Management ---

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

	// --- Note Management ---

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

	// --- Source Citation Management ---

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
		final Set<String> before = new HashSet<>();
		for(final FLEFRecord rec : model.getRecordsByType("SOURCE")){
			if(rec.getId() != null){
				before.add(rec.getId());
			}
		}

		final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
		final JDialog dialog = sourceHandler.createNewDialog(null, model);
		dialog.setVisible(true);

		String newSourceId = null;
		for(final FLEFRecord rec : model.getRecordsByType("SOURCE")){
			if(rec.getId() != null && !before.contains(rec.getId())){
				newSourceId = rec.getId();
				break;
			}
		}

		if(newSourceId != null){
			final FLEFRecord citationRecord = FLEFRecord.createChildWithValue(3, "SOURCE", FLEFRecordUtils.formatXRef(newSourceId));
			final SourceCitationDialog citationDialog = new SourceCitationDialog(null, model, citationRecord);
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
			return (rec != null && sourceHandler != null? sourceHandler.getDisplayName(rec): rawSourceId);
		}
		return "[empty]";
	}

	// --- Data Loading & Saving ---

	private void loadData(){
		// 1 NAME -> 2 TYPE
		final String type = FLEFRecordUtils.getChildValue(nameRecord, "TYPE");
		if(StringUtils.isNotBlank(type)){
			typeCombo.setSelectedItem(type);
		}

		// 1 NAME -> 2 VALUE
		final FLEFRecord valueRec = FLEFRecordUtils.findChild(nameRecord, "VALUE");
		if(valueRec != null){
			valueField.setText(valueRec.getValue());

			// 2 VALUE -> 3 LOCALE
			final String locale = FLEFRecordUtils.getChildValue(valueRec, "LOCALE");
			if(StringUtils.isNotBlank(locale)){
				localeCombo.setSelectedItem(locale);
			}

			// 2 VALUE -> 3 VALID_FROM
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

			// 2 VALUE -> 3 VALID_TO
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

			// 2 VALUE -> 3 TEXT_VALUE_VARIANT (PHONETIC / TRANSCRIPTION)
			variantRecords.clear();
			variantListModel.clear();
			for(final FLEFRecord child : valueRec.getChildren()){
				if("PHONETIC".equals(child.getTag()) || "TRANSCRIPTION".equals(child.getTag())){
					variantRecords.add(child);
					variantListModel.addElement(getVariantDisplay(child));
				}
			}

			// 2 VALUE -> 3 NOTE
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

			// 2 VALUE -> 3 SOURCE
			sourceCitations.clear();
			sourceListModel.clear();
			for(final FLEFRecord child : valueRec.getChildren()){
				if("SOURCE".equals(child.getTag())){
					sourceCitations.add(child);
					sourceListModel.addElement(getSourceCitationDisplay(child));
				}
			}
		}
	}

	private void save(){
		final String text = valueField.getText().trim();
		if(text.isEmpty()){
			JOptionPane.showMessageDialog(this, "Name value cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Rebuild 1 NAME
		nameRecord.getChildren().clear();

		// 2 VALUE <TEXT>
		final FLEFRecord valueRec = FLEFRecord.createChildWithValue(2, "VALUE", text);

		// 3 LOCALE <LOCALE_CODE>
		final String locale = (String)localeCombo.getSelectedItem();
		if(StringUtils.isNotBlank(locale)){
			valueRec.addChild(FLEFRecord.createChildWithValue(3, "LOCALE", locale.trim()));
		}

		// 3 VALID_FROM
		if(validFromDatePanel.hasData()){
			final FLEFRecord dateRecord = validFromDatePanel.saveToRecord(null);
			if(dateRecord != null){
				final FLEFRecord validFrom = FLEFRecord.createChild(3, "VALID_FROM");
				final FLEFRecord dateCopy = FLEFRecordUtils.copyRecordWithLevel(dateRecord, 4);
				validFrom.addChild(dateCopy);
				valueRec.addChild(validFrom);
			}
		}

		// 3 VALID_TO
		if(validToDatePanel.hasData()){
			final FLEFRecord dateRecord = validToDatePanel.saveToRecord(null);
			if(dateRecord != null){
				final FLEFRecord validTo = FLEFRecord.createChild(3, "VALID_TO");
				final FLEFRecord dateCopy = FLEFRecordUtils.copyRecordWithLevel(dateRecord, 4);
				validTo.addChild(dateCopy);
				valueRec.addChild(validTo);
			}
		}

		// 3 <<TEXT_VALUE_VARIANT>>
		for(final FLEFRecord variant : variantRecords){
			variant.setLevel(3);
			valueRec.addChild(variant);
		}

		// 3 NOTE @<XREF:NOTE>@
		for(final String noteId : noteIds){
			FLEFRecordUtils.addReferenceChild(valueRec, "NOTE", noteId);
		}

		// 3 <<SOURCE_CITATION>>
		for(final FLEFRecord citation : sourceCitations){
			citation.setLevel(3);
			citation.setTag("SOURCE");
			valueRec.addChild(citation);
		}

		nameRecord.addChild(valueRec);

		// 2 TYPE <NAME_TYPE>
		final String type = (String)typeCombo.getSelectedItem();
		if(StringUtils.isNotBlank(type)){
			nameRecord.addChild(FLEFRecord.createChildWithValue(2, "TYPE", type.trim()));
		}

		saved = true;

//FIXME
FLEFFile.print(model);
//		dispose(); // commented for debugging
	}

	public boolean isSaved(){
		return saved;
	}

	public FLEFRecord getNameRecord(){
		return nameRecord;
	}

	// --- Sub-Dialog for TEXT_VALUE_VARIANT ---

	private static class TextValueVariantDialog extends JDialog{

		@Serial
		private static final long serialVersionUID = 1L;

		private final JRadioButton phoneticRadio = new JRadioButton("Phonetic", true);
		private final JRadioButton transcriptionRadio = new JRadioButton("Transcription");

		private final JTextField systemField = new JTextField(15); // System (IPA, romaji, etc.)
		private final JTextField typeField = new JTextField(15);   // Type (romanized, etc.)
		private final JTextField valueField = new JTextField(20);  // Piece/Text value

		private final JLabel systemLabel = new JLabel("System*:");
		private final JLabel typeLabel = new JLabel("Type:");
		private final JLabel valueLabel = new JLabel("Value*:");

		private FLEFRecord variantRecord;
		private boolean saved;

		TextValueVariantDialog(final Window parent, final FLEFRecord existing){
			super(parent, existing == null? "Add Text Value Variant": "Edit Text Value Variant", ModalityType.APPLICATION_MODAL);
			this.variantRecord = existing;
			initComponents();
			loadData();
			pack();
			setLocationRelativeTo(parent);
		}

		private void initComponents(){
			final ButtonGroup group = new ButtonGroup();
			group.add(phoneticRadio);
			group.add(transcriptionRadio);

			final JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			radioPanel.add(phoneticRadio);
			radioPanel.add(transcriptionRadio);

			final JPanel panel = new JPanel(new MigLayout("ins 10, fillx", "[right]rel[grow]", "[]5[]5[]5[]"));
			panel.add(new JLabel("Variant Kind:"), "align label");
			panel.add(radioPanel, "growx, wrap");

			panel.add(systemLabel, "align label");
			panel.add(systemField, "growx, wrap");

			panel.add(typeLabel, "align label");
			panel.add(typeField, "growx, wrap");

			panel.add(valueLabel, "align label");
			panel.add(valueField, "growx, wrap");

			phoneticRadio.addActionListener(e -> updateFieldsState());
			transcriptionRadio.addActionListener(e -> updateFieldsState());

			final JButton okBtn = new JButton("OK");
			final JButton cancelBtn = new JButton("Cancel");
			okBtn.addActionListener(e -> save());
			cancelBtn.addActionListener(e -> dispose());

			final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			buttonPanel.add(okBtn);
			buttonPanel.add(cancelBtn);

			setLayout(new BorderLayout());
			add(panel, BorderLayout.CENTER);
			add(buttonPanel, BorderLayout.SOUTH);

			updateFieldsState();
		}

		private void updateFieldsState(){
			final boolean isTranscription = transcriptionRadio.isSelected();
			typeLabel.setEnabled(isTranscription);
			typeField.setEnabled(isTranscription);
			systemLabel.setText(isTranscription? "System*:": "System*:");
		}

		private void loadData(){
			if(variantRecord == null) return;

			if("TRANSCRIPTION".equals(variantRecord.getTag())){
				transcriptionRadio.setSelected(true);
				systemField.setText(variantRecord.getValue());
				typeField.setText(FLEFRecordUtils.getChildValue(variantRecord, "TYPE"));
				valueField.setText(FLEFRecordUtils.getChildValue(variantRecord, "VALUE"));
			}
			else{
				phoneticRadio.setSelected(true);
				systemField.setText(variantRecord.getValue());
				valueField.setText(FLEFRecordUtils.getChildValue(variantRecord, "VALUE"));
			}
			updateFieldsState();
		}

		private void save(){
			final String system = systemField.getText().trim();
			final String value = valueField.getText().trim();

			if(system.isEmpty() || value.isEmpty()){
				JOptionPane.showMessageDialog(this, "System and Value fields are required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			if(phoneticRadio.isSelected()){
				variantRecord = FLEFRecord.createChildWithValue(3, "PHONETIC", system);
				variantRecord.addChild(FLEFRecord.createChildWithValue(4, "VALUE", value));
			}
			else{
				variantRecord = FLEFRecord.createChildWithValue(3, "TRANSCRIPTION", system);
				final String type = typeField.getText().trim();
				if(!type.isEmpty()){
					variantRecord.addChild(FLEFRecord.createChildWithValue(4, "TYPE", type));
				}
				variantRecord.addChild(FLEFRecord.createChildWithValue(4, "VALUE", value));
			}

			saved = true;
			dispose();
		}

		public boolean isSaved(){
			return saved;
		}

		public FLEFRecord getVariantRecord(){
			return variantRecord;
		}
	}

}
