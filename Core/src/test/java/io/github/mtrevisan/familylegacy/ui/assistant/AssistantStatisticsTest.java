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

import io.github.mtrevisan.familylegacy.ui.assistant.AssistantStatistics.Event;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


/** The counts of what the user does with the findings: off by default, only kinds and numbers, cumulative over the runs. */
class AssistantStatisticsTest{

	/** A file that is kept in memory, and that survives the statistics (as a file survives a run). */
	private static final class Memory implements AssistantStatistics.Store{

		private String text;
		private int writes;

		@Override
		public String read(){
			return text;
		}

		@Override
		public void write(final String content){
			text = content;
			writes ++;
		}

	}

	private static AssistantStatistics statistics(final Memory file, final AtomicBoolean enabled,
			final AtomicReference<AssistantTuning> tuning){
		return new AssistantStatistics(file, enabled::get, tuning::get, false);
	}

	private static AssistantStatistics statistics(final Memory file){
		return statistics(file, new AtomicBoolean(true), new AtomicReference<>(AssistantTuning.DEFAULTS));
	}


	@Test
	void nothingIsCountedUntilTheUserTurnsItOn(){
		final Memory file = new Memory();
		final AtomicBoolean enabled = new AtomicBoolean(false);
		final AssistantStatistics statistics = statistics(file, enabled, new AtomicReference<>(AssistantTuning.DEFAULTS));

		statistics.record("gap.no_death_certain", Event.ACTED, 0);
		statistics.flush();

		Assertions.assertEquals(0L, statistics.count("gap.no_death_certain", Event.ACTED));
		Assertions.assertNull(file.text);
		Assertions.assertTrue(statistics.rows().isEmpty());

		enabled.set(true);
		statistics.record("gap.no_death_certain", Event.ACTED, 0);
		Assertions.assertEquals(1L, statistics.count("gap.no_death_certain", Event.ACTED));
	}

	@Test
	void whatTheUserDoesIsCountedForEachKindWithTheAveragePositionOfWhatWasActedOn(){
		final AssistantStatistics statistics = statistics(new Memory());

		statistics.add("gap.no_birth", Event.SHOWN, 40, -1);
		statistics.record("gap.no_birth", Event.ACTED, 2);
		statistics.record("gap.no_birth", Event.ACTED, 4);
		statistics.record("gap.no_birth", Event.IGNORED, 7);
		statistics.record("gap.no_birth", Event.IGNORED_KIND, 7);
		statistics.record("gap.no_birth", Event.SNOOZED, 1);

		final AssistantStatistics.Row row = statistics.rows().getFirst();
		Assertions.assertEquals("gap.no_birth", row.kind());
		Assertions.assertEquals(40L, row.count(Event.SHOWN));
		Assertions.assertEquals(2L, row.count(Event.ACTED));
		Assertions.assertEquals(2L, row.ignored());
		Assertions.assertEquals(3., row.averagePosition());
		Assertions.assertEquals(0.05, row.actedShare(), 1e-9);
		//the position of what was not acted on does not count: it is where the findings were, not what the user chose
		Assertions.assertEquals(3., statistics.averagePosition("gap.no_birth"));
		Assertions.assertEquals(-1., statistics.averagePosition("gap.unknown"));
	}

	@Test
	void theCountsAddUpOverTheRuns(){
		final Memory file = new Memory();
		final AssistantStatistics first = statistics(file);
		first.record("place.no_source", Event.ACTED, 1);
		first.add("place.no_source", Event.SHOWN, 10, -1);
		first.flush();

		//the next run reads the file and adds to it
		final AssistantStatistics second = statistics(file);
		second.record("place.no_source", Event.ACTED, 3);
		second.add("place.no_source", Event.SHOWN, 5, -1);
		second.flush();

		final AssistantStatistics third = statistics(file);
		Assertions.assertEquals(2L, third.count("place.no_source", Event.ACTED));
		Assertions.assertEquals(15L, third.count("place.no_source", Event.SHOWN));
		Assertions.assertEquals(2., third.averagePosition("place.no_source"));
	}

