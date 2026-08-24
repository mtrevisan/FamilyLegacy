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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs.records;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ClassifiedNameHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;


/**
 * Dialog for editing a {@code REPOSITORY_RECORD} according to FLEF 0.1.2.
 * <p>
 * Structure:
 * <pre>
 * record RepositoryRecord {
 *   id: LocalID
 *   name+: ClassifiedNameStructure
 *   custodian?: Xref&lt;IndividualRecord&gt;
 *   place?: PlaceCitation
 *   contact*: ContactStructure
 *   note*: Xref&lt;NoteRecord&gt;
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): name, custodian, place, contact
 * Tab 7 (Sources): SourceRecord (repository references this repository)
 * Tab 8 (Notes): note
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class RepositoryRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 3053114409506763765L;


	private static final String TAG_NAME = "NAME";
	private static final String TAG_CUSTODIAN = "CUSTODIAN";
	private static final String TAG_PLACE = "PLACE";
	private static final String TAG_CONTACT = "CONTACT";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_PRIVACY = "PRIVACY";
	private static final String TAG_AUDIT = "AUDIT";


	private final RecordDialogComponents components;

	private final JPanel propertiesPanel;

	private final EntityListPanel namePanel;
	private final EntityField custodianField;
	private final EntityField placeField;
	private final EntityListPanel contactPanel;


	public static RepositoryRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, RepositoryRecordDialog::new);
	}

	public static RepositoryRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, RepositoryRecordDialog::new);
	}


	private RepositoryRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, RepositoryHandler.class);

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]5[]10[]");

		namePanel = EntityListPanel.createForStructure(TAG_NAME, this, "Names*", model, ClassifiedNameHandler.class);
		custodianField = EntityField.createForRecordFromReference(TAG_CUSTODIAN, this, model, IndividualHandler.class);
		placeField = EntityField.createForStructureWithReference(TAG_PLACE, this, model, PlaceCitationHandler.class);
		contactPanel = EntityListPanel.createForStructure(TAG_CONTACT, this, "Contacts", model, ContactHandler.class);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.SOURCE_ON_REPOSITORY, TAG_SOURCE, "Sources with Citations", SourceHandler.class, RepositoryHandler.class)
			.withComponent(PanelKey.NOTE, TAG_NOTE, "Notes", NoteHandler.class, NoteHandler.class)
			.withComponent(PanelKey.PRIVACY, TAG_PRIVACY, null, null, null)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null, null, null)
			.build();


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// name
		GUIHelper.addComponent(propertiesPanel, namePanel);

		// custodian
		GUIHelper.addLabeledComponent(propertiesPanel, "Custodian:", custodianField);

		// place
		GUIHelper.addLabeledComponent(propertiesPanel, "Place:", placeField);

		// contact
		GUIHelper.addComponent(propertiesPanel, contactPanel);

		return propertiesPanel;
	}

	@Override
	protected JPanel createSourcesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel sourcePanel = components.getPanel(PanelKey.SOURCE_ON_REPOSITORY);
		GUIHelper.addComponent(panel, sourcePanel);

		return panel;
	}

	@Override
	protected JPanel createNotesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel notePanel = components.getPanel(PanelKey.NOTE);
		GUIHelper.addComponent(panel, notePanel);

		return panel;
	}

	@Override
	protected JPanel createPrivacyPanel(){
		return components.getPanel(PanelKey.PRIVACY);
	}

	@Override
	protected JPanel createAuditPanel(){
		return components.getPanel(PanelKey.AUDIT);
	}


	@Override
	protected void loadData(){
		components.load(record);

		namePanel.load(record);
		custodianField.load(record);
		placeField.load(record);
		contactPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(!namePanel.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"At least one name is required.",
				tabbedPane, propertiesPanel, namePanel);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		components.save(record);

		namePanel.save(record);
		custodianField.saveReferences(record);
		placeField.saveReferences(record);
		contactPanel.save(record);
	}


	public static void main(final String[] args) throws IOException{
		try(final InputStream is = RepositoryRecordDialog.class.getResourceAsStream("/tests/test.flef")){
			final String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			GUIHelper.launch(RepositoryRecordDialog::createEdit, content, "R1");
		}
	}

}
