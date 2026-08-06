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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.NoteRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/* DONE */
/**
 * Panel for managing a list of {@code NOTE} references according to FLEF 0.1.1.
 * <p>
 * Provides:
 * <ul>
 *   <li>Add existing note</li>
 *   <li>Create new note</li>
 *   <li>Edit note</li>
 *   <li>Remove note reference</li>
 * </ul>
 */
public class NoteListPanel extends AbstractListPanel2{

	@Serial
	private static final long serialVersionUID = -5638163012098890098L;


	static{
		HandlerRegistry.register(new NoteHandler());
	}


	private final String path;

	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);


	/**
	 * Constructs a NoteListPanel without a border.
	 *
	 * @param parent the parent dialog
	 * @param model        the FLEF model
	 */
	public NoteListPanel(final String path, final Dialog parent, final FLEFModel model){
		this(path, parent, "Notes", model);
	}

	/**
	 * Constructs a NoteListPanel with a titled border.
	 *
	 * @param parent the parent dialog
	 * @param borderTitle  the border title, or {@code null} for no border
	 * @param model        the FLEF model
	 */
	public NoteListPanel(final String path, final Dialog parent, final String borderTitle, final FLEFModel model){
		super(parent, borderTitle, model);

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
				builder.item("Add Existing...", this::addItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			}
		);
	}

	@Override
	protected String getDisplay(final FLEFRecord note){
		if(note != null)
			return noteHandler.getDisplayText(note, model);

		return "--";
	}

	@Override
	protected FLEFRecord showAddDialog(){
		final FLEFRecord[] result = {null};
		final GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parent, model, noteHandler, selectedItem -> {
			final String selectedId = selectedItem.getValue();
			final FLEFRecord note = model.getRecordById(selectedId);
				if(note != null && !items.contains(note))
					result[0] = note;
			}
		);
		dialog.setVisible(true);

		return result[0];
	}

	/**
	 * Creates a new note and adds it to the list.
	 */
	@Override
	protected FLEFRecord showCreateNewDialog(){
		final NoteRecordDialog dialog = (NoteRecordDialog)noteHandler.createNewDialog(parent, model);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, "Note not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final JDialog dialog = noteHandler.createEditDialog(parent, model, existing);
		dialog.setVisible(true);

		// Return the same record (it was updated in place)
		return existing;
	}

	public void load(final FLEFRecord record){
		final List<FLEFRecord> noteCitations = FLEFRecordHelper.findChildren(record, path);
		final List<FLEFRecord> notes = new ArrayList<>(noteCitations.size());
		for(final FLEFRecord noteCitation : noteCitations)
			notes.add(model.getRecordById(noteCitation.getValue()));
		setItems(notes);
	}

	public void saveReferences(final FLEFRecord record){
		for(final FLEFRecord note : getItems())
			FLEFRecordHelper.addChild(record, path, note.getFormattedId());
	}

}
