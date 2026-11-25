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
package io.github.mtrevisan.familylegacy.research;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionResolution;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.research.InferenceInspection.Finding;
import io.github.mtrevisan.familylegacy.research.InferenceInspection.Inspection;
import io.github.mtrevisan.familylegacy.sources.EvidenceTrace;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * The Genealogical Proof Standard as a workflow: a research question, the evidence collected for it, the conflicts among that evidence,
 * the conclusion that resolves them and the proof statement that argues it. For a research question (or for a conclusion, with the
 * question it answers) it tells where the work stands and, above all, <em>what is still missing before the conclusion can be considered
 * reasonably proven</em>.
 * <p>
 * The five elements of the standard are checked against what the file holds: reasonably exhaustive research (activities, tasks, the
 * number and kind of the sources), complete citations, analysis and correlation (the evidence is assessed), the resolution of conflicting
 * evidence (the evidence trace of every assertion involved, the conclusions) and a soundly reasoned conclusion (proof status, preferred
 * assertion, narrative). Every missing thing is a {@link Gap} that says what it is about and how much it matters ({@link Severity}); the
 * gaps are also grouped by {@link Stage} of the workflow.
 * <p>
 * It is a reading of the records and never changes them, and it never decides anything: it says what the file does not yet show, the
 * judgement stays with the researcher. See {@link GpsProofStatement} for the proof statement that is written from the same data. The
 * result is data with no text of the user interface (see {@link Kind}).
 */
public final class GpsWorkflow{

	/** The stages of the workflow, in order. */
	public enum Stage{
		/** The question is asked, and it says what it is about. */
		QUESTION,
		/** The evidence is collected, cited and assessed. */
		EVIDENCE,
		/** The conflicts among the evidence are found and resolved. */
		CONFLICTS,
		/** The conclusion is made, with its proof status and its reasoning. */
		CONCLUSION,
		/** The proof statement can be written. */
		PROOF
	}

	/** The five elements of the Genealogical Proof Standard. */
	public enum Element{
		EXHAUSTIVE_RESEARCH,
		CITATIONS,
		ANALYSIS,
		CONFLICT_RESOLUTION,
		SOUND_CONCLUSION
	}

	/** How much a gap matters. */
	public enum Severity{
		/** The conclusion cannot be considered reasonably proven while it stays. */
		REQUIRED,
		/** It makes the proof stronger, but its absence does not keep the conclusion from being reasonably proven. */
		RECOMMENDED
	}

	/** What the stage looks like. */
	public enum StageState{
		/** Nothing is missing. */
		DONE,
		/** Nothing required is missing, there are only recommendations left. */
		DONE_WITH_REMARKS,
		/** Something required is missing. */
		OPEN
	}

	/** How far the proof is. */
	public enum Readiness{
		/** There is no question, evidence or conclusion to speak of. */
		NOT_STARTED,
		/** Something required is missing. */
		IN_PROGRESS,
		/** Nothing required is missing: the conclusion can be considered reasonably proven (there may be recommendations left). */
		REASONABLY_PROVEN,
		/** Nothing is missing. */
		COMPLETE
	}

	/** What can be missing. */
	public enum Kind{
		/** The conclusion does not say which research question it answers. */
		NO_QUESTION(Stage.QUESTION, Element.EXHAUSTIVE_RESEARCH, Severity.REQUIRED),
		/** The question does not say what it is about. */
		NO_TARGET(Stage.QUESTION, Element.EXHAUSTIVE_RESEARCH, Severity.REQUIRED),

		/** No search has been completed. */
		NO_SEARCH(Stage.EVIDENCE, Element.EXHAUSTIVE_RESEARCH, Severity.REQUIRED),
		/** A research activity is planned or in progress. */
		UNFINISHED_ACTIVITY(Stage.EVIDENCE, Element.EXHAUSTIVE_RESEARCH, Severity.RECOMMENDED),
		/** A search found the records unavailable. */
		UNAVAILABLE_RECORDS(Stage.EVIDENCE, Element.EXHAUSTIVE_RESEARCH, Severity.RECOMMENDED),
		/** A research task of the question is still to do. */
		OPEN_TASK(Stage.EVIDENCE, Element.EXHAUSTIVE_RESEARCH, Severity.RECOMMENDED),
		/** Fewer than two sources support the assertions. */
		FEW_SOURCES(Stage.EVIDENCE, Element.EXHAUSTIVE_RESEARCH, Severity.RECOMMENDED),
		/** No original source is among the evidence. */
		NO_ORIGINAL_SOURCE(Stage.EVIDENCE, Element.EXHAUSTIVE_RESEARCH, Severity.RECOMMENDED),

