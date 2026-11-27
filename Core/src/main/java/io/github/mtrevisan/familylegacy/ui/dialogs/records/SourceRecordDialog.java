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
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
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
import io.github.mtrevisan.familylegacy.ui.handlers.IdentifierHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.NameHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceCitationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchTaskHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListModel;
import java.awt.Window;
import java.io.IOException;
import java.util.List;


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
 *   kind?: enum { civil_registration, church_register, census, ..., compilation, artifact } | Text   // what the source IS, not its physical form
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
 * Tab 1 (Properties): title, author, publisher, date, place, kind, media_type, repository, document
 * Tab 6 (Research): ConclusionRecord (resolves = this source), ResearchQuestionRecord (target[source] = this source), ResearchActivityRecord (source contains this source)
 * Tab 7 (Sources): repository sources, derived_from (the sources this one derives from)
 * Tab 8 (Notes): note (the stories, from the standard level, in a list above the others)
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class SourceRecordDialog extends BaseRecordDialog{

	/** Whether the stories have a tab of their own (from the standard level), or are listed with the other notes. */
	private final boolean storiesApart;

	private final EntityListPanel titlePanel;
	private final EntityListPanel authorPanel;
	private final EntityListPanel identifierPanel;
	private final BoundTextField publisherField;
	private final DateField dateField;
	private final EntityField placeField;
	private final BoundComboBox<String> kindCombo;
	private final BoundComboBox<String> mediaTypeCombo;
	/**
	 * The title as a plain text, for a source that has none or one plain title (the common case: one title, no type, no language, no
	 * variants): the list of the titles, with a dialog for each, is for the others and a click away.
	 */
	private final JTextField titleField = new JTextField(40);
	private final JButton severalTitlesButton = new JButton(I18N.t("dialog.source.title.several"));
	private final JPanel titleRow = new JPanel(new MigLayout("ins 0,fillx", "[grow,fill]rel[]"));
	private boolean plainTitle = true;
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

		storiesApart = storiesApart();

		titlePanel = EntityListPanel.createForStructure(SourceReader.TAG_TITLE, this, GUIHelper.requiredTitle("dialog.source.title"), model,
			NameHandler.class);
		severalTitlesButton.setToolTipText(I18N.t("dialog.source.title.several.tooltip"));
		severalTitlesButton.addActionListener(e -> showTitleList());
		titleRow.add(titleField, "growx");
		titleRow.add(severalTitlesButton);
		authorPanel = EntityListPanel.createForStructure(SourceReader.TAG_AUTHOR, this, I18N.t("dialog.source.author"), model,
			AgentHandler.class);
		identifierPanel = EntityListPanel.createForStructure(SourceReader.TAG_IDENTIFIER, this, I18N.t("dialog.source.identifiers"), model,
			IdentifierHandler.class);
		publisherField = new BoundTextField(SourceReader.TAG_PUBLISHER);
		dateField = DateField.createWithWrapperTag(SourceReader.TAG_DATE, this, I18N.t("dialog.source.valid_date"), model);
		placeField = EntityField.createForStructureWithReference(SourceReader.TAG_PLACE, this, model,
			PlaceCitationHandler.class);
		kindCombo = new BoundComboBox<>(SourceReader.TAG_KIND, GUIHelper.fillCombo(SourceReader.KINDS, null));
		kindCombo.setI18NPrefix("enum.source.kind");
		kindCombo.setEditable(true);
		mediaTypeCombo = new BoundComboBox<>(SourceReader.TAG_MEDIA_TYPE, GUIHelper.fillCombo(SourceReader.MEDIA_TYPES, null));
		mediaTypeCombo.setI18NPrefix("enum.source.media_type");
		mediaTypeCombo.setEditable(true);

		components = new RecordDialogBuilder(this, model, this.record)
			.withComponent(PanelKey.REPOSITORY, SourceReader.TAG_REPOSITORY, I18N.t("dialog.component.repositories_with_citations"))
			.withComponent(PanelKey.DOCUMENT, SourceReader.TAG_MEDIA, I18N.t("dialog.component.media"))
			.withComponent(PanelKey.SOURCE, SourceReader.TAG_DERIVED_FROM, I18N.t("dialog.component.derived_from"))
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, ConclusionHandler.TYPE, I18N.t("dialog.component.conclusions"))
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, ResearchQuestionHandler.TYPE, I18N.t("dialog.component.research.questions"))
			.withComponent(PanelKey.RESEARCH_TASK_ON_RELATED, ResearchTaskHandler.TYPE, I18N.t("dialog.component.research.tasks"))
			.withComponent(PanelKey.RESEARCH_ACTIVITY_ON_SOURCE, ResearchActivityHandler.TYPE, I18N.t("dialog.component.research.activities"))
			//the stories are apart from the notes
			.withComponent(PanelKey.storyPanel(storiesApart), SourceReader.TAG_NOTE, storiesApart? I18N.t("dialog.component.stories"): null)
			.withComponent(PanelKey.notePanel(storiesApart), SourceReader.TAG_NOTE, storiesApart? I18N.t("dialog.component.notes_except_stories"): null)
			.withComponent(PanelKey.PRIVACY, SourceReader.TAG_PRIVACY, null)
			.withComponent(PanelKey.AUDIT, SourceReader.TAG_AUDIT, null)
			.build();

		components.bind(publisherField);
		components.bind(kindCombo);
		components.bind(mediaTypeCombo);

		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]5[]5[]5[]5[]5[]5[]5[]10[]");
		propertiesPanel = panel;

		// title: a text, or the list of the titles
		GUIHelper.addLabeledComponent(panel, GUIHelper.requiredLabel("dialog.source.title"), titleRow);
		GUIHelper.addComponent(panel, titlePanel);

		// author
		GUIHelper.addComponent(panel, authorPanel);

		// publisher
		GUIHelper.addLabeledComponent(panel, I18N.l("dialog.source.publisher"), publisherField);

		// date
		GUIHelper.addLabeledComponent(panel, I18N.l("dialog.source.date"), dateField);

		// place
		GUIHelper.addLabeledComponent(panel, I18N.l("dialog.place"), placeField);

		// kind
		GUIHelper.addLabeledComponent(panel, I18N.l("dialog.source.kind"), kindCombo);

		// media type
		GUIHelper.addLabeledComponent(panel, I18N.l("dialog.source.media_type"), mediaTypeCombo);

		// identifiers of the source itself (ISBN, DOI, ...)
		GUIHelper.addComponent(panel, identifierPanel);

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
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]");

		// conclusion
		final JPanel conclusionPanel = components.getPanel(PanelKey.CONCLUSION_ON_RESOLVES);
		GUIHelper.addComponent(panel, conclusionPanel);

		// research question
		final JPanel researchQuestionPanel = components.getPanel(PanelKey.RESEARCH_QUESTION_ON_TARGET);
		GUIHelper.addComponent(panel, researchQuestionPanel);

		// research tasks that name this record
		GUIHelper.addComponent(panel, components.getPanel(PanelKey.RESEARCH_TASK_ON_RELATED));

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
		//the one tab of the notes: from the standard level it has the list of the stories above the one of the other notes
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, storiesApart? "[]15[]": "[]");

		if(storiesApart)
			GUIHelper.addComponent(panel, components.getPanel(PanelKey.storyPanel(true)));
		GUIHelper.addComponent(panel, components.getPanel(PanelKey.notePanel(storiesApart)));

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


	/** @return	Whether the title is a plain one (nothing but its text): the ones that the text field can show. */
	private static boolean isPlainTitle(final FLEFRecord title){
		return (title.childCount() == 1 && Strings.CI.equals(SourceReader.TAG_TITLE_VALUE, title.childAt(0).getTag()));
	}

	/** Shows the text field or the list of the titles. */
	private void applyTitleMode(){
		titleRow.setVisible(plainTitle);
		final javax.swing.JLabel label = GUIHelper.getLabeledComponent(titleRow);
		if(label != null)
			label.setVisible(plainTitle);
		titlePanel.setVisible(!plainTitle);
		if(propertiesPanel != null){
			propertiesPanel.revalidate();
			propertiesPanel.repaint();
		}
	}

	/** Puts the text of the field in the title of the source (the only one: the field is shown only for a source that has at most one). */
	private void syncTitle(){
		final String text = StringUtils.normalizeSpace(titleField.getText());
		if(text.isEmpty())
			return;

		final List<FLEFRecord> titles = titlePanel.getItems();
		if(titles.isEmpty())
			titlePanel.addItemDirectly(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
				.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_TITLE_VALUE, text)));
		else if(!text.equals(FLEFRecordHelper.getChildValue(titles.getFirst(), SourceReader.TAG_TITLE_VALUE))){
			FLEFRecordHelper.updateChildValue(titles.getFirst(), SourceReader.TAG_TITLE_VALUE, text);
			titlePanel.getList()
				.repaint();
		}
	}

	/** Goes from the text field to the list of the titles (the text typed is the first title), to add the others or to edit them one by one. */
	private void showTitleList(){
		syncTitle();

		plainTitle = false;
		applyTitleMode();
	}


	@Override
	protected void loadData(){
		titlePanel.load(record);
		final List<FLEFRecord> titles = titlePanel.getItems();
		plainTitle = (titles.isEmpty() || titles.size() == 1 && isPlainTitle(titles.getFirst()));
		titleField.setText(titles.isEmpty()
			? StringUtils.EMPTY
			: StringUtils.defaultString(FLEFRecordHelper.getChildValue(titles.getFirst(), SourceReader.TAG_TITLE_VALUE)));
		applyTitleMode();
		authorPanel.load(record);
		identifierPanel.load(record);
		dateField.load(record);
		placeField.load(record);

		components.load(record);
		derivationPanel.load(record.getId());
	}

	/** A source cannot be derived from itself, directly or through other sources (the derivations MUST be acyclic). */
	@Override
	protected boolean validData(){
		if(plainTitle? StringUtils.isBlank(titleField.getText()): !titlePanel.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.source.title")),
				tabbedPane, propertiesPanel, (plainTitle? titleField: titlePanel.getList()));

			return false;
		}

		final EntityListPanel derivedFromPanel = (EntityListPanel)components.getPanel(PanelKey.SOURCE);
		final ListModel<FLEFRecord> citations = derivedFromPanel.getList()
			.getModel();
		for(int i = 0; i < citations.getSize(); i ++){
			final String originalId = SourceHelper.extractReferencedId(citations.getElementAt(i));
			if(SourceHelper.isOrDerivesFrom(model, originalId, record.getId())){
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
		if(plainTitle)
			syncTitle();
		titlePanel.save(record);
		authorPanel.save(record);
		identifierPanel.save(record);
		dateField.save(record);
		placeField.saveReferences(record);

		components.save(record);
	}


	static void main(final String[] args) throws IOException{
		GUIHelper.launch(SourceRecordDialog::createEdit, "/tests/test.flef", "S1");
	}

}
