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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.reports.index.EventIndex;
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import io.github.mtrevisan.reports.ReportSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;


/**
 * Builds the Index of Individuals and Index of Places sections.
 *
 * <p>Each index is a single-column table with a meaningful header
 * ({@code Individual} / {@code Place}), so the reader knows what the column
 * represents. When the index is empty, a paragraph with the localized
 * {@code (empty)} marker is emitted instead of a table with a
 * mis-labelled header.</p>
 */
public final class IndexSection implements SectionBuilder{

	private static final String TAG_PLACE = PlaceCitationReader.TAG_PLACE;
	private static final String TAG_ORIGINAL_TEXT = PlaceCitationReader.TAG_ORIGINAL_TEXT;

	private static final String TAG_PLACE_ORIGINAL_TEXT = FLEFRecordHelper.composePath(TAG_PLACE, TAG_ORIGINAL_TEXT);


	private final ReportContext ctx;


	public IndexSection(final ReportContext ctx){
		this.ctx = ctx;
	}


	/* ======================================================================
	 *                          Build
	 * ====================================================================== */

	@Override
	public List<ReportSection> build(){
		final List<ReportSection> out = new ArrayList<>();
		if(ctx.config.indexIndividuals())
			out.addAll(individualsIndex());
		if(ctx.config.indexPlaces())
			out.addAll(indexOf(ctx.labels.indexOfPlaces(),
				ctx.labels.sections().columnPlace(),
				individualPlaces(), s -> s));
		return out;
	}


