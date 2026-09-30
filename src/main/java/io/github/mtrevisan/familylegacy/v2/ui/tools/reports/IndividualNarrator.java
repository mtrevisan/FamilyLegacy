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
package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.index.KinshipResolver;
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.index.RelationIndex;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Builds a rich, chronological narrative biography of an individual from events,
 * attributes, notes, and relationships stored in the FLEF model.
 *
 * <p>Sentence order follows the individual's life flow:</p>
 * <ol>
 *   <li>birth (date, place);</li>
 *   <li>baptism / early life rites;</li>
 *   <li>titles, noble status, and physical characteristics;</li>
 *   <li>marriages and divorces (with status checking);</li>
 *   <li>children grouped by co-parent and relationship type;</li>
 *   <li>occupations, military service/rank, and education;</li>
 *   <li>scalar attributes (religion, ethnicity, citizenship, nationality, social class, caste, literacy, language);</li>
 *   <li>emigration / immigration events;</li>
 *   <li>residences and moves;</li>
 *   <li>custom life events with narrative descriptions or honors;</li>
 *   <li>biographical notes and oral history anecdotes;</li>
 *   <li>death, cause of death, and burial or cremation.</li>
 * </ol>
 */
final class IndividualNarrator{

	/* ======================================================================
	 *                          Tags
	 * ====================================================================== */

	private static final String TAG_TYPE = "type";
	private static final String TAG_VALUE = "value";
	private static final String TAG_NAME = "name";
	private static final String TAG_PART = "part";
	private static final String TAG_CAUSE = "cause";
	private static final String TAG_REASON = "reason";
	private static final String TAG_DESCRIPTION = "description";
	private static final String TAG_NOTE = "note";
	private static final String TAG_TEXT = "text";
	private static final String TAG_VALID_FROM = "valid_from";
	private static final String TAG_VALID_TO = "valid_to";
	private static final String TAG_STATUS = "status";

	/* ======================================================================
	 *                          Attribute / event type names
	 * ====================================================================== */

	private static final String TYPE_BIRTH = "birth";
	private static final String TYPE_BAPTISM = "baptism";
	private static final String TYPE_DEATH = "death";
	private static final String TYPE_BURIAL = "burial";
	private static final String TYPE_CREMATION = "cremation";
	private static final String TYPE_EMIGRATION = "emigration";
	private static final String TYPE_IMMIGRATION = "immigration";
	private static final String TYPE_DIVORCE = "divorce";
	private static final String TYPE_DIVORCE_DECREE = "divorce_decree";
	private static final String TYPE_DIVORCE_FILED = "divorce_filed";
	private static final String TYPE_ANNULMENT = "annulment";

	private static final String TYPE_OCCUPATION = "occupation";
	private static final String TYPE_RESIDENCE = "residence";
	private static final String TYPE_CHARACTERISTIC = "characteristic";
	private static final String TYPE_TITLE = "title";
	private static final String TYPE_MILITARY_RANK = "military_rank";
	private static final String TYPE_EDUCATION = "education";
	private static final String TYPE_RELIGION = "religion";
	private static final String TYPE_ETHNICITY = "ethnicity";
	private static final String TYPE_CITIZENSHIP = "citizenship";
	private static final String TYPE_NATIONALITY = "nationality";
	private static final String TYPE_SOCIAL_CLASS = "social_class";
	private static final String TYPE_CASTE = "caste";
	private static final String TYPE_LITERACY = "literacy";
	private static final String TYPE_LANGUAGE = "language";

	private static final String[] MARRIAGE_TYPES = {
		"marriage", "civil_spouse", "religious_spouse",
		"customary_spouse", "cohabiting_partner", "engaged_partner"
	};

	private static final String[] DIVORCE_TYPES = {
		TYPE_DIVORCE, TYPE_DIVORCE_DECREE, TYPE_DIVORCE_FILED, TYPE_ANNULMENT
	};

