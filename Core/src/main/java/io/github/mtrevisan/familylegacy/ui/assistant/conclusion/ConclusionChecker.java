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
package io.github.mtrevisan.familylegacy.ui.assistant.conclusion;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionResolution;
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticKeys;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Finds the conflicts of the research that no conclusion evaluates yet: several births, baptisms, deaths or burials of an individual
 * (dates or places that cannot all be right), competing
 * parentage, conflicting relationships, attributes with two values, identity hypotheses left open.
 * <p>
 * A conflict is settled when a ConclusionRecord evaluates it: its RESOLVES contains at least two of the competing assertions and
 * it either prefers one of them or states that no one prevails (conflicting evidence) or that they are all false (disproven).
 * A conflict is only reported, never decided: the assertions are preserved, the candidate preferred assertion is a suggestion
 * with its reason, and a conclusion is always written by the researcher.
 * </p>
 */
final class ConclusionChecker{

	/** The most biological parents an individual can have. */
	private static final int MAX_BIOLOGICAL_PARENTS = 2;
	/** The attributes that can have only one value for a person (the other ones change in time, or can be several). */
	private static final Set<String> SINGLE_VALUED_ATTRIBUTES = Set.of(IndividualAttributeReader.ENUM_TYPE_NATIONAL_ID,
		IndividualAttributeReader.ENUM_TYPE_SEX);
	private static final Set<String> BIOLOGICAL_TYPES = Set.of(RelationshipReader.ENUM_TYPE_CHILD,
		RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);
	/** The relationships that make a person a descendant of another one: a cycle of them is impossible. */
	private static final Set<String> LINEAGE_TYPES = Set.of(RelationshipReader.ENUM_TYPE_CHILD,
		RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD);
	private static final Map<String, Integer> CONFIDENCE_RANK = Map.of(ConfidenceReader.ENUM_LOW, 1, ConfidenceReader.ENUM_MEDIUM, 2, ConfidenceReader.ENUM_HIGH, 3);


	private ConclusionChecker(){}


	/** @return the findings, without the drafts of the conclusions */
	static List<Diagnostic> check(final ConclusionData data){
		return findings(data).stream()
			.map(ConclusionFinding::diagnostic)
			.toList();
	}

	/** @return the findings, each with the draft of the conclusion that would resolve it */
	static List<ConclusionFinding> findings(final ConclusionData data){
		final List<ConclusionFinding> findings = new ArrayList<>();
		checkEvents(data, findings);
		checkParentage(data, findings);
		checkLineage(data, findings);
		checkAttributes(data, findings);
		checkHypotheses(data, findings);
		return findings;
	}


	/** Whether a conclusion evaluates the competing assertions (the definition is shared with the merger: {@link ConclusionResolution}). */
	static boolean isResolved(final Set<String> competing, final List<ConclusionData.Conclusion> conclusions){
		for(final ConclusionData.Conclusion conclusion : conclusions)
			if(ConclusionResolution.settles(competing,
					new ConclusionResolution.Evaluation(conclusion.resolves(), conclusion.preferred(), conclusion.proofStatus())))
				return true;
		return false;
	}


	private static void checkEvents(final ConclusionData data, final List<ConclusionFinding> findings){
		final Map<String, List<ConclusionData.PrincipalEvent>> byPersonAndType = new LinkedHashMap<>();
		for(final ConclusionData.PrincipalEvent event : data.events())
			byPersonAndType.computeIfAbsent(event.personId() + "|" + event.type(), k -> new ArrayList<>())
				.add(event);

		for(final List<ConclusionData.PrincipalEvent> events : byPersonAndType.values()){
			if(events.size() < 2)
				continue;

			final ConclusionData.Person person = data.persons().get(events.getFirst().personId());
			final Set<String> competing = new LinkedHashSet<>();
			final List<String> ids = new ArrayList<>();
			ids.add(person.id());
			for(final ConclusionData.PrincipalEvent event : events){
				competing.addAll(event.recordIds());
				ids.add(event.eventId());
			}
			if(isResolved(competing, data.conclusions()))
				continue;

			final String type = events.getFirst().type();
			final String dates = String.join("; ", events.stream()
				.map(e -> e.eventId() + ": " + (e.dateText() != null && !e.dateText().isBlank()? e.dateText(): "?")
					+ (e.placeId() != null? " @ " + e.placeId(): StringUtils.EMPTY))
				.toList());
			final boolean conflicting = areIncompatible(events, data.placeAncestors());
			findings.add(finding((conflicting? Diagnostic.Severity.WARNING: Diagnostic.Severity.INFORMATION),
				(conflicting? "conflicting_events": "multiple_events"), ids, type + (conflicting? "_date": StringUtils.EMPTY),
				events.stream().map(ConclusionData.PrincipalEvent::eventId).toList(), person.name(), type, events.size(), dates,
				suggestion(events.stream().collect(Collectors.toMap(ConclusionData.PrincipalEvent::eventId,
					ConclusionData.PrincipalEvent::backing, (a, b) -> a, LinkedHashMap::new)))));
		}
	}

