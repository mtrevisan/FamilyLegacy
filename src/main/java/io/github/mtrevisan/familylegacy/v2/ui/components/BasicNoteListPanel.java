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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Dialog;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/* DONE */
/**
 * Panel for managing a list of a simple {@code NOTE} references according to FLEF 0.1.1.
 */
public class BasicNoteListPanel extends AbstractListPanel<String>{

	@Serial
	private static final long serialVersionUID = 4276649156298328979L;


	static{
		HandlerRegistry.register(new NoteHandler());
	}


	private final String path;


	/**
	 * Constructs a BasicNoteListPanel with a titled border.
	 *
	 * @param parent the parent dialog
	 * @param borderTitle  the border title, or {@code null} for no border
	 */
	public BasicNoteListPanel(final String path, final Dialog parent, final String borderTitle){
		super(parent, borderTitle, null);

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
			}
		);
	}

	@Override
	protected String getDisplay(final String note){
		return (note != null? note : "--");
	}

	@Override
	protected String showAddDialog(){
		return null;
	}

	/**
	 * Creates a new note and adds it to the list.
	 */
	@Override
	protected String showCreateNewDialog(){
		final JTextArea textArea = new JTextArea(10, 50);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);

		final int result = JOptionPane.showConfirmDialog(
			parent,
			new JScrollPane(textArea),
			"Add Note",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE
		);

		final String text = textArea.getText().trim();
		return (result == JOptionPane.OK_OPTION && StringUtils.isNotEmpty(text)? text: null);
	}

	@Override
	protected String showEditDialog(final String existing){
		if(existing == null)
			return null;

		final JTextArea textArea = new JTextArea(existing, 10, 50);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);

		final int result = JOptionPane.showConfirmDialog(
			parent,
			new JScrollPane(textArea),
			"Edit Note",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE
		);

		final String text = textArea.getText().trim();
		return (result == JOptionPane.OK_OPTION && StringUtils.isNotEmpty(text)? text: null);
	}

	public void load(final FLEFRecord record){
		clear();

		if(record == null)
			return;

		final List<FLEFRecord> noteRecords = FLEFRecordHelper.findChildren(record, path);
		final List<String> notes = new ArrayList<>(noteRecords.size());
		for(final FLEFRecord noteRecord : noteRecords)
			notes.add(noteRecord.getValue());
		setItems(notes);
	}

	public void save(final FLEFRecord record){
		FLEFRecordHelper.removeChildren(record, path);

		for(final String note : getItems())
			FLEFRecordHelper.addChild(record, path, note);
	}

}
