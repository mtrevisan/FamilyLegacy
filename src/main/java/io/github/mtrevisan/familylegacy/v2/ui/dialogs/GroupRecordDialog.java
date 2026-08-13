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
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.PreferredImagePanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityReferenceListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ClassifiedNameHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.Serial;
import java.util.function.Consumer;


/**
 * Dialog for editing a {@code GROUP_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record GroupRecord {
 *   id: LocalID
 *   name*: ClassifiedNameStructure
 *   type?: enum { family, household, neighborhood, fraternity, club, literary_society, association, organization, tribe } | Text
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
 * Tab 1 (Properties): name, type, preferred_image
 * Tab 2 (Attributes): GroupAttributeRecord (group = this group)
 * Tab 3 (Relationships): RelationshipRecord (subject = this group), RelationshipRecord (object = this group)
 * Tab 4 (Participations): EventParticipationRecord (participant.group = this group)
 * Tab 5 (Context): ContextImpactRecord (target.group = this group)
 * Tab 6 (Research): ConclusionRecord (resolves = this group), IdentityHypothesisRecord (subject/candidate = this group), ResearchQuestionRecord (target.group = this group)
 * Tab 7 (Sources): source
 * Tab 8 (Notes): note
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class GroupRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -4670126000119212972L;


	private static final String TAG_NAME = "NAME";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_PREFERRED_IMAGE = "PREFERRED_IMAGE";
	private static final String TAG_PRIVACY = "PRIVACY";
	private static final String TAG_CONTEXT_IMPACT = "CONTEXT_IMPACT";
	private static final String TAG_CONCLUSION = "CONCLUSION";
	private static final String TAG_GROUP_ATTRIBUTE = "GROUP_ATTRIBUTE";
	private static final String TAG_RELATIONSHIP = "RELATIONSHIP";
	private static final String TAG_EVENT_PARTICIPATION = "EVENT_PARTICIPATION";
	private static final String TAG_IDENTITY_HYPOTHESIS = "IDENTITY_HYPOTHESIS";
	private static final String TAG_RESEARCH_QUESTION = "RESEARCH_QUESTION";
	private static final String TAG_AUDIT = "AUDIT";


	private final RecordDialogComponents components;

	private final PreferredImagePanel preferredImagePanel;
	private final EntityReferenceListPanel namePanel;
	private final BoundComboBox<String> typeCombo;


	public static GroupRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, GroupRecordDialog::new);
	}

	public static GroupRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, GroupRecordDialog::new);
	}


	private GroupRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, GroupHandler.class);

		preferredImagePanel = new PreferredImagePanel(TAG_PREFERRED_IMAGE, this);
		namePanel = EntityReferenceListPanel.createForStructure(
			TAG_NAME, this, "Names", model, ClassifiedNameHandler.class);
		typeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{
			StringUtils.EMPTY,
			"family", "household", "neighbourhood", "fraternity", "club", "literary_society",
			"association", "organisation", "tribe"
		});
		typeCombo.setEditable(true);

		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.GROUP_ATTRIBUTE, TAG_GROUP_ATTRIBUTE, "Group Attributes", GroupAttributeHandler.class, GroupHandler.class)
			.withComponent(PanelKey.RELATIONSHIP_AS_SUBJECT, TAG_RELATIONSHIP, "Members", RelationshipHandler.class, GroupHandler.class)
			.withComponent(PanelKey.RELATIONSHIP_AS_OBJECT, TAG_RELATIONSHIP, "Relationships", RelationshipHandler.class, GroupHandler.class)
			.withComponent(PanelKey.EVENT_PARTICIPATION_ON_PARTICIPANT, TAG_EVENT_PARTICIPATION, "Participations", EventParticipationHandler.class, GroupHandler.class)
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_TARGET, TAG_CONTEXT_IMPACT, "Context Impacts", ContextImpactHandler.class, GroupHandler.class)
			.withComponent(PanelKey.CONCLUSION, TAG_CONCLUSION, "Conclusions", ConclusionHandler.class, GroupHandler.class)
			.withComponent(PanelKey.IDENTITY_HYPOTHESIS, TAG_IDENTITY_HYPOTHESIS, "Identity Hypotheses", IdentityHypothesisHandler.class, GroupHandler.class)
			.withComponent(PanelKey.RESEARCH_QUESTION, TAG_RESEARCH_QUESTION, "Research Questions", ResearchQuestionHandler.class, GroupHandler.class)
			.withComponent(PanelKey.SOURCE, TAG_SOURCE, "Sources", SourceHandler.class, SourceHandler.class)
			.withComponent(PanelKey.NOTE, TAG_NOTE, "Notes", NoteHandler.class, NoteHandler.class)
			.withComponent(PanelKey.PRIVACY, TAG_PRIVACY, null, null, null)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null, null, null)
			.build();

		components.bind(typeCombo);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]15[]10[]");

		// preferred image
		panel.add(preferredImagePanel, "span 2,growx,align center");

		// names
		GUIHelper.addComponent(panel, namePanel);

		// type
		final JPanel typePanel = GUIHelper.createLabelFieldPanel(0, "[]");
		GUIHelper.addLabeledComponent(typePanel, "Type:", typeCombo);
		GUIHelper.addComponent(panel, typePanel);

		return panel;
	}

	@Override
	protected JPanel createAttributesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel groupAttributePanel = components.getPanel(PanelKey.GROUP_ATTRIBUTE);
		GUIHelper.addComponent(panel, groupAttributePanel);

		return panel;
	}

	@Override
	protected JPanel createRelationshipsPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]");

		final JPanel memberPanel = components.getPanel(PanelKey.RELATIONSHIP_AS_SUBJECT);
		GUIHelper.addComponent(panel, memberPanel);

		final JPanel relationshipPanel = components.getPanel(PanelKey.RELATIONSHIP_AS_OBJECT);
		GUIHelper.addComponent(panel, relationshipPanel);

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
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]");

		final JPanel conclusionPanel = components.getPanel(PanelKey.CONCLUSION);
		GUIHelper.addComponent(panel, conclusionPanel);

		final JPanel identityHypothesisPanel = components.getPanel(PanelKey.IDENTITY_HYPOTHESIS);
		GUIHelper.addComponent(panel, identityHypothesisPanel);

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
		preferredImagePanel.load(record);
		namePanel.load(record);

		components.load(record);
	}

	@Override
	protected void saveData(){
		preferredImagePanel.save(record);
		namePanel.save(record);

		components.save(record);
	}


	public static void main(final String[] args){
		final FLEFRecord groupAttribute = FLEFRecord.createMainRecord("GA1", "GROUP_ATTRIBUTE");
		groupAttribute.addChild(FLEFRecord.createChildWithTagAndValue("GROUP", "@G1@"));
		groupAttribute.addChild(FLEFRecord.createChildWithTagAndValue("TYPE", "residence"));

		final FLEFRecord relationship1 = FLEFRecord.createMainRecord("RL1", "RELATIONSHIP")
			.addChild(FLEFRecord.createChildWithTagAndValue("TYPE", "associate"));
		relationship1.addChild(FLEFRecord.createChildWithTag("SUBJECT")
			.addChild(FLEFRecord.createChildWithTagAndValue("INDIVIDUAL", "@I1@"))
		);
		relationship1.addChild(FLEFRecord.createChildWithTag("OBJECT")
			.addChild(FLEFRecord.createChildWithTagAndValue("GROUP", "@G1@"))
		);
		final FLEFRecord relationship2 = FLEFRecord.createMainRecord("RL2", "RELATIONSHIP")
			.addChild(FLEFRecord.createChildWithTagAndValue("TYPE", "custom"));
		relationship2.addChild(FLEFRecord.createChildWithTag("SUBJECT")
			.addChild(FLEFRecord.createChildWithTagAndValue("GROUP", "@G1@"))
		);
		relationship2.addChild(FLEFRecord.createChildWithTag("OBJECT")
			.addChild(FLEFRecord.createChildWithTagAndValue("INDIVIDUAL", "@I1@"))
		);

		final FLEFRecord group1 = FLEFRecord.createMainRecord("G1", "GROUP");
		group1.addChild(FLEFRecord.createChildWithTagAndValue("TYPE", "household"));
		group1.addChild(FLEFRecord.createChildWithTagAndValue("NOTE", "@N1@"));
		final FLEFRecord group2 = FLEFRecord.createMainRecord("G2", "GROUP");

		final FLEFRecord note1 = FLEFRecord.createMainRecord("N1", "NOTE");
		note1.addChild(FLEFRecord.createChildWithTagAndValue("VALUE", "Group note"));

		final FLEFRecord source1 = FLEFRecord.createMainRecord("S1", "SOURCE");
		source1.addChild(FLEFRecord.createChildWithTag("TITLE")
			.addChild(FLEFRecord.createChildWithTagAndValue("VALUE", "Group source"))
		);

		final FLEFRecord individual = FLEFRecord.createMainRecord("I1", "INDIVIDUAL");
		individual.addChild(FLEFRecord.createChildWithTagAndValue("SEX", "male"));

		final FLEFRecord eventParticipation1 = FLEFRecord.createMainRecord("EP1", "EVENT_PARTICIPATION");
		eventParticipation1.addChild(FLEFRecord.createChildWithTag("PARTICIPANT")
			.addChild(FLEFRecord.createChildWithTagAndValue("GROUP", "@G1@"))
		);

		final FLEFRecord contextImpact1 = FLEFRecord.createMainRecord("CI1", "CONTEXT_IMPACT");
		contextImpact1.addChild(FLEFRecord.createChildWithTag("CONTEXT")
			.addChild(FLEFRecord.createChildWithTagAndValue("CULTURAL_NORM", "@CN1@"))
		);
		contextImpact1.addChild(FLEFRecord.createChildWithTag("TARGET")
			.addChild(FLEFRecord.createChildWithTagAndValue("GROUP", "@G1@"))
		);

		final FLEFRecord conclusion1 = FLEFRecord.createMainRecord("CC1", "CONCLUSION");
		conclusion1.addChild(FLEFRecord.createChildWithTagAndValue("CONTEXT", "death cause"));
		conclusion1.addChild(FLEFRecord.createChildWithTag("RESOLVES")
			.addChild(FLEFRecord.createChildWithTagAndValue("GROUP", "@G1@"))
		);

		final FLEFRecord identityHypothesis1 = FLEFRecord.createMainRecord("IH1", "IDENTITY_HYPOTHESIS");
		identityHypothesis1.addChild(FLEFRecord.createChildWithTag("SUBJECT")
			.addChild(FLEFRecord.createChildWithTagAndValue("GROUP", "@G1@"))
		);
		identityHypothesis1.addChild(FLEFRecord.createChildWithTag("CANDIDATE")
			.addChild(FLEFRecord.createChildWithTagAndValue("GROUP", "@G2@"))
		);
		final FLEFRecord identityHypothesis2 = FLEFRecord.createMainRecord("IH2", "IDENTITY_HYPOTHESIS");
		identityHypothesis2.addChild(FLEFRecord.createChildWithTag("SUBJECT")
			.addChild(FLEFRecord.createChildWithTagAndValue("GROUP", "@G3@"))
		);
		identityHypothesis2.addChild(FLEFRecord.createChildWithTag("CANDIDATE")
			.addChild(FLEFRecord.createChildWithTagAndValue("GROUP", "@G1@"))
		);

		final FLEFRecord researchQuestion1 = FLEFRecord.createMainRecord("RQ1", "RESEARCH_QUESTION");
		researchQuestion1.addChild(FLEFRecord.createChildWithTagAndValue("TITLE", "rq title"));
		researchQuestion1.addChild(FLEFRecord.createChildWithTagAndValue("QUESTION", "is?"));
		researchQuestion1.addChild(FLEFRecord.createChildWithTag("TARGET")
			.addChild(FLEFRecord.createChildWithTagAndValue("GROUP", "@G1@"))
		);
		researchQuestion1.addChild(FLEFRecord.createChildWithTagAndValue("STATUS", "open"));

		final Consumer<FLEFModel> modelFiller = model -> {
			model.addRecord(groupAttribute);
			model.addRecord(relationship1);
			model.addRecord(relationship2);
			model.addRecord(group1);
			model.addRecord(group2);
			model.addRecord(note1);
			model.addRecord(source1);
			model.addRecord(individual);
			model.addRecord(eventParticipation1);
			model.addRecord(contextImpact1);
			model.addRecord(conclusion1);
			model.addRecord(identityHypothesis1);
			model.addRecord(identityHypothesis2);
			model.addRecord(researchQuestion1);
		};
		GUIHelper.launch(GroupRecordDialog::createEdit, modelFiller, group1);
	}

}
