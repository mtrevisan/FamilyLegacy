package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


/**
 * Builds a short narrative biography of an individual from the events,
 * attributes and relationships indexed by {@link RelationIndex}.
 *
 * <p>Sentence order follows the individual's life:</p>
 * <ol>
 *   <li>birth (date, place);</li>
 *   <li>physical description ({@code characteristic} attributes);</li>
 *   <li>marriages and divorces;</li>
 *   <li>children grouped by (other parent, relationship type), so biological,
 *       adoptive, foster, guarded and step children are all attributed
 *       correctly even when they belong to different unions;</li>
 *   <li>occupations;</li>
 *   <li>religion, ethnicity, citizenship (other attributes with a scalar
 *       value);</li>
 *   <li>residences and subsequent moves, ordered by start date and deduplicated
 *       by place;</li>
 *   <li>death (date, place, cause).</li>
 * </ol>
 */
final class LifeNarrator{

	/* ======================================================================
	 *                          Tags
	 * ====================================================================== */

	private static final String TAG_TYPE = "type";
	private static final String TAG_VALUE = "value";
	private static final String TAG_NAME = "name";
	private static final String TAG_PART = "part";
	private static final String TAG_CAUSE = "cause";
	private static final String TAG_REASON = "reason";
	private static final String TAG_VALID_FROM = "valid_from";
	private static final String TAG_VALID_TO = "valid_to";
	private static final String TAG_STATUS = "status";

	/* ======================================================================
	 *                          Attribute / event type names
	 * ====================================================================== */

	private static final String TYPE_BIRTH = "birth";
	private static final String TYPE_DEATH = "death";
	private static final String TYPE_DIVORCE = "divorce";
	private static final String TYPE_DIVORCE_DECREE = "divorce_decree";
	private static final String TYPE_OCCUPATION = "occupation";
	private static final String TYPE_RESIDENCE = "residence";
	private static final String TYPE_CHARACTERISTIC = "characteristic";
	private static final String TYPE_RELIGION = "religion";
	private static final String TYPE_ETHNICITY = "ethnicity";
	private static final String TYPE_CITIZENSHIP = "citizenship";
	private static final String TYPE_NATIONALITY = "nationality";
	private static final String TYPE_SOCIAL_CLASS = "social_class";

	private static final String[] MARRIAGE_TYPES = {
		"marriage", "civil_spouse", "religious_spouse",
		"customary_spouse", "cohabiting_partner"
	};

	private static final String[] DIVORCE_TYPES = {
		"divorce", "divorce_decree", "annulment"
	};

	/** Attributes that are described with a "was X: Y" sentence. */
	private static final String[][] SCALAR_ATTRS = {
		{TYPE_RELIGION, "religion"},
		{TYPE_ETHNICITY, "ethnicity"},
		{TYPE_CITIZENSHIP, "citizenship"},
		{TYPE_NATIONALITY, "nationality"},
		{TYPE_SOCIAL_CLASS, "social class"}
	};

	/** Relationship type that marks the current person as a step-parent. */
	private static final String REL_STEP_CHILD = "step_child";


	private final FLEFModel model;
	private final RelationIndex idx;
	private final ReportLabels labels;


	LifeNarrator(final FLEFModel model, final RelationIndex idx, final ReportLabels labels){
		this.model = Objects.requireNonNull(model);
		this.idx = Objects.requireNonNull(idx);
		this.labels = Objects.requireNonNull(labels);
	}


	/* ======================================================================
	 *                          Entry point
	 * ====================================================================== */

	String narrate(final FLEFRecord person){
		if(person == null)
			return "";

		final List<FLEFRecord> events = idx.eventsOf(person);
		final List<FLEFRecord> attrs = idx.attributesOf(person);

		final FLEFRecord birth = findEvent(events, TYPE_BIRTH);
		final FLEFRecord death = findEvent(events, TYPE_DEATH);
		final List<FLEFRecord> marriages = findEvents(events, MARRIAGE_TYPES);
		final List<FLEFRecord> divorces = findEvents(events, DIVORCE_TYPES);
		final List<FLEFRecord> occupations = findAttrs(attrs, TYPE_OCCUPATION);
		final List<FLEFRecord> residences = findAttrs(attrs, TYPE_RESIDENCE);
		final List<FLEFRecord> characteristics = findAttrs(attrs, TYPE_CHARACTERISTIC);

		final String name = displayName(person);
		final List<String> sentences = new ArrayList<>();

		// 1. Birth
		if(birth != null){
			final String d = orNull(FLEFRecordHelper.extractDate(birth));
			final String p = orNull(FLEFRecordHelper.extractPlace(birth, model));
			sentences.add(labels.narrativeBirth(name, d, p));
		}
		else
			sentences.add(labels.narrativeBirthUnknown(name));

		// 2. Physical description
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
		}

		// 3b. Divorces / annulments
		for(final FLEFRecord d : divorces){
			final String dt = orNull(FLEFRecordHelper.extractDate(d));
			final FLEFRecord spouse = spouseFromEvent(person, d);
			final String spouseName = (spouse != null? displayName(spouse): "?");
			sentences.add(labels.narrativeDivorce(name, spouseName, dt));
		}

		// 4. Children grouped by (other parent, relationship type)
		addChildrenSentences(sentences, person, name);

		// 5. Occupations
		for(final FLEFRecord occ : occupations){
			final String v = FLEFRecordHelper.getChildValue(occ, TAG_VALUE);
			if(v != null && !v.isBlank())
				sentences.add(labels.narrativeOccupation(name, v.trim()));
		}

		// 6. Religion, ethnicity, citizenship, nationality, social class
		for(final String[] pair : SCALAR_ATTRS){
			for(final FLEFRecord a : findAttrs(attrs, pair[0])){
				final String v = FLEFRecordHelper.getChildValue(a, TAG_VALUE);
				if(v != null && !v.isBlank())
					sentences.add(labels.narrativeAttribute(name, pair[1], v.trim()));
			}
		}

		// 7. Residences and moves
		addResidenceSentences(sentences, name, residences);

		// 8. Death
		if(death != null){
			final String d = orNull(FLEFRecordHelper.extractDate(death));
			final String p = orNull(FLEFRecordHelper.extractPlace(death, model));
			String cause = FLEFRecordHelper.getChildValue(death, TAG_CAUSE + "." + TAG_REASON);
			if(cause == null)
				cause = FLEFRecordHelper.getChildValue(death, TAG_CAUSE);
			sentences.add(labels.narrativeDeath(name, d, p, orNull(cause)));
		}

		return String.join(" ", sentences);
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
		return "biological_child";
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
			final int rank = rankOf(edge.relationshipType());
			if(rank < bestRank){
				bestRank = rank;
				best = candidate;
			}
		}
		return (best != null? best: fallback);
	}

	private static int rankOf(final String relationshipType){
		if(relationshipType == null)
			return 90;
		return switch(relationshipType.toLowerCase(Locale.ROOT)){
			case "biological_child" -> 10;
			case "adoptive_child" -> 20;
			case "foster_child" -> 30;
			case "guarded_child" -> 40;
			case "step_child" -> 50;
			default -> 60;
		};
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

		for(final FLEFRecord ep : model.getRecordsByType("event_participation")){
			final String eid = FLEFRecordHelper.getChildValue(ep, "event");
			if(!Objects.equals(eventId, eid))
				continue;

			final FLEFRecord pf = FLEFRecordHelper.findChild(ep, "participant");
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
