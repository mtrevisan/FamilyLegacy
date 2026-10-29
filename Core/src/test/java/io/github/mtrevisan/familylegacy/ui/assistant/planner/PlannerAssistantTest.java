package io.github.mtrevisan.familylegacy.ui.assistant.planner;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


class PlannerAssistantTest{

	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Anna /Rossi/
		1 BIRT
		2 DATE 1 JAN 1850
		1 FAMC @F1@
		0 @I2@ INDI
		1 NAME Bruno /Rossi/
		1 BIRT
		2 DATE 1 JAN 1820
		1 FAMS @F1@
		0 @F1@ FAM
		1 HUSB @I2@
		1 CHIL @I1@
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static List<Diagnostic> diagnose(final FLEFModel model){
		return new PlannerAssistantContributor().diagnosticProviders().getFirst().diagnose(new AssistantContext(null, null, model));
	}

	private static FLEFRecord question(final String id, final String targetId){
		return FLEFRecord.createMainRecord(id, "RESEARCH_QUESTION")
			.addChild(FLEFRecord.createChildWithTagAndValue("title", "Parents of Bruno"))
			.addChild(FLEFRecord.createChildWithTagAndValue("question", "Who were the parents of Bruno?"))
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", targetId)))
			.addChild(FLEFRecord.createChildWithTagAndValue("status", "open"));
	}

	private static String idOf(final FLEFModel model, final String name){
		return model.getRecordsByType("INDIVIDUAL").stream()
			.filter(r -> io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader.extractPrimaryFullname(r).contains(name))
			.findFirst().orElseThrow().getId();
	}


	@Test
	void anAncestorWithUnknownParentsIsABrickWallOfTheModel() throws IOException{
		final FLEFModel model = convert();

		final List<Diagnostic> found = diagnose(model);

		final List<Diagnostic> walls = found.stream().filter(d -> d.key().startsWith("planner.brick_wall|")).toList();
		Assertions.assertEquals(1, walls.size(), found.toString());
		Assertions.assertEquals(List.of(idOf(model, "Bruno")), walls.getFirst().recordIds());
		Assertions.assertNotNull(walls.getFirst().action());
	}

	@Test
	void anOpenQuestionAboutThemHasNextStepsAndTheBrickWallIsNoLongerProposed() throws IOException{
		final FLEFModel model = convert();
		final String bruno = idOf(model, "Bruno");
		model.addRecord(question("RQ1", bruno));
		final int count = model.getRecordCount();

		final List<Diagnostic> found = diagnose(model);

		Assertions.assertTrue(found.stream().noneMatch(d -> d.key().startsWith("planner.brick_wall|")), found.toString());
		final List<Diagnostic> steps = found.stream().filter(d -> d.key().startsWith("planner.next_step|")).toList();
		Assertions.assertFalse(steps.isEmpty());
		Assertions.assertTrue(steps.stream().allMatch(d -> d.recordIds().equals(List.of("RQ1", bruno))), steps.toString());
		Assertions.assertTrue(steps.stream().allMatch(d -> d.action() != null));
		//suggesting does not touch the model
		Assertions.assertEquals(count, model.getRecordCount());
	}

	@Test
	void aModelWithoutResearchHasNothing(){
		Assertions.assertTrue(diagnose(new FLEFModel()).isEmpty());
		Assertions.assertTrue(new PlannerAssistantContributor().diagnosticProviders().getFirst()
			.diagnose(new AssistantContext(null, null, null)).isEmpty());
	}

}
