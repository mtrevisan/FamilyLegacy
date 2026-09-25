package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.kinship.KinshipCalculator;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;


/**
 * Immutable shared state for a single report generation.
 */
final class ReportContext{

	private static final String TAG_PRIVACY_LEVEL = "privacy.level";
	private static final String PRIVACY_PUBLIC = "public";
	private static final String PRIVACY_RESTRICTED = "restricted";
	private static final String PRIVACY_CONFIDENTIAL = "confidential";

	private static final String TAG_TITLE = "title";
	private static final String TAG_RULE_TYPE = "rule_type";
	private static final String TAG_TYPE = "type";
	private static final String TAG_EXPIRES = "privacy.expires";

	private static final String TYPE_CULTURAL_NORM = "cultural_norm";


	enum RootKind{
		INDIVIDUAL, GROUP, EVENT, SOURCE, PLACE, REPOSITORY, DOCUMENT,
		RESEARCH_QUESTION, RESEARCH_ACTIVITY, RESEARCH_TASK,
		CONCLUSION, IDENTITY_HYPOTHESIS, CULTURAL_NORM, HISTORIC_EVENT, OTHER
	}


	final FLEFModel model;
	final FLEFRecord root;
	final ReportConfig config;
	final ReportLabels labels;
	final RelationIndex index;

	private final RootKind rootKind;
	private Set<String> relatedRecordIds;

	private KinshipCalculator kinship;
	private CitationRenderer citations;
	private Function<String, String> contextLabelResolver;


	private ReportContext(final FLEFModel model, final FLEFRecord root,
		final ReportConfig config){
		this.model = model;
		this.root = root;
		this.config = config;
		this.labels = new ReportLabels(config.language());
		this.index = RelationIndex.build(model, this::isVisible);
		this.rootKind = detectRootKind(root);
	}


	static ReportContext build(final FLEFModel model, final FLEFRecord root,
		final ReportConfig config){
		Objects.requireNonNull(model, "model");
		Objects.requireNonNull(root, "root");
		Objects.requireNonNull(config, "config");
		return new ReportContext(model, root, config);
	}


	private static RootKind detectRootKind(final FLEFRecord root){
		final String tag = (root.getTag() != null? root.getTag().toLowerCase(Locale.ROOT): "");
		return switch(tag){
			case "group"               -> RootKind.GROUP;
			case "event"               -> RootKind.EVENT;
			case "source"              -> RootKind.SOURCE;
			case "place"               -> RootKind.PLACE;
			case "repository"          -> RootKind.REPOSITORY;
			case "document"            -> RootKind.DOCUMENT;
			case "research_question"   -> RootKind.RESEARCH_QUESTION;
			case "research_activity"   -> RootKind.RESEARCH_ACTIVITY;
			case "research_task"       -> RootKind.RESEARCH_TASK;
			case "conclusion"          -> RootKind.CONCLUSION;
			case "identity_hypothesis" -> RootKind.IDENTITY_HYPOTHESIS;
			case "cultural_norm"       -> RootKind.CULTURAL_NORM;
			case "historic_event"      -> RootKind.HISTORIC_EVENT;
			case "individual"          -> RootKind.INDIVIDUAL;
			default                    -> RootKind.OTHER;
		};
	}

	RootKind rootKind(){
		return rootKind;
	}

	boolean isIndividualRoot(){
		return rootKind == RootKind.INDIVIDUAL;
	}

	boolean isGroupRoot(){
		return rootKind == RootKind.GROUP;
	}

	boolean isEventRoot(){
		return rootKind == RootKind.EVENT;
	}

	boolean isSourceRoot(){
		return rootKind == RootKind.SOURCE;
	}

	boolean isPlaceRoot(){
		return rootKind == RootKind.PLACE;
	}

	boolean isRepositoryRoot(){
		return rootKind == RootKind.REPOSITORY;
	}

	boolean isDocumentRoot(){
		return rootKind == RootKind.DOCUMENT;
	}

