package io.github.mtrevisan.familylegacy.importer.gedcom;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


class GEDCOM7NormalizerTest{

	private static List<GEDCOMNode> normalize(final String gedcom) throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(gedcom));
		GEDCOM7Normalizer.normalize(roots);
		return roots;
	}

	@Test
	void calendarKeywordBecomesEscape() throws IOException{
		final List<GEDCOMNode> roots = normalize("""
			0 HEAD
			1 GEDC
			2 VERS 7.0
			0 @I1@ INDI
			1 BIRT
			2 DATE JULIAN 1 JAN 1700
			1 DEAT
			2 DATE BET JULIAN 1 JAN 1700 AND FRENCH_R 1 VEND 5
			""");

		Assertions.assertEquals("@#DJULIAN@ 1 JAN 1700", roots.get(1).getChildren().get(0).getChildren().get(0).getValue());
		Assertions.assertEquals("BET @#DJULIAN@ 1 JAN 1700 AND @#DFRENCH R@ 1 VEND 5",
			roots.get(1).getChildren().get(1).getChildren().get(0).getValue());
	}

	@Test
	void calendarKeywordAfterATabIsRecognized() throws IOException{
		final List<GEDCOMNode> roots = normalize("0 HEAD\n0 @I1@ INDI\n1 BIRT\n2 DATE BET\tJULIAN 1 JAN 1700 AND 2 JAN 1700\n");

		Assertions.assertTrue(roots.get(1).getChildren().get(0).getChildren().get(0).getValue().contains("@#DJULIAN@"));
	}

	@Test
	void normalizationIsRepeatable() throws IOException{
		final List<GEDCOMNode> roots = normalize("""
			0 HEAD
			0 @I1@ INDI
			1 BIRT
			2 DATE 1 JAN 1700
			3 PHRASE around the new year
			""");
		GEDCOM7Normalizer.normalize(roots);

		Assertions.assertEquals("INT 1 JAN 1700 (around the new year)", roots.get(1).getChildren().get(0).getChildren().get(0).getValue());
	}

	@Test
	void voidSourceCitationBecomesAnUnrecordedSource() throws IOException{
		final List<GEDCOMNode> roots = normalize("""
			0 HEAD
			0 @I1@ INDI
			1 SOUR @VOID@
			2 PAGE 12
			1 FAMC @VOID@
			""");

		final List<GEDCOMNode> children = roots.get(1).getChildren();
		Assertions.assertEquals(1, children.size());
		Assertions.assertNull(children.get(0).getValue());
		Assertions.assertEquals("SOUR", children.get(0).getTag());
	}

}
