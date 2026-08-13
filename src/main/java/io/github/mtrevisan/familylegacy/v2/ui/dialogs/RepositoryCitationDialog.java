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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Dialog;
import java.io.Serial;


/**
 * Dialog for editing a {@code REPOSITORY_CITATION} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * struct RepositoryCitation {
 *   repository: Xref&lt;RepositoryRecord&gt;
 *   location?: Text
 *   note?: Text
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): location
 * Tab 8 (Notes): note
 */
public class RepositoryCitationDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 7964432114921960068L;


	private static final String TAG_REPOSITORY = "REPOSITORY";
	private static final String TAG_LOCATION = "LOCATION";
	private static final String TAG_NOTE = "NOTE";


	private final RecordDialogComponents components;

	private final JPanel propertiesPanel;

	private final BoundTextField repository;
	private final BoundTextField locationField;


	public static RepositoryCitationDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, RepositoryCitationDialog::new);
	}

	public static RepositoryCitationDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, RepositoryCitationDialog::new);
	}


	private RepositoryCitationDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, RepositoryCitationHandler.class);

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]");

		repository = new BoundTextField(TAG_REPOSITORY);
		locationField = new BoundTextField(TAG_LOCATION);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.NOTE, TAG_NOTE, "Notes", NoteHandler.class, NoteHandler.class)
			.build();

		components.bind(repository);
		components.bind(locationField);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// location
		GUIHelper.addLabeledComponent(propertiesPanel, "Location:", locationField);

		return propertiesPanel;
	}

	@Override
	protected JPanel createNotesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel notePanel = components.getPanel(PanelKey.NOTE);
		GUIHelper.addComponent(panel, notePanel);

		return panel;
	}


	public void setRepository(final String repositoryId){
		if(StringUtils.isNotEmpty(repositoryId)){
			if(!confirmRecordExistsForType(repositoryId, RepositoryHandler.class))
				return;

			repository.setText(repositoryId);

			refreshLayout();
		}
	}

	private void refreshLayout(){
		propertiesPanel.revalidate();
		propertiesPanel.repaint();

		pack();
	}


	@Override
	protected void loadData(){
		if(record == null)
			return;

		components.load(record);
	}

	@Override
	protected boolean validData(){
		if(StringUtils.isEmpty(repository.getText())){
			JOptionPane.showMessageDialog(null,
				"Repository is required for a citation.\n" +
					"Please select a repository record.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		FLEFRecordHelper.updateChildValue(record, TAG_REPOSITORY, XRefHelper.formatXRef(repository.getText()));

		components.save(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final FLEFRecord repository = FLEFRecord.createMainRecord("R1", TAG_REPOSITORY);
			model.addRecord(repository);

//			final FLEFRecord repositoryCitation = FLEFRecord.createEmpty();
//			repositoryCitation.addChild(FLEFRecord.createChildWithValue(TAG_REPOSITORY, "R1"));
//			final RepositoryCitationDialog dialog = RepositoryCitationDialog.createEdit(null, model, repositoryCitation);
			final RepositoryCitationDialog dialog = RepositoryCitationDialog.createNew(null, model);
			dialog.setRepository("R1");
			dialog.setVisible(true);
		});
	}

}
