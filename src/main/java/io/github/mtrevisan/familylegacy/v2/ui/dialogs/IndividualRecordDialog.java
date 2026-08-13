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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.PreferredImagePanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityReferenceListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionTargetHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PersonalNameHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.function.Consumer;

/*
ONGOING
Tab 1: Properties
Tab 2: Attributes (IndividualAttributeRecord, GroupAttributeRecord)
Tab 3: Relationships
Tab 4: Participations (EventParticipationRecord)
Tab 5: Context (tutto ciò che proviene da ContextImpactRecord)
Tab 6: Research (ConclusionRecord, IdentityHypothesisRecord, ResearchQuestionRecord, ResearchActivityRecord, ResearchTaskRecord)

ordine generale:
1. Properties
2. Attributes
3. Relationships
4. Events
5. Context
6. Research
7. Sources
8. Notes
9. Administration


## IndividualRecord
Tab 1 (Properties): name, sex, preferred_image
Tab 2 (Attributes): IndividualAttributeRecord (individual = this individual)
Tab 3 (Relationships): RelationshipRecord (subject = this individual), RelationshipRecord (object = this individual)
Tab 4 (Participations): EventParticipationRecord (participant.individual = this individual)
Tab 5 (Context): ContextImpactRecord (target.individual = this individual)
Tab 6 (Research): ConclusionRecord (resolves/preferred = this individual), IdentityHypothesisRecord (subject/candidate = this individual), ResearchQuestionRecord (target.individual = this individual)
Tab 7 (Sources): source
Tab 8 (Notes): note


## GroupRecord
Tab 1 (Properties): name, type, preferred_image
Tab 2 (Attributes): GroupAttributeRecord (group = this group)
Tab 3 (Relationships): RelationshipRecord (subject = this group), RelationshipRecord (object = this group)
Tab 4 (Participations): EventParticipationRecord (participant.group = this group)
Tab 5 (Context): ContextImpactRecord (target.group = this group)
Tab 6 (Research): ConclusionRecord (resolves/preferred = this group), IdentityHypothesisRecord (subject/candidate = this group), ResearchQuestionRecord (target.group = this group)
Tab 7 (Sources): source
Tab 8 (Notes): note


## EventRecord
Tab 1 (Properties): type, description, date, place, agency, cause, evidence
Tab 2 (Attributes): -
Tab 3 (Relationships): -
Tab 4 (Participations): EventParticipationRecord (event = this event)
Tab 5 (Context): ContextImpactRecord (target.event = this event)
Tab 6 (Research): ConclusionRecord (resolves/preferred = this event), ResearchQuestionRecord (target.event = this event)
Tab 7 (Sources): source
Tab 8 (Notes): note


## IndividualAttributeRecord
Tab 1 (Properties): individual, type, value, valid_from, valid_to, place, evidence
Tab 2 (Attributes): -
Tab 3 (Relationships): -
Tab 4 (Participations): -
Tab 5 (Context): ContextImpactRecord (target.individual_attribute = this attribute)
Tab 6 (Research): ConclusionRecord (resolves/preferred = this attribute), ResearchQuestionRecord (target.individual_attribute = this attribute)
Tab 7 (Sources): source
Tab 8 (Notes): -


## GroupAttributeRecord
Tab 1 (Properties): group, type, value, valid_from, valid_to, place, evidence
Tab 2 (Attributes): -
Tab 3 (Relationships): -
Tab 4 (Participations): -
Tab 5 (Context): ContextImpactRecord (target.group_attribute = this attribute)
Tab 6 (Research): ConclusionRecord (resolves/preferred = this attribute), ResearchQuestionRecord (target.group_attribute = this attribute)
Tab 7 (Sources): source
Tab 8 (Notes): -


## RelationshipRecord
Tab 1 (Properties): subject, object, type, role, status, valid_from, valid_to, evidence
Tab 2 (Attributes): -
Tab 3 (Relationships): -
Tab 4 (Participations): -
Tab 5 (Context): ContextImpactRecord (target.relationship = this relationship)
Tab 6 (Research): ConclusionRecord (resolves/preferred = this relationship), ResearchQuestionRecord (target.relationship = this relationship)
Tab 7 (Sources): source
Tab 8 (Notes): note


## EventParticipationRecord
Tab 1 (Properties): event, participant, role, evidence
Tab 2 (Attributes): -
Tab 3 (Relationships): -
Tab 4 (Participations): -
Tab 5 (Context): ContextImpactRecord (target.event_participation = this participation)
Tab 6 (Research): ConclusionRecord (resolves/preferred = this participation), ResearchQuestionRecord (target.event_participation = this participation)
Tab 7 (Sources): source
Tab 8 (Notes): note


## PlaceRecord
Tab 1 (Properties): name, type, map, evidence
Tab 2 (Attributes): -
Tab 3 (Relationships): PlaceRelationshipRecord (subject = this place), PlaceRelationshipRecord (object = this place)
Tab 4 (Participations): EventParticipationRecord (participant.place = this place)
Tab 5 (Context): ContextImpactRecord (target.place = this place)
Tab 6 (Research): ConclusionRecord (resolves/preferred = this place), IdentityHypothesisRecord (subject/candidate = this place), ResearchQuestionRecord (target.place = this place)
Tab 7 (Sources): source
Tab 8 (Notes): -


## PlaceRelationshipRecord
Tab 1 (Properties): subject, object, type, valid_from, valid_to
Tab 2 (Attributes): -
Tab 3 (Relationships): -
Tab 4 (Participations): -
Tab 5 (Context): ContextImpactRecord (target.place_relationship = this relationship)
Tab 6 (Research): ConclusionRecord (resolves/preferred = this relationship), ResearchQuestionRecord (target.place_relationship = this relationship)
Tab 7 (Sources): source
Tab 8 (Notes): note


## SourceRecord
Tab 1 (Properties): title, author, publisher, date, place, media_type, repository, document
Tab 2 (Attributes): -
Tab 3 (Relationships): -
Tab 4 (Participations): -
Tab 5 (Context): -
Tab 6 (Research): ConclusionRecord (resolves/preferred = this source), ResearchQuestionRecord (target.source = this source), ResearchActivityRecord (source contains this source)
Tab 7 (Sources): -
Tab 8 (Notes): note


## DocumentRecord
Tab 1 (Properties): file, mapping, description
Tab 2 (Attributes): -
Tab 3 (Relationships): -
Tab 4 (Participations): -
Tab 5 (Context): -
Tab 6 (Research): ResearchQuestionRecord (target.document = this document)
Tab 7 (Sources): SourceRecord (document contains this document)
Tab 8 (Notes): note


## RepositoryRecord
Tab 1 (Properties): name, custodian, place, contact
Tab 2 (Attributes): -
Tab 3 (Relationships): -
Tab 4 (Participations): -
Tab 5 (Context): -
Tab 6 (Research): -
Tab 7 (Sources): SourceRecord (repository references this repository)
Tab 8 (Notes): note


## CulturalNormRecord
Tab 1 (Properties): title, rule_type, place, valid_from, valid_to, evidence
Tab 2 (Attributes): -
Tab 3 (Relationships): -
Tab 4 (Participations): -
Tab 5 (Context): ContextImpactRecord (context.cultural_norm = this norm)
Tab 6 (Research): ConclusionRecord (resolves/preferred = this norm), ResearchQuestionRecord (target.cultural_norm = this norm)
Tab 7 (Sources): source
Tab 8 (Notes): note


## HistoricEventRecord
Tab 1 (Properties): type, title, date, place, evidence
Tab 2 (Attributes): -
Tab 3 (Relationships): -
Tab 4 (Participations): -
Tab 5 (Context): ContextImpactRecord (context.historic_event = this historic event)
Tab 6 (Research): ConclusionRecord (resolves/preferred = this historic event), ResearchQuestionRecord (target.historic_event = this historic event)
Tab 7 (Sources): source
Tab 8 (Notes): note


## ContextImpactRecord
Tab 1 (Properties): context, target, impact_type, significance, evidence
Tab 2 (Attributes): -
Tab 3 (Relationships): -
Tab 4 (Participations): -
Tab 5 (Context): -
Tab 6 (Research): -
Tab 7 (Sources): source
Tab 8 (Notes): -


## IdentityHypothesisRecord
Tab 1 (Properties): subject, candidate, comment, evidence
Tab 2 (Attributes): -
Tab 3 (Relationships): -
Tab 4 (Participations): -
Tab 5 (Context): ContextImpactRecord (target.identity_hypothesis = this hypothesis)
Tab 6 (Research): ConclusionRecord (resolves/preferred = this hypothesis), ResearchQuestionRecord (target.identity_hypothesis = this hypothesis)
Tab 7 (Sources): source
Tab 8 (Notes): -


## ResearchQuestionRecord
Tab 1 (Properties): title, question, target, status, conclusion, conclusion_confidence, rationale, created, closed
Tab 2 (Attributes): -
Tab 3 (Relationships): -
Tab 4 (Participations): -
Tab 5 (Context): -
Tab 6 (Research): ResearchActivityRecord (question contains this question), ResearchTaskRecord (question contains this question), ConclusionRecord (research contains this question)
Tab 7 (Sources): -
Tab 8 (Notes): -


## ResearchActivityRecord
Tab 1 (Properties): question, date, activity_type, status, action, target, search_scope, result, observation, conclusion, conclusion_confidence, source, parent, task
Tab 2 (Attributes): -
Tab 3 (Relationships): -
Tab 4 (Participations): -
Tab 5 (Context): -
Tab 6 (Research): -
Tab 7 (Sources): source
Tab 8 (Notes): -


## ResearchTaskRecord
Tab 1 (Properties): description, question, created_by, status, priority, due_date, outcome
Tab 2 (Attributes): -
Tab 3 (Relationships): -
Tab 4 (Participations): -
Tab 5 (Context): -
Tab 6 (Research): -
Tab 7 (Sources): -
Tab 8 (Notes): -
*/

