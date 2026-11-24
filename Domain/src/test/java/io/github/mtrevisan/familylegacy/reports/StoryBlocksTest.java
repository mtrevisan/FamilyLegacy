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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The blocks of the text of a story: paragraphs, and for Markdown the headings, the lists and the rules. */
class StoryBlocksTest{

	@Test
	void theLinesOfAParagraphAreJoinedAndABlankLineStartsAnother(){
		final List<ReportSection> blocks = StoryBlocks.parse("First line\nsecond line.\n\n\nNext one.\r\nAnd more.", true, 2, true);

		Assertions.assertEquals(List.of(new ReportSection.Paragraph("First line second line."),
			new ReportSection.Paragraph("Next one. And more.")), blocks);
	}

	@Test
	void aPlainStoryHasNothingButParagraphs(){
		final List<ReportSection> blocks = StoryBlocks.parse("# Not a heading\n- not a list\n\nSecond.", false, 2, true);

		Assertions.assertEquals(List.of(new ReportSection.Paragraph("# Not a heading - not a list"), new ReportSection.Paragraph("Second.")),
			blocks);
	}

	@Test
	void theHeadingsOfTheTextAreUnderTheTitleOfTheStory(){
		Assertions.assertEquals(List.of(new ReportSection.Heading(3, "One"), new ReportSection.Heading(4, "Two")),
			StoryBlocks.parse("# One\n## Two ##", true, 2, true));
		//(without a title the first level takes its place)
		Assertions.assertEquals(List.of(new ReportSection.Heading(2, "One"), new ReportSection.Heading(3, "Two")),
			StoryBlocks.parse("# One\n## Two", true, 2, false));
		//(and never deeper than the sixth)
		Assertions.assertEquals(List.of(new ReportSection.Heading(6, "Deep")), StoryBlocks.parse("###### Deep", true, 5, true));
	}

	@Test
	void bulletedAndNumberedItemsAreAListWithTheirContinuations(){
		final List<ReportSection> blocks = StoryBlocks.parse("Before:\n- a\n* b\n  continued\n+ c\n1. d\n2) e\nAfter.", true, 2, true);

		Assertions.assertEquals(List.of(new ReportSection.Paragraph("Before:"),
			new ReportSection.BulletList(List.of("a", "b continued", "c", "d", "e")),
			new ReportSection.Paragraph("After.")), blocks);
	}

	@Test
	void aRuleIsARuleAndAnAsteriskOfEmphasisIsNotAnItem(){
		final List<ReportSection> blocks = StoryBlocks.parse("*Emphasis* at the start.\n\n***\n\n1850 was the year.", true, 2, true);

		Assertions.assertEquals(List.of(new ReportSection.Paragraph("*Emphasis* at the start."), ReportSection.HorizontalRule.INSTANCE,
			new ReportSection.Paragraph("1850 was the year.")), blocks);
	}

	@Test
	void anEmptyTextHasNoBlocks(){
		Assertions.assertEquals(List.of(), StoryBlocks.parse(null, true, 2, true));
		Assertions.assertEquals(List.of(), StoryBlocks.parse("  \n\n ", true, 2, true));
	}

}
