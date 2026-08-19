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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import java.awt.Dialog;
import java.io.Serial;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;


/**
 * Panel for managing a list of a simple {@code NOTE} references according to FLEF 0.1.1.
 */
public class BasicNoteListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = 4276649156298328979L;


	private static final String TAG_DATE = "DATE";


	private final String path;

	private final boolean saveDate;
	private final String noteTag;


	/**
	 * Constructs a BasicNoteListPanel with a titled border.
	 *
	 * @param parent	the parent dialog
	 * @param borderTitle	the border title, or {@code null} for no border
	 */
	public BasicNoteListPanel(final String path, final Dialog parent, final String borderTitle,
			final boolean saveDate, final String noteTag){
		super(parent, borderTitle, null);

		this.path = path;

		this.saveDate = saveDate;
		this.noteTag = noteTag;
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
	protected String getDisplayText(final FLEFRecord note){
		final String date = FLEFRecordHelper.getChildValuesAsString(note, TAG_DATE);
		final String comment = FLEFRecordHelper.getChildValuesAsString(note, noteTag);
		final StringBuilder sb = new StringBuilder();
		sb.append('(')
			.append(date)
			.append(") ")
			.append(comment);
		return sb.toString();
	}

	@Override
	protected FLEFRecord showAddDialog(){
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		final JTextArea textArea = new JTextArea(10, 50);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);

		final int result = JOptionPane.showConfirmDialog(
			parent,
			GUIHelper.createScrollPane(textArea),
			"Add Note",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE
		);

		final String text = textArea.getText()
			.trim();
		if(result == JOptionPane.OK_OPTION && StringUtils.isNotEmpty(text)){
			final FLEFRecord newNote = FLEFRecordHelper.getOrCreateTargetNode(FLEFRecord.createEmpty(), path);
			final String creationDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
			newNote.addChild(FLEFRecord.createChildWithTagAndValue(TAG_DATE, creationDate));
			newNote.addChild(FLEFRecord.createChildWithTagAndValue(noteTag, text));
			return newNote;
		}
		return null;
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord record){
		final String displayText = getDisplayText(record);
		final JTextArea textArea = new JTextArea(displayText, 10, 50);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);

		final int result = JOptionPane.showConfirmDialog(
			parent,
			GUIHelper.createScrollPane(textArea),
			"Edit Note",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE
		);

		final String text = textArea.getText()
			.trim();
		if(result == JOptionPane.OK_OPTION && StringUtils.isNotEmpty(text))
			FLEFRecordHelper.updateChildValue(record, noteTag, text);

		return record;
	}

	/**
	 * Loads notes from the given record.
	 *
	 * @param record	the record containing the notes
	 */
	public void load(final FLEFRecord record){
		clear();

		final List<FLEFRecord> notes = FLEFRecordHelper.findChildren(record, path);
		setItems(notes);
	}

	/**
	 * Saves the current notes to the given record.
	 *
	 * @param record	the record to save to
	 */
	public void save(final FLEFRecord record){
		final FLEFRecord parentRecord = FLEFRecordHelper.getOrCreateTargetNode(record, path);
		for(final FLEFRecord item : getItems()){
			final FLEFRecord date = FLEFRecordHelper.findChild(item, TAG_DATE);
			final FLEFRecord text = FLEFRecordHelper.findChild(item, noteTag);
			if(saveDate)
				parentRecord.addChild(date);
			parentRecord.addChild(text);
		}
	}

}
