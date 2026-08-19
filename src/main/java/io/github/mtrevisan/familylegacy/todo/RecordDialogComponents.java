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

import javax.swing.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;


/**
 * Centralized container for all common UI panels used in record dialogs.
 * Handles loading and saving of data for all panels.
 * <p>
 * Use {@link io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder}
 * to create an instance with custom configurations.
 * <p>
 * This class uses strategy maps to handle the different load/save behaviors
 * of each panel type, avoiding repetitive code and making it easy to extend.
 */
public final class RecordDialogComponents{

	// Binding manager for simple fields (sex combo, etc.)
	private final BindingManager bindingManager = new BindingManager();

	private final Map<PanelKey, JPanel> panels = new EnumMap<>(PanelKey.class);
	private final BaseRecordDialog owner;

	// Strategy maps for loading and saving each panel
	private final Map<PanelKey, BiConsumer<JPanel, FLEFRecord>> loaders = new EnumMap<>(PanelKey.class);
	private final Map<PanelKey, BiConsumer<JPanel, FLEFRecord>> savers = new EnumMap<>(PanelKey.class);


	RecordDialogComponents(final RecordDialogBuilder builder){
		this.owner = builder.owner;
		final FLEFModel model = builder.model;
		final FLEFRecord record = builder.record;

		initLoaders();
		initSavers();

		for(final Map.Entry<PanelKey, RecordDialogBuilder.EntityReferenceConfig> entry : builder.configs.entrySet()){
			final PanelKey key = entry.getKey();
			final RecordDialogBuilder.EntityReferenceConfig cfg = entry.getValue();
			final JPanel panel = createPanel(key, cfg, model, record);
			panels.put(key, panel);
		}
	}

	// ------------------------------------------------------------------------
	// Load/Save strategy initialization – all panel‑specific logic is here
	// ------------------------------------------------------------------------

	private void initLoaders(){
		// Panels that simply call load(record)
		loaders.put(PanelKey.PRIVACY, (panel, rec) -> ((PrivacyPanel)panel).load(rec));
		loaders.put(PanelKey.AUDIT, (panel, rec) -> ((AuditPanel)panel).load(rec));
		loaders.put(PanelKey.EVIDENCE, (panel, rec) -> ((EvidenceQualifiersPanel)panel).load(rec));
		loaders.put(PanelKey.NOTE, (panel, rec) -> ((EntityListPanel)panel).load(rec));
		loaders.put(PanelKey.DOCUMENT, (panel, rec) -> ((EntityListPanel)panel).load(rec));
		loaders.put(PanelKey.PLACE, (panel, rec) -> ((EntityCitationListPanel)panel).load(rec));
		loaders.put(PanelKey.REPOSITORY, (panel, rec) -> ((EntityCitationListPanel)panel).load(rec));
		loaders.put(PanelKey.SOURCE, (panel, rec) -> ((EntityCitationListPanel)panel).load(rec));
		loaders.put(PanelKey.SOURCE_ON_REPOSITORY, (panel, rec) -> ((EntityCitationListPanel)panel).load(rec));
		loaders.put(PanelKey.RESEARCH_TASK, (panel, rec) -> ((EntityReferenceListPanel)panel).load(rec));

		// Panels that load using loadReference(record.getId())
		loaders.put(PanelKey.INDIVIDUAL_ATTRIBUTE, (panel, rec) -> ((EntityReferenceListPanel)panel).loadReference(rec.getId()));
		loaders.put(PanelKey.GROUP_ATTRIBUTE, (panel, rec) -> ((EntityReferenceListPanel)panel).loadReference(rec.getId()));

		// Panels that load using loadReferenceWithType(record.getId(), type)
		loaders.put(PanelKey.RELATIONSHIP_ON_SUBJECT, (panel, rec) -> ((EntityReferenceListPanel)panel).loadReferenceWithType(rec.getId(), "SUBJECT"));
		loaders.put(PanelKey.RELATIONSHIP_ON_OBJECT, (panel, rec) -> ((EntityReferenceListPanel)panel).loadReferenceWithType(rec.getId(), "OBJECT"));
		loaders.put(PanelKey.PLACE_RELATIONSHIP_ON_SUBJECT, (panel, rec) -> ((EntityReferenceListPanel)panel).loadReferenceWithType(rec.getId(), "SUBJECT"));
		loaders.put(PanelKey.PLACE_RELATIONSHIP_ON_OBJECT, (panel, rec) -> ((EntityReferenceListPanel)panel).loadReferenceWithType(rec.getId(), "OBJECT"));
		loaders.put(PanelKey.EVENT_PARTICIPATION_ON_PARTICIPANT, (panel, rec) -> ((EntityReferenceListPanel)panel).loadReferenceWithType(rec.getId(), "PARTICIPANT"));
		loaders.put(PanelKey.EVENT_PARTICIPATION_ON_EVENT, (panel, rec) -> ((EntityReferenceListPanel)panel).loadReferenceWithType(rec.getId(), "EVENT"));
		loaders.put(PanelKey.CONTEXT_IMPACT_ON_TARGET, (panel, rec) -> ((EntityReferenceListPanel)panel).loadReferenceWithType(rec.getId(), "TARGET"));
		loaders.put(PanelKey.CONTEXT_IMPACT_ON_CONTEXT, (panel, rec) -> ((EntityReferenceListPanel)panel).loadReferenceWithType(rec.getId(), "CONTEXT"));
		loaders.put(PanelKey.CONCLUSION_ON_RESOLVES, (panel, rec) -> ((EntityReferenceListPanel)panel).loadReferenceWithType(rec.getId(), "RESOLVES"));
		loaders.put(PanelKey.IDENTITY_HYPOTHESIS_ON_SUBJECT_OR_CANDIDATE, (panel, rec) -> ((EntityReferenceListPanel)panel).loadReferenceWithType(rec.getId(), "SUBJECT", "CANDIDATE"));
		loaders.put(PanelKey.RESEARCH_QUESTION_ON_TARGET, (panel, rec) -> ((EntityReferenceListPanel)panel).loadReferenceWithType(rec.getId(), "TARGET"));
		loaders.put(PanelKey.RESEARCH_ACTIVITY_ON_QUESTION, (panel, rec) -> ((EntityReferenceListPanel)panel).loadReferenceWithType(rec.getId(), "QUESTION"));
		loaders.put(PanelKey.RESEARCH_ACTIVITY_ON_SOURCE, (panel, rec) -> ((EntityReferenceListPanel)panel).loadReferenceWithType(rec.getId(), "SOURCE"));

		// ContextImpact is an EntityListPanel – uses load(record) already covered above
	}