	/** Core life cycle events skipped when processing custom described events. */
	private static final String[] CORE_LIFE_EVENTS = {
		TYPE_BIRTH, TYPE_BAPTISM, TYPE_DEATH, TYPE_BURIAL, TYPE_CREMATION,
		TYPE_EMIGRATION, TYPE_IMMIGRATION
	};

	/** Scalar attributes described with a generic attribute sentence. */
	private static final String[][] SCALAR_ATTRS = {
		{TYPE_MILITARY_RANK, "military rank"},
		{TYPE_EDUCATION, "education"},
		{TYPE_RELIGION, "religion"},
		{TYPE_ETHNICITY, "ethnicity"},
		{TYPE_CITIZENSHIP, "citizenship"},
		{TYPE_NATIONALITY, "nationality"},
		{TYPE_SOCIAL_CLASS, "social class"},
		{TYPE_CASTE, "caste"},
		{TYPE_LITERACY, "literacy"},
		{TYPE_LANGUAGE, "language"}
	};

	/** Relationship type marking the current person as a step-parent. */
	private static final String REL_STEP_CHILD = "step_child";


	private final FLEFModel model;
	private final RelationIndex idx;
	private final ReportLabels labels;


	IndividualNarrator(final FLEFModel model, final RelationIndex idx, final ReportLabels labels){
		this.model = Objects.requireNonNull(model);
		this.idx = Objects.requireNonNull(idx);
		this.labels = Objects.requireNonNull(labels);
	}


	/* ======================================================================
	 *                          Entry point
	 * ====================================================================== */

