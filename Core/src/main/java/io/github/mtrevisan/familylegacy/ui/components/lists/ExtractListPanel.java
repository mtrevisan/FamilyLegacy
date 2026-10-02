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
package io.github.mtrevisan.familylegacy.ui.components.lists;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.DocumentReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.ui.bindings.BindingsHelper;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundFilteredComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextArea;
import io.github.mtrevisan.familylegacy.ui.helpers.FileHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.LocaleHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Window;
import java.util.List;


/**
 * Panel for managing a list of extracts with text, type, locale, and notes.
 */
public class ExtractListPanel extends AbstractListPanel<FLEFRecord>{

	private final String path;


	public ExtractListPanel(final String path, final Window parent, final String panelTitle, final FLEFModel model){
		super(parent, panelTitle, model);

		this.path = path;


		initComponents();
	}


	@Override
	protected void initComponents(){
		super.initComponents();

		BindingsHelper.installBehavior(list,
			this::editItem, null,
			this::createNewItem, this::removeItem,
			builder -> {
				builder.item(I18N.t("popupmenu.create.new"), this::createNewItem);
				builder.separator();
				builder.selectionSensitiveItem(I18N.t("popupmenu.edit"), this::editItem);
				builder.selectionSensitiveItem(I18N.t("popupmenu.remove"), this::removeItem);
			}
		);
	}

	@Override
	protected String getDisplayText(final FLEFRecord record){
		final String text = SourceCitationReader.extractExtractText(record);
		final String type = SourceCitationReader.extractExtractType(record);
		final String locale = SourceCitationReader.extractExtractLocale(record);

		final StringBuilder sb = new StringBuilder();
		if(StringUtils.isNotEmpty(locale))
			sb.append('[')
				.append(locale)
				.append("] ");
		if(StringUtils.isNotEmpty(text))
			sb.append(GUIHelper.limitTextLength(StringUtils.replaceChars(text, '\n', '|')));
		else{
			// First document_part
			final FLEFRecord documentPart = FLEFRecordHelper.findChildren(record, SourceCitationReader.TAG_DOCUMENT_PART).stream()
				.findFirst()
				.orElse(null);
			if(documentPart != null){
				final FLEFRecord documentCitation = FLEFRecordHelper.findChild(documentPart, SourceCitationReader.TAG_DOCUMENT);
				final String documentId = (documentCitation != null? documentCitation.getValue(): null);
				final FLEFRecord document = model.getRecordById(documentId);
				if(document != null){
					final String description = FLEFRecordHelper.getChildValue(document, DocumentReader.TAG_DESCRIPTION);

					if(StringUtils.isNotEmpty(description))
						sb.append(description);
					else{
						final String uri = FLEFRecordHelper.getChildValue(document, DocumentReader.TAG_URI);
						if(StringUtils.isNotEmpty(uri))
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
		final DocumentPartListPanel documentPartPanel = new DocumentPartListPanel(SourceCitationReader.TAG_DOCUMENT_PART, parent, I18N.t("dialog.extract.document.parts.title"), model);
		final BoundTextArea textArea = new BoundTextArea(SourceCitationReader.TAG_TEXT, 3, 25);
		final BoundComboBox<String> typeCombo = new BoundComboBox<>(SourceCitationReader.TAG_TYPE, GUIHelper.fillCombo(SourceCitationReader.EXTRACT_TYPES, I18N.t("search.combo.any")));
		typeCombo.setI18NPrefix("enum.extract.type");
		final BoundFilteredComboBox<String> localeCombo = new BoundFilteredComboBox<>(SourceCitationReader.TAG_LOCALE, LocaleHelper.getAvailableLanguageTags());
		localeCombo.setEditable(true);
		final BasicNoteListPanel basicNote = new BasicNoteListPanel(SourceCitationReader.TAG_NOTE, parent, "Notes", SourceCitationReader.TAG_NOTE);


		loadExtractData(record, documentPartPanel, textArea, typeCombo, localeCombo, basicNote);


		final JDialog dialog = new JDialog(parent, I18N.t(record == null? "dialog.extract.title.add": "dialog.extract.title.edit"), Dialog.ModalityType.APPLICATION_MODAL);
		initExtractComponents(dialog, documentPartPanel, textArea, typeCombo, localeCombo, basicNote);

		final FLEFRecord[] result = {record};
		final JPanel buttonPanel = GUIHelper.createButtonPanel(dialog,
			() -> {
				if(!validExtractData(dialog, documentPartPanel, textArea))
					return;

				if(record == null){
					final FLEFRecord res = FLEFRecord.createEmpty();
					documentPartPanel.saveReferences(res);
					res.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_TEXT, textArea.getText()));
					res.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_TYPE, (String)typeCombo.getSelectedItem()));
					res.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_LOCALE, (String)localeCombo.getSelectedItem()));
					for(final FLEFRecord note : basicNote.getItems())
						res.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_NOTE, NoteReader.extractText(note)));
					result[0] = res;
				}
				else{
					documentPartPanel.saveReferences(record);
					FLEFRecordHelper.updateChildValue(record, SourceCitationReader.TAG_TEXT, textArea.getText());
					FLEFRecordHelper.updateChildValue(record, SourceCitationReader.TAG_TYPE, (String)typeCombo.getSelectedItem());
					FLEFRecordHelper.updateChildValue(record, SourceCitationReader.TAG_LOCALE, (String)localeCombo.getSelectedItem());
					for(final FLEFRecord note : basicNote.getItems())
						FLEFRecordHelper.updateChildValue(record, SourceCitationReader.TAG_NOTE, NoteReader.extractText(note));
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
			final BoundTextArea textArea, final BoundComboBox<String> typeCombo,
			final BoundFilteredComboBox<String> localeCombo, final BasicNoteListPanel basicNote){
		dialog.setLayout(GUIHelper.createLabelFieldLayout(10, "[]10[]"));

		GUIHelper.addComponent(dialog, documentPartPanel);

		GUIHelper.addLabeledComponent(dialog, I18N.t("dialog.extract.text") + "*:", textArea);

		GUIHelper.addLabeledComponent(dialog, I18N.t("dialog.extract.type") + "*:", typeCombo);

		GUIHelper.addLabeledComponent(dialog, I18N.t("dialog.extract.locale") + ":", localeCombo);

		GUIHelper.addComponent(dialog, basicNote);
	}

	private static void loadExtractData(final FLEFRecord record, final DocumentPartListPanel documentPartPanel,
			final BoundTextArea textArea, final BoundComboBox<String> typeCombo,
			final BoundFilteredComboBox<String> localeCombo, final BasicNoteListPanel basicNote){
		if(record == null)
			return;

		final List<FLEFRecord> documentParts = FLEFRecordHelper.findChildren(record, SourceCitationReader.TAG_DOCUMENT_PART);
		final String text = SourceCitationReader.extractExtractText(record);
		final String type = SourceCitationReader.extractExtractType(record);
		final String locale = SourceCitationReader.extractExtractLocale(record);
		final List<String> notes = FLEFRecordHelper.findChildren(record, SourceCitationReader.TAG_NOTE).stream()
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
			basicNote.addItemDirectly(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_NOTE, note));
	}

	private static boolean validExtractData(final JDialog dialog, final DocumentPartListPanel documentPartPanel,
			final BoundTextArea textArea){
		if(documentPartPanel.isEmpty() && textArea.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(dialog,
				I18N.tf("validation.at.least.one.of", I18N.t("dialog.extract.document.part"), I18N.t("dialog.extract.text")),
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