/* DONE */
/**
 * Dialog for editing an {@code INDIVIDUAL_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record IndividualRecord {
 *   id: LocalID
 *   name*: PersonalNameStructure
 *   sex?: enum { male, female, unknown }
 *   note*: Xref&lt;NoteRecord&gt;
 *   source*: SourceCitation
 *   preferred_image?: struct {
 *     uri: Uri
 *     crop?: CropRect
 *   }
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 */
public class IndividualRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -4670126000119212974L;


	private static final String TAG_PERSONAL_NAME = "NAME";
	private static final String TAG_SEX = "SEX";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_PREFERRED_IMAGE = "PREFERRED_IMAGE";
	private static final String TAG_RESTRICTION = "RESTRICTION";

	private static final String TAG_CULTURAL_NORM = "CULTURAL_NORM";
	private static final String TAG_CONCLUSION = "CONCLUSION";
	private static final String TAG_INDIVIDUAL_ATTRIBUTE = "INDIVIDUAL_ATTRIBUTE";
	private static final String TAG_RELATIONSHIP = "RELATIONSHIP";


	static{
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new IndividualAttributeHandler());
		HandlerRegistry.register(new RelationshipHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new PersonalNameHandler());
		HandlerRegistry.register(new ConclusionHandler());
		HandlerRegistry.register(new ConclusionTargetHandler());
		HandlerRegistry.register(new CulturalNormHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final EntityReferenceListPanel personalNamePanel;
	private final BoundComboBox<String> sexCombo;
	private final EntityReferenceListPanel notePanel;
	private final EntityCitationListPanel sourcePanel;
	private final PreferredImagePanel preferredImagePanel;
	private final RestrictionPanel privacyPanel;
	private final ModificationPanel auditPanel;

	// Other
	private final EntityReferenceListPanel culturalNormPanel;
	private final EntityReferenceListPanel conclusionPanel;
	private final EntityReferenceListPanel memberPanel;
	private final EntityReferenceListPanel attributePanel;
	private final EntityReferenceListPanel relationshipPanel;


	public static IndividualRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, IndividualRecordDialog::new);
	}

	public static IndividualRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, IndividualRecordDialog::new);
	}


	private IndividualRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(IndividualHandler.TYPE));

		personalNamePanel = EntityReferenceListPanel.createForStructure(TAG_PERSONAL_NAME, this, "Personal Names*", model, PersonalNameHandler.TYPE);
		sexCombo = new BoundComboBox<>(TAG_SEX, new String[]{
			StringUtils.EMPTY,
			"male", "female", "unknown"});
		notePanel = EntityReferenceListPanel.createForRecord(TAG_NOTE, this, "Notes", model, NoteHandler.TYPE)
			.withParentEntity(this.record.getId(), IndividualHandler.TYPE);
		sourcePanel = new EntityCitationListPanel(TAG_SOURCE, this, "Sources", model, SourceHandler.TYPE);
		preferredImagePanel = new PreferredImagePanel(TAG_PREFERRED_IMAGE, this);
		privacyPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		auditPanel = new ModificationPanel(this);

		culturalNormPanel = EntityReferenceListPanel.createForRecord(TAG_CULTURAL_NORM, this, "Cultural Norms", model, CulturalNormHandler.TYPE)
			.withParentEntity(this.record.getId(), IndividualHandler.TYPE);
		conclusionPanel = EntityReferenceListPanel.createForRecord(TAG_CONCLUSION, this, "Conclusions", model, ConclusionTargetHandler.TYPE)
			.withParentEntity(this.record.getId(), IndividualHandler.TYPE);
		memberPanel = EntityReferenceListPanel.createForRecord(TAG_RELATIONSHIP, this, "Members", model, RelationshipHandler.TYPE)
			.withParentEntity(this.record.getId(), IndividualHandler.TYPE);
		attributePanel = EntityReferenceListPanel.createForRecord(TAG_INDIVIDUAL_ATTRIBUTE, this, "Individual Attributes", model, IndividualAttributeHandler.TYPE)
			.withParentEntity(this.record.getId(), IndividualHandler.TYPE);
		relationshipPanel = EntityReferenceListPanel.createForRecord(TAG_RELATIONSHIP, this, "Relationships", model, RelationshipHandler.TYPE)
			.withParentEntity(this.record.getId(), IndividualHandler.TYPE);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(sexCombo);


		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Privacy", privacyPanel);
		tabbedPane.addTab("Audit", auditPanel);

		finalizeLayout(tabbedPane);
	}

	private JPanel createMainPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,wrap 1", "[grow]", "[]15[]10[]10[]"));

		panel.add(preferredImagePanel, "growx,align center");

		// names
		panel.add(personalNamePanel, "growx");

		// Sex – now using the bound combo box
		final JPanel sexPanel = new JPanel(new MigLayout("ins 0,fillx", "[right]rel[grow]"));
		sexPanel.add(new JLabel("Sex:"), "align label");
		sexPanel.add(sexCombo, "growx");
		panel.add(sexPanel, "growx");

		// members
		panel.add(memberPanel, "growx");

		// attributes
		panel.add(attributePanel, "growx");

		return panel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]10[]10[]10[]10[]"));
		panel.add(culturalNormPanel, "growx");
		panel.add(conclusionPanel, "growx");
		panel.add(relationshipPanel, "growx");
		panel.add(notePanel, "growx");
		panel.add(sourcePanel, "growx");
		return panel;
	}


	@Override
	protected void loadData(){
		bindingManager.load(record);

		personalNamePanel.load(record);
		culturalNormPanel.load(record);
		notePanel.load(record);
		sourcePanel.load(record);
		preferredImagePanel.load(record);
		privacyPanel.load(record);
		auditPanel.load(record);

		conclusionPanel.loadReference(record.getId());
		memberPanel.loadReference(record.getId());
		attributePanel.loadReference(record.getId());
		relationshipPanel.loadReference(record.getId());
	}

	@Override
	protected boolean validData(){
		return true;
	}

	@Override
	protected void saveData(){
		bindingManager.save(record);

		personalNamePanel.save(record);
		culturalNormPanel.save(record);
		notePanel.save(record);
		sourcePanel.save(record);
		preferredImagePanel.save(record);
		privacyPanel.save(record);
		auditPanel.save(record);

		conclusionPanel.save(record);
		memberPanel.save(record);
		attributePanel.save(record);
		relationshipPanel.save(record);
	}


	public static void main(final String[] args){
//		GUIHelper.launch(IndividualRecordDialog::createNew, modelFiller);

		final FLEFRecord individualAttribute = FLEFRecord.createMainRecord("IA1", "INDIVIDUAL_ATTRIBUTE");
		individualAttribute.addChild(FLEFRecord.createChildWithTagAndValue("INDIVIDUAL", "@I1@"));
		individualAttribute.addChild(FLEFRecord.createChildWithTagAndValue("TYPE", "residence"));
		final FLEFRecord individual = FLEFRecord.createMainRecord("I1", "INDIVIDUAL");
		final FLEFRecord relationship = FLEFRecord.createMainRecord("RL1", "RELATIONSHIP")
			.addChild(FLEFRecord.createChildWithTagAndValue("TYPE", "group_member"));
		relationship.addChild(FLEFRecord.createChildWithTag("SUBJECT")
			.addChild(FLEFRecord.createChildWithTagAndValue("INDIVIDUAL", "@I1@"))
		);
		relationship.addChild(FLEFRecord.createChildWithTag("OBJECT")
			.addChild(FLEFRecord.createChildWithTagAndValue("GROUP", "@G1@"))
		);
		final FLEFRecord group = FLEFRecord.createMainRecord("G1", "GROUP");

		final Consumer<FLEFModel> modelFiller = model -> {
			model.addRecord(individualAttribute);
			model.addRecord(individual);
			model.addRecord(relationship);
			model.addRecord(group);
		};
		GUIHelper.launch(IndividualRecordDialog::createEdit, modelFiller, individual);
	}

}
