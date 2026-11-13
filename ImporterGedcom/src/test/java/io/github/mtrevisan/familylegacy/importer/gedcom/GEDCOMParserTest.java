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
package io.github.mtrevisan.familylegacy.importer.gedcom;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


class GEDCOMParserTest{

	private static List<GEDCOMNode> parse(final String gedcom) throws IOException{
		return new GEDCOMParser().parse(new StringReader(gedcom));
	}

	@Test
	void valueEqualToAtFDoesNotFail() throws IOException{
		final List<GEDCOMNode> roots = parse("""
			0 HEAD
			0 @I1@ INDI
			1 NOTE @F
			1 NOTE @@F
			""");

		final List<GEDCOMNode> notes = roots.get(1).getChildren();
		Assertions.assertEquals("@F", notes.get(0).getValue());
		Assertions.assertEquals("@F", notes.get(1).getValue());
	}

	@Test
	void textStartingWithAtFIsNotRewritten() throws IOException{
		final List<GEDCOMNode> roots = parse("""
			0 HEAD
			0 @I1@ INDI
			1 NOTE @F123
			""");

		Assertions.assertEquals("@F123", roots.get(1).getChildren().get(0).getValue());
	}

	@Test
	void referencesAreLeftToTheConverter() throws IOException{
		final List<GEDCOMNode> roots = parse("""
			0 HEAD
			0 @FAM1@ FAM
			1 HUSB @I1@
			0 @I1@ INDI
			1 FAMS @FAM1@
			""");

		Assertions.assertEquals("FAM1", roots.get(1).getXrefId());
		Assertions.assertEquals("@FAM1@", roots.get(2).getChildren().get(0).getValue());
	}

	@Test
	void continuationsGoToTheirSuperiorNotToTheLastLine() throws IOException{
		final List<GEDCOMNode> roots = parse("""
			0 HEAD
			0 @S1@ SOUR
			1 TITL The
			2 CONC  book
			2 CONT second line
			1 NOTE first
			2 CONT second
			2 SOUR @S2@
			2 CONT third
			""");

		final GEDCOMNode source = roots.get(1);
		Assertions.assertEquals("The book\nsecond line", source.getChildren().get(0).getValue());
		Assertions.assertEquals("first\nsecond\nthird", source.getChildren().get(1).getValue());
	}

	@Test
	void linesWithLeadingWhitespaceAreRead() throws IOException{
		final List<GEDCOMNode> roots = parse("0 HEAD\n0 @I1@ INDI\n  \t1 NAME John /Doe/\n");

		Assertions.assertEquals(1, roots.get(1).getChildren().size());
		Assertions.assertEquals("John /Doe/", roots.get(1).getChildren().get(0).getValue());
	}

	@Test
	void orphanContinuationIsIgnored() throws IOException{
		final List<GEDCOMNode> roots = parse("1 CONT lost\n0 HEAD\n");

		Assertions.assertEquals(1, roots.size());
		Assertions.assertNull(roots.get(0).getValue());
	}

}
