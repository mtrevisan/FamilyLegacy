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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.RepositoryCitationDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.RepositoryRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/* DONE */
/**
 * Panel for managing a list of {@code REPOSITORY} references according to FLEF 0.1.1.
 */
public class RepositoryCitationListPanel extends AbstractListPanel2{

	@Serial
	private static final long serialVersionUID = -7836305319216138743L;


	private static final String TAG_REPOSITORY = "REPOSITORY";


	static{
		HandlerRegistry.register(new RepositoryHandler());
	}


	private final String path;

	private final RecordTypeHandler<?> repositoryHandler;


	/**
	 * Constructs a RepositoryCitationListPanel without a border.
	 *
	 * @param parent the parent dialog
	 * @param model        the FLEF model
	 */
	public RepositoryCitationListPanel(final String path, final Dialog parent, final FLEFModel model){
		this(path, parent, "Repositories", model);
	}

	/**
	 * Constructs a RepositoryCitationListPanel with a titled border.
	 *
	 * @param parent the parent dialog
	 * @param borderTitle  the border title, or {@code null} for no border
	 * @param model        the FLEF model
	 */
	public RepositoryCitationListPanel(final String path, final Dialog parent, final String borderTitle,
		final FLEFModel model){
		super(parent, borderTitle, model);

		this.path = path;

		repositoryHandler = HandlerRegistry.getHandler(RepositoryHandler.TYPE);
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
				builder.selectionSensitiveItem("Edit...", this::editRepository);
				builder.selectionSensitiveItem("Edit Citation...", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			});
	}

	@Override
	protected String getDisplay(final FLEFRecord repositoryCitation){
		final String repositoryId = findRecordRepositoryId(repositoryCitation);
		if(repositoryId != null){
			final FLEFRecord repository = model.getRecordById(repositoryId);
			if(repository != null)
				return repositoryHandler.getDisplayText(repository, model);
			return repositoryId;
		}
		return "--";
	}

	public String findRecordRepositoryId(final FLEFRecord repositoryCitation){
		String id = null;
		for(final FLEFRecord child : repositoryCitation.getChildren())
			if(TAG_REPOSITORY.equals(child.getTag()))
				id = XRefHelper.extractXRef(child.getValue());
		return id;
	}

	@Override
	protected FLEFRecord showAddDialog(){
		final FLEFRecord[] result = {null};
		final GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parent, model, repositoryHandler, selectedItem -> {
			final String selectedId = selectedItem.getValue();
			final FLEFRecord repositoryCitation = model.getRecordById(selectedId);
			if(repositoryCitation != null && !items.contains(repositoryCitation)){
				final String repositoryId = findRecordRepositoryId(repositoryCitation);
				final FLEFRecord repository = model.getRecordById(repositoryId);
				if(repository != null)
					result[0] = repository;
			}
		}
		);
		dialog.setVisible(true);

		return result[0];
	}

	/**
	 * Creates a new repository and adds a citation for it and adds this one to the list.
	 */
	@Override
	protected FLEFRecord showCreateNewDialog(){
		final RepositoryRecordDialog newRepositoryDialog = (RepositoryRecordDialog)repositoryHandler.createNewDialog(parent, model);
		newRepositoryDialog.setVisible(true);

		FLEFRecord newRepositoryCitation = null;
		if(newRepositoryDialog.isSaved()){
			final FLEFRecord newRepository = newRepositoryDialog.getRecord();
			if(newRepository != null){
				final String newRepositoryId = newRepository.getId();
				final FLEFRecord repositoryCitation = FLEFRecord.createEmpty();
				FLEFRecordHelper.updateChildValue(repositoryCitation, TAG_REPOSITORY, XRefHelper.formatXRef(newRepositoryId));
				final RepositoryCitationDialog citationDialog = RepositoryCitationDialog.createEdit(parent, model, repositoryCitation);
				citationDialog.setVisible(true);

				if(citationDialog.isSaved())
					newRepositoryCitation = citationDialog.getRecord();
				else
					model.removeRecord(newRepositoryId);
			}
		}
		return newRepositoryCitation;
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, "Repository Citation not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final JDialog dialog = RepositoryCitationDialog.createEdit(parent, model, existing);
		dialog.setVisible(true);

		// Return the same record (it was updated in place)
		return existing;
	}

	public final void editRepository(){
		final int idx = list.getSelectedIndex();
		if(idx == -1)
			return;

		final FLEFRecord repositoryCitation = items.get(idx);
		final String repositoryId = findRecordRepositoryId(repositoryCitation);
		if(repositoryId != null){
			final FLEFRecord repository = model.getRecordById(repositoryId);
			final RepositoryRecordDialog dialog = RepositoryRecordDialog.createEdit(parent, model, repository);
			dialog.setVisible(true);

			if(dialog.isSaved())
				listModel.setElementAt(getDisplay(repositoryCitation), idx);
		}
	}

	public void load(final FLEFRecord record){
		final List<FLEFRecord> repositoryCitations = FLEFRecordHelper.findChildren(record, path);
		final List<FLEFRecord> repositories = new ArrayList<>();
		for(final FLEFRecord repositoryCitation : repositoryCitations){
			final String repositoryId = findRecordRepositoryId(repositoryCitation);
			if(repositoryId != null){
				final FLEFRecord repository = model.getRecordById(repositoryId);
				repositories.add(repository);
			}
		}
		setItems(repositories);
	}

	public void save(final FLEFRecord record){
		super.save(record, path);
	}

}