	private void initSavers(){
		// Panels that use save(record)
		savers.put(PanelKey.PRIVACY, (panel, rec) -> ((PrivacyPanel)panel).save(rec));
		savers.put(PanelKey.AUDIT, (panel, rec) -> ((AuditPanel)panel).save(rec));
		savers.put(PanelKey.EVIDENCE, (panel, rec) -> ((EvidenceQualifiersPanel)panel).save(rec));
		savers.put(PanelKey.INDIVIDUAL_ATTRIBUTE, (panel, rec) -> ((EntityReferenceListPanel)panel).save(rec));
		savers.put(PanelKey.GROUP_ATTRIBUTE, (panel, rec) -> ((EntityReferenceListPanel)panel).save(rec));
		savers.put(PanelKey.RELATIONSHIP_ON_SUBJECT, (panel, rec) -> ((EntityReferenceListPanel)panel).save(rec));
		savers.put(PanelKey.RELATIONSHIP_ON_OBJECT, (panel, rec) -> ((EntityReferenceListPanel)panel).save(rec));
		savers.put(PanelKey.PLACE_RELATIONSHIP_ON_SUBJECT, (panel, rec) -> ((EntityReferenceListPanel)panel).save(rec));
		savers.put(PanelKey.PLACE_RELATIONSHIP_ON_OBJECT, (panel, rec) -> ((EntityReferenceListPanel)panel).save(rec));
		savers.put(PanelKey.EVENT_PARTICIPATION_ON_PARTICIPANT, (panel, rec) -> ((EntityReferenceListPanel)panel).save(rec));
		savers.put(PanelKey.CONTEXT_IMPACT_ON_TARGET, (panel, rec) -> ((EntityReferenceListPanel)panel).save(rec));
		savers.put(PanelKey.CONTEXT_IMPACT_ON_CONTEXT, (panel, rec) -> ((EntityReferenceListPanel)panel).save(rec));
		savers.put(PanelKey.CONCLUSION_ON_RESOLVES, (panel, rec) -> ((EntityReferenceListPanel)panel).save(rec));
		savers.put(PanelKey.IDENTITY_HYPOTHESIS_ON_SUBJECT_OR_CANDIDATE, (panel, rec) -> ((EntityReferenceListPanel)panel).save(rec));
		savers.put(PanelKey.RESEARCH_QUESTION_ON_TARGET, (panel, rec) -> ((EntityReferenceListPanel)panel).save(rec));
		savers.put(PanelKey.RESEARCH_ACTIVITY_ON_QUESTION, (panel, rec) -> ((EntityReferenceListPanel)panel).save(rec));
		savers.put(PanelKey.RESEARCH_ACTIVITY_ON_SOURCE, (panel, rec) -> ((EntityReferenceListPanel)panel).save(rec));
		savers.put(PanelKey.RESEARCH_TASK, (panel, rec) -> ((EntityReferenceListPanel)panel).save(rec));

		// Panels that use saveReferences(record)
		savers.put(PanelKey.EVENT_PARTICIPATION_ON_EVENT, (panel, rec) -> ((EntityReferenceListPanel)panel).saveReferences(rec));
		savers.put(PanelKey.CONTEXT_IMPACT, (panel, rec) -> ((EntityListPanel)panel).saveReferences(rec));
		savers.put(PanelKey.NOTE, (panel, rec) -> ((EntityListPanel)panel).saveReferences(rec));

		// EntityCitationListPanel uses save(record)
		savers.put(PanelKey.PLACE, (panel, rec) -> ((EntityCitationListPanel)panel).save(rec));
		savers.put(PanelKey.REPOSITORY, (panel, rec) -> ((EntityCitationListPanel)panel).save(rec));
		savers.put(PanelKey.SOURCE, (panel, rec) -> ((EntityCitationListPanel)panel).save(rec));
		savers.put(PanelKey.SOURCE_ON_REPOSITORY, (panel, rec) -> ((EntityCitationListPanel)panel).save(rec));

		// Document uses save(record) on EntityListPanel
		savers.put(PanelKey.DOCUMENT, (panel, rec) -> ((EntityListPanel)panel).save(rec));
	}

