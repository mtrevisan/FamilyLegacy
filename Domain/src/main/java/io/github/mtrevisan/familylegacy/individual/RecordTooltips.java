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
package io.github.mtrevisan.familylegacy.individual;

import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * The lines of the tooltips of the boxes that tell how well the sources support an event (the {@code evidence} qualifiers of its
 * citations).
 * <p>
 * The lines are text to join to a tooltip, with {@code <br>} between them (see {@link #append(String, String...)}); they are {@code null}
 * when the record has nothing to say, so that a record without evidence has the tooltip it always had.
 */
public final class RecordTooltips{

	private static final String TAG_BR = "<br>";
	private static final String TAG_HTML_OPEN = "<html>";
	private static final String TAG_HTML_CLOSE = "</html>";
	private static final String TAG_SOURCE = "source";

	private RecordTooltips(){}


	/**
	 * @param event	The record of an event, or of any assertion with citations (can be {@code null}).
	 * @return	The evidence qualifiers of its citations, as "primary, original, direct" (the citations that differ are separated
	 * 	by a semicolon; a negative evidence is said so, as the sources attest the absence); {@code null} if it has none.
	 */
	public static String evidence(final FLEFRecord event){
		if(event == null)
			return null;

		final List<FLEFRecord> structures = new ArrayList<>(FLEFRecordHelper.findChildren(event, EvidenceQualifiersReader.TAG_EVIDENCE));
		structures.addAll(FLEFRecordHelper.findChildren(event,
			FLEFRecordHelper.composePath(TAG_SOURCE, EvidenceQualifiersReader.TAG_EVIDENCE)));

		final Set<String> assessments = new LinkedHashSet<>();
		for(final FLEFRecord evidence : structures){
			final List<String> parts = new ArrayList<>(3);
			add(parts, EvidenceQualifiersReader.extractInformationType(evidence));
			add(parts, EvidenceQualifiersReader.extractSourceType(evidence));
			add(parts, EvidenceQualifiersReader.extractEvidenceType(evidence));
			if(!parts.isEmpty())
				assessments.add(String.join(", ", parts));
		}
		return (assessments.isEmpty()? null: String.join("; ", assessments));
	}

	/**
	 * Adds lines to a tooltip.
	 *
	 * @param tooltip	The tooltip, with or without {@code <html>} (can be {@code null} or empty).
	 * @param lines	The lines to add (the {@code null} or blank ones are skipped).
	 * @return	The tooltip, in HTML, with a line more for each one; the tooltip itself if there is none.
	 */
	public static String append(final String tooltip, final String... lines){
		return append(tooltip, false, lines);
	}

	/**
	 * Adds a paragraph to a tooltip: the lines, after an empty line that sets them apart from what is above.
	 *
	 * @param tooltip	The tooltip, with or without {@code <html>} (can be {@code null} or empty).
	 * @param lines	The lines to add (the {@code null} or blank ones are skipped).
	 * @return	The tooltip, in HTML, with the paragraph; the tooltip itself if there is no line.
	 */
	public static String appendParagraph(final String tooltip, final String... lines){
		return append(tooltip, true, lines);
	}

	private static String append(final String tooltip, final boolean gap, final String... lines){
		final StringBuilder sb = new StringBuilder();
		for(final String line : lines)
			if(StringUtils.isNotBlank(line))
				sb.append(TAG_BR).append(escape(line));
		if(sb.isEmpty())
			return tooltip;

		if(StringUtils.isBlank(tooltip))
			return TAG_HTML_OPEN + sb.substring(TAG_BR.length()) + TAG_HTML_CLOSE;

		final String body = (tooltip.endsWith(TAG_HTML_CLOSE)
			? tooltip.substring(0, tooltip.length() - TAG_HTML_CLOSE.length())
			: TAG_HTML_OPEN + tooltip);
		return body + (gap? TAG_BR: StringUtils.EMPTY) + sb + TAG_HTML_CLOSE;
	}


	private static void add(final List<String> parts, final String value){
		if(StringUtils.isNotBlank(value))
			parts.add(value);
	}

	private static String escape(final String text){
		return StringUtils.replaceEach(text, new String[]{"&", "<", ">"}, new String[]{"&amp;", "&lt;", "&gt;"});
	}

}
