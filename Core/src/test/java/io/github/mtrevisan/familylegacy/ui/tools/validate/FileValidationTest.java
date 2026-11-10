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
package io.github.mtrevisan.familylegacy.ui.tools.validate;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.io.merger.manual.MergeGrammar;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.AgentReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.quality.ConsistencyService;
import io.github.mtrevisan.familylegacy.quality.FileValidation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/** The validation of a file is one control: the grammar, and the structure of the records (circles, relationships with themselves, names). */
class FileValidationTest{

	/**
	 * Mario (I1) and his father Giovanni (I2) are each other's child; Anna (I3), Luca (I4) and Rosa (I5) are three in a circle; Pietro (I6)
	 * is fine; Sconosciuto (I7) has no name.
	 */
	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Mario /Rossi/
		1 SEX M
		1 FAMC @F1@
		1 FAMS @F2@
		0 @I2@ INDI
		1 NAME Giovanni /Rossi/
		1 SEX M
		1 FAMC @F2@
		1 FAMS @F1@
		0 @I3@ INDI
		1 NAME Anna /Bianchi/
		1 SEX F
		1 FAMC @F3@
		1 FAMS @F5@
		0 @I4@ INDI
		1 NAME Luca /Bianchi/
		1 SEX M
		1 FAMC @F4@
		1 FAMS @F3@
		0 @I5@ INDI
		1 NAME Rosa /Bianchi/
		1 SEX F
		1 FAMC @F5@
		1 FAMS @F4@
		0 @I6@ INDI
		1 NAME Pietro /Verdi/
		1 SEX M
		0 @I7@ INDI
		1 SEX M
		0 @F1@ FAM
		1 HUSB @I2@
		1 CHIL @I1@
		0 @F2@ FAM
		1 HUSB @I1@
		1 CHIL @I2@
		0 @F3@ FAM
		1 HUSB @I4@
		1 CHIL @I3@
		0 @F4@ FAM
		1 HUSB @I5@
		1 CHIL @I4@
		0 @F5@ FAM
		1 HUSB @I3@
		1 CHIL @I5@
		""";


	private static FLEFModel model() throws IOException{
		return new GEDCOMToFLEFConverter().convert(new GEDCOMParser().parse(new StringReader(GEDCOM)));
	}

	private static List<ConsistencyService.Issue> issues(final FLEFModel model, final ConsistencyService.Severity severity){
		return ConsistencyService.check(model).issues().stream()
			.filter(issue -> issue.severity() == severity)
			.toList();
	}


	@Test
	void eachCircleOfParentageIsReportedOnceWhateverItsSize() throws IOException{
		final List<ConsistencyService.Issue> circles = issues(model(), ConsistencyService.Severity.ERROR).stream()
			.filter(issue -> issue.message().startsWith("Circular parentage"))
			.toList();

		//two circles, one of two individuals and one of three: not one error for every individual in them
		Assertions.assertEquals(2, circles.size(), circles.toString());
		Assertions.assertEquals("I1", circles.get(0).recordId());
		Assertions.assertTrue(circles.get(0).message().contains("Mario Rossi") && circles.get(0).message().contains("Giovanni Rossi"),
			circles.get(0).message());
		Assertions.assertEquals("I3", circles.get(1).recordId());
		Assertions.assertTrue(circles.get(1).message().contains("Rosa Bianchi"), circles.get(1).message());
		//Pietro, who is in no circle, is in none of the messages
		Assertions.assertFalse(circles.get(0).message().contains("Pietro") || circles.get(1).message().contains("Pietro"));
	}

	@Test
	void anIndividualWithoutANameIsAWarning() throws IOException{
		final List<ConsistencyService.Issue> warnings = issues(model(), ConsistencyService.Severity.WARNING);

		Assertions.assertEquals(1, warnings.size(), warnings.toString());
		Assertions.assertEquals("I7", warnings.getFirst().recordId());
	}

	@Test
	void aRelationshipWithItselfIsAnErrorWhateverTheKindOfRecord() throws IOException{
		final FLEFModel model = model();
		model.addRecord(FLEFRecord.createMainRecord("R900", RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT).addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_GROUP, "G7")))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET).addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_GROUP, "G7")))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, RelationshipReader.ENUM_TYPE_PART_OF)));
		model.addRecord(FLEFRecord.createMainRecord("PR900", PlaceRelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_SUBJECT).addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_PLACE, "P1")))
			.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_TARGET).addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_PLACE, "P1"))));

		final List<String> selves = issues(model, ConsistencyService.Severity.ERROR).stream()
			.filter(issue -> issue.message().contains("points to itself"))
			.map(ConsistencyService.Issue::recordId)
			.toList();

		Assertions.assertEquals(List.of("R900", "PR900"), selves);
	}

	@Test
	void moreThanTwoBiologicalParentsIsAnError() throws IOException{
		final FLEFModel model = model();
		//Pietro (I6) gets three biological parents: Mario (I1), Giovanni (I2) and Anna (I3)
		for(final String parent : List.of("I1", "I2", "I3"))
			model.addRecord(biologicalChild("RC" + parent, "I6", parent));

		final List<ConsistencyService.Issue> found = issues(model, ConsistencyService.Severity.ERROR).stream()
			.filter(issue -> issue.message().contains("biological parents"))
			.toList();

		Assertions.assertEquals(1, found.size(), found.toString());
		Assertions.assertEquals("I6", found.getFirst().recordId());
		Assertions.assertTrue(found.getFirst().message().contains("3 biological parents"), found.getFirst().message());
		Assertions.assertTrue(found.getFirst().message().contains("Anna Bianchi"), found.getFirst().message());
	}

	private static FLEFModel peopleOf(final String... ids){
		final FLEFModel model = new FLEFModel();
		for(final String id : ids)
			model.addRecord(FLEFRecord.createMainRecord(id, IndividualReader.TYPE)
				.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME).addChild(FLEFRecord.createChildWithTag(DateReader.TAG_PART)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_GIVEN))
					.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_VALUE, "Name" + id)))));
		return model;
	}

	private static List<ConsistencyService.Issue> siblingsThatAreParents(final FLEFModel model){
		return issues(model, ConsistencyService.Severity.ERROR).stream()
			.filter(issue -> "sibling_and_parent".equals(issue.kind()))
			.toList();
	}

	@Test
	void aSiblingThatIsAlsoAParentIsAnError(){
		//C is a child of P and of S, and S is a child of P: C and S share P, so they are siblings, and S cannot also be the parent of C
		final FLEFModel model = peopleOf("P", "S", "C");
		model.addRecord(biologicalChild("R1", "C", "P"));
		model.addRecord(biologicalChild("R2", "C", "S"));
		model.addRecord(biologicalChild("R3", "S", "P"));

		final List<ConsistencyService.Issue> found = siblingsThatAreParents(model);

		Assertions.assertEquals(1, found.size(), found.toString());
		Assertions.assertEquals("C", found.getFirst().recordId());
		Assertions.assertTrue(found.getFirst().message().contains("NameS") && found.getFirst().message().contains("NameP"),
			found.getFirst().message());
	}

	private static List<ConsistencyService.Issue> childrenMarriedToParents(final FLEFModel model){
		return issues(model, ConsistencyService.Severity.ERROR).stream()
			.filter(issue -> "child_married_to_parent".equals(issue.kind()))
			.toList();
	}

	@Test
	void aBiologicalChildMarriedToTheirParentIsAnError(){
		final FLEFModel model = peopleOf("P", "C");
		model.addRecord(biologicalChild("R1", "C", "P"));
		//the marriage is written with the parent as the subject: the order does not matter
		model.addRecord(relationship("R2", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, RelationshipReader.TAG_INDIVIDUAL, "P",
			RelationshipReader.TAG_INDIVIDUAL, "C"));

		final List<ConsistencyService.Issue> found = childrenMarriedToParents(model);

		Assertions.assertEquals(1, found.size(), found.toString());
		Assertions.assertEquals("C", found.getFirst().recordId());
	}

	@Test
	void anAdoptiveChildOrAnEngagementIsNotReported(){
		final FLEFModel model = peopleOf("P", "C", "D");
		model.addRecord(biologicalChild("R1", "C", "P"));
		//an engagement is not a marriage
		model.addRecord(relationship("R2", RelationshipReader.ENUM_TYPE_ENGAGED_PARTNER, RelationshipReader.TAG_INDIVIDUAL, "C",
			RelationshipReader.TAG_INDIVIDUAL, "P"));
		//D is not a biological child of P (a step-parent can marry the other parent, and the marriage of an adoptive link is another matter)
		model.addRecord(relationship("R3", RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD, RelationshipReader.TAG_INDIVIDUAL, "D",
			RelationshipReader.TAG_INDIVIDUAL, "P"));
		model.addRecord(relationship("R4", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, RelationshipReader.TAG_INDIVIDUAL, "D",
			RelationshipReader.TAG_INDIVIDUAL, "P"));

		Assertions.assertTrue(childrenMarriedToParents(model).isEmpty());
	}

	@Test
	void aGrandparentOrAnUncleIsNotASiblingThatIsAParent(){
		final FLEFModel model = peopleOf("G", "P", "C", "U");
		//C is a child of P, P of G (a line of descent); U, the uncle, is another child of G: nobody is both sibling and parent
		model.addRecord(biologicalChild("R1", "C", "P"));
		model.addRecord(biologicalChild("R2", "P", "G"));
		model.addRecord(biologicalChild("R3", "U", "G"));

		Assertions.assertTrue(siblingsThatAreParents(model).isEmpty());
	}

	@Test
	void twoBiologicalParentsAreFineAndTheSameParentTwiceIsOne() throws IOException{
		final FLEFModel model = model();
		model.addRecord(biologicalChild("RA", "I6", "I1"));
		model.addRecord(biologicalChild("RB", "I6", "I2"));
		//the same parent again, written as another relationship
		model.addRecord(biologicalChild("RC", "I6", "I2"));

		Assertions.assertTrue(issues(model, ConsistencyService.Severity.ERROR).stream()
			.noneMatch(issue -> issue.message().contains("biological parents")));
	}

	private static List<String> sameSexErrors(final FLEFModel model){
		return issues(model, ConsistencyService.Severity.ERROR).stream()
			.map(ConsistencyService.Issue::message)
			.filter(message -> message.contains("two biological"))
			.toList();
	}

	@Test
	void twoBiologicalFathersAndTwoBiologicalMothersAreErrors() throws IOException{
		final FLEFModel fathers = model();
		//Pietro (I6) is the child of Mario (I1) and Giovanni (I2), both male
		fathers.addRecord(biologicalChild("RA", "I6", "I1"));
		fathers.addRecord(biologicalChild("RB", "I6", "I2"));
		final FLEFModel mothers = model();
		//and of Anna (I3) and Rosa (I5), both female
		mothers.addRecord(biologicalChild("RA", "I6", "I3"));
		mothers.addRecord(biologicalChild("RB", "I6", "I5"));

		final List<String> fatherErrors = sameSexErrors(fathers);
		final List<String> motherErrors = sameSexErrors(mothers);

		Assertions.assertEquals(1, fatherErrors.size(), fatherErrors.toString());
		Assertions.assertTrue(fatherErrors.getFirst().contains("two biological fathers"), fatherErrors.getFirst());
		Assertions.assertTrue(fatherErrors.getFirst().contains("Mario Rossi") && fatherErrors.getFirst().contains("Giovanni Rossi"),
			fatherErrors.getFirst());
		//the message says that the sex may be the wrong thing
		Assertions.assertTrue(fatherErrors.getFirst().contains("sex"), fatherErrors.getFirst());
		Assertions.assertEquals(1, motherErrors.size(), motherErrors.toString());
		Assertions.assertTrue(motherErrors.getFirst().contains("two biological mothers"), motherErrors.getFirst());
	}

	@Test
	void aFatherAndAMotherAreFine() throws IOException{
		final FLEFModel model = model();
		//Mario (male) and Anna (female)
		model.addRecord(biologicalChild("RA", "I6", "I1"));
		model.addRecord(biologicalChild("RB", "I6", "I3"));

		Assertions.assertTrue(sameSexErrors(model).isEmpty());
	}

	@Test
	void aMissingOrUnknownSexIsNotChecked() throws IOException{
		//Sconosciuto (I7) has no sex recorded here: with a male (I1) as the other parent nothing can be said
		final FLEFModel missing = model();
		for(final FLEFRecord sex : PreferredSex.assertions("I7", missing))
			missing.removeRecord(sex.getId());
		missing.addRecord(biologicalChild("RA", "I6", "I1"));
		missing.addRecord(biologicalChild("RB", "I6", "I7"));
		//a parent whose sex is unknown
		final FLEFModel unknown = model();
		unknown.addRecord(FLEFRecord.createMainRecord("I8", IndividualReader.TYPE));
		unknown.addRecord(FLEFRecord.createMainRecord("IA8", IndividualAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, "I8"))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_SEX))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, IndividualReader.ENUM_SEX_UNKNOWN)));
		unknown.addRecord(biologicalChild("RA", "I6", "I1"));
		unknown.addRecord(biologicalChild("RB", "I6", "I8"));

		Assertions.assertTrue(sameSexErrors(missing).isEmpty());
		Assertions.assertTrue(sameSexErrors(unknown).isEmpty());
	}

	@Test
	void withMoreThanTwoParentsTheErrorIsTheNumberOfThemNotTheirSex() throws IOException{
		final FLEFModel model = model();
		for(final String parent : List.of("I1", "I2", "I3"))
			model.addRecord(biologicalChild("RC" + parent, "I6", parent));

		Assertions.assertTrue(sameSexErrors(model).isEmpty());
	}

	private static FLEFRecord biologicalChild(final String id, final String childId, final String parentId){
		return FLEFRecord.createMainRecord(id, RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT).addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, childId)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET).addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, parentId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
	}

	@Test
	void aLongLineOfAncestorsDoesNotOverflowTheStackAndACircleAtItsEndIsFound(){
		final Map<String, List<String>> parentsOf = new HashMap<>();
		final int length = 60_000;
		for(int i = 0; i < length; i ++)
			parentsOf.put("I" + i, new ArrayList<>(List.of("I" + (i + 1))));
		//no circle
		Assertions.assertTrue(ConsistencyService.circles(parentsOf).isEmpty());

		//the last one is the parent of the first: one circle with all of them
		parentsOf.put("I" + length, new ArrayList<>(List.of("I0")));
		final List<List<String>> circles = ConsistencyService.circles(parentsOf);

		Assertions.assertEquals(1, circles.size());
		Assertions.assertEquals(length + 1, circles.getFirst().size());
	}

	@Test
	void theValidationOfTheFileIncludesTheStructureAndErrorsComeFirst() throws IOException{
		final FLEFValidator validator = new FLEFValidator(MergeGrammar.instance());

		final List<FLEFValidator.ValidationError> found = FileValidation.run(validator, model());

		final List<FLEFValidator.Severity> order = found.stream().map(FLEFValidator.ValidationError::severity).toList();
		Assertions.assertTrue(order.contains(FLEFValidator.Severity.ERROR) && order.contains(FLEFValidator.Severity.WARNING), order.toString());
		final int firstWarning = order.indexOf(FLEFValidator.Severity.WARNING);
		Assertions.assertFalse(order.subList(firstWarning, order.size()).contains(FLEFValidator.Severity.ERROR), order.toString());
		Assertions.assertTrue(found.stream().anyMatch(error -> error.recordId() != null && error.recordId().equals("I7")
			&& error.severity() == FLEFValidator.Severity.WARNING));
	}

	@Test
	void theStructuralChecksRunEvenWhenTheGrammarStopsTheValidation() throws IOException{
		final FLEFValidator validator = new FLEFValidator(MergeGrammar.instance());
		final FLEFModel model = model();
		//a record without its audit does not follow the grammar: the validator stops at the schema
		FLEFRecordHelper.removeChild(model.getRecordById("I6"), IndividualReader.TAG_AUDIT);
		final List<FLEFValidator.ValidationError> byGrammar = validator.validateAllStructured(model);

		final List<FLEFValidator.ValidationError> found = FileValidation.run(validator, model);

		Assertions.assertFalse(byGrammar.isEmpty(), "the record without its audit must be a problem of the grammar");
		Assertions.assertTrue(found.stream().anyMatch(error -> error.message().startsWith("Circular parentage")), found.toString());
	}

	@Test
	void everyStructuralIssueHasAKindThatTheReportGroupsBy() throws IOException{
		final List<FLEFValidator.ValidationError> found = FileValidation.structure(model());

		Assertions.assertFalse(found.isEmpty());
		Assertions.assertTrue(found.stream().noneMatch(error -> error.kind().isEmpty()), found.toString());
		Assertions.assertTrue(found.stream().anyMatch(error -> error.kind().equals("cycle")), found.toString());
		Assertions.assertTrue(found.stream().anyMatch(error -> error.kind().equals("no_name")), found.toString());
	}

	@Test
	void aModelThatIsMissingHasNoStructuralIssues(){
		Assertions.assertTrue(FileValidation.structure(null).isEmpty());
		Assertions.assertTrue(FileValidation.structure(new FLEFModel()).isEmpty());
	}

	@Test
	void theSummaryTellsErrorsAndWarningsAndAFileWithWarningsOnlyIsValid(){
		final FLEFValidator.ValidationError error = new FLEFValidator.ValidationError("e", "I1");
		final FLEFValidator.ValidationError warning = new FLEFValidator.ValidationError("w", "I2", FLEFValidator.Severity.WARNING);

		Assertions.assertEquals("The file is valid. No errors found.", ValidationReportDialog.summaryOf(List.of()));
		Assertions.assertEquals("2 issues found", ValidationReportDialog.summaryOf(List.of(error, error)));
		Assertions.assertEquals("No errors; 1 warning to look at", ValidationReportDialog.summaryOf(List.of(warning)));
		Assertions.assertEquals("1 error, 2 warnings", ValidationReportDialog.summaryOf(List.of(error, warning, warning)));
	}

	@Test
	void anErrorCreatedWithoutASeverityIsAnError(){
		Assertions.assertEquals(FLEFValidator.Severity.ERROR, new FLEFValidator.ValidationError("m", null).severity());
	}


	/* ======================================================================
	 *                          Conclusions, relationships, hierarchies, duplicates, dates
	 * ====================================================================== */

	private static FLEFRecord link(final String tag, final String kind, final String id){
		return FLEFRecord.createChildWithTag(tag).addChild(FLEFRecord.createChildWithTagAndValue(kind, id));
	}

	private static FLEFRecord relationship(final String id, final String type, final String subjectKind, final String subjectId,
			final String targetKind, final String targetId){
		return FLEFRecord.createMainRecord(id, RelationshipReader.TYPE)
			.addChild(link(RelationshipReader.TAG_SUBJECT, subjectKind, subjectId))
			.addChild(link(RelationshipReader.TAG_TARGET, targetKind, targetId))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, type));
	}

	private static FLEFRecord placeRelationship(final String id, final String type, final String subjectId, final String targetId){
		return FLEFRecord.createMainRecord(id, PlaceRelationshipReader.TYPE)
			.addChild(link(PlaceRelationshipReader.TAG_SUBJECT, PlaceRelationshipReader.TAG_PLACE, subjectId))
			.addChild(link(PlaceRelationshipReader.TAG_TARGET, PlaceRelationshipReader.TAG_PLACE, targetId))
			.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_TYPE, type));
	}

	private static FLEFRecord conclusion(final String id, final String proofStatus, final List<String> resolves, final String preferred){
		final FLEFRecord conclusion = FLEFRecord.createMainRecord(id, ConclusionReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_PROOF_STATUS, proofStatus));
		for(final String target : resolves)
			conclusion.addChild(link(ConclusionReader.TAG_RESOLVES, AgentReader.TAG_INDIVIDUAL, target));
		if(preferred != null)
			conclusion.addChild(link(ConclusionReader.TAG_PREFERRED, AgentReader.TAG_INDIVIDUAL, preferred));
		return conclusion;
	}

	private static List<ConsistencyService.Issue> issuesContaining(final FLEFModel model, final String text){
		return ConsistencyService.check(model).issues().stream()
			.filter(issue -> issue.message().contains(text))
			.toList();
	}

	@Test
	void thePreferredRecordOfAConclusionMustBeOneOfTheOnesItResolves() throws IOException{
		final FLEFModel model = model();
		model.addRecord(conclusion("CC1", ConclusionReader.ENUM_PROOF_STATUS_PROVEN, List.of("I1", "I2"), "I3"));
		model.addRecord(conclusion("CC2", ConclusionReader.ENUM_PROOF_STATUS_PROVEN, List.of("I1", "I2"), "I2"));

		final List<ConsistencyService.Issue> found = issuesContaining(model, "preferred record");

		Assertions.assertEquals(1, found.size(), found.toString());
		Assertions.assertEquals("CC1", found.getFirst().recordId());
		Assertions.assertEquals(ConsistencyService.Severity.ERROR, found.getFirst().severity());
	}

	@Test
	void aConclusionCannotRefertoRecordsThatDoNotExist() throws IOException{
		final FLEFModel model = model();
		model.addRecord(conclusion("CC1", ConclusionReader.ENUM_PROOF_STATUS_PROVEN, List.of("I1", "NOPE"), "I1"));

		final List<ConsistencyService.Issue> found = issuesContaining(model, "does not exist");

		Assertions.assertEquals(1, found.size(), found.toString());
		Assertions.assertTrue(found.getFirst().message().contains("NOPE"));
		Assertions.assertEquals(ConsistencyService.Severity.ERROR, found.getFirst().severity());
	}

	@Test
	void aConclusionThatDisprovesNormallyResolvesTwoRecordsAndPrefersNone() throws IOException{
		final FLEFModel model = model();
		model.addRecord(conclusion("CC1", ConclusionReader.ENUM_PROOF_STATUS_DISPROVEN, List.of("I1"), null));
		model.addRecord(conclusion("CC2", ConclusionReader.ENUM_PROOF_STATUS_DISPROVEN, List.of("I1", "I2"), "I1"));
		model.addRecord(conclusion("CC3", ConclusionReader.ENUM_PROOF_STATUS_DISPROVEN, List.of("I1", "I2"), null));

		final List<ConsistencyService.Issue> few = issuesContaining(model, "at least two records");
		final List<ConsistencyService.Issue> preferred = issuesContaining(model, "no preferred record");

		Assertions.assertEquals(List.of("CC1"), few.stream().map(ConsistencyService.Issue::recordId).toList());
		Assertions.assertEquals(List.of("CC2"), preferred.stream().map(ConsistencyService.Issue::recordId).toList());
		//it is a recommendation of the protocol, not a rule
		Assertions.assertEquals(ConsistencyService.Severity.WARNING, few.getFirst().severity());
		Assertions.assertEquals(ConsistencyService.Severity.WARNING, preferred.getFirst().severity());
	}

	@Test
	void aRelationshipTypeMustBeValidBetweenTheKindsOfRecordItLinks() throws IOException{
		final FLEFModel model = model();
		//a spouse that is a group, a membership between two individuals, and the valid ones
		model.addRecord(relationship("X1", RelationshipReader.ENUM_TYPE_SPOUSE, RelationshipReader.TAG_INDIVIDUAL, "I1", RelationshipReader.TAG_GROUP, "G1"));
		model.addRecord(relationship("X2", RelationshipReader.ENUM_TYPE_GROUP_MEMBER, RelationshipReader.TAG_INDIVIDUAL, "I1", RelationshipReader.TAG_INDIVIDUAL, "I2"));
		model.addRecord(relationship("X3", RelationshipReader.ENUM_TYPE_PART_OF, RelationshipReader.TAG_GROUP, "G1", RelationshipReader.TAG_INDIVIDUAL, "I1"));
		model.addRecord(relationship("X4", RelationshipReader.ENUM_TYPE_GROUP_MEMBER, RelationshipReader.TAG_INDIVIDUAL, "I1", RelationshipReader.TAG_GROUP, "G1"));
		model.addRecord(relationship("X5", RelationshipReader.ENUM_TYPE_PART_OF, RelationshipReader.TAG_GROUP, "G1", RelationshipReader.TAG_GROUP, "G2"));
		model.addRecord(relationship("X6", RelationshipReader.ENUM_TYPE_SPOUSE, RelationshipReader.TAG_INDIVIDUAL, "I1", RelationshipReader.TAG_INDIVIDUAL, "I2"));

		final List<String> wrong = issuesContaining(model, "cannot link").stream()
			.map(ConsistencyService.Issue::recordId)
			.toList();

		Assertions.assertEquals(List.of("X1", "X2", "X3"), wrong);
	}

	@Test
	void theRelationshipsOfAnImportedFileAreValid() throws IOException{
		//what the import writes (children, spouses, memberships of the families) is what the check accepts
		Assertions.assertTrue(issuesContaining(model(), "cannot link").isEmpty());
	}

	@Test
	void aCircleInTheHierarchyOfTheGroupsIsReportedOnce() throws IOException{
		final FLEFModel model = model();
		model.addRecord(relationship("X1", RelationshipReader.ENUM_TYPE_PART_OF, RelationshipReader.TAG_GROUP, "G1", RelationshipReader.TAG_GROUP, "G2"));
		model.addRecord(relationship("X2", RelationshipReader.ENUM_TYPE_PART_OF, RelationshipReader.TAG_GROUP, "G2", RelationshipReader.TAG_GROUP, "G3"));
		model.addRecord(relationship("X3", RelationshipReader.ENUM_TYPE_PART_OF, RelationshipReader.TAG_GROUP, "G3", RelationshipReader.TAG_GROUP, "G1"));
		//a chain that is not a circle
		model.addRecord(relationship("X4", RelationshipReader.ENUM_TYPE_PART_OF, RelationshipReader.TAG_GROUP, "G4", RelationshipReader.TAG_GROUP, "G5"));

		final List<ConsistencyService.Issue> found = issuesContaining(model, "Circular hierarchy of groups");

		Assertions.assertEquals(1, found.size(), found.toString());
		Assertions.assertEquals("G1", found.getFirst().recordId());
		Assertions.assertEquals(ConsistencyService.Severity.ERROR, found.getFirst().severity());
	}

	@Test
	void aCircleInTheJurisdictionsOfThePlacesIsAnErrorAndInTheirSuccessionAWarning() throws IOException{
		final FLEFModel model = model();
		model.addRecord(placeRelationship("PR1", PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF, "P1", "P2"));
		model.addRecord(placeRelationship("PR2", PlaceRelationshipReader.ENUM_TYPE_ECCLESIASTICAL_PART_OF, "P2", "P1"));
		model.addRecord(placeRelationship("PR3", PlaceRelationshipReader.ENUM_TYPE_SUCCESSOR_OF, "P3", "P4"));
		model.addRecord(placeRelationship("PR4", PlaceRelationshipReader.ENUM_TYPE_SPLIT_FROM, "P4", "P3"));

		final List<ConsistencyService.Issue> jurisdiction = issuesContaining(model, "Circular jurisdiction");
		final List<ConsistencyService.Issue> succession = issuesContaining(model, "Circular succession");

		Assertions.assertEquals(1, jurisdiction.size(), jurisdiction.toString());
		Assertions.assertEquals(ConsistencyService.Severity.ERROR, jurisdiction.getFirst().severity());
		Assertions.assertEquals(1, succession.size(), succession.toString());
		Assertions.assertEquals(ConsistencyService.Severity.WARNING, succession.getFirst().severity());
	}

	private static FLEFRecord citation(final String tag, final String citedId){
		return FLEFRecord.createChildWithTag(tag)
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, citedId))
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, SourceCitationReader.STANCE_SUPPORTS));
	}

	/** A source derived from the given sources. */
	private static FLEFRecord sourceDerivedFrom(final String id, final String... originalIds){
		final FLEFRecord source = FLEFRecord.createMainRecord(id, SourceReader.TYPE);
		for(final String originalId : originalIds)
			source.addChild(citation(SourceReader.TAG_DERIVED_FROM, originalId));
		return source;
	}

	@Test
	void aCircleOfSourcesDerivedFromEachOtherIsOneError() throws IOException{
		final FLEFModel model = model();
		model.addRecord(sourceDerivedFrom("SX1", "SX2"));
		model.addRecord(sourceDerivedFrom("SX2", "SX3"));
		model.addRecord(sourceDerivedFrom("SX3", "SX1"));
		//a chain that is not a circle, a source derived from two others that derive from the same one (a diamond, not a circle)
		model.addRecord(sourceDerivedFrom("SX4", "SX5"));
		model.addRecord(sourceDerivedFrom("SX5"));
		model.addRecord(sourceDerivedFrom("SX6", "SX7", "SX8"));
		model.addRecord(sourceDerivedFrom("SX7", "SX9"));
		model.addRecord(sourceDerivedFrom("SX8", "SX9"));
		model.addRecord(sourceDerivedFrom("SX9"));

		final List<ConsistencyService.Issue> found = issuesContaining(model, "Circular derivation of sources");

		Assertions.assertEquals(1, found.size(), found.toString());
		Assertions.assertEquals("SX1", found.getFirst().recordId());
		Assertions.assertEquals(ConsistencyService.Severity.ERROR, found.getFirst().severity());
		Assertions.assertEquals("source_cycle", found.getFirst().kind());
	}

	@Test
	void aSourceDerivedFromItselfIsAnError() throws IOException{
		final FLEFModel model = model();
		model.addRecord(sourceDerivedFrom("SX1", "SX1"));

		final List<ConsistencyService.Issue> found = issuesContaining(model, "is derived from itself");

		Assertions.assertEquals(1, found.size(), found.toString());
		Assertions.assertEquals("SX1", found.getFirst().recordId());
		Assertions.assertEquals(ConsistencyService.Severity.ERROR, found.getFirst().severity());
		//it is not also reported as a circle of several
		Assertions.assertTrue(issuesContaining(model, "Circular derivation of sources").isEmpty());
	}

	@Test
	void sourcesWhoseNotesCiteEachOtherAreNotACircleOfDerivation() throws IOException{
		//a note may cite a source to complete the text: it says nothing about where the source comes from
		final FLEFModel model = model();
		final FLEFRecord first = FLEFRecord.createMainRecord("SX1", SourceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_NOTE)
				.addChild(FLEFRecord.createChildWithTagAndValue("text", "a note"))
				.addChild(citation(SourceCitationReader.TAG_SOURCE, "SX2")));
		final FLEFRecord second = FLEFRecord.createMainRecord("SX2", SourceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_NOTE)
				.addChild(FLEFRecord.createChildWithTagAndValue("text", "a note"))
				.addChild(citation(SourceCitationReader.TAG_SOURCE, "SX1")));
		model.addRecord(first);
		model.addRecord(second);

		Assertions.assertTrue(issuesContaining(model, "Circular derivation of sources").isEmpty());
	}

	@Test
	void theSameRelationshipWrittenTwiceIsOneWarningAndACoupleFromBothSidesIsTheSame() throws IOException{
		final FLEFModel model = model();
		model.addRecord(relationship("X1", RelationshipReader.ENUM_TYPE_SPOUSE, RelationshipReader.TAG_INDIVIDUAL, "I1", RelationshipReader.TAG_INDIVIDUAL, "I2"));
		//the same couple written from the other side
		model.addRecord(relationship("X2", RelationshipReader.ENUM_TYPE_SPOUSE, RelationshipReader.TAG_INDIVIDUAL, "I2", RelationshipReader.TAG_INDIVIDUAL, "I1"));
		//a child is not symmetric: the parent of the child and the child of the parent are not the same
		model.addRecord(relationship("X3", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, RelationshipReader.TAG_INDIVIDUAL, "I6", RelationshipReader.TAG_INDIVIDUAL, "I1"));
		model.addRecord(relationship("X4", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, RelationshipReader.TAG_INDIVIDUAL, "I1", RelationshipReader.TAG_INDIVIDUAL, "I6"));
		//a different type between the same two is another relationship
		model.addRecord(relationship("X5", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, RelationshipReader.TAG_INDIVIDUAL, "I1", RelationshipReader.TAG_INDIVIDUAL, "I2"));

		final List<ConsistencyService.Issue> found = issuesContaining(model, "is written");

		Assertions.assertEquals(1, found.size(), found.toString());
		Assertions.assertEquals("X1", found.getFirst().recordId());
		Assertions.assertTrue(found.getFirst().message().contains("X1, X2"), found.getFirst().message());
		Assertions.assertEquals(ConsistencyService.Severity.WARNING, found.getFirst().severity());
	}

	@Test
	void twoAssociatesWithDifferentRolesAreTwoRelationshipsAndTheSameRoleIsOne() throws IOException{
		final FLEFModel model = model();
		//a guardian and a ward: what each is to the other, not one relationship written twice
		model.addRecord(relationship("X1", RelationshipReader.ENUM_TYPE_ASSOCIATE, RelationshipReader.TAG_INDIVIDUAL, "I1", RelationshipReader.TAG_INDIVIDUAL, "I2")
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_ROLE, "Guardian")));
		model.addRecord(relationship("X2", RelationshipReader.ENUM_TYPE_ASSOCIATE, RelationshipReader.TAG_INDIVIDUAL, "I2", RelationshipReader.TAG_INDIVIDUAL, "I1")
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_ROLE, "Ward")));
		Assertions.assertTrue(issuesContaining(model, "is written").isEmpty());

		//friends, written from both sides, with the role in different capitals: one
		model.addRecord(relationship("X3", RelationshipReader.ENUM_TYPE_ASSOCIATE, RelationshipReader.TAG_INDIVIDUAL, "I1", RelationshipReader.TAG_INDIVIDUAL, "I3")
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_ROLE, "Friend")));
		model.addRecord(relationship("X4", RelationshipReader.ENUM_TYPE_ASSOCIATE, RelationshipReader.TAG_INDIVIDUAL, "I3", RelationshipReader.TAG_INDIVIDUAL, "I1")
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_ROLE, "friend")));
		final List<ConsistencyService.Issue> found = issuesContaining(model, "is written");

		Assertions.assertEquals(1, found.size(), found.toString());
		Assertions.assertTrue(found.getFirst().message().contains("X3, X4"), found.getFirst().message());
	}

	private static FLEFRecord date(final String tag, final String isoDate){
		return FLEFRecord.createChildWithTag(tag)
			.addChild(FLEFRecord.createChildWithTag(DateReader.TAG_VALUE)
				.addChild(FLEFRecord.createChildWithTag(DateReader.TAG_POINT)
					.addChild(FLEFRecord.createChildWithTag(DateReader.TAG_VALUE)
						.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_FULL_DATE, isoDate)))));
	}

	private static List<String> validityErrors(final FLEFModel model){
		return new FLEFValidator(MergeGrammar.instance()).validateBusinessRulesStructured(model).stream()
			.map(FLEFValidator.ValidationError::message)
			.filter(message -> message.contains("must not be before"))
			.toList();
	}

	@Test
	void aRelationshipThatEndsBeforeItBeginsIsAnErrorByTheDaysNotByTheText() throws IOException{
		final FLEFModel model = model();
		model.addRecord(relationship("X1", RelationshipReader.ENUM_TYPE_SPOUSE, RelationshipReader.TAG_INDIVIDUAL, "I1", RelationshipReader.TAG_INDIVIDUAL, "I2")
			.addChild(date(RelationshipReader.TAG_VALID_FROM, "1900-05-01")).addChild(date(RelationshipReader.TAG_VALID_TO, "1850-01-01")));
		//a text that sorts the other way: 9 is after 10 as text, but the day is before
		model.addRecord(relationship("X2", RelationshipReader.ENUM_TYPE_SPOUSE, RelationshipReader.TAG_INDIVIDUAL, "I3", RelationshipReader.TAG_INDIVIDUAL, "I4")
			.addChild(date(RelationshipReader.TAG_VALID_FROM, "0999-03-01")).addChild(date(RelationshipReader.TAG_VALID_TO, "1000-03-01")));
		model.addRecord(relationship("X3", RelationshipReader.ENUM_TYPE_SPOUSE, RelationshipReader.TAG_INDIVIDUAL, "I5", RelationshipReader.TAG_INDIVIDUAL, "I6")
			.addChild(date(RelationshipReader.TAG_VALID_FROM, "1850-01-01")).addChild(date(RelationshipReader.TAG_VALID_TO, "1900-05-01")));
		model.addRecord(placeRelationship("PR1", PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF, "P1", "P2")
			.addChild(date(RelationshipReader.TAG_VALID_FROM, "1900-05-01")).addChild(date(RelationshipReader.TAG_VALID_TO, "1850-01-01")));

		final List<String> errors = validityErrors(model);

		Assertions.assertEquals(2, errors.size(), errors.toString());
		Assertions.assertTrue(errors.get(0).contains("VALID_TO must not be before VALID_FROM"), errors.get(0));
	}


	/* ======================================================================
	 *                          Events and media
	 * ====================================================================== */

	private static FLEFRecord event(final String id, final String type){
		return FLEFRecord.createMainRecord(id, EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, type));
	}

	private static FLEFRecord participation(final String id, final String eventId, final String individualId, final String role){
		final FLEFRecord participation = FLEFRecord.createMainRecord(id, EventParticipationReader.TYPE)
			.addChild(link(EventParticipationReader.TAG_PARTICIPANT, EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, individualId))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, eventId));
		if(role != null)
			participation.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, role));
		return participation;
	}

	private static List<String> eventIssues(final FLEFModel model){
		return ConsistencyService.check(model).issues().stream()
			.filter(issue -> issue.message().contains("participants"))
			.map(ConsistencyService.Issue::recordId)
			.toList();
	}

	@Test
	void anEventNobodyTookPartInIsAWarning() throws IOException{
		final FLEFModel model = model();
		model.addRecord(event("E1", "residence"));
		model.addRecord(event("E2", "residence"));
		model.addRecord(participation("P2", "E2", "I1", EventParticipationReader.ENUM_ROLE_PRINCIPAL));

		final List<ConsistencyService.Issue> found = ConsistencyService.check(model).issues().stream()
			.filter(issue -> issue.message().contains("has no participants"))
			.toList();

		Assertions.assertEquals(1, found.size(), found.toString());
		Assertions.assertEquals("E1", found.getFirst().recordId());
		Assertions.assertEquals(ConsistencyService.Severity.WARNING, found.getFirst().severity());
	}

	@Test
	void aLifeEventWhoseParticipantsAreAllKnownNotToBeThePrincipalIsNotAboutAnybody() throws IOException{
		final FLEFModel model = model();
		//a birth with a witness only: nobody was born
		model.addRecord(event("E1", EventReader.ENUM_TYPE_BIRTH));
		model.addRecord(participation("P1", "E1", "I1", EventParticipationReader.ENUM_ROLE_WITNESS));
		//a birth with a principal, a death with a principal and a witness: fine
		model.addRecord(event("E2", EventReader.ENUM_TYPE_BIRTH));
		model.addRecord(participation("P2", "E2", "I2", EventParticipationReader.ENUM_ROLE_PRINCIPAL));
		model.addRecord(event("E3", EventReader.ENUM_TYPE_DEATH));
		model.addRecord(participation("P3a", "E3", "I3", EventParticipationReader.ENUM_ROLE_PRINCIPAL));
		model.addRecord(participation("P3b", "E3", "I4", EventParticipationReader.ENUM_ROLE_WITNESS));
		//a baptism with a participant that has no role: the role is optional, nothing can be said
		model.addRecord(event("E4", "baptism"));
		model.addRecord(participation("P4a", "E4", "I1", EventParticipationReader.ENUM_ROLE_SPONSOR));
		model.addRecord(participation("P4b", "E4", "I2", null));
		//a marriage with a witness only is not judged: the other spouse may be unknown
		model.addRecord(event("E5", EventReader.ENUM_TYPE_MARRIAGE));
		model.addRecord(participation("P5", "E5", "I1", EventParticipationReader.ENUM_ROLE_WITNESS));

		Assertions.assertEquals(List.of("E1"), eventIssues(model));
	}

	@Test
	void theEventsOfAnImportedFileAreNotAboutNobody() throws IOException{
		final String gedcom = "0 HEAD\n1 CHAR UTF-8\n0 @I1@ INDI\n1 NAME Mario /Rossi/\n1 SEX M\n1 BIRT\n2 DATE 1 JAN 1850\n1 DEAT\n2 DATE 1 JAN 1920\n";
		final FLEFModel imported = new GEDCOMToFLEFConverter().convert(new GEDCOMParser().parse(new StringReader(gedcom)));

		Assertions.assertFalse(imported.getRecordsByType(EventReader.TYPE).isEmpty());
		Assertions.assertTrue(eventIssues(imported).isEmpty(), eventIssues(imported).toString());
	}

	private static FLEFRecord media(final String id, final String uri){
		return FLEFRecord.createMainRecord(id, MediaReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(MediaReader.TAG_URI, uri))
			.addChild(FLEFRecord.createChildWithTagAndValue(MediaReader.TAG_KIND, MediaReader.ENUM_KIND_IMAGE));
	}

	@Test
	void aMediaWhoseFileIsNotThereIsAWarning(@TempDir final Path directory) throws IOException{
		final Path existing = Files.createFile(directory.resolve("birth.jpg"));
		Files.createDirectories(directory.resolve("scans"));
		Files.createFile(directory.resolve("scans").resolve("page1.png"));
		final FLEFModel model = new FLEFModel();
		model.addRecord(media("M1", existing.toString()));
		model.addRecord(media("M2", directory.resolve("lost.jpg").toString()));
		model.addRecord(media("M3", existing.toUri().toString()));
		model.addRecord(media("M4", directory.resolve("lost.pdf").toUri().toString()));
		model.addRecord(media("M5", "scans/page1.png"));
		model.addRecord(media("M6", "scans/page2.png"));

		final List<String> missing = io.github.mtrevisan.familylegacy.quality.MediaFiles.missing(model, directory.toString()).stream()
			.map(ConsistencyService.Issue::recordId)
			.toList();

		//the ones that exist (a path, a file: address, a path relative to the base directory) are not reported
		Assertions.assertEquals(List.of("M2", "M4", "M6"), missing);
	}

	@Test
	void aRemoteLocationAndARelativePathWithNoBaseDirectoryAreNotJudged(@TempDir final Path directory){
		final FLEFModel model = new FLEFModel();
		model.addRecord(media("M1", "https://example.org/scans/birth.jpg"));
		model.addRecord(media("M2", "scans/page1.png"));
		model.addRecord(media("M3", "not a valid path <>|?*"));

		Assertions.assertTrue(io.github.mtrevisan.familylegacy.quality.MediaFiles.missing(model, null).isEmpty());
		//with a base directory the relative one is looked for, and is not there
		Assertions.assertEquals(List.of("M2"), io.github.mtrevisan.familylegacy.quality.MediaFiles.missing(model, directory.toString()).stream()
			.map(ConsistencyService.Issue::recordId)
			.filter("M2"::equals)
			.toList());
	}

	@Test
	void theMissingFilesAreInTheValidationOfTheFileAsWarnings(@TempDir final Path directory) throws IOException{
		final FLEFModel model = model();
		model.addRecord(media("M1", directory.resolve("lost.jpg").toString()));
		final FLEFValidator validator = new FLEFValidator(MergeGrammar.instance());

		final List<FLEFValidator.ValidationError> found = FileValidation.run(validator, model, directory.toString());

		Assertions.assertTrue(found.stream().anyMatch(error -> "M1".equals(error.recordId()) && error.severity() == FLEFValidator.Severity.WARNING
			&& error.message().contains("was not found")), found.toString());
		//without a media nothing is looked for
		Assertions.assertTrue(FileValidation.run(validator, model(), directory.toString()).stream()
			.noneMatch(error -> error.message().contains("was not found")));
	}

}
