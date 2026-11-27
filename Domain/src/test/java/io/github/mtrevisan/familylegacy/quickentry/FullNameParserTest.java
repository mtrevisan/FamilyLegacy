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
package io.github.mtrevisan.familylegacy.quickentry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.StringJoiner;


/** The proposal of the parser: the parts are written as {@code type:value}, separated by a vertical bar. */
class FullNameParserTest{

	private static String parts(final String fullName){
		final StringJoiner joiner = new StringJoiner("|");
		for(final ParsedName.Part part : FullNameParser.parse(fullName).parts())
			joiner.add(part.type() + ":" + part.value());
		return joiner.toString();
	}


	@Test
	void blankIsNoName(){
		Assertions.assertTrue(FullNameParser.parse(null).isEmpty());
		Assertions.assertTrue(FullNameParser.parse("   ").isEmpty());
		Assertions.assertNull(FullNameParser.parse("").toRecord());
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = ';', quoteCharacter = '~', value = {
		"Giovanni; given:Giovanni",
		"Giovanni Rossi; given:Giovanni|family:Rossi",
		"  Giovanni    Rossi  ; given:Giovanni|family:Rossi",
		"Giovanni Battista Rossi; given:Giovanni Battista|family:Rossi",
		"John E Smith; given:John E|family:Smith",
		"Anna Maria D'Angelo; given:Anna Maria|family:D'Angelo",
		"John O'Brien; given:John|family:O'Brien",
		"Wei Li; given:Wei|family:Li",
	})
	void plainOrder(final String name, final String expected){
		Assertions.assertEquals(expected, parts(name));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = ';', quoteCharacter = '~', value = {
		//Italian, modern and historical
		"Mario De Luca; given:Mario|family_prefix:De|family:Luca",
		"Leonardo da Vinci; given:Leonardo|family_prefix:da|family:Vinci",
		"Antonio dalla Chiesa; given:Antonio|family_prefix:dalla|family:Chiesa",
		"Giovanni de la Casa; given:Giovanni|family_prefix:de la|family:Casa",
		"Pietro de li Rossi; given:Pietro|family_prefix:de li|family:Rossi",
		"Lorenzo de' Medici; given:Lorenzo|family_prefix:de'|family:Medici",
		"Tommaso De’ Medici; given:Tommaso|family_prefix:De’|family:Medici",
		"Paolo dell' Orto; given:Paolo|family_prefix:dell'|family:Orto",
		"Paolo dell'Orto; given:Paolo|family:dell'Orto",
		"Maria Lo Presti; given:Maria|family_prefix:Lo|family:Presti",
		"Maria La Rosa; given:Maria|family_prefix:La|family:Rosa",
		"Carlo delli Santi; given:Carlo|family_prefix:delli|family:Santi",
		//Spanish, Portuguese, Catalan
		"Maria de la Cruz; given:Maria|family_prefix:de la|family:Cruz",
		"Juan de los Santos; given:Juan|family_prefix:de los|family:Santos",
		"Jose Ortega y Gasset; given:Jose|family:Ortega y Gasset",
		"Jordi Pujol i Soley; given:Jordi|family:Pujol i Soley",
		"Joao Teixeira e Silva; given:Joao|family:Teixeira e Silva",
		"Ana dos Santos; given:Ana|family_prefix:dos|family:Santos",
		"Rosa Maria de la Vega y Gasset; given:Rosa Maria|family_prefix:de la|family:Vega y Gasset",
		//French
		"Charles de Gaulle; given:Charles|family_prefix:de|family:Gaulle",
		"Jean du Bois; given:Jean|family_prefix:du|family:Bois",
		"Marie Le Pen; given:Marie|family_prefix:Le|family:Pen",
		"Anne de la Fontaine; given:Anne|family_prefix:de la|family:Fontaine",
		//Dutch, Flemish, Afrikaans
		"Vincent van Gogh; given:Vincent|family_prefix:van|family:Gogh",
		"Jan van der Berg; given:Jan|family_prefix:van der|family:Berg",
		"Pieter van den Broek; given:Pieter|family_prefix:van den|family:Broek",
		"Gerard 't Hooft; given:Gerard|family_prefix:'t|family:Hooft",
		"Jacobus van 't Hoff; given:Jacobus|family_prefix:van 't|family:Hoff",
		"Hendrik op de Beeck; given:Hendrik|family_prefix:op de|family:Beeck",
		//German
		"Ludwig van Beethoven; given:Ludwig|family_prefix:van|family:Beethoven",
		"Ursula von der Leyen; given:Ursula|family_prefix:von der|family:Leyen",
		"Karl von und zu Guttenberg; given:Karl|family_prefix:von und zu|family:Guttenberg",
		"Hans zur Nieden; given:Hans|family_prefix:zur|family:Nieden",
		//Scandinavian, English, Celtic
		"Anders af Ekenstam; given:Anders|family_prefix:af|family:Ekenstam",
		"Thomas atte Wood; given:Thomas|family_prefix:atte|family:Wood",
		"Mary Ni Bhriain; given:Mary|family_prefix:Ni|family:Bhriain",
		"Rhys ap Gruffydd; given:Rhys|family_prefix:ap|family:Gruffydd",
		//Arabic and Hebrew
		"Mohammed bin Salman; given:Mohammed|family_prefix:bin|family:Salman",
		"Omar ibn al-Khattab; given:Omar|family_prefix:ibn|family:al-Khattab",
		"David ben Gurion; given:David|family_prefix:ben|family:Gurion",
	})
	void particles(final String name, final String expected){
		Assertions.assertEquals(expected, parts(name));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = ';', quoteCharacter = '~', value = {
		//a particle that is a name too is a particle only after a given name
		"Al Pacino; given:Al|family:Pacino",
		"Ben Gurion; given:Ben|family:Gurion",
		"Li Wei; given:Li|family:Wei",
		//a connector in capital letters is an initial
		"John E Smith; given:John E|family:Smith",
		//the last word is always the family name
		"Maria De; given:Maria|family:De",
		//no given name: the particles are still the prefix
		"De Luca; family_prefix:De|family:Luca",
		"Van Gogh; family_prefix:Van|family:Gogh",
	})
	void ambiguousParticles(final String name, final String expected){
		Assertions.assertEquals(expected, parts(name));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = ';', quoteCharacter = '~', value = {
		"Rossi, Giovanni; given:Giovanni|family:Rossi",
		"Rossi,Giovanni Maria; given:Giovanni Maria|family:Rossi",
		"De Luca, Mario; given:Mario|family_prefix:De|family:Luca",
		"van der Berg, Jan; given:Jan|family_prefix:van der|family:Berg",
		"Rossi, Giovanni, Jr.; given:Giovanni|family:Rossi|generation:Jr.",
		"Giovanni Rossi, Jr.; given:Giovanni|family:Rossi|generation:Jr.",
		"Rossi,; family:Rossi",
	})
	void invertedOrder(final String name, final String expected){
		Assertions.assertEquals(expected, parts(name));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = ';', quoteCharacter = '~', value = {
		"Giovanni /Rossi/; given:Giovanni|family:Rossi",
		"Giovanni /Rossi; given:Giovanni|family:Rossi",
		"/Rossi/ Giovanni; family:Rossi|given:Giovanni",
		"Maria /de la Cruz/; given:Maria|family_prefix:de la|family:Cruz",
		"Pedro /Calderón de la Barca/; given:Pedro|family:Calderón de la Barca",
		"Giovanni /Rossi/ Jr.; given:Giovanni|family:Rossi|generation:Jr.",
		"Giovanni /Rossi/ Esq; given:Giovanni|family:Rossi|suffix:Esq",
		"Maria Teresa /García Márquez/; given:Maria Teresa|family:García Márquez",
	})
	void markedFamilyName(final String name, final String expected){
		Assertions.assertEquals(expected, parts(name));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = ';', quoteCharacter = '~', value = {
		"ROSSI Giovanni; family:ROSSI|given:Giovanni",
		"ROSSI Giovanni Maria; family:ROSSI|given:Giovanni Maria",
		"DE LA CRUZ Maria; family_prefix:DE LA|family:CRUZ|given:Maria",
		"GIOVANNI ROSSI; given:GIOVANNI|family:ROSSI",
	})
	void capitalFamilyName(final String name, final String expected){
		Assertions.assertEquals(expected, parts(name));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = ';', quoteCharacter = '~', value = {
		"Dr. Mario Rossi; prefix:Dr.|given:Mario|family:Rossi",
		"Prof Giovanni Rossi; prefix:Prof|given:Giovanni|family:Rossi",
		"Dr. Rossi; prefix:Dr.|family:Rossi",
		"Dott. Ing. Mario Rossi; prefix:Dott. Ing.|given:Mario|family:Rossi",
		"Don Giovanni; given:Don|family:Giovanni",
		"Henry VIII; given:Henry|generation:VIII",
		"John Smith Jr.; given:John|family:Smith|generation:Jr.",
		"John Smith III; given:John|family:Smith|generation:III",
		"Giovanni Rossi Sr; given:Giovanni|family:Rossi|generation:Sr",
		"Jr; given:Jr",
	})
	void honorificsAndGenerations(final String name, final String expected){
		Assertions.assertEquals(expected, parts(name));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = ';', quoteCharacter = '~', value = {
		"Giovanni \"Gianni\" Rossi; given:Giovanni|nickname:Gianni|family:Rossi",
		"Giovanni “Gianni” Rossi; given:Giovanni|nickname:Gianni|family:Rossi",
		"Giovanni «Gianni» Rossi; given:Giovanni|nickname:Gianni|family:Rossi",
		"\"Gianni\" Giovanni Rossi; nickname:Gianni|given:Giovanni|family:Rossi",
		"Giovanni Rossi \"Gianni Bello\"; given:Giovanni|family:Rossi|nickname:Gianni Bello",
		//a quote that is not closed is just text
		"Giovanni \"Gianni Rossi; given:Giovanni \"Gianni|family:Rossi",
	})
	void nicknames(final String name, final String expected){
		Assertions.assertEquals(expected, parts(name));
	}

	@Test
	void theFullNameLeavesOutTheNicknameAndTheHonorific(){
		Assertions.assertEquals("Mario Rossi Jr.", FullNameParser.parse("Dr. Mario \"Mao\" Rossi Jr.").fullName());
	}

	@Test
	void theRecordIsAnIndividualNameWithItsPartsInOrder(){
		final var name = FullNameParser.parse("Maria de la Cruz").toRecord();

		Assertions.assertEquals("name", name.getTag());
		Assertions.assertEquals(3, name.getChildren().size());
	}

}