	/**
	 * Whether two of the events certainly differ: no day is common to their dates, or their places are different and neither contains
	 * the other (a town and its province are compatible).
	 */
	private static boolean areIncompatible(final List<ConclusionData.PrincipalEvent> events,
			final Map<String, Set<String>> placeAncestors){
		for(int i = 0; i < events.size(); i ++)
			for(int j = i + 1; j < events.size(); j ++){
				final ConclusionData.PrincipalEvent first = events.get(i);
				final ConclusionData.PrincipalEvent second = events.get(j);
				if(first.date() != null && second.date() != null && !first.date().intersects(second.date()))
					return true;
				if(first.placeId() != null && second.placeId() != null && !first.placeId().equals(second.placeId())
						&& !placeAncestors.getOrDefault(first.placeId(), Set.of()).contains(second.placeId())
						&& !placeAncestors.getOrDefault(second.placeId(), Set.of()).contains(first.placeId()))
					return true;
			}
		return false;
	}

	/**
	 * The candidate preferred assertion, if one is better supported than all the others, or the reason there is none. The confidence the
	 * researcher gave comes first, then what the sources weigh with their evidence qualifiers (original, primary, direct).
	 *
	 * @param backings how well each competing assertion is backed, by the identifier of the assertion
	 */
	private static String suggestion(final Map<String, ConclusionData.Backing> backings){
		final Comparator<Map.Entry<String, ConclusionData.Backing>> byQuality = Comparator
			.comparingInt((Map.Entry<String, ConclusionData.Backing> e) -> rank(e.getValue().confidence()))
			.thenComparingInt(e -> e.getValue().weight())
			.reversed();
		final List<Map.Entry<String, ConclusionData.Backing>> ranked = backings.entrySet().stream()
			.sorted(byQuality)
			.toList();
		if(ranked.size() < 2 || byQuality.compare(ranked.get(0), ranked.get(1)) == 0)
			return I18N.t("conclusion.suggest.none");

		final ConclusionData.Backing best = ranked.getFirst().getValue();
		return I18N.tf("conclusion.suggest.preferred", ranked.getFirst().getKey(), rank(best.confidence()) > 0? best.confidence(): "-",
			best.sourceCount(), best.weight());
	}

