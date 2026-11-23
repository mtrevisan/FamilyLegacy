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
package io.github.mtrevisan.familylegacy.assistant.draft;

import io.github.mtrevisan.familylegacy.assistant.gap.GapDraft;
import io.github.mtrevisan.familylegacy.assistant.identity.IdentityDraft;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;


public class DraftEnrichmentTest{

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 23);


	@Test
	void dueDateDependsOnPriority(){
		Assertions.assertEquals("2026-12-07", DraftEnrichment.dueDate("high", TODAY));
		Assertions.assertEquals("2026-12-23", DraftEnrichment.dueDate("normal", TODAY));
		Assertions.assertEquals("2027-02-21", DraftEnrichment.dueDate("low", TODAY));
		Assertions.assertEquals("2026-12-23", DraftEnrichment.dueDate(null, TODAY));
	}

	@Test
	void taskGetsWhereToLookAndDueDate(){
		final GapDraft draft = new GapDraft(GapDraft.Kind.TASK, "", "Find a source.", List.of("I1"), "high");

		final GapDraft enriched = DraftEnrichment.enrich(draft, List.of("Archivio di Stato"), List.of("Registro [S1]"), TODAY);

		Assertions.assertTrue(enriched.text().startsWith("Find a source."));
		Assertions.assertTrue(enriched.text().contains("Archivio di Stato"));
		Assertions.assertTrue(enriched.text().contains("Registro [S1]"));
		Assertions.assertEquals("2026-12-07", enriched.dueDate());
		Assertions.assertNull(enriched.followUp());
		Assertions.assertEquals(List.of("I1"), enriched.records());
	}

	@Test
	void repositoryAlreadyNamedIsNotRepeated(){
		final GapDraft draft = new GapDraft(GapDraft.Kind.TASK, "", "Look at Archivio di Stato.", List.of("I1"), "normal");

		final GapDraft enriched = DraftEnrichment.enrich(draft, List.of("Archivio di Stato"), List.of(), TODAY);

		Assertions.assertEquals("Look at Archivio di Stato.", enriched.text());
	}

	@Test
	void dueDateAlreadyProposedIsKept(){
		final GapDraft draft = new GapDraft(GapDraft.Kind.TASK, "", "x", List.of(), "low", "2027-01-01", null);

		Assertions.assertEquals("2027-01-01", DraftEnrichment.enrich(draft, List.of(), List.of(), TODAY).dueDate());
	}

	@Test
	void questionGetsTheTaskThatWouldFollow(){
		final GapDraft draft = new GapDraft(GapDraft.Kind.QUESTION, "Birth of Mario", "When was Mario born?", List.of("I1"), null);

		final GapDraft enriched = DraftEnrichment.enrich(draft, List.of("Parrocchia"), List.of(), TODAY);

		Assertions.assertNotNull(enriched.followUp());
		Assertions.assertTrue(enriched.followUp().contains("Birth of Mario"));
		Assertions.assertTrue(enriched.followUp().contains("Parrocchia"));
		Assertions.assertNull(enriched.dueDate());
	}

	@Test
	void nothingKnownLeavesTheTextAlone(){
		final GapDraft draft = new GapDraft(GapDraft.Kind.TASK, "", "Do it.", List.of(), "normal");

		Assertions.assertEquals("Do it.", DraftEnrichment.enrich(draft, List.of(), List.of(), TODAY).text());
	}

	@Test
	void hypothesisGetsWhereToLookInTheRationale(){
		final IdentityDraft draft = new IdentityDraft("I1", "I2", "Alike.", "low", List.of("a"), List.of("b"), List.of("S9"));

		final IdentityDraft enriched = DraftEnrichment.enrich(draft, List.of("Archivio"), List.of("Registro [S1]"));

		Assertions.assertTrue(enriched.rationale().startsWith("Alike."));
		Assertions.assertTrue(enriched.rationale().contains("Archivio"));
		Assertions.assertTrue(enriched.rationale().contains("Registro [S1]"));
		Assertions.assertEquals(draft.sharedSourceIds(), enriched.sharedSourceIds());
		Assertions.assertEquals(draft.similarities(), enriched.similarities());
	}

	@Test
	void atMostThreeNamesAreListed(){
		final GapDraft draft = new GapDraft(GapDraft.Kind.TASK, "", "x", List.of(), "normal");

		final String text = DraftEnrichment.enrich(draft, List.of("A1", "B2", "C3", "D4"), List.of(), TODAY).text();

		Assertions.assertTrue(text.contains("C3"));
		Assertions.assertFalse(text.contains("D4"));
	}

}
