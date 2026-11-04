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
package io.github.mtrevisan.reports;

import java.util.List;
import java.util.Locale;
import java.util.Set;


/**
 * A report: its title, the sections, and the language it is written in.
 *
 * @param title	The title.
 * @param subtitle	The subtitle (can be {@code null}).
 * @param sections	The sections, in order.
 * @param language	The language of the text, as a BCP 47 tag ({@code "it"}, {@code "zh-CN"}); {@code "en"} if {@code null} or blank.
 * @param contentsTitle	The title of the table of contents, in that language; {@code "Contents"} if {@code null} or blank.
 */
public record ReportDocument(String title, String subtitle, List<ReportSection> sections, String language, String contentsTitle){

	public static final String DEFAULT_LANGUAGE = "en";
	private static final String DEFAULT_CONTENTS_TITLE = "Contents";
	/** The languages that are written from right to left. */
	private static final Set<String> RIGHT_TO_LEFT = Set.of("ar", "he", "fa", "ur");


	public ReportDocument{
		sections = List.copyOf(sections);
		language = (language == null || language.isBlank()? DEFAULT_LANGUAGE: language.strip());
		contentsTitle = (contentsTitle == null || contentsTitle.isBlank()? DEFAULT_CONTENTS_TITLE: contentsTitle.strip());
	}

	/** A report in English. */
	public ReportDocument(final String title, final String subtitle, final List<ReportSection> sections){
		this(title, subtitle, sections, DEFAULT_LANGUAGE, DEFAULT_CONTENTS_TITLE);
	}


	/** @return	Whether the language is written from right to left. */
	public boolean isRightToLeft(){
		return RIGHT_TO_LEFT.contains(Locale.forLanguageTag(language).getLanguage());
	}

}
