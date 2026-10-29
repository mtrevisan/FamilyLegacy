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
package io.github.mtrevisan.familylegacy.ui.assistant.relationship;

import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticKeys;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Helps to choose the right type of the relationships, and finds the ones that are missing or inconsistent.
 * <p>
 * It suggests a type only when the evidence of the research points to it (an adoption event, a marriage that came after the birth of
 * the child, the banns or the license of a marriage), says what that evidence is, and leaves the choice to the researcher: a
 * relationship is never created or changed by the assistant. For a relationship that is missing it prepares the draft of a new one,
 * to be completed and confirmed in the usual editor.
 * </p>
 * <p>
 * What other assistants already report is not repeated: the conflicting parentage and the descent cycles are findings of the
 * Conclusion Assistant, the ages of the parents of the Chronology Assistant.
 * </p>
 */
final class RelationshipChecker{

	private static final String CHILD = RelationshipReader.ENUM_TYPE_CHILD;
	private static final String BIOLOGICAL_CHILD = RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD;
	private static final String ADOPTIVE_CHILD = RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD;
	private static final String STEP_CHILD = "step_child";
	private static final String SPOUSE = "spouse";
	private static final String CIVIL_SPOUSE = "civil_spouse";
	private static final String RELIGIOUS_SPOUSE = "religious_spouse";
	private static final String ENGAGED_PARTNER = "engaged_partner";

	/** The relationships that make two individuals a married couple. */
	private static final Set<String> MARRIAGE_TYPES = Set.of(SPOUSE, CIVIL_SPOUSE, RELIGIOUS_SPOUSE, "customary_spouse");
	private static final Set<String> CHILD_TYPES = Set.of(RelationshipReader.INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES);
	private static final Set<String> PARTNER_TYPES = Set.copyOf(RelationshipReader.PARTNER_TYPES);
	/** The relationships that are the same whichever of the two is the subject. */
	private static final Set<String> SYMMETRIC_TYPES;
	private static final Set<String> INDIVIDUAL_TO_INDIVIDUAL;
	static{
		final Set<String> symmetric = new LinkedHashSet<>(PARTNER_TYPES);
		symmetric.add("associate");
		SYMMETRIC_TYPES = Set.copyOf(symmetric);

		final Set<String> individualToIndividual = new LinkedHashSet<>(Arrays.asList(RelationshipReader.INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES));
		individualToIndividual.addAll(Arrays.asList(RelationshipReader.INDIVIDUAL_TO_INDIVIDUAL_SOCIAL_TYPES));
		//the generic spouse is what an import from GEDCOM gives to a marriage that does not say which kind it is
		individualToIndividual.add(SPOUSE);
		INDIVIDUAL_TO_INDIVIDUAL = Set.copyOf(individualToIndividual);
	}
	private static final Set<String> INDIVIDUAL_TO_GROUP = Set.of(RelationshipReader.INDIVIDUAL_TO_GROUP_TYPES);
	private static final Set<String> GROUP_TO_GROUP = Set.of(RelationshipReader.GROUP_TO_GROUP_TYPES);

	private static final String EVENT_MARRIAGE = "marriage";
	private static final String EVENT_BANNS = "marriage_bann";
	private static final String EVENT_LICENSE = "marriage_license";
	private static final String EVENT_ADOPTION = "adoption";
	private static final Set<String> MARRIAGE_EVENTS = Set.of(EVENT_MARRIAGE, EVENT_BANNS, EVENT_LICENSE, "marriage_contract",
		"marriage_settlement");

	/** The most records a finding about many relationships lists. */
	static final int MAX_LISTED = 200;


	private RelationshipChecker(){}


	/** @return the findings, without the drafts of the relationships */
	static List<Diagnostic> check(final RelationshipData data){
		return findings(data).stream()
			.map(RelationshipFinding::diagnostic)
			.toList();
	}

