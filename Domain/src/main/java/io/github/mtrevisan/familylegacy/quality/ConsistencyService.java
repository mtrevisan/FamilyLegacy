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
package io.github.mtrevisan.familylegacy.quality;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
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
 * The structural checks of a {@link FLEFModel}: what the grammar cannot say because it is about the graph of the records, not about one
 * of them. They are part of the validation of the file (see {@link FileValidation}), not a tool of their own.
 * <p>
 * The checks are conservative: they only flag what is provably wrong from the records alone, without dates or outside knowledge, so
 * that the report does not train the user to ignore it:
 * <ul>
 *   <li><b>Circular parentage</b> (error): individuals who are, through the biological child links, their own ancestors. Each circle is
 *       reported once, whatever the number of individuals in it;</li>
 *   <li><b>More than two biological parents</b> (error): an individual has at most two, whatever their sex; the same parent written
 *       twice is one;</li>
 *   <li><b>Two biological mothers or two biological fathers</b> (error): the two parents of an individual have the same sex, both
 *       recorded. If the sex of one is missing there is nothing to check: it is the opposite of the other, if that is known, or it
 *       cannot be said. If a sex is wrong the contradiction is what shows it, and the message says that either the sex or the
 *       parentage is wrong;</li>
 *   <li><b>Conclusion that contradicts itself</b>: the preferred record is not among the ones it resolves (error, the protocol says it
 *       MUST be); a {@code disproven} conclusion that resolves fewer than two records, or has a preferred one (warning, the protocol
 *       says SHOULD);</li>
 *   <li><b>Sibling that is also a parent</b> (error): two individuals who share a biological parent are siblings, so one of them
 *       cannot be the biological parent of the other;</li>
 *   <li><b>Relationship that cannot link what it links</b> (error): a type that is not valid between the kinds of the two records
 *       (a spouse that is a group, a membership between two individuals);</li>
 *   <li><b>Circular hierarchy</b>: groups that are part of each other, places that are part of each other (error), places that
 *       succeed each other in a circle (warning, since the dates may explain it); each circle once;</li>
 *   <li><b>Relationship written twice</b> (warning): the same type between the same two records (a couple written from both sides is
 *       one relationship);</li>
 *   <li><b>Event without participants</b> (warning), and a birth, baptism, death, burial or cremation whose participants all have a
 *       role and none is the principal, the one the event is about (warning). A participant with no role says nothing: the role is
 *       optional, so then it cannot be said;</li>
 *   <li><b>Dates that cannot be right</b> (error): a death before the birth, an event after the death or before the birth of one who
 *       took part in it, a child born before the parent or after the death of the mother, a union before the birth or after the death
 *       of a partner; see {@link DateConsistency}, which judges by the days a date may stand for (approximate, decade, century);</li>
 *   <li><b>Relationship with itself</b> (error): the subject and the target of a relationship, or of a place relationship, are the same
 *       record, of whatever kind;</li>
 *   <li><b>Individual without a name</b> (warning): the record has no name, or only empty ones.</li>
 * </ul>
 * More checks (dates, ages, marriage validity) are the job of the Chronology Assistant.
 */
public final class ConsistencyService{

