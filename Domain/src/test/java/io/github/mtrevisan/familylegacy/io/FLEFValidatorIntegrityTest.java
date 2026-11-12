package io.github.mtrevisan.familylegacy.io;

import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;


class FLEFValidatorIntegrityTest{

	private static FLEFGrammar grammar;


	@BeforeAll
	static void loadGrammar(){
		grammar = FLEFGrammarParser.parseStandalone("""
			file F {
			  Record+
			}
			Record = oneof {
			  header: Header
			  note: NoteRecord
			  event_participation: ParticipationRecord
			}
			record Header {
			  version: string
			}
			record NoteRecord {
			  text: string
			  link?: Xref<NoteRecord>
			  inner*: InnerStructure
			}
			record InnerStructure {
			  text?: string
			  target?: Xref<NoteRecord>
			}
			record ParticipationRecord {
			  participant: Participant
			  role?: string
			}
			Participant = oneof {
			  individual: Xref<NoteRecord>
			  place: Xref<NoteRecord>
			}
			""");
	}


	@Test
	void nestedIdsAreNotReportedAsUnresolvedReferences(){
		final FLEFModel model = new FLEFParser().parse("""
			note N1 {
				text a
				inner X1 {
					text b
				}
			}
			""");

		final List<FLEFValidator.ValidationError> errors = new FLEFValidator(grammar).validateIntegrityStructured(model);

		Assertions.assertTrue(errors.isEmpty(), errors.toString());
	}

	@Test
	void anUnresolvedReferenceIsReported(){
		final FLEFModel model = new FLEFParser().parse("""
			note N1 {
				text a
				link N9
				inner X1 {
					target N8
				}
			}
			note N2 {
				text b
				link N1
			}
			""");

		final List<FLEFValidator.ValidationError> errors = new FLEFValidator(grammar).validateIntegrityStructured(model);

		Assertions.assertEquals(2, errors.size(), errors.toString());
		Assertions.assertTrue(errors.stream().allMatch(e -> "N1".equals(e.recordId())));
		Assertions.assertTrue(errors.stream().anyMatch(e -> e.message().contains("'N9'")));
		Assertions.assertTrue(errors.stream().anyMatch(e -> e.message().contains("'N8'")));
	}

	@Test
	void aReferenceInsideAOneOfIsChecked(){
		final FLEFModel model = new FLEFParser().parse("""
			note N1 {
				text a
			}
			event_participation EP1 {
				participant {
					individual N7
				}
			}
			""");

		final List<FLEFValidator.ValidationError> errors = new FLEFValidator(grammar).validateIntegrityStructured(model);

		Assertions.assertEquals(1, errors.size(), errors.toString());
		Assertions.assertEquals("EP1", errors.getFirst().recordId());
		Assertions.assertTrue(errors.getFirst().message().contains("'N7'"));
	}

	@Test
	void duplicateTopLevelIdsAreDetectedAtParseTime(){
		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse("""
			note N1 {
				text a
			}
			note N1 {
				text b
			}
			""");

		Assertions.assertEquals(1, model.getRecordCount());
		Assertions.assertTrue(model.getDuplicateIds().contains("N1"));
		Assertions.assertEquals(1, parser.getWarnings().size());

		final List<FLEFValidator.ValidationError> errors = new FLEFValidator(grammar).validateIntegrityStructured(model);
		Assertions.assertEquals(1, errors.size(), errors.toString());
		Assertions.assertEquals("N1", errors.getFirst().recordId());
	}

	@Test
	void programmaticReplacementIsNotADuplicate(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("N1", "note"));
		model.addRecord(FLEFRecord.createMainRecord("N1", "note"));

		Assertions.assertEquals(1, model.getRecordCount());
		Assertions.assertTrue(model.getDuplicateIds().isEmpty());
	}

	@Test
	void aRecordWithoutIdIsRejected(){
		final FLEFModel model = new FLEFModel();

		Assertions.assertThrows(IllegalArgumentException.class, () -> model.addRecord(FLEFRecord.createEmpty()));
		Assertions.assertThrows(IllegalArgumentException.class, () -> model.addRecord(null));
	}

	@Test
	void aLegacyRecordWithoutIdIsSkippedWithAWarning(){
		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse("""
			records {
				note {
					text a
				}
				note N1 {
					text b
				}
			}
			""");

		Assertions.assertEquals(1, model.getRecordCount());
		Assertions.assertEquals(1, parser.getWarnings().size());
	}

	@Test
	void nullAndEmptyHeadersAreEqual(){
		final FLEFModel withNull = new FLEFModel();
		final FLEFModel withEmpty = new FLEFModel();
		withEmpty.setHeader(FLEFRecord.createEmpty());

		Assertions.assertEquals(withNull, withEmpty);
		Assertions.assertEquals(withNull.hashCode(), withEmpty.hashCode());
	}

	@Test
	void thePropertyRoleRequiresAPlace(){
		final FLEFModel model = new FLEFParser().parse("""
			event_participation EP1 {
				participant {
					individual N1
				}
				role property
			}
			event_participation EP2 {
				participant {
					place N1
				}
				role property
			}
			event_participation EP3 {
				participant {
					place N1
				}
				role witness
			}
			event_participation EP4 {
				participant {
					individual N1
				}
				role witness
			}
			""");

		final List<FLEFValidator.ValidationError> errors = new FLEFValidator(grammar).validateBusinessRulesStructured(model);

		Assertions.assertEquals(2, errors.size(), errors.toString());
		Assertions.assertTrue(errors.stream().anyMatch(e -> "EP1".equals(e.recordId())));
		Assertions.assertTrue(errors.stream().anyMatch(e -> "EP3".equals(e.recordId())));
	}

}
