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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Extracts the {@link TemporalTrack} list for a row entity from the FLEF
 * model.
 * <p>
 * Three tracks are produced, one per {@link TemporalTrackType}:
 * <ul>
 *   <li>{@link TemporalTrackType#EVENT} — every
 *       {@code EventParticipationRecord} whose participant is the row entity,
 *       resolved to the referenced {@code EventRecord} and its date.</li>
 *   <li>{@link TemporalTrackType#ATTRIBUTE} — every
 *       {@code IndividualAttributeRecord} or {@code GroupAttributeRecord}
 *       owned by the row entity, using {@code valid_from} / {@code valid_to}
 *       to build the entry span.</li>
 *   <li>{@link TemporalTrackType#CONTEXT} — every
 *       {@code ContextImpactRecord} whose target is the row entity, using
 *       the context's own time range (from the referenced historic event
 *       or cultural norm) as the entry span.</li>
 * </ul>
 * Entries without any parsable date are skipped: the projection is a
 * temporal view and ignores undated assertions.
 */
public final class TemporalExtractor{

	// Event-related tags.
	private static final String TAG_TYPE = "type";
	private static final String TAG_ROLE = "role";
	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_EVENT = "event";
	private static final String TAG_DATE = "date";

	// Attribute-related tags.
	private static final String TAG_VALID_FROM = "valid_from";
	private static final String TAG_VALID_TO = "valid_to";
	private static final String TAG_VALUE = "value";

	// Context-related tags.
	private static final String TAG_CONTEXT = "context";
	private static final String TAG_TARGET = "target";
	private static final String TAG_IMPACT_TYPE = "impact_type";

	private static final String TAG_RULE_TYPE    = "rule_type";
	private static final String TAG_TITLE        = "title";


	private final FLEFModel model;
	private final DateNormalizer normalizer;
	private final TemporalIndices indices;


	public TemporalExtractor(final FLEFModel model, final DateNormalizer normalizer,
		final TemporalIndices indices){
		this.model = model;
		this.normalizer = normalizer;
		this.indices = indices;
	}


	/**
	 * Builds the temporal tracks for the given row entity. Each track is
	 * always present (possibly empty) so that the layout stage can rely on
	 * a stable structure.
	 *
	 * @param entity the row entity (must not be {@code null})
	 * @return the ordered list of tracks
	 */
	public List<TemporalTrack> extractTracks(final TemporalEntityRef entity){
		final List<TemporalEntry> events = extractEventEntries(entity);
		final List<TemporalEntry> attributes = extractAttributeEntries(entity);
		final List<TemporalEntry> contexts = extractContextEntries(entity);

		final List<TemporalTrack> tracks = new ArrayList<>(3);
		tracks.add(new TemporalTrack(TemporalTrackType.EVENT, events, "Events"));
		tracks.add(new TemporalTrack(TemporalTrackType.ATTRIBUTE, attributes, "Attributes"));
		tracks.add(new TemporalTrack(TemporalTrackType.CONTEXT, contexts, "Context"));
		return tracks;
	}


	/* ======================================================================
	 *                       EVENT track
	 * ====================================================================== */

	private List<TemporalEntry> extractEventEntries(final TemporalEntityRef entity){
		final List<TemporalEntry> result = new ArrayList<>();

		// Only iterate over the participations that actually reference this entity.
		for(final FLEFRecord participation : indices.eventParticipationsFor(entity.id())){
			if(!isParticipant(participation, entity))
				continue;

			final String eventId = FLEFRecordHelper.getChildValue(participation, TAG_EVENT);
			if(eventId == null)
				continue;

			final FLEFRecord event = model.getRecordById(eventId);
			if(event == null || !EventHandler.TYPE.equalsIgnoreCase(event.getTag()))
				continue;

			final FLEFRecord dateStructure = FLEFRecordHelper.findChild(event, TAG_DATE);
			final TemporalSpan span = normalizer.normalize(dateStructure);
			if(span == null)
				continue;

			final String type = FLEFRecordHelper.getChildValue(event, TAG_TYPE);
			final String role = FLEFRecordHelper.getChildValue(participation, TAG_ROLE);
			final String label = (type != null? type.replace('_', ' '): "event");

			result.add(new TemporalEntry(span, label, type, role, event, null));
		}
		return result;
	}

	/**
	 * Returns whether the given event participation record has the entity as
	 * participant. Both the id and the participant kind must match, so that
	 * a place and an individual that happen to share the same LocalID cannot
	 * cross-match.
	 */
	private static boolean isParticipant(final FLEFRecord participation, final TemporalEntityRef entity){
		final FLEFRecord participantRecord = FLEFRecordHelper.findChild(participation, TAG_PARTICIPANT);
		if(participantRecord == null)
			return false;
		final FLEFRecord ref = participantRecord.getTheOnlyChild();
		if(ref == null || ref.getValue() == null)
			return false;
		if(!entity.id().equals(ref.getValue()))
			return false;
		return matchesRefTag(entity.type(), ref.getTag());
	}

	/**
	 * Maps a {@link TemporalEntityType} to the FLEF participant reference tag.
	 */
	private static boolean matchesRefTag(final TemporalEntityType type, final String refTag){
		if(refTag == null)
			return false;
		return switch(type){
			case INDIVIDUAL     -> "individual".equalsIgnoreCase(refTag);
			case GROUP          -> "group".equalsIgnoreCase(refTag);
			case PLACE          -> "place".equalsIgnoreCase(refTag);
			case HISTORIC_EVENT -> HistoricEventHandler.TYPE.equalsIgnoreCase(refTag);
			case CULTURAL_NORM  -> CulturalNormHandler.TYPE.equalsIgnoreCase(refTag);
		};
	}


	/* ======================================================================
	 *                       ATTRIBUTE track
	 * ====================================================================== */

	private List<TemporalEntry> extractAttributeEntries(final TemporalEntityRef entity){
		return switch(entity.type()){
			case INDIVIDUAL -> extractIndividualAttributeEntries(entity);
			case GROUP -> extractGroupAttributeEntries(entity);
			case PLACE -> List.of();
			default -> List.of();
		};
	}

	private List<TemporalEntry> extractIndividualAttributeEntries(final TemporalEntityRef entity){
		final List<TemporalEntry> result = new ArrayList<>();
		for(final FLEFRecord attribute : indices.individualAttributesFor(entity.id())){
			final TemporalEntry entry = buildAttributeEntry(attribute);
			if(entry != null)
				result.add(entry);
		}
		return result;
	}

	private List<TemporalEntry> extractGroupAttributeEntries(final TemporalEntityRef entity){
		final List<TemporalEntry> result = new ArrayList<>();
		for(final FLEFRecord attribute : indices.groupAttributesFor(entity.id())){
			final TemporalEntry entry = buildAttributeEntry(attribute);
			if(entry != null)
				result.add(entry);
		}
		return result;
	}

	private TemporalEntry buildAttributeEntry(final FLEFRecord attribute){
		final FLEFRecord from = FLEFRecordHelper.findChild(attribute, TAG_VALID_FROM);
		final FLEFRecord to = FLEFRecordHelper.findChild(attribute, TAG_VALID_TO);
		final TemporalSpan span = normalizer.combineBounds(from, to);
		if(span == null)
			return null;

		final String type = FLEFRecordHelper.getChildValue(attribute, TAG_TYPE);
		final String value = FLEFRecordHelper.getChildValue(attribute, TAG_VALUE);
		final String label = (type != null? type.replace('_', ' '): "attribute");

		return new TemporalEntry(span, label, type, value, attribute, null);
	}

	private TemporalSpan buildAttributeSpan(final FLEFRecord from, final FLEFRecord to){
		return normalizer.combineBounds(from, to);
	}


	/* ======================================================================
	 *                       CONTEXT track
	 * ====================================================================== */

	/**
	 * Collects all entries for the CONTEXT track of the given entity. Three
	 * sources contribute:
	 * <ul>
	 *   <li>{@code ContextImpactRecord} instances whose target is the entity;</li>
	 *   <li>for PLACE rows only: {@code HistoricEventRecord} instances whose
	 *       {@code place} citation points to the place;</li>
	 *   <li>for PLACE rows only: {@code CulturalNormRecord} instances whose
	 *       {@code place} citation points to the place.</li>
	 * </ul>
	 */
	private List<TemporalEntry> extractContextEntries(final TemporalEntityRef entity){
		final List<TemporalEntry> result = new ArrayList<>();
		collectContextImpactEntries(entity, result);
		if(entity.type() == TemporalEntityType.PLACE){
			collectHistoricEventPlaceEntries(entity, result);
			collectCulturalNormPlaceEntries(entity, result);
		}
		return result;
	}

	/**
	 * Collects {@code ContextImpactRecord} entries targeting the given entity.
	 * Extracted from the previous version of {@code extractContextEntries}.
	 */
	private void collectContextImpactEntries(final TemporalEntityRef entity, final List<TemporalEntry> output){
		for(final FLEFRecord impact : indices.contextImpactsFor(entity.id())){
			final FLEFRecord contextRef = FLEFRecordHelper.findChild(impact, TAG_CONTEXT);
			if(contextRef == null)
				continue;
			final FLEFRecord contextRecord = contextRef.getTheOnlyChild();
			if(contextRecord == null || contextRecord.getValue() == null)
				continue;

			final FLEFRecord context = model.getRecordById(contextRecord.getValue());
			if(context == null)
				continue;

			final TemporalSpan span = contextSpan(context);
			if(span == null)
				continue;

			final String impactType = FLEFRecordHelper.getChildValue(impact, TAG_IMPACT_TYPE);
			final String label = context.getTag()
				.replace('_', ' ');
			output.add(new TemporalEntry(span, label, context.getTag(), impactType, context, null));
		}
	}

	private void collectHistoricEventPlaceEntries(final TemporalEntityRef entity, final List<TemporalEntry> output){
		for(final FLEFRecord record : indices.historicEventsFor(entity.id())){
			final FLEFRecord date = FLEFRecordHelper.findChild(record, TAG_DATE);
			final TemporalSpan span = normalizer.normalize(date);
			if(span == null)
				continue;

			final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
			final String title = FLEFRecordHelper.getChildValue(record, TAG_TITLE);
			final String label = (title != null? title: type != null? type: "historic event");
			output.add(new TemporalEntry(span, label, type, StringUtils.EMPTY, record, null));
		}
	}

	private void collectCulturalNormPlaceEntries(final TemporalEntityRef entity, final List<TemporalEntry> output){
		for(final FLEFRecord record : indices.culturalNormsFor(entity.id())){
			final FLEFRecord from = FLEFRecordHelper.findChild(record, TAG_VALID_FROM);
			final FLEFRecord to = FLEFRecordHelper.findChild(record, TAG_VALID_TO);
			final TemporalSpan span = normalizer.combineBounds(from, to);
			if(span == null)
				continue;

			final String ruleType = FLEFRecordHelper.getChildValue(record, TAG_RULE_TYPE);
			final String title = FLEFRecordHelper.getChildValue(record, TAG_TITLE);
			final String label = (title != null? title: ruleType != null? ruleType: "cultural norm");
			output.add(new TemporalEntry(span, label, ruleType, StringUtils.EMPTY, record, null));
		}
	}

	/**
	 * Returns whether the given record carries a {@code place} citation whose
	 * inner reference points to the given place id.
	 */
	private static boolean referencesPlace(final FLEFRecord record, final String placeId){
		final FLEFRecord placeCitation = FLEFRecordHelper.findChild(record, PlaceHandler.TYPE);
		if(placeCitation == null)
			return false;
		final FLEFRecord ref = FLEFRecordHelper.findChild(placeCitation, PlaceHandler.TYPE);
		return (ref != null && placeId.equals(ref.getValue()));
	}

	private static boolean isImpactTarget(final FLEFRecord impact, final TemporalEntityRef entity){
		final FLEFRecord targetRef = FLEFRecordHelper.findChild(impact, TAG_TARGET);
		if(targetRef == null)
			return false;
		final FLEFRecord ref = targetRef.getTheOnlyChild();
		if(ref == null || ref.getValue() == null)
			return false;
		return entity.id().equals(ref.getValue());
	}

	private TemporalSpan contextSpan(final FLEFRecord context){
		return switch(context.getTag()){
			case HistoricEventHandler.TYPE -> {
				final FLEFRecord date = FLEFRecordHelper.findChild(context, TAG_DATE);
				yield normalizer.normalize(date);
			}
			case CulturalNormHandler.TYPE -> {
				final FLEFRecord from = FLEFRecordHelper.findChild(context, TAG_VALID_FROM);
				final FLEFRecord to = FLEFRecordHelper.findChild(context, TAG_VALID_TO);
				yield buildAttributeSpan(from, to);
			}
			default -> null;
		};
	}

}