	boolean isResearchQuestionRoot(){
		return rootKind == RootKind.RESEARCH_QUESTION;
	}

	boolean isConclusionRoot(){
		return rootKind == RootKind.CONCLUSION;
	}

	boolean isResearchActivityRoot(){ return rootKind == RootKind.RESEARCH_ACTIVITY; }
	boolean isResearchTaskRoot(){ return rootKind == RootKind.RESEARCH_TASK; }
	boolean isIdentityHypothesisRoot(){ return rootKind == RootKind.IDENTITY_HYPOTHESIS; }
	boolean isCulturalNormRoot(){ return rootKind == RootKind.CULTURAL_NORM; }
	boolean isHistoricEventRoot(){ return rootKind == RootKind.HISTORIC_EVENT; }


	/* ======================================================================
	 *                          Privacy
	 * ====================================================================== */

	/**
	 * Three-level privacy decision, applied uniformly to every record,
	 * including embedded nodes such as {@code source} citations that carry
	 * their own {@code privacy} block.
	 *
	 * <ul>
	 *   <li>{@link PrivacyPolicy#SHOW_ALL}: every node is visible;</li>
	 *   <li>{@link PrivacyPolicy#HIDE_CONFIDENTIAL}: {@code confidential}
	 *       nodes are hidden, {@code restricted} and {@code public} are
	 *       shown;</li>
	 *   <li>{@link PrivacyPolicy#HIDE_RESTRICTED_AND_CONFIDENTIAL}: both
	 *       {@code confidential} and {@code restricted} are hidden.</li>
	 * </ul>
	 */
	boolean isVisible(final FLEFRecord rec){
		if(rec == null) return false;

		final PrivacyPolicy policy = config.privacyPolicy();
		if(policy == PrivacyPolicy.SHOW_ALL) return true;

		final String level = readPrivacyLevel(rec);
		if(PRIVACY_CONFIDENTIAL.equals(level)) return false;
		if(policy == PrivacyPolicy.HIDE_RESTRICTED_AND_CONFIDENTIAL
			&& PRIVACY_RESTRICTED.equals(level))
			return false;

		// Even when the level allows display, an expired restriction means
		// the data is now public.
		return true;
	}

	/**
	 * Whether the privacy restriction has expired as of {@code today}.
	 * A record with an expired restriction is treated as public even when
	 * the policy would otherwise hide it. Used by callers that want to
	 * surface the expiry in the report.
	 */
	boolean isPrivacyExpired(final FLEFRecord rec){
		final String expires = FLEFRecordHelper.getChildValue(rec, TAG_EXPIRES);
		if(expires == null || expires.isBlank())
			return false;
		try{
			final LocalDate expiry = LocalDate.parse(expires.trim());
			return !expiry.isAfter(LocalDate.now());
		}
		catch(final Exception ignored){
			return false;
		}
	}

	String readPrivacyLevel(final FLEFRecord rec){
		final String level = FLEFRecordHelper.getChildValue(rec, TAG_PRIVACY_LEVEL);
		if(level == null || level.isBlank()) return PRIVACY_PUBLIC;
		final String norm = level.trim().toLowerCase(Locale.ROOT);
		return switch(norm){
			case PRIVACY_RESTRICTED, PRIVACY_CONFIDENTIAL -> norm;
			default -> PRIVACY_PUBLIC;
		};
	}

	boolean hasPrivacyRestriction(final FLEFRecord rec){
		return !PRIVACY_PUBLIC.equals(readPrivacyLevel(rec));
	}

	FLEFRecord visible(final FLEFRecord rec){
		return (isVisible(rec)? rec: null);
	}

	List<FLEFRecord> visible(final List<FLEFRecord> recs){
		if(recs == null || recs.isEmpty()) return List.of();
		if(config.privacyPolicy() == PrivacyPolicy.SHOW_ALL) return recs;
		return recs.stream().filter(this::isVisible).toList();
	}

	List<FLEFRecord> visibleRecordsByType(final String type){
		return visible(model.getRecordsByType(type));
	}

