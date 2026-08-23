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
package io.github.mtrevisan.familylegacy.v2.ui.components.lists;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.FileHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;
import java.util.List;


/**
 * Panel for managing a list of extracts with text, type, locale, and notes.
 */
public class ExtractListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = -259585503419013969L;


	private static final String TAG_DOCUMENT = "DOCUMENT";
	private static final String TAG_DOCUMENT_PART = "DOCUMENT_PART";
	private static final String TAG_DESCRIPTION = "DESCRIPTION";
	private static final String TAG_FILE = "FILE";
	private static final String TAG_TEXT = "TEXT";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_LOCALE = "LOCALE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_COMMENT = "COMMENT";


	private final String path;


	public ExtractListPanel(final String path, final Dialog parent, final String panelTitle, final FLEFModel model){
		super(parent, panelTitle, model);

		this.path = path;
	}


	@Override
	protected void initComponents(){
		super.initComponents();

		GUIHelper.installBehavior(list,
			this::editItem, null,
			this::createNewItem, this::removeItem,
			builder -> {
				builder.item("Create New…", this::createNewItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit…", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			}
		);
	}

	@Override
	protected String getDisplayText(final FLEFRecord record){
		final String text = FLEFRecordHelper.getChildValue(record, TAG_TEXT);
		final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
		final String locale = FLEFRecordHelper.getChildValue(record, TAG_LOCALE);

		final StringBuilder sb = new StringBuilder();
		if(StringUtils.isNotEmpty(locale))
			sb.append('[')
				.append(locale)
				.append("] ");
		if(StringUtils.isNotEmpty(text))
			sb.append(GUIHelper.limitTextLength(text));
		else{
			// First document_part
			final FLEFRecord documentPart = FLEFRecordHelper.findChildren(record, TAG_DOCUMENT_PART).stream()
				.findFirst()
				.orElse(null);
			if(documentPart != null){
				final FLEFRecord documentCitation = FLEFRecordHelper.findChild(documentPart, TAG_DOCUMENT);
				final String documentId = (documentCitation != null? documentCitation.getValue(): null);
				final FLEFRecord document = model.getRecordById(documentId);
				if(document != null){
					final String description = FLEFRecordHelper.getChildValue(document, TAG_DESCRIPTION);

					if(StringUtils.isNotBlank(description))
						sb.append(description);
					else{
						final String uri = FLEFRecordHelper.getChildValue(document, TAG_FILE);
						if(StringUtils.isNotBlank(uri))
							sb.append(FileHelper.getFilename(uri));
						else
							sb.append('[')
								.append(document.getId())
								.append(']');
					}
				}
			}
		}

		if(StringUtils.isNotEmpty(type)){
			if(!sb.isEmpty())
				sb.append(' ');
			sb.append('(')
				.append(type)
				.append(')');
		}

		return (!sb.isEmpty()
			? sb.toString()
			: "--");
	}

	@Override
	protected FLEFRecord showAddDialog(){
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		return showExtractDialog(null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord record){
		return showExtractDialog(record);
	}

	/**
	 * Shows the extract dialog for creating or editing an extract.
	 *
	 * @param record	the initial extract record, or {@code null} for a new extract
	 * @return the created/updated extract record, or {@code null} if canceled
	 */
	private FLEFRecord showExtractDialog(final FLEFRecord record){
		final DocumentPartListPanel documentPartPanel = new DocumentPartListPanel(TAG_DOCUMENT_PART, parent, "Document Parts", model);
		final BoundTextArea textArea = new BoundTextArea(TAG_TEXT, 3, 25);
		final BoundComboBox<String> typeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{
			"verbatim", "summarized", "translated", "normalized"});
		final BoundComboBox<String> localeCombo = new BoundComboBox<>(TAG_LOCALE, new String[]{
			StringUtils.EMPTY,
			"en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"});
		localeCombo.setEditable(true);
		final BasicNoteListPanel basicNote = new BasicNoteListPanel(TAG_NOTE, parent, "Notes", TAG_NOTE);


		loadExtractData(record, documentPartPanel, textArea, typeCombo, localeCombo, basicNote);


		final JDialog dialog = new JDialog(parent, record == null? "Add Extract": "Edit Extract", Dialog.ModalityType.APPLICATION_MODAL);
		initExtractComponents(dialog, documentPartPanel, textArea, typeCombo, localeCombo, basicNote);

		final FLEFRecord[] result = {record};
		final JPanel buttonPanel = GUIHelper.createSaveCancelButtonPanel(dialog.getRootPane(),
			() -> {
				if(!validExtractData(dialog, documentPartPanel, textArea))
					return;

				if(record == null){
					final FLEFRecord res = FLEFRecord.createEmpty();
					documentPartPanel.saveReferences(res);
					res.addChild(FLEFRecord.createChildWithTagAndValue(TAG_TEXT, textArea.getText()));
					res.addChild(FLEFRecord.createChildWithTagAndValue(TAG_TYPE, (String)typeCombo.getSelectedItem()));
					res.addChild(FLEFRecord.createChildWithTagAndValue(TAG_LOCALE, (String)localeCombo.getSelectedItem()));
					for(final FLEFRecord note : basicNote.getItems())
						res.addChild(FLEFRecord.createChildWithTagAndValue(TAG_NOTE, FLEFRecordHelper.getChildValue(note, TAG_COMMENT)));
					result[0] = res;
				}
				else{
					documentPartPanel.saveReferences(record);
					FLEFRecordHelper.updateChildValue(record, TAG_TEXT, textArea.getText());
					FLEFRecordHelper.updateChildValue(record, TAG_TYPE, (String)typeCombo.getSelectedItem());
					FLEFRecordHelper.updateChildValue(record, TAG_LOCALE, (String)localeCombo.getSelectedItem());
					for(final FLEFRecord note : basicNote.getItems())
						FLEFRecordHelper.updateChildValue(record, TAG_NOTE, FLEFRecordHelper.getChildValue(note, TAG_COMMENT));
				}

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
		dialog.setLayout(GUIHelper.createLabelFieldLayout(10, "[]10[]"));

		GUIHelper.addComponent(dialog, documentPartPanel);

		GUIHelper.addLabeledComponent(dialog, "Text*:", textArea);

		GUIHelper.addLabeledComponent(dialog, "Type*:", typeCombo);

		GUIHelper.addLabeledComponent(dialog, "Locale:", localeCombo);

		GUIHelper.addComponent(dialog, basicNote);
	}

	private static void loadExtractData(final FLEFRecord record, final DocumentPartListPanel documentPartPanel,
			final BoundTextArea textArea, final BoundComboBox<String> typeCombo, final BoundComboBox<String> localeCombo,
			final BasicNoteListPanel basicNote){
		if(record == null)
			return;

		final List<FLEFRecord> documentParts = FLEFRecordHelper.findChildren(record, TAG_DOCUMENT_PART);
		final String text = FLEFRecordHelper.getChildValue(record, TAG_TEXT);
		final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
		final String locale = FLEFRecordHelper.getChildValue(record, TAG_LOCALE);
		final List<String> notes = FLEFRecordHelper.findChildren(record, TAG_NOTE).stream()
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
			basicNote.addItemDirectly(FLEFRecord.createChildWithTagAndValue(TAG_COMMENT, note));
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

		if(record == null || record.isEmpty())
			return;

		final List<FLEFRecord> extracts = FLEFRecordHelper.extractStructures(record, path);
		setItems(extracts);
	}

	public void save(final FLEFRecord record){
		super.save(record, path);
	}

}
