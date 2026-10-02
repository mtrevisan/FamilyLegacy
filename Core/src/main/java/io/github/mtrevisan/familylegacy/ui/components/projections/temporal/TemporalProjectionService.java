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
package io.github.mtrevisan.familylegacy.ui.components.projections.temporal;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.ContextImpactReader;
import io.github.mtrevisan.familylegacy.io.model.readers.CulturalNormReader;
import io.github.mtrevisan.familylegacy.io.model.readers.HistoricEventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;


/**
 * Orchestrates the construction of a {@link TemporalProjectionModel} from a
 * FLEF model.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>enumerate the candidate row entities (individuals, groups, places)
 *       and apply the inclusion filter;</li>
 *   <li>extract the temporal tracks for each included row via
 *       {@link TemporalExtractor};</li>
 *   <li>build the {@link TemporalConnection} arcs from
 *       {@code RelationshipRecord} instances whose both endpoints are
 *       included;</li>
 *   <li>build the {@link TemporalContextBand} bands from
 *       {@code HistoricEventRecord} and {@code CulturalNormRecord}
 *       instances;</li>
 *   <li>build the {@link ContextImpactLink} links from
 *       {@code ContextImpactRecord} instances whose target is included;</li>
 *   <li>compute the temporal domain of the model.</li>
 * </ul>
 * The service caches the last built model; {@link #invalidate()} clears the
 * cache when the underlying model changes.
 */
public final class TemporalProjectionService{

	private final FLEFModel model;

	private TemporalIndices indices;
	private TemporalExtractor extractor;
	private TemporalProjectionModel cachedModel;


	public TemporalProjectionService(final FLEFModel model){
		this.model = model;
	}


	/**
	 * Builds a projection including every row entity that carries at least
	 * one dated entry.
	 *
	 * @return the projection model
	 */
	public TemporalProjectionModel build(){
		return build(entity -> true);
	}

	public TemporalProjectionModel build(final Predicate<TemporalEntityRef> filter){
		final Predicate<TemporalEntityRef> effective = (filter != null? filter: entity -> true);

		// Build the indices exactly once per projection build.
		indices = TemporalIndices.build(model);
		extractor = new TemporalExtractor(model, indices);

		final List<TemporalRow> rows = buildRows(effective);
		final Set<String> includedIds = collectIds(rows);

		final List<TemporalConnection> connections = buildAllConnections(includedIds);
		final List<TemporalContextBand> bands = buildBands();
		final List<ContextImpactLink> impacts = buildImpactLinks(rows, connections, bands);

		cachedModel = TemporalProjectionModel.of(rows, connections, bands, impacts);
		return cachedModel;
	}

	/**
	 * Returns the last built model, or {@code null} if none has been built.
	 *
	 * @return the cached model, or {@code null}
	 */
	public TemporalProjectionModel getCachedModel(){
		return cachedModel;
	}

	/**
	 * Invalidates the cached projection. Must be called whenever the
	 * underlying FLEF model changes.
	 */
	public void invalidate(){
		cachedModel = null;
	}


	/* ======================================================================
	 *                       Rows
	 * ====================================================================== */

	private List<TemporalRow> buildRows(final Predicate<TemporalEntityRef> filter){
		final List<TemporalRow> rows = new ArrayList<>();

		collectRows(IndividualHandler.TYPE, TemporalEntityType.INDIVIDUAL, filter, rows);
		collectRows(GroupHandler.TYPE, TemporalEntityType.GROUP, filter, rows);
		collectRows(PlaceHandler.TYPE, TemporalEntityType.PLACE, filter, rows);

		// Stable sort: entity type, then display label, then id.
		rows.sort((a, b) -> {
			int cmp = a.entity().type().compareTo(b.entity().type());
			if(cmp == 0)
				cmp = a.entity().displayLabel().compareToIgnoreCase(b.entity().displayLabel());
			if(cmp == 0)
				cmp = a.entity().id().compareTo(b.entity().id());
			return cmp;
		});

		// Recompute the sort order after sorting.
		final List<TemporalRow> ordered = new ArrayList<>(rows.size());
		for(int i = 0; i < rows.size(); i ++)
			ordered.add(rows.get(i)
				.withSortOrder(i));
		return ordered;
	}

