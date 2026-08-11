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
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.IndividualField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.PlaceCitationField;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityReferenceListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.StructureListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ClassifiedNameHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
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
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new ClassifiedNameHandler());
		HandlerRegistry.register(new ContactHandler());
	}


	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]10[]5[]"));

	private final StructureListPanel namePanel;
	private final IndividualField custodianField;
	private final PlaceCitationField placeCitationField;
	private final StructureListPanel contactPanel;
	private final EntityReferenceListPanel notePanel;
	private final ModificationPanel modificationPanel;


	public static RepositoryRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, RepositoryRecordDialog::new);
	}

	public static RepositoryRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, RepositoryRecordDialog::new);
	}


	private RepositoryRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(RepositoryHandler.TYPE));

		namePanel = new StructureListPanel(TAG_NAME, this, "Names*", model, ClassifiedNameHandler.TYPE);
		custodianField = IndividualField.create(TAG_CUSTODIAN, this, model);
		placeCitationField = PlaceCitationField.create(TAG_PLACE, this, model);
		contactPanel = new StructureListPanel(TAG_CONTACT, this, "Contacts", model, ContactHandler.TYPE);
		notePanel = new EntityReferenceListPanel(TAG_NOTE, this, "Notes", model, NoteHandler.TYPE)
			.withParentEntity(this.record.getId(), RepositoryHandler.TYPE);
		modificationPanel = new ModificationPanel(this);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Modification", modificationPanel);

		finalizeLayout(tabbedPane);
	}

	private JPanel createMainPanel(){
		// name
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
		custodianField.saveReferences(record);
		placeCitationField.saveReferences(record);
		contactPanel.save(record);
		notePanel.saveReferences(record);
		modificationPanel.save(record);
	}


	public static void main(final String[] args){
		GUIHelper.launch(RepositoryRecordDialog::createNew);
	}

}
