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
package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.DirectRelationshipCellRenderer;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.InverseRelationshipCellRenderer;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchTaskHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;

import javax.swing.JPanel;
import java.util.Objects;


/**
 * Enumeration representing distinct keys for dialog panels, encapsulating their creation logic.
 */
public enum PanelKey{

	// IndividualAttributeRecord (individual = this individual)
	INDIVIDUAL_ATTRIBUTE((owner, cfg, model, record) ->
		createOneOfReferencePanel(owner, cfg, model, null, record, IndividualAttributeHandler.class)),

	// GroupAttributeRecord (group = this group)
	GROUP_ATTRIBUTE((owner, cfg, model, record) ->
		createOneOfReferencePanel(owner, cfg, model, null, record, GroupAttributeHandler.class)),

	// RelationshipRecord (subject = this individual)
	// RelationshipRecord (subject = this group)
	RELATIONSHIP_ON_SUBJECT((owner, cfg, model, record) ->
		createOneOfReferencePanel(owner, cfg, model, EntityListPanel.ActorType.SUBJECT, record, RelationshipHandler.class)
			.withCellRenderer(new DirectRelationshipCellRenderer(model))),

	// RelationshipRecord (target = this individual)
	// RelationshipRecord (target = this group)
	RELATIONSHIP_ON_TARGET((owner, cfg, model, record) ->
		createOneOfReferencePanel(owner, cfg, model, EntityListPanel.ActorType.OBJECT, record, RelationshipHandler.class)
			.withCellRenderer(new InverseRelationshipCellRenderer(model))),

	// PlaceRelationshipRecord (subject = this place)
	PLACE_RELATIONSHIP_ON_SUBJECT((owner, cfg, model, record) ->
		createOneOfReferencePanel(owner, cfg, model, EntityListPanel.ActorType.SUBJECT, record, PlaceRelationshipHandler.class)),

	// PlaceRelationshipRecord (target = this place)
	PLACE_RELATIONSHIP_ON_TARGET((owner, cfg, model, record) ->
		createOneOfReferencePanel(owner, cfg, model, EntityListPanel.ActorType.OBJECT, record, PlaceRelationshipHandler.class)),

	// EventParticipationRecord (participant[individual] = this individual)
	// EventParticipationRecord (participant[group] = this group)
	// EventParticipationRecord (participant[place] = this place)
	EVENT_PARTICIPATION_ON_PARTICIPANT((owner, cfg, model, record) ->
		createOneOfReferencePanel(owner, cfg, model, null, record, EventParticipationHandler.class)),

	// EventParticipationRecord (event = this event)
	EVENT_PARTICIPATION_ON_EVENT((owner, cfg, model, record) ->
		createOneOfReferencePanel(owner, cfg, model, EntityListPanel.ActorType.EVENT, record, EventParticipationHandler.class)),

	// ContextImpactRecord (target[individual] = this individual)
	// ContextImpactRecord (target[group] = this group)
	// ContextImpactRecord (target[individual_attribute] = this attribute)
	// ContextImpactRecord (target[identity_hypothesis] = this hypothesis)
	// ContextImpactRecord (target[event] = this event)
	// ContextImpactRecord (target[event_participation] = this participation)
	// ContextImpactRecord (target[relationship] = this relationship)
	// ContextImpactRecord (target[place_relationship] = this relationship)
	// ContextImpactRecord (target[place] = this place)
	CONTEXT_IMPACT_ON_TARGET((owner, cfg, model, record) ->
		createOneOfReferencePanel(owner, cfg, model, null, record, ContextImpactHandler.class)),

	// ContextImpactRecord (context[historic_event] = this historic event)
	// ContextImpactRecord (context[cultural_norm] = this norm)
	CONTEXT_IMPACT_ON_CONTEXT((owner, cfg, model, record) ->
		createOneOfReferencePanel(owner, cfg, model, null, record, ContextImpactHandler.class)),

	// ConclusionRecord (resolves = this individual)
	// ConclusionRecord (resolves = this group)
	// ConclusionRecord (resolves = this attribute)
	// ConclusionRecord (resolves = this source)
	// ConclusionRecord (resolves = this event)
	// ConclusionRecord (resolves = this participation)
	// ConclusionRecord (resolves = this historic event)
	// ConclusionRecord (resolves = this question)
	// ConclusionRecord (resolves = this relationship)
	// ConclusionRecord (resolves = this relationship)
	// ConclusionRecord (resolves = this place)
	// ConclusionRecord (resolves = this norm)
	CONCLUSION_ON_RESOLVES((owner, cfg, model, record) ->
		createOneOfReferencePanel(owner, cfg, model, null, record, ConclusionHandler.class)),

	// ConclusionRecord (research contains this question)
	CONCLUSION_ON_RESEARCH((owner, cfg, model, record) ->
		createOneOfReferencePanel(owner, cfg, model, null, record, ConclusionHandler.class)),

	// IdentityHypothesisRecord (identities contains this individual)
	// IdentityHypothesisRecord (identities contains this group)
	// IdentityHypothesisRecord (identities contains this place)
	IDENTITY_HYPOTHESIS_ON_IDENTITY((owner, cfg, model, record) ->
		createOneOfReferencePanel(owner, cfg, model, null, record, IdentityHypothesisHandler.class)),

