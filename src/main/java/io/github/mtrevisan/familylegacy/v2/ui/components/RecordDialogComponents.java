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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.PathBound;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityReferenceListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;

import javax.swing.JPanel;
import java.util.EnumMap;
import java.util.Map;


/**
 * Centralized container for all common UI panels used in record dialogs.
 * Handles loading and saving of data for all panels.
 * <p>
 * Use {@link io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder} to create an instance with custom configurations.
 */
public final class RecordDialogComponents{

	// Binding manager for simple fields (sex combo, etc.)
	private final BindingManager bindingManager = new BindingManager();


	private final Map<PanelKey, JPanel> panels = new EnumMap<>(PanelKey.class);
	// Owner dialog (used for listeners, etc.)
	private final BaseRecordDialog owner;


	RecordDialogComponents(final RecordDialogBuilder builder){
		this.owner = builder.owner;

		final FLEFModel model = builder.model;
		final FLEFRecord record = builder.record;

		// Initialize each panel with builder-provided values:
		for(final Map.Entry<PanelKey, RecordDialogBuilder.EntityReferenceConfig> entry : builder.configs.entrySet()){
			final PanelKey key = entry.getKey();
			final RecordDialogBuilder.EntityReferenceConfig cfg = entry.getValue();

			final JPanel panel = createPanel(key, cfg, model, record);

			panels.put(key, panel);
		}
	}