	/** @return the findings, each with the draft of the relationship that would settle it (if it is about a missing one) */
	static List<RelationshipFinding> findings(final RelationshipData data){
		final List<RelationshipFinding> findings = new ArrayList<>();
		final Index index = new Index(data);
		checkSelf(data, findings);
		checkParticipants(data, findings);
		checkDuplicates(data, findings);
		checkDates(data, findings);
		checkRoles(data, findings);
		final Set<String> explained = new LinkedHashSet<>();
		checkChildTypes(data, index, findings, explained);
		checkSpouseTypes(data, index, findings, explained);
		checkGenericTypes(data, findings, explained);
		checkMissingMarriages(data, index, findings);
		checkEngagements(data, index, findings);
		return findings;
	}


	/** The relationships between two individuals, and the events they both took part in. */
	private static final class Index{

		private final Map<String, List<RelationshipData.Relationship>> byPair = new HashMap<>();
		private final Map<String, List<RelationshipData.Relationship>> bySubject = new HashMap<>();
		private final Map<String, List<RelationshipData.Event>> byPairEvents = new HashMap<>();


		Index(final RelationshipData data){
			for(final RelationshipData.Relationship relationship : data.relationships()){
				if(relationship.subjectKind() != RelationshipData.Kind.INDIVIDUAL
						|| relationship.targetKind() != RelationshipData.Kind.INDIVIDUAL)
					continue;

				byPair.computeIfAbsent(pair(relationship.subjectId(), relationship.targetId()), k -> new ArrayList<>())
					.add(relationship);
				bySubject.computeIfAbsent(relationship.subjectId(), k -> new ArrayList<>())
					.add(relationship);
			}
			for(final RelationshipData.Event event : data.events()){
				final List<String> people = event.participants().stream()
					.map(RelationshipData.Participant::personId)
					.distinct()
					.toList();
				for(int i = 0; i < people.size(); i ++)
					for(int j = i + 1; j < people.size(); j ++)
						byPairEvents.computeIfAbsent(pair(people.get(i), people.get(j)), k -> new ArrayList<>())
							.add(event);
			}
		}

		List<RelationshipData.Relationship> between(final String first, final String second){
			return byPair.getOrDefault(pair(first, second), List.of());
		}

		List<RelationshipData.Relationship> of(final String subjectId){
			return bySubject.getOrDefault(subjectId, List.of());
		}

		List<RelationshipData.Event> eventsOf(final String first, final String second){
			return byPairEvents.getOrDefault(pair(first, second), List.of());
		}

	}

	private static String pair(final String first, final String second){
		return (first.compareTo(second) <= 0? first + "|" + second: second + "|" + first);
	}


	private static void checkSelf(final RelationshipData data, final List<RelationshipFinding> findings){
		for(final RelationshipData.Relationship relationship : data.relationships())
			if(relationship.subjectId().equals(relationship.targetId()))
				findings.add(finding(Diagnostic.Severity.ERROR, "self_relationship", List.of(relationship.subjectId(), relationship.id()),
					null, name(data, relationship.subjectId()), relationship.type()));
	}

	/** A relationship whose type does not fit what it links (a spouse that is a group, a membership between two individuals). */
	private static void checkParticipants(final RelationshipData data, final List<RelationshipFinding> findings){
		for(final RelationshipData.Relationship relationship : data.relationships()){
			if(relationship.subjectId().equals(relationship.targetId()))
				continue;

			final Set<String> valid = validTypes(relationship.subjectKind(), relationship.targetKind());
			if(!valid.contains(relationship.type()))
				findings.add(finding(Diagnostic.Severity.WARNING, "invalid_for_participants",
					List.of(relationship.subjectId(), relationship.targetId(), relationship.id()), null,
					relationship.type(), relationship.subjectKind().name().toLowerCase(java.util.Locale.ROOT),
					relationship.targetKind().name().toLowerCase(java.util.Locale.ROOT), name(data, relationship.subjectId()),
					name(data, relationship.targetId())));
		}
	}

	private static Set<String> validTypes(final RelationshipData.Kind subject, final RelationshipData.Kind target){
		if(subject == RelationshipData.Kind.INDIVIDUAL)
			return (target == RelationshipData.Kind.INDIVIDUAL? INDIVIDUAL_TO_INDIVIDUAL: INDIVIDUAL_TO_GROUP);
		return (target == RelationshipData.Kind.GROUP? GROUP_TO_GROUP: Set.of());
	}

