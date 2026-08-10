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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;
import java.util.List;


/* DONE */
public class ExtractListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = -259585503419013969L;


	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_DOCUMENT_PART = "DOCUMENT_PART";
	private static final String TAG_DOCUMENT = "DOCUMENT";
	private static final String TAG_TEXT = "TEXT";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_LOCALE = "LOCALE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_COMMENT = "COMMENT";


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
		if(StringUtils.isNotEmpty(locale))
			sb.append("[")
				.append(locale)
				.append("] ");
		if(StringUtils.isNotEmpty(text))
			sb.append(text.length() > 50? text.substring(0, 47) + "...": text);
		if(StringUtils.isNotEmpty(type))
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
		if(existing == null){
			JOptionPane.showMessageDialog(parent, "Extract entry not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		return showExtractDialog(existing);
	}

	private FLEFRecord showExtractDialog(final FLEFRecord initial){
		final DocumentPartListPanel documentPartPanel = new DocumentPartListPanel(TAG_DOCUMENT_PART, parent, model);
		final BoundTextArea textArea = new BoundTextArea(TAG_TEXT, 3, 25);
		final BoundComboBox<String> typeCombo = new BoundComboBox<>(TAG_TYPE,
			new String[]{"transcript", "extract", "abstract"});
		final BoundComboBox<String> localeCombo = new BoundComboBox<>(TAG_LOCALE,
			new String[]{StringUtils.EMPTY, "en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"});
		localeCombo.setEditable(true);
		final BasicNoteListPanel basicNote = new BasicNoteListPanel(TAG_NOTE, parent, "Notes",
			false, TAG_NOTE);

		loadExtractData(initial, documentPartPanel, textArea, typeCombo, localeCombo, basicNote);

		final JDialog dialog = new JDialog(parent, initial == null? "Add Extract": "Edit Extract", true);
		initExtractComponents(dialog, documentPartPanel, textArea, typeCombo, localeCombo, basicNote);

		final FLEFRecord[] result = {null};
		final JPanel buttonPanel = GUIHelper.createButtonPanel(dialog.getRootPane(),
			() -> {
				if(!validExtractData(dialog, documentPartPanel, textArea))
					return;

				final FLEFRecord res = FLEFRecord.createEmpty();
				documentPartPanel.saveReferences(res);
				res.addChild(FLEFRecord.createChildWithValue(TAG_TEXT, textArea.getText()));
				res.addChild(FLEFRecord.createChildWithValue(TAG_TYPE, (String)typeCombo.getSelectedItem()));
				res.addChild(FLEFRecord.createChildWithValue(TAG_LOCALE, (String)localeCombo.getSelectedItem()));
				for(final FLEFRecord note : basicNote.getItems())
					res.addChild(FLEFRecord.createChildWithValue(TAG_NOTE, FLEFRecordHelper.getChildValuesAsString(note, TAG_COMMENT)));
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
		if(StringUtils.isNotEmpty(type))
			typeCombo.setSelectedItem(type);
		if(StringUtils.isNotEmpty(locale))
			localeCombo.setSelectedItem(locale);
		for(final String note : notes)
			basicNote.addItemDirectly(FLEFRecord.createChildWithValue(TAG_COMMENT, note));
	}

	private static boolean validExtractData(final JDialog dialog, final DocumentPartListPanel documentPartPanel,
			final BoundTextArea textArea){
		if(documentPartPanel.isEmpty() && textArea.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(dialog, "Extract document parts or value cannot be both empty.",
				null, null, documentPartPanel);

			return false;
		}

		return true;
	}

	public void load(final FLEFRecord record){
		clear();

		if(record == null)
			return;

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
