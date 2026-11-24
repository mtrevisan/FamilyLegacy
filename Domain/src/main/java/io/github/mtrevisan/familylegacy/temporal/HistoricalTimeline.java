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
package io.github.mtrevisan.familylegacy.temporal;

import io.github.mtrevisan.familylegacy.events.EventHelper;
import io.github.mtrevisan.familylegacy.events.EventSummary;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ContextImpactReader;
import io.github.mtrevisan.familylegacy.io.model.readers.CulturalNormReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.HistoricEventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The timeline of a person against history: the events and the attributes of the individual and, in the same list, the historic events
 * and the cultural norms of its time, in chronological order, each with the age of the person at that date.
 * <p>
 * <b>What is in.</b> A historic event or a cultural norm is listed if it overlaps the lifetime of the individual (see {@link Lifetime}),
 * or if a {@code context_impact} links it to the individual, to one of its events or to one of its attributes (it is then {@code linked},
 * and listed even if it falls outside the lifetime: a law of the year before the birth may explain the birth). A historic event or norm
 * that is not linked is also filtered by the {@link HistoricalScope}: how far its place is from the places where the individual lived
 * (the places of its events, see {@link HistoricalPlaceScope}); one that has no place is a world event. The local ones come first among those
 * of the same day.
 * <p>
 * Dates that cannot be read are left out, as in the other timelines. Records that are not visible under the current privacy policy are left
 * out.
 */
public final class HistoricalTimeline{

	/** What an item of the timeline is. */
	public enum Kind{
		/** An event of the individual. */
		LIFE_EVENT,
		/** An attribute of the individual. */
		ATTRIBUTE,
		/** A historic event. */
		HISTORIC_EVENT,
		/** A cultural norm. */
		CULTURAL_NORM
	}


	/**
	 * The filters of the timeline.
	 *
	 * @param scopes	The scopes of the historic events and cultural norms that are listed, if they are not linked to the individual.
	 * @param culturalNorms	Whether the cultural norms are listed.
	 * @param attributes	Whether the attributes of the individual are listed.
	 */
	public record Options(Set<HistoricalScope> scopes, boolean culturalNorms, boolean attributes){

		public Options{
			scopes = (scopes.isEmpty()? EnumSet.noneOf(HistoricalScope.class): EnumSet.copyOf(scopes));
		}


		/** @return	Everything that touches the places of the individual, and the world: not the places that are elsewhere. */
		public static Options defaults(){
			return new Options(EnumSet.of(HistoricalScope.LOCAL, HistoricalScope.REGIONAL, HistoricalScope.NATIONAL, HistoricalScope.WORLD),
				true, true);
		}

		/** @return	Everything. */
		public static Options all(){
			return new Options(EnumSet.allOf(HistoricalScope.class), true, true);
		}

	}

	/**
	 * A line of the timeline.
	 *
	 * @param kind	What it is.
	 * @param span	When it was.
	 * @param date	The date as written ("1850", "1850-03-02 – 1852"), empty if it has none.
	 * @param label	What it is: the type of the event, the title of the historic event.
	 * @param detail	More about it: the description of the event, the value of the attribute, the type of the historic event.
	 * @param place	The place, empty if it has none.
	 * @param scope	How far from the places of the individual a historic event or norm is, {@code null} for what belongs to the individual.
	 * @param linked	Whether a {@code context_impact} links a historic event or norm to the individual, to one of its events or attributes.
	 * @param impact	The impact type of that {@code context_impact} ("explains"), empty if it has none or the item is not linked.
	 * @param age	The age of the individual at the start of the item in whole years, {@code null} if it is not known.
	 * @param record	The record.
	 */
	public record Item(Kind kind, TemporalSpan span, String date, String label, String detail, String place, HistoricalScope scope, boolean linked,
			String impact, Integer age, FLEFRecord record){

		/** @return	Whether a {@code context_impact} links this item to the individual. */
		public boolean isLinked(){
			return linked;
		}

		/** @return	Whether the item is of the context (a historic event or a cultural norm) and not of the individual. */
		public boolean isContext(){
			return (kind == Kind.HISTORIC_EVENT || kind == Kind.CULTURAL_NORM);
		}

	}

	/**
	 * @param lifetime	The lifetime of the individual.
	 * @param items	The items, in chronological order.
	 */
	public record Result(Lifetime lifetime, List<Item> items){

		/** @return	An empty result. */
		public static Result empty(){
			return new Result(Lifetime.UNKNOWN, List.of());
		}

	}


	private HistoricalTimeline(){}


