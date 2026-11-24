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

import io.github.mtrevisan.familylegacy.imaging.MediaRegions;
import io.github.mtrevisan.familylegacy.imaging.MediaRegions.IndividualMedia;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.CropReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.sources.SourceHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.util.List;
import java.util.Map;


/** The media of an individual: the ones it cites and has as preferred image, where it is tagged, in the dossier and in the media table. */
class IndividualMediaTest{

	private static FLEFRecord crop(final int x, final int y, final int width, final int height){
		return FLEFRecord.createChildWithTag(CropReader.TAG_CROP)
			.addChild(FLEFRecord.createChildWithTagAndValue(CropReader.TAG_X, String.valueOf(x)))
			.addChild(FLEFRecord.createChildWithTagAndValue(CropReader.TAG_Y, String.valueOf(y)))
			.addChild(FLEFRecord.createChildWithTagAndValue(CropReader.TAG_WIDTH, String.valueOf(width)))
			.addChild(FLEFRecord.createChildWithTagAndValue(CropReader.TAG_HEIGHT, String.valueOf(height)));
	}

	private static FLEFRecord citation(final String mediaId, final String caption, final FLEFRecord crop){
		final FLEFRecord citation = FLEFRecord.createChildWithTag(MediaCitationReader.TAG)
			.addChild(FLEFRecord.createChildWithTagAndValue(MediaCitationReader.TAG_MEDIA, mediaId));
		if(caption != null)
			citation.addChild(FLEFRecord.createChildWithTagAndValue(MediaCitationReader.TAG_CAPTION, caption));
		if(crop != null)
			citation.addChild(crop);
		return citation;
	}

	private static FLEFRecord preferred(final String mediaId, final FLEFRecord crop){
		final FLEFRecord preferred = FLEFRecord.createChildWithTag(IndividualReader.TAG_PREFERRED_IMAGE)
			.addChild(FLEFRecord.createChildWithTagAndValue(CropReader.TAG_MEDIA, mediaId));
		if(crop != null)
			preferred.addChild(crop);
		return preferred;
	}

	private static FLEFRecord media(final String id, final String uri, final String description){
		final FLEFRecord media = FLEFRecord.createMainRecord(id, MediaReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(MediaReader.TAG_URI, uri))
			.addChild(FLEFRecord.createChildWithTagAndValue(MediaReader.TAG_KIND, MediaReader.ENUM_KIND_IMAGE));
		if(description != null)
			media.addChild(FLEFRecord.createChildWithTagAndValue(MediaReader.TAG_DESCRIPTION, description));
		return media;
	}

