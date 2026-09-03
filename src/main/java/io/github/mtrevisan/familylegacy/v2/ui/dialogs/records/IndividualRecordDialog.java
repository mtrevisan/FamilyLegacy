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
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.PreferredImagePanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PersonalNameHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.IOException;
import java.io.Serial;


/*
TODO undo/redo a livello di record dopo che si è fatto salva di una dialog (chiedere, quindi ripristinare il record precedente)

public class RecordUpdateCommand extends AbstractUndoableEdit{
	private final FLEFModel model;
	private final FLEFRecord originalState = FLEFRecord.createEmpty();
	private final FLEFRecord newState = FLEFRecord.createEmpty();

	public RecordUpdateCommand(FLEFModel model, FLEFRecord originalState, FLEFRecord newState){
		this.model = model;

		originalState.deepCopyTo(this.originalState);
		newState.deepCopyTo(this.newState);
	}

	@Override
	public void undo() throws CannotUndoException{
		super.undo();

		model.addRecord(originalState);
	}

	@Override
	public void redo() throws CannotRedoException{
		super.redo();

		model.addRecord(newState);
	}

}

protected void onOk() {
  FLEFRecord copyBefore = record.clone();
  saveData(); // Salva i dati dal dialog al record corrente

  // Registra il comando nell'UndoManager globale
  UndoManager globalUndoManager = model.getUndoManager();
  globalUndoManager.addEdit(new RecordUpdateCommand(model, copyBefore, record));

  // Notifica il ridisegno globale dell'albero (Direct Pull)
  model.notifyDataChanged();

  dispose();
}
*/
/**
 * Dialog for editing an {@code INDIVIDUAL_RECORD} according to FLEF 0.1.2.
 * <p>
 * Structure:
 * <pre>
 * record IndividualRecord {
 *   id: LocalID
 *   name*: PersonalNameStructure
 *   sex?: enum { male, female, unknown }
 *   source*: SourceCitation
 *   note*: Xref&lt;NoteRecord&gt;
 *   preferred_image?: struct {
 *     uri: Uri
 *     crop?: CropRect
 *   }
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): name, sex, preferred_image
 * Tab 2 (Attributes): IndividualAttributeRecord (individual = this individual)
 * Tab 3 (Relationships): RelationshipRecord (subject = this individual), RelationshipRecord (target = this individual)
 * Tab 4 (Participations): EventParticipationRecord (participant[individual] = this individual)
 * Tab 5 (Context): ContextImpactRecord (target[individual] = this individual)
 * Tab 6 (Research): ConclusionRecord (resolves = this individual), IdentityHypothesisRecord (identity = this individual), ResearchQuestionRecord (target[individual] = this individual)
 * Tab 7 (Sources): source
 * Tab 8 (Notes): note
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class IndividualRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -4670126000119212974L;


	private static final String TAG_PREFERRED_IMAGE = "PREFERRED_IMAGE";
	private static final String TAG_PERSONAL_NAME = "NAME";
	private static final String TAG_SEX = "SEX";
	private static final String TAG_INDIVIDUAL_ATTRIBUTE = "INDIVIDUAL_ATTRIBUTE";
	private static final String TAG_RELATIONSHIP = "RELATIONSHIP";
	private static final String TAG_EVENT_PARTICIPATION = "EVENT_PARTICIPATION";
	private static final String TAG_CONTEXT_IMPACT = "CONTEXT_IMPACT";
	private static final String TAG_CONCLUSION = "CONCLUSION";
	private static final String TAG_IDENTITY_HYPOTHESIS = "IDENTITY_HYPOTHESIS";
	private static final String TAG_RESEARCH_QUESTION = "RESEARCH_QUESTION";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_PRIVACY = "PRIVACY";
	private static final String TAG_AUDIT = "AUDIT";


	private final RecordDialogComponents components;

	private final PreferredImagePanel preferredImagePanel;
	private final EntityListPanel personalNamePanel;
	private final BoundComboBox<String> sexCombo;


	public static IndividualRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, IndividualRecordDialog::new);
	}

	public static IndividualRecordDialog createEdit(final Dialog parent, final FLEFModel model,
		final FLEFRecord record){
		return createEdit(parent, model, record, IndividualRecordDialog::new);
	}


	private IndividualRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, IndividualHandler.class);

		preferredImagePanel = new PreferredImagePanel(TAG_PREFERRED_IMAGE, this);
		personalNamePanel = EntityListPanel.createForStructure(TAG_PERSONAL_NAME, this, "Personal Names*", model, PersonalNameHandler.class);
		sexCombo = new BoundComboBox<>(TAG_SEX, new String[]{
			StringUtils.EMPTY,
			"male", "female", "unknown"});

		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.INDIVIDUAL_ATTRIBUTE, TAG_INDIVIDUAL_ATTRIBUTE, "Individual Attributes", IndividualAttributeHandler.class, IndividualHandler.class)
			.withComponent(PanelKey.RELATIONSHIP_ON_SUBJECT, TAG_RELATIONSHIP, "Relationships", RelationshipHandler.class, IndividualHandler.class)
			.withComponent(PanelKey.RELATIONSHIP_ON_TARGET, TAG_RELATIONSHIP, "Members", RelationshipHandler.class, IndividualHandler.class)
			.withComponent(PanelKey.EVENT_PARTICIPATION_ON_PARTICIPANT, TAG_EVENT_PARTICIPATION, "Participations", EventParticipationHandler.class, IndividualHandler.class)
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_TARGET, TAG_CONTEXT_IMPACT, "Context Impacts", ContextImpactHandler.class, IndividualHandler.class)
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, TAG_CONCLUSION, "Conclusions", ConclusionHandler.class, IndividualHandler.class)
			.withComponent(PanelKey.IDENTITY_HYPOTHESIS_ON_IDENTITY, TAG_IDENTITY_HYPOTHESIS, "Identity Hypotheses", IdentityHypothesisHandler.class, IndividualHandler.class)
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, TAG_RESEARCH_QUESTION, "Research Questions", ResearchQuestionHandler.class, IndividualHandler.class)
			.withComponent(PanelKey.SOURCE, TAG_SOURCE, "Sources with Citations", SourceHandler.class, SourceCitationHandler.class)
			.withComponent(PanelKey.NOTE, TAG_NOTE, "Notes", NoteHandler.class, NoteHandler.class)
			.withComponent(PanelKey.PRIVACY, TAG_PRIVACY, null, null, null)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null, null, null)
			.build();

		components.bind(sexCombo);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]20[]10[]");

		// preferred image
		panel.add(preferredImagePanel, "span 2,growx,align center");

		// names
		GUIHelper.addComponent(panel, personalNamePanel);

		// sex
		final JPanel sexPanel = GUIHelper.createLabelFieldPanel(0, "[]15[]10[]");
		GUIHelper.addLabeledComponent(sexPanel, "Sex:", sexCombo);
		GUIHelper.addComponent(panel, sexPanel);

		return panel;
	}

	@Override
	protected JPanel createAttributesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel attributePanel = components.getPanel(PanelKey.INDIVIDUAL_ATTRIBUTE);
		GUIHelper.addComponent(panel, attributePanel);

		return panel;
	}

	@Override
	protected JPanel createRelationshipsPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]15[]");

		final JPanel relationshipAsSubjectPanel = components.getPanel(PanelKey.RELATIONSHIP_ON_SUBJECT);
		GUIHelper.addComponent(panel, relationshipAsSubjectPanel);

		final JPanel relationshipAsObjectPanel = components.getPanel(PanelKey.RELATIONSHIP_ON_TARGET);
		GUIHelper.addComponent(panel, relationshipAsObjectPanel);

		return panel;
	}

	@Override
	protected JPanel createParticipationsPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel eventParticipationPanel = components.getPanel(PanelKey.EVENT_PARTICIPATION_ON_PARTICIPANT);
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
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]15[]15[]");

		final JPanel conclusionPanel = components.getPanel(PanelKey.CONCLUSION_ON_RESOLVES);
		GUIHelper.addComponent(panel, conclusionPanel);

		final JPanel identityHypothesisPanel = components.getPanel(PanelKey.IDENTITY_HYPOTHESIS_ON_IDENTITY);
		GUIHelper.addComponent(panel, identityHypothesisPanel);

		final JPanel researchQuestionPanel = components.getPanel(PanelKey.RESEARCH_QUESTION_ON_TARGET);
		GUIHelper.addComponent(panel, researchQuestionPanel);

		return panel;
	}

	@Override
	protected JPanel createSourcesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]10[]10[]");

		final JPanel sourcePanel = components.getPanel(PanelKey.SOURCE);
		GUIHelper.addComponent(panel, sourcePanel);

		return panel;
	}

	@Override
	protected JPanel createNotesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]10[]10[]");

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
		preferredImagePanel.load(record);
		personalNamePanel.load(record);

		final EntityListPanel identityHypothesisPanel = (EntityListPanel)components.getPanel(PanelKey.IDENTITY_HYPOTHESIS_ON_IDENTITY);
		identityHypothesisPanel.withParentEntity(record);

		components.load(record);
	}

	@Override
	protected void saveData(){
		preferredImagePanel.save(record);
		personalNamePanel.save(record);

		components.save(record);
	}


	public static void main(final String[] args) throws IOException{
		GUIHelper.launch(IndividualRecordDialog::createEdit, "/tests/test.flef", "I1");
	}

}
