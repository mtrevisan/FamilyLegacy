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
package io.github.mtrevisan.familylegacy.assistant;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;


/**
 * The findings of the Research Assistant as a table (CSV) or as a page (HTML), to look at them outside the application: to filter them,
 * sort them, share them or compare them with the report of Validate File. Nothing is changed in the research.
 * <p>
 * The columns are the severity, the category, the kind (the same in every language, as in the keys of the findings), the title, the
 * explanation, the identifiers of the records (separated by spaces) and whether the finding is one the user ignored.
 * </p>
 */
public final class FindingsExport{

	/** A finding, and whether the user ignored it. */
	public record Entry(Diagnostic diagnostic, boolean ignored){}


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

	/** @return whether a file is written as a page (HTML) or as a table (CSV, otherwise), by its name */
	static boolean isHtml(final Path file){
		final String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
		return (name.endsWith(".html") || name.endsWith(".htm"));
	}

	/** Writes the findings as CSV or, if the name of the file says so, as a page. */
	public static void writeAs(final Path file, final List<Entry> entries) throws IOException{
		if(isHtml(file))
			Files.writeString(file, toHtml(entries), StandardCharsets.UTF_8);
		else
			write(file, entries);
	}

	/**
	 * @param entries the findings, in the order they are listed
	 * @return a self-contained page (no script, no external file) with the findings in a table, to read them or to send them to somebody:
	 * the colours follow the theme of the reader; the ignored findings are marked
	 */
	static String toHtml(final List<Entry> entries){
		final StringBuilder html = new StringBuilder(4_096);
		html.append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n<title>").append(escape(I18N.t("assistant.export.html.title")))
			.append("</title>\n<style>\n")
			.append(":root{--fg:#1b1b1b;--bg:#fff;--line:#d0d0d0;--error:#c62828;--warning:#b26a00;--information:#2e6da4;--muted:#666}\n")
			.append("@media(prefers-color-scheme:dark){:root{--fg:#e8e8e8;--bg:#1e1e1e;--line:#444;--error:#ff6b6b;--warning:#ffb74d;")
			.append("--information:#7fb3e6;--muted:#aaa}}\n")
			.append("body{font:14px/1.45 system-ui,sans-serif;color:var(--fg);background:var(--bg);margin:16px}\n")
			.append("table{border-collapse:collapse;width:100%}\n")
			.append("th,td{border:1px solid var(--line);padding:4px 8px;text-align:left;vertical-align:top}\n")
			.append("td.explanation{white-space:pre-wrap}\n")
			.append(".error{color:var(--error);font-weight:600}.warning{color:var(--warning);font-weight:600}")
			.append(".information{color:var(--information)}.ignored td{color:var(--muted)}\n")
			.append("</style>\n</head>\n<body>\n<h1>").append(escape(I18N.t("assistant.export.html.title"))).append("</h1>\n<p>")
			.append(escape(I18N.tf("assistant.export.html.summary", entries.size()))).append("</p>\n<table>\n<tr>");
		for(final String column : List.of("severity", "category", "kind", "title", "explanation", "records", "ignored"))
			html.append("<th>").append(escape(I18N.t("assistant.export.column." + column))).append("</th>");
		html.append("</tr>\n");
		for(final Entry entry : entries){
			final Diagnostic diagnostic = entry.diagnostic();
			final String severity = diagnostic.severity().name().toLowerCase(Locale.ROOT);
			html.append(entry.ignored()? "<tr class=\"ignored\">": "<tr>")
				.append("<td class=\"").append(severity).append("\">").append(escape(severity)).append("</td>")
				.append("<td>").append(escape(diagnostic.category().title())).append("</td>")
				.append("<td>").append(escape(diagnostic.kind())).append("</td>")
				.append("<td>").append(escape(diagnostic.title())).append("</td>")
				.append("<td class=\"explanation\">").append(escape(diagnostic.explanation())).append("</td>")
				.append("<td>").append(escape(String.join(StringUtils.SPACE, diagnostic.recordIds()))).append("</td>")
				.append("<td>").append(entry.ignored()? escape(I18N.t("assistant.export.yes")): "").append("</td></tr>\n");
		}
		return html.append("</table>\n</body>\n</html>\n").toString();
	}

	private static String escape(final String text){
		if(text == null)
			return "";
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
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
