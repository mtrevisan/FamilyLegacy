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
package io.github.mtrevisan.familylegacy.ui.tools.reports;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
final class IndexSection implements SectionBuilder{

	private static final String TAG_PLACE = "place";
	private static final String TAG_ORIGINAL_TEXT = "original_text";


	private final ReportContext ctx;


	IndexSection(final ReportContext ctx){
		this.ctx = ctx;
	}


	/* ======================================================================
	 *                          Build
	 * ====================================================================== */

	@Override
	public List<ReportSection> build(){
		final List<ReportSection> out = new ArrayList<>();
		if(ctx.config.indexIndividuals())
			out.addAll(indexOf(ctx.labels.indexOfIndividuals(),
				ctx.labels.sections().columnIndividual(),
				relatedIndividuals(), ctx::displayText));
		if(ctx.config.indexPlaces())
			out.addAll(indexOf(ctx.labels.indexOfPlaces(),
				ctx.labels.sections().columnPlace(),
				individualPlaces(), s -> s));
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
		sorted.sort(Comparator.comparing(
			t -> label.apply(t).toLowerCase(ctx.labels.language().locale())));

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
		for(final FLEFRecord r : ctx.index.reachable(ctx.root))
			byId.putIfAbsent(r.getId(), r);
		for(final FLEFRecord r : ctx.index.indirectRelations(ctx.root))
			byId.putIfAbsent(r.getId(), r);

		final List<FLEFRecord> out = new ArrayList<>(byId.values());
		out.sort(Comparator.comparing(
			(final FLEFRecord r) -> ctx.displayText(r)
				.toLowerCase(ctx.labels.language().locale())));
		return out;
	}


	private List<String> individualPlaces(){
		final Set<String> places = new LinkedHashSet<>();
		for(final FLEFRecord e : ctx.index.eventsOf(ctx.root)) addPlacesOf(e, places);
		for(final FLEFRecord a : ctx.index.attributesOf(ctx.root)) addPlacesOf(a, places);
		final List<String> out = new ArrayList<>(places);
		out.sort(String.CASE_INSENSITIVE_ORDER);
		return out;
	}


	private void addPlacesOf(final FLEFRecord rec, final Set<String> out){
		final String original = FLEFRecordHelper.getChildValue(rec,
			TAG_PLACE + "." + TAG_ORIGINAL_TEXT);
		if(original != null && !original.isBlank())
			out.add(original.trim());
		final String name = ReportFormatters.resolvePlaceName(ctx.model, rec);
		if(name != null && !name.isBlank())
			out.add(name.trim());
	}

}
