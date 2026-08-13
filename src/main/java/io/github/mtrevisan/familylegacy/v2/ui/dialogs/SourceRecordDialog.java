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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.PlaceCitationField;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityReferenceListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NameHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.Serial;
import java.util.function.Consumer;


/**
 * Dialog for editing a {@code SOURCE_RECORD} according to FLEF 0.1.1.
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
 * Tab 6 (Research): ConclusionRecord (resolves/preferred = this source), ResearchQuestionRecord (target.source = this source), ResearchActivityRecord (source contains this source)
 * Tab 8 (Notes): note
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class SourceRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 8722200901398839002L;


	private static final String DOT = ".";

	private static final String TAG_TITLE = "TITLE";
	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_AUTHOR = "AUTHOR";
	private static final String TAG_PUBLISHER = "PUBLISHER";
	private static final String TAG_DATE = "DATE";
	private static final String TAG_PLACE = "PLACE";
	private static final String TAG_MEDIA_TYPE = "MEDIA_TYPE";
	private static final String TAG_REPOSITORY = "REPOSITORY";
	private static final String TAG_DOCUMENT = "DOCUMENT";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_CONCLUSION = "CONCLUSION";
	private static final String TAG_RESEARCH_QUESTION = "RESEARCH_QUESTION";
	private static final String TAG_RESEARCH_ACTIVITY = "RESEARCH_ACTIVITY";
	private static final String TAG_PRIVACY = "PRIVACY";
	private static final String TAG_AUDIT = "AUDIT";


	private final RecordDialogComponents components;

	private final EntityReferenceListPanel titlePanel;
	private final BoundTextField authorField;
	private final BoundTextField publisherField;
	private final DateField dateField;
	private final PlaceCitationField placeCitationField;
	private final BoundComboBox<String> mediaTypeCombo;


	public static SourceRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, SourceRecordDialog::new);
	}

	public static SourceRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, SourceRecordDialog::new);
	}


	private SourceRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, SourceHandler.class);

		titlePanel = EntityReferenceListPanel.createForStructure(TAG_TITLE, this, "Title*", model, NameHandler.class);
		authorField = new BoundTextField(TAG_AUTHOR);
		publisherField = new BoundTextField(TAG_PUBLISHER);
		dateField = DateField.createWithWrapperTag(TAG_DATE, this, "Valid Date", model);
		placeCitationField = PlaceCitationField.create(TAG_PLACE, this, model);
		mediaTypeCombo = new BoundComboBox<>(TAG_MEDIA_TYPE, new String[]{
			StringUtils.EMPTY,
			"audio", "book", "card", "electronic", "fiche", "film",
			"magazine", "manuscript", "map", "newspaper", "photo",
			"tombstone", "video"
		});
		mediaTypeCombo.setEditable(true);

		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.REPOSITORY, TAG_REPOSITORY, "Repositories", RepositoryCitationHandler.class, SourceHandler.class)
			.withComponent(PanelKey.DOCUMENT, TAG_DOCUMENT, "Documents", DocumentHandler.class, SourceHandler.class)
			.withComponent(PanelKey.NOTE, TAG_NOTE, "Notes", NoteHandler.class, SourceHandler.class)
			.withComponent(PanelKey.CONCLUSION, TAG_CONCLUSION, "Conclusions", ConclusionHandler.class, SourceHandler.class)
			.withComponent(PanelKey.RESEARCH_QUESTION, TAG_RESEARCH_QUESTION, "Research Questions", ResearchQuestionHandler.class, SourceHandler.class)
			.withComponent(PanelKey.RESEARCH_ACTIVITY_ON_SOURCE, TAG_RESEARCH_ACTIVITY, "Research Activities", ResearchActivityHandler.class, SourceHandler.class)
			.withComponent(PanelKey.PRIVACY, TAG_PRIVACY, null, null, null)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null, null, null)
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
		GUIHelper.addLabeledComponent(panel, "Author:", authorField);

		// publisher
		GUIHelper.addLabeledComponent(panel, "Publisher:", publisherField);

		// date
		GUIHelper.addLabeledComponent(panel, "Date:", dateField);

		// place
		GUIHelper.addLabeledComponent(panel, "Place:", placeCitationField);

		// media type
		GUIHelper.addLabeledComponent(panel, "Media Type:", mediaTypeCombo);

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
		final JPanel conclusionPanel = components.getPanel(PanelKey.CONCLUSION);
		GUIHelper.addComponent(panel, conclusionPanel);

		// research question
		final JPanel researchQuestionPanel = components.getPanel(PanelKey.RESEARCH_QUESTION);
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
		placeCitationField.load(record);

		components.load(record);
	}

	@Override
	protected void saveData(){
		titlePanel.save(record);
		dateField.save(record);
		placeCitationField.saveReferences(record);

		components.save(record);
	}


	public static void main(final String[] args){
		final FLEFRecord source = FLEFRecord.createMainRecord("S1", "SOURCE");
		source.addChild(FLEFRecord.createChildWithTagAndValue("AUTHOR", "auth"));
		source.addChild(FLEFRecord.createChildWithTagAndValue("PUBLISHER", "pub"));
		source.addChild(FLEFRecord.createChildWithTag("DATE")
			.addChild(FLEFRecord.createChildWithTag("VALUE")
				.addChild(FLEFRecord.createChildWithTag("POINT")
					.addChild(FLEFRecord.createChildWithTag("SINGLE_DATE")
//						.addChild(FLEFRecord.createChildWithTag("FULL_DATE")
//							.addChild(FLEFRecord.createChildWithTagAndValue("VALUE", "2001-01-01"))
//						)
//						.addChild(FLEFRecord.createChildWithTag("DECADE")
//							.addChild(FLEFRecord.createChildWithTagAndValue("START_YEAR", "1940"))
//						)
						.addChild(FLEFRecord.createChildWithTag("CENTURY")
							.addChild(FLEFRecord.createChildWithTagAndValue("ORDINAL", "19"))
							.addChild(FLEFRecord.createChildWithTagAndValue("PART", "second_quarter"))
						)
					)
				)
			)
		);


		final Consumer<FLEFModel> modelFiller = model -> {
			model.addRecord(source);
		};
		GUIHelper.launch(SourceRecordDialog::createEdit, modelFiller, source);
	}

}
