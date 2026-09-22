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
package io.github.mtrevisan.familylegacy.v2.ui.tools.places;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ReportDialog;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;
import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;


/**
 * Finds place names that look like duplicates of each other but are
 * written differently: differences in case, leading or trailing spaces,
 * accents, or punctuation. The tool does not modify the model; it
 * produces a report so the user can decide which spelling to keep and
 * edit the places accordingly.
 * <p>
 * Two names are considered "the same under normalization" when, after
 * lower-casing, accent stripping, punctuation removal, and whitespace
 * collapsing, they are equal. The report lists each such group with the
 * place ids so the user can open them directly.
 */
public final class NormalizePlaceNamesTool implements ToolOperation{


	@Override
	public String getName(){
		return "Normalize Place Names…";
	}

	@Override
	public void run(final ToolContext context){
		final Map<String, List<FLEFRecord>> groups = new TreeMap<>();
		for(final FLEFRecord place : PlaceHelper.listAllPlaces(context.model())){
			final String name = PlaceHelper.displayName(place);
			if(name == null || name.isBlank())
				continue;
			final String key = normalize(name);
			groups.computeIfAbsent(key, k -> new ArrayList<>()).add(place);
		}

		// Keep only groups with more than one distinct original spelling.
		final Map<String, List<FLEFRecord>> suspects = new LinkedHashMap<>();
		for(final Map.Entry<String, List<FLEFRecord>> entry : groups.entrySet()){
			if(entry.getValue().size() < 2)
				continue;
			final long distinctSpellings = entry.getValue().stream()
				.map(PlaceHelper::displayName)
				.distinct()
				.count();
			if(distinctSpellings > 1)
				suspects.put(entry.getKey(), entry.getValue());
		}

		ReportDialog.showHtml(context.owner(), "Normalize Place Names",
			buildReport(suspects));
	}


	/**
	 * Normalizes a name for comparison: lower-case, NFD decomposition
	 * with combining marks stripped, punctuation replaced by spaces,
	 * whitespace collapsed, leading and trailing spaces removed.
	 */
	private static String normalize(final String name){
		String s = name.toLowerCase(Locale.ROOT);
		s = Normalizer.normalize(s, Normalizer.Form.NFD);
		s = s.replaceAll("\\p{M}+", StringUtils.EMPTY);
		s = s.replaceAll("[\\p{Punct}]+", StringUtils.SPACE);
		s = s.replaceAll("\\s+", StringUtils.SPACE).trim();
		return s;
	}

	private static String buildReport(final Map<String, List<FLEFRecord>> suspects){
		final StringBuilder body = new StringBuilder();
		body.append("<h1>Normalize Place Names</h1>");

		if(suspects.isEmpty()){
			body.append("<p class='ok'><b>✓ No suspicious place name variants found.</b></p>");
			return ReportDialog.document(body.toString());
		}

		body.append("<p class='warn'><b>Groups with more than one spelling: ")
			.append(suspects.size()).append("</b></p>");
		body.append("<p class='hint'>Open the places in the record editor and align "
			+ "the spelling to a single preferred form.</p>");

		for(final Map.Entry<String, List<FLEFRecord>> entry : suspects.entrySet()){
			body.append("<h2>").append(ReportDialog.escape(entry.getKey())).append("</h2>");
			body.append("<table>");
			body.append("<tr><th>Place</th><th>Spelling</th><th>Type</th></tr>");
			for(final FLEFRecord place : entry.getValue()){
				body.append("<tr>");
				body.append("<td>").append(ReportDialog.escape(place.getId())).append("</td>");
				body.append("<td>").append(ReportDialog.escape(PlaceHelper.displayName(place))).append("</td>");
				final String type = PlaceHelper.placeType(place);
				body.append("<td>").append(ReportDialog.escape(type != null? type: StringUtils.EMPTY)).append("</td>");
				body.append("</tr>");
			}
			body.append("</table>");
		}

		return ReportDialog.document(body.toString());
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasAnyPlaces());
	}

}