	List<FLEFRecord> visibleChildren(final FLEFRecord parent, final String tag){
		return visible(FLEFRecordHelper.findChildren(parent, tag));
	}


	/* ======================================================================
	 *                          Related records
	 * ====================================================================== */

	Set<String> relatedRecordIds(){
		if(relatedRecordIds == null)
			relatedRecordIds = computeRelatedRecordIds();
		return relatedRecordIds;
	}

	private Set<String> computeRelatedRecordIds(){
		final Set<String> ids = new LinkedHashSet<>();
		ids.add(root.getId());

		switch(rootKind){
			case INDIVIDUAL -> computeForIndividual(ids);
			case GROUP -> computeForGroup(ids);
			case EVENT -> computeForEvent(ids);
			case SOURCE -> computeForSource(ids);
			case PLACE -> computeForPlace(ids);
			case REPOSITORY -> computeForRepository(ids);
			case DOCUMENT -> computeForDocument(ids);
			case RESEARCH_QUESTION -> computeForResearchQuestion(ids);
			case CONCLUSION -> computeForConclusion(ids);
			case OTHER -> { /* only the root itself */ }
		}
		return ids;
	}

	private void computeForIndividual(final Set<String> ids){
		for(final FLEFRecord e : index.eventsOf(root)) ids.add(e.getId());
		for(final FLEFRecord a : index.attributesOf(root)) ids.add(a.getId());
		for(final FLEFRecord r : index.relationshipsOfSubject(root)) ids.add(r.getId());
		for(final FLEFRecord ep : index.participationsOfParticipant(root)) ids.add(ep.getId());
		for(final FLEFRecord m : index.groupMembershipsOf(root)) ids.add(m.getId());
		addIdentityHypotheses(ids);
	}

	private void computeForGroup(final Set<String> ids){
		for(final FLEFRecord a : index.attributesOfGroup(root)) ids.add(a.getId());
		for(final FLEFRecord e : index.eventsOfGroup(root)) ids.add(e.getId());
		for(final FLEFRecord m : index.membersOf(root)) ids.add(m.getId());
		for(final FLEFRecord r : index.parentGroupsOf(root)) ids.add(r.getId());
		for(final FLEFRecord r : index.childGroupsOf(root)) ids.add(r.getId());
		for(final FLEFRecord ep : index.participationsOfParticipant(root)) ids.add(ep.getId());
		addIdentityHypotheses(ids);
	}

	private void computeForEvent(final Set<String> ids){
		for(final FLEFRecord ep : index.participationsOfEvent(root)) ids.add(ep.getId());
	}

	private void computeForSource(final Set<String> ids){
		for(final FLEFRecord c : index.citationsOfSource(root))
			if(c.getId() != null) ids.add(c.getId());
		for(final FLEFRecord d : index.documentsOfSource(root))
			if(d.getId() != null) ids.add(d.getId());
		for(final FLEFRecord r : index.repositoriesOfSource(root))
			if(r.getId() != null) ids.add(r.getId());
	}

	private void computeForPlace(final Set<String> ids){
		for(final FLEFRecord e : index.eventsAtPlace(root)) ids.add(e.getId());
		for(final FLEFRecord e : index.eventsByPlace(root)) ids.add(e.getId());
		for(final FLEFRecord a : index.attributesAtPlace(root)) ids.add(a.getId());
		for(final FLEFRecord r : index.placeRelationshipsAsSubject(root)) ids.add(r.getId());
		for(final FLEFRecord r : index.placeRelationshipsAsTarget(root)) ids.add(r.getId());
		addIdentityHypotheses(ids);
	}

	private void computeForRepository(final Set<String> ids){
		for(final FLEFRecord s : index.sourcesOfRepository(root)) ids.add(s.getId());
		for(final FLEFRecord rc : index.repositoryCitationsOf(root))
			if(rc.getId() != null) ids.add(rc.getId());
	}

	private void computeForDocument(final Set<String> ids){
		// All citations that reference this document.
		for(final FLEFRecord src : index.sourcesOfDocument(root))
			ids.add(src.getId());
		for(final FLEFRecord cit : index.citationsOfDocument(root))
			if(cit.getId() != null) ids.add(cit.getId());
	}

