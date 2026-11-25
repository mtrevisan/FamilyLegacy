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
package io.github.mtrevisan.familylegacy.sources;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionResolution;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ExtractReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Why do we believe this? The evidence trace of an assertion (an event, an attribute, a relationship, an individual...): the chain
 * <pre>assertion -> citation -> extract -> source</pre>
 * with the citations that support it and the ones that contradict it, the confidence, the assertions that compete with it, the
 * conclusions that evaluate them (and the one they prefer), and the reasoning path that leads to a {@link Verdict}.
 * <p>
 * It is read from the model and never changes it, and it never decides anything: the verdict only tells how the evidence stands, the
 * judgement stays with the researcher's conclusions. The records hidden by the privacy policy are left out. The reasoning is given as
 * {@link Step}s (a key and its arguments) so that the user interface words them.
 *
 * @param subject	The assertion that is explained.
 * @param alternatives	The assertions that compete with it (several births of a person, two values of a single-valued attribute...).
 * @param conclusions	The conclusions that evaluate the subject or one of its competitors.
 * @param verdict	How the evidence stands.
 * @param reasoning	The reasoning path, step by step.
 */
public record EvidenceTrace(Assertion subject, List<Assertion> alternatives, List<ConclusionLink> conclusions, Verdict verdict,
		List<Step> reasoning){

	/** How the evidence of the subject stands. */
	public enum Verdict{
		/** No citation: nothing says where it comes from. */
		UNDOCUMENTED,
		/** Cited, but no citation supports it (they all contradict it, or are negative evidence). */
		CONTRADICTED,
		/** Supported, and nothing competes with it. */
		UNCONTESTED,
		/** There are competitors and a conclusion of the researcher evaluates them. */
		SETTLED,
		/** There are competitors, no conclusion, and the subject is better supported than all of them. */
		BEST_SUPPORTED,
		/** There are competitors, no conclusion, and the subject is not clearly better supported. */
		OPEN
	}

	/**
	 * One citation of an assertion, with the extracts and the source it leads to.
	 *
	 * @param sourceId	The identifier of the cited source ({@code null} if the citation does not say it).
	 * @param source	The source record ({@code null} if it is not in the model).
	 * @param sourceTitle	The title of the source (its identifier if it has none).
	 * @param locator	The place in the source (empty if none).
	 * @param stance	{@code supports} or {@code contradicts}.
	 * @param extracts	The extracts the citation references.
	 * @param evidence	The evidence qualifiers as one line (empty if not assessed).
	 * @param weight	What the citation weighs (see {@link SourceCitationReader#citationWeight(FLEFRecord)}).
	 */
	public record Link(String sourceId, FLEFRecord source, String sourceTitle, String locator, String stance, List<FLEFRecord> extracts,
			String evidence, int weight){

		/** @return	Whether the citation contradicts the assertion. */
		public boolean contradicts(){
			return SourceCitationReader.STANCE_CONTRADICTS.equals(stance);
		}

		/** @return	Whether the citation backs the assertion (it neither contradicts it nor is negative evidence). */
		public boolean supports(){
			return (weight > 0);
		}
	}

	/**
	 * An assertion and its evidence.
	 *
	 * @param record	The record.
	 * @param text	The text that names it to the user.
	 * @param confidence	The confidence the researcher gave ({@code low}, {@code medium}, {@code high}; empty if none).
	 * @param links	The citations, in the order of the file.
	 * @param weight	What the citations weigh altogether.
	 * @param preferred	Whether a conclusion prefers it.
	 */
	public record Assertion(FLEFRecord record, String text, String confidence, List<Link> links, int weight, boolean preferred){

		/** @return	How many citations support it. */
		public long supporting(){
			return links.stream()
				.filter(Link::supports)
				.count();
		}

		/** @return	How many citations do not (they contradict it, or are negative evidence). */
		public long contradicting(){
			return links.size() - supporting();
		}
	}

	/**
	 * A conclusion that evaluates the assertions.
	 *
	 * @param conclusion	The record.
	 * @param issue	The issue it resolves (empty if none).
	 * @param proofStatus	The proof status (empty if none).
	 * @param narrative	The reasoning the researcher wrote (empty if none).
	 * @param preferredId	The identifier of the assertion it prefers ({@code null} if none).
	 * @param preferredText	The text of the assertion it prefers (empty if none).
	 * @param settles	Whether it settles the conflict among the competing assertions (see {@link ConclusionResolution}).
	 */
	public record ConclusionLink(FLEFRecord conclusion, String issue, String proofStatus, String narrative, String preferredId,
			String preferredText, boolean settles){}

	/**
	 * A step of the reasoning.
	 *
	 * @param key	What the step says (it is the suffix of the message key that words it).
	 * @param args	What the message says about.
	 */
	public record Step(String key, List<Object> args){}


	/** The events of which a person can have one only. */
	private static final Set<String> SINGLE_VALUED_EVENTS = Set.of(EventReader.ENUM_TYPE_BIRTH, EventReader.ENUM_TYPE_DEATH,
		EventReader.ENUM_TYPE_BURIAL);
	/** The attributes of which a person can have one only. */
	private static final Set<String> SINGLE_VALUED_ATTRIBUTES = Set.of(IndividualAttributeReader.ENUM_TYPE_NATIONAL_ID,
		IndividualAttributeReader.ENUM_TYPE_SEX);
	private static final String TAG_CONFIDENCE = "confidence";


	/**
	 * @param model	The model.
	 * @param recordId	The identifier of the assertion.
	 * @return	The trace; {@code null} if there is no such record, it is hidden by the privacy policy or it is a source (see
	 * 	{@link SourceWorkspace} for the way back from a source).
	 */
	public static EvidenceTrace of(final FLEFModel model, final String recordId){
		return of(model, recordId, new Index(model));
	}

	private static EvidenceTrace of(final FLEFModel model, final String recordId, final Index index){
		final FLEFRecord record = (recordId != null? model.getRecordById(recordId): null);
		if(record == null || SourceReader.isOfType(record) || PrivacyService.isHidden(model, record))
			return null;

		final List<FLEFRecord> competitors = competitorsOf(model, record, index);
		final Set<String> competing = new LinkedHashSet<>();
		addIds(index, record, competing);
		for(final FLEFRecord competitor : competitors)
			addIds(index, competitor, competing);

		final List<FLEFRecord> conclusionRecords = new ArrayList<>();
		for(final FLEFRecord conclusion : index.conclusions){
			boolean targets = competing.contains(refOf(FLEFRecordHelper.findChild(conclusion, ConclusionReader.TAG_PREFERRED)));
			for(final FLEFRecord target : FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESOLVES))
				targets |= competing.contains(refOf(target));
			if(targets)
				conclusionRecords.add(conclusion);
		}

		final Set<String> preferred = new LinkedHashSet<>();
		for(final FLEFRecord conclusion : conclusionRecords){
			final String proof = FLEFRecordHelper.getChildValue(conclusion, ConclusionReader.TAG_PROOF_STATUS);
			final String id = refOf(FLEFRecordHelper.findChild(conclusion, ConclusionReader.TAG_PREFERRED));
			if(id != null && !ConclusionReader.ENUM_PROOF_STATUS_DISPROVEN.equals(proof))
				preferred.add(id);
		}

		final Assertion subject = assertion(model, record, preferred);
		final List<Assertion> alternatives = new ArrayList<>(competitors.size());
		for(final FLEFRecord competitor : competitors)
			alternatives.add(assertion(model, competitor, preferred));

		final List<ConclusionLink> conclusions = new ArrayList<>(conclusionRecords.size());
		for(final FLEFRecord conclusion : conclusionRecords){
			final Set<String> resolves = new LinkedHashSet<>();
			for(final FLEFRecord target : FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESOLVES))
				resolves.add(refOf(target));
			final String preferredId = refOf(FLEFRecordHelper.findChild(conclusion, ConclusionReader.TAG_PREFERRED));
			final String proof = StringUtils.defaultString(FLEFRecordHelper.getChildValue(conclusion, ConclusionReader.TAG_PROOF_STATUS));
			final FLEFRecord preferredRecord = (preferredId != null? model.getRecordById(preferredId): null);
			conclusions.add(new ConclusionLink(conclusion, StringUtils.defaultString(ConclusionReader.extractIssues(conclusion)), proof,
				StringUtils.defaultString(ConclusionReader.extractNarrative(conclusion)), preferredId,
				(preferredRecord != null? RecordDisplay.describe(preferredRecord, model): StringUtils.EMPTY),
				ConclusionResolution.settles(competing, new ConclusionResolution.Evaluation(resolves, preferredId,
					(proof.isEmpty()? null: proof)))));
		}

		final List<Step> steps = new ArrayList<>();
		final Verdict verdict = reason(subject, alternatives, conclusions, steps);
		return new EvidenceTrace(subject, List.copyOf(alternatives), List.copyOf(conclusions), verdict, List.copyOf(steps));
	}

	/**
	 * The traces of everything that says something about an individual: the individual itself, the events they take part in, their
	 * attributes and their relationships.
	 *
	 * @param model	The model.
	 * @param individualId	The identifier of the individual.
	 * @return	The traces, the individual's own first; empty if the individual is missing or hidden.
	 */
	public static List<EvidenceTrace> ofIndividual(final FLEFModel model, final String individualId){
		final FLEFRecord individual = (individualId != null? model.getRecordById(individualId): null);
		final List<EvidenceTrace> traces = new ArrayList<>();
		if(individual == null || !IndividualReader.isOfType(individual) || PrivacyService.isHidden(model, individual))
			return traces;

		final Index index = new Index(model);
		final Set<String> ids = new LinkedHashSet<>();
		ids.add(individualId);
		ids.addAll(index.eventsOfIndividual.getOrDefault(individualId, List.of()));
		ids.addAll(index.attributesOfIndividual.getOrDefault(individualId, List.of()));
		ids.addAll(index.relationshipsOfIndividual.getOrDefault(individualId, List.of()));

		for(final String id : ids){
			final EvidenceTrace trace = of(model, id, index);
			if(trace != null)
				traces.add(trace);
		}
		return traces;
	}


	private static Assertion assertion(final FLEFModel model, final FLEFRecord record, final Set<String> preferred){
		final List<Link> links = new ArrayList<>();
		int weight = 0;
		for(final FLEFRecord child : record.getChildren()){
			if(!SourceReader.isOfType(child))
				continue;

			final String sourceId = SourceHelper.extractReferencedId(child);
			final FLEFRecord source = (sourceId != null? model.getRecordById(sourceId): null);
			final FLEFRecord evidence = FLEFRecordHelper.findChild(child, SourceCitationReader.TAG_EVIDENCE);
			final String stance = FLEFRecordHelper.getChildValue(child, SourceCitationReader.TAG_STANCE);
			final int citationWeight = SourceCitationReader.citationWeight(child);
			weight += citationWeight;
			links.add(new Link(sourceId, source, (source != null? SourceHelper.sourceTitle(source): StringUtils.defaultString(sourceId)),
				StringUtils.defaultString(FLEFRecordHelper.getChildValue(child, SourceCitationReader.TAG_LOCATOR)),
				(stance != null? stance: SourceCitationReader.STANCE_SUPPORTS), PrivacyService.visible(model,
					ExtractReader.extractsOf(child, model)), evidenceText(evidence), citationWeight));
		}
		return new Assertion(record, RecordDisplay.describe(record, model),
			StringUtils.defaultString(FLEFRecordHelper.getChildValue(record, TAG_CONFIDENCE)), List.copyOf(links), weight,
			preferred.contains(record.getId()));
	}

	private static String evidenceText(final FLEFRecord evidence){
		if(evidence == null)
			return StringUtils.EMPTY;

		final List<String> parts = new ArrayList<>(3);
		for(final String part : new String[]{EvidenceQualifiersReader.extractSourceType(evidence),
				EvidenceQualifiersReader.extractInformationType(evidence), EvidenceQualifiersReader.extractEvidenceType(evidence)})
			if(StringUtils.isNotBlank(part))
				parts.add(part);
		return String.join(", ", parts);
	}


	/** Builds the reasoning path and tells the verdict. */
	private static Verdict reason(final Assertion subject, final List<Assertion> alternatives, final List<ConclusionLink> conclusions,
			final List<Step> steps){
		if(subject.links().isEmpty())
			steps.add(step("uncited"));
		else{
			steps.add(step("cited", subject.links().size(), subject.supporting(), subject.contradicting(), subject.weight()));
			final long assessed = subject.links().stream()
				.filter(link -> !link.evidence().isEmpty())
				.count();
			steps.add(step("assessed", assessed, subject.links().size()));
		}
		if(!subject.confidence().isEmpty())
			steps.add(step("confidence", subject.confidence()));

		if(alternatives.isEmpty())
			steps.add(step("no_alternatives"));
		else{
			final List<String> texts = new ArrayList<>(alternatives.size());
			for(final Assertion alternative : alternatives)
				texts.add(alternative.text());
			steps.add(step("alternatives", alternatives.size(), String.join("; ", texts)));
			steps.add(step("weighs", subject.text(), subject.weight()));
			for(final Assertion alternative : alternatives)
				steps.add(step("weighs", alternative.text(), alternative.weight()));
		}

		boolean settled = false;
		for(final ConclusionLink conclusion : conclusions){
			settled |= conclusion.settles();
			steps.add(step((conclusion.settles()? "conclusion_settles": "conclusion_draft"), conclusion.conclusion().getId(),
				(conclusion.issue().isEmpty()? "?": conclusion.issue()), (conclusion.proofStatus().isEmpty()? "?": conclusion.proofStatus()),
				(conclusion.preferredText().isEmpty()? "-": conclusion.preferredText())));
		}

		final Verdict verdict;
		if(subject.links().isEmpty())
			verdict = Verdict.UNDOCUMENTED;
		else if(!alternatives.isEmpty() && settled)
			verdict = Verdict.SETTLED;
		else if(subject.supporting() == 0)
			verdict = Verdict.CONTRADICTED;
		else if(alternatives.isEmpty())
			verdict = Verdict.UNCONTESTED;
		else if(alternatives.stream().allMatch(alternative -> subject.weight() > alternative.weight()))
			verdict = Verdict.BEST_SUPPORTED;
		else
			verdict = Verdict.OPEN;
		steps.add(step("verdict_" + verdict.name().toLowerCase(Locale.ROOT)));
		return verdict;
	}

	private static Step step(final String key, final Object... args){
		return new Step(key, List.of(args));
	}


	/** The records that compete with the one: they cannot all be true. */
	private static List<FLEFRecord> competitorsOf(final FLEFModel model, final FLEFRecord record, final Index index){
		final Map<String, FLEFRecord> competitors = new LinkedHashMap<>();
		if(EventReader.isOfType(record)){
			final String type = EventReader.extractType(record);
			if(type != null && SINGLE_VALUED_EVENTS.contains(type))
				for(final String principal : index.principalsOfEvent.getOrDefault(record.getId(), Set.of()))
					for(final String otherId : index.eventsOfPrincipal.getOrDefault(principal + "|" + type, List.of()))
						if(!otherId.equals(record.getId()))
							competitors.putIfAbsent(otherId, model.getRecordById(otherId));
		}
		else if(IndividualAttributeReader.isOfType(record)){
			final String type = FLEFRecordHelper.getChildValue(record, IndividualAttributeReader.TAG_TYPE);
			final String individualId = IndividualAttributeReader.extractIndividualId(record);
			if(type != null && individualId != null && SINGLE_VALUED_ATTRIBUTES.contains(type))
				for(final String otherId : index.attributesOfIndividualAndType.getOrDefault(individualId + "|" + type, List.of()))
					if(!otherId.equals(record.getId()))
						competitors.putIfAbsent(otherId, model.getRecordById(otherId));
		}
		else if(RelationshipReader.isOfType(record)){
			final String subjectId = EventParticipationReader.extractParticipantRef(record, RelationshipReader.TAG_SUBJECT);
			final String targetId = EventParticipationReader.extractParticipantRef(record, RelationshipReader.TAG_TARGET);
			final String type = RelationshipReader.extractType(record);
			if(subjectId != null && targetId != null)
				for(final String otherId : index.relationshipsOfPair.getOrDefault(subjectId + "|" + targetId, List.of())){
					final FLEFRecord other = model.getRecordById(otherId);
					if(!otherId.equals(record.getId()) && !StringUtils.equals(type, RelationshipReader.extractType(other)))
						competitors.putIfAbsent(otherId, other);
				}
		}
		return PrivacyService.visible(model, List.copyOf(competitors.values()));
	}

	/** Adds the identifier of the record, and of the participations in it if it is an event: a conclusion can point to any of them. */
	private static void addIds(final Index index, final FLEFRecord record, final Set<String> ids){
		ids.add(record.getId());
		if(EventReader.isOfType(record))
			ids.addAll(index.participationsOfEvent.getOrDefault(record.getId(), List.of()));
	}

	/**
	 * What the trace needs to find in the model, read once: a trace is made of many lookups, and scanning every record for each one
	 * makes a big file wait.
	 */
	private static final class Index{

		final List<FLEFRecord> conclusions;
		final Map<String, List<String>> participationsOfEvent = new LinkedHashMap<>();
		final Map<String, Set<String>> principalsOfEvent = new LinkedHashMap<>();
		/** The single-valued events of each individual, by {@code individual|type}. */
		final Map<String, List<String>> eventsOfPrincipal = new LinkedHashMap<>();
		final Map<String, List<String>> eventsOfIndividual = new LinkedHashMap<>();
		final Map<String, List<String>> attributesOfIndividual = new LinkedHashMap<>();
		final Map<String, List<String>> attributesOfIndividualAndType = new LinkedHashMap<>();
		final Map<String, List<String>> relationshipsOfIndividual = new LinkedHashMap<>();
		final Map<String, List<String>> relationshipsOfPair = new LinkedHashMap<>();


		Index(final FLEFModel model){
			conclusions = PrivacyService.visible(model, model.getRecordsByType(ConclusionReader.TYPE));

			for(final FLEFRecord participation : model.getRecordsByType(EventParticipationReader.TYPE)){
				final String eventId = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT);
				if(eventId == null)
					continue;

				participationsOfEvent.computeIfAbsent(eventId, k -> new ArrayList<>())
					.add(participation.getId());
				final String individualId = EventParticipationReader.extractParticipantRef(participation);
				if(individualId == null)
					continue;

				final List<String> events = eventsOfIndividual.computeIfAbsent(individualId, k -> new ArrayList<>());
				if(!events.contains(eventId))
					events.add(eventId);
				final String role = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_ROLE);
				if(role == null || EventParticipationReader.ENUM_ROLE_PRINCIPAL.equals(role))
					principalsOfEvent.computeIfAbsent(eventId, k -> new LinkedHashSet<>())
						.add(individualId);
			}
			for(final Map.Entry<String, Set<String>> entry : principalsOfEvent.entrySet()){
				final FLEFRecord event = model.getRecordById(entry.getKey());
				final String type = (EventReader.isOfType(event)? EventReader.extractType(event): null);
				if(type != null && SINGLE_VALUED_EVENTS.contains(type))
					for(final String principal : entry.getValue())
						eventsOfPrincipal.computeIfAbsent(principal + "|" + type, k -> new ArrayList<>())
							.add(entry.getKey());
			}

			for(final FLEFRecord attribute : model.getRecordsByType(IndividualAttributeReader.TYPE)){
				final String individualId = IndividualAttributeReader.extractIndividualId(attribute);
				if(individualId == null)
					continue;

				attributesOfIndividual.computeIfAbsent(individualId, k -> new ArrayList<>())
					.add(attribute.getId());
				attributesOfIndividualAndType.computeIfAbsent(individualId + "|"
						+ FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_TYPE), k -> new ArrayList<>())
					.add(attribute.getId());
			}

			for(final FLEFRecord relationship : model.getRecordsByType(RelationshipReader.TYPE)){
				final String subjectId = EventParticipationReader.extractParticipantRef(relationship, RelationshipReader.TAG_SUBJECT);
				final String targetId = EventParticipationReader.extractParticipantRef(relationship, RelationshipReader.TAG_TARGET);
				if(subjectId != null)
					relationshipsOfIndividual.computeIfAbsent(subjectId, k -> new ArrayList<>())
						.add(relationship.getId());
				if(targetId != null && !targetId.equals(subjectId))
					relationshipsOfIndividual.computeIfAbsent(targetId, k -> new ArrayList<>())
						.add(relationship.getId());
				if(subjectId != null && targetId != null)
					relationshipsOfPair.computeIfAbsent(subjectId + "|" + targetId, k -> new ArrayList<>())
						.add(relationship.getId());
			}
		}

	}

	/** The identifier a field of a conclusion points to. */
	private static String refOf(final FLEFRecord field){
		if(field == null)
			return null;

		final FLEFRecord child = field.getTheOnlyChild();
		if(child != null && child.getValue() != null)
			return child.getValue();
		return field.getValue();
	}

}