	/**
	 * @param model	The file.
	 * @param individualId	The identifier of the individual.
	 * @param options	The filters.
	 * @return	The timeline of the individual, empty if there is no such visible individual.
	 */
	public static Result build(final FLEFModel model, final String individualId, final Options options){
		final FLEFRecord individual = (individualId != null? model.getRecordById(individualId): null);
		if(!IndividualReader.isOfType(individual) || PrivacyService.visible(model, List.of(individual)).isEmpty())
			return Result.empty();

		final TemporalIndices indices = TemporalIndices.build(model);
		final TemporalEntityRef ref = TemporalEntityRef.of(TemporalEntityType.INDIVIDUAL, individual, individualId);
		final List<TemporalEntry> events = new ArrayList<>();
		final List<TemporalEntry> attributes = new ArrayList<>();
		for(final TemporalTrack track : new TemporalExtractor(model, indices).extractTracks(ref))
			switch(track.type()){
				case EVENT -> events.addAll(track.entries());
				case ATTRIBUTE -> attributes.addAll(track.entries());
				default -> { /* the historical context is rebuilt here, by place */ }
			}

		final List<TemporalEntry> dated = new ArrayList<>(events);
		dated.addAll(attributes);
		final Lifetime lifetime = Lifetime.of(dated);

		final HistoricalPlaceScope placeScope = new HistoricalPlaceScope(model, placesOf(events));
		final Map<String, String> linked = linkedContexts(indices, individualId, events, attributes);

		final List<Item> items = new ArrayList<>();
		for(final TemporalEntry entry : events)
			items.add(lifeEventItem(model, lifetime, entry));
		if(options.attributes())
			for(final TemporalEntry entry : attributes)
				items.add(attributeItem(lifetime, entry));
		for(final FLEFRecord record : indices.allHistoricEvents()){
			final TemporalSpan span = DateNormalizer.normalize(FLEFRecordHelper.findChild(record, HistoricEventReader.TAG_DATE));
			final Item item = contextItem(model, lifetime, placeScope, options, linked, record, span, Kind.HISTORIC_EVENT);
			if(item != null)
				items.add(item);
		}
		if(options.culturalNorms())
			for(final FLEFRecord record : indices.allCulturalNorms()){
				final TemporalSpan span = DateNormalizer.combineBounds(FLEFRecordHelper.findChild(record, CulturalNormReader.TAG_VALID_FROM),
					FLEFRecordHelper.findChild(record, CulturalNormReader.TAG_VALID_TO));
				final Item item = contextItem(model, lifetime, placeScope, options, linked, record, span, Kind.CULTURAL_NORM);
				if(item != null)
					items.add(item);
			}

		items.sort(Comparator.comparingLong(HistoricalTimeline::sortKey)
			.thenComparingInt((Item item) -> item.kind().ordinal())
			.thenComparing(item -> (item.scope() != null? item.scope().ordinal(): -1))
			.thenComparing(Item::label, String.CASE_INSENSITIVE_ORDER));
		return new Result(lifetime, items);
	}

	private static long sortKey(final Item item){
		final TemporalSpan span = item.span();
		//an interval with no start is placed by its end
		return (span.minJdn() != Long.MIN_VALUE? span.minJdn(): span.maxJdn());
	}

	/** The places of the individual: those of its events. */
	private static Set<String> placesOf(final List<TemporalEntry> events){
		final Set<String> places = new HashSet<>();
		for(final TemporalEntry entry : events){
			final String placeId = EventHelper.eventPlaceId(entry.sourceRecord());
			if(StringUtils.isNotBlank(placeId))
				places.add(placeId);
		}
		return places;
	}

	/** @return	The historic events and the cultural norms that a context impact links to the individual, its events or its attributes, with the type of impact. */
	private static Map<String, String> linkedContexts(final TemporalIndices indices, final String individualId,
			final List<TemporalEntry> events, final List<TemporalEntry> attributes){
		final Set<String> targets = new HashSet<>();
		targets.add(individualId);
		for(final TemporalEntry entry : events)
			if(entry.sourceRecord() != null && entry.sourceRecord().getId() != null)
				targets.add(entry.sourceRecord().getId());
		for(final TemporalEntry entry : attributes)
			if(entry.sourceRecord() != null && entry.sourceRecord().getId() != null)
				targets.add(entry.sourceRecord().getId());
		for(final FLEFRecord participation : indices.eventParticipationsFor(individualId))
			if(participation.getId() != null)
				targets.add(participation.getId());

		final Map<String, String> linked = new HashMap<>();
		for(final String target : targets)
			for(final FLEFRecord impact : indices.contextImpactsFor(target)){
				final FLEFRecord contextRef = FLEFRecordHelper.findChild(impact, ContextImpactReader.TAG_CONTEXT);
				final FLEFRecord context = (contextRef != null? contextRef.getTheOnlyChild(): null);
				if(context == null || context.getValue() == null)
					continue;

				final String type = StringUtils.defaultString(ContextImpactReader.extractImpactType(impact));
				//the first one found stays, an empty type being replaced by a real one
				linked.merge(context.getValue(), type, (old, added) -> (old.isEmpty()? added: old));
			}
		return linked;
	}