	private JPanel createPanel(final PanelKey key, final RecordDialogBuilder.EntityReferenceConfig cfg,
			final FLEFModel model, final FLEFRecord record){
		return switch(key){
			case
				// RelationshipRecord (subject = this individual)
				// RelationshipRecord (subject = this group)
				RELATIONSHIP_ON_SUBJECT,

				// PlaceRelationshipRecord (subject = this place)
				PLACE_RELATIONSHIP_ON_SUBJECT -> {
					final EntityReferenceListPanel panel = EntityReferenceListPanel.createForRecord(cfg.tag(),
						owner, cfg.title(), model, cfg.handlerClass(), EntityReferenceListPanel.ActorType.SUBJECT);
					if(record != null)
						panel.withParentEntity(record.getId(), HandlerRegistry.getHandlerType(cfg.handlerType()));
					yield panel;
				}

			case
				// RelationshipRecord (object = this individual)
				// RelationshipRecord (object = this group)
				RELATIONSHIP_ON_OBJECT,

				// PlaceRelationshipRecord (object = this place)
				PLACE_RELATIONSHIP_ON_OBJECT -> {
					final EntityReferenceListPanel panel = EntityReferenceListPanel.createForRecord(cfg.tag(),
						owner, cfg.title(), model, cfg.handlerClass(), EntityReferenceListPanel.ActorType.OBJECT);
					if(record != null)
						panel.withParentEntity(record.getId(), HandlerRegistry.getHandlerType(cfg.handlerType()));
					yield panel;
				}

			case
				// IndividualAttributeRecord (individual = this individual)
				INDIVIDUAL_ATTRIBUTE,
				// GroupAttributeRecord (group = this group)
				GROUP_ATTRIBUTE,

				// EventParticipationRecord (participant.individual = this individual)
				// EventParticipationRecord (participant.group = this group)
				// EventParticipationRecord (participant.place = this place)
				EVENT_PARTICIPATION_ON_PARTICIPANT,

				// ContextImpactRecord (target.individual = this individual)
				// ContextImpactRecord (target.group = this group)
				// ContextImpactRecord (target.individual_attribute = this attribute)
				// ContextImpactRecord (target.identity_hypothesis = this hypothesis)
				// ContextImpactRecord (target.event = this event)
				// ContextImpactRecord (target.event_participation = this participation)
				// ContextImpactRecord (target.relationship = this relationship)
				// ContextImpactRecord (target.place_relationship = this relationship)
				// ContextImpactRecord (target.place = this place)
				CONTEXT_IMPACT_ON_TARGET,
				// ContextImpactRecord (context.historic_event = this historic event)
				// ContextImpactRecord (context.cultural_norm = this norm)
				CONTEXT_IMPACT_ON_CONTEXT,

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
				CONCLUSION_ON_RESOLVES,
				// IdentityHypothesisRecord (subject/candidate = this individual)
				// IdentityHypothesisRecord (subject/candidate = this group)
				// IdentityHypothesisRecord (subject/candidate = this place)
				IDENTITY_HYPOTHESIS_ON_SUBJECT_OR_CANDIDATE,
				// ResearchQuestionRecord (target.individual = this individual)
				// ResearchQuestionRecord (target.group = this group)
				// ResearchQuestionRecord (target.individual_attribute = this attribute)
				// ResearchQuestionRecord (target.source = this source)
				// ResearchQuestionRecord (target.event = this event)
				// ResearchQuestionRecord (target.event_participation = this participation)
				// ResearchQuestionRecord (target.document = this document)
				// ResearchQuestionRecord (target.historic_event = this historic event)
				// ResearchQuestionRecord (target.relationship = this relationship)
				// ResearchQuestionRecord (target.place_relationship = this relationship)
				// ResearchQuestionRecord (target.place = this place)
				// ResearchQuestionRecord (target.cultural_norm = this norm)
				RESEARCH_QUESTION_ON_TARGET -> {
					final EntityReferenceListPanel panel = EntityReferenceListPanel.createForRecord(cfg.tag(),
						owner, cfg.title(), model, cfg.handlerClass());
					if(record != null)
						panel.withParentEntity(record.getId(), HandlerRegistry.getHandlerType(cfg.handlerType()));
					yield panel;
				}

			// EventParticipationRecord (event = this event)
			case EVENT_PARTICIPATION_ON_EVENT -> {
				final EntityReferenceListPanel panel = EntityReferenceListPanel.createForRecord(cfg.tag(),
					owner, cfg.title(), model, cfg.handlerClass());
				if(record != null)
					panel.withParentEntity(record.getId(), HandlerRegistry.getHandlerType(cfg.handlerType()));
				yield panel;
			}

			case
				// ResearchActivityRecord (source contains this source)
				RESEARCH_ACTIVITY_ON_SOURCE,
				// ResearchActivityRecord (question contains this question)
				RESEARCH_ACTIVITY_ON_QUESTION,
				// ResearchTaskRecord (question contains this question)
				RESEARCH_TASK ->
				EntityReferenceListPanel.createForRecord(cfg.tag(), owner, cfg.title(), model, cfg.handlerClass());

			case PLACE,
				  REPOSITORY,
				  SOURCE ->
				new EntityCitationListPanel(cfg.tag(), cfg.citationTag(), owner, cfg.title(), model, cfg.handlerType());

			// SourceRecord (repository references this repository)
			case SOURCE_ON_REPOSITORY ->
				EntityReferenceListPanel.createForRecord(cfg.tag(), owner, cfg.title(), model, cfg.handlerClass());

			case EVIDENCE -> new EvidenceQualifiersPanel(cfg.tag(), owner, cfg.title(), model, HandlerRegistry.getHandlerType(cfg.handlerType()));

			// embedded reference
			case CONTEXT_IMPACT,
				  NOTE,
				  DOCUMENT ->
				new EntityListPanel(cfg.tag(), owner, cfg.title(), model, cfg.handlerType());

			case PRIVACY -> new PrivacyPanel(cfg.tag(), owner);

			case AUDIT -> new AuditPanel(owner);
		};
	}


	/**
	 * Registers a bound component.
	 *
	 * @param component	the component to register
	 */
	public void bind(final PathBound component){
		bindingManager.bind(component);
	}

