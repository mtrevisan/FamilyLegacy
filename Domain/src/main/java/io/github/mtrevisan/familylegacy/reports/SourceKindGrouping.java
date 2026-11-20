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

import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLabels;
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


/**
 * Groups the sources of a report by their kind ({@code SourceRecord.kind}: census, church register, military record...): the kinds of the
 * vocabulary come first, in the order of the vocabulary, then the kinds written as free text (alphabetically), then the sources that have
 * no kind.
 */
final class SourceKindGrouping{

	private static final List<String> VOCABULARY = Arrays.asList(SourceReader.KINDS);


	private SourceKindGrouping(){}


	/** @return	The kind of the source, trimmed; empty if it has none. */
	static String kindOf(final FLEFRecord source){
		return StringUtils.trimToEmpty(SourceReader.extractKind(source));
	}

	/** @return	The order of the kinds: those of the vocabulary, then the free ones, then none. */
	static Comparator<String> kindOrder(){
		return Comparator.comparingInt(SourceKindGrouping::rank)
			.thenComparing(String.CASE_INSENSITIVE_ORDER);
	}

	private static int rank(final String kind){
		if(kind.isEmpty())
			return Integer.MAX_VALUE;
		final int index = VOCABULARY.indexOf(kind);
		return (index >= 0? index: VOCABULARY.size());
	}

	/** @return	The heading of a group, in the language of the report. */
	static String heading(final ReportLabels labels, final String kind){
		return (kind.isEmpty()
			? labels.sections().sourceKindUnspecified()
			: ReportFormatters.escape(labels.enumValue("enum.source.kind", kind)));
	}

}
