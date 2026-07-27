package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.FLEFFile;
import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;

import io.github.mtrevisan.familylegacy.v2.ui.components.DatePanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;


/**
 * Dialog for editing a {@code CULTURAL_NORM_RECORD} according to FLEF 0.1.0.
 * <p>
 * Structure:
 * <pre>
 * CULTURAL_NORM_RECORD :=
 *   n @<XREF:CULTURAL_NORM>@ CULTURAL_NORM    {1:1}
 *     +1 TITLE <CULTURAL_NORM_DESCRIPTIVE_TITLE>    {0:1}
 *     +1 <<PLACE_STRUCTURE>>    {0:1}
 *     +1 VALID_FROM    {0:1}
 *       +2 <<DATE_STRUCTURE>>    {1:1}
 *     +1 VALID_TO    {0:1}
 *       +2 <<DATE_STRUCTURE>>    {1:1}
 *     +1 NOTE @<XREF:NOTE>@    {0:M}
 *     +1 <<EVIDENCE_QUALIFIERS>>    {0:1}
 *     +1 <<SOURCE_CITATION>>    {0:M}
 *     +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class CulturalNormDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 950729006569948384L;


	// Handlers
	static{
		HandlerRegistry.register(new CulturalNormHandler());
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final Frame parent;
	private final BoundTextField titleField = new BoundTextField("TITLE", 30);
	private final JTextField placeDisplayField = new JTextField(20);
	private FLEFRecord placeStructureRecord;
	private final EvidenceQualifiersPanel placeQualifiers = new EvidenceQualifiersPanel("Evidence");
	private final EvidenceQualifiersPanel culturalNormQualifiers = new EvidenceQualifiersPanel("Evidence");
	private final JTextField validFromDisplayField = new JTextField(20);
	private final JTextField validToDisplayField = new JTextField(20);
	private FLEFRecord validFromDateRecord;
	private FLEFRecord validToDateRecord;
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]5[]5[]5[]"));
	private final NoteListPanel notePanel;
	private final SourceCitationListPanel sourceCitationPanel;
	private final ModificationPanel modificationPanel;


	public static CulturalNormDialog createNew(final Frame parent, final FLEFModel model){
		return new CulturalNormDialog(parent, model, null);
	}

	public static CulturalNormDialog createEdit(final Frame parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new CulturalNormDialog(parent, model, record);
	}

	private CulturalNormDialog(final Frame parent, final FLEFModel model, final FLEFRecord record){
		super(parent, buildTitle(record), model, record, HandlerRegistry.getHandler(CulturalNormHandler.TYPE));

		this.parent = parent;

		modificationPanel = new ModificationPanel(this);
		notePanel = new NoteListPanel(model, this);
		sourceCitationPanel = new SourceCitationListPanel(model, this);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	private static String buildTitle(final FLEFRecord record){
		return (record == null? "New Cultural Norm": "Edit Cultural Norm - " + record.getId());
	}

	@Override
	protected void initComponents(){
		bindingManager.bind(titleField);

		setupDateField(validFromDisplayField, () -> validFromDateRecord != null, this::newValidFrom,
			this::editValidFrom, this::clearValidFrom);
		setupDateField(validToDisplayField, () -> validToDateRecord != null, this::newValidTo,
			this::editValidTo, this::clearValidTo);

		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());

		final JPanel modificationContainer = new JPanel(new MigLayout("top", "[grow]", "[grow]"));
		modificationContainer.add(modificationPanel, "grow");
		tabbedPane.addTab("Modification", modificationContainer);

		setLayout(new MigLayout("fillx,top"));
		add(tabbedPane, "growx");

		add(createButtonPanel(), BorderLayout.SOUTH);
	}

	private void setupDateField(final JTextField field, final Supplier<Boolean> hasSelection, final Runnable newAction,
			final Runnable editAction, final Runnable clearAction){
		field.setEditable(false);
		field.setBackground(UIManager.getColor("TextField.background"));
		GUIHelper.installBehavior(field,
			hasSelection,
			editAction,
			newAction,
			clearAction,
			builder -> {
				builder.item("Set Date...", newAction);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", editAction);
				builder.selectionSensitiveItem("Clear", clearAction);
			});
	}

	private JPanel createMainPanel(){
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// TITLE
		mainPanel.add(new JLabel("Title:"), "align label");
		mainPanel.add(titleField, "growx,wrap");

		// PLACE GROUP
		final JPanel placePanel = new JPanel(new MigLayout("ins 5, fillx, top", "[right]rel[grow]", "[]5[]"));
		placePanel.setBorder(BorderFactory.createTitledBorder("Place"));

		placePanel.add(new JLabel("Place:"), "align label");
		placeDisplayField.setEditable(false);
		placeDisplayField.setBackground(UIManager.getColor("TextField.background"));
		GUIHelper.installBehavior(placeDisplayField,
			() -> placeStructureRecord != null,
			this::editPlace,
			this::createNewPlace,
			this::clearPlace,
			builder -> {
				builder.item("Create New...", this::createNewPlace);
				builder.item("Add Existing...", this::addPlace);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editPlace);
				builder.selectionSensitiveItem("Clear", this::clearPlace);
			});
		placePanel.add(placeDisplayField, "growx,wrap");

		// PLACE QUALIFIERS (Certainty / Credibility)
		placePanel.add(placeQualifiers, "span 2,growx,wrap");

		mainPanel.add(placePanel, "span 2,growx,wrap");

		// Validity Range Panel
		final JPanel validityPanel = new JPanel(new MigLayout("ins 5, fillx, top", "[right]rel[grow]", "[]5[]"));
		validityPanel.setBorder(BorderFactory.createTitledBorder("Validity Range"));

		validityPanel.add(new JLabel("Valid From:"), "align label");
		validityPanel.add(validFromDisplayField, "growx, wrap");

		validityPanel.add(new JLabel("Valid To:"), "align label");
		validityPanel.add(validToDisplayField, "growx, wrap");

		mainPanel.add(validityPanel, "span 2, growx, wrap");

		mainPanel.add(culturalNormQualifiers, "span 2,growx,wrap");

		return mainPanel;
	}

	private void newValidFrom(){
		final FLEFRecord updated = showNewDateDialog("Valid From Date");
		if(updated != null){
			validFromDateRecord = updated;
			updateDateDisplay(validFromDisplayField, validFromDateRecord);
		}
	}

	private void editValidFrom(){
		final FLEFRecord updated = showEditDateDialog("Valid From Date", validFromDateRecord);
		if(updated != null){
			validFromDateRecord = updated;
			updateDateDisplay(validFromDisplayField, validFromDateRecord);
		}
	}

	private void clearValidFrom(){
		validFromDateRecord = null;
		validFromDisplayField.setText(StringUtils.EMPTY);
	}

	private void newValidTo(){
		final FLEFRecord updated = showNewDateDialog("Valid To Date");
		if(updated != null){
			validToDateRecord = updated;
			updateDateDisplay(validToDisplayField, validToDateRecord);
		}
	}

	private void editValidTo(){
		final FLEFRecord updated = showEditDateDialog("Valid To Date", validToDateRecord);
		if(updated != null){
			validToDateRecord = updated;
			updateDateDisplay(validToDisplayField, validToDateRecord);
		}
	}

	private void clearValidTo(){
		validToDateRecord = null;
		validToDisplayField.setText(StringUtils.EMPTY);
	}

	private FLEFRecord showNewDateDialog(final String title){
		final DateDialog dialog = DateDialog.createNew(this, model, title);
		dialog.setVisible(true);
		return (dialog.isSaved()? dialog.getDateRecord(): null);
	}

	private FLEFRecord showEditDateDialog(final String title, final FLEFRecord dateRecord){
		final DateDialog dialog = DateDialog.createEdit(this, model, title, dateRecord);
		dialog.setVisible(true);
		return (dialog.isSaved()? dialog.getDateRecord(): dateRecord);
	}

	private void updateDateDisplay(final JTextField field, final FLEFRecord dateRecord){
		if(dateRecord == null){
			field.setText(StringUtils.EMPTY);
			return;
		}
		field.setText(DatePanel.getDisplayText(dateRecord));
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 5,fillx,top,wrap 1", "[grow]", "[]5[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// Notes
		panel.add(notePanel, "growx");

		// Source Citations
		panel.add(sourceCitationPanel, "growx");

		return panel;
	}

	private void createNewPlace(){
		final RecordTypeHandler<?> placeHandler = HandlerRegistry.getHandler(PlaceHandler.TYPE);
		final BaseRecordDialog dialog = (BaseRecordDialog)placeHandler.createNewDialog(parent, model);
		dialog.setVisible(true);

		if(dialog.isSaved() && dialog.getRecord() != null){
			placeStructureRecord = dialog.getRecord();
			updatePlaceDisplay();
		}
	}

	private void addPlace(){
		final RecordTypeHandler<?> placeHandler = HandlerRegistry.getHandler(PlaceHandler.TYPE);
		final GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parent, model, placeHandler, selectedId -> {
			if(selectedId != null){
				placeStructureRecord = model.getRecordById(selectedId);
				updatePlaceDisplay();
			}
		});
		dialog.setVisible(true);
	}

	private void editPlace(){
		if(placeStructureRecord == null)
			return;

		final PlaceStructureDialog dialog = new PlaceStructureDialog(this, model, placeStructureRecord);
		dialog.setVisible(true);
		if(dialog.isSaved())
			updatePlaceDisplay();
	}

	private void clearPlace(){
		placeStructureRecord = null;
		placeDisplayField.setText(StringUtils.EMPTY);
	}

	private void updatePlaceDisplay(){
		String displayText = StringUtils.EMPTY;
		if(placeStructureRecord != null){
			final RecordTypeHandler<?> placeHandler = HandlerRegistry.getHandler(PlaceHandler.TYPE);
			displayText = placeHandler.getDisplayName(placeStructureRecord);
		}
		placeDisplayField.setText(displayText);
	}

	// ==================== Load / Save ====================

	@Override
	protected void loadData(){
		bindingManager.loadFromRecord(record);

		// PLACE_STRUCTURE
		placeStructureRecord = FLEFRecordUtils.findChild(record, "PLACE");
		if(placeStructureRecord != null){
			updatePlaceDisplay();

			final String placeCert = FLEFRecordUtils.getChildValue(placeStructureRecord, "CERTAINTY");
			final String placeCred = FLEFRecordUtils.getChildValue(placeStructureRecord, "CREDIBILITY");
			placeQualifiers.load(placeCert, placeCred);
		}
		else
			clearPlace();

		// Load VALID_FROM
		final FLEFRecord validFrom = FLEFRecordUtils.findChild(record, "VALID_FROM");
		if(validFrom != null){
			validFromDateRecord = FLEFRecordUtils.findChild(validFrom, "DATE");
			updateDateDisplay(validFromDisplayField, validFromDateRecord);
		}
		else
			clearValidFrom();

		// Load VALID_TO
		final FLEFRecord validTo = FLEFRecordUtils.findChild(record, "VALID_TO");
		if(validTo != null){
			validToDateRecord = FLEFRecordUtils.findChild(validTo, "DATE");
			updateDateDisplay(validToDisplayField, validToDateRecord);
		}
		else
			clearValidTo();

		// NOTE
		final List<String> noteIds = new ArrayList<>();
		for(final FLEFRecord child : record.getChildren())
			if("NOTE".equals(child.getTag()) && child.getValue() != null)
				noteIds.add(child.getValue());
		notePanel.loadFromNoteIds(noteIds);

		// SOURCE_CITATION
		final List<FLEFRecord> sources = new ArrayList<>();
		for(final FLEFRecord child : record.getChildren())
			if("SOURCE".equals(child.getTag()))
				sources.add(child);
		sourceCitationPanel.setItems(sources);

		final String placeCert = FLEFRecordUtils.getChildValue(record, "CERTAINTY");
		final String placeCred = FLEFRecordUtils.getChildValue(record, "CREDIBILITY");
		culturalNormQualifiers.load(placeCert, placeCred);

		// MODIFICATION
		modificationPanel.loadFromRecord(record);
	}

	@Override
	protected boolean validateData(){
		return true;
	}

	@Override
	protected void saveRecord(){
		FLEFRecordUtils.removeAllChildren(record);

		bindingManager.saveToRecord(record);

		// PLACE
		FLEFRecordUtils.removeChildren(record, "PLACE");
		if(placeStructureRecord != null && placeStructureRecord.getValue() != null){
			placeStructureRecord.setTag("PLACE");
			FLEFRecordUtils.updateChildValue(placeStructureRecord, "CERTAINTY", placeQualifiers.getCertainty());
			FLEFRecordUtils.updateChildValue(placeStructureRecord, "CREDIBILITY", placeQualifiers.getCredibility());
			record.addChild(placeStructureRecord);
		}

		// VALID_FROM
		if(validFromDateRecord != null){
			final FLEFRecord validFrom = FLEFRecord.createChild("VALID_FROM");
			validFrom.addChild(validFromDateRecord);
			record.addChild(validFrom);
		}

		// VALID_TO
		if(validToDateRecord != null){
			final FLEFRecord validTo = FLEFRecord.createChild("VALID_TO");
			validTo.addChild(validToDateRecord);
			record.addChild(validTo);
		}

		// NOTE
		for(final String id : notePanel.getNoteIds()){
			FLEFRecordUtils.addChild(record, "NOTE", FLEFRecordUtils.formatXRef(id));
		}

		// SOURCE
		for(final FLEFRecord source : sourceCitationPanel.getItems()){
			source.setTag("SOURCE");
			record.addChild(source);
		}

		FLEFRecordUtils.updateChildValue(record, "CERTAINTY", culturalNormQualifiers.getCertainty());
		FLEFRecordUtils.updateChildValue(record, "CREDIBILITY", culturalNormQualifiers.getCredibility());

		// MODIFICATION
		modificationPanel.saveToRecord(record);

		if(isNew)
			model.addRecord(record);
		isSaved = true;

// TODO to be removed
FLEFFile.print(model);
//		dispose();
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final CulturalNormDialog dialog = CulturalNormDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
