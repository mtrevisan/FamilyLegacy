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
package io.github.mtrevisan.familylegacy.groups;

import io.github.mtrevisan.familylegacy.events.EventHelper;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DatePrecision;
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.temporal.HistoricalPlaceScope;
import io.github.mtrevisan.familylegacy.temporal.HistoricalScope;
import io.github.mtrevisan.familylegacy.temporal.Lifetime;
import io.github.mtrevisan.familylegacy.temporal.TemporalContextBand;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntityRef;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntityType;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntry;
import io.github.mtrevisan.familylegacy.temporal.TemporalExtractor;
import io.github.mtrevisan.familylegacy.temporal.TemporalIndices;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionModel;
import io.github.mtrevisan.familylegacy.temporal.TemporalRow;
import io.github.mtrevisan.familylegacy.temporal.TemporalTrack;
import io.github.mtrevisan.familylegacy.temporal.TemporalTrackType;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


/**
 * Builds the model of the lifespan comparison: several individuals on a shared time axis, the same model of the general temporal projection,
 * so that its layout, axis and renderers show it (and the SVG writer draws it).
 * <p>
 * There is a row for each individual, the earliest born first, with:
 * <ol>
 *   <li>the bar of its life, from the birth to the death (see {@link Lifetime}); with no death, it goes to the latest date known;</li>
 *   <li>its events: all, or only the main ones (birth, marriage, divorce, death, burial, emigration, immigration);</li>
 *   <li>its attributes, if asked for;</li>
 *   <li>the historical context that a {@code context_impact} links to it.</li>
 * </ol>
 * Behind the rows are the historic events and the cultural norms that overlap the axis, as in the other timelines. With the place-aware
 * context, those that happened in a place that has no relation with the places of any of the individuals (see
 * {@link HistoricalPlaceScope}) are left out, so that the axis is not filled with the wars of another country.
 * <p>
 * Records that are not visible under the current privacy policy are left out.
 */
public final class LifespanComparisonBuilder{

	/** The events that are drawn when only the main ones are asked for. */
	public static final Set<String> KEY_EVENT_TYPES = Set.of(EventReader.ENUM_TYPE_BIRTH, EventReader.ENUM_TYPE_MARRIAGE,
		EventReader.ENUM_TYPE_DIVORCE, EventReader.ENUM_TYPE_DEATH, EventReader.ENUM_TYPE_BURIAL, EventReader.ENUM_TYPE_EMIGRATION,
		EventReader.ENUM_TYPE_IMMIGRATION);

	/** The type and the role of the bar of a life. */
	public static final String LIFE = "life";


	/**
	 * What the comparison shows.
	 *
	 * @param keyEventsOnly	Whether only the main events of the individuals are drawn (see {@link #KEY_EVENT_TYPES}).
	 * @param attributes	Whether the attributes of the individuals are drawn.
	 * @param historicContext	Whether the historic events and the cultural norms are drawn behind the rows.
	 * @param placeAware	Whether the historic context is only the one of the places of the individuals (and the world).
	 */
	public record Options(boolean keyEventsOnly, boolean attributes, boolean historicContext, boolean placeAware){

		/** @return	All the events, no attributes, the historic context of the places of the individuals. */
		public static Options defaults(){
			return new Options(false, false, true, true);
		}

	}


	private LifespanComparisonBuilder(){}


	/**
	 * @param model	The file.
	 * @param individualIds	The identifiers of the individuals to compare.
	 * @param options	What to show.
	 * @return	The model of the comparison, empty if none of the individuals is visible and has a date.
	 */
	public static TemporalProjectionModel build(final FLEFModel model, final List<String> individualIds, final Options options){
		final TemporalExtractor extractor = new TemporalExtractor(model, TemporalIndices.build(model));

		final List<TemporalRow> rows = new ArrayList<>(individualIds.size());
		final Set<String> places = new HashSet<>();
		for(final String id : new LinkedHashSet<>(individualIds)){
			final FLEFRecord individual = (id != null? model.getRecordById(id): null);
			if(!IndividualReader.isOfType(individual) || PrivacyService.visible(model, List.of(individual)).isEmpty())
				continue;

			final TemporalEntityRef ref = TemporalEntityRef.of(TemporalEntityType.INDIVIDUAL, individual,
				RecordDisplay.describe(individual, model));
			final TemporalRow row = rowOf(model, extractor.extractTracks(ref), ref, individual, options, places);
			if(row.hasEntries())
				rows.add(row);
		}
		if(rows.isEmpty())
			return TemporalProjectionModel.empty();

		rows.sort(Comparator.comparingLong(LifespanComparisonBuilder::birthOf)
			.thenComparing(row -> row.entity().displayLabel(), String.CASE_INSENSITIVE_ORDER)
			.thenComparing(row -> row.entity().id()));
		final List<TemporalRow> ordered = new ArrayList<>(rows.size());
		for(int i = 0; i < rows.size(); i ++)
			ordered.add(rows.get(i)
				.withSortOrder(i));

		final NormalizedDate start = GroupTimelineBuilder.domainStart(ordered);
		final NormalizedDate end = GroupTimelineBuilder.domainEnd(ordered);
		return new TemporalProjectionModel(ordered, bandsOf(model, start, end, places, options), start, end);
	}

