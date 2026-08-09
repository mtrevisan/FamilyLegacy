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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.NoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;


/* DONE */
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
 */
public class RepositoryCitationDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 7964432114921960068L;


	private static final String TAG_REPOSITORY = "REPOSITORY";
	private static final String TAG_LOCATION = "LOCATION";
	private static final String TAG_NOTE = "NOTE";


	static{
		HandlerRegistry.register(new RepositoryCitationHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final BoundTextField repository;
	private final BoundTextField locationField;
	private final NoteListPanel notePanel;


	/**
	 * Creates a new dialog to create a new record.
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @return	A new dialog instance.
	 */
	public static RepositoryCitationDialog createNew(final Dialog parent, final FLEFModel model,
			final String repositoryId){
		return new RepositoryCitationDialog(parent, model, repositoryId, null);
	}

	/**
	 * Creates a new dialog to edit an existing record.
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @param record	The record to edit (must not be {@code null}).
	 * @return	A new dialog instance.
	 */
	public static RepositoryCitationDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new RepositoryCitationDialog(parent, model, null, record);
	}


	private RepositoryCitationDialog(final Dialog parent, final FLEFModel model, final String repositoryId,
			final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(RepositoryCitationHandler.TYPE));

		repository = new BoundTextField(TAG_REPOSITORY, 20);
		if(StringUtils.isNotEmpty(repositoryId))
			repository.setText(repositoryId);
		locationField = new BoundTextField(TAG_LOCATION, 20);
		notePanel = new NoteListPanel(TAG_NOTE, this, null, model);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(repository);
		bindingManager.bind(locationField);


		setLayout(new MigLayout("ins 10,fillx,top"));

		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("Notes", notePanel);
		add(tabbedPane, "growx");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]"));

		// location
		mainPanel.add(new JLabel("Location:"), "align label");
		mainPanel.add(locationField, "growx,wrap");

		return mainPanel;
	}


	@Override
	protected void loadData(){
		if(record == null)
			return;

		bindingManager.load(record);

		if(StringUtils.isBlank(repository.getText())){
			JOptionPane.showMessageDialog(this, "Invalid Repository ID: `" + repository.getText() + "`.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return;
		}

		notePanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(StringUtils.isEmpty(repository.getText())){
			JOptionPane.showMessageDialog(null,
				"REPOSITORY is required for a citation.\n" +
					"Please select a repository record.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		FLEFRecordHelper.updateChildValue(record, TAG_REPOSITORY, XRefHelper.formatXRef(repository.getText()));

		bindingManager.save(record);

		notePanel.saveReferences(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
//			FLEFRecord repositoryCitation = FLEFRecord.createEmpty();
//			repositoryCitation.addChild(FLEFRecord.createChildWithValue(TAG_REPOSITORY, "@R1@"));
//			final RepositoryCitationDialog dialog = RepositoryCitationDialog.createEdit(null, model, repositoryCitation);
			final RepositoryCitationDialog dialog = RepositoryCitationDialog.createNew(null, model, "@R1@");
			dialog.setVisible(true);
		});
	}

}
