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

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.ui.bindings.BindingsHelper;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextArea;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Window;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;


/**
 * Panel for managing a list of a simple {@code NOTE} references according to FLEF 0.1.3.
 */
public class BasicNoteListPanel extends AbstractListPanel<FLEFRecord>{

	private final String path;

	private final String recordTag;


	/**
	 * Constructs a BasicNoteListPanel with a titled border.
	 *
	 * @param parent	the parent dialog
	 * @param panelTitle	the border title, or {@code null} for no border
	 */
	public BasicNoteListPanel(final String path, final Window parent, final String panelTitle, final String recordTag){
		super(parent, panelTitle, null);

		this.path = path;

		this.recordTag = recordTag;


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
		final String date = NoteReader.extractDate(record);
		final String comment = FLEFRecordHelper.getChildValue(record, recordTag);
		return "(" + date + ") " + comment;
	}

	@Override
	protected FLEFRecord showAddDialog(){
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		return showNoteDialog(null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord record){
		return showNoteDialog(record);
	}

	private FLEFRecord showNoteDialog(final FLEFRecord record){
		final String note = FLEFRecordHelper.getChildValue(record, recordTag);


		final JDialog dialog = new JDialog(parent, I18N.t(record == null? "dialog.note.title.add": "dialog.note.title.edit"), Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setLayout(GUIHelper.createLabelFieldLayout(10, "[]"));

		final BoundTextArea textArea = new BoundTextArea(recordTag, 10, 50);
		if(record != null)
			textArea.setText(note);

		GUIHelper.addComponent(dialog, textArea);


		final FLEFRecord[] result = {record};
		final JPanel buttonPanel = GUIHelper.createButtonPanel(dialog,
			() -> {
				if(!validNoteData(textArea))
					return;

				final String text = textArea.getText()
					.trim();
				if(record == null){
					final String creationDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
					final FLEFRecord newNote = FLEFRecordHelper.getOrCreateTargetNode(FLEFRecord.createEmpty(), path)
						.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_DATE, creationDate))
						.addChild(FLEFRecord.createChildWithTagAndValue(recordTag, text));
					result[0] = newNote;
				}
				else
					FLEFRecordHelper.updateChildValue(record, recordTag, text);

				dialog.dispose();
			},
			dialog::dispose);
		dialog.add(buttonPanel, BorderLayout.SOUTH);

		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);

		return result[0];
	}

	protected boolean validNoteData(final BoundTextArea textArea){
		if(StringUtils.isEmpty(textArea.getText().trim())){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.name.note")),
				null, null, textArea);

			return false;
		}

		return true;
	}

	/**
	 * Loads notes from the given record.
	 *
	 * @param record	the record containing the notes
	 */
	public void load(final FLEFRecord record){
		clear();

		if(record == null || record.isEmpty())
			return;

		final List<FLEFRecord> notes = FLEFRecordHelper.extractStructures(record, path);
		setItems(notes);
	}

	/**
	 * Saves the current notes to the given record.
	 *
	 * @param record	the record to save to
	 */
	public void save(final FLEFRecord record){
		record.addChildren(getItems());
	}

}