	// ResearchQuestionRecord (target[individual] = this individual)
	// ResearchQuestionRecord (target[group] = this group)
	// ResearchQuestionRecord (target[individual_attribute] = this attribute)
	// ResearchQuestionRecord (target[source] = this source)
	// ResearchQuestionRecord (target[event] = this event)
	// ResearchQuestionRecord (target[event_participation] = this participation)
	// ResearchQuestionRecord (target[document] = this document)
	// ResearchQuestionRecord (target[historic_event] = this historic event)
	// ResearchQuestionRecord (target[relationship] = this relationship)
	// ResearchQuestionRecord (target[place_relationship] = this relationship)
	// ResearchQuestionRecord (target[place] = this place)
	// ResearchQuestionRecord (target[cultural_norm] = this norm)
	RESEARCH_QUESTION_ON_TARGET((owner, cfg, model, record) ->
		createOneOfReferencePanel(owner, cfg, model, null, record, ResearchQuestionHandler.class)),

	// ResearchActivityRecord (question contains this question)
	RESEARCH_ACTIVITY_ON_QUESTION((owner, cfg, model, record) ->
		createOneOfReferencePanel(owner, cfg, model, null, record, ResearchActivityHandler.class)),

	// ResearchActivityRecord (source contains this source)
	RESEARCH_ACTIVITY_ON_SOURCE((owner, cfg, model, record) ->
		createOneOfReferencePanel(owner, cfg, model, null, record, ResearchActivityHandler.class)),

	// ResearchTaskRecord (question contains this question)
	RESEARCH_TASK_ON_QUESTION((owner, cfg, model, record) ->
		createOneOfReferencePanel(owner, cfg, model, null, record, ResearchTaskHandler.class)),

	// SourceRecord (repository references this repository)
	SOURCE_ON_REPOSITORY((owner, cfg, model, record) ->
		createOneOfReferencePanel(owner, cfg, model, null, record, SourceHandler.class)),

	// SourceRecord (document references this document)
	SOURCE_ON_DOCUMENT((owner, cfg, model, record) ->
		createOneOfReferencePanel(owner, cfg, model, null, record, SourceHandler.class)),

	// Citation wrapper panels
	PLACE((owner, cfg, model, record) -> createCitationWrapperPanel(owner, cfg, model, PlaceCitationHandler.class)),
	REPOSITORY((owner, cfg, model, record) -> createCitationWrapperPanel(owner, cfg, model, RepositoryCitationHandler.class)),
	SOURCE((owner, cfg, model, record) -> createCitationWrapperPanel(owner, cfg, model, SourceCitationHandler.class)),

	// Structure panel
	NOTE((owner, cfg, model, record) -> createStructurePanel(owner, cfg, model, NoteHandler.class)),

	// Entity reference panels
	DOCUMENT((owner, cfg, model, record) -> createEntityReferencePanel(owner, cfg, model, DocumentHandler.class)),
	RESEARCH_QUESTION((owner, cfg, model, record) -> createEntityReferencePanel(owner, cfg, model, ResearchQuestionHandler.class)),
	TASK((owner, cfg, model, record) -> createEntityReferencePanel(owner, cfg, model, ResearchTaskHandler.class)),

	// Specialized components
	EVIDENCE((owner, cfg, model, record) -> new EvidenceQualifiersPanel(cfg.tag(), cfg.title())),
	PRIVACY((owner, cfg, model, record) -> new PrivacyPanel(cfg.tag())),
	AUDIT((owner, cfg, model, record) -> new AuditPanel(owner));


	private final PanelFactory factory;


	PanelKey(final PanelFactory factory){
		this.factory = Objects.requireNonNull(factory);
	}


	/**
	 * Creates the component panel associated with this key.
	 *
	 * @param owner  The parent dialog owner.
	 * @param cfg    The configuration for this entity panel.
	 * @param model  The underlying FLEF model.
	 * @param record The active record.
	 * @return The created UI panel.
	 */
	public JPanel createPanel(final BaseRecordDialog owner, final RecordDialogBuilder.EntityReferenceConfig cfg,
			final FLEFModel model, final FLEFRecord record){
		return factory.createPanel(owner, cfg, model, record);
	}

	// Helper methods to keep panel instantiation concise and readable

	private static EntityListPanel createOneOfReferencePanel(final BaseRecordDialog owner,
			final RecordDialogBuilder.EntityReferenceConfig cfg, final FLEFModel model,
			final EntityListPanel.ActorType actorType, final FLEFRecord record,
			final Class<? extends RecordTypeHandler<?>> handlerType){
		final EntityListPanel panel = EntityListPanel.createForOneOfReference(cfg.tag(), owner, cfg.title(), model, actorType)
			.withHandlerTypes(handlerType);
		if(record != null)
			panel.withParentEntity(record);
		return panel;
	}

	private static EntityListPanel createCitationWrapperPanel(final BaseRecordDialog owner,
			final RecordDialogBuilder.EntityReferenceConfig cfg, final FLEFModel model,
			final Class<? extends RecordTypeHandler<?>> handlerType){
		return EntityListPanel.createForCitationWrapper(cfg.tag(), owner, cfg.title(), model, handlerType);
	}

	private static EntityListPanel createStructurePanel(final BaseRecordDialog owner,
			final RecordDialogBuilder.EntityReferenceConfig cfg, final FLEFModel model,
			final Class<? extends RecordTypeHandler<?>> handlerType){
		return EntityListPanel.createForStructure(cfg.tag(), owner, cfg.title(), model, handlerType);
	}

	private static EntityListPanel createEntityReferencePanel(final BaseRecordDialog owner,
			final RecordDialogBuilder.EntityReferenceConfig cfg, final FLEFModel model,
			final Class<? extends RecordTypeHandler<?>> handlerType){
		return EntityListPanel.createForEntityReference(cfg.tag(), owner, cfg.title(), model, handlerType);
	}

}