	String narrate(final FLEFRecord person){
		if(person == null)
			return StringUtils.EMPTY;

		final List<FLEFRecord> events = idx.eventsOf(person);
		final List<FLEFRecord> attrs = idx.attributesOf(person);

		final FLEFRecord birth = findEvent(events, TYPE_BIRTH);
		final FLEFRecord baptism = findEvent(events, TYPE_BAPTISM);
		final FLEFRecord death = findEvent(events, TYPE_DEATH);
		final FLEFRecord burial = findEvent(events, TYPE_BURIAL);
		final FLEFRecord cremation = findEvent(events, TYPE_CREMATION);
		final FLEFRecord emigration = findEvent(events, TYPE_EMIGRATION);
		final FLEFRecord immigration = findEvent(events, TYPE_IMMIGRATION);

		final List<FLEFRecord> marriages = findEvents(events, MARRIAGE_TYPES);
		final List<FLEFRecord> divorces = findEvents(events, DIVORCE_TYPES);
		final List<FLEFRecord> occupations = findAttrs(attrs, TYPE_OCCUPATION);
		final List<FLEFRecord> titles = findAttrs(attrs, TYPE_TITLE);
		final List<FLEFRecord> residences = findAttrs(attrs, TYPE_RESIDENCE);
		final List<FLEFRecord> characteristics = findAttrs(attrs, TYPE_CHARACTERISTIC);

		final String name = displayName(person);
		final List<String> sentences = new ArrayList<>();

		// 0. Varianti del nome (Name Variants)
		addNameVariants(sentences, person);

		// 1. Birth
		if(birth != null){
			final String d = orNull(FLEFRecordHelper.extractDate(birth));
			final String p = orNull(FLEFRecordHelper.extractPlace(birth, model));
			sentences.add(labels.narrativeBirth(name, d, p));
		}
		else
			sentences.add(labels.narrativeBirthUnknown(name));

		// 1b. Baptism
		if(baptism != null){
			final String d = orNull(FLEFRecordHelper.extractDate(baptism));
			final String p = orNull(FLEFRecordHelper.extractPlace(baptism, model));
			sentences.add(labels.narrativeBaptism(name, d, p));
		}

		// 2. Titles and Physical characteristics
		for(final FLEFRecord t : titles){
			final String v = FLEFRecordHelper.getChildValue(t, TAG_VALUE);
			if(v != null && !v.isBlank())
				sentences.add(labels.narrativeTitle(name, v.trim()));
		}
		for(final FLEFRecord c : characteristics){
			final String v = FLEFRecordHelper.getChildValue(c, TAG_VALUE);
			if(v != null && !v.isBlank())
				sentences.add(labels.narrativeCharacteristic(name, v.trim()));
		}

		// 3. Marriages
		for(final FLEFRecord m : marriages){
			final String d = orNull(FLEFRecordHelper.extractDate(m));
			final String p = orNull(FLEFRecordHelper.extractPlace(m, model));
			final FLEFRecord spouse = spouseFromEvent(person, m);
			final String spouseName = (spouse != null? displayName(spouse): null);
			sentences.add(labels.narrativeMarriage(name, spouseName, d, p));

			// Check status tag on relationship or marriage record
			final String status = FLEFRecordHelper.getChildValue(m, TAG_STATUS);
			if("ended".equalsIgnoreCase(status) || "divorced".equalsIgnoreCase(status) || "annulled".equalsIgnoreCase(status)){
				final String validTo = extractValidDate(m, TAG_VALID_TO);
				sentences.add(labels.narrativeDivorce(name, spouseName, validTo));
			}
		}

		// 3b. Divorces / annulments events
		for(final FLEFRecord d : divorces){
			final String dt = orNull(FLEFRecordHelper.extractDate(d));
			final FLEFRecord spouse = spouseFromEvent(person, d);
			final String spouseName = (spouse != null? displayName(spouse): "?");
			sentences.add(labels.narrativeDivorce(name, spouseName, dt));
		}

		// 4. Children grouped by (co-parent, relationship type)
		addChildrenSentences(sentences, person, name);

		// 5. Occupations
		for(final FLEFRecord occ : occupations){
			final String v = FLEFRecordHelper.getChildValue(occ, TAG_VALUE);
			if(v != null && !v.isBlank())
				sentences.add(labels.narrativeOccupation(name, v.trim()));
		}

		// 6. Scalar attributes (Military, Education, Religion, Ethnicity, etc.)
		for(final String[] pair : SCALAR_ATTRS){
			for(final FLEFRecord a : findAttrs(attrs, pair[0])){
				final String v = FLEFRecordHelper.getChildValue(a, TAG_VALUE);
				if(v != null && !v.isBlank())
					sentences.add(labels.narrativeAttribute(name, pair[1], v.trim()));
			}
		}

		// 7. Migration events
		if(emigration != null){
			final String d = orNull(FLEFRecordHelper.extractDate(emigration));
			final String p = orNull(FLEFRecordHelper.extractPlace(emigration, model));
			sentences.add(labels.narrativeEmigration(name, d, p));
		}
		if(immigration != null){
			final String d = orNull(FLEFRecordHelper.extractDate(immigration));
			final String p = orNull(FLEFRecordHelper.extractPlace(immigration, model));
			sentences.add(labels.narrativeImmigration(name, d, p));
		}

		// 8. Residences and moves
		addResidenceSentences(sentences, name, residences);

		// 9. Custom events with descriptions (awards, honors, military events, etc.)
		addDescribedEvents(sentences, events, name);

		// 10. Biographical notes / oral tradition anecdotes
		addIndividualNotes(sentences, person);

		// 11. Death, cause, burial/cremation
		if(death != null){
			final String d = orNull(FLEFRecordHelper.extractDate(death));
			final String p = orNull(FLEFRecordHelper.extractPlace(death, model));
			String cause = FLEFRecordHelper.getChildValue(death, TAG_CAUSE + "." + TAG_REASON);
			if(cause == null)
				cause = FLEFRecordHelper.getChildValue(death, TAG_CAUSE);
			sentences.add(labels.narrativeDeath(name, d, p, orNull(cause)));
		}

		if(burial != null){
			final String d = orNull(FLEFRecordHelper.extractDate(burial));
			final String p = orNull(FLEFRecordHelper.extractPlace(burial, model));
			sentences.add(labels.narrativeBurial(name, d, p));
		}
		else if(cremation != null){
			final String d = orNull(FLEFRecordHelper.extractDate(cremation));
			final String p = orNull(FLEFRecordHelper.extractPlace(cremation, model));
			sentences.add(labels.narrativeCremation(name, d, p));
		}

		return String.join(StringUtils.SPACE, sentences);
	}

