package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.List;
import java.util.Objects;


/**
 * Builds a short narrative for a group from its events and attributes.
 *
 * <p>Sentence order follows the group's life cycle:</p>
 * <ol>
 *   <li>founding (date, place);</li>
 *   <li>residence (first {@code residence} attribute);</li>
 *   <li>dissolution (date, place).</li>
 * </ol>
 */
final class GroupNarrator{

	private static final String TAG_TYPE = "type";
	private static final String TAG_VALUE = "value";

	private static final String TYPE_FOUNDING = "founding";
	private static final String TYPE_DISSOLVED = "dissolved";
	private static final String TYPE_RESIDENCE = "residence";


	private final FLEFModel model;
	private final RelationIndex idx;
	private final ReportLabels labels;


	GroupNarrator(final FLEFModel model, final RelationIndex idx, final ReportLabels labels){
		this.model = Objects.requireNonNull(model);
		this.idx = Objects.requireNonNull(idx);
		this.labels = Objects.requireNonNull(labels);
	}


	String narrate(final FLEFRecord group){
		final List<FLEFRecord> events = idx.eventsOfGroup(group);

		final FLEFRecord founding = findEvent(events, TYPE_FOUNDING);
		final FLEFRecord dissolution = findEvent(events, TYPE_DISSOLVED);

		final String name = labels.language().locale().getLanguage().isEmpty()
			? "the group": null; // placeholder, overwritten below

		// Resolve the name using the caller's label; the narrator is not the
		// right place to format the group name, so we simply take the ID and
		// let the caller's sentence templates do the work.
		final String groupName = ReportFormatters.orEmpty(group.getId());

		final java.util.List<String> sentences = new java.util.ArrayList<>();

		if(founding != null){
			final String d = FLEFRecordHelper.extractDate(founding);
			final String p = FLEFRecordHelper.extractPlace(founding, model);
			sentences.add(labels.groupNarrativeFounding(groupName, d, p));
		}
		else
			sentences.add(labels.groupNarrativeFoundingUnknown(groupName));

		// Residence
		for(final FLEFRecord a : idx.attributesOfGroup(group)){
			final String t = FLEFRecordHelper.getChildValue(a, TAG_TYPE);
			if(!TYPE_RESIDENCE.equalsIgnoreCase(t))
				continue;
			final String v = FLEFRecordHelper.getChildValue(a, TAG_VALUE);
			if(v != null && !v.isBlank())
				sentences.add(labels.groupNarrativeResidence(groupName, v.trim()));
			break;
		}

		if(dissolution != null){
			final String d = FLEFRecordHelper.extractDate(dissolution);
			final String p = FLEFRecordHelper.extractPlace(dissolution, model);
			sentences.add(labels.groupNarrativeDissolution(groupName, d, p));
		}

		return String.join(" ", sentences);
	}


	private static FLEFRecord findEvent(final List<FLEFRecord> events, final String type){
		for(final FLEFRecord e : events){
			final String t = FLEFRecordHelper.getChildValue(e, TAG_TYPE);
			if(type.equalsIgnoreCase(t))
				return e;
		}
		return null;
	}

}
