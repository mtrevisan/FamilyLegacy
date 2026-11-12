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
package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;


/**
 * The findings of the Research Assistant as a table (CSV), to look at them outside the application: to filter them, sort them, share
 * them or compare them with the report of Validate File. Nothing is changed in the research.
 * <p>
 * The columns are the severity, the category, the kind (the same in every language, as in the keys of the findings), the title, the
 * explanation, the identifiers of the records (separated by spaces) and whether the finding is one the user ignored.
 * </p>
 */
final class FindingsExport{

	/** A finding, and whether the user ignored it. */
	record Entry(Diagnostic diagnostic, boolean ignored){}


	private static final char SEPARATOR = ';';
	/** The byte order mark, so that a spreadsheet reads the accents as UTF-8. */
	private static final String BYTE_ORDER_MARK = "﻿";


	private FindingsExport(){}


	/**
	 * @param entries the findings, in the order they are listed
	 * @return the CSV, with a header line and a line for each finding (lines end with CRLF, as in RFC 4180)
	 */
	static String toCsv(final List<Entry> entries){
		final StringBuilder csv = new StringBuilder();
		csv.append(line(I18N.t("assistant.export.column.severity"), I18N.t("assistant.export.column.category"),
			I18N.t("assistant.export.column.kind"), I18N.t("assistant.export.column.title"), I18N.t("assistant.export.column.explanation"),
			I18N.t("assistant.export.column.records"), I18N.t("assistant.export.column.ignored")));
		for(final Entry entry : entries){
			final Diagnostic diagnostic = entry.diagnostic();
			csv.append(line(diagnostic.severity().name().toLowerCase(Locale.ROOT), diagnostic.category().title(), diagnostic.kind(),
				diagnostic.title(), diagnostic.explanation(), String.join(StringUtils.SPACE, diagnostic.recordIds()),
				(entry.ignored()? I18N.t("assistant.export.yes"): "")));
		}
		return csv.toString();
	}

	/** Writes the CSV, in UTF-8. */
	static void write(final Path file, final List<Entry> entries) throws IOException{
		Files.writeString(file, BYTE_ORDER_MARK + toCsv(entries), StandardCharsets.UTF_8);
	}


	private static String line(final String... fields){
		final StringBuilder line = new StringBuilder();
		for(int i = 0; i < fields.length; i ++){
			if(i > 0)
				line.append(SEPARATOR);
			line.append(field(fields[i]));
		}
		return line.append("\r\n").toString();
	}

	/** A field is quoted if it has a separator, a quote or a line break; the quotes in it are doubled. */
	private static String field(final String value){
		if(value == null)
			return "";

		final boolean quote = (value.indexOf(SEPARATOR) >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0);
		return (quote? "\"" + value.replace("\"", "\"\"") + "\"": value);
	}

}