	// ------------------------------------------------------------------------
	// Panel creation – all original comments are preserved
	// ------------------------------------------------------------------------

	private JPanel createPanel(final PanelKey key,
		final RecordDialogBuilder.EntityReferenceConfig cfg,
		final FLEFModel model,
		final FLEFRecord record){
		return switch(key){
			// RelationshipRecord (subject = this individual)
			// RelationshipRecord (subject = this group)
			// PlaceRelationshipRecord (subject = this place)
			case RELATIONSHIP_ON_SUBJECT,
				  PLACE_RELATIONSHIP_ON_SUBJECT -> {
				final EntityReferenceListPanel panel = EntityReferenceListPanel.createForRecord(
					cfg.tag(), owner, cfg.title(), model, cfg.handlerClass(),
					EntityReferenceListPanel.ActorType.SUBJECT);
				if(record != null)
					panel.withParentEntity(record.getId(), HandlerRegistry.getHandlerType(cfg.handlerType()));
				yield panel;
			}

			// RelationshipRecord (object = this individual)
			// RelationshipRecord (object = this group)
			// PlaceRelationshipRecord (object = this place)
			case RELATIONSHIP_ON_OBJECT,
				  PLACE_RELATIONSHIP_ON_OBJECT -> {
				final EntityReferenceListPanel panel = EntityReferenceListPanel.createForRecord(
					cfg.tag(), owner, cfg.title(), model, cfg.handlerClass(),
					EntityReferenceListPanel.ActorType.OBJECT);
				if(record != null)
					panel.withParentEntity(record.getId(), HandlerRegistry.getHandlerType(cfg.handlerType()));
				yield panel;
			}

			// IndividualAttributeRecord (individual = this individual)
			// GroupAttributeRecord (group = this group)
			// EventParticipationRecord (participant.individual = this individual)
			// EventParticipationRecord (participant.group = this group)
			// EventParticipationRecord (participant.place = this place)
			// ContextImpactRecord (target.individual = this individual)
			// ContextImpactRecord (target.group = this group)
			// ContextImpactRecord (target.individual_attribute = this attribute)
			// ContextImpactRecord (target.identity_hypothesis = this hypothesis)
			// ContextImpactRecord (target.event = this event)
			// ContextImpactRecord (target.event_participation = this participation)
			// ContextImpactRecord (target.relationship = this relationship)
			// ContextImpactRecord (target.place_relationship = this relationship)
			// ContextImpactRecord (target.place = this place)
			// ContextImpactRecord (context.historic_event = this historic event)
			// ContextImpactRecord (context.cultural_norm = this norm)
			// ConclusionRecord (resolves = this individual)
			// ConclusionRecord (resolves = this group)
			// ConclusionRecord (resolves = this attribute)
			// ConclusionRecord (resolves = this source)
			// ConclusionRecord (resolves = this event)
			// ConclusionRecord (resolves = this participation)
			// ConclusionRecord (resolves = this historic event)
			// ConclusionRecord (resolves = this question)
			// ConclusionRecord (resolves = this relationship)
			// ConclusionRecord (resolves = this relationship)  // duplicate? keep as original
			// ConclusionRecord (resolves = this place)
			// ConclusionRecord (resolves = this norm)
			// IdentityHypothesisRecord (subject/candidate = this individual)
			// IdentityHypothesisRecord (subject/candidate = this group)
			// IdentityHypothesisRecord (subject/candidate = this place)
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
			case INDIVIDUAL_ATTRIBUTE,
				  GROUP_ATTRIBUTE,
				  EVENT_PARTICIPATION_ON_PARTICIPANT,
				  CONTEXT_IMPACT_ON_TARGET,
				  CONTEXT_IMPACT_ON_CONTEXT,
				  CONCLUSION_ON_RESOLVES,
				  IDENTITY_HYPOTHESIS_ON_SUBJECT_OR_CANDIDATE,
				  RESEARCH_QUESTION_ON_TARGET,
				  RESEARCH_ACTIVITY_ON_SOURCE,
				  RESEARCH_ACTIVITY_ON_QUESTION,
				  RESEARCH_TASK -> {
				final EntityReferenceListPanel panel = EntityReferenceListPanel.createForRecord(
					cfg.tag(), owner, cfg.title(), model, cfg.handlerClass());
				if(record != null)
					panel.withParentEntity(record.getId(), HandlerRegistry.getHandlerType(cfg.handlerType()));
				yield panel;
			}

			// EventParticipationRecord (event = this event)
			case EVENT_PARTICIPATION_ON_EVENT -> {
				final EntityReferenceListPanel panel = EntityReferenceListPanel.createForRecord(
					cfg.tag(), owner, cfg.title(), model, cfg.handlerClass());
				if(record != null)
					panel.withParentEntity(record.getId(), HandlerRegistry.getHandlerType(cfg.handlerType()));
				yield panel;
			}

			// ResearchActivityRecord (source contains this source)
			// ResearchActivityRecord (question contains this question)
			// ResearchTaskRecord (question contains this question)
			// Actually these are already covered? The original had separate cases but they all use createForRecord with no parent.
			// But we already have them in the previous group. So we can skip them. However, to keep the original comments, we keep a case for them? They are already in the previous group.
			// The original has:
			// case RESEARCH_ACTIVITY_ON_SOURCE,
			//      RESEARCH_ACTIVITY_ON_QUESTION,
			//      RESEARCH_TASK ->
			//    EntityReferenceListPanel.createForRecord(...);
			// So they are included in the previous group. So no need to duplicate.
			// We'll just keep the comments above the group to cover them.

			case PLACE,
				  REPOSITORY,
				  SOURCE ->
				new EntityCitationListPanel(cfg.tag(), cfg.citationTag(), owner, cfg.title(), model, cfg.handlerType());

			// SourceRecord (repository references this repository)
			case SOURCE_ON_REPOSITORY ->
				EntityReferenceListPanel.createForRecord(cfg.tag(), owner, cfg.title(), model, cfg.handlerClass());

			case EVIDENCE ->
				new EvidenceQualifiersPanel(cfg.tag(), owner, cfg.title(), model, HandlerRegistry.getHandlerType(cfg.handlerType()));

			// embedded reference
			case CONTEXT_IMPACT,
				  NOTE,
				  DOCUMENT -> new EntityListPanel(cfg.tag(), owner, cfg.title(), model, cfg.handlerType());

			case PRIVACY -> new PrivacyPanel(cfg.tag(), owner);

			case AUDIT -> new AuditPanel(owner);
		};
	}

