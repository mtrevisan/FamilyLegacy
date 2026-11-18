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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A part of a report that is a page of its own in the renderers that split it (a book, a website): the sections from a heading of the
 * first level to the next one. The first chapter is the title page, with what comes before the first heading.
 *
 * @param title	The title of the chapter, without Markdown and record references.
 * @param titlePage	Whether it is the title page, that carries the title of the document.
 * @param sections	The sections of the chapter, starting with its heading (except for the title page).
 */
public record ReportChapter(String title, boolean titlePage, List<ReportSection> sections){

	/** The headings up to this level start a chapter. */
	public static final int CHAPTER_LEVEL = 1;


	public ReportChapter{
		sections = List.copyOf(sections);
	}


	/**
	 * Splits a document at its headings of the first level.
	 *
	 * @param doc	The document.
	 * @return	The chapters, the first one is the title page; always at least one.
	 */
	public static List<ReportChapter> split(final ReportDocument doc){
		final List<ReportChapter> chapters = new ArrayList<>();
		String title = ReportMarkup.plain(doc.title());
		boolean titlePage = true;
		List<ReportSection> sections = new ArrayList<>();
		for(final ReportSection section : doc.sections()){
			if(section instanceof ReportSection.Heading(int level, String text) && level <= CHAPTER_LEVEL){
				chapters.add(new ReportChapter(title, titlePage, sections));
				title = ReportMarkup.withoutRecordIds(ReportMarkup.plain(text));
				titlePage = false;
				sections = new ArrayList<>();
			}
			sections.add(section);
		}
		chapters.add(new ReportChapter(title, titlePage, sections));
		return chapters;
	}

	/**
	 * @param chapters	The chapters of a document.
	 * @return	For each record reference found in a heading, the index of the first chapter that has it.
	 */
	public static Map<String, Integer> anchors(final List<ReportChapter> chapters){
		final Map<String, Integer> anchors = new HashMap<>();
		for(int i = 0; i < chapters.size(); i ++)
			for(final ReportSection section : chapters.get(i).sections())
				if(section instanceof ReportSection.Heading heading)
					for(final String id : ReportMarkup.recordIds(heading.text()))
						anchors.putIfAbsent(id, i);
		return anchors;
	}

}