	/**
	 * Collects the rows for the given entity type, using the appropriate
	 * {@link RecordTypeHandler} to compute the display label of each entity.
	 */
	private void collectRows(final String recordType, final TemporalEntityType entityType,
			final Predicate<TemporalEntityRef> filter, final List<TemporalRow> output){
		final RecordTypeHandler<?> handler = handlerFor(entityType);
		final List<FLEFRecord> records = model.getRecordsByType(recordType);
		for(final FLEFRecord record : records){
			final String id = record.getId();
			if(id == null)
				continue;

			final String label = computeLabel(handler, record);
			final TemporalEntityRef ref = new TemporalEntityRef(entityType, id, record, label);
			if(!filter.test(ref))
				continue;

			final List<TemporalTrack> tracks = extractor.extractTracks(ref);
			final TemporalRow row = new TemporalRow(ref, tracks, 0, false);
			if(row.hasEntries())
				output.add(row);
		}
	}

	/**
	 * Returns the singleton handler for the given row entity type.
	 */
	private static RecordTypeHandler<?> handlerFor(final TemporalEntityType type){
		return switch(type){
			case INDIVIDUAL -> IndividualHandler.getInstance();
			case GROUP -> GroupHandler.getInstance();
			case PLACE -> PlaceHandler.getInstance();
			default -> throw new IllegalArgumentException(
				"No handler for row entity type: " + type);
		};
	}

	/**
	 * Computes the display label for the given record using its handler,
	 * falling back to the record id if the handler returns an empty text or
	 * throws.
	 */
	private String computeLabel(final RecordTypeHandler<?> handler, final FLEFRecord record){
		try{
			final String text = handler.getDisplayText(record, model);
			if(text != null && !text.isBlank())
				return text;
		}
		catch(final RuntimeException ignored){
			// Fall through to the id-based fallback.
		}
		return (record.getId() != null? record.getId(): "?");
	}

	private static Set<String> collectIds(final List<TemporalRow> rows){
		final Set<String> ids = new HashSet<>(rows.size());
		for(final TemporalRow row : rows)
			ids.add(row.entity().id());
		return ids;
	}


	/* ======================================================================
	 *                       Connections
	 * ====================================================================== */

	/**
	 * Builds all temporal connections: individual/group relationships and
	 * place relationships. Both produce arcs between rows, distinguished only
	 * by their {@link TemporalConnectionType}.
	 */
	private List<TemporalConnection> buildAllConnections(final Set<String> includedIds){
		final List<TemporalConnection> result = new ArrayList<>();
		result.addAll(buildRelationshipConnections(includedIds));
		result.addAll(buildPlaceConnections(includedIds));
		return result;
	}

	/**
	 * Builds connections from {@code RelationshipRecord} instances whose both
	 * endpoints are included rows.
	 */
	private List<TemporalConnection> buildRelationshipConnections(final Set<String> includedIds){
		final List<TemporalConnection> connections = new ArrayList<>();
		final Map<String, FLEFRecord> recordIndex = new HashMap<>();

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String subjectId = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualHandler.TYPE);
			final String objectId = relationship.extractReferencedId(RelationshipReader.TAG_OBJECT, IndividualHandler.TYPE);
			if(subjectId == null || objectId == null)
				continue;
			if(!includedIds.contains(subjectId) || !includedIds.contains(objectId))
				continue;

			final FLEFRecord subject = indexLookup(recordIndex, subjectId);
			final FLEFRecord object = indexLookup(recordIndex, objectId);
			if(subject == null || object == null)
				continue;

			final TemporalEntityRef sourceRef = toRowRef(subject, includedIds);
			final TemporalEntityRef objectRef = toRowRef(object, includedIds);
			if(sourceRef == null || objectRef == null)
				continue;