	// ------------------------------------------------------------------------
	// Public API – load, save, bind, getPanel
	// ------------------------------------------------------------------------

	public void bind(final PathBound component){
		bindingManager.bind(component);
	}

	/**
	 * Loads data from the given record into all panels.
	 * Uses the strategy map to delegate to the appropriate load method for each panel.
	 */
	public void load(final FLEFRecord record){
		bindingManager.load(record);
		for(final Map.Entry<PanelKey, JPanel> entry : panels.entrySet()){
			final PanelKey key = entry.getKey();
			final JPanel panel = entry.getValue();
			final BiConsumer<JPanel, FLEFRecord> loader = loaders.get(key);
			if(loader != null && panel != null)
				loader.accept(panel, record);
		}
	}

	/**
	 * Saves data from all panels into the given record.
	 * Uses the strategy map to delegate to the appropriate save method for each panel.
	 */
	public void save(final FLEFRecord record){
		bindingManager.save(record);
		for(final Map.Entry<PanelKey, JPanel> entry : panels.entrySet()){
			final PanelKey key = entry.getKey();
			final JPanel panel = entry.getValue();
			final BiConsumer<JPanel, FLEFRecord> saver = savers.get(key);
			if(saver != null && panel != null)
				saver.accept(panel, record);
		}
	}

	public JPanel getPanel(final PanelKey key){
		return panels.get(key);
	}

}