		/** An assertion has no citation. */
		UNCITED(Stage.EVIDENCE, Element.CITATIONS, Severity.REQUIRED),
		/** A citation says neither where in the source nor which passage. */
		NO_LOCATOR(Stage.EVIDENCE, Element.CITATIONS, Severity.RECOMMENDED),

		/** A citation of an assertion is not assessed (original or derived, primary or secondary, direct or indirect). */
		UNASSESSED_EVIDENCE(Stage.EVIDENCE, Element.ANALYSIS, Severity.RECOMMENDED),
		/** Nothing records the analysis of the evidence: no rationale, no narrative, no analysis activity. */
		NO_ANALYSIS(Stage.EVIDENCE, Element.ANALYSIS, Severity.RECOMMENDED),

		/** Assertions compete and no conclusion evaluates them. */
		UNRESOLVED_CONFLICT(Stage.CONFLICTS, Element.CONFLICT_RESOLUTION, Severity.REQUIRED),
		/** An assertion is cited, but no citation supports it. */
		CONTRADICTED(Stage.CONFLICTS, Element.CONFLICT_RESOLUTION, Severity.REQUIRED),
		/** An assertion is supported, but a citation contradicts it and nothing addresses it. */
		UNADDRESSED_CONTRADICTION(Stage.CONFLICTS, Element.CONFLICT_RESOLUTION, Severity.REQUIRED),
		/** A conclusion evaluates records but takes no position. */
		OPEN_CONCLUSION(Stage.CONFLICTS, Element.CONFLICT_RESOLUTION, Severity.REQUIRED),
		/** A conclusion prefers an assertion that is contradicted by a citation. */
		PREFERRED_CONTRADICTED(Stage.CONFLICTS, Element.CONFLICT_RESOLUTION, Severity.REQUIRED),
		/** A conclusion prefers an assertion that weighs less than an alternative. */
		PREFERRED_OUTWEIGHED(Stage.CONFLICTS, Element.CONFLICT_RESOLUTION, Severity.REQUIRED),
		/** An alternative to the preferred assertion is supported by evidence too. */
		RIVAL_SUPPORTED(Stage.CONFLICTS, Element.CONFLICT_RESOLUTION, Severity.RECOMMENDED),

		/** There is no conclusion. */
		NO_CONCLUSION(Stage.CONCLUSION, Element.SOUND_CONCLUSION, Severity.REQUIRED),
		/** A conclusion has no proof status. */
		NO_PROOF_STATUS(Stage.CONCLUSION, Element.SOUND_CONCLUSION, Severity.REQUIRED),
		/** A conclusion has no narrative that explains the reasoning. */
		NO_NARRATIVE(Stage.CONCLUSION, Element.SOUND_CONCLUSION, Severity.REQUIRED),
		/** The status says the preferred assertion is supported or proven, but none is preferred. */
		STATUS_WITHOUT_PREFERRED(Stage.CONCLUSION, Element.SOUND_CONCLUSION, Severity.REQUIRED),
		/** The preferred assertion has no citation that supports it. */
		PREFERRED_UNSUPPORTED(Stage.CONCLUSION, Element.SOUND_CONCLUSION, Severity.REQUIRED),
		/** The conclusion is proven, but the preferred assertion has no direct evidence from an original source. */
		PROVEN_WITHOUT_DIRECT_ORIGINAL(Stage.CONCLUSION, Element.SOUND_CONCLUSION, Severity.REQUIRED),
		/** A target of the conclusion is not in the model. */
		UNRESOLVED_TARGET(Stage.CONCLUSION, Element.SOUND_CONCLUSION, Severity.RECOMMENDED),
		/** No conclusion decides: the evidence is still conflicting. */
		NO_DECISION(Stage.CONCLUSION, Element.SOUND_CONCLUSION, Severity.REQUIRED),
		/** The best conclusion is only supported, not proven. */
		NOT_PROVEN(Stage.CONCLUSION, Element.SOUND_CONCLUSION, Severity.RECOMMENDED),
		/** The question has no summary of its conclusion. */
		NO_SUMMARY(Stage.CONCLUSION, Element.SOUND_CONCLUSION, Severity.RECOMMENDED),
		/** The question is marked as resolved while something required is missing. */
		RESOLVED_TOO_SOON(Stage.CONCLUSION, Element.SOUND_CONCLUSION, Severity.RECOMMENDED);