	/** The same relationship written more than once (a pair of spouses written from both sides is the same relationship). */
	private static void checkDuplicates(final RelationshipData data, final List<RelationshipFinding> findings){
		final Map<String, List<RelationshipData.Relationship>> same = new LinkedHashMap<>();
		for(final RelationshipData.Relationship relationship : data.relationships()){
			if(relationship.subjectId().equals(relationship.targetId()))
				continue;

			final boolean symmetric = SYMMETRIC_TYPES.contains(relationship.type());
			final String key = (symmetric
				? pair(relationship.subjectId(), relationship.targetId())
				: relationship.subjectId() + ">" + relationship.targetId()) + "|" + relationship.type();
			same.computeIfAbsent(key, k -> new ArrayList<>())
				.add(relationship);
		}

		for(final List<RelationshipData.Relationship> duplicates : same.values()){
			if(duplicates.size() < 2)
				continue;

			final RelationshipData.Relationship first = duplicates.getFirst();
			final List<String> ids = new ArrayList<>(List.of(first.subjectId(), first.targetId()));
			duplicates.forEach(r -> ids.add(r.id()));
			findings.add(finding(Diagnostic.Severity.WARNING, "duplicate_relationship", ids, null, name(data, first.subjectId()),
				name(data, first.targetId()), first.type(), duplicates.size()));
		}
	}

	private static void checkDates(final RelationshipData data, final List<RelationshipFinding> findings){
		for(final RelationshipData.Relationship relationship : data.relationships())
			if(relationship.from() != null && relationship.to() != null && relationship.to().isBefore(relationship.from()))
				findings.add(finding(Diagnostic.Severity.ERROR, "dates_reversed",
					List.of(relationship.subjectId(), relationship.targetId(), relationship.id()), null, relationship.type(),
					name(data, relationship.subjectId()), name(data, relationship.targetId()), relationship.fromText()));
	}

	/** The role of the subject says nothing more than the type does, for the relationships of the family. */
	private static void checkRoles(final RelationshipData data, final List<RelationshipFinding> findings){
		for(final RelationshipData.Relationship relationship : data.relationships())
			if(relationship.role() != null && !relationship.role().isBlank()
					&& (CHILD_TYPES.contains(relationship.type()) || PARTNER_TYPES.contains(relationship.type())))
				findings.add(finding(Diagnostic.Severity.INFORMATION, "unnecessary_role",
					List.of(relationship.subjectId(), relationship.targetId(), relationship.id()), null, relationship.type(),
					relationship.role(), name(data, relationship.subjectId()), name(data, relationship.targetId())));
	}


	/**
	 * A child of the generic type, or recorded as biological, whose parent the evidence points to be something else: an adoption in
	 * which both took part, or a parent married to the other (biological) parent after the child was born, that is a step-parent.
	 *
	 * @param explained the relationships already given a suggestion, that the finding about the generic types leaves out
	 */
	private static void checkChildTypes(final RelationshipData data, final Index index,
			final List<RelationshipFinding> findings, final Set<String> explained){
		for(final RelationshipData.Relationship relationship : data.relationships()){
			if(!CHILD.equals(relationship.type()) && !BIOLOGICAL_CHILD.equals(relationship.type()))
				continue;
			if(relationship.subjectKind() != RelationshipData.Kind.INDIVIDUAL
					|| relationship.targetKind() != RelationshipData.Kind.INDIVIDUAL
					|| relationship.subjectId().equals(relationship.targetId()))
				continue;

			final String child = relationship.subjectId();
			final String parent = relationship.targetId();
			final boolean generic = CHILD.equals(relationship.type());

			final RelationshipData.Event adoption = index.eventsOf(child, parent).stream()
				.filter(e -> EVENT_ADOPTION.equals(e.type()))
				.findFirst()
				.orElse(null);
			if(adoption != null){
				findings.add(finding((generic? Diagnostic.Severity.INFORMATION: Diagnostic.Severity.WARNING),
					(generic? "suggest_adoptive": "conflicting_adoption"), List.of(child, parent, relationship.id(), adoption.id()),
					null, name(data, child), name(data, parent), adoption.id(), adoption.dateText()));
				explained.add(relationship.id());
				continue;
			}

			if(generic){
				final RelationshipData.Person childPerson = data.persons().get(child);
				if(childPerson == null || childPerson.birth() == null)
					continue;

				for(final RelationshipData.Relationship other : index.of(child)){
					final String otherParent = other.targetId();
					if(otherParent.equals(parent) || other.id().equals(relationship.id())
							|| !(CHILD.equals(other.type()) || BIOLOGICAL_CHILD.equals(other.type())))
						continue;

					//the parent married the other parent when the child was already born
					final RelationshipData.Relationship marriage = index.between(parent, otherParent).stream()
						.filter(u -> PARTNER_TYPES.contains(u.type()) && u.from() != null
							&& u.from().min() > childPerson.birth().max())
						.findFirst()
						.orElse(null);
					if(marriage != null){
						findings.add(finding(Diagnostic.Severity.INFORMATION, "suggest_step",
							List.of(child, parent, otherParent, relationship.id(), marriage.id()), null, name(data, child),
							name(data, parent), name(data, otherParent), marriage.fromText()));
						explained.add(relationship.id());
						break;
					}
				}
			}
		}
	}

