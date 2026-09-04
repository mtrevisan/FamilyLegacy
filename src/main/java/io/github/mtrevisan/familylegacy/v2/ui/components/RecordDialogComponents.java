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
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.PathBound;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
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
			// RelationshipRecord (subject = this individual)
			// RelationshipRecord (subject = this group)
			case RELATIONSHIP_ON_SUBJECT ->
				createOneOfReferencePanel(cfg, model, EntityListPanel.ActorType.SUBJECT, record,
					RelationshipHandler.class);

			// PlaceRelationshipRecord (subject = this place)
			case PLACE_RELATIONSHIP_ON_SUBJECT ->
				createOneOfReferencePanel(cfg, model, EntityListPanel.ActorType.SUBJECT, record,
					PlaceRelationshipHandler.class);

			// RelationshipRecord (target = this individual)
			// RelationshipRecord (target = this group)
			case RELATIONSHIP_ON_TARGET ->
				createOneOfReferencePanel(cfg, model, EntityListPanel.ActorType.OBJECT, record,
					RelationshipHandler.class);
			// PlaceRelationshipRecord (target = this place)
			case PLACE_RELATIONSHIP_ON_TARGET ->
				createOneOfReferencePanel(cfg, model, EntityListPanel.ActorType.OBJECT, record,
					PlaceRelationshipHandler.class);

			// IndividualAttributeRecord (individual = this individual)
			case INDIVIDUAL_ATTRIBUTE ->
				createOneOfReferencePanel(cfg, model, null, record,
					IndividualAttributeHandler.class);
			// GroupAttributeRecord (group = this group)
			case GROUP_ATTRIBUTE ->
				createOneOfReferencePanel(cfg, model, null, record,
					GroupAttributeHandler.class);

			// EventParticipationRecord (participant[individual] = this individual)
			// EventParticipationRecord (participant[group] = this group)
			// EventParticipationRecord (participant[place] = this place)
			case EVENT_PARTICIPATION_ON_PARTICIPANT ->
				createOneOfReferencePanel(cfg, model, null, record,
					EventParticipationHandler.class);

			case
				// ContextImpactRecord (target[individual] = this individual)
				// ContextImpactRecord (target[group] = this group)
				// ContextImpactRecord (target[individual_attribute] = this attribute)
				// ContextImpactRecord (target[identity_hypothesis] = this hypothesis)
				// ContextImpactRecord (target[event] = this event)
				// ContextImpactRecord (target[event_participation] = this participation)
				// ContextImpactRecord (target[relationship] = this relationship)
				// ContextImpactRecord (target[place_relationship] = this relationship)
				// ContextImpactRecord (target[place] = this place)
				CONTEXT_IMPACT_ON_TARGET,
				// ContextImpactRecord (context[historic_event] = this historic event)
				// ContextImpactRecord (context[cultural_norm] = this norm)
				CONTEXT_IMPACT_ON_CONTEXT ->
					createOneOfReferencePanel(cfg, model, null, record,
						ContextImpactHandler.class);

			case
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
				// ConclusionRecord (research contains this question)
				CONCLUSION_ON_RESEARCH ->
					createOneOfReferencePanel(cfg, model, null, record,
						ConclusionHandler.class);
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
			case RESEARCH_QUESTION_ON_TARGET ->
				createOneOfReferencePanel(cfg, model, null, record,
					ResearchQuestionHandler.class);

			// IdentityHypothesisRecord (identities contains this individual)
			// IdentityHypothesisRecord (identities contains this group)
			// IdentityHypothesisRecord (identities contains this place)
			case IDENTITY_HYPOTHESIS_ON_IDENTITY ->
				createOneOfReferencePanel(cfg, model, null, record,
					IdentityHypothesisHandler.class);

			// EventParticipationRecord (event = this event)
			case EVENT_PARTICIPATION_ON_EVENT ->
				createOneOfReferencePanel(cfg, model, EntityListPanel.ActorType.EVENT, record,
					EventParticipationHandler.class);

			case
				// ResearchActivityRecord (source contains this source)
				RESEARCH_ACTIVITY_ON_SOURCE,
				// ResearchActivityRecord (question contains this question)
				RESEARCH_ACTIVITY_ON_QUESTION ->
				createOneOfReferencePanel(cfg, model, null, record,
					ResearchActivityHandler.class);
			// ResearchTaskRecord (question contains this question)
			case RESEARCH_TASK_ON_QUESTION ->
				createOneOfReferencePanel(cfg, model, null, record,
					ResearchTaskHandler.class);

			case PLACE -> createCitationWrapperPanel(cfg, model, PlaceCitationHandler.class);
			case REPOSITORY -> createCitationWrapperPanel(cfg, model, RepositoryCitationHandler.class);
			case SOURCE -> createCitationWrapperPanel(cfg, model, SourceCitationHandler.class);

			case
				// SourceRecord (repository references this repository)
				SOURCE_ON_REPOSITORY,
				// SourceRecord (document references this document)
				SOURCE_ON_DOCUMENT ->
				createOneOfReferencePanel(cfg, model, null, record,
					SourceHandler.class);

			case EVIDENCE -> new EvidenceQualifiersPanel(cfg.tag(), cfg.title());

			case NOTE -> createStructurePanel(cfg, model, NoteHandler.class);

			// embedded reference
			case DOCUMENT -> createEntityReferencePanel(cfg, model, DocumentHandler.class);
			case RESEARCH_QUESTION -> createEntityReferencePanel(cfg, model, ResearchQuestionHandler.class);
			case TASK -> createEntityReferencePanel(cfg, model, ResearchTaskHandler.class);

			case PRIVACY -> new PrivacyPanel(cfg.tag());

			case AUDIT -> new AuditPanel(owner);
		};
	}

	private EntityListPanel createOneOfReferencePanel(final RecordDialogBuilder.EntityReferenceConfig cfg,
			final FLEFModel model, final EntityListPanel.ActorType actorType, final FLEFRecord record,
			final Class<? extends RecordTypeHandler<?>> handlerType){
		final EntityListPanel panel = EntityListPanel.createForOneOfReference(cfg.tag(), owner, cfg.title(), model,
				actorType)
			.withHandlerTypes(handlerType);
		if(record != null)
			panel.withParentEntity(record);
		return panel;
	}

	private EntityListPanel createCitationWrapperPanel(final RecordDialogBuilder.EntityReferenceConfig cfg,
			final FLEFModel model, final Class<? extends RecordTypeHandler<?>> handlerType){
		return EntityListPanel.createForCitationWrapper(cfg.tag(), owner, cfg.title(), model, handlerType);
	}

	private EntityListPanel createStructurePanel(final RecordDialogBuilder.EntityReferenceConfig cfg,
			final FLEFModel model, final Class<? extends RecordTypeHandler<?>> handlerType){
		return EntityListPanel.createForStructure(cfg.tag(), owner, cfg.title(), model, handlerType);
	}

	private EntityListPanel createEntityReferencePanel(final RecordDialogBuilder.EntityReferenceConfig cfg,
			final FLEFModel model, final Class<? extends RecordTypeHandler<?>> handlerType){
		return EntityListPanel.createForEntityReference(cfg.tag(), owner, cfg.title(), model, handlerType);
	}


	public JPanel getPanel(final PanelKey key){
		return panels.get(key);
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
		if(!record.hasChildren())
			return;

		bindingManager.load(record);

		final EntityListPanel individualAttribute = ((EntityListPanel)getPanel(PanelKey.INDIVIDUAL_ATTRIBUTE));
		if(individualAttribute != null)
			individualAttribute.loadReference(record.getId());
		final EntityListPanel groupAttribute = ((EntityListPanel)getPanel(PanelKey.GROUP_ATTRIBUTE));
		if(groupAttribute != null)
			groupAttribute.loadReference(record.getId());

		final EntityListPanel relationshipAsSubject = ((EntityListPanel)getPanel(PanelKey.RELATIONSHIP_ON_SUBJECT));
		if(relationshipAsSubject != null)
			relationshipAsSubject.loadReferenceWithType(record.getId(), "SUBJECT");
		final EntityListPanel relationshipAsTarget = ((EntityListPanel)getPanel(PanelKey.RELATIONSHIP_ON_TARGET));
		if(relationshipAsTarget != null)
			relationshipAsTarget.loadReferenceWithType(record.getId(), "TARGET");

		final EntityListPanel placeRelationshipAsSubject = ((EntityListPanel)getPanel(PanelKey.PLACE_RELATIONSHIP_ON_SUBJECT));
		if(placeRelationshipAsSubject != null)
			placeRelationshipAsSubject.loadReferenceWithType(record.getId(), "SUBJECT");
		final EntityListPanel placeRelationshipAsTarget = ((EntityListPanel)getPanel(PanelKey.PLACE_RELATIONSHIP_ON_TARGET));
		if(placeRelationshipAsTarget != null)
			placeRelationshipAsTarget.loadReferenceWithType(record.getId(), "TARGET");

		final EntityListPanel eventParticipationOnParticipant = ((EntityListPanel)getPanel(PanelKey.EVENT_PARTICIPATION_ON_PARTICIPANT));
		if(eventParticipationOnParticipant != null)
			eventParticipationOnParticipant.loadReferenceWithType(record.getId(), "PARTICIPANT");
		final EntityListPanel eventParticipationOnEvent = ((EntityListPanel)getPanel(PanelKey.EVENT_PARTICIPATION_ON_EVENT));
		if(eventParticipationOnEvent != null)
			eventParticipationOnEvent.withParentEntity(record)
				.loadCitationsWithType(record.getId(), "EVENT");

		final EntityListPanel contextOnTarget = ((EntityListPanel)getPanel(PanelKey.CONTEXT_IMPACT_ON_TARGET));
		if(contextOnTarget != null)
			contextOnTarget.loadReferenceWithType(record.getId(), "TARGET");
		final EntityListPanel contextOnContext = ((EntityListPanel)getPanel(PanelKey.CONTEXT_IMPACT_ON_CONTEXT));
		if(contextOnContext != null)
			contextOnContext.loadReferenceWithType(record.getId(), "CONTEXT");

		final EntityListPanel conclusionOnResolves = ((EntityListPanel)getPanel(PanelKey.CONCLUSION_ON_RESOLVES));
		if(conclusionOnResolves != null)
			conclusionOnResolves.loadReferenceWithType(record.getId(), "RESOLVES");
		final EntityListPanel conclusionOnResearch = ((EntityListPanel)getPanel(PanelKey.CONCLUSION_ON_RESEARCH));
		if(conclusionOnResearch != null)
			conclusionOnResearch.withParentEntity(record)
				.loadCitationsWithType(record.getId(), "RESEARCH");
		final EntityListPanel identityHypothesis = ((EntityListPanel)getPanel(PanelKey.IDENTITY_HYPOTHESIS_ON_IDENTITY));
		if(identityHypothesis != null)
			identityHypothesis.loadReferenceWithType(record.getId(), "IDENTITY");
		final EntityListPanel researchQuestionOnTarget = ((EntityListPanel)getPanel(PanelKey.RESEARCH_QUESTION_ON_TARGET));
		if(researchQuestionOnTarget != null)
			researchQuestionOnTarget.loadReferenceWithType(record.getId(), "TARGET");
		final EntityListPanel researchActivityOnQuestion = ((EntityListPanel)getPanel(PanelKey.RESEARCH_ACTIVITY_ON_QUESTION));
		if(researchActivityOnQuestion != null)
			researchActivityOnQuestion.withParentEntity(record)
				.loadCitationsWithType3(record.getId(), "QUESTION");
		final EntityListPanel researchActivityOnSource = ((EntityListPanel)getPanel(PanelKey.RESEARCH_ACTIVITY_ON_SOURCE));
		if(researchActivityOnSource != null)
			researchActivityOnSource.withParentEntity(record)
				.loadCitationsWithType2(record.getId(), "SOURCE");

		final EntityListPanel place = ((EntityListPanel)getPanel(PanelKey.PLACE));
		if(place != null)
			place.load(record);

		final EntityListPanel repository = ((EntityListPanel)getPanel(PanelKey.REPOSITORY));
		if(repository != null)
			repository.load(record);

		final EntityListPanel source = ((EntityListPanel)getPanel(PanelKey.SOURCE));
		if(source != null)
			source.load(record);
		final EntityListPanel sourceOnRepository = ((EntityListPanel)getPanel(PanelKey.SOURCE_ON_REPOSITORY));
		if(sourceOnRepository != null)
			sourceOnRepository.withParentEntity(record)
				.loadCitationsWithType2(record.getId(), "REPOSITORY");
		final EntityListPanel sourceOnDocument = ((EntityListPanel)getPanel(PanelKey.SOURCE_ON_DOCUMENT));
		if(sourceOnDocument != null)
			sourceOnDocument.withParentEntity(record)
				.loadCitationsWithType3(record.getId(), "DOCUMENT");

		final EntityListPanel document = ((EntityListPanel)getPanel(PanelKey.DOCUMENT));
		if(document != null)
			document.load(record);

		final EntityListPanel researchQuestion = ((EntityListPanel)getPanel(PanelKey.RESEARCH_QUESTION));
		if(researchQuestion != null)
			researchQuestion.load(record);

		final EntityListPanel researchTaskOnQuestion = ((EntityListPanel)getPanel(PanelKey.RESEARCH_TASK_ON_QUESTION));
		if(researchTaskOnQuestion != null)
			researchTaskOnQuestion.withParentEntity(record)
				.loadCitationsWithType(record.getId(), "QUESTION");

		final EntityListPanel note = ((EntityListPanel)getPanel(PanelKey.NOTE));
		if(note != null)
			note.load(record);

		final EntityListPanel task = ((EntityListPanel)getPanel(PanelKey.TASK));
		if(task != null)
			task.load(record);

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

		for(final JPanel panel : panels.values()){
			if(panel instanceof EntityListPanel elp)
				elp.save(record);
			else if(panel instanceof EvidenceQualifiersPanel eqp)
				eqp.save(record);
			else if(panel instanceof PrivacyPanel pp)
				pp.save(record);
			else if(panel instanceof AuditPanel ap)
				ap.save(record);
		}
	}

}