	/** The types of place relationship that put a place inside another one, and the ones that say that a place came from another. */
	private static final Set<String> PLACE_PART_OF = Set.of(PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF, PlaceRelationshipReader.ENUM_TYPE_GEOGRAPHIC_PART_OF, PlaceRelationshipReader.ENUM_TYPE_ECCLESIASTICAL_PART_OF,
		PlaceRelationshipReader.ENUM_TYPE_JUDICIAL_PART_OF, PlaceRelationshipReader.ENUM_TYPE_CADASTRAL_PART_OF);
	private static final Set<String> PLACE_SUCCESSION = Set.of(PlaceRelationshipReader.ENUM_TYPE_SUCCESSOR_OF, PlaceRelationshipReader.ENUM_TYPE_MERGED_FROM, PlaceRelationshipReader.ENUM_TYPE_SPLIT_FROM,
		PlaceRelationshipReader.ENUM_TYPE_RECEIVED_TERRITORY_FROM);
	/** The types that can link an individual to an individual: the ones the editor offers. */
	private static final Set<String> INDIVIDUAL_TO_INDIVIDUAL_TYPES = Set.of(RelationshipReader.INDIVIDUAL_TO_INDIVIDUAL_TYPES);
	/** The events of the life of a person: they are about one of the participants, the principal. */
	private static final Set<String> PRINCIPAL_EVENT_TYPES = Set.of(EventReader.ENUM_TYPE_BIRTH, "baptism", "christening", EventReader.ENUM_TYPE_DEATH, EventReader.ENUM_TYPE_BURIAL, EventReader.ENUM_TYPE_CREMATION);
	private static final String PRINCIPAL_ROLE = EventParticipationReader.ENUM_ROLE_PRINCIPAL;
	/** The most biological parents of an individual. */
	private static final int MAX_BIOLOGICAL_PARENTS = 2;
	/** The most members of a circle that are named in its message. */
	private static final int MAX_NAMED_MEMBERS = 6;

	/**
	 * One inconsistency detected.
	 *
	 * @param severity	How serious it is.
	 * @param recordId	The record it is about.
	 * @param message	What is wrong.
	 * @param kind	What kind of problem it is, a stable key ({@code circular_parentage}...): the report groups by it. Empty if not known.
	 */
	public record Issue(Severity severity, String recordId, String message, String kind){

		public Issue(final Severity severity, final String recordId, final String message){
			this(severity, recordId, message, "");
		}
	}

	public enum Severity{ WARNING, ERROR }

	/** Result of the consistency check. */
	public record Report(int individualsChecked, int relationshipsChecked, List<Issue> issues){

		public boolean isClean(){
			return issues.isEmpty();
		}
	}


	private ConsistencyService(){
	}


	/** The start of the keys of the texts of the issues: what follows it is the kind of the issue. */
	private static final String ISSUE_KEY_PREFIX = "text.consistency.issue.";

	/** @return	An issue whose message is the text of the key (with the arguments) and whose kind is the end of the key. */
	private static Issue issue(final Severity severity, final String recordId, final String key, final Object... arguments){
		return new Issue(severity, recordId, I18N.tf(key, arguments), key.substring(ISSUE_KEY_PREFIX.length()));
	}


	public static Report check(final FLEFModel model){
		final List<Issue> issues = new ArrayList<>();

		final List<FLEFRecord> individuals = model.getRecordsByType(IndividualReader.TYPE);
		for(final FLEFRecord individual : individuals)
			checkIndividualName(individual, issues);

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipReader.TYPE);
		final Map<String, List<String>> parentsOf = new HashMap<>();
		for(final FLEFRecord relationship : relationships){
			checkSelfReference(relationship, RelationshipReader.TAG_SUBJECT, RelationshipReader.TAG_TARGET,
				"text.consistency.issue.self_relationship", issues);
			collectParentEdge(relationship, parentsOf);
		}
		final List<FLEFRecord> placeRelationships = model.getRecordsByType(PlaceRelationshipReader.TYPE);
		for(final FLEFRecord relationship : placeRelationships)
			checkSelfReference(relationship, PlaceRelationshipReader.TAG_SUBJECT, PlaceRelationshipReader.TAG_TARGET,
				"text.consistency.issue.self_place_relationship", issues);
		checkBiologicalParents(model, parentsOf, issues);
		checkParentsOfTheSameSex(model, parentsOf, issues);
		checkCircularParentage(model, parentsOf, issues);
		checkSiblingsThatAreParents(model, parentsOf, issues);
		checkRelationshipKinds(model, relationships, issues);
		checkHierarchies(model, relationships, placeRelationships, issues);
		checkDuplicateRelationships(model, relationships, placeRelationships, issues);
		checkConclusions(model, issues);
		checkEvents(model, issues);
		issues.addAll(DateConsistency.check(model));

