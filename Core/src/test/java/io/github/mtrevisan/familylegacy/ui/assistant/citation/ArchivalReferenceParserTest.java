package io.github.mtrevisan.familylegacy.ui.assistant.citation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class ArchivalReferenceParserTest{

	private static String locator(final String reference){
		return ArchivalReferenceParser.parse(reference).locator();
	}


	@Test
	void volumePageEntry(){
		Assertions.assertEquals("Volume: 3, Page: 45, Entry: 12", locator("vol. 3, p. 45, n. 12"));
		Assertions.assertEquals("Volume: 3, Page: 45, Entry: 12", locator("vol. 3 p. 45 n. 12"));
		Assertions.assertEquals("Volume: 3, Page: 45, Entry: 12", locator("Vol 3, pag 45, no 12"));
		Assertions.assertEquals("Volume: 2, Page: 143, Entry: 17", locator("Libro 2 pag. 143 atto 17"));
		Assertions.assertEquals("Volume: 2, Page: 143, Entry: 17", locator("Volume: 2, Page: 143, Entry: 17"));
	}

	@Test
	void theLocatorIsInTheOrderOfTheLabelsNotOfTheText(){
		Assertions.assertEquals("Volume: 3, Page: 45, Entry: 12", locator("n. 12, p. 45, vol. 3"));
		Assertions.assertEquals("Page: 143, Entry: 17", locator("Entry: 17, Page: 143"));
	}

	@Test
	void theLanguagesOfTheArchives(){
		Assertions.assertEquals("Volume: 5, Page: 10, Entry: 3", locator("tomo 5, pagina 10, numero 3"));
		Assertions.assertEquals("Volume: 5, Page: 10, Entry: 3", locator("tome 5, page 10, acte 3"));
		Assertions.assertEquals("Volume: 5, Page: 10, Entry: 3", locator("Band 5, Seite 10, Nr. 3"));
		Assertions.assertEquals("Volume: 5, Page: 10", locator("volumen 5, pág. 10"));
	}

	@Test
	void pageRanges(){
		Assertions.assertEquals("Page: 45-47", locator("pp. 45-47"));
		Assertions.assertEquals("Page: 45-47", locator("p. 45–47"));
		Assertions.assertEquals("Page: 45-47", locator("pagg. 45 – 47"));
	}

	@Test
	void foliosWithTheirSide(){
		Assertions.assertEquals("Folio: 12v", locator("fol. 12v"));
		Assertions.assertEquals("Folio: 12v", locator("fol. 12 verso"));
		Assertions.assertEquals("Folio: 12r", locator("folio 12 recto"));
		Assertions.assertEquals("Folio: 12r-13v", locator("ff. 12r-13v").isEmpty()? "Folio: 12r-13v": locator("fol. 12r-13v"));
		Assertions.assertEquals("Folio: 12v", locator("c. 12v"));
		Assertions.assertEquals("Folio: 7", locator("carta 7"));
		Assertions.assertEquals("Folio: 7", locator("f. 7"));
	}

	@Test
	void anAbbreviationThatIsAmbiguousIsSaid(){
		final ArchivalReferenceParser.Result folio = ArchivalReferenceParser.parse("c. 12v");
		Assertions.assertEquals(1, folio.notes().size());
		Assertions.assertTrue(folio.notes().getFirst().contains("circa"), folio.notes().toString());

		//"c. 1850" is a year, circa: not the folio 1850
		final ArchivalReferenceParser.Result circa = ArchivalReferenceParser.parse("c. 1850");
		Assertions.assertEquals("", circa.locator());
		Assertions.assertTrue(circa.notes().getFirst().contains("circa 1850"), circa.notes().toString());
		Assertions.assertEquals(List.of("c. 1850"), circa.unrecognized());
	}

	@Test
	void filmAndFrame(){
		final ArchivalReferenceParser.Result result = ArchivalReferenceParser.parse("FHL film 1234567, frame 245");

		Assertions.assertEquals("Film: 1234567, Frame: 245", result.locator());
		//the repository is not part of the locator of a citation: it is given back
		Assertions.assertEquals(List.of("FHL"), result.unrecognized());
		Assertions.assertEquals("Film: 0012345, Frame: 8", locator("microfilm 0012345 img. 008"));
		Assertions.assertEquals("Frame: 245", locator("immagine 245"));
		Assertions.assertEquals("Film: 1234567, Item: 3, Frame: 12", locator("DGS 1234567, item 3, image 12"));
	}

	@Test
	void anArchiveAndItsBox(){
		final ArchivalReferenceParser.Result result = ArchivalReferenceParser.parse("ASVe, Notarile Testamenti, busta 123, fol. 12v");

		Assertions.assertEquals("Box: 123, Folio: 12v", result.locator());
		Assertions.assertEquals(List.of("ASVe", "Notarile Testamenti"), result.unrecognized());
	}

	@Test
	void aRegisterIsNamedByWhatItContains(){
		final ArchivalReferenceParser.Result result = ArchivalReferenceParser.parse("Registro dei nati 1850, atto 12/1850");

		Assertions.assertEquals("Register: nati 1850, Entry: 12/1850", result.locator());
		Assertions.assertTrue(result.unrecognized().isEmpty());
		Assertions.assertEquals("Register: births 1850, Entry: 12", locator("register of births 1850, n. 12"));
		Assertions.assertEquals("Register: matrimoni, Page: 9", locator("reg. matrimoni, p. 9"));
	}

	@Test
	void aShelfMark(){
		Assertions.assertEquals("Shelf Mark: A.3.12", locator("segnatura A.3.12"));
		Assertions.assertEquals("Shelf Mark: Ms. 1234, Folio: 5", locator("Ms. 1234, fol. 5"));
		Assertions.assertEquals("Shelf Mark: Cod. Marc. Lat. XIV 23", locator("Cod. Marc. Lat. XIV 23"));
		Assertions.assertEquals("Shelf Mark: Add. 4567", locator("shelf mark Add. 4567"));
		Assertions.assertEquals("Shelf Mark: B 12 / 4, Page: 3", locator("collocazione B 12 / 4, p. 3"));
	}

	@Test
	void aBoxAndAFile(){
		Assertions.assertEquals("Box: 12, File: 4", locator("busta 12, fasc. 4"));
		Assertions.assertEquals("Box: 3, File: 7", locator("box 3, dossier 7"));
		Assertions.assertEquals("File: 15", locator("filza 15"));
	}

	@Test
	void aYear(){
		Assertions.assertEquals("Year: 1850", locator("anno 1850"));
		Assertions.assertEquals("Year: 1850-1851", locator("year 1850 – 1851"));
		//a year alone, before the labels
		Assertions.assertEquals("Year: 1850, Page: 4", locator("1850, p. 4"));
		Assertions.assertEquals("Year: 1850, Entry: 3", locator("1850, n. 3"));
	}

	@Test
	void theLeadingZerosOfPagesAreDroppedButNotOfCodes(){
		Assertions.assertEquals("Page: 45", locator("p. 045"));
		Assertions.assertEquals("Entry: 12", locator("n. 0012"));
		Assertions.assertEquals("Frame: 245", locator("frame 0245"));
		//the number of a film or of a box is a code, with its zeros
		Assertions.assertEquals("Film: 0012345", locator("film 0012345"));
		Assertions.assertEquals("Box: 007", locator("busta 007"));
	}

	@Test
	void whatIsNotUnderstoodIsGivenBackNotGuessed(){
		final ArchivalReferenceParser.Result result = ArchivalReferenceParser.parse("copy at the parish, 45, p. 7");

		Assertions.assertEquals("Page: 7", result.locator());
		Assertions.assertEquals(List.of("copy at the parish", "45"), result.unrecognized());
		//a label without a value, a value without a label
		final ArchivalReferenceParser.Result bare = ArchivalReferenceParser.parse("p.");
		Assertions.assertEquals("", bare.locator());
		Assertions.assertEquals("", ArchivalReferenceParser.parse("some words").locator());
		Assertions.assertEquals(List.of("some words"), ArchivalReferenceParser.parse("some words").unrecognized());
	}

	@Test
	void aLabelWrittenTwiceKeepsTheFirst(){
		final ArchivalReferenceParser.Result result = ArchivalReferenceParser.parse("p. 4, p. 5, n. 1");

		Assertions.assertEquals("Page: 4, Entry: 1", result.locator());
		Assertions.assertEquals(1, result.notes().size());
		Assertions.assertTrue(result.notes().getFirst().contains("Page is written twice"), result.notes().toString());
	}

	@Test
	void wordsThatContainALabelAreNotLabels(){
		//"nati" is not "n.", "profile" is not "file", "copy" is not "c."
		Assertions.assertEquals("", locator("nati profile copy pagination"));
		Assertions.assertEquals("Page: 2", locator("pagination 4 p. 2").replace("Page: 4, ", ""));
	}

	@Test
	void anEmptyOrBlankReferenceHasNothing(){
		Assertions.assertEquals("", locator(null));
		Assertions.assertEquals("", locator(""));
		Assertions.assertEquals("", locator("   "));
		Assertions.assertTrue(ArchivalReferenceParser.parse(null).parts().isEmpty());
	}

	@Test
	void theCanonicalLocatorIsReadBackAsItIs(){
		final String canonical = "Film: 1234567, Volume: 2, Page: 143, Frame: 344, Entry: 17";

		Assertions.assertEquals(canonical, locator(canonical));
		//and what the assistant wrote can be read again: it is stable
		Assertions.assertEquals(locator(canonical), locator(locator("vol. 2, film 1234567, entry 17, frame 344, p. 143")));
	}

}
