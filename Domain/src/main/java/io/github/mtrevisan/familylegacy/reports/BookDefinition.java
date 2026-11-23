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

import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import org.apache.commons.lang3.StringUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * A book: an ordered list of reports ({@link BookEntry}), with the pages that come before them and the indexes that come after.
 *
 * @param title	The title; blank for the one of the language.
 * @param subtitle	The subtitle; blank for none.
 * @param author	Lines of text of the title page (who prepared the book, for whom...); blank for none.
 * @param language	The language of the book: the one of every report in it, of the headings, of the indexes.
 * @param coverPage	Whether the title is alone on the first page, and the table of contents starts on the next one (the formats that have pages).
 * @param titlePage	Whether there is a page with the title, the subtitle and the author before the preface.
 * @param prefaceTitle	The heading of the preface; blank for the one of the language.
 * @param prefaceText	The text of the preface (paragraphs are separated by a blank line); blank for no preface.
 * @param indexes	The indexes at the end of the book; each one gathers what the reports list, and replaces the one of every report.
 * @param entries	The reports, in order.
 */
public record BookDefinition(String title, String subtitle, String author, ReportLanguage language, boolean coverPage, boolean titlePage,
		String prefaceTitle, String prefaceText, Set<BookIndex> indexes, List<BookEntry> entries){

	public BookDefinition{
		Objects.requireNonNull(language, "language");
		title = StringUtils.defaultString(title).strip();
		subtitle = StringUtils.defaultString(subtitle).strip();
		author = StringUtils.defaultString(author).strip();
		prefaceTitle = StringUtils.defaultString(prefaceTitle).strip();
		prefaceText = StringUtils.defaultString(prefaceText).strip();
		indexes = (indexes.isEmpty()? Set.of(): Set.copyOf(EnumSet.copyOf(indexes)));
		entries = List.copyOf(entries);
	}

}
