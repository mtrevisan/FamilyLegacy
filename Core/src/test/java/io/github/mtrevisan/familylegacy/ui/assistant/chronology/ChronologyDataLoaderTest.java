package io.github.mtrevisan.familylegacy.ui.assistant.chronology;

import io.github.mtrevisan.familylegacy.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** The checks on a real model: what is read from the records, and what is found. */
class ChronologyDataLoaderTest{

	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Anna /Rossi/
		1 SEX F
		1 BIRT
		2 DATE 1 JAN 1850
		0 @I2@ INDI
		1 NAME Bruno /Rossi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1840
		1 DEAT
		2 DATE 1 JAN 1830
		0 @I3@ INDI
		1 NAME Carlo /Bianchi/
		1 SEX M
		1 BIRT
		2 DATE ABT 1800
		1 DEAT
		2 DATE 1 JAN 1870
		1 BURI
		2 DATE 5 JAN 1870
		0 @F1@ FAM
		1 WIFE @I1@
		1 CHIL @I2@
		0 @F2@ FAM
		1 HUSB @I3@
		1 WIFE @I1@
		1 MARR
		2 DATE 1 JUN 1875
		""";


	private static FLEFModel convert(final String gedcom) throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(gedcom));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static ChronologyData.Person personNamed(final ChronologyData data, final String name){
		return data.persons().values().stream()
			.filter(p -> p.name().contains(name))
			.findFirst()
			.orElseThrow();
	}


	@Test
	void readsPeopleDatesAndRelationships() throws IOException{
		final ChronologyData data = ChronologyDataLoader.load(convert(GEDCOM));

		Assertions.assertEquals(3, data.persons().size());
		final ChronologyData.Person anna = personNamed(data, "Anna");
		final ChronologyData.Person bruno = personNamed(data, "Bruno");
		final ChronologyData.Person carlo = personNamed(data, "Carlo");
		Assertions.assertEquals(SexType.FEMALE, anna.sex());
		Assertions.assertEquals(SexType.MALE, bruno.sex());
		Assertions.assertNotNull(anna.birth());
		Assertions.assertEquals(anna.birth().min(), anna.birth().max());
		Assertions.assertNull(anna.death());
		Assertions.assertNotNull(bruno.death());

		//an approximate date is a range, not a day
		Assertions.assertTrue(carlo.birth().max() - carlo.birth().min() > 365);

		//Bruno is the subject (the child), Anna the target (the parent)
		Assertions.assertEquals(1, data.parents().stream()
			.filter(link -> link.childId().equals(bruno.id()) && link.parentId().equals(anna.id()) && link.biological())
			.count());
		Assertions.assertFalse(data.events().isEmpty());
		Assertions.assertTrue(data.events().stream().anyMatch(e -> "burial".equals(e.type()) && e.personId().equals(carlo.id())));
		Assertions.assertTrue(data.events().stream().allMatch(ChronologyData.LifeEvent::principal));
		Assertions.assertFalse(data.unions().isEmpty());
		Assertions.assertTrue(data.unions().stream().allMatch(u -> u.firstId() != null && u.secondId() != null));
	}

	@Test
	void findsTheInconsistenciesOfAModel() throws IOException{
		final List<Diagnostic> findings = ChronologyChecker.check(ChronologyDataLoader.load(convert(GEDCOM)));
		final List<String> titles = findings.stream()
			.map(Diagnostic::title)
			.toList();

		//Bruno (1840) is a child of Anna (1850) and is said to have died in 1830
		Assertions.assertTrue(titles.stream().anyMatch(t -> t.startsWith("Death before birth: Bruno")), titles.toString());
		Assertions.assertTrue(titles.stream().anyMatch(t -> t.startsWith("Child born before the parent: Bruno")), titles.toString());
		//Anna marries Carlo in 1875, after his death in 1870
		Assertions.assertTrue(titles.stream().anyMatch(t -> t.startsWith("Event after death: Carlo")), titles.toString());
		//the burial five days after the death is fine, and so is the approximate birth of Carlo
		Assertions.assertTrue(titles.stream().noneMatch(t -> t.contains("burial")), titles.toString());
		Assertions.assertFalse(titles.stream().anyMatch(t -> t.contains("Carlo") && t.startsWith("Death before")), titles.toString());
	}

	@Test
	void anEmptyModelHasNothingToReport() throws IOException{
		final List<Diagnostic> findings = ChronologyChecker.check(ChronologyDataLoader.load(convert("0 HEAD\n1 CHAR UTF-8\n")));

		Assertions.assertTrue(findings.isEmpty());
	}

}