	private void computeForResearchQuestion(final Set<String> ids){
		for(final FLEFRecord a : index.activitiesForQuestion(root)) ids.add(a.getId());
		for(final FLEFRecord t : index.tasksForQuestion(root)) ids.add(t.getId());
		for(final FLEFRecord c : index.conclusionsForQuestion(root)) ids.add(c.getId());
		for(final FLEFRecord target : FLEFRecordHelper.findChildren(root, "target")){
			final FLEFRecord ref = target.getTheOnlyChild();
			if(ref != null && ref.getValue() != null && !ref.getValue().isBlank())
				ids.add(ref.getValue());
		}
	}

	private void computeForConclusion(final Set<String> ids){
		for(final FLEFRecord r : FLEFRecordHelper.findChildren(root, "resolves")){
			final FLEFRecord ref = r.getTheOnlyChild();
			if(ref != null && ref.getValue() != null && !ref.getValue().isBlank())
				ids.add(ref.getValue());
		}
		final FLEFRecord preferred = FLEFRecordHelper.findChild(root, "preferred");
		if(preferred != null){
			final FLEFRecord ref = preferred.getTheOnlyChild();
			if(ref != null && ref.getValue() != null && !ref.getValue().isBlank())
				ids.add(ref.getValue());
		}
		for(final FLEFRecord q : FLEFRecordHelper.findChildren(root, "research"))
			if(q.getValue() != null) ids.add(q.getValue());
	}

	private void addIdentityHypotheses(final Set<String> ids){
		for(final FLEFRecord h : visibleRecordsByType("identity_hypothesis")){
			for(final FLEFRecord cand : FLEFRecordHelper.findChildren(h, "identity")){
				final FLEFRecord ref = cand.getTheOnlyChild();
				if(ref != null && Objects.equals(root.getId(), ref.getValue()))
					ids.add(h.getId());
			}
		}
	}


	/* ======================================================================
	 *                          Shared resources
	 * ====================================================================== */

	KinshipCalculator kinship(){
		if(kinship == null) kinship = KinshipCalculator.forModel(model);
		return kinship;
	}

	CitationRenderer citations(){
		if(citations == null) citations = new CitationRenderer(this);
		return citations;
	}

	Function<String, String> contextLabelResolver(){
		if(contextLabelResolver == null) contextLabelResolver = this::resolveContextLabel;
		return contextLabelResolver;
	}

	String resolveContextLabel(final String id){
		if(id == null) return null;
		final FLEFRecord rec = model.getRecordById(id);
		if(rec == null) return id;
		final String title = FLEFRecordHelper.getChildValue(rec, TAG_TITLE);
		if(title != null && !title.isBlank()) return title.trim();
		final String tag = rec.getTag();
		if(TYPE_CULTURAL_NORM.equalsIgnoreCase(tag)){
			final String rt = FLEFRecordHelper.getChildValue(rec, TAG_RULE_TYPE);
			if(rt != null && !rt.isBlank()) return rt.replace('_', ' ');
		}
		else{
			final String t = FLEFRecordHelper.getChildValue(rec, TAG_TYPE);
			if(t != null && !t.isBlank()) return t.replace('_', ' ');
		}
		return id;
	}


	/* ======================================================================
	 *                          Lookups
	 * ====================================================================== */

	String displayText(final FLEFRecord rec){
		final String tag = rec.getTag();
		final RecordTypeHandler<?> h = (tag != null? HandlerRegistry.getHandler(tag): null);
		return (h != null? h.getDisplayText(rec, model): rec.getId());
	}

	String sourceTitle(final String sourceId){
		final FLEFRecord src = model.getRecordById(sourceId);
		if(src == null) return sourceId;
		return SourceHandler.getInstance().getDisplayText(src, model);
	}

	String formatInt(final int v){
		return String.format(labels.language().locale(), "%,d", v);
	}

}
