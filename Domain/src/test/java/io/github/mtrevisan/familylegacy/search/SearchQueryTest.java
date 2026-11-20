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
package io.github.mtrevisan.familylegacy.search;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;


class SearchQueryTest{

	private static List<String> shown(final String query){
		return SearchQuery.parse(query).terms().stream()
			.map(t -> t.kind().name().charAt(0) + ":" + t.display())
			.toList();
	}

	private static SearchMatcher.Analysis analyse(final String query, final String text, final Set<SearchMode> modes){
		return SearchMatcher.analyse(SearchQuery.parse(query), List.of(new SearchMatcher.Segment(SearchField.DISPLAY_TEXT, text)), modes);
	}


	@Test
	void theQueryIsSplitInWordsPhrasesAndWildcards(){
		Assertions.assertEquals(List.of("W:gall", "P:\"bortolo gallinaro\""), shown("gall \"Bortolo Gallinaro\""));
		Assertions.assertEquals(SearchQuery.Kind.WILDCARD, SearchQuery.parse("gall*").terms().getFirst().kind());
		Assertions.assertTrue(SearchQuery.parse("   ").isEmpty());
		Assertions.assertTrue(SearchQuery.parse("\"\"").isEmpty());
		Assertions.assertTrue(SearchQuery.parse(null).isEmpty());
	}

	@Test
	void anUnclosedQuoteEndsWithTheText(){
		Assertions.assertEquals(List.of("P:\"bortolo gall\""), shown("\"Bortolo Gall"));
	}

	@Test
	void aBackslashMakesTheNextCharacterLiteral(){
		Assertions.assertEquals(List.of("P:\"say \"hi\"\""), shown("\"say \\\"hi\\\"\""));
		Assertions.assertEquals(SearchQuery.Kind.PHRASE, SearchQuery.parse("a\\*b").terms().getFirst().kind());
		Assertions.assertEquals("a*b", SearchQuery.parse("a\\*b").terms().getFirst().text());
		Assertions.assertEquals("a\\b", SearchQuery.parse("a\\\\b").terms().getFirst().text());
		Assertions.assertEquals("a\\b", SearchQuery.parse("a\\b").terms().getFirst().text());
	}

	@Test
	void aPhraseIsExact(){
		final Set<SearchMode> none = Set.of();
		Assertions.assertTrue(analyse("\"bortolo gall\"", "Bortolo Gallinaro [I1]", none).matchedTerms() == 1, "case-insensitive, a part of a word");
		Assertions.assertEquals(0, analyse("\"gallinaro bortolo\"", "Bortolo Gallinaro", none).matchedTerms(), "the order counts");
		Assertions.assertEquals(0, analyse("\"bortolo gallinaro\"", "Bortolo Zanon Gallinaro", none).matchedTerms(), "contiguous");
		Assertions.assertEquals(0, analyse("\"bortolp gallinaro\"", "Bortolo Gallinaro", Set.of(SearchMode.FUZZY)).matchedTerms(), "never fuzzy");
		Assertions.assertEquals(0, analyse("\"bortolo gall\"", "Bortolo Gallinaro", Set.of(SearchMode.WHOLE_WORD)).matchedTerms(), "whole word");
		Assertions.assertEquals(1, analyse("\"bortolo gallinaro\"", "Bortolo Gallinaro", Set.of(SearchMode.WHOLE_WORD)).matchedTerms());
	}

	@Test
	void aQuoteCanBeSearched(){
		Assertions.assertEquals(1, analyse("\\\"zuanne\\\"", "Giovanni \"Zuanne\" Rossi", Set.of()).matchedTerms());
		Assertions.assertEquals(0, analyse("\\\"zuanne\\\"", "Giovanni Zuanne Rossi", Set.of()).matchedTerms());
		Assertions.assertEquals(1, analyse("\"\\\"zuanne\\\"\"", "Giovanni \"Zuanne\" Rossi", Set.of()).matchedTerms());
	}

	@Test
	void wildcardsMatchWholeWords(){
		Assertions.assertTrue(SearchMatcher.globMatches("gall" + SearchQuery.ANY_RUN, "gallinaro"));
		Assertions.assertTrue(SearchMatcher.globMatches("" + SearchQuery.ANY_RUN + "naro", "gallinaro"));
		Assertions.assertTrue(SearchMatcher.globMatches("g" + SearchQuery.ANY_RUN + "l" + SearchQuery.ANY_RUN + "o", "gallinaro"));
		Assertions.assertTrue(SearchMatcher.globMatches("b" + SearchQuery.ANY_ONE + "rtolo", "bortolo"));
		Assertions.assertFalse(SearchMatcher.globMatches("b" + SearchQuery.ANY_ONE + "tolo", "bortolo"));
		Assertions.assertFalse(SearchMatcher.globMatches("gall", "gallinaro"));
		Assertions.assertTrue(SearchMatcher.globMatches("" + SearchQuery.ANY_RUN, "x"));

		Assertions.assertEquals(1, analyse("gall*", "Bortolo Gallinaro", Set.of()).matchedTerms());
		Assertions.assertEquals(0, analyse("*llin", "Bortolo Gallinaro", Set.of()).matchedTerms(), "a whole word, not a part");
		Assertions.assertEquals(1, analyse("b?rtolo", "Bortolo Gallinaro", Set.of()).matchedTerms());
		Assertions.assertEquals(0, analyse("gall*", "Bortolo Gali", Set.of(SearchMode.FUZZY)).matchedTerms(), "not fuzzy");
	}

