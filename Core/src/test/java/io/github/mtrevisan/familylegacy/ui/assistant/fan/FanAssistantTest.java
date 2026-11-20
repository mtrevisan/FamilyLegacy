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
package io.github.mtrevisan.familylegacy.ui.assistant.fan;

import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.assistant.DiagnosticProvider;
import io.github.mtrevisan.familylegacy.assistant.fan.FanAssistantContributor;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.assistant.SwingAssistantContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.dossier.IndividualDossierPanel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;


/** The FAN table in the dossier, and what the Research Assistant says about the people who appear with the selected individual. */
class FanAssistantTest{

	private static final String AUDIT = "\taudit.creation.date 2026-01-01T00:00:00Z\n";
	private static final String HEADER = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		""";

	//Anna is baptised in 1890 and marries in 1910: Bruno is witness at both, Carla and Dario are godparents at the baptism
	private static final String FLEF = HEADER
		+ person("I1", "Anna") + person("I2", "Bruno") + person("I3", "Carla") + person("I4", "Dario")
		+ event("E1", "1890") + event("E2", "1910")
		+ participation("EP1", "E1", "I1", "principal") + participation("EP2", "E1", "I2", "witness")
		+ participation("EP3", "E1", "I3", "sponsor") + participation("EP4", "E1", "I4", "sponsor")
		+ participation("EP5", "E2", "I1", "principal") + participation("EP6", "E2", "I2", "witness");


	/** An individual with a death event, so that it is not living and is public. */
	private static String person(final String id, final String name){
		return "individual " + id + " {\n\tname {\n\t\tpart {\n\t\t\ttype given\n\t\t\tvalue " + name + "\n\t\t}\n\t}\n" + AUDIT + "}\n"
			+ "event D" + id + " {\n\ttype death\n" + AUDIT + "}\n"
			+ participation("DP" + id, "D" + id, id, "principal");
	}

	private static String event(final String id, final String year){
		return "event " + id + " {\n\ttype death\n\tdate.value.point {\n\t\tvalue.full_date " + year + "\n\t\tcalendar gregorian\n\t}\n"
			+ AUDIT + "}\n";
	}

	private static String participation(final String id, final String eventId, final String individualId, final String role){
		return "event_participation " + id + " {\n\tevent " + eventId + "\n\tparticipant.individual " + individualId + "\n\trole " + role
			+ "\n" + AUDIT + "}\n";
	}

	private static List<Diagnostic> findings(final FLEFModel model, final String selectedId){
		final List<Diagnostic> findings = new ArrayList<>();
		for(final DiagnosticProvider provider : new FanAssistantContributor().diagnosticProviders())
			findings.addAll(provider.diagnose(new SwingAssistantContext(null, null, model, null, null, selectedId)));
		return findings;
	}

	private static JTable findTable(final Container container){
		for(final Component component : container.getComponents()){
			if(component instanceof JTable table)
				return table;
			if(component instanceof Container child){
				final JTable table = findTable(child);
				if(table != null)
					return table;
			}
		}
		return null;
	}


	@Test
	void theSelectedIndividualGetsTheFindingsOfItsFan(){
		final List<Diagnostic> findings = findings(new FLEFParser().parse(FLEF), "I1");

		final List<String> kinds = findings.stream()
			.map(Diagnostic::kind)
			.toList();
		//the witness and the godparents are to be searched together with Anna; the godparents might be a couple; the witness recurs
		Assertions.assertEquals(List.of("fan.together", "fan.couple", "fan.recurring"), kinds);
		final Diagnostic together = findings.get(0);
		Assertions.assertTrue(together.recordIds().containsAll(List.of("I1", "I2", "I3", "I4")));
		Assertions.assertTrue(findings.stream().allMatch(Diagnostic::isIgnorable));
		Assertions.assertTrue(findings.stream().allMatch(finding -> finding.action() == null), "it suggests, it does not act");
	}

	@Test
	void nothingIsSaidWhenNothingIsSelectedOrTheSelectionIsNotAnIndividual(){
		final FLEFModel model = new FLEFParser().parse(FLEF);

		Assertions.assertTrue(findings(model, null).isEmpty());
		Assertions.assertTrue(findings(model, "E1").isEmpty());
		Assertions.assertTrue(findings(model, "I99").isEmpty());
	}

	@Test
	void theDossierShowsTheFanTableStrongestFirst(){

		final IndividualDossierPanel panel = new IndividualDossierPanel(new FLEFParser().parse(FLEF));
		panel.setIndividual("I1");

		final JTable table = findTable(panel);
		Assertions.assertNotNull(table);
		Assertions.assertEquals(3, table.getRowCount());
		//Bruno is at two events, the godparents at one
		Assertions.assertEquals("Bruno [I2]", table.getValueAt(0, 0));
		Assertions.assertEquals(2, table.getValueAt(0, 1));
		Assertions.assertEquals("1890-1910", table.getValueAt(0, 3));
	}

	@Test
	void theDossierOfSomebodyWithNobodyAroundHasNoFanTable(){

		final IndividualDossierPanel panel = new IndividualDossierPanel(new FLEFParser().parse(HEADER + person("I1", "Anna")));
		panel.setIndividual("I1");

		Assertions.assertNull(findTable(panel));
	}

}
