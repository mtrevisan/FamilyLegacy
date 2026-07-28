package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.SourceCitationDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JDialog;
import java.awt.Dialog;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.io.Serial;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Panel for managing a list of SOURCE_CITATION records.
 * <p>
 * Provides:
 * <ul>
 *   <li>Add existing source citation</li>
 *   <li>Create new source + add citation</li>
 *   <li>Edit citation</li>
 *   <li>Remove citation</li>
 * </ul>
 */
public class SourceCitationListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = -764509672344287269L;


	// Handlers
	static{
		HandlerRegistry.register(new SourceHandler());
	}


	/**
	 * Constructs a SourceCitationListPanel.
	 *
	 * @param parentDialog the parent dialog
	 * @param model        the FLEF model
	 */
	public SourceCitationListPanel(Dialog parentDialog, FLEFModel model){
		super(parentDialog, "Source Citations", model);
	}

	@Override
	protected String getDisplay(FLEFRecord citation){
		String sourceId = citation.getValue();
		if(sourceId != null){
			FLEFRecord rec = model.getRecordById(sourceId);
			if(rec != null){
				final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
				return sourceHandler.getDisplayText(rec);
			}
			return sourceId;
		}
		return "[empty]";
	}

	@Override
	protected FLEFRecord showAddDialog(){
		// Add existing source
		final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
		final GenericSelectionDialog<?> selDialog = new GenericSelectionDialog<>(
			parentDialog, model, sourceHandler, selectedId -> {
			if(selectedId != null){
				// TODO The callback will be handled, but we need to return the citation
			}
		});
		selDialog.setVisible(true);

		// Since the callback doesn't return the citation directly, we handle it differently
		// We'll use a holder pattern
		final FLEFRecord[] result = {null};
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parentDialog, model, sourceHandler, selectedId -> {
			if(selectedId != null){
				result[0] = FLEFRecord.createChildWithValue("SOURCE", FLEFRecordUtils.formatXRef(selectedId));
			}
		});
		dialog.setVisible(true);
		return result[0];
	}

	@Override
	protected FLEFRecord showEditDialog(FLEFRecord existing){
		SourceCitationDialog editDialog = new SourceCitationDialog(parentDialog, model, existing);
		editDialog.setVisible(true);
		if(editDialog.isSaved()){
			FLEFRecord updated = editDialog.getCitationRecord();
			if(updated != null){
				updated.setTag("SOURCE");
				return updated;
			}
		}
		return null;
	}

	/**
	 * Creates a new source and adds a citation for it.
	 * This method is called from the context menu via the builder.
	 */
	public void createNewSourceAndAddCitation(){
		Set<String> before = new HashSet<>();
		for(FLEFRecord rec : model.getRecordsByType("SOURCE")){
			String id = rec.getId();
			if(id != null) before.add(id);
		}

		final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
		JDialog newSourceDialog = sourceHandler.createNewDialog(parentDialog, model);
		newSourceDialog.setVisible(true);

		String newSourceId = null;
		for(FLEFRecord rec : model.getRecordsByType("SOURCE")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				newSourceId = id;
				break;
			}
		}

		if(newSourceId != null){
			FLEFRecord citationRecord = FLEFRecord.createChildWithValue("SOURCE", FLEFRecordUtils.formatXRef(newSourceId));
			SourceCitationDialog citationDialog = new SourceCitationDialog(parentDialog, model, citationRecord);
			citationDialog.setVisible(true);

			if(citationDialog.isSaved()){
				FLEFRecord savedCitation = citationDialog.getCitationRecord();
				if(savedCitation != null){
					savedCitation.setTag("SOURCE");
					addItemDirectly(savedCitation);
				}
			}
		}
	}

	/**
	 * Loads a list of citations into the panel.
	 *
	 * @param citations the list of citation records
	 */
	public void loadFromCitations(List<FLEFRecord> citations){
		clear();
		for(FLEFRecord citation : citations){
			citation.setTag("SOURCE");
			addItemDirectly(citation);
		}
	}

	/**
	 * Returns the list of citation records.
	 *
	 * @return the citations
	 */
	public List<FLEFRecord> getCitations(){
		return getItems();
	}

	/**
	 * Overrides the builder to add "New..." and "Add Existing..." items.
	 */
	@Override
	protected void initComponents(){
		super.initComponents();

		// Override the behavior to add "New..." and "Add Existing..."
		// We need to re-install the behavior with the custom builder
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
				builder.item("Create New...", this::createNewSourceAndAddCitation);
				builder.item("Add Existing...", this::addItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			});
	}

}