	/**
	 * Anna cites the wedding photograph with her face (it is also her portrait) and the whole house photograph, and has the passport photograph
	 * as preferred image; Bruno is tagged in the wedding photograph too.
	 */
	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(media("M1", "wedding.jpg", "The wedding"));
		model.addRecord(media("M2", "house.jpg", null));
		model.addRecord(media("M3", "passport.jpg", null));
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(citation("M1", null, crop(10, 20, 30, 40)))
			.addChild(citation("M1", "second face", crop(100, 20, 30, 40)))
			.addChild(citation("M2", "Childhood home", null))
			.addChild(preferred("M1", crop(10, 20, 30, 40))));
		model.addRecord(FLEFRecord.createMainRecord("I2", IndividualReader.TYPE)
			.addChild(citation("M1", null, crop(200, 20, 30, 40)))
			.addChild(preferred("M3", null)));
		model.addRecord(FLEFRecord.createMainRecord("I3", IndividualReader.TYPE));
		return model;
	}


	@Test
	void theMediaOfAnIndividualAreItsCitationsAndItsPreferredImage(){
		final List<IndividualMedia> media = MediaRegions.mediaOf(model().getRecordById("I1"));

		Assertions.assertEquals(List.of("M1", "M2"), media.stream().map(IndividualMedia::mediaId).toList());
		final IndividualMedia wedding = media.getFirst();
		//(the face is cited and is the preferred image: the same region, told once; the second face is another one)
		Assertions.assertEquals(List.of(new Rectangle(10, 20, 30, 40), new Rectangle(100, 20, 30, 40)),
			wedding.regions().stream().map(region -> region.area()).toList());
		Assertions.assertEquals(List.of("I1"), wedding.regions().getFirst().individuals());
		Assertions.assertTrue(wedding.isTagged());
		Assertions.assertTrue(wedding.portrait());
		Assertions.assertNotNull(wedding.citation());
		//(a media that is cited with no crop is the individual's, but it is not tagged)
		Assertions.assertFalse(media.get(1).isTagged());
		Assertions.assertFalse(media.get(1).portrait());
	}

	@Test
	void aPreferredImageThatIsNotCitedIsAMediaToo(){
		final List<IndividualMedia> media = MediaRegions.mediaOf(model().getRecordById("I2"));

		Assertions.assertEquals(List.of("M1", "M3"), media.stream().map(IndividualMedia::mediaId).toList());
		final IndividualMedia passport = media.get(1);
		Assertions.assertNull(passport.citation());
		Assertions.assertTrue(passport.portrait());
		Assertions.assertFalse(passport.isTagged(), "the whole image");
	}

	@Test
	void noMediaNothing(){
		Assertions.assertTrue(MediaRegions.mediaOf(model().getRecordById("I3")).isEmpty());
		Assertions.assertTrue(MediaRegions.mediaOf(null).isEmpty());
	}

	@Test
	void theTaggedPeopleAreCountedForEachMedia(){
		final Map<String, Integer> tagged = MediaRegions.taggedPeople(model());

		Assertions.assertEquals(2, tagged.get("M1"), "Anna and Bruno");
		Assertions.assertNull(tagged.get("M2"), "cited with no crop");
		Assertions.assertNull(tagged.get("M3"), "preferred image of the whole image");
	}

	@Test
	void theMediaTableTellsHowManyPeopleAreTagged(){
		final FLEFModel model = model();
		final Map<String, Integer> references = SourceHelper.countDocumentReferences(model);
		final Map<String, Integer> tagged = MediaRegions.taggedPeople(model);

		Assertions.assertEquals(2, SourceHelper.toDocumentRow(model.getRecordById("M1"), references, tagged).taggedCount());
		Assertions.assertEquals(0, SourceHelper.toDocumentRow(model.getRecordById("M2"), references, tagged).taggedCount());
		Assertions.assertEquals(0, SourceHelper.toDocumentRow(model.getRecordById("M1"), references).taggedCount());
	}

	@Test
	void theDossierHasASectionForTheMediaAfterTheSources(){
		final IndividualDossier dossier = new IndividualDossierService(model()).build("I1");

		final List<DossierEntry> media = dossier.entriesOf(DossierSectionType.MEDIA);
		Assertions.assertEquals(2, media.size());
		Assertions.assertTrue(DossierSectionType.SOURCES.ordinal() < DossierSectionType.MEDIA.ordinal()
			&& DossierSectionType.MEDIA.ordinal() < DossierSectionType.CONTEXT.ordinal());

		final DossierEntry wedding = media.getFirst();
		Assertions.assertEquals("image", wedding.label());
		Assertions.assertEquals("The wedding", wedding.value());
		Assertions.assertTrue(wedding.subtitle().contains("2 regions"), wedding.subtitle());
		Assertions.assertTrue(wedding.subtitle().contains("portrait"), wedding.subtitle());
		Assertions.assertTrue(wedding.subtitle().contains("wedding.jpg"), wedding.subtitle());
		//(the row opens the editor of the individual on the list of its media, with the citation selected)
		Assertions.assertEquals("I1", wedding.editRecord().getId());
		Assertions.assertEquals(EditorTab.SOURCES, wedding.tab());
		Assertions.assertSame(MediaRegions.mediaOf(dossier.individual()).getFirst().citation(), wedding.focus());

		final DossierEntry house = media.get(1);
		Assertions.assertEquals("Childhood home", house.value());
		Assertions.assertFalse(house.subtitle().contains("region"), house.subtitle());
	}

	@Test
	void aPreferredImageThatIsNotCitedOpensTheProperties(){
		final List<DossierEntry> media = new IndividualDossierService(model()).build("I2").entriesOf(DossierSectionType.MEDIA);

		Assertions.assertEquals(2, media.size());
		Assertions.assertEquals(EditorTab.PROPERTIES, media.get(1).tab());
		Assertions.assertNull(media.get(1).focus());
		Assertions.assertTrue(media.get(1).subtitle().contains("portrait"), media.get(1).subtitle());
	}

	@Test
	void aMediaThatDoesNotExistIsLeftOut(){
		final FLEFModel model = model();
		model.getRecordById("I3").addChild(citation("M9", "gone", null));

		Assertions.assertTrue(new IndividualDossierService(model).build("I3").entriesOf(DossierSectionType.MEDIA).isEmpty());
	}

}