	private static Item lifeEventItem(final FLEFModel model, final Lifetime lifetime, final TemporalEntry entry){
		final FLEFRecord event = entry.sourceRecord();
		return new Item(Kind.LIFE_EVENT, entry.span(), StringUtils.defaultString(EventHelper.dateText(event, EventReader.TAG_DATE)),
			entry.label(), StringUtils.defaultString(EventHelper.eventDescription(event)),
			StringUtils.defaultString(EventSummary.placeName(event, model)), null, false, StringUtils.EMPTY, ageOf(lifetime, entry.span()), event);
	}

	private static Item attributeItem(final Lifetime lifetime, final TemporalEntry entry){
		return new Item(Kind.ATTRIBUTE, entry.span(), validityOf(entry.sourceRecord(), IndividualAttributeReader.TAG_VALID_FROM, IndividualAttributeReader.TAG_VALID_TO), entry.label(),
			entry.role(), StringUtils.EMPTY, null, false, StringUtils.EMPTY, ageOf(lifetime, entry.span()), entry.sourceRecord());
	}

	private static Item contextItem(final FLEFModel model, final Lifetime lifetime, final HistoricalPlaceScope placeScope,
			final Options options, final Map<String, String> linked, final FLEFRecord record, final TemporalSpan span, final Kind kind){
		if(span == null)
			return null;

		final String impact = linked.get(record.getId());
		final String placeId = EventHelper.eventPlaceId(record);
		final HistoricalScope scope = placeScope.scopeOf(placeId);
		//what is not linked has to be in the time of the individual and not too far from it
		if(impact == null && (!lifetime.overlaps(span) || !options.scopes().contains(scope)))
			return null;

		final boolean historic = (kind == Kind.HISTORIC_EVENT);
		final String title = FLEFRecordHelper.getChildValue(record, (historic? HistoricEventReader.TAG_TITLE: CulturalNormReader.TAG_TITLE));
		final String type = FLEFRecordHelper.getChildValue(record, (historic? HistoricEventReader.TAG_TYPE: CulturalNormReader.TAG_TYPE));
		final String typeLabel = (StringUtils.isBlank(type)? StringUtils.EMPTY
			: I18N.t((historic? "enum.historic_event.type.": "enum.cultural_norm.rule_type.") + type, type.replace('_', ' ')));
		final String fallback = I18N.t(historic? "text.temporal.historic_event": "text.temporal.cultural_norm");
		final String label = (StringUtils.isNotBlank(title)? title: (!typeLabel.isEmpty()? typeLabel: fallback));
		final String detail = (StringUtils.isNotBlank(title)? typeLabel: StringUtils.EMPTY);
		final String date = (historic
			? StringUtils.defaultString(EventHelper.dateText(record, HistoricEventReader.TAG_DATE))
			: validityOf(record, CulturalNormReader.TAG_VALID_FROM, CulturalNormReader.TAG_VALID_TO));
		final String place = (StringUtils.isNotBlank(placeId)? StringUtils.defaultString(EventSummary.placeName(record, model)): StringUtils.EMPTY);

		return new Item(kind, span, date, label, detail, place, scope, (impact != null), StringUtils.defaultString(impact), ageOf(lifetime, span), record);
	}

	private static Integer ageOf(final Lifetime lifetime, final TemporalSpan span){
		return (span.minJdn() != Long.MIN_VALUE? lifetime.ageAt(span.minJdn()): null);
	}

	/** @return	The two dates of a validity, as written: "from – to", or the one that is. */
	private static String validityOf(final FLEFRecord record, final String fromTag, final String toTag){
		final String from = StringUtils.defaultString(EventHelper.dateText(record, fromTag));
		final String to = StringUtils.defaultString(EventHelper.dateText(record, toTag));
		if(from.isEmpty() || to.isEmpty())
			return from + to;
		return from + " – " + to;
	}

}
