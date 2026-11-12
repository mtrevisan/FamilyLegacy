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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;


/**
 * The findings of the palette are in groups, by severity and by kind, that can be collapsed: the kinds start collapsed. The palette is
 * made and used on the event dispatch thread, as in the application (its detail pane is laid out there).
 */
class PaletteCollapseTest{

	private static Diagnostic finding(final Diagnostic.Severity severity, final String kind, final String title){
		return new Diagnostic(severity, AssistantCategory.PLACES, title, "why", List.of(), null, kind + "|" + title);
	}

	private static ResearchAssistantPalette palette(final List<Diagnostic> findings, final Set<String> toggled, final String selectedIdentity){
		final ResearchAssistantService service = new ResearchAssistantService();
		final ResearchAssistantService.Recalculation recalculation = new ResearchAssistantService.Recalculation(findings, 0, 0);
		return new ResearchAssistantPalette(service, new AssistantContext(null, null, new FLEFModel()), recalculation,
			new ResearchAssistantPalette.State("", Set.of(), false, selectedIdentity, 0, toggled, false));
	}

	private static List<Diagnostic> findings(){
		return List.of(finding(Diagnostic.Severity.ERROR, "place.self_relation", "Self A"),
			finding(Diagnostic.Severity.WARNING, "place.possible_duplicate", "Duplicate A"),
			finding(Diagnostic.Severity.WARNING, "place.possible_duplicate", "Duplicate B"),
			finding(Diagnostic.Severity.WARNING, "place.inverted_hierarchy", "Inverted A"));
	}

	/** Runs the check on the event dispatch thread, and gives back what it found wrong. */
	private static void onEventDispatchThread(final Runnable check) throws InterruptedException, InvocationTargetException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");

		try{
			SwingUtilities.invokeAndWait(check);
		}
		catch(final InvocationTargetException ite){
			if(ite.getCause() instanceof AssertionError ae)
				throw ae;
			throw ite;
		}
	}


	@Test
	void theSeveritiesStartOpenAndTheKindsStartCollapsed() throws Exception{
		onEventDispatchThread(() -> {
			final List<String> rows = palette(findings(), Set.of(), null).rows();

			Assertions.assertEquals(List.of(
				"[s:ERROR open Errors (1)]", "[k:ERROR:place.self_relation closed Places related to themselves (1)]",
				"[s:WARNING open Warnings (3)]", "[k:WARNING:place.possible_duplicate closed Possible duplicate places (2)]",
				"[k:WARNING:place.inverted_hierarchy closed Places that are part of a smaller place (1)]"), rows);
		});
	}

	@Test
	void aKindOpensWhenItIsToggledAndTheFindingOfTheSelectionIsShown() throws Exception{
		onEventDispatchThread(() -> {
			final List<String> opened = palette(findings(), Set.of("k:WARNING:place.possible_duplicate"), null).rows();
			Assertions.assertTrue(opened.contains("Duplicate A") && opened.contains("Duplicate B"), opened.toString());
			Assertions.assertFalse(opened.contains("Inverted A"), opened.toString());

			//the palette that comes back after an action shows the finding the user acted on
			final Diagnostic acted = findings().get(3);
			final List<String> back = palette(findings(), Set.of(), ResearchAssistantService.identity(acted)).rows();
			Assertions.assertTrue(back.contains("Inverted A"), back.toString());
			Assertions.assertFalse(back.contains("Duplicate A"), back.toString());
		});
	}

	@Test
	void aHeadingTogglesWhenItIsChosen() throws Exception{
		onEventDispatchThread(() -> {
			final ResearchAssistantPalette palette = palette(findings(), Set.of(), null);

			//the heading of the kind of the duplicates is the fourth row
			palette.select(3);
			palette.toggleSelected();
			Assertions.assertTrue(palette.rows().contains("Duplicate A"), palette.rows().toString());

			palette.toggleSelected();
			Assertions.assertFalse(palette.rows().contains("Duplicate A"), palette.rows().toString());
		});
	}


	@Test
	void theDetailOfAFindingListsAllItsRecordsWithTheirIdentifiers() throws Exception{
		onEventDispatchThread(() -> {
			final Diagnostic finding = new Diagnostic(Diagnostic.Severity.WARNING, AssistantCategory.PLACES, "Title", "Explanation",
				List.of("P1", "RL12"), null, "place.x|P1,RL12|");
			final ResearchAssistantPalette palette = palette(List.of(finding), Set.of("k:WARNING:place.x"), null);
			final List<String> rows = palette.rows();

			palette.select(rows.indexOf("Title"));
			final String detail = palette.detail();

			Assertions.assertTrue(detail.contains("Records:"), detail);
			Assertions.assertTrue(detail.contains("P1") && detail.contains("RL12"), detail);
		});
	}


	@Test
	void theRecordsOfTheDetailThatCanBeOpenedAreLinks() throws Exception{
		onEventDispatchThread(() -> {
			final FLEFModel model = new FLEFModel();
			model.addRecord(io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord.createMainRecord("I1", "individual"));
			final Diagnostic finding = new Diagnostic(Diagnostic.Severity.WARNING, AssistantCategory.PLACES, "Title", "Explanation",
				List.of("I1", "X9"), null, "place.x|I1,X9|");
			final ResearchAssistantPalette palette = new ResearchAssistantPalette(new ResearchAssistantService(),
				new AssistantContext(null, null, model), new ResearchAssistantService.Recalculation(List.of(finding), 0, 0),
				new ResearchAssistantPalette.State("", Set.of(), false, null, 0, Set.of("k:WARNING:place.x"), false));

			palette.select(palette.rows().indexOf("Title"));
			final String detail = palette.detail();

			//a record with an editor is a link, a record that is not there is plain text; the identifier appears once
			Assertions.assertTrue(detail.contains("<a href=\"record:I1\">"), detail);
			Assertions.assertFalse(detail.contains("record:X9"), detail);
			Assertions.assertEquals(1, StringUtils.countMatches(detail, "[I1]"), detail);
		});
	}

}
