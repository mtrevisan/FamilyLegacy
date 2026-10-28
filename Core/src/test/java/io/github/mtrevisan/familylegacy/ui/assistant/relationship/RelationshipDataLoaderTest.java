package io.github.mtrevisan.familylegacy.ui.assistant.relationship;

import io.github.mtrevisan.familylegacy.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticActions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** The checks on a real model: what is read from the records, and what is found. */
class RelationshipDataLoaderTest{

	/** Carlo and Anna are married, and have a child: the way an import from GEDCOM writes a marriage without a type. */
	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Anna /Rossi/
		1 SEX F
		1 BIRT
		2 DATE 1 JAN 1850
		0 @I2@ INDI
		1 NAME Carlo /Bianchi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1845
		0 @I3@ INDI
		1 NAME Dino /Bianchi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1870
		1 FAMC @F1@
		0 @I4@ INDI
		1 NAME Fede /Verdi/
		1 SEX F
		1 BIRT
		2 DATE 1 JAN 1855
		0 @F1@ FAM
		1 HUSB @I2@
		1 WIFE @I1@
		1 CHIL @I3@
		1 MARR
		2 DATE 1 JUN 1868
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static List<RelationshipFinding> findings(final FLEFModel model){
		return RelationshipChecker.findings(RelationshipDataLoader.load(model));
	}

	private static String id(final FLEFModel model, final String name){
		return model.getRecordsByType("individual").stream()
			.filter(r -> IndividualReader.extractPrimaryFullname(r).contains(name))
			.findFirst()
			.orElseThrow()
			.getId();
	}

	/** A relationship written as the editor writes it. */
	private static FLEFRecord relationship(final String id, final String subject, final String target, final String type){
		final FLEFRecord relationship = FLEFRecord.createMainRecord(id, "relationship");
		relationship.addChild(FLEFRecord.createChildWithTag("subject")
			.addChild(FLEFRecord.createChildWithTagAndValue("individual", subject)));
		relationship.addChild(FLEFRecord.createChildWithTag("target")
			.addChild(FLEFRecord.createChildWithTagAndValue("individual", target)));
		relationship.addChild(FLEFRecord.createChildWithTagAndValue("type", type));
		return relationship;
	}


	@Test
	void readsTheRelationshipsAndTheEventsOfAModel() throws IOException{
		final FLEFModel model = convert();

		final RelationshipData data = RelationshipDataLoader.load(model);

		Assertions.assertEquals(4, data.persons().size());
		Assertions.assertEquals(1, data.groups().size());
		//the child and the spouse relationships, and the memberships of the family group
		final List<String> types = data.relationships().stream().map(RelationshipData.Relationship::type).toList();
		Assertions.assertTrue(types.contains("biological_child"), types.toString());
		Assertions.assertTrue(types.stream().anyMatch(t -> t.endsWith("spouse")), types.toString());
		Assertions.assertTrue(types.contains("group_member"), types.toString());
		//the groups are read as groups, and the individuals as individuals
		Assertions.assertTrue(data.relationships().stream()
			.filter(r -> "group_member".equals(r.type()))
			.allMatch(r -> r.subjectKind() == RelationshipData.Kind.INDIVIDUAL && r.targetKind() == RelationshipData.Kind.GROUP));
		//the marriage is an event the spouses took part in
		Assertions.assertTrue(data.events().stream()
			.anyMatch(e -> "marriage".equals(e.type()) && e.participants().size() >= 2 && e.date() != null));
		Assertions.assertNotNull(data.persons().get(id(model, "Dino")).birth());
	}

	@Test
	void anImportedFamilyHasNothingWrong() throws IOException{
		final List<Diagnostic> findings = findings(convert()).stream()
			.map(RelationshipFinding::diagnostic)
			.toList();

		//the memberships of the group fit what they link; only the marriage without a type may be reported
		Assertions.assertTrue(findings.stream().noneMatch(f -> f.title().startsWith("Type not valid")), findings.toString());
		Assertions.assertTrue(findings.stream().noneMatch(f -> f.severity() != Diagnostic.Severity.INFORMATION), findings.toString());
		Assertions.assertTrue(findings.stream().noneMatch(f -> f.title().startsWith("Marriage without")), findings.toString());
	}