		private final Stage stage;
		private final Element element;
		private final Severity severity;


		Kind(final Stage stage, final Element element, final Severity severity){
			this.stage = stage;
			this.element = element;
			this.severity = severity;
		}

		public Stage stage(){
			return stage;
		}

		public Element element(){
			return element;
		}

		public Severity severity(){
			return severity;
		}

		/** @return the suffix of the message keys of the user interface */
		public String key(){
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/**
	 * One thing that is missing.
	 *
	 * @param kind        what is missing
	 * @param subjectId   the record it is about (the assertion, the conclusion, the activity, the task...), or {@code null} if it is
	 *                    about the whole research, or if the gap stands for the {@code more} gaps of its kind that are not listed
	 * @param subjectText how the record reads to the user, or {@code null}
	 * @param more        how many more gaps of this kind are not listed (only when there is no subject)
	 */
	public record Gap(Kind kind, String subjectId, String subjectText, int more){

		public Severity severity(){
			return kind.severity();
		}
	}

	/**
	 * @param stage       the stage
	 * @param state       what it looks like
	 * @param required    how many required things are missing in it
	 * @param recommended how many recommended things are missing in it
	 */
	public record StageStatus(Stage stage, StageState state, int required, int recommended){}

	/**
	 * What the evidence of the research rests on.
	 *
	 * @param assertions the assertions (events, attributes, relationships, individuals...) that the research is about
	 * @param citations  how many citations they have
	 * @param supporting how many of them support the assertion they are cited for
	 * @param sources    the identifiers of the sources that support the assertions
	 * @param originals  how many supporting citations are of an original source
	 * @param assessed   how many citations have their evidence qualified
	 */
	public record EvidenceSummary(int assertions, int citations, int supporting, Set<String> sources, int originals, int assessed){}

	/**
	 * Where the work stands.
	 *
	 * @param subject      the research question, or the conclusion the workflow was asked about
	 * @param questions    the research questions the research belongs to
	 * @param targets      the identifiers of the records the research is about
	 * @param activities   the research activities of the questions
	 * @param tasks        the research tasks of the questions
	 * @param conclusions  the conclusions of the questions
	 * @param traces       the evidence trace of every assertion the research is about
	 * @param inspections  how each conclusion was reached
	 * @param evidence     what the evidence rests on
	 * @param gaps         what is missing, the required ones first
	 * @param stages       the state of every stage
	 * @param readiness    how far the proof is
	 */
	public record Assessment(FLEFRecord subject, List<FLEFRecord> questions, List<String> targets, List<FLEFRecord> activities,
			List<FLEFRecord> tasks, List<FLEFRecord> conclusions, List<EvidenceTrace> traces, List<Inspection> inspections,
			EvidenceSummary evidence, List<Gap> gaps, List<StageStatus> stages, Readiness readiness){

		/** @return the gaps that keep the conclusion from being considered reasonably proven */
		public List<Gap> required(){
			return gaps.stream()
				.filter(gap -> gap.severity() == Severity.REQUIRED)
				.toList();
		}

		public List<Gap> gaps(final Stage stage){
			return gaps.stream()
				.filter(gap -> gap.kind().stage() == stage)
				.toList();
		}

		public List<Gap> gaps(final Element element){
			return gaps.stream()
				.filter(gap -> gap.kind().element() == element)
				.toList();
		}

		/** @return the state of the stage */
		public StageStatus status(final Stage stage){
			return stages.stream()
				.filter(status -> status.stage() == stage)
				.findFirst()
				.orElseThrow();
		}

		/** @return the first stage that still has something required to do, {@code null} if there is none */
		public Stage currentStage(){
			return stages.stream()
				.filter(status -> status.state() == StageState.OPEN)
				.map(StageStatus::stage)
				.findFirst()
				.orElse(null);
		}

		/** @return whether the element has nothing missing */
		public boolean isMet(final Element element){
			return gaps(element).isEmpty();
		}
	}


	/** How many gaps of one kind are listed one by one, the others are only counted. */
	private static final int MAX_PER_KIND = 6;
	private static final Set<String> NOT_ASSERTIONS = Set.of(SourceReader.TYPE, MediaReader.TYPE, RepositoryReader.TYPE,
		ConclusionReader.TYPE);


	private GpsWorkflow(){}


	/**
	 * @param model   the model
	 * @param subject a research question or a conclusion
	 * @return where the work stands, and what is missing
	 */
	public static Assessment assess(final FLEFModel model, final FLEFRecord subject){
		final boolean conclusionSubject = ConclusionReader.isOfType(subject);

		final List<FLEFRecord> questions = new ArrayList<>();
		if(conclusionSubject){
			for(final FLEFRecord reference : FLEFRecordHelper.findChildren(subject, ConclusionReader.TAG_RESEARCH)){
				final FLEFRecord question = record(model, idOf(reference));
				if(question != null && ResearchQuestionReader.isOfType(question) && !questions.contains(question))
					questions.add(question);
			}
		}
		else
			questions.add(subject);
		final Set<String> questionIds = new LinkedHashSet<>();
		for(final FLEFRecord question : questions)
			questionIds.add(question.getId());

		final Set<String> targets = new LinkedHashSet<>();
		final List<FLEFRecord> conclusions = new ArrayList<>();
		if(conclusionSubject)
			conclusions.add(subject);
		for(final FLEFRecord question : questions)
			for(final FLEFRecord target : FLEFRecordHelper.findChildren(question, ResearchQuestionReader.TAG_TARGET)){
				final String id = armOf(target);
				final FLEFRecord record = record(model, id);
				if(record == null)
					continue;

				if(ConclusionReader.isOfType(record))
					addIfAbsent(conclusions, record);
				else
					targets.add(id);
			}
		if(!conclusionSubject)
			for(final FLEFRecord conclusion : PrivacyService.visible(model, ResearchHelper.listConclusions(model)))
				for(final FLEFRecord reference : FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESEARCH))
					if(questionIds.contains(idOf(reference))){
						addIfAbsent(conclusions, conclusion);
						break;
					}

		//the assertions a conclusion sets aside are only compared with the preferred one: it is the one the conclusion rests on, and the
		//one that needs its evidence (when the conclusion prefers none, all of them are still to be judged)
		for(final FLEFRecord conclusion : conclusions){
			final String preferred = armOf(FLEFRecordHelper.findChild(conclusion, ConclusionReader.TAG_PREFERRED));
			for(final String id : (preferred != null? Set.of(preferred): resolvesOf(conclusion)))
				if(record(model, id) != null)
					targets.add(id);
		}

		final List<FLEFRecord> activities = new ArrayList<>();
		for(final FLEFRecord activity : PrivacyService.visible(model, ResearchHelper.listActivities(model)))
			if(refersTo(activity, ResearchActivityReader.TAG_QUESTION, questionIds))
				activities.add(activity);
		final List<FLEFRecord> tasks = new ArrayList<>();
		for(final FLEFRecord task : PrivacyService.visible(model, ResearchHelper.listTasks(model)))
			if(refersTo(task, ResearchTaskReader.TAG_QUESTION, questionIds))
				tasks.add(task);

		final List<EvidenceTrace> traces = traces(model, targets);
		final List<Inspection> inspections = new ArrayList<>();
		for(final FLEFRecord conclusion : conclusions)
			inspections.add(InferenceInspection.inspect(model, conclusion));

		final Collector gaps = new Collector(model);
		final EvidenceSummary evidence = evidence(traces, inspections);
		checkQuestion(gaps, questions, targets);
		checkResearch(gaps, questions, activities, tasks, traces, conclusions, evidence);
		checkConflicts(gaps, traces, conclusions, inspections);
		checkConclusions(gaps, questions, conclusions, inspections);
		checkQuestionStatus(gaps, questions, gaps.build());
		final List<Gap> finalGaps = gaps.build();
		final List<StageStatus> stages = stages(finalGaps);
		return new Assessment(subject, List.copyOf(questions), List.copyOf(targets), List.copyOf(activities), List.copyOf(tasks),
			List.copyOf(conclusions), List.copyOf(traces), List.copyOf(inspections), evidence, finalGaps, stages,
			readiness(finalGaps, questions, targets, activities, conclusions));
	}

