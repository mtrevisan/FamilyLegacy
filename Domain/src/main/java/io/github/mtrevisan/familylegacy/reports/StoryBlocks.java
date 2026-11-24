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

import io.github.mtrevisan.reports.ReportSection;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The text of a story as the blocks of a report. A story is prose, so a line break is not a break: the lines of a paragraph are joined (a
 * paragraph with a line break would be preformatted, without its inline Markdown), and a blank line starts another paragraph.
 * <p>
 * A story in Markdown has, besides the paragraphs, the headings ({@code # Title}), the lists ({@code - item}, {@code 1. item}) and the
 * horizontal rules ({@code ---}); the inline marks (bold, italic, code, links) are left to the renderers. A numbered list is written as a
 * list of bullets, since a report has no numbered list.
 */
final class StoryBlocks{

	private static final int MAX_HEADING_LEVEL = 6;
	private static final Pattern HEADING = Pattern.compile("(#{1,6})\\s+(.+?)(?:\\s+#+)?");
	private static final Pattern BULLET = Pattern.compile("[-*+]\\s+(.+)");
	private static final Pattern ORDERED = Pattern.compile("\\d{1,9}[.)]\\s+(.+)");
	private static final Pattern RULE = Pattern.compile("(?:-{3,}|\\*{3,}|_{3,})");


	private StoryBlocks(){}


	/**
	 * @param text	The text of the story.
	 * @param markdown	Whether the text is in Markdown (it has the headings, the lists and the rules) or is plain (only paragraphs).
	 * @param titleLevel	The level of the heading of the story.
	 * @param titled	Whether the story has a heading: then the headings of its text are under it, otherwise they take its place.
	 * @return	The blocks of the story's text, in order (empty if the text is).
	 */
	static List<ReportSection> parse(final String text, final boolean markdown, final int titleLevel, final boolean titled){
		final List<ReportSection> blocks = new ArrayList<>();
		final List<String> paragraph = new ArrayList<>();
		final List<String> items = new ArrayList<>();
		final int headingBase = (titled? titleLevel: titleLevel - 1);

		for(final String line : StringUtils.splitPreserveAllTokens(StringUtils.replace(StringUtils.defaultString(text), "\r\n", "\n"), '\n')){
			final String stripped = line.strip();
			if(stripped.isEmpty()){
				flush(blocks, paragraph, items);
				continue;
			}
			if(!markdown){
				paragraph.add(stripped);
				continue;
			}

			if(RULE.matcher(stripped).matches()){
				flush(blocks, paragraph, items);
				blocks.add(ReportSection.HorizontalRule.INSTANCE);
				continue;
			}
			final Matcher heading = HEADING.matcher(stripped);
			if(heading.matches()){
				flush(blocks, paragraph, items);
				blocks.add(new ReportSection.Heading(Math.min(MAX_HEADING_LEVEL, headingBase + heading.group(1).length()), heading.group(2)));
				continue;
			}
			final Matcher item = itemOf(stripped);
			if(item != null){
				flushParagraph(blocks, paragraph);
				items.add(item.group(1));
				continue;
			}
			if(!items.isEmpty() && Character.isWhitespace(line.charAt(0))){
				//the continuation of an item
				items.set(items.size() - 1, items.getLast() + StringUtils.SPACE + stripped);
				continue;
			}

			flushItems(blocks, items);
			paragraph.add(stripped);
		}
		flush(blocks, paragraph, items);
		return blocks;
	}

	private static Matcher itemOf(final String line){
		final Matcher bullet = BULLET.matcher(line);
		if(bullet.matches())
			return bullet;
		final Matcher ordered = ORDERED.matcher(line);
		return (ordered.matches()? ordered: null);
	}

	private static void flush(final List<ReportSection> blocks, final List<String> paragraph, final List<String> items){
		flushParagraph(blocks, paragraph);
		flushItems(blocks, items);
	}

	private static void flushParagraph(final List<ReportSection> blocks, final List<String> paragraph){
		if(paragraph.isEmpty())
			return;

		blocks.add(new ReportSection.Paragraph(String.join(StringUtils.SPACE, paragraph)));
		paragraph.clear();
	}

	private static void flushItems(final List<ReportSection> blocks, final List<String> items){
		if(items.isEmpty())
			return;

		blocks.add(new ReportSection.BulletList(List.copyOf(items)));
		items.clear();
	}

}
