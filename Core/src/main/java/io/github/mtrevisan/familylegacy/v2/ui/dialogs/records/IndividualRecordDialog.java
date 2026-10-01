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
import io.github.mtrevisan.familylegacy.v2.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.PreferredImagePanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PersonalNameHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;

import javax.swing.JPanel;
import java.awt.Window;
import java.io.IOException;


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

protected void onOk(){
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
 * Dialog for editing an {@code INDIVIDUAL_RECORD} according to FLEF 0.1.3.
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

	private final PreferredImagePanel preferredImagePanel;
	private final EntityListPanel personalNamePanel;
	private final BoundComboBox<String> sexCombo;


	public static IndividualRecordDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, IndividualRecordDialog::new);
	}

	public static IndividualRecordDialog createEdit(final Window parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, IndividualRecordDialog::new);
	}


	private IndividualRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, IndividualHandler.getInstance());

		preferredImagePanel = new PreferredImagePanel(IndividualReader.TAG_PREFERRED_IMAGE, this);
		personalNamePanel = EntityListPanel.createForStructure(IndividualReader.TAG_NAME, this, I18N.t("dialog.individual.personal.name") + "*", model, PersonalNameHandler.class);
		sexCombo = new BoundComboBox<>(IndividualReader.TAG_SEX, GUIHelper.fillCombo(IndividualReader.SEXES, null));
		sexCombo.setI18NPrefix("enum.individual.sex");

		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.INDIVIDUAL_ATTRIBUTE, IndividualAttributeHandler.TYPE, I18N.t("dialog.component.individual.attributes"))
			.withComponent(PanelKey.RELATIONSHIP_ON_SUBJECT, RelationshipHandler.TYPE, I18N.t("dialog.component.relationship.on.target"))
			.withComponent(PanelKey.RELATIONSHIP_ON_OBJECT, RelationshipHandler.TYPE, I18N.t("dialog.component.relationship.on.subject"))
			.withComponent(PanelKey.EVENT_PARTICIPATION_ON_PARTICIPANT, EventParticipationHandler.TYPE, I18N.t("dialog.component.event.participations"))
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_TARGET, ContextImpactHandler.TYPE, I18N.t("dialog.component.context.impact"))
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, ConclusionHandler.TYPE, I18N.t("dialog.component.conclusions"))
			.withComponent(PanelKey.IDENTITY_HYPOTHESIS_ON_IDENTITY, IdentityHypothesisHandler.TYPE, I18N.t("dialog.component.identity.hypotheses"))
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, ResearchQuestionHandler.TYPE, I18N.t("dialog.component.research.questions"))
			.withComponent(PanelKey.SOURCE, IndividualReader.TAG_SOURCE, I18N.t("dialog.component.sources.with.citations"))
			.withComponent(PanelKey.NOTE, IndividualReader.TAG_NOTE, null)
			.withComponent(PanelKey.PRIVACY, IndividualReader.TAG_PRIVACY, null)
			.withComponent(PanelKey.AUDIT, IndividualReader.TAG_AUDIT, null)
			.build();

		components.bind(sexCombo);


		// Set up the image carousel selection listener on the source list
		setupSourceListSelection();

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
		GUIHelper.addLabeledComponent(sexPanel, I18N.t("dialog.individual.sex") + ":", sexCombo);
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

		// Relationships in which this individual is the subject (Parents, Guardians, Groups)
		final JPanel relationshipAsSubjectPanel = components.getPanel(PanelKey.RELATIONSHIP_ON_SUBJECT);
		GUIHelper.addComponent(panel, relationshipAsSubjectPanel);

		// Relationships in which this individual is the target (biological/adopted children, dependents)
		final JPanel relationshipAsTargetPanel = components.getPanel(PanelKey.RELATIONSHIP_ON_OBJECT);
		GUIHelper.addComponent(panel, relationshipAsTargetPanel);

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
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]15[]");

		final JPanel sourcePanel = components.getPanel(PanelKey.SOURCE);
		GUIHelper.addComponent(panel, sourcePanel);

		// Image carousel below the source list
		// It will be hidden if no images are found
		GUIHelper.addComponent(panel, imageCarouselPanel);

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


	public IndividualRecordDialog witSex(final SexType sex){
		if(sex != null){
			sexCombo.setText(sex.getRawSex());
			sexCombo.setEnabled(false);
		}

		return this;
	}


	@Override
	protected void loadData(){
		preferredImagePanel.load(record);
		personalNamePanel.load(record);

		components.load(record);


		// Initially, update carousel based on the first selected source (if any)
		updateCarouselFromSelectedSource();
	}

	@Override
	protected void saveData(){
		preferredImagePanel.save(record);
		personalNamePanel.save(record);

		components.save(record);
//		try{
//			FLEFWriter.createCompact().write(model,
//				new File("C://Users/mauro/IdeaProjects/FamilyLegacy/src/main/resources/tests/out.flef").toPath(),
//				false);
//		}
//		catch(IOException e){
//			throw new RuntimeException(e);
//		}
	}


	public static void main(final String[] args) throws IOException{
		GUIHelper.launch(IndividualRecordDialog::createEdit, "/tests/test.flef", "I1");
	}

}