	private static void addIfAbsent(final List<FLEFRecord> records, final FLEFRecord record){
		if(!records.contains(record))
			records.add(record);
	}

	private static FLEFRecord record(final FLEFModel model, final String id){
		final FLEFRecord record = (id != null? model.getRecordById(id): null);
		return (record != null && !PrivacyService.isHidden(model, record)? record: null);
	}

	/** The evidence trace of every assertion the research is about: the individuals through everything that is said about them. */
	private static List<EvidenceTrace> traces(final FLEFModel model, final Set<String> targets){
		final Map<String, EvidenceTrace> traces = new LinkedHashMap<>();
		for(final String id : targets){
			final FLEFRecord record = model.getRecordById(id);
			if(record == null || NOT_ASSERTIONS.contains(record.getTag().toUpperCase(Locale.ROOT)))
				continue;

			final List<EvidenceTrace> found = (IndividualReader.isOfType(record)
				? EvidenceTrace.ofIndividual(model, id)
				: Collections.singletonList(EvidenceTrace.of(model, id)));
			for(final EvidenceTrace trace : found)
				if(trace != null)
					traces.putIfAbsent(trace.subject().record().getId(), trace);
		}
		return List.copyOf(traces.values());
	}

	private static EvidenceSummary evidence(final List<EvidenceTrace> traces, final List<Inspection> inspections){
		int citations = 0;
		int supporting = 0;
		int originals = 0;
		int assessed = 0;
		final Set<String> sources = new LinkedHashSet<>();
		for(final EvidenceTrace trace : traces)
			for(final FLEFRecord citation : FLEFRecordHelper.findChildren(trace.subject().record(), SourceCitationReader.TAG_SOURCE)){
				citations ++;
				final FLEFRecord qualifiers = FLEFRecordHelper.findChild(citation, SourceCitationReader.TAG_EVIDENCE);
				final String sourceType = (qualifiers != null? EvidenceQualifiersReader.extractSourceType(qualifiers): null);
				if(isAssessed(qualifiers))
					assessed ++;
				if(!SourceCitationReader.isContradicting(citation) && SourceCitationReader.citationWeight(citation) > 0){
					supporting ++;
					if(SourceCitationReader.extractSource(citation) != null)
						sources.add(SourceCitationReader.extractSource(citation));
					if(EvidenceQualifiersReader.ENUM_SOURCE_TYPE_ORIGINAL.equals(sourceType))
						originals ++;
				}
			}
		return new EvidenceSummary(traces.size(), citations, supporting, Set.copyOf(sources), originals, assessed);
	}

