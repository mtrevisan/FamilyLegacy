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
package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.search.SearchMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;


/** The search of the palette matches as the record selection dialog does: by words, with the whole word and fuzzy modes. */
class AssistantSearchModesTest{

	private static final Set<SearchMode> PLAIN = EnumSet.noneOf(SearchMode.class);

	private static final Diagnostic CONFLICT = finding("Conflicting birth dates", "Anna has two births");
	private static final Diagnostic ORDER = finding("Child born before the mother", "Bruno is older than Anna");


	private static Diagnostic finding(final String title, final String explanation){
		return new Diagnostic(Diagnostic.Severity.WARNING, AssistantCategory.DIAGNOSTICS, title, explanation, List.of("I1"), null, title);
	}


	@Test
	void aWordOfTheTextIsFound(){
		Assertions.assertEquals(List.of(CONFLICT), ResearchAssistantService.filter("conflicting", PLAIN, List.of(CONFLICT, ORDER)));
	}

	@Test
	void theWordsCanBeInTheTitleAndInTheExplanation(){
		Assertions.assertEquals(List.of(CONFLICT), ResearchAssistantService.filter("birth anna", PLAIN, List.of(CONFLICT)));
	}

	@Test
	void wholeWordRefusesAPartOfAWord(){
		Assertions.assertEquals(List.of(CONFLICT), ResearchAssistantService.filter("conflict", PLAIN, List.of(CONFLICT, ORDER)));
		Assertions.assertTrue(ResearchAssistantService.filter("conflict", EnumSet.of(SearchMode.WHOLE_WORD), List.of(CONFLICT, ORDER)).isEmpty());
	}

	@Test
	void fuzzyToleratesATypo(){
		Assertions.assertEquals(List.of(CONFLICT), ResearchAssistantService.filter("confilcting", EnumSet.of(SearchMode.FUZZY), List.of(CONFLICT, ORDER)));
	}

	@Test
	void anAbbreviationStillMatchesWhenNoModeIsChosen(){
		Assertions.assertEquals(List.of(ORDER), ResearchAssistantService.filter("cbbm", PLAIN, List.of(CONFLICT, ORDER)));
		Assertions.assertTrue(ResearchAssistantService.filter("cbbm", EnumSet.of(SearchMode.FUZZY), List.of(CONFLICT, ORDER)).isEmpty());
	}

	@Test
	void aBlankQueryMatchesEverything(){
		Assertions.assertEquals(2, ResearchAssistantService.filter("", PLAIN, List.of(CONFLICT, ORDER)).size());
	}

}
