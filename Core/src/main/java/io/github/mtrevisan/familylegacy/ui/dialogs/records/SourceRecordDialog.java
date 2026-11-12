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

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.sources.SourceHelper;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.PanelKey;
import io.github.mtrevisan.familylegacy.ui.dialogs.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.ui.dialogs.structures.DateField;
import io.github.mtrevisan.familylegacy.ui.handlers.AgentHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.NameHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceCitationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;

import javax.swing.JPanel;
import javax.swing.ListModel;
import java.awt.Window;
import java.io.IOException;


/**
 * Dialog for editing a {@code SOURCE_RECORD} according to FLEF 0.2.0.
 * <p>
 * Structure:
 * <pre>
 * record SourceRecord {
 *   title+: NameStructure
 *   author*: Agent             // the person, agency, or entity who created the record. For a published work, this could be the author, compiler, transcriber, abstractor, or editor. For an unpublished source, this may be an individual, a government agency, church organization, or private organization, etc.
 *   publisher?: Text
 *   date?: DateStructure
 *   place?: PlaceCitation
 *   media_type?: enum { audio, book, card, electronic, fiche, film, magazine, manuscript, map, newspaper, photo, tombstone, video } | Text
 *   repository*: RepositoryCitation
 *   media*: Xref&lt;MediaRecord&gt;
 *   derived_from*: SourceCitation   // the sources this one was made from (a transcription, an abstract, an index, a copy, a compilation); an original has none
 *   note*: Xref&lt;NoteRecord&gt;
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 *
 * Agent = oneof {
 *  individual: Xref&lt;IndividualRecord&gt;   // the author is an individual described in this file
 *  group: Xref&lt;GroupRecord&gt;             // the author is a group or organization described in this file (e.g. a parish, a confraternity)
 *  text: Text                           // the author is not described in this file (e.g. a publisher's editor, an unidentified clerk)
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): title, author, publisher, date, place, media_type, repository, document
 * Tab 6 (Research): ConclusionRecord (resolves = this source), ResearchQuestionRecord (target[source] = this source), ResearchActivityRecord (source contains this source)
 * Tab 7 (Sources): repository sources, derived_from (the sources this one derives from)
 * Tab 8 (Notes): note
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class SourceRecordDialog extends BaseRecordDialog{

	private final EntityListPanel titlePanel;
	private final EntityListPanel authorPanel;
	private final BoundTextField publisherField;
	private final DateField dateField;
	private final EntityField placeField;
	private final BoundComboBox<String> mediaTypeCombo;
	private JPanel propertiesPanel;
	private JPanel sourcesPanel;
	/** The tree of the derivations, shown only when there are enough related sources. */
	private SourceDerivationPanel derivationPanel;


	public static SourceRecordDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, SourceRecordDialog::new);
	}

	public static SourceRecordDialog createEdit(final Window parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, SourceRecordDialog::new);
	}


	private SourceRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, SourceHandler.getInstance());

		titlePanel = EntityListPanel.createForStructure(SourceReader.TAG_TITLE, this, GUIHelper.requiredTitle("dialog.source.title"), model,
			NameHandler.class);
		authorPanel = EntityListPanel.createForStructure(SourceReader.TAG_AUTHOR, this, I18N.t("dialog.source.author"), model,
			AgentHandler.class);
		publisherField = new BoundTextField(SourceReader.TAG_PUBLISHER);
		dateField = DateField.createWithWrapperTag(SourceReader.TAG_DATE, this, I18N.t("dialog.source.valid_date"), model);
		placeField = EntityField.createForStructureWithReference(SourceReader.TAG_PLACE, this, model,
			PlaceCitationHandler.class);
		mediaTypeCombo = new BoundComboBox<>(SourceReader.TAG_MEDIA_TYPE, GUIHelper.fillCombo(SourceReader.MEDIA_TYPES, null));
		mediaTypeCombo.setI18NPrefix("enum.source.media_type");
		mediaTypeCombo.setEditable(true);

		components = new RecordDialogBuilder(this, model, this.record)
			.withComponent(PanelKey.REPOSITORY, SourceReader.TAG_REPOSITORY, I18N.t("dialog.component.repositories_with_citations"))
			.withComponent(PanelKey.DOCUMENT, SourceReader.TAG_MEDIA, I18N.t("dialog.component.media"))
			.withComponent(PanelKey.SOURCE, SourceReader.TAG_DERIVED_FROM, I18N.t("dialog.component.derived_from"))
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, ConclusionHandler.TYPE, I18N.t("dialog.component.conclusions"))
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, ResearchQuestionHandler.TYPE, I18N.t("dialog.component.research.questions"))
			.withComponent(PanelKey.RESEARCH_ACTIVITY_ON_SOURCE, ResearchActivityHandler.TYPE, I18N.t("dialog.component.research.activities"))
			.withComponent(PanelKey.NOTE, SourceReader.TAG_NOTE, null)
			.withComponent(PanelKey.PRIVACY, SourceReader.TAG_PRIVACY, null)
			.withComponent(PanelKey.AUDIT, SourceReader.TAG_AUDIT, null)
			.build();

		components.bind(publisherField);
		components.bind(mediaTypeCombo);

		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]5[]5[]5[]5[]5[]10[]");
		propertiesPanel = panel;

		// title
		GUIHelper.addComponent(panel, titlePanel);

		// author
		GUIHelper.addComponent(panel, authorPanel);

		// publisher
		GUIHelper.addLabeledComponent(panel, I18N.l("dialog.source.publisher"), publisherField);

		// date
		GUIHelper.addLabeledComponent(panel, I18N.l("dialog.source.date"), dateField);

		// place
		GUIHelper.addLabeledComponent(panel, I18N.l("dialog.place"), placeField);

		// media type
		GUIHelper.addLabeledComponent(panel, I18N.l("dialog.source.media_type"), mediaTypeCombo);

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

		//the sources this one derives from
		final JPanel derivedFromPanel = components.getPanel(PanelKey.SOURCE);
		GUIHelper.addComponent(panel, derivedFromPanel);

		//the tree of the derivations, above and below this source
		derivationPanel = new SourceDerivationPanel(model);
		GUIHelper.addComponent(panel, derivationPanel);

		sourcesPanel = panel;
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
		authorPanel.load(record);
		dateField.load(record);
		placeField.load(record);

		components.load(record);
		derivationPanel.load(record.getId());
	}

	/** A source cannot be derived from itself, directly or through other sources (the derivations MUST be acyclic). */
	@Override
	protected boolean validData(){
		if(!titlePanel.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.source.title")),
				tabbedPane, propertiesPanel, titlePanel.getList());

			return false;
		}

		final EntityListPanel derivedFromPanel = (EntityListPanel)components.getPanel(PanelKey.SOURCE);
		final ListModel<FLEFRecord> citations = derivedFromPanel.getList()
			.getModel();
		for(int i = 0; i < citations.getSize(); i ++){
			final String originalId = SourceHelper.extractReferencedId(citations.getElementAt(i));
			if(originalId != null && SourceHelper.isOrDerivesFrom(model, originalId, record.getId())){
				GUIHelper.showValidationErrorAndFocus(this,
					I18N.tf("validation.source_derivation_cycle", originalId),
					tabbedPane, sourcesPanel, derivedFromPanel.getList());

				return false;
			}
		}

		return true;
	}

	@Override
	protected void saveData(){
		titlePanel.save(record);
		authorPanel.save(record);
		dateField.save(record);
		placeField.saveReferences(record);

		components.save(record);
	}


	static void main(final String[] args) throws IOException{
		GUIHelper.launch(SourceRecordDialog::createEdit, "/tests/test.flef", "S1");
	}

}
