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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class ReportMarkupTest{

	@Test
	void linkWithoutTargetFunctionIsOnlyItsText(){
		Assertions.assertEquals("see Mario", ReportMarkup.inline("see [Mario](@I1@)", null));
		Assertions.assertEquals("see Mario", ReportMarkup.inline("see [Mario](@I1@)", id -> null));
	}

	@Test
	void linkTargetIsEscaped(){
		Assertions.assertEquals("<a href=\"p.html?a=1&amp;b=2&quot;#I1\">Mario</a>",
			ReportMarkup.inline("[Mario](@I1@)", id -> "p.html?a=1&b=2\"#" + id));
	}

	@Test
	void birthAsteriskIsNotItalic(){
		Assertions.assertEquals("*1850 Padova, *1852 Roma", ReportMarkup.inline("*1850 Padova, *1852 Roma", null));
		Assertions.assertEquals("(*1850-*1900)", ReportMarkup.inline("(*1850-*1900)", null));
		Assertions.assertEquals("*1850", ReportMarkup.inline("*1850", null));
	}

	@Test
	void spansFollowTheSameRulesAsTheMarkup(){
		Assertions.assertEquals(List.of(new ReportMarkup.Span("a ", false, false, false, null),
				new ReportMarkup.Span("b", true, false, false, null),
				new ReportMarkup.Span(" ", false, false, false, null),
				new ReportMarkup.Span("c", false, true, false, null),
				new ReportMarkup.Span(" ", false, false, false, null),
				new ReportMarkup.Span("d", false, false, true, null)),
			ReportMarkup.spans("a **b** *c* `d`"));
		//a lone asterisk is a literal
		Assertions.assertEquals(List.of(new ReportMarkup.Span("*1850 Padova, *1852 Roma", false, false, false, null)),
			ReportMarkup.spans("*1850 Padova, *1852 Roma"));
		//what is inside a code span is not Markdown
		Assertions.assertEquals(List.of(new ReportMarkup.Span("**x**", false, false, true, null)), ReportMarkup.spans("`**x**`"));
		Assertions.assertTrue(ReportMarkup.spans(null).isEmpty());
	}

	@Test
	void spansCarryTheLinkOfARecord(){
		Assertions.assertEquals(List.of(new ReportMarkup.Span("see ", false, false, false, null),
				new ReportMarkup.Span("Mario", false, false, false, "I1")),
			ReportMarkup.spans("see [Mario](@I1@)"));
	}

	@Test
	void italicAndBold(){
		Assertions.assertEquals("a <i>b</i> c", ReportMarkup.inline("a *b* c", null));
		Assertions.assertEquals("a <b>b</b> c", ReportMarkup.inline("a **b** c", null));
		Assertions.assertEquals("<i>x</i>.", ReportMarkup.inline("*x*.", null));
		Assertions.assertEquals("2*3*4", ReportMarkup.inline("2*3*4", null));
	}

	@Test
	void codeSpanIsNotMarkdown(){
		Assertions.assertEquals("<code>a*b*c</code> and <i>d</i>", ReportMarkup.inline("`a*b*c` and *d*", null));
		Assertions.assertEquals("<code>&lt;x&gt;</code>", ReportMarkup.inline("`<x>`", null));
	}

	@Test
	void plainKeepsLiteralAsterisks(){
		Assertions.assertEquals("*1850 Mario", ReportMarkup.plain("*1850 Mario"));
		Assertions.assertEquals("Mario Rossi code", ReportMarkup.plain("**Mario** *Rossi* `code`"));
		Assertions.assertEquals("Mario", ReportMarkup.plain("[Mario](@I1@)"));
		Assertions.assertEquals("", ReportMarkup.plain(null));
	}

	@Test
	void recordIds(){
		Assertions.assertEquals(List.of("I1", "F2"), ReportMarkup.recordIds("A [I1] and [F2] and [I1]"));
		Assertions.assertEquals("Mario Rossi", ReportMarkup.withoutRecordIds("Mario Rossi [I12]"));
		Assertions.assertEquals("Mario and Anna", ReportMarkup.withoutRecordIds("Mario [I1] and Anna [F2]"));
	}

	@Test
	void escapeDropsInvalidXmlCharacters(){
		Assertions.assertEquals("a&amp;b&lt;&gt;&quot;", ReportMarkup.escape("a&b<>\"\u0001"));
		Assertions.assertEquals("", ReportMarkup.escape(null));
	}

}