	/** A spouse of the generic type: the banns and the license of the marriage say which kind of marriage it is, if they are known. */
	private static void checkSpouseTypes(final RelationshipData data, final Index index,
			final List<RelationshipFinding> findings, final Set<String> explained){
		for(final RelationshipData.Relationship relationship : data.relationships()){
			if(!SPOUSE.equals(relationship.type()) || relationship.subjectKind() != RelationshipData.Kind.INDIVIDUAL
					|| relationship.targetKind() != RelationshipData.Kind.INDIVIDUAL
					|| relationship.subjectId().equals(relationship.targetId()))
				continue;

			final String suggested = marriageTypeOf(index.eventsOf(relationship.subjectId(), relationship.targetId()));
			if(suggested == null)
				continue;

			final String evidence = (CIVIL_SPOUSE.equals(suggested)? EVENT_LICENSE: EVENT_BANNS);
			findings.add(finding(Diagnostic.Severity.INFORMATION, "suggest_spouse",
				List.of(relationship.subjectId(), relationship.targetId(), relationship.id()), null,
				name(data, relationship.subjectId()), name(data, relationship.targetId()), suggested, evidence));
			explained.add(relationship.id());
		}
	}

	/**
	 * The kind of marriage the events of a couple point to: the banns are announced by the church (religious), the license is issued
	 * by the civil authority (civil). {@code null} if they do not tell, or they point to both.
	 */
	private static String marriageTypeOf(final List<RelationshipData.Event> events){
		final boolean banns = events.stream().anyMatch(e -> EVENT_BANNS.equals(e.type()));
		final boolean license = events.stream().anyMatch(e -> EVENT_LICENSE.equals(e.type()));
		if(banns == license)
			return null;
		return (banns? RELIGIOUS_SPOUSE: CIVIL_SPOUSE);
	}

