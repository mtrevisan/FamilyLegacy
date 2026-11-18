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
package io.github.mtrevisan.familylegacy.dossier;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


/** A row of the dossier tells the tab of the editor it belongs to, and the item to focus on. */
class DossierEntryFocusTest{

	private FLEFModel model;
	private FLEFRecord sex;


	@BeforeEach
	void setUp(){
		model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		sex = FLEFRecord.createMainRecord("IA1", IndividualAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, "I1"))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_SEX))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, "female"));
		model.addRecord(sex);
	}


	@Test
	void anEntryWithoutFocusOpensTheEditorAsItIs(){
		final DossierEntry entry = DossierEntry.of("label", "value", sex);

		Assertions.assertNull(entry.tab());
		Assertions.assertNull(entry.focus());
	}

	@Test
	void theFocusIsKeptByTheEntry(){
		final DossierEntry entry = DossierEntry.of("label", "value", sex)
			.withFocus(EditorTab.NOTES, sex);

		Assertions.assertEquals(EditorTab.NOTES, entry.tab());
		Assertions.assertSame(sex, entry.focus());
		Assertions.assertSame(sex, entry.editRecord());
	}

	@Test
	void theSexRowIsInTheAttributesSectionAndOpensTheSexAttribute(){
		final IndividualDossier dossier = new IndividualDossierService(model).build("I1");

		//sex is an attribute: it is not in the identity section
		Assertions.assertTrue(dossier.entriesOf(DossierSectionType.IDENTITY).stream()
			.noneMatch(entry -> "female".equals(entry.value())));

		final List<DossierEntry> attributes = dossier.entriesOf(DossierSectionType.ATTRIBUTES);
		final DossierEntry row = attributes.stream()
			.filter(entry -> "female".equals(entry.value()))
			.findFirst()
			.orElseThrow();
		Assertions.assertEquals("IA1", row.editRecord().getId());
	}

}
