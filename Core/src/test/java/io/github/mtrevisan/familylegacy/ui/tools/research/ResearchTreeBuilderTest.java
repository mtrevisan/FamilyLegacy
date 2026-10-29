package io.github.mtrevisan.familylegacy.ui.tools.research;

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


class ResearchTreeBuilderTest{

	@BeforeAll
	static void loadGrammar() throws IOException{
		// the grammar initializes the index of the Xref paths
		try(final InputStream is = ResearchTreeBuilderTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
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

	private static String question(final String id, final String title){
		return "research_question " + id + " {\n"
			+ "\ttitle " + title + StringUtils.LF
			+ "\tquestion Who?\n"
			+ "\tstatus open\n"
			+ "\taudit.creation.date 2026-01-01T00:00:00Z\n"
			+ "}\n";
	}

	private static String activity(final String id, final String questionId, final String extra){
		return "research_activity " + id + " {\n"
			+ "\tquestion " + questionId + StringUtils.LF
			+ "\tactivity_type search\n"
			+ "\tstatus planned\n"
			+ "\taction Look in the archive\n"
			+ extra
			+ "\taudit.creation.date 2026-01-01T00:00:00Z\n"
			+ "}\n";
	}

	private static String task(final String id, final String questionId, final String extra){
		return "research_task " + id + " {\n"
			+ "\tdescription Ask the priest\n"
			+ "\tquestion " + questionId + StringUtils.LF
			+ "\tstatus open\n"
			+ extra
			+ "\taudit.creation.date 2026-01-01T00:00:00Z\n"
			+ "}\n";
	}


	@Test
	void tasksAreNestedUnderTheActivityThatCreatedThem(){
		final FLEFModel model = parse(question("Q1", "Parents")
			+ activity("A1", "Q1", "")
			+ task("T1", "Q1", "\tcreated_by A1\n")
			+ task("T2", "Q1", ""));

		final List<ResearchTreeBuilder.Node> roots = ResearchTreeBuilder.build(model);

		Assertions.assertEquals(1, roots.size());
		final ResearchTreeBuilder.Node questionNode = roots.get(0);
		Assertions.assertEquals(ResearchTreeBuilder.Kind.QUESTION, questionNode.kind());
		Assertions.assertEquals(2, questionNode.children().size());

		final ResearchTreeBuilder.Node activityNode = questionNode.children().get(0);
		Assertions.assertEquals(ResearchTreeBuilder.Kind.ACTIVITY, activityNode.kind());
		Assertions.assertEquals(1, activityNode.children().size());
		Assertions.assertEquals("T1", activityNode.children().get(0).record().getId());

		final ResearchTreeBuilder.Node looseTask = questionNode.children().get(1);
		Assertions.assertEquals(ResearchTreeBuilder.Kind.TASK, looseTask.kind());
		Assertions.assertEquals("T2", looseTask.record().getId());
	}

	@Test
	void childActivitiesAreNestedUnderTheirParentActivity(){
		final FLEFModel model = parse(question("Q1", "Parents")
			+ activity("A1", "Q1", "")
			+ activity("A2", "Q1", "\tparent_activity A1\n"));

		final ResearchTreeBuilder.Node questionNode = ResearchTreeBuilder.build(model).get(0);

		Assertions.assertEquals(1, questionNode.children().size());
		final ResearchTreeBuilder.Node parent = questionNode.children().get(0);
		Assertions.assertEquals("A1", parent.record().getId());
		Assertions.assertEquals(1, parent.children().size());
		Assertions.assertEquals("A2", parent.children().get(0).record().getId());
	}

	@Test
	void parentCyclesDoNotDropActivities(){
		final FLEFModel model = parse(question("Q1", "Parents")
			+ activity("A1", "Q1", "\tparent_activity A2\n")
			+ activity("A2", "Q1", "\tparent_activity A1\n"));

		final ResearchTreeBuilder.Node questionNode = ResearchTreeBuilder.build(model).get(0);

		Assertions.assertEquals(2, questionNode.count(ResearchTreeBuilder.Kind.ACTIVITY));
	}

	@Test
	void recordsReferencingMoreQuestionsAppearUnderEachOfThem(){
		final FLEFModel model = parse(question("Q1", "Parents") + question("Q2", "Birth")
			+ "research_activity A1 {\n\tquestion Q1\n\tquestion Q2\n\tactivity_type search\n\tstatus planned\n"
			+ "\taction Look\n\taudit.creation.date 2026-01-01T00:00:00Z\n}\n");

		final List<ResearchTreeBuilder.Node> roots = ResearchTreeBuilder.build(model);

		Assertions.assertEquals(2, roots.size());
		Assertions.assertEquals(1, roots.get(0).count(ResearchTreeBuilder.Kind.ACTIVITY));
		Assertions.assertEquals(1, roots.get(1).count(ResearchTreeBuilder.Kind.ACTIVITY));
	}

	@Test
	void filterKeepsOnlyTheMatchingBranches(){
		final FLEFModel model = parse(question("Q1", "Parents") + question("Q2", "Birth")
			+ task("T1", "Q2", ""));

		final List<ResearchTreeBuilder.Node> roots = ResearchTreeBuilder.build(model);

		Assertions.assertNotNull(roots.get(0).filter("parents"));
		Assertions.assertNull(roots.get(0).filter("priest"));
		final ResearchTreeBuilder.Node filtered = roots.get(1).filter("priest");
		Assertions.assertNotNull(filtered);
		Assertions.assertEquals(1, filtered.children().size());
	}

}