	/**
	 * The index of the individuals: by family name, then by the other names (as a book is), with the years of birth and death.
	 */
	private List<ReportSection> individualsIndex(){
		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, ctx.labels.indexOfIndividuals()));

		final List<FLEFRecord> individuals = relatedIndividuals();
		if(individuals.isEmpty()){
			out.add(new ReportSection.Paragraph(ctx.labels.sections().empty()));
			return out;
		}

		final Locale locale = ctx.labels.language().locale();
		final Map<FLEFRecord, String> keys = new IdentityHashMap<>();
		for(final FLEFRecord individual : individuals)
			keys.put(individual, FamilyBookGenerator.sortName(individual, locale));
		final Comparator<String> order = ReportCollation.of(locale);
		individuals.sort(Comparator.comparing((final FLEFRecord individual) -> FamilyBookGenerator.NO_NAME.equals(keys.get(individual)))
			.thenComparing(individual -> keys.get(individual), order)
			.thenComparing(FLEFRecord::getId));

		final TreeText text = new TreeText(ctx);
		final List<List<String>> rows = new ArrayList<>(individuals.size());
		for(final FLEFRecord individual : individuals)
			rows.add(List.of(ReportFormatters.escape(ctx.displayText(individual)), ReportFormatters.orEmpty(text.years(individual))));
		out.add(new ReportSection.Table(List.of(ctx.labels.sections().columnIndividual(), ctx.labels.sections().columnYears()), rows));
		return out;
	}


	/* ======================================================================
	 *                          Generic index renderer
	 * ====================================================================== */

	/**
	 * Renders a single-column index.
	 *
	 * @param title       the section heading
	 * @param columnLabel the column header (e.g. {@code Individual})
	 * @param items       the records or strings to list
	 * @param label       a function producing the display text of an item
	 */
	private <T> List<ReportSection> indexOf(final String title, final String columnLabel,
		final Collection<T> items, final Function<T, String> label){
		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, title));

		if(items.isEmpty()){
			out.add(new ReportSection.Paragraph(ctx.labels.sections().empty()));
			return out;
		}

		final List<T> sorted = new ArrayList<>(items);
		sorted.sort(Comparator.comparing(label, ReportCollation.of(ctx.labels.language().locale())));

		final List<List<String>> rows = new ArrayList<>(sorted.size());
		for(final T t : sorted)
			rows.add(List.of(ReportFormatters.escape(label.apply(t))));

		out.add(new ReportSection.Table(List.of(columnLabel), rows));
		return out;
	}


	/* ======================================================================
	 *                          Collectors
	 * ====================================================================== */

	private List<FLEFRecord> relatedIndividuals(){
		final Map<String, FLEFRecord> byId = new LinkedHashMap<>();
		//the root of the report is not an individual in a report about a source, a place, ...
		for(final FLEFRecord r : ctx.index.reachable(ctx.root))
			if(IndividualReader.isOfType(r))
				byId.putIfAbsent(r.getId(), r);
		for(final FLEFRecord r : ctx.index.indirectRelations(ctx.root))
			byId.putIfAbsent(r.getId(), r);
		//the individuals that take part in what happens at a place, or that cite a source
		switch(ReportContext.rootKindOf(ctx.root)){
			case PLACE -> {
				for(final FLEFRecord event : ctx.index.eventsAtPlace(ctx.root))
					addParticipants(byId, event);
				for(final FLEFRecord event : ctx.index.eventsByPlace(ctx.root))
					addParticipants(byId, event);
			}
			case SOURCE -> {
				for(final FLEFRecord citation : ctx.index.citationsOfSource(ctx.root)){
					final FLEFRecord owner = ctx.ownerOfCitation(citation);
					if(owner == null)
						continue;
					if(IndividualReader.isOfType(owner) && ctx.isVisible(owner))
						byId.putIfAbsent(owner.getId(), owner);
					else
						addParticipants(byId, owner);
				}
			}
			default -> { /* nothing more */ }
		}
		//the members of a group
		if(ReportContext.rootKindOf(ctx.root) == ReportContext.RootKind.GROUP)
			for(final FLEFRecord membership : ctx.index.membersOf(ctx.root)){
				final String memberId = GroupStorySection.memberIndividualId(membership);
				final FLEFRecord member = (memberId != null? ctx.visible(ctx.model.getRecordById(memberId)): null);
				if(member != null)
					byId.putIfAbsent(member.getId(), member);
			}

		final List<FLEFRecord> out = new ArrayList<>(byId.values());
		out.sort(Comparator.comparing((final FLEFRecord r) -> ctx.displayText(r), ReportCollation.of(ctx.labels.language().locale())));
		return out;
	}


	/** The individuals that take part in the event, if they can be shown. */
	private void addParticipants(final Map<String, FLEFRecord> byId, final FLEFRecord event){
		if(!EventReader.isOfType(event))
			return;

		for(final EventIndex.Participant participant : ctx.index.participantsOf(event)){
			final FLEFRecord person = participant.record();
			if(person != null && IndividualReader.isOfType(person) && ctx.isVisible(person))
				byId.putIfAbsent(person.getId(), person);
		}
	}


	private List<String> individualPlaces(){
		final Set<String> places = new LinkedHashSet<>();
		for(final FLEFRecord subject : ctx.subjects()){
			if(ReportContext.rootKindOf(subject) == ReportContext.RootKind.GROUP){
				for(final FLEFRecord e : ctx.index.eventsOfGroup(subject)) addPlacesOf(e, places);
				continue;
			}

			for(final FLEFRecord e : ctx.index.eventsOf(subject)) addPlacesOf(e, places);
			for(final FLEFRecord a : ctx.index.attributesOf(subject)) addPlacesOf(a, places);
		}
		final List<String> out = new ArrayList<>(places);
		out.sort(String.CASE_INSENSITIVE_ORDER);
		return out;
	}


	private void addPlacesOf(final FLEFRecord rec, final Set<String> out){
		final String original = FLEFRecordHelper.getChildValue(rec, TAG_PLACE_ORIGINAL_TEXT);
		if(original != null && !original.isBlank())
			out.add(original.trim());
		final String name = ReportFormatters.resolvePlaceName(ctx.model, rec);
		if(name != null && !name.isBlank())
			out.add(name.trim());
	}

}
