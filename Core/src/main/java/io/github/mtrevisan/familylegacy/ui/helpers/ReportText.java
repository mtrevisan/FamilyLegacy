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
package io.github.mtrevisan.familylegacy.ui.helpers;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * The pieces of the plain-text reports (File Properties, Diagnostics): the texts are the ones of the language of the user, and the
 * values are aligned in a column, whatever the length of the labels.
 */
public final class ReportText{

	private static final int LABEL_WIDTH = 18;
	private static final int TAG_WIDTH = 24;


	private ReportText(){}


	/** Adds the title of the report, as {@code === title ===}, and an empty line. */
	public static void heading(final StringBuilder sb, final String title){
		sb.append("=== ")
			.append(title)
			.append(" ===")
			.append(StringUtils.LF)
			.append(StringUtils.LF);
	}

	/** Adds the title of a section, as {@code --- title ---}. */
	public static void section(final StringBuilder sb, final String key){
		sb.append("--- ")
			.append(I18N.t(key))
			.append(" ---")
			.append(StringUtils.LF);
	}

	/** Adds a line: the label (the text of the key and a colon) and the value, aligned with the others. */
	public static void line(final StringBuilder sb, final String key, final Object value){
		String label = I18N.t(key) + ":";
		label = StringUtils.rightPad(label, LABEL_WIDTH);
		//a label as long as the column still has a space before the value
		if(!label.endsWith(StringUtils.SPACE))
			label += StringUtils.SPACE;
		sb.append(label)
			.append(value)
			.append(StringUtils.LF);
	}

	/** Adds an empty line. */
	public static void blank(final StringBuilder sb){
		sb.append(StringUtils.LF);
	}

	/** Adds the number of records of the model, and the number for each type of record. */
	public static void modelStatistics(final StringBuilder sb, final FLEFModel model){
		if(model == null){
			sb.append(I18N.t("text.report.no_model"))
				.append(StringUtils.LF);
			return;
		}

		try{
			final List<FLEFRecord> all = model.getRecords();
			line(sb, "text.report.total_records", all.size());
			blank(sb);
			sb.append(I18N.t("text.report.by_tag"))
				.append(':')
				.append(StringUtils.LF);

			final Map<String, Long> byTag = new TreeMap<>();
			for(final FLEFRecord record : all)
				if(record.getTag() != null)
					byTag.merge(record.getTag(), 1L, Long::sum);

			for(final Map.Entry<String, Long> entry : byTag.entrySet())
				sb.append("  ")
					.append(StringUtils.rightPad(entry.getKey(), TAG_WIDTH))
					.append(StringUtils.SPACE)
					.append(entry.getValue())
					.append(StringUtils.LF);
		}
		catch(final RuntimeException e){
			sb.append(I18N.t("text.report.stats_error"))
				.append(": ")
				.append(e.getMessage())
				.append(StringUtils.LF);
		}
	}

}