	private static TemporalRow rowOf(final FLEFModel model, final List<TemporalTrack> extracted, final TemporalEntityRef ref,
			final FLEFRecord individual, final Options options, final Set<String> places){
		final List<TemporalEntry> life = new ArrayList<>();
		TemporalTrack events = null;
		TemporalTrack attributes = null;
		TemporalTrack context = null;
		for(final TemporalTrack track : extracted)
			switch(track.type()){
				case EVENT -> {
					events = track;
					life.addAll(track.entries());
				}
				case ATTRIBUTE -> {
					attributes = track;
					life.addAll(track.entries());
				}
				case CONTEXT -> context = track;
				default -> { /* not extracted */ }
			}

		final List<TemporalTrack> tracks = new ArrayList<>(4);
		final Lifetime lifetime = Lifetime.of(life);
		if(lifetime.isKnown())
			tracks.add(new TemporalTrack(TemporalTrackType.LIFESPAN, List.of(lifeBar(lifetime, individual)),
				I18N.t("text.historical.track.life")));
		if(events != null){
			for(final TemporalEntry entry : events.entries()){
				final String placeId = EventHelper.eventPlaceId(entry.sourceRecord());
				if(StringUtils.isNotBlank(placeId))
					places.add(placeId);
			}
			tracks.add(options.keyEventsOnly()
				? new TemporalTrack(TemporalTrackType.EVENT, events.entries().stream()
					.filter(entry -> KEY_EVENT_TYPES.contains(entry.type().toLowerCase(Locale.ROOT)))
					.toList(), events.label())
				: events);
		}
		if(options.attributes() && attributes != null)
			tracks.add(attributes);
		if(options.historicContext() && context != null)
			tracks.add(context);
		return new TemporalRow(ref, tracks, 0, false);
	}

	/** The bar of a life: from the birth to the death, or to the latest date known. */
	private static TemporalEntry lifeBar(final Lifetime lifetime, final FLEFRecord individual){
		final TemporalSpan span = TemporalSpan.spanning(NormalizedDate.exact(lifetime.start(), DatePrecision.DAY),
			NormalizedDate.exact(lifetime.end(), DatePrecision.DAY));
		final String birth = (lifetime.birth() != null
			? StringUtils.defaultString(EventHelper.dateText(lifetime.birth().sourceRecord(), EventReader.TAG_DATE)): "?");
		final String death = (lifetime.death() != null
			? StringUtils.defaultString(EventHelper.dateText(lifetime.death().sourceRecord(), EventReader.TAG_DATE)): "?");
		return new TemporalEntry(span, birth + " – " + death, LIFE, LIFE, individual, null);
	}

	private static long birthOf(final TemporalRow row){
		for(final TemporalTrack track : row.tracks())
			if(track.type() == TemporalTrackType.LIFESPAN && !track.isEmpty())
				return track.entries().getFirst().span().minJdn();
		return Long.MAX_VALUE;
	}

	/**
	 * The historical context for a strip of lives: the historic events and cultural norms that overlap the given days and happened where
	 * the individuals lived (or in the world), the earliest first.
	 *
	 * @param model	The file.
	 * @param individualIds	The individuals of the strip.
	 * @param start	The first day of the axis.
	 * @param end	The last day of the axis.
	 * @return	The bands to draw behind the rows.
	 */
	public static List<TemporalContextBand> contextBandsOf(final FLEFModel model, final Collection<String> individualIds,
			final NormalizedDate start, final NormalizedDate end){
		final TemporalIndices indices = TemporalIndices.build(model);
		final Set<String> places = new HashSet<>();
		for(final String id : individualIds)
			for(final FLEFRecord participation : indices.eventParticipationsFor(id)){
				final String eventId = (FLEFRecordHelper
					.getChildValue(participation, EventParticipationReader.TAG_EVENT));
				final FLEFRecord event = (eventId != null ? model.getRecordById(eventId) : null);
				final String placeId = EventHelper.eventPlaceId(event);
				if(StringUtils.isNotBlank(placeId))
					places.add(placeId);
			}
		return bandsOf(model, start, end, places, Options.defaults());
	}

	private static List<TemporalContextBand> bandsOf(final FLEFModel model, final NormalizedDate start, final NormalizedDate end,
			final Set<String> places, final Options options){
		if(!options.historicContext())
			return List.of();

		final List<TemporalContextBand> bands = GroupTimelineBuilder.contextBands(model, start, end);
		if(!options.placeAware())
			return bands;

		final HistoricalPlaceScope scope = new HistoricalPlaceScope(model, places);
		return bands.stream()
			.filter(band -> scope.scopeOf(band.place() != null? band.place().getId(): null) != HistoricalScope.ELSEWHERE)
			.toList();
	}

}