	private static boolean isAssessed(final FLEFRecord qualifiers){
		return (qualifiers != null && (StringUtils.isNotBlank(EvidenceQualifiersReader.extractSourceType(qualifiers))
			|| StringUtils.isNotBlank(EvidenceQualifiersReader.extractInformationType(qualifiers))
			|| StringUtils.isNotBlank(EvidenceQualifiersReader.extractEvidenceType(qualifiers))));
	}


	/* ======================================================================
	 *                          The checks
	 * ====================================================================== */

	private static void checkQuestion(final Collector gaps, final List<FLEFRecord> questions, final Set<String> targets){
		if(questions.isEmpty())
			gaps.add(Kind.NO_QUESTION, null);
		else if(targets.isEmpty())
			for(final FLEFRecord question : questions)
				if(FLEFRecordHelper.findChildren(question, ResearchQuestionReader.TAG_TARGET).isEmpty())
					gaps.add(Kind.NO_TARGET, question);
	}

	/** The research: exhaustive, cited, analysed. */
	private static void checkResearch(final Collector gaps, final List<FLEFRecord> questions,
			final List<FLEFRecord> activities, final List<FLEFRecord> tasks, final List<EvidenceTrace> traces,
			final List<FLEFRecord> conclusions, final EvidenceSummary evidence){
		if(questions.isEmpty())
			return;

		boolean searched = false;
		boolean analysed = false;
		for(final FLEFRecord activity : activities){
			final String status = ResearchActivityReader.extractStatus(activity);
			final String type = ResearchActivityReader.extractActivityType(activity);
			final String result = ResearchActivityReader.extractResult(activity);
			final boolean finished = ResearchActivityReader.ENUM_STATUS_COMPLETED.equalsIgnoreCase(status);
			if(finished && "search".equalsIgnoreCase(type))
				searched = true;
			if(finished && "analysis".equalsIgnoreCase(type))
				analysed = true;
			if(ResearchActivityReader.ENUM_STATUS_PLANNED.equalsIgnoreCase(status)
					|| ResearchActivityReader.ENUM_STATUS_IN_PROGRESS.equalsIgnoreCase(status))
				gaps.add(Kind.UNFINISHED_ACTIVITY, activity);
			if(ResearchActivityReader.ENUM_RESULT_UNAVAILABLE.equalsIgnoreCase(result))
				gaps.add(Kind.UNAVAILABLE_RECORDS, activity);
		}
		if(!searched)
			gaps.add(Kind.NO_SEARCH, null);
		for(final FLEFRecord task : tasks){
			final String status = ResearchTaskReader.extractStatus(task);
			if(!ResearchTaskReader.ENUM_STATUS_COMPLETED.equalsIgnoreCase(status) && !ResearchTaskReader.ENUM_STATUS_ABANDONED.equalsIgnoreCase(status))
				gaps.add(Kind.OPEN_TASK, task);
		}

		if(traces.isEmpty())
			return;

		for(final EvidenceTrace trace : traces){
			final FLEFRecord record = trace.subject().record();
			final List<FLEFRecord> citations = FLEFRecordHelper.findChildren(record, SourceCitationReader.TAG_SOURCE);
			if(citations.isEmpty())
				gaps.add(Kind.UNCITED, record);
			boolean noLocator = false;
			boolean unassessed = false;
			for(final FLEFRecord citation : citations){
				noLocator |= (StringUtils.isBlank(FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_LOCATOR))
					&& FLEFRecordHelper.findChildren(citation, SourceCitationReader.TAG_EXTRACT).isEmpty());
				unassessed |= !isAssessed(FLEFRecordHelper.findChild(citation, SourceCitationReader.TAG_EVIDENCE));
			}
			if(noLocator)
				gaps.add(Kind.NO_LOCATOR, record);
			if(unassessed)
				gaps.add(Kind.UNASSESSED_EVIDENCE, record);
		}

