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
import io.github.mtrevisan.familylegacy.io.model.readers.AuditStamp;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.ui.components.lists.MediaPartListPanel;
import io.github.mtrevisan.familylegacy.ui.components.lists.PreferredImagePanel;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.PanelKey;
import io.github.mtrevisan.familylegacy.ui.dialogs.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PersonalNameHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchTaskHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Window;
import java.io.IOException;


/**
 * Dialog for editing an {@code INDIVIDUAL_RECORD} according to FLEF 0.2.0.
 * <p>
 * Structure:
 * <pre>
 * record IndividualRecord {
 *   name*: PersonalNameStructure
 *   source*: SourceCitation
 *   note*: Xref&lt;NoteRecord&gt;
 *   preferred_image?: struct {
 *     media: Xref&lt;MediaRecord&gt;
 *     crop?: CropRect
 *   }
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): name, preferred_image
 * Tab 2 (Attributes): IndividualAttributeRecord (individual = this individual)
 * <p>
 * The sex is not a field of the individual: it is edited as the {@code sex} IndividualAttributeRecords of the Attributes tab (see
 * {@link PreferredSex}).
 * <p>
 * Tab 3 (Relationships): RelationshipRecord (subject = this individual), RelationshipRecord (target = this individual)
 * Tab 4 (Participations): EventParticipationRecord (participant[individual] = this individual)
 * Tab 5 (Context): ContextImpactRecord (target[individual] = this individual)
 * Tab 6 (Research): ConclusionRecord (resolves = this individual), IdentityHypothesisRecord (identity = this individual), ResearchQuestionRecord (target[individual] = this individual)
 * Tab 7 (Sources): source
 * Tab 8 (Stories): note (kind story)
 * Tab 9 (Notes): note (the others)
 * Tab 10 (Privacy): privacy
 * Tab 11 (Audit): audit
 */
public class IndividualRecordDialog extends BaseRecordDialog{

	/** Whether the stories have a tab of their own (from the standard level), or are listed with the other notes. */
	private final boolean storiesApart;
	private final PreferredImagePanel preferredImagePanel;
	private final EntityListPanel personalNamePanel;
	/** The sex given by whoever creates the individual (the tree: the father is male): saved as a sex attribute. */
	private SexType presetSex;
	/** The only sex the individual may have (the one opposite to the other parent), with the message shown when another is entered. */
	private SexType requiredSex;
	private String requiredSexReason;


	private final MediaPartListPanel mediaCitationPanel;


