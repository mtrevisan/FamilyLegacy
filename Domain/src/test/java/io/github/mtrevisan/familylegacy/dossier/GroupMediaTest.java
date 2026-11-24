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

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The dossier of a group has a section for the media that the group cites and has as preferred image. */
class GroupMediaTest{

	private static final String AUDIT = "\taudit.creation.date 2020-01-01T00:00:00Z\n";
	private static final String CROP = "\t\tcrop {\n\t\t\tx 20\n\t\t\ty 10\n\t\t\twidth 40\n\t\t\theight 30\n\t\t}\n";


	/** The Rossi family cites the reunion photograph (cropped, and it is its preferred image) and the house photograph; M3 is not theirs. */
	private static FLEFModel model(){
		return new FLEFParser().parse("media M1 {\n\turi reunion.jpg\n\tkind image\n\tdescription The reunion\n" + AUDIT + "}\n"
			+ "media M2 {\n\turi house.jpg\n\tkind image\n" + AUDIT + "}\n"
			+ "media M3 {\n\turi other.jpg\n\tkind image\n" + AUDIT + "}\n"
			+ "group G1 {\n\tname.value Rossi\n"
			+ "\tmedia {\n\t\tmedia M1\n" + CROP + "\t}\n"
			+ "\tmedia {\n\t\tmedia M2\n\t\tcaption The family house\n\t}\n"
			+ "\tpreferred_image {\n\t\tmedia M1\n" + CROP + "\t}\n" + AUDIT + "}\n"
			+ "group G2 {\n\tname.value Bianchi\n" + AUDIT + "}\n");
	}


	@Test
	void theDossierOfAGroupHasASectionForItsMedia(){
		final GroupDossier dossier = new GroupDossierService(model()).build("G1");

		final List<DossierEntry> media = dossier.entriesOf(GroupDossierSectionType.MEDIA);
		Assertions.assertEquals(2, media.size());
		Assertions.assertTrue(GroupDossierSectionType.SOURCES.ordinal() < GroupDossierSectionType.MEDIA.ordinal()
			&& GroupDossierSectionType.MEDIA.ordinal() < GroupDossierSectionType.CONTEXT.ordinal());

		final DossierEntry reunion = media.getFirst();
		Assertions.assertEquals("image", reunion.label());
		Assertions.assertEquals("The reunion", reunion.value());
		Assertions.assertTrue(reunion.subtitle().contains("portrait"), reunion.subtitle());
		Assertions.assertTrue(reunion.subtitle().contains("reunion.jpg"), reunion.subtitle());
		//(a crop of a group tags nobody: it is not told as a region)
		Assertions.assertFalse(reunion.subtitle().contains("region"), reunion.subtitle());
		//(the row opens the editor of the group on the list of its media, with the citation selected)
		Assertions.assertEquals("G1", reunion.editRecord().getId());
		Assertions.assertEquals(EditorTab.SOURCES, reunion.tab());
		Assertions.assertNotNull(reunion.focus());

		final DossierEntry house = media.get(1);
		Assertions.assertEquals("The family house", house.value());
		Assertions.assertFalse(house.subtitle().contains("portrait"), house.subtitle());
	}

	@Test
	void aGroupWithoutMediaHasNoSection(){
		Assertions.assertTrue(new GroupDossierService(model()).build("G2").entriesOf(GroupDossierSectionType.MEDIA).isEmpty());
	}

	@Test
	void theSectionHasALabel(){
		Assertions.assertEquals("Media", GroupDossierSectionType.MEDIA.getDisplayLabel());
		Assertions.assertEquals("Media", DossierSectionType.MEDIA.getDisplayLabel());
	}

}
