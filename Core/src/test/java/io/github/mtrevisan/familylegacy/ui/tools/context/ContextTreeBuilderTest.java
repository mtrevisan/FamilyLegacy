package io.github.mtrevisan.familylegacy.ui.tools.context;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;


class ContextTreeBuilderTest{

	@BeforeAll
	static void loadGrammar() throws IOException{
		// the grammar initializes the index of the Xref paths
		try(final InputStream is = ContextTreeBuilderTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			FLEFGrammarParser.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
		}
	}


	private static FLEFModel parse(final String records){
		return new FLEFParser().parse("""
			header {
				protocol {
					name Family LEgacy Format
					version 0.2.0
				}
				date 2026-01-01T00:00:00Z
			}
			""" + records);
	}

	private static String historicEvent(final String id, final String title){
		return "historic_event " + id + " {\n"
			+ "\ttype war\n"
			+ "\ttitle " + title + StringUtils.LF
			+ "\taudit.creation.date 2026-01-01T00:00:00Z\n"
			+ "}\n";
	}

	private static String culturalNorm(final String id, final String title){
		return "cultural_norm " + id + " {\n"
			+ "\ttitle " + title + StringUtils.LF
			+ "\ttype naming_convention\n"
			+ "\taudit.creation.date 2026-01-01T00:00:00Z\n"
			+ "}\n";
	}

	private static String impact(final String id, final String contextTag, final String contextId, final String targetId,
			final String impactType){
		return "context_impact " + id + " {\n"
			+ "\tcontext." + contextTag + StringUtils.SPACE + contextId + StringUtils.LF
			+ "\ttarget.individual " + targetId + StringUtils.LF
			+ "\timpact_type " + impactType + StringUtils.LF
			+ "\taudit.creation.date 2026-01-01T00:00:00Z\n"
			+ "}\n";
	}


	@Test
	void impactsAreNestedUnderTheirContext(){
		final FLEFModel model = parse(historicEvent("H1", "Plague")
			+ culturalNorm("N1", "Patronymics")
			+ impact("CI1", "historic_event", "H1", "I1", "causes")
			+ impact("CI2", "historic_event", "H1", "I2", "influences")
			+ impact("CI3", "cultural_norm", "N1", "I1", "explains"));

		final List<ContextTreeBuilder.Node> roots = ContextTreeBuilder.build(model);

		Assertions.assertEquals(2, roots.size());

		final ContextTreeBuilder.Node events = roots.get(0);
		Assertions.assertEquals(ContextTreeBuilder.Kind.HISTORIC_EVENTS, events.kind());
		Assertions.assertEquals(1, events.children().size());
		final ContextTreeBuilder.Node plague = events.children().get(0);
		Assertions.assertEquals(ContextTreeBuilder.Kind.HISTORIC_EVENT, plague.kind());
		Assertions.assertEquals("H1", plague.record().getId());
		Assertions.assertTrue(plague.label().contains("Plague"));
		Assertions.assertTrue(plague.label().endsWith("(2I)"));
		Assertions.assertEquals(2, plague.children().size());
		Assertions.assertEquals(ContextTreeBuilder.Kind.IMPACT, plague.children().get(0).kind());
		Assertions.assertEquals("CI1", plague.children().get(0).record().getId());
		Assertions.assertTrue(plague.children().get(0).label().contains("causes"));

		final ContextTreeBuilder.Node norms = roots.get(1);
		Assertions.assertEquals(ContextTreeBuilder.Kind.CULTURAL_NORMS, norms.kind());
		Assertions.assertEquals("CI3", norms.children().get(0).children().get(0).record().getId());
	}

	@Test
	void impactOfAMissingContextGoesUnderOrphans(){
		final FLEFModel model = parse(historicEvent("H1", "Plague")
			+ impact("CI1", "historic_event", "H1", "I1", "causes")
			+ impact("CI2", "historic_event", "H9", "I1", "causes"));

		final List<ContextTreeBuilder.Node> roots = ContextTreeBuilder.build(model);

		Assertions.assertEquals(2, roots.size());
		final ContextTreeBuilder.Node orphans = roots.get(1);
		Assertions.assertEquals(ContextTreeBuilder.Kind.ORPHANS, orphans.kind());
		Assertions.assertEquals(1, orphans.children().size());
		Assertions.assertEquals("CI2", orphans.children().get(0).record().getId());
	}

	@Test
	void contextsAreSortedByTitleAndEmptySectionsAreOmitted(){
		final FLEFModel model = parse(historicEvent("H1", "Plague")
			+ historicEvent("H2", "Amnesty"));

		final List<ContextTreeBuilder.Node> roots = ContextTreeBuilder.build(model);

		Assertions.assertEquals(1, roots.size());
		Assertions.assertEquals(ContextTreeBuilder.Kind.HISTORIC_EVENTS, roots.get(0).kind());
		Assertions.assertEquals("H2", roots.get(0).children().get(0).record().getId());
		Assertions.assertEquals("H1", roots.get(0).children().get(1).record().getId());
	}

	@Test
	void filterKeepsOnlyTheMatchingBranches(){
		final FLEFModel model = parse(historicEvent("H1", "Plague")
			+ historicEvent("H2", "Amnesty")
			+ impact("CI1", "historic_event", "H1", "I1", "causes"));

		final ContextTreeBuilder.Node events = ContextTreeBuilder.build(model).get(0);

		final ContextTreeBuilder.Node byContext = events.filter("amnesty");
		Assertions.assertEquals(1, byContext.children().size());
		Assertions.assertEquals("H2", byContext.children().get(0).record().getId());

		final ContextTreeBuilder.Node byImpact = events.filter("causes");
		Assertions.assertEquals(1, byImpact.children().size());
		Assertions.assertEquals("CI1", byImpact.children().get(0).children().get(0).record().getId());

		Assertions.assertNull(events.filter("no such text"));
	}

}
