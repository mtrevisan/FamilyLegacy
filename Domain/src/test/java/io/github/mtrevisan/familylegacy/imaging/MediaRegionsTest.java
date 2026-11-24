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
package io.github.mtrevisan.familylegacy.imaging;

import io.github.mtrevisan.familylegacy.imaging.MediaRegions.Region;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.util.List;


/** The regions of a media are the crops that the individuals have on it. */
class MediaRegionsTest{

	private static final String AUDIT = "\taudit.creation.date 2020-01-01T00:00:00Z\n";


	private static String crop(final int x, final int y, final int width, final int height){
		return "\t\tcrop {\n\t\t\tx " + x + "\n\t\t\ty " + y + "\n\t\t\twidth " + width + "\n\t\t\theight " + height + "\n\t\t}\n";
	}

	private static String citation(final String media, final String crop, final String... extra){
		final StringBuilder text = new StringBuilder("\tmedia {\n\t\tmedia ").append(media).append('\n').append(crop);
		for(final String line : extra)
			text.append("\t\t").append(line).append('\n');
		return text.append("\t}\n").toString();
	}

	private static String individual(final String id, final String body){
		return "individual " + id + " {\n\tname.part.type given\n\tname.part.value " + id + "\n" + body + AUDIT + "}\n";
	}

	private static FLEFModel model(final String... individuals){
		return new FLEFParser().parse(String.join("", individuals)
			+ "media M1 {\n\turi photo.jpg\n\tkind image\n" + AUDIT + "}\n"
			+ "media M2 {\n\turi other.jpg\n\tkind image\n" + AUDIT + "}\n");
	}

	private static Region region(final int x, final int y, final int width, final int height, final Integer page, final String... individuals){
		return new Region(new Rectangle(x, y, width, height), page, List.of(individuals));
	}


	@Test
	void theRegionsAreTheCropsOfTheIndividualsOnTheMedia(){
		final FLEFModel model = model(
			individual("I1", citation("M1", crop(10, 20, 30, 40))),
			individual("I2", citation("M1", crop(10, 20, 30, 40)) + citation("M2", crop(1, 1, 5, 5))),
			individual("I3", citation("M1", crop(100, 20, 30, 40))));

		Assertions.assertEquals(List.of(region(10, 20, 30, 40, null, "I1", "I2"), region(100, 20, 30, 40, null, "I3")),
			MediaRegions.of(model, "M1"));
		Assertions.assertEquals(List.of(region(1, 1, 5, 5, null, "I2")), MediaRegions.of(model, "M2"));
	}

	@Test
	void aCitationWithoutCropIsNotARegion(){
		final FLEFModel model = model(individual("I1", "\tmedia {\n\t\tmedia M1\n\t\tcaption the whole photograph\n\t}\n"));

		Assertions.assertTrue(MediaRegions.of(model, "M1").isEmpty());
	}

	@Test
	void anEmptyOrNegativeCropIsNotARegion(){
		final FLEFModel model = model(individual("I1", citation("M1", crop(10, 20, 0, 40)) + citation("M1", crop(-1, 20, 30, 40))));

		Assertions.assertTrue(MediaRegions.of(model, "M1").isEmpty());
	}

	@Test
	void thePreferredImageIsARegionOfItsIndividual(){
		final FLEFModel model = model(
			individual("I1", "\tpreferred_image {\n\t\tmedia M1\n" + crop(10, 20, 30, 40) + "\t}\n"),
			individual("I2", "\tpreferred_image {\n\t\tmedia M2\n" + crop(10, 20, 30, 40) + "\t}\n"),
			individual("I3", "\tpreferred_image {\n\t\tmedia M1\n\t}\n"));

		Assertions.assertEquals(List.of(region(10, 20, 30, 40, null, "I1")), MediaRegions.of(model, "M1"));
	}

	@Test
	void thePreferredImageAndACitationWithTheSameCropAreOneRegion(){
		final FLEFModel model = model(individual("I1", citation("M1", crop(10, 20, 30, 40))
			+ "\tpreferred_image {\n\t\tmedia M1\n" + crop(10, 20, 30, 40) + "\t}\n"));

		Assertions.assertEquals(List.of(region(10, 20, 30, 40, null, "I1")), MediaRegions.of(model, "M1"));
	}

	@Test
	void theSameCropOnAnotherPageIsAnotherRegion(){
		final FLEFModel model = model(
			individual("I1", citation("M1", crop(10, 20, 30, 40))),
			individual("I2", citation("M1", crop(10, 20, 30, 40), "page 1")),
			individual("I3", citation("M1", crop(10, 20, 30, 40), "page 2")));

		Assertions.assertEquals(List.of(region(10, 20, 30, 40, null, "I1", "I2"), region(10, 20, 30, 40, 2, "I3")),
			MediaRegions.of(model, "M1"));
	}

