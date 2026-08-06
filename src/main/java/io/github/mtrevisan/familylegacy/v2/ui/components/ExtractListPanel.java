package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;
import java.util.List;


/* DONE */
public class ExtractListPanel extends AbstractListPanel2{

	@Serial
	private static final long serialVersionUID = -259585503419013969L;


	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_DOCUMENT_PART = "DOCUMENT_PART";
	private static final String TAG_DOCUMENT = "DOCUMENT";
	private static final String TAG_TEXT = "TEXT";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_LOCALE = "LOCALE";
	private static final String TAG_NOTE = "NOTE";


	static{
		HandlerRegistry.register(new NoteHandler());
	}


	private final String path;

	private List<FLEFRecord> sourceDocuments;


	public ExtractListPanel(final String path, final Dialog parent, final FLEFModel model){
		super(parent, "Extracts", model);

		this.path = path;
	}


	@Override
	protected void initComponents(){
		super.initComponents();

		GUIHelper.installBehavior(list,
			this::editItem,
			this::createNewItem,
			this::removeItem,
			builder -> {
				builder.item("Create New...", this::createNewItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			});
	}

	@Override
	protected String getDisplay(final FLEFRecord item){
		final String text = FLEFRecordHelper.getChildValue(item, TAG_TEXT);
		final String type = FLEFRecordHelper.getChildValue(item, TAG_TYPE);
		final String locale = FLEFRecordHelper.getChildValue(item, TAG_LOCALE);

		final StringBuilder sb = new StringBuilder();
		if(!StringUtils.isEmpty(locale))
			sb.append("[")
				.append(locale)
				.append("] ");
		if(!StringUtils.isEmpty(text))
			sb.append(text.length() > 50? text.substring(0, 47) + "...": text);
		if(!StringUtils.isEmpty(type))
			sb.append(" (")
				.append(type)
				.append(")");
		return sb.toString();
	}

	@Override
	protected FLEFRecord showAddDialog(){
		return null;
	}

	/**
	 * Creates a new extract and adds it to the list.
	 */
	@Override
	protected FLEFRecord showCreateNewDialog(){
		return showExtractDialog(null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		return showExtractDialog(existing);
	}

	private FLEFRecord showExtractDialog(final FLEFRecord initial){
		final DocumentPartListPanel documentPartPanel = new DocumentPartListPanel(TAG_DOCUMENT_PART, parent, model, sourceDocuments);
		final BoundTextArea textArea = new BoundTextArea(TAG_TEXT, 3, 25);
		final BoundComboBox<String> typeCombo = new BoundComboBox<>(TAG_TYPE,
			new String[]{"transcript", "extract", "abstract"});
		final BoundComboBox<String> localeCombo = new BoundComboBox<>(TAG_LOCALE,
			new String[]{StringUtils.EMPTY, "en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"});
		localeCombo.setEditable(true);
		final BasicNoteListPanel basicNote = new BasicNoteListPanel(TAG_NOTE, parent);

		loadExtractData(initial, documentPartPanel, textArea, typeCombo, localeCombo, basicNote);

		final JDialog dialog = new JDialog(parent, initial == null? "Add Extract": "Edit Extract", true);
		initExtractComponents(dialog, documentPartPanel, textArea, typeCombo, localeCombo, basicNote);

		final FLEFRecord[] result = {null};
		final JPanel buttonPanel = GUIHelper.createButtonPanel(dialog.getRootPane(),
			() -> {
				if(!validExtractData(dialog, documentPartPanel, textArea, typeCombo))
					return;

				final FLEFRecord res = FLEFRecord.createEmpty();
				documentPartPanel.saveReferences(res);
				res.addChild(FLEFRecord.createChildWithValue(TAG_TEXT, textArea.getValue()));
				res.addChild(FLEFRecord.createChildWithValue(TAG_TYPE, (String)typeCombo.getSelectedItem()));
				res.addChild(FLEFRecord.createChildWithValue(TAG_LOCALE, (String)localeCombo.getSelectedItem()));
				for(final String note : basicNote.getItems())
					res.addChild(FLEFRecord.createChildWithValue(TAG_NOTE, note));
				result[0] = res;

				dialog.dispose();
			},
			dialog::dispose);
		dialog.add(buttonPanel, BorderLayout.SOUTH);

		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);

		return result[0];
	}

	private static void initExtractComponents(final JDialog dialog, final DocumentPartListPanel documentPartPanel,
			final BoundTextArea textArea, final BoundComboBox<String> typeCombo, final BoundComboBox<String> localeCombo,
			final BasicNoteListPanel basicNote){
		dialog.setLayout(new MigLayout("ins 10,fillx", "[right]rel[grow]", "[]10[]"));

		dialog.add(documentPartPanel, "span 2,growx,wrap");

		dialog.add(new JLabel("Text*:"), "align label,top");
		dialog.add(GUIHelper.createScrollPane(textArea), "growx,wrap");

		dialog.add(new JLabel("Type*:"), "align label");
		dialog.add(typeCombo, "growx,wrap");

		dialog.add(new JLabel("Locale:"), "align label");
		dialog.add(localeCombo, "growx,wrap");

		dialog.add(basicNote, "span 2,growx,wrap");
	}

	private static void loadExtractData(final FLEFRecord initial, final DocumentPartListPanel documentPartPanel,
			final BoundTextArea textArea, final BoundComboBox<String> typeCombo, final BoundComboBox<String> localeCombo,
			final BasicNoteListPanel basicNote){
		if(initial == null)
			return;

		final List<FLEFRecord> documentParts = FLEFRecordHelper.findChildren(initial, TAG_DOCUMENT_PART);
		final String text = FLEFRecordHelper.getChildValue(initial, TAG_TEXT);
		final String type = FLEFRecordHelper.getChildValue(initial, TAG_TYPE);
		final String locale = FLEFRecordHelper.getChildValue(initial, TAG_LOCALE);
		final List<String> notes = FLEFRecordHelper.findChildren(initial, TAG_NOTE).stream()
			.map(FLEFRecord::getValue)
			.toList();

		for(final FLEFRecord documentPart : documentParts)
			documentPartPanel.addItemDirectly(documentPart);
		textArea.setText(text);
		if(!StringUtils.isEmpty(type))
			typeCombo.setSelectedItem(type);
		if(!StringUtils.isEmpty(locale))
			localeCombo.setSelectedItem(locale);
		for(final String note : notes)
			basicNote.addItemDirectly(note);
	}

	private static boolean validExtractData(final JDialog dialog, final DocumentPartListPanel documentPartPanel,
			final BoundTextArea textArea, final BoundComboBox<String> typeCombo){
		if(documentPartPanel.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(dialog, "Extract document parts cannot be empty.",
				null, null, documentPartPanel);

			return false;
		}
		if(StringUtils.isEmpty(textArea.getValue())){
			GUIHelper.showValidationErrorAndFocus(dialog, "Extract value cannot be empty.",
				null, null, textArea);

			return false;
		}
		if(StringUtils.isEmpty((String)typeCombo.getSelectedItem())){
			GUIHelper.showValidationErrorAndFocus(dialog, "Extract type cannot be empty.",
				null, null, textArea);

			return false;
		}

		return true;
	}

	public void load(final FLEFRecord record){
		final String sourceId = FLEFRecordHelper.getChildValue(record, TAG_SOURCE);
		final FLEFRecord source = model.getRecordById(sourceId);
		sourceDocuments = FLEFRecordHelper.findChildren(source, TAG_DOCUMENT);

		final List<FLEFRecord> extracts = FLEFRecordHelper.findChildren(record, path);
		setItems(extracts);
	}

	public void save(final FLEFRecord record){
		super.save(record, path);
	}

}