	/**
	 * Loads data from the given record into all panels.
	 */
	public void load(final FLEFRecord record){
		bindingManager.load(record);

		final EntityReferenceListPanel individualAttribute = ((EntityReferenceListPanel)getPanel(PanelKey.INDIVIDUAL_ATTRIBUTE));
		if(individualAttribute != null)
			individualAttribute.loadReference(record.getId());
		final EntityReferenceListPanel groupAttribute = ((EntityReferenceListPanel)getPanel(PanelKey.GROUP_ATTRIBUTE));
		if(groupAttribute != null)
			groupAttribute.loadReference(record.getId());

		final EntityReferenceListPanel relationshipAsSubject = ((EntityReferenceListPanel)getPanel(PanelKey.RELATIONSHIP_ON_SUBJECT));
		if(relationshipAsSubject != null)
			relationshipAsSubject.loadReferenceWithType(record.getId(), "SUBJECT");
		final EntityReferenceListPanel relationshipAsObject = ((EntityReferenceListPanel)getPanel(PanelKey.RELATIONSHIP_ON_OBJECT));
		if(relationshipAsObject != null)
			relationshipAsObject.loadReferenceWithType(record.getId(), "OBJECT");

		final EntityReferenceListPanel placeRelationshipAsSubject = ((EntityReferenceListPanel)getPanel(PanelKey.PLACE_RELATIONSHIP_ON_SUBJECT));
		if(placeRelationshipAsSubject != null)
			placeRelationshipAsSubject.loadReferenceWithType(record.getId(), "SUBJECT");
		final EntityReferenceListPanel placeRelationshipAsObject = ((EntityReferenceListPanel)getPanel(PanelKey.PLACE_RELATIONSHIP_ON_OBJECT));
		if(placeRelationshipAsObject != null)
			placeRelationshipAsObject.loadReferenceWithType(record.getId(), "OBJECT");

		final EntityReferenceListPanel eventParticipationOnParticipant = ((EntityReferenceListPanel)getPanel(PanelKey.EVENT_PARTICIPATION_ON_PARTICIPANT));
		if(eventParticipationOnParticipant != null)
			eventParticipationOnParticipant.loadReferenceWithType(record.getId(), "PARTICIPANT");
		final EntityReferenceListPanel eventParticipationOnEvent = ((EntityReferenceListPanel)getPanel(PanelKey.EVENT_PARTICIPATION_ON_EVENT));
		if(eventParticipationOnEvent != null)
			eventParticipationOnEvent.loadReferenceWithType(record.getId(), "EVENT");

		final EntityListPanel contextImpact = ((EntityListPanel)getPanel(PanelKey.CONTEXT_IMPACT));
		if(contextImpact != null)
			contextImpact.load(record);
		final EntityReferenceListPanel contextOnTarget = ((EntityReferenceListPanel)getPanel(PanelKey.CONTEXT_IMPACT_ON_TARGET));
		if(contextOnTarget != null)
			contextOnTarget.loadReferenceWithType(record.getId(), "TARGET");
		final EntityReferenceListPanel contextOnContext = ((EntityReferenceListPanel)getPanel(PanelKey.CONTEXT_IMPACT_ON_CONTEXT));
		if(contextOnContext != null)
			contextOnContext.loadReferenceWithType(record.getId(), "CONTEXT");

		final EntityReferenceListPanel conclusion = ((EntityReferenceListPanel)getPanel(PanelKey.CONCLUSION_ON_RESOLVES));
		if(conclusion != null)
			conclusion.loadReferenceWithType(record.getId(), "RESOLVES");
		final EntityReferenceListPanel identityHypothesis = ((EntityReferenceListPanel)getPanel(PanelKey.IDENTITY_HYPOTHESIS_ON_SUBJECT_OR_CANDIDATE));
		if(identityHypothesis != null)
			identityHypothesis.loadReferenceWithType(record.getId(), "SUBJECT", "CANDIDATE");
		final EntityReferenceListPanel researchQuestion = ((EntityReferenceListPanel)getPanel(PanelKey.RESEARCH_QUESTION_ON_TARGET));
		if(researchQuestion != null)
			researchQuestion.loadReferenceWithType(record.getId(), "TARGET");
		final EntityReferenceListPanel researchActivityOnQuestion = ((EntityReferenceListPanel)getPanel(PanelKey.RESEARCH_ACTIVITY_ON_QUESTION));
		if(researchActivityOnQuestion != null)
			researchActivityOnQuestion.loadReferenceWithType(record.getId(), "QUESTION");
		final EntityReferenceListPanel researchActivityOnSource = ((EntityReferenceListPanel)getPanel(PanelKey.RESEARCH_ACTIVITY_ON_SOURCE));
		if(researchActivityOnSource != null)
			researchActivityOnSource.loadReferenceWithType(record.getId(), "SOURCE");

		final EntityCitationListPanel place = ((EntityCitationListPanel)getPanel(PanelKey.PLACE));
		if(place != null)
			place.load(record);

		final EntityCitationListPanel repository = ((EntityCitationListPanel)getPanel(PanelKey.REPOSITORY));
		if(repository != null)
			repository.load(record);

		final EntityCitationListPanel source = ((EntityCitationListPanel)getPanel(PanelKey.SOURCE));
		if(source != null)
			source.load(record);
		final EntityCitationListPanel sourceOnRepository = ((EntityCitationListPanel)getPanel(PanelKey.SOURCE_ON_REPOSITORY));
		if(sourceOnRepository != null)
			sourceOnRepository.load(record);

		final EntityListPanel document = ((EntityListPanel)getPanel(PanelKey.DOCUMENT));
		if(document != null)
			document.load(record);

		final EntityReferenceListPanel researchTask = ((EntityReferenceListPanel)getPanel(PanelKey.RESEARCH_TASK));
		if(researchTask != null)
			researchTask.load(record);

		final EntityListPanel note = ((EntityListPanel)getPanel(PanelKey.NOTE));
		if(note != null)
			note.load(record);

		final EvidenceQualifiersPanel evidence = ((EvidenceQualifiersPanel)getPanel(PanelKey.EVIDENCE));
		if(evidence != null)
			evidence.load(record);

		final PrivacyPanel privacy = ((PrivacyPanel)getPanel(PanelKey.PRIVACY));
		if(privacy != null)
			privacy.load(record);

		final AuditPanel audit = ((AuditPanel)getPanel(PanelKey.AUDIT));
		if(audit != null)
			audit.load(record);
	}

