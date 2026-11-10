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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.consanguinity.KinshipCalculator;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.CulturalNormReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.HistoricEventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyPolicy;
import io.github.mtrevisan.familylegacy.privacy.PrivacyResolver;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLabels;
import io.github.mtrevisan.familylegacy.reports.index.RelationIndex;
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import io.github.mtrevisan.familylegacy.sources.SourceDerivationTree;
import org.apache.commons.lang3.StringUtils;

import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * Immutable shared state for a single report generation process.
 * Holds references to the underlying model, root entity, report configuration,
 * specialized indices, and internationalization label providers.
 */
public final class ReportContext{

	private static final String TAG_TITLE = SourceReader.TAG_TITLE;
	private static final String TAG_TYPE = HistoricEventReader.TAG_TYPE;


	/** Categorizes the type of the root FLEF record being reported. */
	public enum RootKind{
		INDIVIDUAL,
		GROUP,
		EVENT,
		SOURCE,
		PLACE,
		REPOSITORY,
		DOCUMENT,
		RESEARCH_QUESTION,
		RESEARCH_ACTIVITY,
		RESEARCH_TASK,
		CONCLUSION,
		IDENTITY_HYPOTHESIS,
		CULTURAL_NORM,
		HISTORIC_EVENT,
		OTHER
	}


	final FLEFModel model;
	final FLEFRecord root;
	final ReportConfig config;
	final ReportLabels labels;
	final RelationIndex index;

	private final RootKind rootKind;
	/** The records the sections that collect (sources, media, places) read from: the root, or the persons of a multi-person report. */
	private final List<FLEFRecord> subjects;
	/** Who owns each source citation: found once for the contexts of a report, that share it. */
	private final SharedLookups shared;
	private Set<String> relatedRecordIds;
	private PrivacyResolver privacy;

	private KinshipCalculator kinship;
	private CitationRenderer citations;
	private Function<String, String> contextLabelResolver;


	/**
	 * Factory method to construct an immutable {@link ReportContext}.
	 *
	 * @param model  The domain model containing FLEF records.
	 * @param root   The root record to generate the report for.
	 * @param config The report configuration options.
	 * @return A fully initialized report context.
	 */
	public static ReportContext build(final FLEFModel model, final FLEFRecord root, final ReportConfig config){
		Objects.requireNonNull(model, "model");
		Objects.requireNonNull(root, "root");
		Objects.requireNonNull(config, "config");

		return new ReportContext(model, root, config, null, List.of(root));
	}

	/**
	 * Builds a context that reuses a pre-built {@link RelationIndex}. Use this
	 * overload when generating reports for many persons of the same model, so
	 * the index is built once and shared.
	 */
	static ReportContext build(final FLEFModel model, final FLEFRecord root, final ReportConfig config,
			final RelationIndex sharedIndex){
		return new ReportContext(model, root, config, sharedIndex, List.of(root));
	}

	/**
	 * Builds a context for the sections that gather what is cited by several persons at once (the sources, media and places of an
	 * ancestor or descendant report).
	 *
	 * @param subjects	The persons whose records are collected; the first one is the root.
	 */
	static ReportContext buildForSubjects(final FLEFModel model, final ReportConfig config, final RelationIndex sharedIndex,
			final List<FLEFRecord> subjects){
		return new ReportContext(model, subjects.getFirst(), config, sharedIndex, List.copyOf(subjects));
	}


	private ReportContext(final FLEFModel model, final FLEFRecord root, final ReportConfig config,
			final RelationIndex sharedIndex, final List<FLEFRecord> subjects){
		this(model, root, config, sharedIndex, subjects, new SharedLookups());
	}

	private ReportContext(final FLEFModel model, final FLEFRecord root, final ReportConfig config,
			final RelationIndex sharedIndex, final List<FLEFRecord> subjects, final SharedLookups shared){
		this.shared = shared;
		this.subjects = subjects;
		this.model = model;
		this.root = root;
		this.config = config;
		this.labels = new ReportLabels(config.language());
		this.index = (sharedIndex != null? sharedIndex: RelationIndex.build(model, this::isVisible));
		this.rootKind = detectRootKind(root);
	}

	/**
	 * @param newRoot	Another record of the same model.
	 * @return	A context of the same report (same options and index) about another record: it is how a report that has a section for each
	 * 	record (the Full Bibliography) builds them.
	 */
	ReportContext withRoot(final FLEFRecord newRoot){
		return new ReportContext(model, newRoot, config, index, List.of(newRoot), shared);
	}

	/**
	 * @param citation	A source citation of the model.
	 * @return	The record that holds the citation, or {@code null}.
	 */
	FLEFRecord ownerOfCitation(final FLEFRecord citation){
		return shared.of(model).get(citation);
	}

	/**
	 * What is found once for the contexts of a report, built when first needed: the record that holds each source citation of a model (a
	 * citation is an embedded structure, found by identity) and the derivations of its sources.
	 */
	private static final class SharedLookups{