	/**
	 * Appends sentences describing secondary/variant names for the individual.
	 */
	private void addNameVariants(final List<String> sentences, final FLEFRecord person){
		for(final FLEFRecord nameRec : FLEFRecordHelper.findChildren(person, TAG_NAME)){
			for(final FLEFRecord variant : FLEFRecordHelper.findChildren(nameRec, "variant")){
				final String v = ReportFormatters.renderNameVariant(variant);
				if(v != null && !v.isBlank())
					sentences.add("*" + labels.sections().nameVariant() + ":* "
						+ ReportFormatters.escape(v));
			}
		}
	}


	/* ======================================================================
	 *                          Described events & Notes
	 * ====================================================================== */

	/**
	 * Appends custom narrative events carrying human-readable descriptions
	 * (e.g. military awards, honors, legal proceedings).
	 */
	private void addDescribedEvents(final List<String> sentences, final List<FLEFRecord> events, final String name){
		for(final FLEFRecord e : events){
			final String type = FLEFRecordHelper.getChildValue(e, TAG_TYPE);
			if(isCoreLifeEvent(type))
				continue;

			final String desc = FLEFRecordHelper.getChildValue(e, TAG_DESCRIPTION);
			if(desc != null && !desc.isBlank()){
				final String d = orNull(FLEFRecordHelper.extractDate(e));
				final String p = orNull(FLEFRecordHelper.extractPlace(e, model));
				sentences.add(labels.narrativeAttribute(name, type != null? type.replace('_', ' '): "event", desc.trim()));
			}
		}
	}

	private boolean isCoreLifeEvent(final String type){
		if(type == null)
			return false;
		for(final String core : CORE_LIFE_EVENTS)
			if(core.equalsIgnoreCase(type))
				return true;
		for(final String m : MARRIAGE_TYPES)
			if(m.equalsIgnoreCase(type))
				return true;
		for(final String div : DIVORCE_TYPES)
			if(div.equalsIgnoreCase(type))
				return true;
		return false;
	}

	/**
	 * Extracts free-text notes or anecdotes attached directly to the individual.
	 */
	private void addIndividualNotes(final List<String> sentences, final FLEFRecord person){
		for(final FLEFRecord noteRec : FLEFRecordHelper.findChildren(person, TAG_NOTE)){
			final String text = FLEFRecordHelper.getChildValue(noteRec, TAG_TEXT);
			if(text != null && !text.isBlank()){
				sentences.add(text.trim());
			}
		}
	}


	/* ======================================================================
	 *                          Children
	 * ====================================================================== */

	private record ChildGroupKey(String otherParentId, String relationshipType){
	}

	/**
	 * Groups the children of {@code person} by (co-parent, relationship type)
	 * and emits one sentence per group. Step-children are reported without a
	 * co-parent, because their other parent is not the person being described.
	 */
	private void addChildrenSentences(final List<String> sentences, final FLEFRecord person,
		final String name){
		final List<FLEFRecord> children = idx.childrenOf(person);
		if(children.isEmpty())
			return;

		final Map<ChildGroupKey, List<FLEFRecord>> groups = new LinkedHashMap<>();
		for(final FLEFRecord child : children){
			final String relType = relationshipTypeFrom(person, child);
			final boolean isStep = REL_STEP_CHILD.equalsIgnoreCase(relType);
			final FLEFRecord otherParent = (isStep? null: otherParentOf(child, person));
			final ChildGroupKey key = new ChildGroupKey(
				(otherParent != null? otherParent.getId(): null), relType);

			groups.computeIfAbsent(key, k -> new ArrayList<>()).add(child);
		}

		for(final Map.Entry<ChildGroupKey, List<FLEFRecord>> e : groups.entrySet()){
			final ChildGroupKey key = e.getKey();
			final List<FLEFRecord> kids = e.getValue();
			final String groupLabel = labels.childGroupLabel(key.relationshipType(), kids.size());
			final String names = joinNames(kids);

			if(key.otherParentId() != null){
				final FLEFRecord other = model.getRecordById(key.otherParentId());
				final String otherName = (other != null? displayName(other): "?");
				sentences.add(labels.narrativeChildrenGroupWith(
					name, otherName, kids.size(), groupLabel, names));
			}
			else
				sentences.add(labels.narrativeChildrenGroup(
					name, kids.size(), groupLabel, names));
		}
	}