	/**
	 * The relationships of a generic type that the evidence does not help to specify: one finding for all of them, not one for each
	 * (an import from GEDCOM gives the generic type to every marriage that does not say which kind it is).
	 */
	private static void checkGenericTypes(final RelationshipData data, final List<RelationshipFinding> findings,
			final Set<String> explained){
		for(final String type : List.of(SPOUSE, CHILD)){
			final List<RelationshipData.Relationship> generic = data.relationships().stream()
				.filter(r -> type.equals(r.type()) && !explained.contains(r.id())
					&& r.subjectKind() == RelationshipData.Kind.INDIVIDUAL && r.targetKind() == RelationshipData.Kind.INDIVIDUAL
					&& !r.subjectId().equals(r.targetId()))
				.toList();
			if(generic.isEmpty())
				continue;

			final List<String> ids = generic.stream()
				.map(RelationshipData.Relationship::id)
				.limit(MAX_LISTED)
				.toList();
			findings.add(new RelationshipFinding(new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.RELATIONSHIPS,
				I18N.tf("relationship.generic_" + type + ".title", generic.size()),
				I18N.tf("relationship.generic_" + type + ".explanation", generic.size(), Math.min(generic.size(), MAX_LISTED)),
				ids, null, DiagnosticKeys.of("relationship.generic_" + type, List.of(), generic.size())), null));
		}
	}

	/** A marriage event of two individuals that no relationship of marriage links. */
	private static void checkMissingMarriages(final RelationshipData data, final Index index,
			final List<RelationshipFinding> findings){
		final Map<String, List<RelationshipData.Event>> marriages = new LinkedHashMap<>();
		final Map<String, List<String>> couples = new LinkedHashMap<>();
		for(final RelationshipData.Event event : data.events()){
			if(!EVENT_MARRIAGE.equals(event.type()))
				continue;

			final List<String> principals = event.participants().stream()
				.filter(RelationshipData.Participant::principal)
				.map(RelationshipData.Participant::personId)
				.filter(data.persons()::containsKey)
				.distinct()
				.toList();
			if(principals.size() != 2)
				continue;

			final String key = pair(principals.get(0), principals.get(1));
			marriages.computeIfAbsent(key, k -> new ArrayList<>())
				.add(event);
			couples.putIfAbsent(key, principals);
		}

		for(final Map.Entry<String, List<RelationshipData.Event>> entry : marriages.entrySet()){
			final List<String> couple = couples.get(entry.getKey());
			final boolean linked = index.between(couple.get(0), couple.get(1)).stream()
				.anyMatch(r -> MARRIAGE_TYPES.contains(r.type()));
			if(linked)
				continue;

			final List<String> ids = new ArrayList<>(couple);
			entry.getValue().forEach(e -> ids.add(e.id()));
			final String type = marriageTypeOf(index.eventsOf(couple.get(0), couple.get(1)));
			findings.add(finding(Diagnostic.Severity.INFORMATION, "marriage_without_relationship", ids,
				new RelationshipDraft(couple.get(0), couple.get(1), type), name(data, couple.get(0)), name(data, couple.get(1)),
				entry.getValue().getFirst().dateText()));
		}
	}

	/** An engagement that the marriage of the couple has not closed. */
	private static void checkEngagements(final RelationshipData data, final Index index, final List<RelationshipFinding> findings){
		for(final RelationshipData.Relationship engagement : data.relationships()){
			if(!ENGAGED_PARTNER.equals(engagement.type()) || engagement.ended() || engagement.to() != null
					|| engagement.subjectKind() != RelationshipData.Kind.INDIVIDUAL
					|| engagement.targetKind() != RelationshipData.Kind.INDIVIDUAL)
				continue;

			final String first = engagement.subjectId();
			final String second = engagement.targetId();
			final RelationshipData.Relationship marriage = index.between(first, second).stream()
				.filter(r -> MARRIAGE_TYPES.contains(r.type()))
				.findFirst()
				.orElse(null);
			final RelationshipData.Event event = (marriage == null
				? index.eventsOf(first, second).stream().filter(e -> EVENT_MARRIAGE.equals(e.type())).findFirst().orElse(null)
				: null);
			if(marriage == null && event == null)
				continue;

			findings.add(finding(Diagnostic.Severity.INFORMATION, "open_engagement",
				List.of(first, second, engagement.id(), (marriage != null? marriage.id(): event.id())), null, name(data, first),
				name(data, second)));
		}
	}


	private static String name(final RelationshipData data, final String id){
		final RelationshipData.Person person = data.persons().get(id);
		if(person != null)
			return person.name();

		return data.groups().getOrDefault(id, id);
	}

	/**
	 * @param draft the relationship that would settle the finding ({@code null} if there is none)
	 */
	private static RelationshipFinding finding(final Diagnostic.Severity severity, final String key, final List<String> recordIds,
			final RelationshipDraft draft, final Object... arguments){
		return new RelationshipFinding(new Diagnostic(severity, AssistantCategory.RELATIONSHIPS,
			I18N.tf("relationship." + key + ".title", arguments), I18N.tf("relationship." + key + ".explanation", arguments),
			recordIds, null, DiagnosticKeys.of("relationship." + key, recordIds, arguments)), draft);
	}

}
