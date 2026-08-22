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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;


/* ONGOING */
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
		namePanel = EntityReferenceListPanel.createForStructure(TAG_NAME, this, "Names", model)
			.withHandlerTypes(ClassifiedNameHandler.class);
		typeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{
			StringUtils.EMPTY,
			"family", "household", "neighbourhood", "fraternity", "club", "literary_society",
			"association", "organisation", "tribe"
		});
		typeCombo.setEditable(true);

		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.GROUP_ATTRIBUTE, TAG_GROUP_ATTRIBUTE, "Group Attributes", GroupAttributeHandler.class, GroupHandler.class)
			.withComponent(PanelKey.RELATIONSHIP_ON_SUBJECT, TAG_RELATIONSHIP, "Members", RelationshipHandler.class, GroupHandler.class)
			.withComponent(PanelKey.RELATIONSHIP_ON_OBJECT, TAG_RELATIONSHIP, "Relationships", RelationshipHandler.class, GroupHandler.class)
			.withComponent(PanelKey.EVENT_PARTICIPATION_ON_PARTICIPANT, TAG_EVENT_PARTICIPATION, "Participations", EventParticipationHandler.class, GroupHandler.class)
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_TARGET, TAG_CONTEXT_IMPACT, "Context Impacts", ContextImpactHandler.class, GroupHandler.class)
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, TAG_CONCLUSION, "Conclusions", ConclusionHandler.class, GroupHandler.class)
			.withComponent(PanelKey.IDENTITY_HYPOTHESIS_ON_SUBJECT_OR_CANDIDATE, TAG_IDENTITY_HYPOTHESIS, "Identity Hypotheses", IdentityHypothesisHandler.class, GroupHandler.class)
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, TAG_RESEARCH_QUESTION, "Research Questions", ResearchQuestionHandler.class, GroupHandler.class)
			.withComponent(PanelKey.SOURCE, TAG_SOURCE, "Sources with Citations", SourceHandler.class, SourceCitationHandler.class)
			.withComponent(PanelKey.NOTE, TAG_NOTE, "Notes", NoteHandler.class, NoteHandler.class)
			.withComponent(PanelKey.PRIVACY, TAG_PRIVACY, null, null, null)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null, null, null)
			.build();

		components.bind(typeCombo);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]20[]10[]");

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
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]15[]");

		final JPanel memberPanel = components.getPanel(PanelKey.RELATIONSHIP_ON_SUBJECT);
		GUIHelper.addComponent(panel, memberPanel);

		final JPanel relationshipPanel = components.getPanel(PanelKey.RELATIONSHIP_ON_OBJECT);
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
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]15[]15[]");

		final JPanel conclusionPanel = components.getPanel(PanelKey.CONCLUSION_ON_RESOLVES);
		GUIHelper.addComponent(panel, conclusionPanel);

		final JPanel identityHypothesisPanel = components.getPanel(PanelKey.IDENTITY_HYPOTHESIS_ON_SUBJECT_OR_CANDIDATE);
		GUIHelper.addComponent(panel, identityHypothesisPanel);

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


	public static void main(final String[] args) throws IOException{
		try(final InputStream is = GroupRecordDialog.class.getResourceAsStream("/tests/test.flef")){
			final String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			GUIHelper.launch(GroupRecordDialog::createEdit, content, "G1");
		}
	}

}
