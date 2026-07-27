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
public class NoteListPanel extends AbstractListPanel<String>{

	@Serial
	private static final long serialVersionUID = -5638163012098890098L;


	// Handlers
	static{
		HandlerRegistry.register(new NoteHandler());
	}


	/**
	 * Constructs a NoteListPanel with a titled border.
	 *
	 * @param model        the FLEF model
	 * @param parentDialog the parent dialog
	 */
	public NoteListPanel(FLEFModel model, Dialog parentDialog){
		super(model, parentDialog, "Notes");
	}

	/**
	 * Constructs a NoteListPanel without a border.
	 *
	 * @param model        the FLEF model
	 * @param parentDialog the parent dialog
	 * @param borderTitle  the border title, or {@code null} for no border
	 */
	public NoteListPanel(FLEFModel model, Dialog parentDialog, String borderTitle){
		super(model, parentDialog, borderTitle);
	}

	@Override
	protected String getDisplay(String noteId){
		FLEFRecord rec = model.getRecordById(noteId);
		if(rec != null){
			final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);
			return noteHandler.getDisplayName(rec);
		}
		return noteId;
	}

	@Override
	protected String showAddDialog(){
		final String[] result = {null};
		final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			GUIHelper.getParentFrame(parentDialog), model, noteHandler, selectedId -> {
			if(selectedId != null && !items.contains(selectedId)){
				result[0] = selectedId;
			}
		});
		dialog.setVisible(true);
		return result[0];
	}

	@Override
	protected String showEditDialog(String existing){
		FLEFRecord rec = model.getRecordById(existing);
		if(rec == null){
			JOptionPane.showMessageDialog(parentDialog, "Note not found: " + existing, "Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);
		JDialog editDialog = noteHandler.createEditDialog(GUIHelper.getParentFrame(parentDialog), model, rec);
		editDialog.setVisible(true);
		// Return the same ID (the note was updated in place)
		return existing;
	}

	/**
	 * Creates a new note and adds it to the list.
	 */
	public void createNewNote(){
		Set<String> before = new HashSet<>(items);
		final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);
		JDialog newNoteDialog = noteHandler.createNewDialog(GUIHelper.getParentFrame(parentDialog), model);
		newNoteDialog.setVisible(true);

		for(FLEFRecord rec : model.getRecordsByType("NOTE")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !items.contains(id)){
				addItemDirectly(id);
				break;
			}
		}
	}

	/**
	 * Loads a list of note IDs into the panel.
	 *
	 * @param noteIds the list of note IDs
	 */
	public void loadFromNoteIds(List<String> noteIds){
		clear();
		for(String id : noteIds){
			if(id != null && !id.isEmpty()){
				addItemDirectly(id);
			}
		}
	}

	/**
	 * Returns the list of note IDs.
	 *
	 * @return the note IDs
	 */
	public List<String> getNoteIds(){
		return getItems();
	}

	/**
	 * Overrides the builder to add "Create New..." and "Add Existing..." items.
	 */
	@Override
	protected void initComponents(){
		super.initComponents();

		// Override the behavior to add "Create New..." and "Add Existing..."
		for(MouseListener listener : list.getMouseListeners())
			list.removeMouseListener(listener);
		for(KeyListener listener : list.getKeyListeners())
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