	private static void checkParentage(final ConclusionData data, final List<ConclusionFinding> findings){
		final Map<String, List<ConclusionData.ParentLink>> biologicalOf = new LinkedHashMap<>();
		final Map<String, List<ConclusionData.ParentLink>> linksOfPair = new LinkedHashMap<>();
		for(final ConclusionData.ParentLink link : data.parents()){
			if(!data.persons().containsKey(link.childId()) || !data.persons().containsKey(link.parentId()))
				continue;

			if(BIOLOGICAL_TYPES.contains(link.type()))
				biologicalOf.computeIfAbsent(link.childId(), k -> new ArrayList<>())
					.add(link);
			linksOfPair.computeIfAbsent(link.childId() + "|" + link.parentId(), k -> new ArrayList<>())
				.add(link);
		}

		for(final Map.Entry<String, List<ConclusionData.ParentLink>> entry : biologicalOf.entrySet()){
			final ConclusionData.Person child = data.persons().get(entry.getKey());
			final Map<String, ConclusionData.ParentLink> parents = new LinkedHashMap<>();
			for(final ConclusionData.ParentLink link : entry.getValue())
				parents.putIfAbsent(link.parentId(), link);

			final Set<String> competing = new LinkedHashSet<>();
			final List<String> ids = new ArrayList<>();
			ids.add(child.id());
			for(final ConclusionData.ParentLink link : entry.getValue()){
				competing.add(link.relationshipId());
				ids.add(link.relationshipId());
			}
			if(isResolved(competing, data.conclusions()))
				continue;

			final String names = String.join(", ", parents.keySet().stream()
				.map(id -> data.persons().get(id).name())
				.toList());
			if(parents.size() > MAX_BIOLOGICAL_PARENTS)
				findings.add(finding(Diagnostic.Severity.WARNING, "too_many_parents", ids, "parentage", competing, child.name(), parents.size(), names));
			else if(parents.size() == MAX_BIOLOGICAL_PARENTS){
				final SexType first = data.persons().get(parents.keySet().iterator().next()).sex();
				final SexType second = data.persons().get(parents.keySet().stream().skip(1).findFirst().orElseThrow()).sex();
				if(first == second && first != SexType.UNKNOWN)
					findings.add(finding(Diagnostic.Severity.WARNING, "same_sex_parents", ids, "parentage", competing, child.name(),
						I18N.t(first == SexType.FEMALE? "conclusion.parents.mothers": "conclusion.parents.fathers"), names));
			}
		}

		for(final List<ConclusionData.ParentLink> links : linksOfPair.values()){
			final Set<String> types = new LinkedHashSet<>();
			links.forEach(l -> types.add(l.type()));
			if(types.size() < 2 || !types.stream().anyMatch(BIOLOGICAL_TYPES::contains))
				continue;

			final Set<String> competing = new LinkedHashSet<>();
			links.forEach(l -> competing.add(l.relationshipId()));
			if(isResolved(competing, data.conclusions()))
				continue;

			final ConclusionData.Person child = data.persons().get(links.getFirst().childId());
			final ConclusionData.Person parent = data.persons().get(links.getFirst().parentId());
			final List<String> ids = new ArrayList<>(List.of(child.id(), parent.id()));
			ids.addAll(competing);
			findings.add(finding(Diagnostic.Severity.WARNING, "conflicting_relationships", ids, "relationship_type", competing,
				child.name(), parent.name(),
				String.join(", ", types),
				suggestion(links.stream().collect(Collectors.toMap(ConclusionData.ParentLink::relationshipId,
					ConclusionData.ParentLink::backing, (a, b) -> a, LinkedHashMap::new)))));
		}
	}

	/** An individual that is their own ancestor: the relationships of descent cannot all be right. */
	private static void checkLineage(final ConclusionData data, final List<ConclusionFinding> findings){
		final Map<String, List<ConclusionData.ParentLink>> parentsOf = new HashMap<>();
		for(final ConclusionData.ParentLink link : data.parents())
			if(LINEAGE_TYPES.contains(link.type()) && data.persons().containsKey(link.childId())
					&& data.persons().containsKey(link.parentId()))
				parentsOf.computeIfAbsent(link.childId(), k -> new ArrayList<>())
					.add(link);

		final Map<String, Set<String>> ancestors = new HashMap<>();
		for(final String id : data.persons().keySet())
			ancestors.put(id, ancestorsOf(id, parentsOf));

		final Set<String> reported = new HashSet<>();
		for(final String id : data.persons().keySet()){
			if(reported.contains(id) || !ancestors.get(id).contains(id))
				continue;

			//the cycle: the individuals that are ancestors of each other
			final Set<String> cycle = new LinkedHashSet<>();
			cycle.add(id);
			for(final String other : ancestors.get(id))
				if(ancestors.get(other).contains(id))
					cycle.add(other);
			reported.addAll(cycle);

			final Set<String> relationships = new LinkedHashSet<>();
			for(final ConclusionData.ParentLink link : data.parents())
				if(cycle.contains(link.childId()) && cycle.contains(link.parentId()) && LINEAGE_TYPES.contains(link.type()))
					relationships.add(link.relationshipId());
			if(isResolved(relationships, data.conclusions()))
				continue;

			final List<String> ids = new ArrayList<>(cycle);
			ids.addAll(relationships);
			findings.add(finding(Diagnostic.Severity.ERROR, "own_ancestor", ids, "parentage", relationships,
				data.persons().get(id).name(),
				String.join(", ", cycle.stream().map(c -> data.persons().get(c).name()).toList())));
		}
	}

	private static Set<String> ancestorsOf(final String id, final Map<String, List<ConclusionData.ParentLink>> parentsOf){
		final Set<String> ancestors = new HashSet<>();
		final Deque<String> toVisit = new ArrayDeque<>();
		toVisit.add(id);
		while(!toVisit.isEmpty())
			for(final ConclusionData.ParentLink link : parentsOf.getOrDefault(toVisit.poll(), List.of()))
				if(ancestors.add(link.parentId()))
					toVisit.add(link.parentId());
		return ancestors;
	}