	/**
	 * Saves data from all panels into the given record.
	 */
	public void save(final FLEFRecord record){
		bindingManager.save(record);


		final EntityReferenceListPanel individualAttribute = ((EntityReferenceListPanel)getPanel(PanelKey.INDIVIDUAL_ATTRIBUTE));
		if(individualAttribute != null)
			individualAttribute.save(record);
		final EntityReferenceListPanel groupAttribute = ((EntityReferenceListPanel)getPanel(PanelKey.GROUP_ATTRIBUTE));
		if(groupAttribute != null)
			groupAttribute.save(record);

		final EntityReferenceListPanel relationshipAsSubject = ((EntityReferenceListPanel)getPanel(PanelKey.RELATIONSHIP_ON_SUBJECT));
		if(relationshipAsSubject != null)
			relationshipAsSubject.save(record);
		final EntityReferenceListPanel relationshipAsObject = ((EntityReferenceListPanel)getPanel(PanelKey.RELATIONSHIP_ON_OBJECT));
		if(relationshipAsObject != null)
			relationshipAsObject.save(record);

		final EntityReferenceListPanel placeRelationshipAsSubject = ((EntityReferenceListPanel)getPanel(PanelKey.PLACE_RELATIONSHIP_ON_SUBJECT));
		if(placeRelationshipAsSubject != null)
			placeRelationshipAsSubject.save(record);
		final EntityReferenceListPanel placeRelationshipAsObject = ((EntityReferenceListPanel)getPanel(PanelKey.PLACE_RELATIONSHIP_ON_OBJECT));
		if(placeRelationshipAsObject != null)
			placeRelationshipAsObject.save(record);

		final EntityReferenceListPanel eventParticipationOnParticipation = ((EntityReferenceListPanel)getPanel(PanelKey.EVENT_PARTICIPATION_ON_PARTICIPANT));
		if(eventParticipationOnParticipation != null)
			eventParticipationOnParticipation.save(record);
		final EntityReferenceListPanel eventParticipationOnEvent = ((EntityReferenceListPanel)getPanel(PanelKey.EVENT_PARTICIPATION_ON_EVENT));
		if(eventParticipationOnEvent != null)
			eventParticipationOnEvent.saveReferences(record);

		final EntityListPanel contextImpact = ((EntityListPanel)getPanel(PanelKey.CONTEXT_IMPACT));
		if(contextImpact != null)
			contextImpact.saveReferences(record);
		final EntityReferenceListPanel contextOnTarget = ((EntityReferenceListPanel)getPanel(PanelKey.CONTEXT_IMPACT_ON_TARGET));
		if(contextOnTarget != null)
			contextOnTarget.save(record);
		final EntityReferenceListPanel contextOnContext = ((EntityReferenceListPanel)getPanel(PanelKey.CONTEXT_IMPACT_ON_CONTEXT));
		if(contextOnContext != null)
			contextOnContext.save(record);

		final EntityReferenceListPanel conclusion = ((EntityReferenceListPanel)getPanel(PanelKey.CONCLUSION_ON_RESOLVES));
		if(conclusion != null)
			conclusion.save(record);
		final EntityReferenceListPanel identityHypothesis = ((EntityReferenceListPanel)getPanel(PanelKey.IDENTITY_HYPOTHESIS_ON_SUBJECT_OR_CANDIDATE));
		if(identityHypothesis != null)
			identityHypothesis.save(record);
		final EntityReferenceListPanel researchQuestion = ((EntityReferenceListPanel)getPanel(PanelKey.RESEARCH_QUESTION_ON_TARGET));
		if(researchQuestion != null)
			researchQuestion.save(record);
		final EntityReferenceListPanel researchActivityOnQuestion = ((EntityReferenceListPanel)getPanel(PanelKey.RESEARCH_ACTIVITY_ON_QUESTION));
		if(researchActivityOnQuestion != null)
			researchActivityOnQuestion.save(record);
		final EntityReferenceListPanel researchActivityOnSource = ((EntityReferenceListPanel)getPanel(PanelKey.RESEARCH_ACTIVITY_ON_SOURCE));
		if(researchActivityOnSource != null)
			researchActivityOnSource.save(record);

		final EntityCitationListPanel place = ((EntityCitationListPanel)getPanel(PanelKey.PLACE));
		if(place != null)
			place.save(record);

		final EntityCitationListPanel repository = ((EntityCitationListPanel)getPanel(PanelKey.REPOSITORY));
		if(repository != null)
			repository.save(record);

		final EntityCitationListPanel source = ((EntityCitationListPanel)getPanel(PanelKey.SOURCE));
		if(source != null)
			source.save(record);
		final EntityCitationListPanel sourceOnRepository = ((EntityCitationListPanel)getPanel(PanelKey.SOURCE_ON_REPOSITORY));
		if(sourceOnRepository != null)
			sourceOnRepository.save(record);

		final EntityListPanel document = ((EntityListPanel)getPanel(PanelKey.DOCUMENT));
		if(document != null)
			document.save(record);

		final EntityReferenceListPanel researchTask = ((EntityReferenceListPanel)getPanel(PanelKey.RESEARCH_TASK));
		if(researchTask != null)
			researchTask.save(record);

		final EntityListPanel note = ((EntityListPanel)getPanel(PanelKey.NOTE));
		if(note != null)
			note.saveReferences(record);

		final EvidenceQualifiersPanel evidence = ((EvidenceQualifiersPanel)getPanel(PanelKey.EVIDENCE));
		if(evidence != null)
			evidence.save(record);

		final PrivacyPanel privacy = ((PrivacyPanel)getPanel(PanelKey.PRIVACY));
		if(privacy != null)
			privacy.save(record);

		final AuditPanel audit = ((AuditPanel)getPanel(PanelKey.AUDIT));
		if(audit != null)
			audit.save(record);
	}


	// ------------------------------------------------------------------------
	// Getters for all panels (for UI composition)
	// ------------------------------------------------------------------------

	public JPanel getPanel(final PanelKey key){
		return panels.get(key);
	}

}