		if(evidence.sources().size() < 2 && evidence.citations() > 0)
			gaps.add(Kind.FEW_SOURCES, null);
		if(evidence.assessed() > 0 && evidence.originals() == 0)
			gaps.add(Kind.NO_ORIGINAL_SOURCE, null);

		boolean reasoned = analysed;
		for(final FLEFRecord question : questions)
			reasoned |= StringUtils.isNotBlank(ResearchHelper.firstTextValue(question, ResearchQuestionReader.TAG_RATIONALE));
		for(final FLEFRecord conclusion : conclusions)
			reasoned |= StringUtils.isNotBlank(ResearchHelper.firstTextValue(conclusion, ConclusionReader.TAG_NARRATIVE));
		if(!reasoned)
			gaps.add(Kind.NO_ANALYSIS, null);
	}

	/** The conflicts among the evidence, and what the conclusions did with them. */
	private static void checkConflicts(final Collector gaps, final List<EvidenceTrace> traces,
			final List<FLEFRecord> conclusions, final List<Inspection> inspections){
		final Set<String> seen = new LinkedHashSet<>();
		for(final EvidenceTrace trace : traces){
			final FLEFRecord record = trace.subject().record();
			switch(trace.verdict()){
				case CONTRADICTED -> gaps.add(Kind.CONTRADICTED, record);
				case OPEN, BEST_SUPPORTED -> {
					//the same conflict is seen from every assertion that takes part in it
					final List<String> group = new ArrayList<>();
					group.add(record.getId());
					for(final EvidenceTrace.Assertion alternative : trace.alternatives())
						group.add(alternative.record().getId());
					group.sort(null);
					if(seen.add(String.join("|", group)))
						gaps.add(Kind.UNRESOLVED_CONFLICT, record);
				}
				default -> {
					if(trace.verdict() != EvidenceTrace.Verdict.SETTLED && trace.subject().links().stream().anyMatch(EvidenceTrace.Link::contradicts))
						gaps.add(Kind.UNADDRESSED_CONTRADICTION, record);
				}
			}
		}

		for(int i = 0; i < conclusions.size(); i ++){
			final FLEFRecord conclusion = conclusions.get(i);
			final ConclusionResolution.Evaluation evaluation = evaluation(conclusion);
			if(ConclusionResolution.isOpen(evaluation) && evaluation.resolves().size() >= 2)
				gaps.add(Kind.OPEN_CONCLUSION, conclusion);
			for(final Finding finding : inspections.get(i).findings())
				switch(finding.kind()){
					case PREFERRED_CONTRADICTED -> gaps.add(Kind.PREFERRED_CONTRADICTED, conclusion);
					case PREFERRED_OUTWEIGHED -> gaps.add(Kind.PREFERRED_OUTWEIGHED, conclusion);
					case RIVAL_SUPPORTED -> gaps.add(Kind.RIVAL_SUPPORTED, conclusion);
					default -> { }
				}
		}
	}