	private static void checkAttributes(final ConclusionData data, final List<ConclusionFinding> findings){
		final Map<String, List<ConclusionData.Attribute>> byPersonAndType = new LinkedHashMap<>();
		for(final ConclusionData.Attribute attribute : data.attributes())
			if(SINGLE_VALUED_ATTRIBUTES.contains(attribute.type()) && data.persons().containsKey(attribute.personId())
					&& attribute.value() != null && !attribute.value().isBlank()
					//an unknown sex says nothing: it does not contradict the others
					&& !(IndividualAttributeReader.ENUM_TYPE_SEX.equals(attribute.type()) && IndividualReader.isSexUnknown(attribute.value())))
				byPersonAndType.computeIfAbsent(attribute.personId() + "|" + attribute.type(), k -> new ArrayList<>())
					.add(attribute);

		for(final List<ConclusionData.Attribute> attributes : byPersonAndType.values()){
			final Set<String> values = new LinkedHashSet<>();
			attributes.forEach(a -> values.add(normalize(a.value())));
			if(values.size() < 2)
				continue;

			final Set<String> competing = new LinkedHashSet<>();
			attributes.forEach(a -> competing.add(a.id()));
			if(isResolved(competing, data.conclusions()))
				continue;

			final ConclusionData.Person person = data.persons().get(attributes.getFirst().personId());
			final List<String> ids = new ArrayList<>();
			ids.add(person.id());
			ids.addAll(competing);
			findings.add(finding(Diagnostic.Severity.WARNING, "conflicting_attributes", ids, attributes.getFirst().type(), competing,
				person.name(),
				attributes.getFirst().type(), String.join(" / ", attributes.stream().map(ConclusionData.Attribute::value).toList()),
				suggestion(attributes.stream().collect(Collectors.toMap(ConclusionData.Attribute::id,
					ConclusionData.Attribute::backing, (a, b) -> a, LinkedHashMap::new)))));
		}
	}

	private static void checkHypotheses(final ConclusionData data, final List<ConclusionFinding> findings){
		for(final ConclusionData.Hypothesis hypothesis : data.hypotheses()){
			if(!data.persons().containsKey(hypothesis.firstId()) || !data.persons().containsKey(hypothesis.secondId()))
				continue;

			final boolean evaluated = data.conclusions().stream()
				.anyMatch(c -> c.resolves().contains(hypothesis.id()));
			if(!evaluated)
				findings.add(finding(Diagnostic.Severity.INFORMATION, "unresolved_identity",
					List.of(hypothesis.id(), hypothesis.firstId(), hypothesis.secondId()),
					"identity", List.of(hypothesis.id()),
					data.persons().get(hypothesis.firstId()).name(), data.persons().get(hypothesis.secondId()).name()));
		}
	}

	/** @return how much a confidence counts, {@code 0} if it is missing */
	private static int rank(final String confidence){
		return (confidence != null? CONFIDENCE_RANK.getOrDefault(confidence, 0): 0);
	}

	private static String normalize(final String value){
		return StringUtils.remove(StringUtils.deleteWhitespace(value.strip().toLowerCase(Locale.ROOT)), '-');
	}

	/** @return the data a finding reports, without the suggestion that closes the explanation of the conflicts of events (localized) */
	private static Object[] keyData(final String key, final Object[] arguments){
		return (key.endsWith("_events") || key.equals("conflicting_attributes") || key.equals("conflicting_relationships")
			? Arrays.copyOf(arguments, arguments.length - 1): arguments);
	}

	/**
	 * @param issue   the issue the conclusion would resolve
	 * @param resolves the competing records the conclusion would evaluate
	 */
	private static ConclusionFinding finding(final Diagnostic.Severity severity, final String key, final List<String> recordIds,
			final String issue, final Collection<String> resolves, final Object... arguments){
		final String title = I18N.tf("conclusion." + key + ".title", arguments);
		final String explanation = I18N.tf("conclusion." + key + ".explanation", arguments);
		return new ConclusionFinding(new Diagnostic(severity, AssistantCategory.CONCLUSIONS, title,
			explanation + StringUtils.SPACE + I18N.t("conclusion.suggest.draft"), recordIds, null,
			DiagnosticKeys.of("conclusion." + key, recordIds, keyData(key, arguments))),
			new ConclusionDraft(issue, List.copyOf(resolves), title + ". " + explanation));
	}

}