	@Test
	void noMediaNoRegions(){
		final FLEFModel model = model(individual("I1", citation("M1", crop(10, 20, 30, 40))));

		Assertions.assertTrue(MediaRegions.of(model, null).isEmpty());
		Assertions.assertTrue(MediaRegions.of(model, "M9").isEmpty());
	}

	@Test
	void onlyTheRegionsOfAPageAreKept(){
		final Region first = region(0, 0, 10, 10, null, "I1");
		final Region firstExplicit = region(0, 0, 20, 20, 1, "I2");
		final Region second = region(0, 0, 30, 30, 2, "I3");
		final List<Region> regions = List.of(first, firstExplicit, second);

		Assertions.assertEquals(List.of(first, firstExplicit), MediaRegions.onPage(regions, null));
		Assertions.assertEquals(List.of(first, firstExplicit), MediaRegions.onPage(regions, 1));
		Assertions.assertEquals(List.of(second), MediaRegions.onPage(regions, 2));
		Assertions.assertTrue(MediaRegions.onPage(regions, 3).isEmpty());
	}

	@Test
	void theSmallestRegionUnderThePointComesFirst(){
		final Region group = region(0, 0, 1000, 800, null, "I1");
		final Region face = region(400, 400, 100, 100, null, "I2");
		final Region otherFace = region(450, 450, 100, 100, null, "I3");
		final List<Region> regions = List.of(group, face, otherFace);

		Assertions.assertEquals(List.of(face, group), MediaRegions.at(regions, 410, 410, null));
		//in both faces, which have the same size (their order is the one of the regions), and in the group
		Assertions.assertEquals(List.of(face, otherFace, group), MediaRegions.at(regions, 480, 480, null));
		Assertions.assertEquals(List.of(group), MediaRegions.at(regions, 900, 100, null));
		//the sides belong to the region; outside of every region there is nothing
		Assertions.assertEquals(List.of(face, group), MediaRegions.at(regions, 400, 400, null));
		Assertions.assertTrue(MediaRegions.at(regions, 1001, 100, null).isEmpty());
	}

	@Test
	void onlyTheRegionsOfThePageShownAreHit(){
		final Region first = region(0, 0, 100, 100, null, "I1");
		final Region second = region(0, 0, 100, 100, 2, "I2");

		Assertions.assertEquals(List.of(first), MediaRegions.at(List.of(first, second), 50, 50, null));
		Assertions.assertEquals(List.of(second), MediaRegions.at(List.of(first, second), 50, 50, 2));
	}

	@Test
	void aRegionIsThePortraitOfWhoseCropIsTheSameMediaAndRectangle(){
		final FLEFModel model = model(
			individual("I1", "\tpreferred_image {\n\t\tmedia M1\n" + crop(10, 20, 30, 40) + "\t}\n"),
			individual("I2", "\tpreferred_image {\n\t\tmedia M2\n" + crop(10, 20, 30, 40) + "\t}\n"),
			individual("I3", "\tpreferred_image {\n\t\tmedia M1\n" + crop(11, 20, 30, 40) + "\t}\n"),
			individual("I4", "\tpreferred_image {\n\t\tmedia M1\n\t}\n"),
			individual("I5", ""));
		final Region region = region(10, 20, 30, 40, null, "I1");

		Assertions.assertTrue(MediaRegions.isPortraitOf(model.getRecordById("I1"), "M1", region));
		//the other media, another rectangle, no crop (the whole image), no portrait at all, nobody
		Assertions.assertFalse(MediaRegions.isPortraitOf(model.getRecordById("I2"), "M1", region));
		Assertions.assertFalse(MediaRegions.isPortraitOf(model.getRecordById("I3"), "M1", region));
		Assertions.assertFalse(MediaRegions.isPortraitOf(model.getRecordById("I4"), "M1", region));
		Assertions.assertFalse(MediaRegions.isPortraitOf(model.getRecordById("I5"), "M1", region));
		Assertions.assertFalse(MediaRegions.isPortraitOf(null, "M1", region));
		Assertions.assertFalse(MediaRegions.isPortraitOf(model.getRecordById("I1"), null, region));
	}

	@Test
	void aRegionNeedsAnAreaAndAValidPage(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> region(0, 0, 0, 10, null));
		Assertions.assertThrows(IllegalArgumentException.class, () -> region(0, 0, 10, 10, 0));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Region(null, null, List.of()));
	}

	@Test
	void theIndividualsOfARegionHaveNoDuplicatesAndTheAreaIsCopied(){
		final Rectangle area = new Rectangle(1, 2, 3, 4);
		final Region region = new Region(area, null, List.of("I1", "I2", "I1"));
		area.x = 99;

		Assertions.assertEquals(List.of("I1", "I2"), region.individuals());
		Assertions.assertEquals(1, region.area().x);
		Assertions.assertEquals(List.of("I3"), region.withIndividuals(List.of("I3")).individuals());
	}

}