	@Test
	void theCountsOfOtherNumbersOfTheAssistantsAreKeptApart(){
		final Memory file = new Memory();
		final AtomicReference<AssistantTuning> tuning = new AtomicReference<>(AssistantTuning.DEFAULTS);
		final AssistantStatistics statistics = statistics(file, new AtomicBoolean(true), tuning);
		statistics.record("gap.no_birth", Event.ACTED, 0);

		tuning.set(AssistantTuning.DEFAULTS.with(AssistantTuning.Parameter.GAP_BIRTH, 60));
		Assertions.assertEquals(0L, statistics.count("gap.no_birth", Event.ACTED));
		statistics.record("gap.no_birth", Event.ACTED, 0);
		statistics.record("gap.no_birth", Event.ACTED, 0);
		statistics.flush();

		Assertions.assertEquals(2L, statistics.count("gap.no_birth", Event.ACTED));
		Assertions.assertEquals(2, statistics.numberOfSets());
		//the file says which numbers each count was made with: only the ones that are not the defaults
		Assertions.assertTrue(file.text.contains("defaults"), file.text);
		Assertions.assertTrue(file.text.contains("gap_birth=60"), file.text);
	}

	@Test
	void theTextHoldsOnlyKindsAndNumbers(){
		final Memory file = new Memory();
		final AssistantStatistics statistics = statistics(file);
		statistics.record("chronology.event_after_death", Event.OPENED, -1);
		statistics.record("chronology.event_after_death", Event.ACTED, 5);
		statistics.flush();

		final String text = statistics.export();

		Assertions.assertEquals(file.text, text);
		Assertions.assertTrue(text.startsWith(AssistantStatistics.HEADER + "\n"), text);
		for(final String line : text.split("\n"))
			Assertions.assertTrue(line.equals(AssistantStatistics.HEADER) || line.startsWith("tuning ") || line.startsWith("kind "), line);
		Assertions.assertTrue(text.contains("ACTED=1") && text.contains("OPENED=1") && text.contains("positions=5:1"), text);
	}

	@Test
	void theFileIsWrittenOnlyWhenSomethingChangedAndResetForgetsEverything(){
		final Memory file = new Memory();
		final AssistantStatistics statistics = statistics(file);

		statistics.flush();
		Assertions.assertEquals(0, file.writes);
		statistics.record("gap.no_birth", Event.VIEWED, -1);
		statistics.flush();
		statistics.flush();
		Assertions.assertEquals(1, file.writes);

		statistics.reset();

		Assertions.assertTrue(statistics.rows().isEmpty());
		Assertions.assertEquals(AssistantStatistics.HEADER + "\n", file.text);
	}

	@Test
	void aFileThatIsNotOursOrDamagedStartsAgain(){
		final Memory file = new Memory();
		file.text = "something else\nkind x y ACTED=3\n";
		Assertions.assertTrue(statistics(file).rows().isEmpty());

		file.text = AssistantStatistics.HEADER + "\nkind abcd1234 gap.x ACTED=notanumber SHOWN=4 positions=oops\nkind\nnot a line\n";
		final AssistantStatistics statistics = statistics(file);
		Assertions.assertDoesNotThrow(statistics::export);
		statistics.record("gap.x", Event.ACTED, 1);
		Assertions.assertEquals(1L, statistics.count("gap.x", Event.ACTED));
	}

	@Test
	void thePageShowsTheCounts(){
		final AssistantStatistics statistics = statistics(new Memory());
		statistics.add("gap.no_birth", Event.SHOWN, 20, -1);
		statistics.record("gap.no_birth", Event.ACTED, 0);

		final JPanel panel = AssistantStatisticsDialog.panel(statistics);

		Assertions.assertEquals(10, AssistantStatisticsDialog.columnNames(panel).size());
		Assertions.assertEquals("Kind of finding", AssistantStatisticsDialog.columnNames(panel).getFirst());
		final JTable table = (JTable)((JScrollPane)panel.getComponent(1)).getViewport().getView();
		Assertions.assertEquals(1, table.getRowCount());
		Assertions.assertEquals(20L, table.getValueAt(0, 1));
		Assertions.assertEquals(1L, table.getValueAt(0, 2));
		Assertions.assertEquals(5., table.getValueAt(0, 3));
	}

	@Test
	void theCountingIsOffByDefaultInTheSettings(){
		final AssistantSettings settings = AssistantSettings.inMemory();
		Assertions.assertFalse(settings.isStatisticsEnabled());

		settings.setStatisticsEnabled(true);

		Assertions.assertTrue(settings.isStatisticsEnabled());
		final ResearchAssistantService service = new ResearchAssistantService(IgnoredFindings.inMemory(), settings);
		service.statistics().record("gap.no_birth", Event.ACTED, 0);
		Assertions.assertEquals(1L, service.statistics().count("gap.no_birth", Event.ACTED));
	}

}