	private String relationshipTypeFrom(final FLEFRecord parent, final FLEFRecord child){
		for(final RelationIndex.ParentEdge edge : idx.parentEdgesOf(child))
			if(edge.parent().getId().equals(parent.getId()))
				return edge.relationshipType();
		return RelationshipHandler.ENUM_TYPE_BIOLOGICAL_CHILD;
	}

	/**
	 * Returns the parent of {@code child} that is not {@code person}, choosing
	 * the most "primary" relationship by rank: biological, adoptive, foster,
	 * guarded, step.
	 */
	private FLEFRecord otherParentOf(final FLEFRecord child, final FLEFRecord person){
		FLEFRecord fallback = null;
		FLEFRecord best = null;
		int bestRank = Integer.MAX_VALUE;
		for(final RelationIndex.ParentEdge edge : idx.parentEdgesOf(child)){
			final FLEFRecord candidate = edge.parent();
			if(candidate.getId().equals(person.getId()))
				continue;
			if(fallback == null)
				fallback = candidate;
			final int rank = KinshipResolver.rankOf(edge.relationshipType());
			if(rank < bestRank){
				bestRank = rank;
				best = candidate;
			}
		}
		return (best != null? best: fallback);
	}


	/* ======================================================================
	 *                          Residence / move helpers
	 * ====================================================================== */

	private void addResidenceSentences(final List<String> sentences, final String name,
		final List<FLEFRecord> residences){
		if(residences.isEmpty())
			return;

		final List<FLEFRecord> sorted = new ArrayList<>(residences);
		sorted.sort(Comparator.comparingInt(this::residenceStartYear));

		final LinkedHashMap<String, FLEFRecord> byPlace = new LinkedHashMap<>();
		for(final FLEFRecord r : sorted){
			final String place = resolvePlace(r);
			if(place == null)
				continue;
			byPlace.putIfAbsent(place, r);
		}

		boolean first = true;
		for(final FLEFRecord r : byPlace.values()){
			final String place = resolvePlace(r);
			final String from = extractValidDate(r, TAG_VALID_FROM);
			final String to = extractValidDate(r, TAG_VALID_TO);
			if(first){
				sentences.add(labels.narrativeResidence(name, place, from, to));
				first = false;
			}
			else
				sentences.add(labels.narrativeMove(name, place, from, to));
		}
	}

	private int residenceStartYear(final FLEFRecord attr){
		final String from = extractValidDate(attr, TAG_VALID_FROM);
		if(from == null)
			return Integer.MAX_VALUE;
		try{
			return Integer.parseInt(from.substring(0, Math.min(4, from.length())));
		}
		catch(final NumberFormatException ignored){
			return Integer.MAX_VALUE;
		}
	}

	private String extractValidDate(final FLEFRecord attr, final String fieldTag){
		final String original = FLEFRecordHelper.getChildValue(attr, fieldTag + ".original_text");
		if(original != null && !original.isBlank())
			return original;

		final String full = FLEFRecordHelper.getChildValue(attr,
			fieldTag + ".value.point.full_date.value");
		if(full != null && !full.isBlank())
			return full;

		final String decade = FLEFRecordHelper.getChildValue(attr,
			fieldTag + ".value.point.decade.start_year");
		if(decade != null && !decade.isBlank())
			return decade + "s";

		final String century = FLEFRecordHelper.getChildValue(attr,
			fieldTag + ".value.point.century.ordinal");
		if(century != null && !century.isBlank())
			return century + "th century";

		return null;
	}