		return new Report(individuals.size(), relationships.size() + placeRelationships.size(), issues);
	}


	private static void checkIndividualName(final FLEFRecord individual, final List<Issue> issues){
		if(StringUtils.isBlank(IndividualReader.extractPrimaryFullname(individual)))
			issues.add(issue(Severity.WARNING, individual.getId(), "text.consistency.issue.no_name", individual.getId()));
	}

	private static void checkSelfReference(final FLEFRecord relationship, final String subjectTag, final String targetTag,
			final String messageKey, final List<Issue> issues){
		final String subjectId = endpoint(relationship, subjectTag);
		if(subjectId != null && subjectId.equals(endpoint(relationship, targetTag)))
			issues.add(issue(Severity.ERROR, relationship.getId(), messageKey, subjectId));
	}

	/** @return	The record that an end of a relationship points to, of whatever kind ({@code null} if it has none). */
	private static String endpoint(final FLEFRecord relationship, final String tag){
		final FLEFRecord end = FLEFRecordHelper.findChild(relationship, tag);
		final FLEFRecord reference = (end != null? end.getTheOnlyChild(): null);
		return (reference != null? reference.getValue(): null);
	}

	private static void collectParentEdge(final FLEFRecord relationship, final Map<String, List<String>> parentsOf){
		final String type = RelationshipReader.extractType(relationship);
		if(!RelationshipReader.isTypeBiologicalChild(type))
			return;
		final String childId = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualReader.TYPE);
		final String parentId = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualReader.TYPE);
		//a child that is its own parent is a relationship with itself: it is reported as such
		if(childId != null && parentId != null && !childId.equals(parentId))
			parentsOf.computeIfAbsent(childId, k -> new ArrayList<>())
				.add(parentId);
	}

	/** An individual has two biological parents at most: more are reported once, with the parents named (the same one twice counts once). */
	private static void checkBiologicalParents(final FLEFModel model, final Map<String, List<String>> parentsOf, final List<Issue> issues){
		final List<String> children = new ArrayList<>(parentsOf.keySet());
		Collections.sort(children);
		for(final String childId : children){
			final List<String> parents = new ArrayList<>(new LinkedHashSet<>(parentsOf.get(childId)));
			if(parents.size() <= MAX_BIOLOGICAL_PARENTS)
				continue;

			final List<String> named = new ArrayList<>();
			for(final String parentId : parents){
				final FLEFRecord parent = model.getRecordById(parentId);
				named.add(parent != null? RecordDisplay.describe(parent, model) + " (" + parentId + ")": parentId);
			}
			issues.add(issue(Severity.ERROR, childId, "text.consistency.issue.parents", childId, parents.size(),
				String.join(", ", named)));
		}
	}

	/**
	 * Two biological parents of the same sex, both recorded, are two mothers or two fathers: an error, whichever is wrong (the sex of
	 * one of them or the parentage). With more than two parents the error is already the number of them; a missing or unknown sex says
	 * nothing.
	 */
	private static void checkParentsOfTheSameSex(final FLEFModel model, final Map<String, List<String>> parentsOf, final List<Issue> issues){
		final List<String> children = new ArrayList<>(parentsOf.keySet());
		Collections.sort(children);
		for(final String childId : children){
			final List<String> parents = new ArrayList<>(new LinkedHashSet<>(parentsOf.get(childId)));
			if(parents.size() != MAX_BIOLOGICAL_PARENTS)
				continue;

			final FLEFRecord first = model.getRecordById(parents.get(0));
			final FLEFRecord second = model.getRecordById(parents.get(1));
			if(first == null || second == null)
				continue;

			final String firstSex = IndividualReader.extractRawSex(first);
			final String secondSex = IndividualReader.extractRawSex(second);
			final boolean mothers = (IndividualReader.isSexFemale(firstSex) && IndividualReader.isSexFemale(secondSex));
			final boolean fathers = (IndividualReader.isSexMale(firstSex) && IndividualReader.isSexMale(secondSex));
			if(!mothers && !fathers)
				continue;

			issues.add(issue(Severity.ERROR, childId, "text.consistency.issue.same_sex_parents", childId,
				I18N.t(mothers? "text.consistency.issue.mothers": "text.consistency.issue.fathers"),
				RecordDisplay.describe(first, model) + " (" + first.getId() + "), " + RecordDisplay.describe(second, model) + " ("
					+ second.getId() + ")"));
		}
	}

	/**
	 * Two individuals who share a biological parent are siblings (or half-siblings): one of them cannot also be the biological parent
	 * of the other. A circle of descent is a different error, reported as such: this one is about a parent that is a sibling.
	 */
	private static void checkSiblingsThatAreParents(final FLEFModel model, final Map<String, List<String>> parentsOf,
			final List<Issue> issues){
		final List<String> children = new ArrayList<>(parentsOf.keySet());
		Collections.sort(children);
		for(final String childId : children){
			final Set<String> parents = new LinkedHashSet<>(parentsOf.get(childId));
			for(final String parentId : parents){
				final List<String> grandparents = parentsOf.get(parentId);
				if(grandparents == null)
					continue;

				final Set<String> shared = new LinkedHashSet<>(grandparents);
				shared.retainAll(parents);
				if(shared.isEmpty())
					continue;

				final FLEFRecord child = model.getRecordById(childId);
				final FLEFRecord parent = model.getRecordById(parentId);
				final FLEFRecord common = model.getRecordById(shared.iterator().next());
				issues.add(issue(Severity.ERROR, childId, "text.consistency.issue.sibling_and_parent",
					(child != null? RecordDisplay.describe(child, model): childId) + " (" + childId + ")",
					(parent != null? RecordDisplay.describe(parent, model): parentId) + " (" + parentId + ")",
					(common != null? RecordDisplay.describe(common, model): shared.iterator().next()) + " (" + shared.iterator().next() + ")"));
			}
		}
	}

	/* ======================================================================
	 *                          Conclusions
	 * ====================================================================== */

	/**
	 * The preferred record of a conclusion must be one of the ones it resolves; a conclusion that disproves normally resolves two records
	 * or more, and prefers none (it says that none of them is right).
	 */
	private static void checkConclusions(final FLEFModel model, final List<Issue> issues){
		for(final FLEFRecord conclusion : model.getRecordsByType(ConclusionReader.TYPE)){
			final List<String> resolved = new ArrayList<>();
			for(final FLEFRecord resolves : FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESOLVES)){
				final FLEFRecord target = resolves.getTheOnlyChild();
				if(target != null && target.getValue() != null)
					resolved.add(target.getValue());
			}
			final FLEFRecord preferredNode = FLEFRecordHelper.findChild(conclusion, ConclusionReader.TAG_PREFERRED);
			final FLEFRecord preferredTarget = (preferredNode != null? preferredNode.getTheOnlyChild(): null);
			final String preferred = (preferredTarget != null? preferredTarget.getValue(): null);

			if(preferred != null && !resolved.contains(preferred))
				issues.add(issue(Severity.ERROR, conclusion.getId(), "text.consistency.issue.preferred", preferred));

			if(ConclusionReader.ENUM_PROOF_STATUS_DISPROVEN.equals(ConclusionReader.extractProofStatus(conclusion))){
				if(resolved.size() < 2)
					issues.add(issue(Severity.WARNING, conclusion.getId(), "text.consistency.issue.disproven_few", resolved.size()));
				if(preferred != null)
					issues.add(issue(Severity.WARNING, conclusion.getId(), "text.consistency.issue.disproven_preferred", preferred));
			}
		}
	}


	/* ======================================================================
	 *                          Events
	 * ====================================================================== */

	/**
	 * An event nobody took part in says nothing; an event of the life of a person (birth, baptism, death, burial, cremation) whose
	 * participants are all known to have another role than the principal is not about anybody.
	 */
	private static void checkEvents(final FLEFModel model, final List<Issue> issues){
		//the roles of the participants of each event; a participant without a role is recorded as an empty one
		final Map<String, List<String>> rolesByEvent = new HashMap<>();
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationReader.TYPE)){
			final String eventId = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT);
			if(eventId != null)
				rolesByEvent.computeIfAbsent(eventId, k -> new ArrayList<>())
					.add(StringUtils.defaultString(FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_ROLE)));
		}

		for(final FLEFRecord event : model.getRecordsByType(EventReader.TYPE)){
			final List<String> roles = rolesByEvent.get(event.getId());
			final String name = RecordDisplay.describe(event, model);
			if(roles == null || roles.isEmpty()){
				issues.add(issue(Severity.WARNING, event.getId(), "text.consistency.issue.event_no_participants", name));
				continue;
			}

			final String type = EventReader.extractType(event);
			if(type != null && PRINCIPAL_EVENT_TYPES.contains(type.toLowerCase(Locale.ROOT))
					&& roles.stream().noneMatch(String::isEmpty) && !roles.contains(PRINCIPAL_ROLE))
				issues.add(issue(Severity.WARNING, event.getId(), "text.consistency.issue.event_no_principal", type, name));
		}
	}


	/* ======================================================================
	 *                          Relationships
	 * ====================================================================== */

	/** The kind of record that an end of a relationship points to ({@code individual}, {@code group}...), {@code null} if it has none. */
	private static String endpointKind(final FLEFRecord relationship, final String tag){
		final FLEFRecord end = FLEFRecordHelper.findChild(relationship, tag);
		final FLEFRecord reference = (end != null? end.getTheOnlyChild(): null);
		return (reference != null? reference.getTag().toLowerCase(Locale.ROOT): null);
	}

	/** A relationship whose type is not valid between the kinds of the two records it links (the type is checked, not the people). */
	private static void checkRelationshipKinds(final FLEFModel model, final List<FLEFRecord> relationships, final List<Issue> issues){
		for(final FLEFRecord relationship : relationships){
			final String type = RelationshipReader.extractType(relationship);
			final String subjectKind = endpointKind(relationship, RelationshipReader.TAG_SUBJECT);
			final String targetKind = endpointKind(relationship, RelationshipReader.TAG_TARGET);
			if(type == null || subjectKind == null || targetKind == null)
				continue;

			final Set<String> valid = validTypes(subjectKind, targetKind);
			if(!valid.contains(type))
				issues.add(issue(Severity.ERROR, relationship.getId(), "text.consistency.issue.relationship_type", type,
					kindLabel(subjectKind), endpoint(relationship, RelationshipReader.TAG_SUBJECT), kindLabel(targetKind),
					endpoint(relationship, RelationshipReader.TAG_TARGET)));
		}
	}

	/** @return	The types of relationship that can link a record of the first kind to one of the second (none for the kinds not linked). */
	private static Set<String> validTypes(final String subjectKind, final String targetKind){
		if(IndividualReader.TYPE.equalsIgnoreCase(subjectKind) && IndividualReader.TYPE.equalsIgnoreCase(targetKind))
			return INDIVIDUAL_TO_INDIVIDUAL_TYPES;
		if(IndividualReader.TYPE.equalsIgnoreCase(subjectKind) && GroupReader.TYPE.equalsIgnoreCase(targetKind))
			return Set.of(RelationshipReader.INDIVIDUAL_TO_GROUP_TYPES);
		if(GroupReader.TYPE.equalsIgnoreCase(subjectKind) && GroupReader.TYPE.equalsIgnoreCase(targetKind))
			return Set.of(RelationshipReader.GROUP_TO_GROUP_TYPES);
		return Set.of();
	}

	private static String kindLabel(final String kind){
		return I18N.t("confirmation.exist_record." + kind, kind);
	}

	/** Relationships (and place relationships) that say the same thing: one issue for each, with the identifiers of all of them. */
	private static void checkDuplicateRelationships(final FLEFModel model, final List<FLEFRecord> relationships,
			final List<FLEFRecord> placeRelationships, final List<Issue> issues){
		final Map<String, List<String>> byKey = new LinkedHashMap<>();
		for(final FLEFRecord relationship : relationships){
			final String type = RelationshipReader.extractType(relationship);
			final String subject = endpoint(relationship, RelationshipReader.TAG_SUBJECT);
			final String target = endpoint(relationship, RelationshipReader.TAG_TARGET);
			if(type == null || subject == null || target == null)
				continue;

			//a couple, or two associates, written from both sides are one relationship
			final boolean symmetric = (RelationshipReader.isTypePartner(type) || RelationshipReader.isTypeAssociate(type));
			final String first = (symmetric && subject.compareTo(target) > 0? target: subject);
			final String second = (first == subject? target: subject);
			byKey.computeIfAbsent("r|" + type + "|" + first + "|" + second, k -> new ArrayList<>())
				.add(relationship.getId());
		}
		for(final FLEFRecord relationship : placeRelationships){
			final String type = PlaceRelationshipReader.extractType(relationship);
			final String subject = endpoint(relationship, PlaceRelationshipReader.TAG_SUBJECT);
			final String target = endpoint(relationship, PlaceRelationshipReader.TAG_TARGET);
			if(type != null && subject != null && target != null)
				byKey.computeIfAbsent("p|" + type + "|" + subject + "|" + target, k -> new ArrayList<>())
					.add(relationship.getId());
		}
		for(final Map.Entry<String, List<String>> entry : byKey.entrySet())
			if(entry.getValue().size() > 1){
				final String[] key = StringUtils.splitPreserveAllTokens(entry.getKey(), '|');
				issues.add(issue(Severity.WARNING, entry.getValue().getFirst(), "text.consistency.issue.duplicate_relationship",
					key[1], entry.getValue().size(), String.join(", ", entry.getValue())));
			}
	}


	/* ======================================================================
	 *                          Hierarchies
	 * ====================================================================== */

	/** Circles in the hierarchy of the groups, of the places, and in the succession of the places: each reported once. */
	private static void checkHierarchies(final FLEFModel model, final List<FLEFRecord> relationships,
			final List<FLEFRecord> placeRelationships, final List<Issue> issues){
		final Map<String, List<String>> groups = new HashMap<>();
		for(final FLEFRecord relationship : relationships)
			if(RelationshipReader.isTypePartOf(RelationshipReader.extractType(relationship)))
				addEdge(groups, endpoint(relationship, RelationshipReader.TAG_SUBJECT), endpoint(relationship, RelationshipReader.TAG_TARGET));
		reportCircles(model, groups, Severity.ERROR, "text.consistency.issue.group_cycle", issues);

		final Map<String, List<String>> places = new HashMap<>();
		final Map<String, List<String>> succession = new HashMap<>();
		for(final FLEFRecord relationship : placeRelationships){
			final String type = PlaceRelationshipReader.extractType(relationship);
			final String subject = endpoint(relationship, PlaceRelationshipReader.TAG_SUBJECT);
			final String target = endpoint(relationship, PlaceRelationshipReader.TAG_TARGET);
			if(type == null)
				continue;

			if(PLACE_PART_OF.contains(type))
				addEdge(places, subject, target);
			else if(PLACE_SUCCESSION.contains(type))
				addEdge(succession, subject, target);
		}
		reportCircles(model, places, Severity.ERROR, "text.consistency.issue.place_cycle", issues);
		//a place that succeeds another one that succeeds it: the dates may explain it (a place that was split and joined again)
		reportCircles(model, succession, Severity.WARNING, "text.consistency.issue.succession_cycle", issues);
	}

	private static void addEdge(final Map<String, List<String>> graph, final String from, final String to){
		//a record related to itself is reported as such, not as a circle
		if(from != null && to != null && !from.equals(to))
			graph.computeIfAbsent(from, k -> new ArrayList<>())
				.add(to);
	}

	private static void reportCircles(final FLEFModel model, final Map<String, List<String>> graph, final Severity severity,
			final String messageKey, final List<Issue> issues){
		for(final List<String> circle : circles(graph))
			issues.add(issue(severity, circle.getFirst(), messageKey, namedMembers(model, circle)));
	}

	/** @return	The members of a circle by name, the first ones only if there are many. */
	private static String namedMembers(final FLEFModel model, final List<String> circle){
		final List<String> named = new ArrayList<>();
		for(final String id : circle.subList(0, Math.min(MAX_NAMED_MEMBERS, circle.size()))){
			final FLEFRecord record = model.getRecordById(id);
			named.add(record != null? RecordDisplay.describe(record, model) + " (" + id + ")": id);
		}
		if(circle.size() > MAX_NAMED_MEMBERS)
			named.add(I18N.tf("text.consistency.issue.and_more", circle.size() - MAX_NAMED_MEMBERS));
		return String.join(", ", named);
	}


	/* ======================================================================
	 *                          Parentage
	 * ====================================================================== */

	/** Reports each circle of parentage once, with the individuals in it: the strongly connected groups of the graph of the parents. */
	private static void checkCircularParentage(final FLEFModel model, final Map<String, List<String>> parentsOf, final List<Issue> issues){
		reportCircles(model, parentsOf, Severity.ERROR, "text.consistency.issue.cycle", issues);
	}

	/**
	 * @param parentsOf	The parents of each individual.
	 * @return	The circles: the groups of two or more individuals that are all ancestors of each other, each with its members in order of
	 * 	identifier; the circles in the order of their first member. Found with Tarjan's algorithm, without recursion (a long line of
	 * 	ancestors must not overflow the stack).
	 */
	public static List<List<String>> circles(final Map<String, List<String>> parentsOf){
		final Map<String, Integer> index = new HashMap<>();
		final Map<String, Integer> low = new HashMap<>();
		final Set<String> onStack = new LinkedHashSet<>();
		final Deque<String> stack = new ArrayDeque<>();
		final List<List<String>> circles = new ArrayList<>();
		int counter = 0;

		final List<String> starts = new ArrayList<>(parentsOf.keySet());
		Collections.sort(starts);
		for(final String start : starts){
			if(index.containsKey(start))
				continue;

			//an explicit stack of (node, position among its parents)
			final Deque<Object[]> work = new ArrayDeque<>();
			work.push(new Object[]{start, 0});
			index.put(start, counter);
			low.put(start, counter ++);
			stack.push(start);
			onStack.add(start);
			while(!work.isEmpty()){
				final Object[] frame = work.peek();
				final String node = (String)frame[0];
				final List<String> parents = parentsOf.getOrDefault(node, List.of());
				final int position = (Integer)frame[1];
				if(position < parents.size()){
					frame[1] = position + 1;
					final String parent = parents.get(position);
					if(!index.containsKey(parent)){
						index.put(parent, counter);
						low.put(parent, counter ++);
						stack.push(parent);
						onStack.add(parent);
						work.push(new Object[]{parent, 0});
					}
					else if(onStack.contains(parent))
						low.put(node, Math.min(low.get(node), index.get(parent)));
					continue;
				}

				work.pop();
				if(!work.isEmpty()){
					final String caller = (String)work.peek()[0];
					low.put(caller, Math.min(low.get(caller), low.get(node)));
				}
				if(low.get(node).equals(index.get(node))){
					final List<String> members = new ArrayList<>();
					String member;
					do{
						member = stack.pop();
						onStack.remove(member);
						members.add(member);
					}while(!member.equals(node));
					if(members.size() > 1){
						Collections.sort(members);
						circles.add(members);
					}
				}
			}
		}
		circles.sort((a, b) -> a.getFirst().compareTo(b.getFirst()));
		return circles;
	}

}
