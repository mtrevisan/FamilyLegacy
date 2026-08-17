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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.ParticipantField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.Serial;
import java.util.function.Consumer;


/**
 * Dialog for editing an {@code IDENTITY_HYPOTHESIS_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record IdentityHypothesisRecord {
 *   id: LocalID
 *   subject: IdentityCandidate
 *   candidate: IdentityCandidate
 *   comment?: Text
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 *   audit: AuditStructure
 *
 *   require subject != candidate
 * }
 *
 * IdentityCandidate = oneof {
 *   individual: Xref&lt;IndividualRecord&gt;
 *   group: Xref&lt;GroupRecord&gt;
 *   place: Xref&lt;PlaceRecord&gt;
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): subject, candidate, comment, evidence
 * Tab 5 (Context): ContextImpactRecord (target.identity_hypothesis = this hypothesis)
 * Tab 6 (Research): ConclusionRecord (resolves/preferred = this hypothesis), ResearchQuestionRecord (target.identity_hypothesis = this hypothesis)
 * Tab 7 (Sources): source
 * Tab 10 (Audit): audit
 */
public class IdentityHypothesisRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -3743748718107492890L;


	private static final String TAG_SUBJECT = "SUBJECT";
	private static final String TAG_CANDIDATE = "CANDIDATE";
	private static final String TAG_COMMENT = "COMMENT";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_EVIDENCE = "EVIDENCE";
	private static final String TAG_CONTEXT_IMPACT = "CONTEXT_IMPACT";
	private static final String TAG_CONCLUSION = "CONCLUSION";
	private static final String TAG_RESEARCH_QUESTION = "RESEARCH_QUESTION";
	private static final String TAG_AUDIT = "AUDIT";


	private final RecordDialogComponents components;

	private final JPanel propertiesPanel;

	private final ParticipantField candidateField;
	private final BoundTextArea commentArea;


	public static IdentityHypothesisRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, IdentityHypothesisRecordDialog::new);
	}

	public static IdentityHypothesisRecordDialog createEdit(final Dialog parent, final FLEFModel model,
		final FLEFRecord record){
		return createEdit(parent, model, record, IdentityHypothesisRecordDialog::new);
	}


	private IdentityHypothesisRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, IdentityHypothesisHandler.class);

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]");

		candidateField = ParticipantField.create(TAG_CANDIDATE, this, model);
		candidateField.setHandlerTypes(IndividualHandler.class, GroupHandler.class, PlaceHandler.class);
		commentArea = new BoundTextArea(TAG_COMMENT, 3, 30);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_TARGET, TAG_CONTEXT_IMPACT, "Context Impacts", ContextImpactHandler.class, IdentityHypothesisHandler.class)
			.withComponent(PanelKey.CONCLUSION, TAG_CONCLUSION, "Conclusions", ConclusionHandler.class, IdentityHypothesisHandler.class)
			.withComponent(PanelKey.RESEARCH_QUESTION, TAG_RESEARCH_QUESTION, "Research Questions", ResearchQuestionHandler.class, IdentityHypothesisHandler.class)
			.withComponent(PanelKey.SOURCE, TAG_SOURCE, "Sources", SourceHandler.class, SourceHandler.class)
			.withComponent(PanelKey.EVIDENCE, TAG_EVIDENCE, "Evidence", null, IdentityHypothesisHandler.class)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null, null, null)
			.build();

		components.bind(commentArea);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// subject
		//parentEntity

		// candidate
		GUIHelper.addLabeledComponent(propertiesPanel, "Candidate*:", candidateField);

		// comment
		GUIHelper.addLabeledComponent(propertiesPanel, "Comment:", commentArea);

		// evidence
		final JPanel evidencePanel = components.getPanel(PanelKey.EVIDENCE);
		GUIHelper.addComponent(propertiesPanel, evidencePanel);

		return propertiesPanel;
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
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]");

		final JPanel conclusionPanel = components.getPanel(PanelKey.CONCLUSION);
		GUIHelper.addComponent(panel, conclusionPanel);

		final JPanel researchQuestionPanel = components.getPanel(PanelKey.RESEARCH_QUESTION);
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
	protected JPanel createAuditPanel(){
		return components.getPanel(PanelKey.AUDIT);
	}


	@Override
	protected void loadData(){
		if(record == null)
			return;

		final FLEFRecord participant = record.getTheOnlyChild(TAG_CANDIDATE);
		candidateField.load(participant != null? participant.getTheOnlyChild(): null);

		components.load(record);


		// load parent subject reference
		final FLEFRecord subject = FLEFRecordHelper.findChild(record, TAG_SUBJECT);
		if(subject != null){
			final FLEFRecord subjectEntity = subject.getTheOnlyChild();
			withParentEntity(subjectEntity.getValue(), subjectEntity.getTag());
		}
	}

	@Override
	protected boolean validData(){
		if(StringUtils.isEmpty(parentEntity.getPath()) || StringUtils.isEmpty(parentEntity.getText())){
			JOptionPane.showMessageDialog(this,
				"Subject is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		if(!candidateField.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Candidate is required.",
				tabbedPane, propertiesPanel, candidateField);

			return false;
		}

		// Subject and candidate must be different records
		final String subjectId = parentEntity.getText();
		final FLEFRecord candidate = candidateField.getParticipantRecord();
		final String candidateId = (candidate != null? candidate.getId(): null);
		if(subjectId != null && subjectId.equals(candidateId)){
			GUIHelper.showValidationErrorAndFocus(this,
				"Subject and candidate must be different records.",
				tabbedPane, propertiesPanel, candidateField);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		FLEFRecordHelper.removeChildren(record, TAG_SUBJECT);
		FLEFRecordHelper.removeChildren(record, TAG_CANDIDATE);
		FLEFRecordHelper.removeChildren(record, TAG_COMMENT);

		candidateField.saveReferences(record);

		// Note: This will also save the subjectField (via bindingManager) and other common panels
		components.save(record);
	}


	public static void main(final String[] args){
		final FLEFRecord identityHypothesis = FLEFRecord.createMainRecord("IH1", "IDENTITY_HYPOTHESIS");
		identityHypothesis.addChild(FLEFRecord.createChildWithTag("SUBJECT")
			.addChild(FLEFRecord.createChildWithTagAndValue("INDIVIDUAL", "@I1@"))
		);
		identityHypothesis.addChild(FLEFRecord.createChildWithTag("CANDIDATE")
			.addChild(FLEFRecord.createChildWithTagAndValue("INDIVIDUAL", "@I2@"))
		);
		identityHypothesis.addChild(FLEFRecord.createChildWithTagAndValue("COMMENT", "Possibly the hypothesis is true"));
		identityHypothesis.addChild(FLEFRecord.createChildWithTag("SOURCE")
			.addChild(FLEFRecord.createChildWithTagAndValue("SOURCE", "@S1@"))
		);
		identityHypothesis.addChild(FLEFRecord.createChildWithTagAndValue("NOTE", "@N1@"));

		final FLEFRecord individual1 = FLEFRecord.createMainRecord("I1", "INDIVIDUAL");
		final FLEFRecord individual2 = FLEFRecord.createMainRecord("I2", "INDIVIDUAL");

		final FLEFRecord source1 = FLEFRecord.createMainRecord("S1", "SOURCE");
		source1.addChild(FLEFRecord.createChildWithTag("TITLE")
			.addChild(FLEFRecord.createChildWithTagAndValue("VALUE", "Hypothesis source"))
		);

		final FLEFRecord contextImpact1 = FLEFRecord.createMainRecord("CI1", "CONTEXT_IMPACT");
		contextImpact1.addChild(FLEFRecord.createChildWithTag("CONTEXT")
			.addChild(FLEFRecord.createChildWithTagAndValue("CULTURAL_NORM", "@CN1@"))
		);
		contextImpact1.addChild(FLEFRecord.createChildWithTag("TARGET")
			.addChild(FLEFRecord.createChildWithTagAndValue("IDENTITY_HYPOTHESIS", "@IH1@"))
		);

		final FLEFRecord conclusion1 = FLEFRecord.createMainRecord("CC1", "CONCLUSION");
		conclusion1.addChild(FLEFRecord.createChildWithTagAndValue("CONTEXT", "death cause"));
		conclusion1.addChild(FLEFRecord.createChildWithTag("RESOLVES")
			.addChild(FLEFRecord.createChildWithTagAndValue("IDENTITY_HYPOTHESIS", "@IH1@"))
		);

		final FLEFRecord researchQuestion1 = FLEFRecord.createMainRecord("RQ1", "RESEARCH_QUESTION");
		researchQuestion1.addChild(FLEFRecord.createChildWithTagAndValue("TITLE", "rq title"));
		researchQuestion1.addChild(FLEFRecord.createChildWithTagAndValue("QUESTION", "is?"));
		researchQuestion1.addChild(FLEFRecord.createChildWithTag("TARGET")
			.addChild(FLEFRecord.createChildWithTagAndValue("IDENTITY_HYPOTHESIS", "@IH1@"))
		);
		researchQuestion1.addChild(FLEFRecord.createChildWithTagAndValue("STATUS", "open"));

		final FLEFRecord note1 = FLEFRecord.createMainRecord("N1", "NOTE");
		note1.addChild(FLEFRecord.createChildWithTagAndValue("VALUE", "id hyp note"));

		final Consumer<FLEFModel> modelFiller = model -> {
			model.addRecord(identityHypothesis);
			model.addRecord(individual1);
			model.addRecord(individual2);
			model.addRecord(source1);
			model.addRecord(contextImpact1);
			model.addRecord(conclusion1);
			model.addRecord(researchQuestion1);
			model.addRecord(note1);
		};

		GUIHelper.launch(IdentityHypothesisRecordDialog::createEdit, modelFiller, identityHypothesis);
	}

}
