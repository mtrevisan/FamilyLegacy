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
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.SourceCitationDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.SourceRecordDialog;
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
 * Panel for managing a list of {@code SOURCE} references according to FLEF 0.1.1.
 * <p>
 * Provides:
 * <ul>
 *   <li>Add existing source citation</li>
 *   <li>Create new source + add citation</li>
 *   <li>Edit citation</li>
 *   <li>Remove citation</li>
 * </ul>
 */
public class SourceCitationListPanel extends AbstractListPanel2{

	@Serial
	private static final long serialVersionUID = -764509672344287269L;


	private static final String TAG_SOURCE = "SOURCE";


	static{
		HandlerRegistry.register(new SourceHandler());
	}


	private final String path;

	private final RecordTypeHandler<?> sourceHandler;


	/**
	 * Constructs a SourceCitationListPanel without a border.
	 *
	 * @param parent the parent dialog
	 * @param model        the FLEF model
	 */
	public SourceCitationListPanel(final String path, final Dialog parent, final FLEFModel model){
		this(path, parent, "Sources", model);
	}

	/**
	 * Constructs a SourceCitationListPanel with a titled border.
	 *
	 * @param parent the parent dialog
	 * @param borderTitle  the border title, or {@code null} for no border
	 * @param model        the FLEF model
	 */
	public SourceCitationListPanel(final String path, final Dialog parent, final String borderTitle,
			final FLEFModel model){
		super(parent, borderTitle, model);

		this.path = path;

		sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
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
			if(TAG_SOURCE.equals(child.getTag()))
				id = XRefHelper.extractXRef(child.getValue());
		return id;
	}

	@Override
	protected FLEFRecord showAddDialog(){
		final FLEFRecord[] result = {null};
		final GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parent, model, sourceHandler, selectedId -> {
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
		final SourceRecordDialog newSourceDialog = (SourceRecordDialog)sourceHandler.createNewDialog(parent, model);
		newSourceDialog.setVisible(true);

		FLEFRecord newSourceCitation = null;
		if(newSourceDialog.isSaved()){
			final FLEFRecord newSource = newSourceDialog.getRecord();
			if(newSource != null){
				final String newSourceId = newSource.getId();
				final FLEFRecord sourceCitation = FLEFRecord.createEmpty();
				FLEFRecordHelper.updateChildValue(sourceCitation, TAG_SOURCE, XRefHelper.formatXRef(newSourceId));
				final SourceCitationDialog citationDialog = SourceCitationDialog.createEdit(parent, model, sourceCitation);
				citationDialog.setVisible(true);

				if(citationDialog.isSaved())
					newSourceCitation = citationDialog.getRecord();
				else
					model.removeRecord(newSourceId);
			}
		}
		return newSourceCitation;
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, "Source Citation not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final JDialog dialog = SourceCitationDialog.createEdit(parent, model, existing);
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
			final SourceRecordDialog dialog = SourceRecordDialog.createEdit(parent, model, source);
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
		super.save(record, path);
	}

}
