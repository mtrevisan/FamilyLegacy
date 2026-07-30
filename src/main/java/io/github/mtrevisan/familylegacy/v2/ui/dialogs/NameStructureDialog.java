package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.DateField;
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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;
import java.awt.Window;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/**
 * Dialog for creating and editing generic NAME_STRUCTURE instances according to the FLEF protocol.
 *
 * <p>Actual Record Hierarchy:
 * <pre>
 * NAME_STRUCTURE :=
 * n NAME    {1:1}
 *   +1 <<TEXT_VALUE>>    {1:1}
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

	// Simple bound fields mapped to TEXT_VALUE protocol
	private final BoundTextField valueField;
	private final BoundComboBox<String> typeCombo;
	private final BoundComboBox<String> localeCombo;

	// Validity Date Fields (Directly wrapping VALID_FROM and VALID_TO)
	private final DateField validFromField;
	private final DateField validToField;

	// Text Value Variants (PHONETIC / TRANSCRIPTION)
	private final DefaultListModel<String> variantListModel = new DefaultListModel<>();
	private final JList<String> variantList = new JList<>(variantListModel);
	private final List<FLEFRecord> variantRecords = new ArrayList<>();

	// Notes & Source Citations
	private final NoteListPanel notePanel;
	private final SourceCitationListPanel sourcePanel;

	private final JTabbedPane tabbedPane = new JTabbedPane();


	public NameStructureDialog(final Window parent, final FLEFModel model, final FLEFRecord existingRecord){
		super(parent, existingRecord == null ? "Add Name" : "Edit Name", ModalityType.APPLICATION_MODAL);

		this.model = model;
		this.nameRecord = (existingRecord != null) ? copyRecord(existingRecord) : FLEFRecord.createChild("NAME");

		valueField = new BoundTextField("VALUE", 30);
		typeCombo = new BoundComboBox<>("TYPE", new String[]{
			StringUtils.EMPTY, "official", "colonial", "indigenous", "historical", "informal", "traditional"
		});
		localeCombo = new BoundComboBox<>("VALUE.LOCALE", new String[]{
			StringUtils.EMPTY, "en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"
		});

		validFromField = DateField.createWithWrapperTag("VALID_FROM", this, "Valid From Date", model);
		validToField = DateField.createWithWrapperTag("VALID_TO", this, "Valid To Date", model);

		notePanel = new NoteListPanel("NOTE", model, this);
		sourcePanel = new SourceCitationListPanel("SOUR", this, model);

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

	private void initComponents(){
		bindingManager.bind(valueField);
		bindingManager.bind(typeCombo);
		bindingManager.bind(localeCombo);

		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("Variants", createVariantsPanel());
		tabbedPane.addTab("References", createReferencesPanel());

		setLayout(new MigLayout("ins 10,fillx,top", "[grow]", "[]10[]"));
		add(tabbedPane, "growx, wrap");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(), this::save, this::dispose);
		add(buttonPanel, "growx");
	}

	private JPanel createMainPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]5[]10[]"));

		panel.add(new JLabel("Name Value*:"), "align label");
		panel.add(valueField, "growx, wrap");

		typeCombo.setEditable(true);
		panel.add(new JLabel("Type:"), "align label");
		panel.add(typeCombo, "growx, wrap");

		localeCombo.setEditable(true);
		panel.add(new JLabel("Locale:"), "align label");
		panel.add(localeCombo, "growx, wrap");

		// Validity Range Panel
		final JPanel validityPanel = new JPanel(new MigLayout("ins 5,fillx,top", "[right]rel[grow]", "[]5[]"));
		validityPanel.setBorder(BorderFactory.createTitledBorder("Validity Range"));
		validityPanel.add(new JLabel("Valid From:"), "align label");
		validityPanel.add(validFromField, "growx, wrap");
		validityPanel.add(new JLabel("Valid To:"), "align label");
		validityPanel.add(validToField, "growx, wrap");
		panel.add(validityPanel, "span 2, growx, wrap");

		return panel;
	}

	private JPanel createVariantsPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top"));
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
		final JPanel panel = new JPanel(new MigLayout("ins 5,fillx,top,wrap 1", "[grow]", "[]5[]"));
		panel.add(notePanel, "growx");
		panel.add(sourcePanel, "growx");
		return panel;
	}

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
		if(idx == -1){
			return;
		}

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
			return "[Phonetic - " + record.getValue() + "]: " + StringUtils.defaultString(val);
		}
		if("TRANSCRIPTION".equals(record.getTag())){
			final String type = FLEFRecordUtils.getChildValue(record, "TYPE");
			final String val = FLEFRecordUtils.getChildValue(record, "VALUE");
			return "[Transcription - " + record.getValue() + (StringUtils.isNotBlank(type) ? " (" + type + ")" : StringUtils.EMPTY) + "]: " + StringUtils.defaultString(val);
		}
		return record.getTag();
	}

	private void loadData(){
		bindingManager.load(nameRecord);

		final FLEFRecord valueRec = FLEFRecordUtils.findChild(nameRecord, "VALUE");
		if(valueRec == null){
			variantRecords.clear();
			variantListModel.clear();
			notePanel.clear();
			sourcePanel.clear();
			return;
		}

		// Load sub-structures under TEXT_VALUE (VALUE)
		validFromField.load(valueRec);
		validToField.load(valueRec);

		// Load variants directly (PHONETIC and TRANSCRIPTION)
		variantRecords.clear();
		variantListModel.clear();
		for(final FLEFRecord child : valueRec.getChildren()){
			if("PHONETIC".equals(child.getTag()) || "TRANSCRIPTION".equals(child.getTag())){
				variantRecords.add(child);
				variantListModel.addElement(getVariantDisplay(child));
			}
		}

		// References
		notePanel.load(valueRec);
		sourcePanel.load(valueRec);
	}

	private void save(){
		final String text = valueField.getText().trim();
		if(text.isEmpty()){
			JOptionPane.showMessageDialog(this, "Name value cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Save top-level bound elements on NAME
		bindingManager.save(nameRecord);

		// Retrieve or create VALUE record (TEXT_VALUE)
		FLEFRecord valueRec = FLEFRecordUtils.findChild(nameRecord, "VALUE");
		if(valueRec == null){
			valueRec = FLEFRecord.createChildWithValue("VALUE", text);
			nameRecord.addChild(valueRec);
		}
		else{
			valueRec.setValue(text);
		}

		// Remove previous sub-nodes under VALUE to prevent duplicates
		FLEFRecordUtils.removeChildren(valueRec, "VALID_FROM", "VALID_TO", "PHONETIC", "TRANSCRIPTION", "NOTE", "SOUR", "SOURCE");

		// Save sub-structures directly into VALUE
		validFromField.save(valueRec);
		validToField.save(valueRec);

		for(final FLEFRecord variant : variantRecords){
			valueRec.addChild(variant);
		}

		notePanel.save(valueRec);
		sourcePanel.save(valueRec);

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
