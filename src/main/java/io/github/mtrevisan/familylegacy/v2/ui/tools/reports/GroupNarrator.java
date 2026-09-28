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
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.index.RelationIndex;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Builds a rich, detailed narrative biography for a group/family unit based on
 * its events, union rites (marriage, religious marriage), attributes, and life cycle.
 *
 * <p>Narrative order reflects the group's lifecycle:</p>
 * <ol>
 *   <li>group establishment (explicit founding or union events such as marriage);</li>
 *   <li>residences and geographic locations;</li>
 *   <li>group attributes (social status, organization type);</li>
 *   <li>group dissolution or end.</li>
 * </ol>
 */
final class GroupNarrator{

	private static final String TAG_TYPE = "type";
	private static final String TAG_VALUE = "value";
	private static final String TAG_NAME = "name";
	private static final String TAG_DESCRIPTION = "description";

	private static final String TYPE_FOUNDING = "founding";
	private static final String TYPE_DISSOLVED = "dissolved";
	private static final String TYPE_RESIDENCE = "residence";

	private static final String[] UNION_TYPES = {
		"marriage", "religious_marriage", "civil_marriage", "customary_marriage",
		"cohabitation", "annulment", "divorce"
	};


	private final FLEFModel model;
	private final RelationIndex idx;
	private final ReportLabels labels;


	GroupNarrator(final FLEFModel model, final RelationIndex idx, final ReportLabels labels){
		this.model = Objects.requireNonNull(model);
		this.idx = Objects.requireNonNull(idx);
		this.labels = Objects.requireNonNull(labels);
	}


	String narrate(final FLEFRecord group){
		if(group == null)
			return StringUtils.EMPTY;

		final List<FLEFRecord> events = idx.eventsOfGroup(group);
		final List<FLEFRecord> attrs = idx.attributesOfGroup(group);

		final String displayName = extractGroupName(group);
		final List<String> sentences = new ArrayList<>();

		// 1. Check explicit founding or union events (marriages/civil rites)
		final FLEFRecord founding = findEvent(events, TYPE_FOUNDING);
		final List<FLEFRecord> unions = findEvents(events, UNION_TYPES);

		if(founding != null){
			final String d = FLEFRecordHelper.extractDate(founding);
			final String p = FLEFRecordHelper.extractPlace(founding, model);
			sentences.add(labels.narrative().groupNarrativeFounding(displayName, d, p));
		}
		else if(!unions.isEmpty()){
			for(final FLEFRecord unionEvt : unions){
				final String type = FLEFRecordHelper.getChildValue(unionEvt, TAG_TYPE);
				final String d = FLEFRecordHelper.extractDate(unionEvt);
				final String p = FLEFRecordHelper.extractPlace(unionEvt, model);

				final String typeLabel = (type != null? type.replace('_', ' '): "union");
				if(d != null || p != null){
					final StringBuilder sb = new StringBuilder();
					sb.append(displayName).append(" was established through ").append(typeLabel);
					if(d != null)
						sb.append(" on ")
							.append(d);
					if(p != null)
						sb.append(" in ")
							.append(p);
					sb.append('.');
					sentences.add(sb.toString());
				}
			}
		}
		else
			sentences.add(labels.narrative().groupNarrativeFoundingUnknown(displayName));

		// 2. Residences and location attributes
		for(final FLEFRecord a : attrs){
			final String t = FLEFRecordHelper.getChildValue(a, TAG_TYPE);
			if(TYPE_RESIDENCE.equalsIgnoreCase(t)){
				final String v = FLEFRecordHelper.getChildValue(a, TAG_VALUE);
				final String p = FLEFRecordHelper.extractPlace(a, model);
				final String loc = (v != null && !v.isBlank()) ? v.trim() : p;
				if(loc != null && !loc.isBlank())
					sentences.add(labels.narrative().groupNarrativeResidence(displayName, loc));
			}
		}

		// 3. Descriptive events/notes attached to group
		for(final FLEFRecord e : events){
			final String type = FLEFRecordHelper.getChildValue(e, TAG_TYPE);
			if(TYPE_FOUNDING.equalsIgnoreCase(type) || isUnionType(type))
				continue;

			final String desc = FLEFRecordHelper.getChildValue(e, TAG_DESCRIPTION);
			if(desc != null && !desc.isBlank())
				sentences.add(desc.trim());
		}

		// 4. Dissolution
		final FLEFRecord dissolution = findEvent(events, TYPE_DISSOLVED);
		if(dissolution != null){
			final String d = FLEFRecordHelper.extractDate(dissolution);
			final String p = FLEFRecordHelper.extractPlace(dissolution, model);
			sentences.add(labels.narrative().groupNarrativeDissolution(displayName, d, p));
		}

		return String.join(StringUtils.SPACE, sentences);
	}

	private String extractGroupName(final FLEFRecord group){
		for(final FLEFRecord n : FLEFRecordHelper.findChildren(group, TAG_NAME)){
			final String val = FLEFRecordHelper.getChildValue(n, TAG_VALUE);
			if(val != null && !val.isBlank())
				return val.trim();
		}
		return ReportFormatters.orEmpty(group.getId());
	}

	private static FLEFRecord findEvent(final List<FLEFRecord> events, final String type){
		for(final FLEFRecord e : events){
			final String t = FLEFRecordHelper.getChildValue(e, TAG_TYPE);
			if(type.equalsIgnoreCase(t))
				return e;
		}
		return null;
	}

	private static List<FLEFRecord> findEvents(final List<FLEFRecord> events, final String... types){
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

	private static boolean isUnionType(final String type){
		if(type == null)
			return false;

		for(final String u : UNION_TYPES)
			if(u.equalsIgnoreCase(type))
				return true;
		return false;
	}

}
