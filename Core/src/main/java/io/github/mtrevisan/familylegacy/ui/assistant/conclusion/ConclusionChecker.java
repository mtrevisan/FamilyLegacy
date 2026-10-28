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

import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticKeys;
import io.github.mtrevisan.familylegacy.ui.assistant.chronology.DateRange;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

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


/**
 * Finds the conflicts of the research that no conclusion evaluates yet: several births or deaths of an individual, competing
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
	private static final Set<String> SINGLE_VALUED_ATTRIBUTES = Set.of("national_id");
	private static final Set<String> BIOLOGICAL_TYPES = Set.of(RelationshipReader.ENUM_TYPE_CHILD,
		RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);
	/** The relationships that make a person a descendant of another one: a cycle of them is impossible. */
	private static final Set<String> LINEAGE_TYPES = Set.of(RelationshipReader.ENUM_TYPE_CHILD,
		RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD);
	private static final Set<String> NO_PREVAILING_STATUSES = Set.of("conflicting_evidence", "disproven");
	private static final Map<String, Integer> CONFIDENCE_RANK = Map.of("low", 1, "medium", 2, "high", 3);


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


	/** Whether a conclusion evaluates the competing assertions. */
	static boolean isResolved(final Set<String> competing, final List<ConclusionData.Conclusion> conclusions){
		for(final ConclusionData.Conclusion conclusion : conclusions){
			final long evaluated = conclusion.resolves().stream()
				.filter(competing::contains)
				.count();
			if(evaluated >= 2 && (competing.contains(conclusion.preferred())
					|| (conclusion.proofStatus() != null && NO_PREVAILING_STATUSES.contains(conclusion.proofStatus()))))
				return true;
		}
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
				.map(e -> e.eventId() + ": " + (e.dateText() != null && !e.dateText().isBlank()? e.dateText(): "?"))
				.toList());
			final boolean conflicting = areIncompatible(events);
			findings.add(finding((conflicting? Diagnostic.Severity.WARNING: Diagnostic.Severity.INFORMATION),
				(conflicting? "conflicting_events": "multiple_events"), ids, type + (conflicting? "_date": ""),
				events.stream().map(ConclusionData.PrincipalEvent::eventId).toList(), person.name(), type, events.size(), dates,
				suggestion(events)));
		}
	}

	/** Whether two of the events certainly differ in date: no day is common to all the dates of the events. */
	private static boolean areIncompatible(final List<ConclusionData.PrincipalEvent> events){
		for(int i = 0; i < events.size(); i ++)
			for(int j = i + 1; j < events.size(); j ++){
				final DateRange first = events.get(i).date();
				final DateRange second = events.get(j).date();
				if(first != null && second != null && !first.intersects(second))
					return true;
			}
		return false;
	}

	/** The candidate preferred assertion, if one is better supported than all the others, or the reason there is none. */
	private static String suggestion(final List<ConclusionData.PrincipalEvent> events){
		final Comparator<ConclusionData.PrincipalEvent> byQuality = Comparator
			.comparingInt((ConclusionData.PrincipalEvent e) -> rank(e.confidence()))
			.thenComparingInt(ConclusionData.PrincipalEvent::sourceCount)
			.reversed();
		final List<ConclusionData.PrincipalEvent> ranked = events.stream()
			.sorted(byQuality)
			.toList();
		final ConclusionData.PrincipalEvent best = ranked.getFirst();
		if(byQuality.compare(best, ranked.get(1)) == 0)
			return I18N.t("conclusion.suggest.none");

		return I18N.tf("conclusion.suggest.preferred", best.eventId(), rank(best.confidence()) > 0
			? best.confidence(): "-", best.sourceCount());
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
						(first == SexType.FEMALE? "mothers": "fathers"), names));
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
				String.join(", ", types)));
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
					&& attribute.value() != null && !attribute.value().isBlank())
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
				attributes.getFirst().type(), String.join(" / ", attributes.stream().map(ConclusionData.Attribute::value).toList())));
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
		return value.strip().toLowerCase(Locale.ROOT).replaceAll("[\\s-]+", "");
	}

	/** @return the data a finding reports, without the suggestion that closes the explanation of the conflicts of events (localized) */
	private static Object[] keyData(final String key, final Object[] arguments){
		return (key.endsWith("_events")? Arrays.copyOf(arguments, arguments.length - 1): arguments);
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
			explanation + " " + I18N.t("conclusion.suggest.draft"), recordIds, null,
			DiagnosticKeys.of("conclusion." + key, recordIds, keyData(key, arguments))),
			new ConclusionDraft(issue, List.copyOf(resolves), title + ". " + explanation));
	}

}
