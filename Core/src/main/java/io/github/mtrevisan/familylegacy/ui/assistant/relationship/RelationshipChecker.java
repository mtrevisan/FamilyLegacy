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

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateRange;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticKeys;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;


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
	private static final String STEP_CHILD = RelationshipReader.ENUM_TYPE_STEP_CHILD;
	private static final String SPOUSE = RelationshipReader.ENUM_TYPE_SPOUSE;
	private static final String ASSOCIATE = RelationshipReader.ENUM_TYPE_ASSOCIATE;
	private static final String INFORMANT_ROLE = EventParticipationReader.ENUM_ROLE_INFORMANT;
	private static final String CIVIL_SPOUSE = RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE;
	private static final String RELIGIOUS_SPOUSE = RelationshipReader.ENUM_TYPE_RELIGIOUS_SPOUSE;
	private static final String ENGAGED_PARTNER = RelationshipReader.ENUM_TYPE_ENGAGED_PARTNER;

	/** The relationships that make two individuals a married couple. */
	private static final Set<String> MARRIAGE_TYPES = Set.of(SPOUSE, CIVIL_SPOUSE, RELIGIOUS_SPOUSE, RelationshipReader.ENUM_TYPE_CUSTOMARY_SPOUSE);
	private static final Set<String> CHILD_TYPES = Set.of(RelationshipReader.INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES);
	private static final Set<String> PARTNER_TYPES = Set.copyOf(RelationshipReader.PARTNER_TYPES);
	/** The relationships that are the same whichever of the two is the subject. */
	private static final Set<String> SYMMETRIC_TYPES;
	private static final Set<String> INDIVIDUAL_TO_INDIVIDUAL = Set.of(RelationshipReader.INDIVIDUAL_TO_INDIVIDUAL_TYPES);
	static{
		final Set<String> symmetric = new LinkedHashSet<>(PARTNER_TYPES);
		symmetric.add(RelationshipReader.ENUM_TYPE_ASSOCIATE);
		SYMMETRIC_TYPES = Set.copyOf(symmetric);
	}
	private static final Set<String> INDIVIDUAL_TO_GROUP = Set.of(RelationshipReader.INDIVIDUAL_TO_GROUP_TYPES);
	private static final Set<String> GROUP_TO_GROUP = Set.of(RelationshipReader.GROUP_TO_GROUP_TYPES);

	private static final String EVENT_MARRIAGE = EventReader.ENUM_TYPE_MARRIAGE;
	private static final String EVENT_BANNS = EventReader.ENUM_TYPE_MARRIAGE_BANN;
	private static final String EVENT_LICENSE = EventReader.ENUM_TYPE_MARRIAGE_LICENSE;
	private static final String EVENT_ADOPTION = EventReader.ENUM_TYPE_ADOPTION;
	private static final Set<String> MARRIAGE_EVENTS = Set.of(EVENT_MARRIAGE, EVENT_BANNS, EVENT_LICENSE, EventReader.ENUM_TYPE_MARRIAGE_CONTRACT,
		EventReader.ENUM_TYPE_MARRIAGE_SETTLEMENT);

	/** The longest time between the death of the father and the birth of the child, in days. */
	private static final int GESTATION_DAYS = 300;



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
		checkParentage(data, findings);
		checkRoles(data, findings);
		final Set<String> explained = new LinkedHashSet<>();
		checkChildTypes(data, index, findings, explained);
		checkSpouseTypes(data, index, findings, explained);
		checkGenericTypes(data, findings, explained);
		checkMissingMarriages(data, index, findings);
		checkEngagements(data, index, findings);
		checkPartnersOfParents(data, index, findings);
		checkBirthInformants(data, index, findings);
		checkSiblingRoles(data, index, findings);
		checkAssociates(data, index, findings);
		return findings;
	}


	/** The relationships between two individuals, and the events they both took part in. */
	private static final class Index{

		private final Map<String, List<RelationshipData.Relationship>> byPair = new HashMap<>();
		private final Map<String, List<RelationshipData.Relationship>> bySubject = new HashMap<>();
		private final Map<String, List<RelationshipData.Relationship>> byPerson = new HashMap<>();
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
				byPerson.computeIfAbsent(relationship.subjectId(), k -> new ArrayList<>())
					.add(relationship);
				if(!relationship.targetId().equals(relationship.subjectId()))
					byPerson.computeIfAbsent(relationship.targetId(), k -> new ArrayList<>())
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

		/** @return the relationships between individuals that have the individual as the subject or as the target */
		List<RelationshipData.Relationship> involving(final String personId){
			return byPerson.getOrDefault(personId, List.of());
		}

		/** @return the biological parents of the individual, as far as the relationships say (generic child included) */
		Set<String> biologicalParents(final String childId){
			final Set<String> parents = new LinkedHashSet<>();
			for(final RelationshipData.Relationship relationship : of(childId))
				if((CHILD.equals(relationship.type()) || BIOLOGICAL_CHILD.equals(relationship.type()))
						&& !relationship.targetId().equals(childId))
					parents.add(relationship.targetId());
			return parents;
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
					null, name(data, relationship.subjectId()), relationship.type(), relationship.id()));
	}

	/** A relationship whose type does not fit what it links (a spouse that is a group, a membership between two individuals). */
	private static void checkParticipants(final RelationshipData data, final List<RelationshipFinding> findings){
		for(final RelationshipData.Relationship relationship : data.relationships()){
			if(relationship.subjectId().equals(relationship.targetId()))
				continue;

			final Set<String> valid = validTypes(relationship.subjectKind(), relationship.targetKind());
			if(!valid.contains(relationship.type()))
				findings.add(finding(Diagnostic.Severity.ERROR, "invalid_for_participants",
					List.of(relationship.subjectId(), relationship.targetId(), relationship.id()), null,
					relationship.type(), relationship.subjectKind().name().toLowerCase(Locale.ROOT),
					relationship.targetKind().name().toLowerCase(Locale.ROOT), name(data, relationship.subjectId()),
					name(data, relationship.targetId()), relationship.id()));
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

			//an association with a role is not symmetric: the role is the one of the subject, so the same two written from both sides
				//with different roles (e.g. "witness" and "newborn") are different relationships
				final boolean hasRole = (ASSOCIATE.equals(relationship.type()) && relationship.role() != null
					&& !relationship.role().isBlank());
				final boolean symmetric = (SYMMETRIC_TYPES.contains(relationship.type()) && !hasRole);
			final String key = (symmetric
				? pair(relationship.subjectId(), relationship.targetId())
				: relationship.subjectId() + ">" + relationship.targetId()) + "|" + relationship.type()
					+ (hasRole? "|" + relationship.role().strip().toLowerCase(Locale.ROOT): "");
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
				name(data, first.targetId()), first.type(), duplicates.size(), idsOf(duplicates)));
		}
	}

	private static void checkDates(final RelationshipData data, final List<RelationshipFinding> findings){
		for(final RelationshipData.Relationship relationship : data.relationships())
			if(relationship.from() != null && relationship.to() != null && relationship.to().isBefore(relationship.from()))
				findings.add(finding(Diagnostic.Severity.ERROR, "dates_reversed",
					List.of(relationship.subjectId(), relationship.targetId(), relationship.id()), null, relationship.type(),
					name(data, relationship.subjectId()), name(data, relationship.targetId()), relationship.fromText(), relationship.id()));
	}

	/**
	 * What the biological parentage cannot be together with: a child and the parent who are also spouses, and a parent who is also a
	 * sibling of the child (they share a biological parent). Both are provable, so errors, as in Validate File: only the
	 * {@code biological_child} type counts (a step-parent or an adoptive parent is not biological).
	 */
	private static void checkParentage(final RelationshipData data, final List<RelationshipFinding> findings){
		//the biological parents of each child, with the relationship that says so
		final Map<String, Map<String, List<String>>> parentsOf = new LinkedHashMap<>();
		final Map<String, List<String>> marriages = new HashMap<>();
		for(final RelationshipData.Relationship relationship : data.relationships()){
			if(relationship.subjectKind() != RelationshipData.Kind.INDIVIDUAL || relationship.targetKind() != RelationshipData.Kind.INDIVIDUAL
					|| relationship.subjectId().equals(relationship.targetId()))
				continue;

			if(BIOLOGICAL_CHILD.equals(relationship.type()))
				parentsOf.computeIfAbsent(relationship.subjectId(), k -> new LinkedHashMap<>())
					.computeIfAbsent(relationship.targetId(), k -> new ArrayList<>())
					.add(relationship.id());
			else if(MARRIAGE_TYPES.contains(relationship.type()))
				marriages.computeIfAbsent(pairKey(relationship.subjectId(), relationship.targetId()), k -> new ArrayList<>())
					.add(relationship.id());
		}

		for(final Map.Entry<String, Map<String, List<String>>> entry : new TreeMap<>(parentsOf).entrySet()){
			final String childId = entry.getKey();
			final Map<String, List<String>> parents = entry.getValue();
			for(final Map.Entry<String, List<String>> parent : parents.entrySet()){
				final String parentId = parent.getKey();
				final List<String> marriage = marriages.get(pairKey(childId, parentId));
				if(marriage != null){
					//every relationship of the pair is named, none is left out
					final List<String> ids = new ArrayList<>(List.of(childId, parentId));
					ids.addAll(parent.getValue());
					ids.addAll(marriage);
					findings.add(finding(Diagnostic.Severity.ERROR, "child_married_to_parent", ids, null, name(data, childId),
						name(data, parentId), String.join(", ", parent.getValue()), String.join(", ", marriage)));
				}

				final Map<String, List<String>> grandparents = parentsOf.get(parentId);
				if(grandparents == null)
					continue;

				final Set<String> shared = new LinkedHashSet<>(grandparents.keySet());
				shared.retainAll(parents.keySet());
				if(!shared.isEmpty()){
					final String commonId = shared.iterator().next();
					final List<String> ids = new ArrayList<>(List.of(childId, parentId, commonId));
					ids.addAll(parent.getValue());
					ids.addAll(grandparents.get(commonId));
					ids.addAll(parents.get(commonId));
					findings.add(finding(Diagnostic.Severity.ERROR, "sibling_and_parent", ids, null,
						name(data, childId), name(data, parentId), name(data, commonId), String.join(", ", parent.getValue()),
						String.join(", ", grandparents.get(commonId)), String.join(", ", parents.get(commonId))));
				}
			}
		}
	}

	/**
	 * Relationships between the same two individuals that a text refers to: the checks use the first one, and the text gives the
	 * identifier of every one, so that none is left out.
	 */
	private record Group(List<RelationshipData.Relationship> all){

		boolean isEmpty(){
			return all.isEmpty();
		}

		RelationshipData.Relationship first(){
			return all.getFirst();
		}

		/** @return the identifiers, to put in a text between brackets (several are separated by commas) */
		String ids(){
			return String.join(", ", idList());
		}

		List<String> idList(){
			return all.stream()
				.map(RelationshipData.Relationship::id)
				.toList();
		}

	}

	private static Group group(final Stream<RelationshipData.Relationship> candidates){
		return new Group(candidates.toList());
	}

	/** @return the identifiers of the relationships, to put in a text between brackets (several are separated by commas) */
	private static String idsOf(final List<RelationshipData.Relationship> relationships){
		return String.join(", ", relationships.stream()
			.map(RelationshipData.Relationship::id)
			.toList());
	}


	private static String pairKey(final String first, final String second){
		return (first.compareTo(second) <= 0? first + "|" + second: second + "|" + first);
	}

	/** The role of the subject says nothing more than the type does, for the relationships of the family. */
	private static void checkRoles(final RelationshipData data, final List<RelationshipFinding> findings){
		for(final RelationshipData.Relationship relationship : data.relationships())
			if(relationship.role() != null && !relationship.role().isBlank()
					&& (CHILD_TYPES.contains(relationship.type()) || PARTNER_TYPES.contains(relationship.type())))
				findings.add(finding(Diagnostic.Severity.INFORMATION, "unnecessary_role",
					List.of(relationship.subjectId(), relationship.targetId(), relationship.id()), null, relationship.type(),
					relationship.role(), name(data, relationship.subjectId()), name(data, relationship.targetId()), relationship.id()));
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
					null, name(data, child), name(data, parent), adoption.id(), adoption.dateText(), relationship.id())
					.withTypeChange(new RelationshipTypeChange(relationship.id(), ADOPTIVE_CHILD)));
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
					final Group marriages = group(index.between(parent, otherParent).stream()
						.filter(u -> PARTNER_TYPES.contains(u.type()) && u.from() != null
							&& u.from().min() > childPerson.birth().max()));
					if(!marriages.isEmpty()){
						final List<String> ids = new ArrayList<>(List.of(child, parent, otherParent, relationship.id()));
						ids.addAll(marriages.idList());
						findings.add(finding(Diagnostic.Severity.INFORMATION, "suggest_step", ids, null, name(data, child),
							name(data, parent), name(data, otherParent), marriages.first().fromText(), relationship.id(), marriages.ids())
							.withTypeChange(new RelationshipTypeChange(relationship.id(), STEP_CHILD)));
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

			final List<RelationshipData.Event> events = index.eventsOf(relationship.subjectId(), relationship.targetId());
			//what the events and their sources say; if nothing, the period and the country, as a weak hint
			MarriageEvidence marriage = marriageEvidence(data, events);
			if(marriage == null)
				marriage = periodEvidence(data, relationship, events);
			if(marriage == null)
				continue;

			final String suggested = marriage.type();
			final String evidence = marriage.description();
			findings.add(finding(Diagnostic.Severity.INFORMATION, "suggest_spouse",
				List.of(relationship.subjectId(), relationship.targetId(), relationship.id()), null,
				name(data, relationship.subjectId()), name(data, relationship.targetId()), suggested, evidence, relationship.id())
				.withTypeChange(new RelationshipTypeChange(relationship.id(), suggested)));
			explained.add(relationship.id());
		}
	}

	/**
	 * @param type        the suggested type
	 * @param description what points to it, for the explanation
	 */
	private record MarriageEvidence(String type, String description){}

	/**
	 * The kind of marriage the events of a couple point to, only as a hint: the banns are announced by the church, and so are the
	 * marriage and the banns held in a place of the church (a parish) or in front of an officiant who is of the clergy (religious);
	 * the license is issued by the civil authority (civil). {@code null} if they do not tell, or they point to both.
	 */
	private static MarriageEvidence marriageEvidence(final RelationshipData data, final List<RelationshipData.Event> events){
		final List<String> religious = new ArrayList<>();
		if(events.stream().anyMatch(e -> EVENT_BANNS.equals(e.type())))
			religious.add(EVENT_BANNS);
		for(final RelationshipData.Event event : events){
			if(!EVENT_MARRIAGE.equals(event.type()) && !EVENT_BANNS.equals(event.type()))
				continue;

			if(event.placeId() != null && data.ecclesiasticalPlaces().contains(event.placeId()))
				religious.add(I18N.t("relationship.evidence.church_place"));
			for(final RelationshipData.Participant participant : event.participants())
				if(EventParticipationReader.ENUM_ROLE_OFFICIANT.equals(participant.role()) && data.clergy().contains(participant.personId()))
					religious.add(I18N.tf("relationship.evidence.clergy_officiant", name(data, participant.personId())));
		}
		//the title of a source of the marriage that says its kind ("marriage Rossi Mario + Bianchi Anna 18500112 religious")
		final List<String> civilSources = new ArrayList<>();
		for(final RelationshipData.Event event : events)
			if(EVENT_MARRIAGE.equals(event.type()) || EVENT_BANNS.equals(event.type()))
				for(final String title : event.sourceTitles()){
					final String lower = title.toLowerCase(Locale.ROOT);
					if(lower.endsWith(" religious"))
						religious.add(I18N.tf("relationship.evidence.source_title", title));
					else if(lower.endsWith(" civil"))
						civilSources.add(I18N.tf("relationship.evidence.source_title", title));
				}
		final boolean license = events.stream().anyMatch(e -> EVENT_LICENSE.equals(e.type()));
		final boolean civil = (license || !civilSources.isEmpty());
		if(religious.isEmpty() == !civil)
			return null;

		return (civil? new MarriageEvidence(CIVIL_SPOUSE, (license? EVENT_LICENSE: civilSources.getFirst()))
			: new MarriageEvidence(RELIGIOUS_SPOUSE, String.join(", ", religious.stream().distinct().toList())));
	}

	/** The first day of 1806, the year the civil marriage of the Napoleonic Kingdom of Italy came into force (a Julian Day Number). */
	private static final long NAPOLEONIC_CODE = LocalDate.of(1806, 1, 1).toEpochDay() + 2_440_588L;
	/** The first day of 1816, when the Napoleonic code ceased to apply in the states of the Restoration. */
	private static final long RESTORATION = LocalDate.of(1816, 1, 1).toEpochDay() + 2_440_588L;
	/** The first day of 1866, from which the civil marriage of the Kingdom of Italy applied (1871 in the Veneto). */
	private static final long KINGDOM_OF_ITALY = LocalDate.of(1866, 1, 1).toEpochDay() + 2_440_588L;

	/**
	 * A weak hint, when the events and their sources say nothing: in Italy only the religious marriage was recognised before the
	 * Napoleonic code (1806) and from the Restoration (1816) until the Kingdom of Italy (1866), so a marriage dated in those periods in a
	 * place of Italy was most probably a religious one. It is a hint about the law of the territory, not evidence about this marriage: the
	 * dates and the territory must be checked, so it says so.
	 *
	 * @return {@code null} if the place is not in Italy, or the date is unknown or in another period
	 */
	private static MarriageEvidence periodEvidence(final RelationshipData data, final RelationshipData.Relationship relationship,
			final List<RelationshipData.Event> events){
		DateRange date = relationship.from();
		String dateText = relationship.fromText();
		String placeId = null;
		for(final RelationshipData.Event event : events)
			if(EVENT_MARRIAGE.equals(event.type()) && event.placeId() != null){
				placeId = event.placeId();
				if(date == null){
					date = event.date();
					dateText = event.dateText();
				}
				break;
			}
		if(date == null || placeId == null)
			return null;

		final String country = data.countries().get(placeId);
		if(!"italia".equals(country) && !"italy".equals(country))
			return null;

		final boolean beforeCode = (date.max() < NAPOLEONIC_CODE);
		final boolean restoration = (date.min() >= RESTORATION && date.max() < KINGDOM_OF_ITALY);
		return ((beforeCode || restoration)
			? new MarriageEvidence(RELIGIOUS_SPOUSE, I18N.tf("relationship.evidence.period_italy", dateText))
			: null);
	}

	/**
	 * The relationships of a generic type that the evidence does not help to specify: one finding for each of them
	 * (an import from GEDCOM gives the generic type to every marriage that does not say which kind it is), grouped by kind in the palette.
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

			for(final RelationshipData.Relationship relationship : generic){
				final List<String> ids = List.of(relationship.subjectId(), relationship.targetId(), relationship.id());
				final Object[] arguments = {name(data, relationship.subjectId()), name(data, relationship.targetId()), relationship.id()};
				findings.add(new RelationshipFinding(new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.RELATIONSHIPS,
					I18N.tf("relationship.generic_" + type + ".title", arguments),
					I18N.tf("relationship.generic_" + type + ".explanation", arguments),
					ids, null, DiagnosticKeys.of("relationship.generic_" + type, ids)), null));
			}
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
			final MarriageEvidence evidence = marriageEvidence(data, index.eventsOf(couple.get(0), couple.get(1)));
			final String type = (evidence != null? evidence.type(): null);
			findings.add(finding(Diagnostic.Severity.INFORMATION, "marriage_without_relationship", ids,
				new RelationshipDraft(couple.get(0), couple.get(1), type, entry.getValue().getFirst().id(), true), name(data, couple.get(0)),
				name(data, couple.get(1)),
				entry.getValue().getFirst().dateText(), entry.getValue().getFirst().id()));
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
			final Group marriages = group(index.between(first, second).stream()
				.filter(r -> MARRIAGE_TYPES.contains(r.type())));
			final RelationshipData.Event event = (marriages.isEmpty()
				? index.eventsOf(first, second).stream().filter(e -> EVENT_MARRIAGE.equals(e.type())).findFirst().orElse(null)
				: null);
			if(marriages.isEmpty() && event == null)
				continue;

			final List<String> ids = new ArrayList<>(List.of(first, second, engagement.id()));
			if(!marriages.isEmpty())
				ids.addAll(marriages.idList());
			else
				ids.add(event.id());
			findings.add(finding(Diagnostic.Severity.INFORMATION, "open_engagement", ids, null, name(data, first),
				name(data, second), engagement.id(), (!marriages.isEmpty()? marriages.ids(): event.id())));
		}
	}


	/**
	 * The spouse of a parent that has no relationship with the child: it may be another parent (the relationship is missing), a
	 * step-parent, or nothing to do with the child. It is a question, not an error: the draft starts the relationship without a type,
	 * for the researcher to choose it. A union that ended before the child was born is not asked about.
	 */
	private static void checkPartnersOfParents(final RelationshipData data, final Index index, final List<RelationshipFinding> findings){
		final Set<String> asked = new LinkedHashSet<>();
		//a child with two biological parents already has the ones it can have: another partner of a parent is not a missing parent
		final Map<String, Set<String>> biologicalParents = new HashMap<>();
		for(final RelationshipData.Relationship tie : data.relationships())
			if((BIOLOGICAL_CHILD.equals(tie.type()) || RelationshipReader.ENUM_TYPE_CHILD.equals(tie.type()))
					&& tie.subjectKind() == RelationshipData.Kind.INDIVIDUAL && tie.targetKind() == RelationshipData.Kind.INDIVIDUAL)
				biologicalParents.computeIfAbsent(tie.subjectId(), k -> new LinkedHashSet<>())
					.add(tie.targetId());
		for(final RelationshipData.Relationship tie : data.relationships()){
			if(!CHILD_TYPES.contains(tie.type()) || tie.subjectKind() != RelationshipData.Kind.INDIVIDUAL
					|| tie.targetKind() != RelationshipData.Kind.INDIVIDUAL || tie.subjectId().equals(tie.targetId()))
				continue;

			final String child = tie.subjectId();
			final String parent = tie.targetId();
			final RelationshipData.Person childPerson = data.persons().get(child);
			for(final RelationshipData.Relationship union : index.involving(parent)){
				if(!MARRIAGE_TYPES.contains(union.type()))
					continue;

				final String partner = (union.subjectId().equals(parent)? union.targetId(): union.subjectId());
				if(partner.equals(child) || partner.equals(parent) || !data.persons().containsKey(partner)
						|| !index.between(child, partner).isEmpty() || !asked.add(child + ">" + partner))
					continue;
				if(union.to() != null && childPerson != null && childPerson.birth() != null && union.to().max() < childPerson.birth().min())
					continue;
				if(biologicalParents.getOrDefault(child, Set.of()).size() >= 2)
					continue;
				//a partner who died before the child was conceived can be neither another parent nor a step-parent
				final RelationshipData.Person partnerPerson = data.persons().get(partner);
				if(partnerPerson != null && partnerPerson.death() != null && childPerson != null && childPerson.birth() != null
						&& partnerPerson.death().max() + GESTATION_DAYS < childPerson.birth().min())
					continue;

				findings.add(finding(Diagnostic.Severity.INFORMATION, "partner_of_parent_not_linked",
					List.of(child, partner, parent, union.id()), new RelationshipDraft(child, partner, null), name(data, child),
					name(data, partner), name(data, parent), union.id()));
			}
		}
	}

	/**
	 * Who declared a birth (the informant) and has no relationship with the child: often a parent, but it may be a relative, the
	 * midwife or a neighbour, so it is a question. The protocol does not let a role say kinship, so nothing is concluded from it: the
	 * draft starts the relationship with no type, cites what the birth event cites, and the researcher chooses. An informant who was
	 * certainly born after the child is not asked about.
	 */
	private static void checkBirthInformants(final RelationshipData data, final Index index, final List<RelationshipFinding> findings){
		final Set<String> asked = new LinkedHashSet<>();
		for(final RelationshipData.Event event : data.events()){
			if(!EventReader.ENUM_TYPE_BIRTH.equals(event.type()))
				continue;

			final List<String> children = event.participants().stream()
				.filter(RelationshipData.Participant::principal)
				.map(RelationshipData.Participant::personId)
				.distinct()
				.toList();
			if(children.size() != 1)
				continue;

			final String child = children.getFirst();
			final RelationshipData.Person childPerson = data.persons().get(child);
			for(final RelationshipData.Participant participant : event.participants()){
				final String informant = participant.personId();
				if(participant.principal() || !INFORMANT_ROLE.equals(participant.role()) || informant.equals(child))
					continue;

				final RelationshipData.Person informantPerson = data.persons().get(informant);
				if(informantPerson == null || !index.between(child, informant).isEmpty() || !asked.add(child + ">" + informant))
					continue;
				if(childPerson != null && childPerson.birth() != null && informantPerson.birth() != null
						&& childPerson.birth().max() < informantPerson.birth().min())
					continue;

				findings.add(finding(Diagnostic.Severity.INFORMATION, "birth_informant_not_linked",
					List.of(child, informant, event.id()), new RelationshipDraft(child, informant, null, event.id(), false),
					name(data, child), name(data, informant), event.id()));
			}
		}
	}

	/**
	 * A relationship of association whose role says that the two are siblings, that the parents do not support. The protocol has no
	 * type for the siblings, so the association with the role is how they are written; the biological parents of the two, when both
	 * have some, tell whether they are full siblings (both in common) or half-siblings (one). Nothing is said if either has no parent.
	 */
	private static void checkSiblingRoles(final RelationshipData data, final Index index, final List<RelationshipFinding> findings){
		for(final RelationshipData.Relationship relationship : data.relationships()){
			if(!ASSOCIATE.equals(relationship.type()) || relationship.subjectKind() != RelationshipData.Kind.INDIVIDUAL
					|| relationship.targetKind() != RelationshipData.Kind.INDIVIDUAL
					|| relationship.subjectId().equals(relationship.targetId()))
				continue;

			final RoleKind kind = RoleKind.of(relationship.role());
			if(kind != RoleKind.SIBLING && kind != RoleKind.HALF_SIBLING)
				continue;

			final Set<String> first = index.biologicalParents(relationship.subjectId());
			final Set<String> second = index.biologicalParents(relationship.targetId());
			if(first.isEmpty() || second.isEmpty())
				continue;

			final Set<String> shared = new LinkedHashSet<>(first);
			shared.retainAll(second);
			final boolean bothKnown = (first.size() == 2 && second.size() == 2);
			final String reason;
			if(shared.isEmpty())
				reason = "relationship.sibling.no_common_parent";
			else if(kind == RoleKind.HALF_SIBLING && shared.size() == 2 && bothKnown)
				reason = "relationship.sibling.both_parents";
			else if(kind == RoleKind.SIBLING && shared.size() == 1 && bothKnown)
				reason = "relationship.sibling.one_parent";
			else
				continue;

			final List<String> ids = new ArrayList<>(List.of(relationship.subjectId(), relationship.targetId(), relationship.id()));
			ids.addAll(first);
			ids.addAll(second);
			findings.add(finding(Diagnostic.Severity.WARNING, "sibling_role_inconsistent", ids.stream().distinct().toList(), null,
				name(data, relationship.subjectId()), name(data, relationship.targetId()), relationship.role(), I18N.t(reason), relationship.id()));
		}
	}

	/**
	 * The associations of two individuals that say nothing or say it wrongly: one whose role is a tie that the protocol has a type for
	 * (a parent, a child, a spouse: the association is for the ties that the list of types does not have), and one between two
	 * individuals already linked by a relationship of the family.
	 */
	private static void checkAssociates(final RelationshipData data, final Index index, final List<RelationshipFinding> findings){
		for(final RelationshipData.Relationship relationship : data.relationships()){
			if(!ASSOCIATE.equals(relationship.type()) || relationship.subjectKind() != RelationshipData.Kind.INDIVIDUAL
					|| relationship.targetKind() != RelationshipData.Kind.INDIVIDUAL
					|| relationship.subjectId().equals(relationship.targetId()))
				continue;

			final List<String> ids = List.of(relationship.subjectId(), relationship.targetId(), relationship.id());
			final RoleKind kind = RoleKind.of(relationship.role());
			if(kind == RoleKind.PARENT_OR_CHILD || kind == RoleKind.SPOUSE){
				findings.add(finding(Diagnostic.Severity.INFORMATION, "associate_kinship_role", ids, null,
					name(data, relationship.subjectId()), name(data, relationship.targetId()), relationship.role(),
					(kind == RoleKind.SPOUSE? SPOUSE: CHILD), relationship.id()));
				continue;
			}

			final Group families = group(index.between(relationship.subjectId(), relationship.targetId()).stream()
				.filter(r -> CHILD_TYPES.contains(r.type()) || PARTNER_TYPES.contains(r.type())));
			//an association with a role says something that the tie does not (a witness, a ward, the birth it took part in): only one
			//without a role adds nothing to the tie of the family
			if(!families.isEmpty() && (relationship.role() == null || relationship.role().isBlank())){
				final List<String> tieIds = new ArrayList<>(List.of(relationship.subjectId(), relationship.targetId(), relationship.id()));
				tieIds.addAll(families.idList());
				findings.add(finding(Diagnostic.Severity.INFORMATION, "associate_redundant", tieIds, null,
					name(data, relationship.subjectId()), name(data, relationship.targetId()), families.first().type(), relationship.id(),
					families.ids()));
			}
		}
	}

	/** What a role written on an association says about the two: the ties of the family that the protocol has a type for, or siblings. */
	private enum RoleKind{
		PARENT_OR_CHILD,
		SPOUSE,
		SIBLING,
		HALF_SIBLING,
		OTHER;

		/** The words that make a role about a step, in-law, god or adoptive tie, or a grandparent: not the plain tie. */
		private static final Set<String> NOT_PLAIN = Set.of("step", "law", "god", "foster", "adoptive", "adopted", "grand", "great",
			"patrigno", "matrigna", "suocero", "suocera", "cognato", "cognata", "genero", "nuora", "padrino", "madrina", "nonno", "nonna");
		private static final Set<String> PARENT_OR_CHILD_WORDS = Set.of("father", "mother", "parent", "son", "daughter", "child",
			"padre", "madre", "genitore", "figlio", "figlia");
		private static final Set<String> SPOUSE_WORDS = Set.of("husband", "wife", "spouse", "marito", "moglie", "coniuge");
		private static final Set<String> SIBLING_WORDS = Set.of("brother", "sister", "sibling", "fratello", "sorella");
		private static final Set<String> HALF_WORDS = Set.of("half", "halfbrother", "halfsister", "fratellastro", "sorellastra", "mezzo",
			"mezza", "mezzofratello", "mezzasorella");


		static RoleKind of(final String role){
			if(role == null || role.isBlank())
				return OTHER;

			final Set<String> words = new LinkedHashSet<>();
			for(final String word : StringUtils.split(role.toLowerCase(Locale.ROOT), " _-/,;.()"))
				words.add(word);
			if(words.stream().anyMatch(NOT_PLAIN::contains))
				return OTHER;

			final boolean half = words.stream().anyMatch(HALF_WORDS::contains);
			final boolean sibling = words.stream().anyMatch(SIBLING_WORDS::contains)
				|| words.contains("fratellastro") || words.contains("sorellastra") || words.contains("halfbrother")
				|| words.contains("halfsister");
			if(half && (sibling || words.contains("fratello") || words.contains("sorella")))
				return HALF_SIBLING;
			if(sibling)
				return SIBLING;
			if(words.stream().anyMatch(PARENT_OR_CHILD_WORDS::contains))
				return PARENT_OR_CHILD;
			if(words.stream().anyMatch(SPOUSE_WORDS::contains))
				return SPOUSE;
			return OTHER;
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