	public static IndividualRecordDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, IndividualRecordDialog::new);
	}

	public static IndividualRecordDialog createEdit(final Window parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, IndividualRecordDialog::new);
	}


	private IndividualRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, IndividualHandler.getInstance());

		storiesApart = storiesApart();

		preferredImagePanel = new PreferredImagePanel(IndividualReader.TAG_PREFERRED_IMAGE, this, model);
		personalNamePanel = EntityListPanel.createForStructure(IndividualReader.TAG_NAME, this, I18N.t("dialog.individual.personal_name"), model, PersonalNameHandler.class);

		mediaCitationPanel = new MediaPartListPanel(MediaCitationReader.TAG, this, I18N.t("dialog.media_citation.panel"), model);
		//the crop of an image of the individual tags it in the image (a region, see the image viewer): the crop dialogs say it
		mediaCitationPanel.setCropIsRegion(true);
		preferredImagePanel.setCropIsRegion(true);
		components = new RecordDialogBuilder(this, model, this.record)
			.withComponent(PanelKey.INDIVIDUAL_ATTRIBUTE, IndividualAttributeHandler.TYPE, I18N.t("dialog.component.individual.attributes"))
			.withComponent(PanelKey.RELATIONSHIP_ON_SUBJECT, RelationshipHandler.TYPE, I18N.t("dialog.component.individual.relationships_on_target"))
			.withComponent(PanelKey.RELATIONSHIP_ON_OBJECT, RelationshipHandler.TYPE, I18N.t("dialog.component.relationships_on_subject"))
			.withComponent(PanelKey.EVENT_PARTICIPATION_ON_PARTICIPANT, EventParticipationHandler.TYPE, I18N.t("dialog.component.event.participations"))
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_TARGET, ContextImpactHandler.TYPE, I18N.t("dialog.component.context.impacts"))
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, ConclusionHandler.TYPE, I18N.t("dialog.component.conclusions"))
			.withComponent(PanelKey.IDENTITY_HYPOTHESIS_ON_IDENTITY, IdentityHypothesisHandler.TYPE, I18N.t("dialog.component.identity.hypotheses"))
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, ResearchQuestionHandler.TYPE, I18N.t("dialog.component.research.questions"))
			.withComponent(PanelKey.RESEARCH_TASK_ON_RELATED, ResearchTaskHandler.TYPE, I18N.t("dialog.component.research.tasks"))
			.withComponent(PanelKey.SOURCE, IndividualReader.TAG_SOURCE, I18N.t("dialog.component.sources_with_citations"))
			//the stories of the individual are apart from its notes
			.withComponent(PanelKey.storyPanel(storiesApart), IndividualReader.TAG_NOTE, null)
			.withComponent(PanelKey.notePanel(storiesApart), IndividualReader.TAG_NOTE, null)
			.withComponent(PanelKey.PRIVACY, IndividualReader.TAG_PRIVACY, null)
			.withComponent(PanelKey.AUDIT, IndividualReader.TAG_AUDIT, null)
			.build();


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
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]15[]15[]10[]");

		final JPanel conclusionPanel = components.getPanel(PanelKey.CONCLUSION_ON_RESOLVES);
		GUIHelper.addComponent(panel, conclusionPanel);

		final JPanel identityHypothesisPanel = components.getPanel(PanelKey.IDENTITY_HYPOTHESIS_ON_IDENTITY);
		GUIHelper.addComponent(panel, identityHypothesisPanel);

		final JPanel researchQuestionPanel = components.getPanel(PanelKey.RESEARCH_QUESTION_ON_TARGET);
		GUIHelper.addComponent(panel, researchQuestionPanel);

		// research tasks that name this record
		GUIHelper.addComponent(panel, components.getPanel(PanelKey.RESEARCH_TASK_ON_RELATED));

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

		//the media attached to the record
		GUIHelper.addComponent(panel, mediaCitationPanel);

		return panel;
	}

	@Override
	protected JPanel createStoriesPanel(){
		if(!storiesApart)
			return null;

		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		GUIHelper.addComponent(panel, components.getPanel(PanelKey.storyPanel(true)));

		return panel;
	}

	@Override
	protected JPanel createNotesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel notePanel = components.getPanel(PanelKey.notePanel(storiesApart));
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
	public void presetSex(final SexType sex){
		witSex(sex);
	}

	@Override
	public void requireSex(final SexType sex, final String reason){
		witSex(sex);
		requiredSex = sex;
		requiredSexReason = reason;
	}

	public IndividualRecordDialog witSex(final SexType sex){
		presetSex = sex;

		return this;
	}


	/** The sex is edited in the Attributes tab, as the sex attributes of the individual (see {@link PreferredSex}). */
	private void savePresetSex(){
		if(presetSex == null || presetSex == SexType.UNKNOWN || !PreferredSex.assertions(record.getId(), model).isEmpty())
			return;

		model.addRecord(FLEFRecord.createMainRecord(IndividualAttributeReader.TYPE, IndividualAttributeReader.ID_PREFIX, model)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, record.getId()))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_SEX))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, presetSex.getRawSex()))
			.addChild(AuditStamp.now()));
		presetSex = null;
	}


	@Override
	protected boolean validData(){
		if(requiredSex == null || requiredSex == SexType.UNKNOWN)
			return true;

		//the sex is edited in the Attributes tab: every assertion must say the required one (none at all is fine, the preset is saved)
		for(final FLEFRecord assertion : PreferredSex.assertions(record.getId(), model))
			if(SexType.fromRaw(FLEFRecordHelper.getChildValue(assertion, IndividualAttributeReader.TAG_VALUE)) != requiredSex){
				MessageDialogs.show(this, requiredSexReason, I18N.t("validation.title"), JOptionPane.ERROR_MESSAGE);

				return false;
			}
		return true;
	}


	@Override
	protected void loadData(){
		preferredImagePanel.load(record);
		personalNamePanel.load(record);

		components.load(record);
		mediaCitationPanel.load(record);


		// Initially, update carousel based on the first selected source (if any)
		updateCarouselFromSelectedSource();
	}

	@Override
	protected void saveData(){
		preferredImagePanel.save(record);
		personalNamePanel.save(record);
		savePresetSex();

		components.save(record);
		FLEFRecordHelper.removeChildren(record, MediaCitationReader.TAG);
		mediaCitationPanel.saveReferences(record);
	}


	static void main(final String[] args) throws IOException{
		GUIHelper.launch(IndividualRecordDialog::createEdit, "/tests/test.flef", "I1");
	}

}