		private Map<FLEFRecord, FLEFRecord> owners;
		private SourceDerivationTree derivations;


		synchronized SourceDerivationTree derivationsOf(final FLEFModel model, final Predicate<FLEFRecord> visible){
			if(derivations == null)
				derivations = new SourceDerivationTree(model.getRecordsByType(SourceReader.TYPE).stream()
					.filter(visible)
					.toList());
			return derivations;
		}


		synchronized Map<FLEFRecord, FLEFRecord> of(final FLEFModel model){
			if(owners == null){
				owners = new IdentityHashMap<>();
				for(final FLEFRecord rec : model.getRecords())
					//the citations of the parts of a record (a name, a note, a place, ...) belong to the record
					for(final FLEFRecord citation : ReportFormatters.collectDescendantsWithTag(rec, SourceCitationReader.TAG_SOURCE))
						owners.putIfAbsent(citation, rec);
			}
			return owners;
		}

	}

	/** @return	The records that the sections collecting sources, media and places read from: the root, unless it is a multi-person report. */
	public List<FLEFRecord> subjects(){
		return subjects;
	}

	/** @return	What kind of record a report about the given one is. */
	public static RootKind rootKindOf(final FLEFRecord root){
		return detectRootKind(root);
	}


	private static RootKind detectRootKind(final FLEFRecord root){
		final String tag = (root.getTag() != null? root.getTag().toUpperCase(Locale.ROOT): StringUtils.EMPTY);
		return switch(tag){
			case GroupReader.TYPE -> RootKind.GROUP;
			case EventReader.TYPE -> RootKind.EVENT;
			case SourceReader.TYPE -> RootKind.SOURCE;
			case PlaceReader.TYPE -> RootKind.PLACE;
			case RepositoryReader.TYPE -> RootKind.REPOSITORY;
			case MediaReader.TYPE -> RootKind.DOCUMENT;
			case ResearchQuestionReader.TYPE -> RootKind.RESEARCH_QUESTION;
			case ResearchActivityReader.TYPE -> RootKind.RESEARCH_ACTIVITY;
			case ResearchTaskReader.TYPE -> RootKind.RESEARCH_TASK;
			case ConclusionReader.TYPE -> RootKind.CONCLUSION;
			case IdentityHypothesisReader.TYPE -> RootKind.IDENTITY_HYPOTHESIS;
			case CulturalNormReader.TYPE -> RootKind.CULTURAL_NORM;
			case HistoricEventReader.TYPE -> RootKind.HISTORIC_EVENT;
			case IndividualReader.TYPE -> RootKind.INDIVIDUAL;
			default -> RootKind.OTHER;
		};
	}

	public RootKind rootKind(){
		return rootKind;
	}

	public boolean isGroupRoot(){
		return (rootKind == RootKind.GROUP);
	}

	public boolean isEventRoot(){
		return (rootKind == RootKind.EVENT);
	}

	public boolean isSourceRoot(){
		return (rootKind == RootKind.SOURCE);
	}

	public boolean isPlaceRoot(){
		return (rootKind == RootKind.PLACE);
	}

	public boolean isRepositoryRoot(){
		return (rootKind == RootKind.REPOSITORY);
	}

	public boolean isDocumentRoot(){
		return rootKind == RootKind.DOCUMENT;
	}

	public boolean isResearchQuestionRoot(){
		return rootKind == RootKind.RESEARCH_QUESTION;
	}

	public boolean isConclusionRoot(){
		return rootKind == RootKind.CONCLUSION;
	}

	public boolean isResearchActivityRoot(){
		return rootKind == RootKind.RESEARCH_ACTIVITY;
	}

	public boolean isResearchTaskRoot(){
		return rootKind == RootKind.RESEARCH_TASK;
	}

	public boolean isIdentityHypothesisRoot(){
		return rootKind == RootKind.IDENTITY_HYPOTHESIS;
	}

	public boolean isCulturalNormRoot(){
		return rootKind == RootKind.CULTURAL_NORM;
	}