	/** The conclusions: status, narrative, preferred assertion, direct evidence. */
	private static void checkConclusions(final Collector gaps, final List<FLEFRecord> questions,
			final List<FLEFRecord> conclusions, final List<Inspection> inspections){
		if(questions.isEmpty() && conclusions.isEmpty())
			return;
		if(conclusions.isEmpty()){
			gaps.add(Kind.NO_CONCLUSION, null);
			return;
		}

		boolean decided = false;
		boolean proven = false;
		for(int i = 0; i < conclusions.size(); i ++){
			final FLEFRecord conclusion = conclusions.get(i);
			final String status = StringUtils.lowerCase(ResearchHelper.proofStatus(conclusion), Locale.ROOT);
			final ConclusionResolution.Evaluation evaluation = evaluation(conclusion);
			//a conclusion that lists competing records and decides nothing is reported as open, with the conflicts
			if(StringUtils.isBlank(status) && (evaluation.resolves().size() < 2 || !ConclusionResolution.isOpen(evaluation)))
				gaps.add(Kind.NO_PROOF_STATUS, conclusion);
			if(ConclusionReader.ENUM_PROOF_STATUS_PROVEN.equals(status) || ConclusionReader.ENUM_PROOF_STATUS_DISPROVEN.equals(status))
				proven = decided = true;
			else if(ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED.equals(status))
				decided = true;

			for(final Finding finding : inspections.get(i).findings())
				switch(finding.kind()){
					case NO_NARRATIVE -> gaps.add(Kind.NO_NARRATIVE, conclusion);
					case STATUS_WITHOUT_PREFERRED -> gaps.add(Kind.STATUS_WITHOUT_PREFERRED, conclusion);
					case PREFERRED_UNSUPPORTED -> gaps.add(Kind.PREFERRED_UNSUPPORTED, conclusion);
					case PROVEN_WITHOUT_DIRECT_ORIGINAL -> gaps.add(Kind.PROVEN_WITHOUT_DIRECT_ORIGINAL, conclusion);
					case UNRESOLVED_TARGET -> gaps.add(Kind.UNRESOLVED_TARGET, conclusion);
					default -> { }
				}
		}
		if(!decided)
			gaps.add(Kind.NO_DECISION, null);
		else if(!proven)
			gaps.add(Kind.NOT_PROVEN, null);
	}

	/** What the question says about itself, once the rest is known. */
	private static void checkQuestionStatus(final Collector gaps, final List<FLEFRecord> questions, final List<Gap> found){
		final boolean requiredMissing = found.stream()
			.anyMatch(gap -> gap.severity() == Severity.REQUIRED);
		final boolean concluded = found.stream()
			.noneMatch(gap -> gap.kind() == Kind.NO_CONCLUSION);
		for(final FLEFRecord question : questions){
			if(requiredMissing && ResearchQuestionReader.ENUM_STATUS_RESOLVED.equalsIgnoreCase(ResearchHelper.status(question)))
				gaps.add(Kind.RESOLVED_TOO_SOON, question);
			if(concluded && StringUtils.isBlank(ResearchHelper.firstTextValue(question, ResearchQuestionReader.TAG_CONCLUSION)))
				gaps.add(Kind.NO_SUMMARY, question);
		}
	}


	/* ======================================================================
	 *                          Stages and readiness
	 * ====================================================================== */

	private static List<StageStatus> stages(final List<Gap> gaps){
		final Map<Stage, int[]> counts = new EnumMap<>(Stage.class);
		for(final Stage stage : Stage.values())
			counts.put(stage, new int[2]);
		int required = 0;
		int recommended = 0;
		for(final Gap gap : gaps){
			final int weight = (gap.subjectId() == null && gap.more() > 0? gap.more(): 1);
			final int[] count = counts.get(gap.kind().stage());
			if(gap.severity() == Severity.REQUIRED){
				count[0] += weight;
				required += weight;
			}
			else{
				count[1] += weight;
				recommended += weight;
			}
		}
		//the proof statement can be written when nothing required is missing anywhere
		counts.put(Stage.PROOF, new int[]{required, recommended});

		final List<StageStatus> stages = new ArrayList<>(Stage.values().length);
		for(final Stage stage : Stage.values()){
			final int[] count = counts.get(stage);
			stages.add(new StageStatus(stage, (count[0] > 0? StageState.OPEN: (count[1] > 0? StageState.DONE_WITH_REMARKS: StageState.DONE)),
				count[0], count[1]));
		}
		return List.copyOf(stages);
	}

