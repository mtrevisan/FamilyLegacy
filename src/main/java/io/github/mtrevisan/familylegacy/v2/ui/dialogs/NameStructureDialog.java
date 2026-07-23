package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
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
 *       +3 LOCALE <LOCALE_CODE>    {0:1}
 *       +3 VALID_FROM    {0:1}
 *         +4 <<DATE_STRUCTURE>>    {1:1}
 *       +3 VALID_TO    {0:1}
 *         +4 <<DATE_STRUCTURE>>    {1:1}
 *       +3 <<TEXT_VALUE_VARIANT>>    {0:M}
 *       +3 NOTE @<XREF:NOTE>@    {0:M}
 *       +3 <<SOURCE_CITATION>>    {0:M}
 *   +1 TYPE <NAME_TYPE>    {0:1}
 * </pre>
 */
public class NameStructureDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 7526263144620538539L;


	// Handlers
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

	// Dates ({0:1} under VALUE)
	private final BoundTextField validFromField = new BoundTextField("VALID_FROM", 15);
	private final BoundTextField validToField = new BoundTextField("VALID_TO", 15);

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
		super(parent, existingRecord == null ? "Add Name" : "Edit Name", ModalityType.APPLICATION_MODAL);
		this.model = model;
		this.nameRecord = (existingRecord != null ? copyRecord(existingRecord) : FLEFRecord.createChild(1, "NAME"));

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

	private JPanel createValidityAndNotesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10, fillx, top", "[grow]", "[]10[]"));

		// Validity Dates Panel
		final JPanel datePanel = new JPanel(new MigLayout("fillx, top", "[right]rel[grow]rel[right]rel[grow]"));
		datePanel.setBorder(new TitledBorder("Validity Dates"));
		datePanel.add(new JLabel("Valid From:"), "align label");
		datePanel.add(validFromField, "growx");
		datePanel.add(new JLabel("Valid To:"), "align label");
		datePanel.add(validToField, "growx");

		panel.add(datePanel, "growx, wrap");

		// Notes Panel
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
		if(idx == -1){
			return;
		}

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
			return StringUtils.isNotBlank(title) ? title + " (" + noteId + ")" : noteId;
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

	private Frame getParentFrame(){
		Container parent = getParent();
		Window w = SwingUtilities.getWindowAncestor(parent);
		if(w instanceof Frame){
			return (Frame)w;
		}
		return null;
//		Container parent = getParent();
//		while(parent != null && !(parent instanceof Frame)){
//			parent = parent.getParent();
//		}
//		return (Frame)parent;
	}

	private void editSource(){
		final int idx = sourceList.getSelectedIndex();
		if(idx == -1){
			return;
		}

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
		if(idx == -1){
			return;
		}

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
			return (rec != null && sourceHandler != null ? sourceHandler.getDisplayName(rec) : rawSourceId);
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
			final FLEFRecord validFromRec = FLEFRecordUtils.findChild(valueRec, "VALID_FROM");
			if(validFromRec != null){
				validFromField.setText(FLEFRecordUtils.getChildValue(validFromRec, "DATE"));
			}

			// 2 VALUE -> 3 VALID_TO
			final FLEFRecord validToRec = FLEFRecordUtils.findChild(valueRec, "VALID_TO");
			if(validToRec != null){
				validToField.setText(FLEFRecordUtils.getChildValue(validToRec, "DATE"));
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

		// 3 VALID_FROM / VALID_TO
		if(StringUtils.isNotBlank(validFromField.getText())){
			final FLEFRecord validFrom = FLEFRecord.createChild(3, "VALID_FROM");
			validFrom.addChild(FLEFRecord.createChildWithValue(4, "DATE", validFromField.getText().trim()));
			valueRec.addChild(validFrom);
		}
		if(StringUtils.isNotBlank(validToField.getText())){
			final FLEFRecord validTo = FLEFRecord.createChild(3, "VALID_TO");
			validTo.addChild(FLEFRecord.createChildWithValue(4, "DATE", validToField.getText().trim()));
			valueRec.addChild(validTo);
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
		dispose();
	}

	public boolean isSaved(){
		return saved;
	}

	public FLEFRecord getNameRecord(){
		return nameRecord;
	}

}
