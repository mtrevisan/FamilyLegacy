package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.SourceCitationDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.SourceDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/* DONE */
/**
 * Panel for managing a list of SOURCE references.
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


	private static final String PARAM_SOURCE = "SOURCE";


	static{
		HandlerRegistry.register(new SourceHandler());
	}


	private final String path;

	private final RecordTypeHandler<?> sourceHandler;


	/**
	 * Constructs a SourceCitationListPanel without a border.
	 *
	 * @param parentDialog the parent dialog
	 * @param model        the FLEF model
	 */
	public SourceCitationListPanel(final String path, final Dialog parentDialog, final FLEFModel model){
		this(path, parentDialog, "Sources", model);
	}

	/**
	 * Constructs a SourceCitationListPanel with a titled border.
	 *
	 * @param parentDialog the parent dialog
	 * @param borderTitle  the border title, or {@code null} for no border
	 * @param model        the FLEF model
	 */
	public SourceCitationListPanel(final String path, final Dialog parentDialog, final String borderTitle,
			final FLEFModel model){
		super(parentDialog, borderTitle, model);

		this.path = path;

		sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
	}


	@Override
	protected void initComponents(){
		super.initComponents();

		GUIHelper.installBehavior(list,
			() -> (list.getSelectedIndex() >= 0),
			this::editItem,
			this::createNewItem,
			this::removeItem,
			builder -> {
				builder.item("Create New...", this::createNewItem);
				builder.item("Add Existing...", this::addItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editSource);
				builder.selectionSensitiveItem("Edit Citation...", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			});
	}

	@Override
	protected String getDisplay(final FLEFRecord sourceCitation){
		final String sourceId = findRecordSourceId(sourceCitation);
		if(sourceId != null){
			final FLEFRecord source = model.getRecordById(sourceId);
			if(source != null)
				return sourceHandler.getDisplayText(source, model);
			return sourceId;
		}
		return "--";
	}

	public String findRecordSourceId(final FLEFRecord sourceCitation){
		String id = null;
		for(final FLEFRecord child : sourceCitation.getChildren())
			if(PARAM_SOURCE.equals(child.getTag()))
				id = XRefHelper.extractXRef(child.getValue());
		return id;
	}

	@Override
	protected FLEFRecord showAddDialog(){
		final FLEFRecord[] result = {null};
		final GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parentDialog, model, sourceHandler, selectedId -> {
				final FLEFRecord sourceCitation = model.getRecordById(selectedId);
				if(sourceCitation != null && !items.contains(sourceCitation)){
					final String sourceId = findRecordSourceId(sourceCitation);
					final FLEFRecord source = model.getRecordById(sourceId);
					if(source != null)
						result[0] = source;
				}
			}
		);
		dialog.setVisible(true);

		return result[0];
	}

	/**
	 * Creates a new source and adds a citation for it and adds this one to the list.
	 */
	@Override
	protected FLEFRecord showCreateNewDialog(){
		final SourceDialog newSourceDialog = (SourceDialog)sourceHandler.createNewDialog(parentDialog, model);
		newSourceDialog.setVisible(true);

		FLEFRecord newSourceCitation = null;
		final FLEFRecord newSource = newSourceDialog.getRecord();
		if(newSource != null){
			final String newSourceId = newSource.getId();
			final FLEFRecord sourceCitation = FLEFRecord.createEmpty();
			sourceCitation.addChild(FLEFRecord.createChildWithValue(PARAM_SOURCE, XRefHelper.formatXRef(newSourceId)));
			final SourceCitationDialog citationDialog = SourceCitationDialog.createEdit(parentDialog, model, sourceCitation);
			citationDialog.setVisible(true);

			if(citationDialog.isSaved())
				newSourceCitation = citationDialog.getRecord();
			else
				model.removeRecord(newSourceId);
		}
		return newSourceCitation;
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parentDialog, "Source Citation not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final JDialog dialog = SourceCitationDialog.createEdit(parentDialog, model, existing);
		dialog.setVisible(true);

		// Return the same record (it was updated in place)
		return existing;
	}

	public final void editSource(){
		final int idx = list.getSelectedIndex();
		if(idx == -1)
			return;

		final FLEFRecord sourceCitation = items.get(idx);
		final String sourceId = findRecordSourceId(sourceCitation);
		if(sourceId != null){
			final FLEFRecord source = model.getRecordById(sourceId);
			final SourceDialog dialog = SourceDialog.createEdit(parentDialog, model, source);
			dialog.setVisible(true);

			if(dialog.isSaved())
				listModel.setElementAt(getDisplay(sourceCitation), idx);
		}
	}

	public void load(final FLEFRecord record){
		final List<FLEFRecord> sourceCitations = FLEFRecordHelper.findChildren(record, path);
		final List<FLEFRecord> sources = new ArrayList<>();
		for(final FLEFRecord sourceCitation : sourceCitations){
			final String sourceId = findRecordSourceId(sourceCitation);
			if(sourceId != null){
				final FLEFRecord source = model.getRecordById(sourceId);
				sources.add(source);
			}
		}
		setItems(sources);
	}

	public void save(final FLEFRecord record){
		for(final FLEFRecord sourceCitation : getItems())
			record.addChild(sourceCitation);
	}

}
