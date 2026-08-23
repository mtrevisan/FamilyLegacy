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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.PlaceCitationField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.Dialog;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;


/**
 * Dialog for editing an {@code EVENT_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record EventRecord {
 *   id: LocalID
 *   type: enum {
 *     birth, death, adoption, graduation, immigration, naturalization, bankruptcy,
 *     guardianship, coroner_report, cremation, burial, education, retirement,
 *     military_induction, military_muster_roll, military_service, military_award,
 *     military_release, military_discharge, military_resignation, military_retirement,
 *     prison, pardon, jury_duty, illness, hospitalization, medical_procedure, honor,
 *     deportation, internment, liberation, emancipation, relocation, emigration,
 *     census, deed, escrow, chancery, will, probate,
 *     engagement, marriage_bann, marriage_contract, marriage_license, marriage_settlement,
 *     marriage, divorce_filed, divorce_decree, divorce, annulment
 *   } | Text
 *   description?: Text
 *   date?: DateStructure
 *   place?: PlaceCitation
 *   agency?: Text
 *   cause?: struct {
 *     value: Text
 *     evidence?: EvidenceQualifiers
 *   }
 *   source*: SourceCitation
 *   note*: Xref&lt;NoteRecord&gt;
 *   evidence?: EvidenceQualifiers
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): type, description, date, place, agency, cause, evidence
 * Tab 4 (Participations): EventParticipationRecord (event = this event)
 * Tab 5 (Context): ContextImpactRecord (target[event] = this event)
 * Tab 6 (Research): ConclusionRecord (resolves = this event), ResearchQuestionRecord (target[event] = this event)
 * Tab 7 (Sources): source
 * Tab 8 (Notes): note
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class EventRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -9191829528682252778L;


	private static final String DOT = ".";

	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_DESCRIPTION = "DESCRIPTION";
	private static final String TAG_DATE = "DATE";
	private static final String TAG_PLACE = "PLACE";
	private static final String TAG_AGENCY = "AGENCY";
	private static final String TAG_CAUSE = "CAUSE";
	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_CONTEXT_IMPACT = "CONTEXT_IMPACT";
	private static final String TAG_CONCLUSION = "CONCLUSION";
	private static final String TAG_EVENT_PARTICIPATION = "EVENT_PARTICIPATION";
	private static final String TAG_RESEARCH_QUESTION = "RESEARCH_QUESTION";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_EVIDENCE = "EVIDENCE";
	private static final String TAG_PRIVACY = "PRIVACY";
	private static final String TAG_AUDIT = "AUDIT";


	private final RecordDialogComponents components;

	private final JPanel propertiesPanel;

	private final BoundComboBox<String> typeCombo;
	private final BoundTextArea titleArea;
	private final DateField dateField;
	private final PlaceCitationField placeCitationField;
	private final BoundTextField agencyField;
	private final BoundTextField causeField;
	private final EvidenceQualifiersPanel causeEvidencePanel;


	public static EventRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, EventRecordDialog::new);
	}

	public static EventRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, EventRecordDialog::new);
	}


	private EventRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, EventHandler.class);

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]15[]10[]15[]15[]15[]");

		typeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{
			StringUtils.EMPTY,
			"birth", "death", "adoption", "graduation", "immigration", "naturalization", "bankruptcy", "guardianship",
			"coroner_report", "cremation", "burial", "education", "retirement", "military_induction",
			"military_muster_roll", "military_service", "military_award", "military_release", "military_discharge",
			"military_resignation", "military_retirement", "prison", "pardon", "jury_duty", "illness", "hospitalization",
			"medical_procedure", "honor", "deportation", "internment", "liberation", "emancipation", "relocation",
			"emigration", "census", "deed", "escrow", "chancery", "will", "probate", "engagement", "marriage_bann",
			"marriage_contract", "marriage_license", "marriage_settlement", "marriage", "divorce_filed", "divorce_decree",
			"divorce", "annulment"
		});
		typeCombo.setEditable(true);
		titleArea = new BoundTextArea(TAG_DESCRIPTION, 3, 25);
		dateField = DateField.createWithWrapperTag(TAG_DATE, this, "Date", model);
		placeCitationField = PlaceCitationField.create(TAG_PLACE, this, model);
		agencyField = new BoundTextField(TAG_AGENCY);
		causeField = new BoundTextField(TAG_CAUSE + DOT + TAG_VALUE);
		causeEvidencePanel = new EvidenceQualifiersPanel(TAG_CAUSE + DOT + TAG_EVIDENCE, this, "Cause Evidence", model, null);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_TARGET, TAG_CONTEXT_IMPACT, "Context Impacts", ContextImpactHandler.class, EventHandler.class)
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, TAG_CONCLUSION, "Conclusions", ConclusionHandler.class, EventHandler.class)
			.withComponent(PanelKey.EVENT_PARTICIPATION_ON_EVENT, TAG_EVENT_PARTICIPATION, "Participations", EventParticipationHandler.class, EventHandler.class)
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, TAG_RESEARCH_QUESTION, "Research Questions", ResearchQuestionHandler.class, EventHandler.class)
			.withComponent(PanelKey.SOURCE, TAG_SOURCE, "Sources with Citations", SourceHandler.class, SourceCitationHandler.class)
			.withComponent(PanelKey.NOTE, TAG_NOTE, "Notes", NoteHandler.class, NoteHandler.class)
			.withComponent(PanelKey.EVIDENCE, TAG_EVIDENCE, "Evidence", null, EventHandler.class)
			.withComponent(PanelKey.PRIVACY, TAG_PRIVACY, null, null, null)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null, null, null)
			.build();

		components.bind(typeCombo);
		components.bind(titleArea);
		components.bind(agencyField);
		components.bind(causeField);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// type
		GUIHelper.addLabeledComponent(propertiesPanel, "Type*:", typeCombo);

		// description
		GUIHelper.addLabeledComponent(propertiesPanel, "Description*:", titleArea);

		// date
		GUIHelper.addLabeledComponent(propertiesPanel, "Date:", dateField);

		// place
		GUIHelper.addLabeledComponent(propertiesPanel, "Place:", placeCitationField);

		// agency
		GUIHelper.addLabeledComponent(propertiesPanel, "Agency:", agencyField);

		// cause panel:
		final JPanel causePanel = GUIHelper.createLabelFieldPanel(5, "[]10[]");
		causePanel.setBorder(new TitledBorder("Cause"));
		GUIHelper.addComponent(causePanel, causeField);
		GUIHelper.addComponent(causePanel, causeEvidencePanel);
		GUIHelper.addComponent(propertiesPanel, causePanel);

		// evidence
		final JPanel evidencePanel = components.getPanel(PanelKey.EVIDENCE);
		GUIHelper.addComponent(propertiesPanel, evidencePanel);

		return propertiesPanel;
	}

	@Override
	protected JPanel createParticipationsPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel eventParticipationPanel = components.getPanel(PanelKey.EVENT_PARTICIPATION_ON_EVENT);
		GUIHelper.addComponent(panel, eventParticipationPanel);

		return panel;
	}

	@Override
	protected JPanel createContextPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel contextPanel = components.getPanel(PanelKey.CONTEXT_IMPACT_ON_TARGET);
		GUIHelper.addComponent(panel, contextPanel);

		return panel;
	}

	@Override
	protected JPanel createResearchPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]15[]");

		final JPanel conclusionPanel = components.getPanel(PanelKey.CONCLUSION_ON_RESOLVES);
		GUIHelper.addComponent(panel, conclusionPanel);

		final JPanel researchQuestionPanel = components.getPanel(PanelKey.RESEARCH_QUESTION_ON_TARGET);
		GUIHelper.addComponent(panel, researchQuestionPanel);

		return panel;
	}

	@Override
	protected JPanel createSourcesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel sourcePanel = components.getPanel(PanelKey.SOURCE);
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

		dateField.load(record);
		placeCitationField.load(record);
		causeEvidencePanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(!typeCombo.isValued()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Type is required.",
				tabbedPane, propertiesPanel, typeCombo);

			return false;
		}

		if(titleArea.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Description is required.",
				tabbedPane, propertiesPanel, titleArea);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		components.save(record);

		dateField.save(record);
		placeCitationField.saveReferences(record);
		causeEvidencePanel.save(record);
	}



	public static void main(final String[] args) throws IOException{
		try(final InputStream is = EventRecordDialog.class.getResourceAsStream("/tests/test.flef")){
			final String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			GUIHelper.launch(EventRecordDialog::createEdit, content, "E1");
		}
	}

}