			final TemporalSpan span = buildConnectionSpan(relationship);
			if(span == null)
				continue;

			final String relType = RelationshipReader.extractType(relationship);
			final String role = FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_ROLE);
			final TemporalConnectionType connectionType = TemporalConnectionType.of(sourceRef.type(), objectRef.type());

			connections.add(new TemporalConnection(connectionType, sourceRef, objectRef, relType, span, relationship,
				role));
		}
		return connections;
	}

	/**
	 * Builds connections from {@code PlaceRelationshipRecord} instances whose
	 * both endpoints are included rows. Place relationships have no
	 * {@code status} field in the protocol, so the span status is always
	 * {@link TemporalSpan#STATUS_UNKNOWN}.
	 */
	private List<TemporalConnection> buildPlaceConnections(final Set<String> includedIds){
		final List<TemporalConnection> result = new ArrayList<>();

		final List<FLEFRecord> placeRelationships = model.getRecordsByType(PlaceRelationshipHandler.TYPE);
		for(final FLEFRecord placeRelationship : placeRelationships){
			final String subjectId = placeRelationship.extractReferencedId(PlaceRelationshipReader.TAG_SUBJECT, PlaceHandler.TYPE);
			final String objectId = placeRelationship.extractReferencedId(PlaceRelationshipReader.TAG_OBJECT, PlaceHandler.TYPE);
			if(subjectId == null || objectId == null)
				continue;
			if(!includedIds.contains(subjectId) || !includedIds.contains(objectId))
				continue;

			final FLEFRecord subject = model.getRecordById(subjectId);
			final FLEFRecord object = model.getRecordById(objectId);
			if(subject == null || object == null)
				continue;

			final TemporalEntityRef sourceRef = toRowRef(subject, includedIds);
			final TemporalEntityRef objectRef = toRowRef(object, includedIds);
			if(sourceRef == null || objectRef == null)
				continue;

			final TemporalSpan span = buildConnectionSpan(placeRelationship);
			if(span == null)
				continue;

			final String relType = FLEFRecordHelper.getChildValue(placeRelationship, PlaceRelationshipReader.TAG_TYPE);
			if(relType == null)
				continue;

			result.add(new TemporalConnection(
				TemporalConnectionType.PLACE_PLACE,
				sourceRef, objectRef,
				relType, span, placeRelationship, StringUtils.EMPTY));
		}
		return result;
	}

	private FLEFRecord indexLookup(final Map<String, FLEFRecord> index, final String id){
		FLEFRecord record = index.get(id);
		if(record == null){
			record = model.getRecordById(id);
			if(record != null)
				index.put(id, record);
		}
		return record;
	}

	private TemporalEntityRef toRowRef(final FLEFRecord record, final Set<String> includedIds){
		final TemporalEntityType type = rowTypeOf(record);
		if(type == null)
			return null;
		if(!includedIds.contains(record.getId()))
			return null;
		final String label = computeLabel(handlerFor(type), record);
		return new TemporalEntityRef(type, record.getId(), record, label);
	}

	private static TemporalEntityType rowTypeOf(final FLEFRecord record){
		final String tag = record.getTag();
		if(tag == null)
			return null;
		return switch(tag){
			case "individual" -> TemporalEntityType.INDIVIDUAL;
			case "group" -> TemporalEntityType.GROUP;
			case "place" -> TemporalEntityType.PLACE;
			default -> null;
		};
	}

	private TemporalSpan buildConnectionSpan(final FLEFRecord relationship){
		final boolean isRelationship = RelationshipHandler.TYPE.equals(relationship.getTag());
		final FLEFRecord from = FLEFRecordHelper.findChild(relationship, (isRelationship? RelationshipReader.TAG_VALID_FROM: PlaceRelationshipReader.TAG_VALID_FROM));
		final FLEFRecord to = FLEFRecordHelper.findChild(relationship, (isRelationship? RelationshipReader.TAG_VALID_TO: PlaceRelationshipReader.TAG_VALID_TO));

		final String status = (isRelationship
			? normalizeStatus(FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_STATUS))
			: null);

		return DateNormalizer.combineBounds(from, to, status);
	}

	private static String normalizeStatus(final String raw){
		if(raw == null)
			return TemporalSpan.STATUS_UNKNOWN;
		return switch(raw.toLowerCase()){
			case TemporalSpan.STATUS_ACTIVE -> TemporalSpan.STATUS_ACTIVE;
			case TemporalSpan.STATUS_ENDED -> TemporalSpan.STATUS_ENDED;
			default -> TemporalSpan.STATUS_UNKNOWN;
		};
	}


	/* ======================================================================
	 *                       Bands
	 * ====================================================================== */

	private List<TemporalContextBand> buildBands(){
		final List<TemporalContextBand> bands = new ArrayList<>();
		collectHistoricEventBands(bands);
		collectCulturalNormBands(bands);
		return bands;
	}

	private void collectHistoricEventBands(final List<TemporalContextBand> output){
		final List<FLEFRecord> historicEvents = model.getRecordsByType(HistoricEventHandler.TYPE);
		for(final FLEFRecord historicEvent : historicEvents){
			final String id = historicEvent.getId();
			if(id == null)
				continue;
			final String title = FLEFRecordHelper.getChildValue(historicEvent, HistoricEventReader.TAG_TITLE);
			final String type = FLEFRecordHelper.getChildValue(historicEvent, HistoricEventReader.TAG_TYPE);
			final TemporalEntityRef ref = new TemporalEntityRef(
				TemporalEntityType.HISTORIC_EVENT, id, historicEvent,
				(title != null? title: type != null? type: id));

			final FLEFRecord date = FLEFRecordHelper.findChild(historicEvent, HistoricEventReader.TAG_DATE);
			final TemporalSpan span = DateNormalizer.normalize(date);
			if(span == null)
				continue;

			final FLEFRecord placeRef = FLEFRecordHelper.findChild(historicEvent, HistoricEventReader.TAG_PLACE);
			final FLEFRecord place = resolvePlace(placeRef);

			output.add(new TemporalContextBand(ref, span, title != null? title: type, type, place));
		}
	}

	private void collectCulturalNormBands(final List<TemporalContextBand> output){
		final List<FLEFRecord> culturalNorms = model.getRecordsByType(CulturalNormHandler.TYPE);
		for(final FLEFRecord culturalNorm : culturalNorms){
			final String id = culturalNorm.getId();
			if(id == null)
				continue;

			final String title = FLEFRecordHelper.getChildValue(culturalNorm, CulturalNormReader.TAG_TITLE);
			final String ruleType = FLEFRecordHelper.getChildValue(culturalNorm, CulturalNormReader.TAG_TYPE);
			final TemporalEntityRef ref = new TemporalEntityRef(
				TemporalEntityType.CULTURAL_NORM, id, culturalNorm,
				(title != null? title: ruleType != null? ruleType: id));

			final FLEFRecord from = FLEFRecordHelper.findChild(culturalNorm, CulturalNormReader.TAG_VALID_FROM);
			final FLEFRecord to = FLEFRecordHelper.findChild(culturalNorm, CulturalNormReader.TAG_VALID_TO);
			final TemporalSpan span = DateNormalizer.combineBounds(from, to);
			if(span == null)
				continue;

			final FLEFRecord placeRef = FLEFRecordHelper.findChild(culturalNorm, CulturalNormReader.TAG_PLACE);
			final FLEFRecord place = resolvePlace(placeRef);

			output.add(new TemporalContextBand(ref, span, title != null? title: ruleType, ruleType, place));
		}
	}

	private FLEFRecord resolvePlace(final FLEFRecord placeCitation){
		if(placeCitation == null)
			return null;
		final FLEFRecord ref = FLEFRecordHelper.findChild(placeCitation, "place");
		if(ref == null)
			return null;
		final String id = ref.getValue();
		return (id != null? model.getRecordById(id): null);
	}


	/* ======================================================================
	 *                       Impact links
	 * ====================================================================== */

	/**
	 * Builds the impact links from {@code ContextImpactRecord} instances whose
	 * context band and target are both part of the projection.
	 * <p>
	 * Targets are resolved against three indices, in this order:
	 * <ol>
	 *   <li>connection refs (indexed by the id of the source relationship or
	 *       place relationship record);</li>
	 *   <li>entry refs (indexed by the id of the source FLEF record of the
	 *       entry, e.g. the event, the attribute, the historic event, the
	 *       cultural norm);</li>
	 *   <li>row refs (indexed by the entity id).</li>
	 * </ol>
	 * A single target may resolve to multiple projection elements — for
	 * example an event with several participants produces one entry per
	 * participant, hence one impact link per participant. All resolutions are
	 * emitted, so that every affected element carries the impact.
	 */
	private List<ContextImpactLink> buildImpactLinks(final List<TemporalRow> rows,
		final List<TemporalConnection> connections, final List<TemporalContextBand> bands){
		final Map<String, TemporalContextBand> bandsByEntityId = new HashMap<>();
		for(final TemporalContextBand band : bands)
			bandsByEntityId.put(band.entity().id(), band);

		// Index 1: connections by source record id.
		final Map<String, List<TemporalProjectionRef>> refsById = new HashMap<>();
		for(final TemporalConnection connection : connections){
			final String id = connection.sourceRecord()
				.getId();
			refsById.computeIfAbsent(id, k -> new ArrayList<>())
				.add(new TemporalProjectionRef.ConnectionRef(connection));
		}

		// Index 2: entries by source record id.
		for(final TemporalRow row : rows)
			for(final TemporalTrack track : row.tracks())
				for(final TemporalEntry entry : track.entries()){
					final FLEFRecord source = entry.sourceRecord();
					if(source == null || source.getId() == null)
						continue;
					refsById.computeIfAbsent(source.getId(), k -> new ArrayList<>())
						.add(new TemporalProjectionRef.EntryRef(row.entity(), entry));
				}

		// Index 3: rows by entity id.
		for(final TemporalRow row : rows)
			refsById.computeIfAbsent(row.entity()
					.id(), k -> new ArrayList<>())
				.add(new TemporalProjectionRef.RowRef(row.entity()));

		// Build impact links.
		final List<ContextImpactLink> links = new ArrayList<>();
		final List<FLEFRecord> contextImpacts = model.getRecordsByType(ContextImpactHandler.TYPE);
		for(final FLEFRecord contextImpact : contextImpacts){
			final FLEFRecord contextRef = FLEFRecordHelper.findChild(contextImpact, ContextImpactReader.TAG_CONTEXT);
			if(contextRef == null)
				continue;
			final FLEFRecord contextRefChild = contextRef.getTheOnlyChild();
			if(contextRefChild == null || contextRefChild.getValue() == null)
				continue;
			final TemporalContextBand band = bandsByEntityId.get(contextRefChild.getValue());
			if(band == null)
				continue;

			final FLEFRecord targetRef = FLEFRecordHelper.findChild(contextImpact, ContextImpactReader.TAG_TARGET);
			if(targetRef == null)
				continue;
			final FLEFRecord targetRefChild = targetRef.getTheOnlyChild();
			if(targetRefChild == null || targetRefChild.getValue() == null)
				continue;
			final String targetId = targetRefChild.getValue();

			final List<TemporalProjectionRef> resolved = refsById.get(targetId);
			if(resolved == null || resolved.isEmpty())
				continue;

			final String impactType = FLEFRecordHelper.getChildValue(contextImpact, ContextImpactReader.TAG_IMPACT_TYPE);
			final String rationale = FLEFRecordHelper.getChildValue(contextImpact, ContextImpactReader.TAG_RATIONALE);
			for(final TemporalProjectionRef ref : resolved)
				links.add(new ContextImpactLink(band, ref, impactType, rationale));
		}
		return links;
	}

}
