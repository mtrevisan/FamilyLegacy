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
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.IOException;
import java.io.Serial;
import java.util.Objects;


/**
 * Dialog for editing an {@code IDENTITY_HYPOTHESIS_RECORD} according to FLEF 0.1.2.
 * <p>
 * Structure:
 * <pre>
 * record IdentityHypothesisRecord {
 *   id: LocalID
 *   identity+: IdentityCandidate
 *   comment?: Text
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 *   audit: AuditStructure
 *
 *   require count(identity) == 2
 *   require identity[0] != identity[1]
 *   require type(identity[0]) == type(identity[1])
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
 * Tab 1 (Properties): identity 1, identity 2, comment, evidence
 * Tab 5 (Context): ContextImpactRecord (target[identity_hypothesis] = this hypothesis)
 * Tab 6 (Research): ConclusionRecord (resolves = this hypothesis), ResearchQuestionRecord (target[identity_hypothesis] = this hypothesis)
 * Tab 7 (Sources): source
 * Tab 10 (Audit): audit
 */
public class IdentityHypothesisRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -3743748718107492890L;


	private static final String TAG_IDENTITY = "IDENTITY";
	private static final String TAG_COMMENT = "COMMENT";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_EVIDENCE = "EVIDENCE";
	private static final String TAG_CONTEXT_IMPACT = "CONTEXT_IMPACT";
	private static final String TAG_CONCLUSION = "CONCLUSION";
	private static final String TAG_RESEARCH_QUESTION = "RESEARCH_QUESTION";
	private static final String TAG_AUDIT = "AUDIT";


	private final RecordDialogComponents components;

	private final JPanel propertiesPanel;

	private final EntityField identity1Field;
	private final EntityField identity2Field;
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

		identity1Field = EntityField.createForRecordFromOneofReference(TAG_IDENTITY, this, model)
			.withHandlerTypes(IndividualHandler.class, GroupHandler.class, PlaceHandler.class);
		identity2Field = EntityField.createForRecordFromOneofReference(TAG_IDENTITY, this, model)
			.withHandlerTypes(IndividualHandler.class, GroupHandler.class, PlaceHandler.class);
		commentArea = new BoundTextArea(TAG_COMMENT, 3, 30);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_TARGET, TAG_CONTEXT_IMPACT, "Context Impacts", ContextImpactHandler.class, IdentityHypothesisHandler.class)
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, TAG_CONCLUSION, "Conclusions", ConclusionHandler.class, IdentityHypothesisHandler.class)
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, TAG_RESEARCH_QUESTION, "Research Questions", ResearchQuestionHandler.class, IdentityHypothesisHandler.class)
			.withComponent(PanelKey.SOURCE, TAG_SOURCE, "Sources with Citations", SourceHandler.class, SourceCitationHandler.class)
			.withComponent(PanelKey.NOTE, TAG_NOTE, "Notes", NoteHandler.class, NoteHandler.class)
			.withComponent(PanelKey.EVIDENCE, TAG_EVIDENCE, "Evidence", null, IdentityHypothesisHandler.class)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null, null, null)
			.build();

		components.bind(commentArea);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// identity 1
		GUIHelper.addLabeledComponent(propertiesPanel, "Identity 1*:", identity1Field);

		// identity 2
		GUIHelper.addLabeledComponent(propertiesPanel, "Identity 2*:", identity2Field);

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
	protected JPanel createAuditPanel(){
		return components.getPanel(PanelKey.AUDIT);
	}


	@Override
	protected void loadData(){
		identity1Field.load(record, 0);
		identity2Field.load(record, 1);

		components.load(record);
	}

	@Override
	protected boolean validData(){
		if(!identity1Field.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"First identity is required.",
				tabbedPane, propertiesPanel, identity1Field);

			return false;
		}

		if(!identity2Field.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Second identity is required.",
				tabbedPane, propertiesPanel, identity2Field);

			return false;
		}

		// identities must be different records
		final FLEFRecord identity1 = identity1Field.getEntity();
		final String identity1Id = (identity1 != null? identity1.getId(): null);
		final FLEFRecord identity2 = identity2Field.getEntity();
		final String identity2Id = (identity2 != null? identity2.getId(): null);
		if(Objects.equals(identity1Id, identity2Id)){
			GUIHelper.showValidationErrorAndFocus(this,
				"Identities must be different records.",
				tabbedPane, propertiesPanel, identity1Field);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		identity1Field.saveReferences(record);
		identity2Field.saveReferences(record);

		components.save(record);
	}


	public static void main(final String[] args) throws IOException{
		GUIHelper.launch(IdentityHypothesisRecordDialog::createEdit, "/tests/test.flef", "IH1");
	}

}