	private static Readiness readiness(final List<Gap> gaps, final List<FLEFRecord> questions, final Set<String> targets,
			final List<FLEFRecord> activities, final List<FLEFRecord> conclusions){
		if(gaps.isEmpty())
			return Readiness.COMPLETE;
		if(gaps.stream().anyMatch(gap -> gap.severity() == Severity.REQUIRED))
			return (targets.isEmpty() && activities.isEmpty() && conclusions.isEmpty()? Readiness.NOT_STARTED: Readiness.IN_PROGRESS);
		return Readiness.REASONABLY_PROVEN;
	}


	/* ======================================================================
	 *                          Reading the records
	 * ====================================================================== */

	private static ConclusionResolution.Evaluation evaluation(final FLEFRecord conclusion){
		final String status = ResearchHelper.proofStatus(conclusion);
		return new ConclusionResolution.Evaluation(resolvesOf(conclusion), armOf(FLEFRecordHelper.findChild(conclusion, ConclusionReader.TAG_PREFERRED)),
			(StringUtils.isBlank(status)? null: status.toLowerCase(Locale.ROOT)));
	}

	private static Set<String> resolvesOf(final FLEFRecord conclusion){
		final Set<String> ids = new LinkedHashSet<>();
		for(final FLEFRecord target : FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESOLVES)){
			final String id = armOf(target);
			if(id != null)
				ids.add(id);
		}
		return ids;
	}

	private static boolean refersTo(final FLEFRecord record, final String tag, final Set<String> ids){
		for(final FLEFRecord reference : FLEFRecordHelper.findChildren(record, tag))
			if(ids.contains(idOf(reference)))
				return true;
		return false;
	}

	/** @return the identifier a reference holds, as its value or as the value of its only child, or {@code null} */
	private static String idOf(final FLEFRecord reference){
		if(reference == null)
			return null;

		String id = reference.getValue();
		if(StringUtils.isBlank(id)){
			final FLEFRecord only = reference.getTheOnlyChild();
			id = (only != null? only.getValue(): null);
		}
		return (StringUtils.isNotBlank(id)? id: null);
	}

	/** @return the identifier an alternative of a "oneof" (a target) points to, or {@code null} */
	private static String armOf(final FLEFRecord union){
		if(union == null)
			return null;

		return (union.getChildren().isEmpty()? idOf(union): idOf(union.getChildren().getFirst()));
	}


	/** Collects the gaps, with a limit on how many of a kind are listed one by one. */
	private static final class Collector{

		private final FLEFModel model;
		private final Map<Kind, List<Gap>> listed = new EnumMap<>(Kind.class);
		private final Map<Kind, Integer> omitted = new EnumMap<>(Kind.class);
		private final Set<String> keys = new LinkedHashSet<>();


		Collector(final FLEFModel model){
			this.model = model;
		}

		/** @param subject the record the gap is about, {@code null} for the whole research */
		void add(final Kind kind, final FLEFRecord subject){
			if(!keys.add(kind + "|" + (subject != null? subject.getId(): StringUtils.EMPTY)))
				return;

			final List<Gap> gaps = listed.computeIfAbsent(kind, k -> new ArrayList<>());
			if(gaps.size() >= MAX_PER_KIND)
				omitted.merge(kind, 1, Integer::sum);
			else
				gaps.add(new Gap(kind, (subject != null? subject.getId(): null), (subject != null? RecordDisplay.describe(subject, model): null), 0));
		}

		/** @return the gaps: the required ones first, each kind in the order of the stages */
		List<Gap> build(){
			final List<Gap> gaps = new ArrayList<>();
			for(final Severity severity : Severity.values())
				for(final Kind kind : Kind.values())
					if(kind.severity() == severity && listed.containsKey(kind)){
						gaps.addAll(listed.get(kind));
						if(omitted.containsKey(kind))
							gaps.add(new Gap(kind, null, null, omitted.get(kind)));
					}
			return gaps;
		}

	}

}