	private String resolvePlace(final FLEFRecord attr){
		final String original = FLEFRecordHelper.getChildValue(attr, "place.original_text");
		if(original != null && !original.isBlank())
			return original.trim();

		final String ref = FLEFRecordHelper.getChildValue(attr, "place.place");
		if(ref == null)
			return null;

		final FLEFRecord place = model.getRecordById(ref);
		if(place == null)
			return null;

		for(final FLEFRecord n : FLEFRecordHelper.findChildren(place, TAG_NAME)){
			final String v = FLEFRecordHelper.getChildValue(n, TAG_VALUE);
			if(v != null && !v.isBlank())
				return v.trim();
		}
		return null;
	}


	/* ======================================================================
	 *                          Event / attribute lookups
	 * ====================================================================== */

	private FLEFRecord findEvent(final List<FLEFRecord> events, final String type){
		for(final FLEFRecord e : events){
			final String t = FLEFRecordHelper.getChildValue(e, TAG_TYPE);
			if(type.equalsIgnoreCase(t))
				return e;
		}
		return null;
	}

	private List<FLEFRecord> findEvents(final List<FLEFRecord> events, final String... types){
		final List<FLEFRecord> out = new ArrayList<>();
		for(final FLEFRecord e : events){
			final String t = FLEFRecordHelper.getChildValue(e, TAG_TYPE);
			if(t == null)
				continue;
			for(final String type : types)
				if(type.equalsIgnoreCase(t)){
					out.add(e);
					break;
				}
		}
		return out;
	}

	private List<FLEFRecord> findAttrs(final List<FLEFRecord> attrs, final String type){
		final List<FLEFRecord> out = new ArrayList<>();
		for(final FLEFRecord a : attrs){
			final String t = FLEFRecordHelper.getChildValue(a, TAG_TYPE);
			if(type.equalsIgnoreCase(t))
				out.add(a);
		}
		return out;
	}


	/* ======================================================================
	 *                          Spouse lookup
	 * ====================================================================== */

	private FLEFRecord spouseFromEvent(final FLEFRecord person, final FLEFRecord event){
		final String eventId = event.getId();
		if(eventId == null)
			return null;

		final List<FLEFRecord> eventParticipations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(final FLEFRecord eventParticipation : eventParticipations){
			final String eid = FLEFRecordHelper.getChildValue(eventParticipation, "event");
			if(!Objects.equals(eventId, eid))
				continue;

			final FLEFRecord pf = FLEFRecordHelper.findChild(eventParticipation, "participant");
			if(pf == null)
				continue;

			final FLEFRecord ref = pf.getTheOnlyChild();
			if(ref == null)
				continue;

			final String pid = ref.getValue();
			if(pid == null || pid.equals(person.getId()))
				continue;

			return model.getRecordById(pid);
		}
		return null;
	}


	/* ======================================================================
	 *                          Name rendering
	 * ====================================================================== */

	private String joinNames(final List<FLEFRecord> people){
		final List<String> names = new ArrayList<>(people.size());
		for(final FLEFRecord p : people)
			names.add(displayName(p));
		return String.join(", ", names);
	}

	private String displayName(final FLEFRecord rec){
		for(final FLEFRecord nameRec : FLEFRecordHelper.findChildren(rec, TAG_NAME)){
			final StringBuilder sb = new StringBuilder();
			for(final FLEFRecord child : nameRec.getChildren()){
				if(!TAG_PART.equalsIgnoreCase(child.getTag()))
					continue;

				final String v = FLEFRecordHelper.getChildValue(child, TAG_VALUE);
				if(v != null && !v.isBlank()){
					if(!sb.isEmpty())
						sb.append(' ');
					sb.append(v.trim());
				}
			}
			if(!sb.isEmpty())
				return sb.toString();
		}
		return (rec.getId() != null? rec.getId(): "?");
	}


	private static String orNull(final String s){
		return (s == null || s.isBlank()? null: s);
	}

}
