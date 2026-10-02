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
package io.github.mtrevisan.familylegacy.ui.dialogs.records;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.ContactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.NameHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceCitationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import javax.swing.JPanel;
import java.awt.Window;
import java.io.IOException;


/**
 * Dialog for editing a {@code REPOSITORY_RECORD} according to FLEF 0.1.3.
 * <p>
 * Structure:
 * <pre>
 * record RepositoryRecord {
 *   id: LocalID
 *   name+: NameStructure
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

	private final JPanel propertiesPanel;

	private final EntityListPanel namePanel;
	private final EntityField custodianField;
	private final EntityField placeField;
	private final EntityListPanel contactPanel;


	public static RepositoryRecordDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, RepositoryRecordDialog::new);
	}

	public static RepositoryRecordDialog createEdit(final Window parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, RepositoryRecordDialog::new);
	}


	private RepositoryRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, RepositoryHandler.getInstance());

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]5[]10[]");

		namePanel = EntityListPanel.createForStructure(RepositoryReader.TAG_NAME, this, I18N.t("dialog.repository.name") + "*", model, NameHandler.class);
		custodianField = EntityField.createForRecordFromReference(RepositoryReader.TAG_CUSTODIAN, this, model, IndividualHandler.class);
		placeField = EntityField.createForStructureWithReference(RepositoryReader.TAG_PLACE, this, model, PlaceCitationHandler.class);
		contactPanel = EntityListPanel.createForStructure(RepositoryReader.TAG_CONTACT, this, I18N.t("dialog.repository.contacts"), model, ContactHandler.class);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.SOURCE_ON_REPOSITORY, SourceHandler.TYPE, I18N.t("dialog.component.sources.with.citations"))
			.withComponent(PanelKey.NOTE, RepositoryReader.TAG_NOTE, null)
			.withComponent(PanelKey.PRIVACY, RepositoryReader.TAG_PRIVACY, null)
			.withComponent(PanelKey.AUDIT, RepositoryReader.TAG_AUDIT, null)
			.build();


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// name
		GUIHelper.addComponent(propertiesPanel, namePanel);

		// custodian
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.repository.custodian") + ":", custodianField);

		// place
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.place") + ":", placeField);

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
				I18N.tf("validation.at.least.one", I18N.t("dialog.repository.name")),
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
		GUIHelper.launch(RepositoryRecordDialog::createEdit, "/tests/test.flef", "R1");
	}

}
