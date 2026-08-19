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
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.ParticipantField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.Serial;
import java.util.function.Consumer;


/**
 * Dialog for editing an {@code EVENT_PARTICIPATION_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record EventParticipationRecord {
 *   id: LocalID
 *   participant: EntityParticipant
 *   event: Xref&lt;EventRecord&gt;
 *   role?: enum {
 *     child, parent, spouse, power_of_attorney, prisoner, witness, officiant, informant, executor, grantor, grantee,
 *     landlord, tenant, soldier, commander, victim, survivor, accused, judge
 *   } | Text
 *   source*: SourceCitation
 *   note*: Xref&lt;NoteRecord&gt;
 *   evidence?: EvidenceQualifiers
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 *
 * EntityParticipant = oneof {
 *   individual: Xref&lt;IndividualRecord&gt;
 *   group: Xref&lt;GroupRecord&gt;
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): event, participant, role, evidence
 * Tab 5 (Context): ContextImpactRecord (target.event_participation = this participation)
 * Tab 6 (Research): ConclusionRecord (resolves/preferred = this participation), ResearchQuestionRecord (target.event_participation = this participation)
 * Tab 7 (Sources): source
 * Tab 8 (Notes): note
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class EventParticipationRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 3519955064561398245L;


	private static final String TAG_EVENT = "EVENT";
	private static final String TAG_PARTICIPANT = "PARTICIPANT";
	private static final String TAG_ROLE = "ROLE";
	private static final String TAG_CONTEXT_IMPACT = "CONTEXT_IMPACT";
	private static final String TAG_CONCLUSION = "CONCLUSION";
	private static final String TAG_RESEARCH_QUESTION = "RESEARCH_QUESTION";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_EVIDENCE = "EVIDENCE";
	private static final String TAG_PRIVACY = "PRIVACY";
	private static final String TAG_AUDIT = "AUDIT";


	private final RecordDialogComponents components;

	private final JPanel propertiesPanel;

	private final BindingManager bindingManager = new BindingManager();

	private final ParticipantField participantField;
	private final ParticipantField eventField;
	private final BoundComboBox<String> roleCombo;


	public static EventParticipationRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, EventParticipationRecordDialog::new);
	}

	public static EventParticipationRecordDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, EventParticipationRecordDialog::new);
	}


	private EventParticipationRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, EventParticipationHandler.class);

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]5[]");

		participantField = ParticipantField.create(TAG_PARTICIPANT, this, model);
		participantField.setHandlerTypes(IndividualHandler.class, GroupHandler.class, PlaceHandler.class);
		eventField = ParticipantField.create(TAG_EVENT, this, model, EventHandler.class);
		roleCombo = new BoundComboBox<>(TAG_ROLE, new String[]{
			StringUtils.EMPTY,
			"child", "parent", "spouse", "power_of_attorney", "prisoner", "witness",
			"officiant", "informant", "executor", "grantor", "grantee",
			"landlord", "tenant", "soldier", "commander", "victim", "survivor",
			"accused", "judge"
		});
		roleCombo.setEditable(true);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_TARGET, TAG_CONTEXT_IMPACT, "Context Impacts", ContextImpactHandler.class, EventParticipationHandler.class)
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, TAG_CONCLUSION, "Conclusions", ConclusionHandler.class, EventParticipationHandler.class)
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, TAG_RESEARCH_QUESTION, "Research Questions", ResearchQuestionHandler.class, EventParticipationHandler.class)
			.withCitationComponent(PanelKey.SOURCE, TAG_SOURCE, TAG_SOURCE, "Sources", SourceHandler.class, SourceHandler.class)
			.withComponent(PanelKey.NOTE, TAG_NOTE, "Notes", NoteHandler.class, NoteHandler.class)
			.withComponent(PanelKey.EVIDENCE, TAG_EVIDENCE, "Evidence", null, EventParticipationHandler.class)
			.withComponent(PanelKey.PRIVACY, TAG_PRIVACY, null, null, null)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null, null, null)
			.build();

		components.bind(roleCombo);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// participant
		GUIHelper.addLabeledComponent(propertiesPanel, "Participant*:", participantField);

		// event
		GUIHelper.addLabeledComponent(propertiesPanel, "Event*:", eventField);

		// role
		GUIHelper.addLabeledComponent(propertiesPanel, "Role:", roleCombo);

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

		// conclusion
		final JPanel conclusionPanel = components.getPanel(PanelKey.CONCLUSION_ON_RESOLVES);
		GUIHelper.addComponent(panel, conclusionPanel);

		// research question
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


	public EventParticipationRecordDialog withEvent(final String eventId){
		if(!confirmRecordExistsForType(eventId, EventHandler.class))
			return this;

		participantField.setParticipant(null);
		GUIHelper.setComponentVisible(participantField, true);

		eventField.setParticipant(FLEFRecord.createMainRecord(eventId, HandlerRegistry.getHandlerType(EventHandler.class)));
		GUIHelper.setComponentVisible(eventField, false);

		return this;
	}

	public EventParticipationRecordDialog withParticipant(final String participantId,
			final Class<? extends RecordTypeHandler<?>> participantHandlerClass){
		if(!confirmRecordExistsForType(participantId, participantHandlerClass))
			return this;

		eventField.setParticipant(FLEFRecord.createEmpty());
		GUIHelper.setComponentVisible(eventField, true);

		participantField.setParticipant(FLEFRecord.createMainRecord(participantId, HandlerRegistry.getHandlerType(participantHandlerClass)));
		GUIHelper.setComponentVisible(participantField, false);

		return this;
	}


	@Override
	protected void loadData(){
		participantField.load(record);
		eventField.load(record);

		components.load(record);
	}

	@Override
	protected boolean validData(){
		if(!participantField.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Participant is required.",
				tabbedPane, propertiesPanel, participantField);

			return false;
		}

		if(!eventField.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Event is required.",
				tabbedPane, propertiesPanel, eventField);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		participantField.saveReferences(record);
		eventField.saveReferences(record);

		components.save(record);
	}


	public static void main(final String[] args){
		final FLEFRecord eventParticipation = FLEFRecord.createMainRecord("EP1", "EVENT_PARTICIPATION");
		eventParticipation.addChild(FLEFRecord.createChildWithTagAndValue("EVENT", "@E1@"));
		eventParticipation.addChild(FLEFRecord.createChildWithTagAndValue("ROLE", "child"));
		eventParticipation.addChild(FLEFRecord.createChildWithTag("PARTICIPANT")
			.addChild(FLEFRecord.createChildWithTagAndValue("INDIVIDUAL", "@I1@"))
		);

		final FLEFRecord event1 = FLEFRecord.createMainRecord("E1", "EVENT");
		event1.addChild(FLEFRecord.createChildWithTagAndValue("TYPE", "war"));

		final FLEFRecord individual1 = FLEFRecord.createMainRecord("I1", "INDIVIDUAL");


		final Consumer<FLEFModel> modelFiller = model -> {
			model.addRecord(eventParticipation);
			model.addRecord(event1);
			model.addRecord(individual1);
		};
		GUIHelper.launch(EventParticipationRecordDialog::createEdit, modelFiller, eventParticipation);
	}

}
