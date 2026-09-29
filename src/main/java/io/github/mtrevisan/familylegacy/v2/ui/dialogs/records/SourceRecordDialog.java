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
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NameHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;

import javax.swing.JPanel;
import java.awt.Window;
import java.io.IOException;


/**
 * Dialog for editing a {@code SOURCE_RECORD} according to FLEF 0.1.3.
 * <p>
 * Structure:
 * <pre>
 * record SourceRecord {
 *   id: LocalID
 *   title+: NameStructure
 *   author?: Text
 *   publisher?: Text
 *   date?: DateStructure
 *   place?: PlaceCitation
 *   media_type?: enum { audio, book, card, electronic, fiche, film, magazine, manuscript, map, newspaper, photo, tombstone, video } | Text
 *   repository*: RepositoryCitation
 *   document*: Xref&lt;DocumentRecord&gt;
 *   note*: Xref&lt;NoteRecord&gt;
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): title, author, publisher, date, place, media_type, repository, document
 * Tab 6 (Research): ConclusionRecord (resolves = this source), ResearchQuestionRecord (target[source] = this source), ResearchActivityRecord (source contains this source)
 * Tab 8 (Notes): note
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class SourceRecordDialog extends BaseRecordDialog{

	private final EntityListPanel titlePanel;
	private final BoundTextField authorField;
	private final BoundTextField publisherField;
	private final DateField dateField;
	private final EntityField placeField;
	private final BoundComboBox<String> mediaTypeCombo;


	public static SourceRecordDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, SourceRecordDialog::new);
	}

	public static SourceRecordDialog createEdit(final Window parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, SourceRecordDialog::new);
	}


	private SourceRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, SourceHandler.getInstance());

		titlePanel = EntityListPanel.createForStructure(SourceHandler.TAG_TITLE, this, I18N.t("dialog.source.title") + "*", model,
			NameHandler.class);
		authorField = new BoundTextField(SourceHandler.TAG_AUTHOR);
		publisherField = new BoundTextField(SourceHandler.TAG_PUBLISHER);
		dateField = DateField.createWithWrapperTag(SourceHandler.TAG_DATE, this, I18N.t("dialog.source.valid.date"), model);
		placeField = EntityField.createForStructureWithReference(PlaceHandler.TYPE, this, model,
			PlaceCitationHandler.class);
		mediaTypeCombo = new BoundComboBox<>(SourceHandler.TAG_MEDIA_TYPE, SourceHandler.MEDIA_TYPES);
		mediaTypeCombo.setI18NPrefix("enum.source.media.type");
		mediaTypeCombo.setEditable(true);

		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.REPOSITORY, SourceHandler.TAG_REPOSITORY, I18N.t("dialog.component.repositories.with.citations"))
			.withComponent(PanelKey.DOCUMENT, SourceHandler.TAG_DOCUMENT, I18N.t("dialog.component.documents"))
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, ConclusionHandler.TYPE, I18N.t("dialog.component.conclusions"))
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, ResearchQuestionHandler.TYPE, I18N.t("dialog.component.research.questions"))
			.withComponent(PanelKey.RESEARCH_ACTIVITY_ON_SOURCE, ResearchActivityHandler.TYPE, I18N.t("dialog.component.research.activities"))
			.withComponent(PanelKey.NOTE, SourceHandler.TAG_NOTE, null)
			.withComponent(PanelKey.PRIVACY, SourceHandler.TAG_PRIVACY, null)
			.withComponent(PanelKey.AUDIT, SourceHandler.TAG_AUDIT, null)
			.build();

		components.bind(authorField);
		components.bind(publisherField);
		components.bind(mediaTypeCombo);

		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]5[]5[]5[]5[]5[]10[]");

		// title
		GUIHelper.addComponent(panel, titlePanel);

		// author
		GUIHelper.addLabeledComponent(panel, I18N.t("dialog.source.author") + ":", authorField);

		// publisher
		GUIHelper.addLabeledComponent(panel, I18N.t("dialog.source.publisher") + ":", publisherField);

		// date
		GUIHelper.addLabeledComponent(panel, I18N.t("dialog.source.date") + ":", dateField);

		// place
		GUIHelper.addLabeledComponent(panel, I18N.t("dialog.place") + ":", placeField);

		// media type
		GUIHelper.addLabeledComponent(panel, I18N.t("dialog.source.media.type") + ":", mediaTypeCombo);

		// repository
		final JPanel repositoryCitationPanel = components.getPanel(PanelKey.REPOSITORY);
		GUIHelper.addComponent(panel, repositoryCitationPanel);

		// document
		final JPanel documentPanel = components.getPanel(PanelKey.DOCUMENT);
		GUIHelper.addComponent(panel, documentPanel);

		return panel;
	}

	@Override
	protected JPanel createResearchPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		// conclusion
		final JPanel conclusionPanel = components.getPanel(PanelKey.CONCLUSION_ON_RESOLVES);
		GUIHelper.addComponent(panel, conclusionPanel);

		// research question
		final JPanel researchQuestionPanel = components.getPanel(PanelKey.RESEARCH_QUESTION_ON_TARGET);
		GUIHelper.addComponent(panel, researchQuestionPanel);

		// research activity
		final JPanel researchActivityPanel = components.getPanel(PanelKey.RESEARCH_ACTIVITY_ON_SOURCE);
		GUIHelper.addComponent(panel, researchActivityPanel);

		return panel;
	}

	@Override
	protected JPanel createSourcesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel repositoryPanel = components.getPanel(PanelKey.REPOSITORY);
		GUIHelper.addComponent(panel, repositoryPanel);

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
		titlePanel.load(record);
		dateField.load(record);
		placeField.load(record);

		components.load(record);
	}

	@Override
	protected void saveData(){
		titlePanel.save(record);
		dateField.save(record);
		placeField.saveReferences(record);

		components.save(record);
	}


	public static void main(final String[] args) throws IOException{
		GUIHelper.launch(SourceRecordDialog::createEdit, "/tests/test.flef", "S1");
	}

}
