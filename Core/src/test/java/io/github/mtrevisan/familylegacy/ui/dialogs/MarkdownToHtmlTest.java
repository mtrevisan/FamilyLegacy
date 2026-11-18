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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.ui.dialogs.help.MarkdownToHtml;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class MarkdownToHtmlTest{

	@Test
	void headingsGetAnAnchorNamedAsTheirSlug(){
		final String html = MarkdownToHtml.convert("# Getting started\n\n## Changing a date of birth");

		Assertions.assertTrue(html.contains("<h1><a name=\"getting-started\"></a>Getting started</h1>"), html);
		Assertions.assertTrue(html.contains("<h2><a name=\"changing-a-date-of-birth\"></a>Changing a date of birth</h2>"), html);
	}

	@Test
	void slugKeepsLettersAndDigitsAndTurnsSpacesIntoHyphens(){
		Assertions.assertEquals("backup-and-restore", MarkdownToHtml.slug("Backup and restore"));
		Assertions.assertEquals("order-of-children-and-partners", MarkdownToHtml.slug("Order of children and partners"));
		Assertions.assertEquals("file-properties", MarkdownToHtml.slug("`File` *Properties*..."));
	}

	@Test
	void titleIsTheFirstLevelOneHeading(){
		Assertions.assertEquals("Files", MarkdownToHtml.title("Intro\n\n# Files\n\n## Other\n\n# Second"));
		Assertions.assertNull(MarkdownToHtml.title("## Only a subheading"));
	}

	@Test
	void anchorsListEveryHeadingInOrder(){
		Assertions.assertEquals(List.of("files", "opening", "saving"), MarkdownToHtml.anchors("# Files\n## Opening\n### Saving"));
	}

	@Test
	void consecutiveLinesFormOneParagraphAndABlankLineEndsIt(){
		final String html = MarkdownToHtml.convert("first line\nsecond line\n\nanother paragraph");

		Assertions.assertEquals("<p>first line second line</p>\n<p>another paragraph</p>\n", html);
	}

	@Test
	void inlineElementsAreConverted(){
		final String html = MarkdownToHtml.convert("A **bold**, an *italic* and `code` with [a link](files.md#opening).");

		Assertions.assertEquals("<p>A <b>bold</b>, an <i>italic</i> and <code>code</code> with <a href=\"files.md#opening\">a link</a>.</p>\n",
			html);
	}

	@Test
	void htmlInTheTextIsEscaped(){
		final String html = MarkdownToHtml.convert("Use <b> & `<i>` here");

		Assertions.assertEquals("<p>Use &lt;b&gt; &amp; <code>&lt;i&gt;</code> here</p>\n", html);
	}

	@Test
	void aBackslashEscapesTheMarkOfAnInlineElement(){
		//the spaces around the marks make them plain text, the backslash too
		Assertions.assertEquals("<p>2 * 3 * 4 and *not italic*</p>\n", MarkdownToHtml.convert("2 * 3 * 4 and \\*not italic\\*"));
	}

	@Test
	void bulletsAndNumberedItemsMakeLists(){
		Assertions.assertEquals("<ul>\n<li>one</li>\n<li>two</li></ul>\n", MarkdownToHtml.convert("- one\n- two"));
		Assertions.assertEquals("<ol>\n<li>first</li>\n<li>second</li></ol>\n", MarkdownToHtml.convert("1. first\n2. second"));
	}

	@Test
	void indentedItemsMakeANestedList(){
		final String html = MarkdownToHtml.convert("- parent\n  - child\n- next");

		Assertions.assertEquals("<ul>\n<li>parent<ul>\n<li>child</li></ul>\n</li>\n<li>next</li></ul>\n", html);
	}

	@Test
	void fencedCodeIsKeptAsItIs(){
		final String html = MarkdownToHtml.convert("```\n# not a heading <x>\n- not a list\n```");

		Assertions.assertEquals("<pre># not a heading &lt;x&gt;\n- not a list\n</pre>\n", html);
	}

	@Test
	void blockQuoteContainsBlocks(){
		final String html = MarkdownToHtml.convert("> a **note**\n> on two lines");

		Assertions.assertEquals("<blockquote><p>a <b>note</b> on two lines</p>\n</blockquote>\n", html);
	}

	@Test
	void tableHasAHeaderRowAndBodyRows(){
		final String html = MarkdownToHtml.convert("| Key | Action |\n|---|---|\n| `F2` | Edit |\n| `Del` | Delete |");

		Assertions.assertTrue(html.contains("<tr><th>Key</th><th>Action</th></tr>"), html);
		Assertions.assertTrue(html.contains("<tr><td><code>F2</code></td><td>Edit</td></tr>"), html);
		Assertions.assertTrue(html.contains("<tr><td><code>Del</code></td><td>Delete</td></tr>"), html);
	}

	@Test
	void horizontalRuleIsNotAListItem(){
		Assertions.assertEquals("<p>text</p>\n<hr>\n", MarkdownToHtml.convert("text\n\n---"));
	}

	@Test
	void linksListsTheTargetsButNotTheOnesInCode(){
		final String markdown = "See [files](files.md) and [web](https://example.org).\n```\n[not](a.md)\n```\n";

		Assertions.assertEquals(List.of("files.md", "https://example.org"), MarkdownToHtml.links(markdown));
	}

	@Test
	void linksIgnoresTheExamplesWrittenAsInlineCode(){
		final String markdown = "Write `[text](flef:confidential)` to mark it, and see [privacy](privacy.md).";

		Assertions.assertEquals(List.of("privacy.md"), MarkdownToHtml.links(markdown));
	}

	@Test
	void documentWrapsTheBodyWithItsStyle(){
		final String html = MarkdownToHtml.document("# T");

		Assertions.assertTrue(html.startsWith("<html><head><style>"), html);
		Assertions.assertTrue(html.contains("</style></head><body><h1>"), html);
		Assertions.assertTrue(html.endsWith("</body></html>"), html);
	}

}