	@Test
	void theWhereOfAMatchIsRemembered(){
		final SearchMatcher.Analysis analysis = SearchMatcher.analyse(SearchQuery.parse("gall zanon"),
			List.of(new SearchMatcher.Segment(SearchField.DISPLAY_TEXT, "Bortolo Gallinaro"),
				new SearchMatcher.Segment(SearchField.NOTES_AND_CITATIONS, "married a Zanon")), Set.of());
		Assertions.assertEquals(List.of(new SearchMatcher.Evidence(SearchField.DISPLAY_TEXT, "gallinaro")), analysis.evidence(0));
		Assertions.assertEquals(List.of(new SearchMatcher.Evidence(SearchField.NOTES_AND_CITATIONS, "zanon")), analysis.evidence(1));
	}

	@Test
	void theLegacyScoreIsUnchanged(){
		Assertions.assertTrue(SearchMatcher.score("", "x", Set.of()).passes());
		Assertions.assertFalse(SearchMatcher.score("zz", null, Set.of()).passes());
		Assertions.assertEquals(2, SearchMatcher.score("gall bor", "Bortolo Gallinaro", Set.of()).matchedTokens());
		Assertions.assertEquals(1., SearchMatcher.score("bortolo", "Bortolo", Set.of()).score(), 1e-9);
		Assertions.assertFalse(SearchMatcher.score("bortolp", "Bortolo", Set.of()).passes());
		Assertions.assertTrue(SearchMatcher.score("bortolp", "Bortolo", Set.of(SearchMode.FUZZY)).passes());
	}

	@Test
	void aRareWordWeighsMoreThanACommonOne(){
		final SearchQuery query = SearchQuery.parse("giovanni gallinaro");
		final List<String> texts = List.of("Giovanni Rossi", "Giovanni Bianchi", "Giovanni Verdi", "Giovanni Gallinaro", "Pietro Gallinaro",
			"Giovanni Neri", "Maria Neri", "Anna Neri");
		final List<SearchMatcher.Analysis> corpus = texts.stream()
			.map(text -> SearchMatcher.analyse(query, List.of(new SearchMatcher.Segment(SearchField.DISPLAY_TEXT, text)), Set.of()))
			.toList();
		final double[] scores = Bm25.score(corpus);

		Assertions.assertTrue(scores[3] > scores[4], "both words beat one");
		Assertions.assertTrue(scores[4] > scores[0], "the rare word (gallinaro) beats the common one (giovanni)");
		Assertions.assertEquals(0., scores[6], 1e-12, "nothing found");
	}

	@Test
	void aShortTextBeatsALongOneForTheSameMatch(){
		final SearchQuery query = SearchQuery.parse("gallinaro");
		final List<SearchMatcher.Analysis> corpus = List.of(
			SearchMatcher.analyse(query, List.of(new SearchMatcher.Segment(SearchField.DISPLAY_TEXT, "Gallinaro")), Set.of()),
			SearchMatcher.analyse(query, List.of(new SearchMatcher.Segment(SearchField.DISPLAY_TEXT,
				"Gallinaro and a very long text with many other words that say nothing")), Set.of()),
			SearchMatcher.analyse(query, List.of(new SearchMatcher.Segment(SearchField.DISPLAY_TEXT, "Rossi")), Set.of()));
		final double[] scores = Bm25.score(corpus);

		Assertions.assertTrue(scores[0] > scores[1]);
	}

	@Test
	void aTypoRanksBelowAnExactWord(){
		final SearchQuery query = SearchQuery.parse("bortolo");
		final Set<SearchMode> fuzzy = Set.of(SearchMode.FUZZY);
		final List<SearchMatcher.Analysis> corpus = List.of(
			SearchMatcher.analyse(query, List.of(new SearchMatcher.Segment(SearchField.DISPLAY_TEXT, "Bortolp")), fuzzy),
			SearchMatcher.analyse(query, List.of(new SearchMatcher.Segment(SearchField.DISPLAY_TEXT, "Bortolo")), fuzzy));
		final double[] scores = Bm25.score(corpus);

		Assertions.assertTrue(scores[1] > scores[0]);
	}
}
