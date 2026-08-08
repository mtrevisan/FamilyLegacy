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
import io.github.mtrevisan.familylegacy.v2.ui.components.ClassifiedNameListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ContactListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.IndividualField;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.PlaceCitationField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;


/* DONE */
/**
 * Dialog for editing a {@code REPOSITORY_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record RepositoryRecord {
 *   id: LocalID
 *   name+: ClassifiedName
 *   custodian?: Xref&lt;IndividualRecord&gt;
 *   place?: PlaceCitation
 *   contact*: ContactStructure
 *   note*: Xref&lt;NoteRecord&gt;
 *   restriction?: RestrictionStructure
 *   modification: ModificationStructure
 * }
 * </pre>
 */
public class RepositoryRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 3053114409506763765L;


	private static final String TAG_NAME = "NAME";
	private static final String TAG_CUSTODIAN = "CUSTODIAN";
	private static final String TAG_PLACE = "PLACE";
	private static final String TAG_CONTACT = "CONTACT";
	private static final String TAG_NOTE = "NOTE";


	static{
		HandlerRegistry.register(new RepositoryHandler());
	}


	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]10[]5[]"));

	private final ClassifiedNameListPanel namePanel;
	private final IndividualField custodianField;
	private final PlaceCitationField placeCitationField;
	private final ContactListPanel contactPanel;
	private final NoteListPanel notePanel;
	private final ModificationPanel modificationPanel;


	public static RepositoryRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return new RepositoryRecordDialog(parent, model, null);
	}

	public static RepositoryRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new RepositoryRecordDialog(parent, model, record);
	}


	private RepositoryRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(RepositoryHandler.TYPE));

		namePanel = new ClassifiedNameListPanel(TAG_NAME, this, "Names*", model);
		custodianField = IndividualField.create(TAG_CUSTODIAN, parent, model);
		placeCitationField = PlaceCitationField.create(TAG_PLACE, parent, model);
		contactPanel = new ContactListPanel(TAG_CONTACT, this, model);
		notePanel = new NoteListPanel(TAG_NOTE, this, model);
		modificationPanel = new ModificationPanel(this);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		setLayout(new MigLayout("ins 10,fillx,top"));

		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Modification", modificationPanel);
		add(tabbedPane, "growx");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		// name structure
		mainPanel.add(namePanel, "span 2,growx,wrap");

		// custodian
		mainPanel.add(new JLabel("Custodian:"), "align label");
		mainPanel.add(custodianField, "growx,wrap");

		// place structure
		mainPanel.add(new JLabel("Place:"), "align label");
		mainPanel.add(placeCitationField, "growx");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]10[]"));
		panel.add(contactPanel, "growx");
		panel.add(notePanel, "growx");
		return panel;
	}


	@Override
	protected void loadData(){
		namePanel.load(record);
		custodianField.load(record);
		placeCitationField.load(record);
		contactPanel.load(record);
		notePanel.load(record);
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(!namePanel.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"At least one name is required.",
				tabbedPane, mainPanel, namePanel);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		namePanel.save(record);
		custodianField.save(record);
		placeCitationField.save(record);
		contactPanel.save(record);
		notePanel.saveReferences(record);
		modificationPanel.save(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final RepositoryRecordDialog dialog = RepositoryRecordDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