	public boolean isHistoricEventRoot(){
		return rootKind == RootKind.HISTORIC_EVENT;
	}


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
	 *       nodes are hidden, {@code restricted} and {@code public} are shown;</li>
	 *   <li>{@link PrivacyPolicy#HIDE_RESTRICTED_AND_CONFIDENTIAL}: both
	 *       {@code confidential} and {@code restricted} are hidden.</li>
	 * </ul>
	 */
	public boolean isVisible(final FLEFRecord record){
		if(record == null)
			return false;

		final PrivacyPolicy policy = config.privacyPolicy();
		if(policy == PrivacyPolicy.SHOW_ALL)
			return true;

		//the level is the effective one: declared (unless expired), inherited, living person, national id
		return !policy.hides(privacy().effectiveLevel(record));
	}

	private PrivacyResolver privacy(){
		if(privacy == null)
			privacy = new PrivacyResolver(model);
		return privacy;
	}

	public FLEFRecord visible(final FLEFRecord rec){
		return (isVisible(rec)? rec: null);
	}

	public List<FLEFRecord> visible(final List<FLEFRecord> recs){
		if(recs == null || recs.isEmpty())
			return List.of();
		if(config.privacyPolicy() == PrivacyPolicy.SHOW_ALL)
			return recs;
		return recs.stream().filter(this::isVisible).toList();
	}

	public List<FLEFRecord> visibleRecordsByType(final String type){
		return visible(model.getRecordsByType(type));
	}

	public List<FLEFRecord> visibleChildren(final FLEFRecord parent, final String tag){
		return visible(FLEFRecordHelper.findChildren(parent, tag));
	}


	/* ======================================================================
	 *                          Related records
	 * ====================================================================== */

	public Set<String> relatedRecordIds(){
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
		for(final FLEFRecord src : index.sourcesOfDocument(root))
			ids.add(src.getId());
		for(final FLEFRecord cit : index.citationsOfDocument(root))
			if(cit.getId() != null) ids.add(cit.getId());
	}

	private void computeForResearchQuestion(final Set<String> ids){
		for(final FLEFRecord a : index.activitiesForQuestion(root)) ids.add(a.getId());
		for(final FLEFRecord t : index.tasksForQuestion(root)) ids.add(t.getId());
		for(final FLEFRecord c : index.conclusionsForQuestion(root)) ids.add(c.getId());
		for(final FLEFRecord target : FLEFRecordHelper.findChildren(root, ResearchQuestionReader.TAG_TARGET)){
			final FLEFRecord ref = target.getTheOnlyChild();
			if(ref != null && ref.getValue() != null && !ref.getValue().isBlank())
				ids.add(ref.getValue());
		}
	}

	private void computeForConclusion(final Set<String> ids){
		for(final FLEFRecord r : FLEFRecordHelper.findChildren(root, ConclusionReader.TAG_RESOLVES)){
			final FLEFRecord ref = r.getTheOnlyChild();
			if(ref != null && ref.getValue() != null && !ref.getValue().isBlank())
				ids.add(ref.getValue());
		}
		final FLEFRecord preferred = FLEFRecordHelper.findChild(root, ConclusionReader.TAG_PREFERRED);
		if(preferred != null){
			final FLEFRecord ref = preferred.getTheOnlyChild();
			if(ref != null && ref.getValue() != null && !ref.getValue().isBlank())
				ids.add(ref.getValue());
		}
		for(final FLEFRecord q : FLEFRecordHelper.findChildren(root, ConclusionReader.TAG_RESEARCH))
			if(q.getValue() != null) ids.add(q.getValue());
	}

	private void addIdentityHypotheses(final Set<String> ids){
		for(final FLEFRecord h : visibleRecordsByType(IdentityHypothesisReader.TYPE)){
			for(final FLEFRecord cand : FLEFRecordHelper.findChildren(h, IdentityHypothesisReader.TAG_IDENTITY)){
				final FLEFRecord ref = cand.getTheOnlyChild();
				if(ref != null && Objects.equals(root.getId(), ref.getValue()))
					ids.add(h.getId());
			}
		}
	}


	/* ======================================================================
	 *                          Shared resources
	 * ====================================================================== */

	public KinshipCalculator kinship(){
		if(kinship == null) kinship = KinshipCalculator.forModel(model).withVocabulary(labels.kinship());
		return kinship;
	}

	/** @return	The derivations of the sources the report shows. */
	public SourceDerivationTree derivations(){
		return shared.derivationsOf(model, this::isVisible);
	}

	public CitationRenderer citations(){
		if(citations == null) citations = new CitationRenderer(this);
		return citations;
	}

	public Function<String, String> contextLabelResolver(){
		if(contextLabelResolver == null) contextLabelResolver = this::resolveContextLabel;
		return contextLabelResolver;
	}

	public String resolveContextLabel(final String id){
		if(id == null)
			return null;

		final FLEFRecord rec = model.getRecordById(id);
		if(rec == null)
			return id;

		final String title = FLEFRecordHelper.getChildValue(rec, TAG_TITLE);
		if(title != null && !title.isBlank())
			return title.trim();

		if(CulturalNormReader.isOfType(rec)){
			final String rt = FLEFRecordHelper.getChildValue(rec, CulturalNormReader.TAG_TYPE);
			if(rt != null && !rt.isBlank())
				return rt.replace('_', ' ');
		}
		else{
			final String t = FLEFRecordHelper.getChildValue(rec, TAG_TYPE);
			if(t != null && !t.isBlank())
				return t.replace('_', ' ');
		}
		return id;
	}


	/* ======================================================================
	 *                          Lookups
	 * ====================================================================== */

	public String displayText(final FLEFRecord rec){
		return RecordDisplay.describe(rec, model);
	}

	public String sourceTitle(final String sourceId){
		final FLEFRecord src = model.getRecordById(sourceId);
		if(src == null)
			return sourceId;
		return SourceReader.displayText(src);
	}

	public String formatInt(final int v){
		return String.format(labels.language().locale(), "%,d", v);
	}

}
