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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.io.Serial;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/* DONE */
/**
 * Panel for managing a list of NOTE references (by ID).
 * <p>
 * Provides:
 * <ul>
 *   <li>Add existing note</li>
 *   <li>Create new note</li>
 *   <li>Edit note</li>
 *   <li>Remove note reference</li>
 * </ul>
 */
public class NoteListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = -5638163012098890098L;


	// Handlers
	static{
		HandlerRegistry.register(new NoteHandler());
	}

	private final RecordTypeHandler<?> noteHandler;


	/**
	 * Constructs a NoteListPanel with a titled border.
	 *
	 * @param model        the FLEF model
	 * @param parentDialog the parent dialog
	 */
	public NoteListPanel(final FLEFModel model, final Dialog parentDialog){
		this(model, parentDialog, "Notes");
	}

	/**
	 * Constructs a NoteListPanel without a border.
	 *
	 * @param model        the FLEF model
	 * @param parentDialog the parent dialog
	 * @param borderTitle  the border title, or {@code null} for no border
	 */
	public NoteListPanel(final FLEFModel model, final Dialog parentDialog, final String borderTitle){
		super(parentDialog, borderTitle, model);

		noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);
	}


	@Override
	protected String getDisplay(final FLEFRecord note){
		if(note != null)
			return noteHandler.getDisplayText(note);

		return "--";
	}

	@Override
	protected FLEFRecord showAddDialog(){
		final FLEFRecord[] result = {null};
		final GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parentDialog, model, noteHandler, selectedId -> {
				final FLEFRecord record = model.getRecordById(selectedId);
				if(record != null && !items.contains(record))
					result[0] = record;
			});
		dialog.setVisible(true);
		return result[0];
	}

	@Override
	protected FLEFRecord showEditDialog(FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parentDialog, "Note not found", "Error", JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final JDialog editDialog = noteHandler.createEditDialog(parentDialog, model, existing);
		editDialog.setVisible(true);
		// Return the same ID (the note was updated in place)
		return existing;
	}

	/**
	 * Creates a new note and adds it to the list.
	 */
	public void createNewNote(){
		final Set<FLEFRecord> before = new HashSet<>(items);
		final JDialog newNoteDialog = noteHandler.createNewDialog(parentDialog, model);
		newNoteDialog.setVisible(true);

		for(final FLEFRecord note : model.getRecordsByType("NOTE"))
			if(note != null && !before.contains(note) && !items.contains(note)){
				addItemDirectly(note);

				break;
			}
	}

	/**
	 * Loads a list of note IDs into the panel.
	 *
	 * @param notes the list of notes
	 */
	public void loadFromNotes(final List<FLEFRecord> notes){
		clear();

		for(final FLEFRecord note : notes)
			if(note != null)
				addItemDirectly(note);
	}

	/**
	 * Returns the list of note IDs.
	 *
	 * @return the note IDs
	 */
	public List<FLEFRecord> getNotes(){
		return getItems();
	}

	/**
	 * Overrides the builder to add "Create New..." and "Add Existing..." items.
	 */
	@Override
	protected void initComponents(){
		super.initComponents();

		// Override the behavior to add "Create New..." and "Add Existing..."
		for(final MouseListener listener : list.getMouseListeners())
			list.removeMouseListener(listener);
		for(final KeyListener listener : list.getKeyListeners())
			list.removeKeyListener(listener);

		GUIHelper.installBehavior(list,
			() -> list.getSelectedIndex() >= 0,
			this::editItem,
			this::addItem,
			this::removeItem,
			builder -> {
				builder.item("Create New...", this::createNewNote);
				builder.item("Add Existing...", this::addItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			});
	}

}
