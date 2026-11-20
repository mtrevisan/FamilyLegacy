package io.github.mtrevisan.familylegacy.ui.tools.reports;

import io.github.mtrevisan.familylegacy.assistant.conclusion.ConclusionChecker;
import io.github.mtrevisan.familylegacy.assistant.conclusion.ConclusionData;
import io.github.mtrevisan.familylegacy.assistant.conclusion.ConclusionDataLoader;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.LivingPrivacy;
import io.github.mtrevisan.familylegacy.reports.ConflictConfidence;
import io.github.mtrevisan.familylegacy.reports.ConflictFilter;
import io.github.mtrevisan.familylegacy.reports.ConflictFinder;
import io.github.mtrevisan.familylegacy.reports.ConflictRecordType;
import io.github.mtrevisan.familylegacy.reports.OptionState;
import io.github.mtrevisan.familylegacy.reports.ReportBuilder;
import io.github.mtrevisan.familylegacy.reports.ReportConfig;
import io.github.mtrevisan.familylegacy.reports.ReportConflict;
import io.github.mtrevisan.familylegacy.reports.ReportOption;
import io.github.mtrevisan.familylegacy.reports.ReportProgressListener;
import io.github.mtrevisan.familylegacy.reports.ReportType;
import io.github.mtrevisan.reports.ReportDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** The Conflicts report on a real model: what the research assistant finds, written with the status that a conclusion gives. */
class ConflictReportTest{

	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Anna /Rossi/
		1 SEX F
		1 BIRT
		2 DATE 1 JAN 1850
		1 BIRT
		2 DATE 1 JAN 1860
		1 DEAT
		2 DATE 1 JAN 1900
		1 DEAT
		2 DATE 1 JAN 1910
		0 @I2@ INDI
		1 NAME Dino /Rossi/
		1 SEX M
		1 FAMC @F1@
		1 FAMC @F2@
		0 @I3@ INDI
		1 NAME Egidio /Bianchi/
		1 SEX M
		0 @I4@ INDI
		1 NAME Fede /Verdi/
		1 SEX F
		0 @I5@ INDI
		1 NAME Gino /Neri/
		1 SEX M
		0 @F1@ FAM
		1 HUSB @I3@
		1 WIFE @I4@
		1 CHIL @I2@
		0 @F2@ FAM
		1 HUSB @I5@
		1 WIFE @I4@
		1 CHIL @I2@
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	/** A conclusion that prefers the first of the births of Anna (it settles the conflict of her births). */
	private static void settleBirths(final FLEFModel model){
		final List<ConclusionData.PrincipalEvent> births = ConclusionDataLoader.load(model).events().stream()
			.filter(e -> EventReader.ENUM_TYPE_BIRTH.equals(e.type()))
			.toList();
		final FLEFRecord conclusion = FLEFRecord.createMainRecord("C1", ConclusionReader.TYPE);
		conclusion.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_ISSUE, "birth_date"));
		conclusion.addChild(FLEFRecord.createChildWithTagAndValue(ConclusionReader.TAG_PROOF_STATUS, ConclusionReader.ENUM_PROOF_STATUS_PROVEN));
		for(final ConclusionData.PrincipalEvent birth : births)
			conclusion.addChild(FLEFRecord.createChildWithTag(ConclusionReader.TAG_RESOLVES)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, birth.eventId())));
		conclusion.addChild(FLEFRecord.createChildWithTag(ConclusionReader.TAG_PREFERRED)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, births.getFirst().eventId())));
		model.addRecord(conclusion);
	}

	/** Gives the individual a name of the type. */
	private static void addName(final FLEFModel model, final String individualId, final String type, final String given, final String family){
		model.getRecordById(individualId).addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
			.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, type))
			.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_PART_TYPE, NameReader.ENUM_PART_TYPE_GIVEN))
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_PART_VALUE, given)))
			.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_PART_TYPE, NameReader.ENUM_PART_TYPE_FAMILY))
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_PART_VALUE, family))));
	}

	private static String text(final FLEFModel model, final ConflictFilter filter){
		return text(model, ReportConfig.defaults()
			.with(ReportOption.CONFLICT_FILTER, filter));
	}

	private static String text(final FLEFModel model, final ReportConfig base){
		final ReportConfig config = base.withLivingPrivacy(LivingPrivacy.SHOW);
		final ReportDocument document = ReportBuilder.build(ReportType.CONFLICTS, model, null, config, ReportProgressListener.NOOP);
		return document.title() + "\n" + document.sections();
	}


	@Test
	void severalNamesOfTheSameKindThatDifferAreAConflictThatNoConclusionCanSettle() throws IOException{
		final FLEFModel model = convert();
		addName(model, "I5", NameReader.ENUM_PERSONAL_TYPE_BIRTH, "Gino", "Neri");
		addName(model, "I5", NameReader.ENUM_PERSONAL_TYPE_BIRTH, "Gino", "Nero");
		//a name of another kind, or the same name written with other capitals, is not a conflict
		addName(model, "I5", NameReader.ENUM_PERSONAL_TYPE_NICKNAME, "Ginetto", "Neri");
		addName(model, "I3", NameReader.ENUM_PERSONAL_TYPE_BIRTH, "Egidio", "Bianchi");
		addName(model, "I3", NameReader.ENUM_PERSONAL_TYPE_BIRTH, "EGIDIO", "bianchi");

		final List<ReportConflict> names = ConflictFinder.provided().find(model).stream()
			.filter(conflict -> conflict.kind() == ReportConflict.Kind.NAME)
			.toList();

		Assertions.assertEquals(1, names.size());
		Assertions.assertEquals(List.of("I5"), names.getFirst().personIds());
		Assertions.assertEquals(ReportConflict.Problem.DIFFERENT_NAMES, names.getFirst().problem());
		Assertions.assertEquals(2, names.getFirst().assertions().size());
		Assertions.assertFalse(names.getFirst().resolved());
		//it is not a finding of the assistant: there is no draft of a conclusion for it
		Assertions.assertTrue(ConclusionChecker.findings(ConclusionDataLoader.load(model)).stream()
			.noneMatch(finding -> finding.draft().resolves().isEmpty()));
		final String text = text(model, ConflictFilter.ALL);
		Assertions.assertTrue(text.contains("Conflicting names"), text);
		Assertions.assertTrue(text.contains("birth: Gino Nero"), text);
		Assertions.assertFalse(text(model, ConflictFilter.RESOLVED).contains("Conflicting names"));
	}

	@Test
	void theFilterByTypeKeepsOnlyThatKindOfRecord() throws IOException{
		final FLEFModel model = convert();
		addName(model, "I5", NameReader.ENUM_PERSONAL_TYPE_BIRTH, "Gino", "Neri");
		addName(model, "I5", NameReader.ENUM_PERSONAL_TYPE_BIRTH, "Gino", "Nero");

		final String events = text(model, ReportConfig.defaults().with(ReportOption.CONFLICT_RECORD_TYPE, ConflictRecordType.EVENT));
		final String names = text(model, ReportConfig.defaults().with(ReportOption.CONFLICT_RECORD_TYPE, ConflictRecordType.NAME));
		final String relationships = text(model, ReportConfig.defaults().with(ReportOption.CONFLICT_RECORD_TYPE, ConflictRecordType.RELATIONSHIP));

		Assertions.assertTrue(events.contains("Conflicting births") && events.contains("Conflicting deaths"), events);
		Assertions.assertFalse(events.contains("Conflicting names") || events.contains("Conflicting parentage"), events);
		Assertions.assertTrue(events.contains("Only the conflicts about births, baptisms, deaths and burials are listed."), events);
		Assertions.assertTrue(names.contains("Conflicting names"), names);
		Assertions.assertFalse(names.contains("Conflicting births"), names);
		Assertions.assertTrue(relationships.contains("Conflicting parentage"), relationships);
		Assertions.assertFalse(relationships.contains("Conflicting births"), relationships);
	}

	@Test
	void theFilterByConfidenceKeepsTheConflictsWithAnAssertionOfThatConfidence() throws IOException{
		final FLEFModel model = convert();
		final ConclusionData.PrincipalEvent birth = ConclusionDataLoader.load(model).events().stream()
			.filter(e -> EventReader.ENUM_TYPE_BIRTH.equals(e.type()))
			.findFirst()
			.orElseThrow();
		model.getRecordById(birth.eventId())
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_CONFIDENCE, "low"));

		final String low = text(model, ReportConfig.defaults().with(ReportOption.CONFLICT_CONFIDENCE, ConflictConfidence.LOW));
		final String high = text(model, ReportConfig.defaults().with(ReportOption.CONFLICT_CONFIDENCE, ConflictConfidence.HIGH));
		final String notStated = text(model, ReportConfig.defaults().with(ReportOption.CONFLICT_CONFIDENCE, ConflictConfidence.NOT_STATED));

		Assertions.assertTrue(low.contains("Conflicting births"), low);
		Assertions.assertFalse(low.contains("Conflicting deaths"), low);
		Assertions.assertTrue(low.contains("Only the conflicts with an assertion whose confidence is low are listed."), low);
		Assertions.assertTrue(high.contains("No conflict has the status chosen for this report."), high);
		Assertions.assertTrue(notStated.contains("Conflicting deaths"), notStated);
	}

	@Test
	void theApplicationProvidesTheFinder(){
		Assertions.assertNotNull(ConflictFinder.provided());
	}

	@Test
	void everyConflictIsThere() throws IOException{
		final String text = text(convert(), ConflictFilter.ALL);

		Assertions.assertTrue(text.startsWith("Conflicts\n"), text);
		for(final String expected : List.of("Conflicting births", "Conflicting deaths", "Conflicting parentage", "Anna", "Dino",
				"the dates cannot all be right", "more than two biological parents", "1850", "1860", "unresolved"))
			Assertions.assertTrue(text.contains(expected), expected + " is missing in " + text);
		Assertions.assertFalse(text.contains("settled, one assertion preferred"), text);
	}

	@Test
	void aConclusionSettlesItsConflictAndTheReportSaysWhat() throws IOException{
		final FLEFModel model = convert();
		settleBirths(model);

		final String all = text(model, ConflictFilter.ALL);

		Assertions.assertTrue(all.contains("proven"), all);
		Assertions.assertTrue(all.contains("Judgement"), all);
		Assertions.assertTrue(all.contains("Preferred"), all);
		//the other conflicts are still not settled
		Assertions.assertTrue(all.contains("unresolved"), all);
	}

	@Test
	void theFilterKeepsTheConflictsThatAreOrAreNotSettled() throws IOException{
		final FLEFModel model = convert();
		settleBirths(model);

		final String unresolved = text(model, ConflictFilter.UNRESOLVED);
		final String resolved = text(model, ConflictFilter.RESOLVED);

		Assertions.assertFalse(unresolved.contains("Conflicting births"), unresolved);
		Assertions.assertTrue(unresolved.contains("Conflicting deaths"), unresolved);
		Assertions.assertTrue(unresolved.contains("Only the conflicts that no conclusion has settled are listed."), unresolved);
		Assertions.assertTrue(resolved.contains("Conflicting births"), resolved);
		Assertions.assertFalse(resolved.contains("Conflicting deaths"), resolved);
		Assertions.assertFalse(resolved.contains("Conflicting parentage"), resolved);
	}

	@Test
	void theConflictsWithTheirConclusionAreAllReported() throws IOException{
		final FLEFModel model = convert();
		settleBirths(model);

		final List<ReportConflict> conflicts = ConflictFinder.provided().find(model);

		Assertions.assertEquals(List.of(ReportConflict.Kind.BIRTH), conflicts.stream()
			.filter(ReportConflict::resolved)
			.map(ReportConflict::kind)
			.toList());
		final ReportConflict births = conflicts.stream()
			.filter(ReportConflict::resolved)
			.findFirst()
			.orElseThrow();
		Assertions.assertEquals("C1", births.settlement().conclusionId());
		Assertions.assertEquals(ConclusionReader.ENUM_PROOF_STATUS_PROVEN, births.settlement().proofStatus());
		Assertions.assertEquals(births.assertions().getFirst().recordId(), births.settlement().preferredId());
		Assertions.assertEquals(2, births.assertions().size());
		Assertions.assertEquals(ReportConflict.Problem.DIFFERENT_DATES, births.problem());
	}

	@Test
	void anEmptyFileHasNoConflict(){
		final String text = text(new FLEFModel(), ConflictFilter.ALL);

		Assertions.assertTrue(text.startsWith("Conflicts\n"), text);
	}

	@Test
	void aFileWithoutConflictsSaysSo() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader("0 HEAD\n1 CHAR UTF-8\n0 @I1@ INDI\n1 NAME Anna /Rossi/\n"));

		final String text = text(new GEDCOMToFLEFConverter().convert(roots), ConflictFilter.ALL);

		Assertions.assertTrue(text.contains("No conflict between competing assertions was found."), text);
	}

	@Test
	void theFilterIsAnOptionOfThisReportOnly(){
		for(final ReportOption option : List.of(ReportOption.CONFLICT_FILTER, ReportOption.CONFLICT_CONFIDENCE, ReportOption.CONFLICT_RECORD_TYPE))
			Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(ReportType.CONFLICTS, null, option), option.name());
		Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(ReportType.CONFLICTS, null, ReportOption.HEADER));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.CONFLICTS, null, ReportOption.SOURCES));
		for(final ReportType type : ReportType.values())
			if(type != ReportType.CONFLICTS)
				for(final ReportOption option : List.of(ReportOption.CONFLICT_FILTER, ReportOption.CONFLICT_CONFIDENCE, ReportOption.CONFLICT_RECORD_TYPE))
					Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(type, null, option), type.name());
	}

}