	@Test
	void aMarriageWhoseRelationshipWasDeletedComesWithADraft() throws IOException{
		final FLEFModel model = convert();
		final RelationshipData data = RelationshipDataLoader.load(model);
		for(final RelationshipData.Relationship relationship : data.relationships())
			if(relationship.type().endsWith("spouse"))
				model.removeRecord(relationship.id());

		final List<RelationshipFinding> findings = findings(model).stream()
			.filter(f -> f.draft() != null)
			.toList();

		Assertions.assertEquals(1, findings.size());
		final RelationshipDraft draft = findings.getFirst().draft();
		Assertions.assertEquals(List.of(id(model, "Carlo"), id(model, "Anna")).stream().sorted().toList(),
			List.of(draft.subjectId(), draft.targetId()).stream().sorted().toList());
		//the draft is a suggestion: nothing was created in the model
		Assertions.assertTrue(model.getRecordsByType("relationship").stream()
			.noneMatch(r -> r.getId().equals(draft.subjectId())));
	}

	@Test
	void theFindingsOfAModelWithRelationshipsAddedByTheResearcher() throws IOException{
		final FLEFModel model = convert();
		final String dino = id(model, "Dino");
		final String fede = id(model, "Fede");
		final String carlo = id(model, "Carlo");
		final String anna = id(model, "Anna");
		//Fede is a generic parent of Dino, and married to Carlo (the other parent) when Dino was already grown
		model.addRecord(relationship("R90", dino, fede, "child"));
		final FLEFRecord marriage = relationship("R91", fede, carlo, "civil_spouse");
		marriage.addChild(FLEFRecord.createChildWithTag("valid_from")
			.addChild(FLEFRecord.createChildWithTag("value")
				.addChild(FLEFRecord.createChildWithTag("point")
					.addChild(FLEFRecord.createChildWithTag("value")
						.addChild(FLEFRecord.createChildWithTagAndValue("full_date", "1 JAN 1890")))
					.addChild(FLEFRecord.createChildWithTagAndValue("calendar", "gregorian")))));
		model.addRecord(marriage);
		//and a relationship of Anna with herself
		model.addRecord(relationship("R92", anna, anna, "associate"));

		final List<String> titles = findings(model).stream()
			.map(f -> f.diagnostic().title())
			.toList();

		Assertions.assertTrue(titles.stream().anyMatch(t -> t.startsWith("Relationship with itself: Anna")), titles.toString());
		Assertions.assertTrue(titles.stream().anyMatch(t -> t.startsWith("Child relationship that looks like a step-parent: Dino")),
			titles.toString());
	}

	@Test
	void everyFindingCanBeOpenedAndIgnored() throws IOException{
		final FLEFModel model = convert();
		final String anna = id(model, "Anna");
		model.addRecord(relationship("R92", anna, anna, "associate"));

		final List<Diagnostic> found = new RelationshipAssistantContributor().diagnosticProviders().getFirst()
			.diagnose(new AssistantContext(null, null, model));

		Assertions.assertFalse(found.isEmpty());
		for(final Diagnostic finding : found){
			Assertions.assertTrue(finding.isIgnorable(), finding.title());
			Assertions.assertFalse(DiagnosticActions.of(finding).isEmpty(), finding.title());
			Assertions.assertEquals(AssistantCategory.RELATIONSHIPS, finding.category());
		}
	}

	@Test
	void aFindingWithADraftOffersItFirst() throws IOException{
		final FLEFModel model = convert();
		final RelationshipData data = RelationshipDataLoader.load(model);
		for(final RelationshipData.Relationship relationship : data.relationships())
			if(relationship.type().endsWith("spouse"))
				model.removeRecord(relationship.id());

		final List<Diagnostic> found = new RelationshipAssistantContributor().diagnosticProviders().getFirst()
			.diagnose(new AssistantContext(null, null, model));
		final Diagnostic missing = found.stream()
			.filter(f -> f.title().startsWith("Marriage without a relationship"))
			.findFirst()
			.orElseThrow();

		//Enter starts the draft, Ctrl+Enter opens the records
		Assertions.assertNotNull(missing.action());
		Assertions.assertEquals("Create Relationship Draft…", missing.action().title());
		Assertions.assertSame(missing.action(), DiagnosticActions.of(missing).getFirst());
		Assertions.assertEquals(2, DiagnosticActions.of(missing).size());
	}

	@Test
	void anEmptyModelHasNothingToReport() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader("0 HEAD\n1 CHAR UTF-8\n"));

		Assertions.assertTrue(findings(new GEDCOMToFLEFConverter().convert(roots)).isEmpty());
	}

}
