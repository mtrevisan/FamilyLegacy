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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.CropReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.LivingPrivacy;
import io.github.mtrevisan.reports.ImageAlignment;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
 * Where the media of a report are (the section, the chapter of the person, an appendix, one on each page) and how the images and the
 * portrait of a person are embedded.
 * <p>
 * Maria Rossi (I1) has three pictures in her media (M1, M2, M3: 800 x 600) and her portrait is a region of M1.
 * </p>
 */
class ReportMediaOptionsTest{

	private static ReportConfig everything(){
		return ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW);
	}


	@TempDir
	Path folder;

	private FLEFModel model;
	private Path[] pictures;


	@BeforeEach
	void buildModel() throws IOException{
		pictures = new Path[3];
		model = new FLEFModel();
		final FLEFRecord maria = FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_GIVEN))
					.addChild(FLEFRecord.createChildWithTagAndValue("value", "Maria")))
				.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_FAMILY))
					.addChild(FLEFRecord.createChildWithTagAndValue("value", "Rossi"))));
		for(int i = 0; i < pictures.length; i ++){
			pictures[i] = folder.resolve("M" + (i + 1) + ".png");
			ImageIO.write(new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB), "png", pictures[i].toFile());
			model.addRecord(FLEFRecord.createMainRecord("M" + (i + 1), MediaReader.TYPE)
				.addChild(FLEFRecord.createChildWithTagAndValue(MediaReader.TAG_URI, pictures[i].toString())));
			maria.addChild(FLEFRecord.createChildWithTag(MediaCitationReader.TAG)
				.addChild(FLEFRecord.createChildWithTagAndValue(MediaCitationReader.TAG_MEDIA, "M" + (i + 1))));
		}
		maria.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_PREFERRED_IMAGE)
			.addChild(FLEFRecord.createChildWithTagAndValue(CropReader.TAG_MEDIA, "M1"))
			.addChild(FLEFRecord.createChildWithTag(CropReader.TAG_CROP)
				.addChild(FLEFRecord.createChildWithTagAndValue(CropReader.TAG_X, "100"))
				.addChild(FLEFRecord.createChildWithTagAndValue(CropReader.TAG_Y, "50"))
				.addChild(FLEFRecord.createChildWithTagAndValue(CropReader.TAG_WIDTH, "200"))
				.addChild(FLEFRecord.createChildWithTagAndValue(CropReader.TAG_HEIGHT, "150"))));
		model.addRecord(maria);
	}


	private ReportDocument report(final ReportConfig config){
		return new ReportGenerator(model, model.getRecordById("I1"), config, ReportType.INDIVIDUAL).generate();
	}

	private static List<ReportSection.Image> images(final ReportDocument document){
		final List<ReportSection.Image> out = new ArrayList<>();
		for(final ReportSection section : document.sections())
			if(section instanceof ReportSection.Image image)
				out.add(image);
		return out;
	}

	private static long pageBreaks(final ReportDocument document){
		return document.sections().stream().filter(ReportSection.PageBreak.class::isInstance).count();
	}

	private static List<String> headings(final ReportDocument document, final int level){
		return document.sections().stream()
			.filter(ReportSection.Heading.class::isInstance)
			.map(ReportSection.Heading.class::cast)
			.filter(heading -> heading.level() == level)
			.map(ReportSection.Heading::text)
			.toList();
	}

	private static BufferedImage read(final Path file) throws IOException{
		return ImageIO.read(file.toFile());
	}

	private boolean isTheMedia(final ReportSection.Image image, final int index){
		return image.file().equals(pictures[index]);
	}


	/* ----- Placement ------------------------------------------------------- */

	@Test
	void theMediaAreInASectionByDefault(){
		final ReportDocument document = report(everything());

		Assertions.assertTrue(headings(document, 1).contains("Media"), headings(document, 1).toString());
		Assertions.assertFalse(headings(document, 2).contains("Media"), headings(document, 2).toString());
		//(the portrait, the media)
		Assertions.assertEquals(4, images(document).size());
		Assertions.assertEquals(0, pageBreaks(document));
	}

	@Test
	void theMediaCanBeInTheChapterOfThePerson(){
		final ReportDocument document = report(everything().with(ReportOption.MEDIA_PLACEMENT, MediaPlacement.INLINE));

		Assertions.assertFalse(headings(document, 1).contains("Media"), headings(document, 1).toString());
		Assertions.assertTrue(headings(document, 2).contains("Media"), headings(document, 2).toString());
		Assertions.assertEquals(4, images(document).size());
		//(inside the life of the person: before the index)
		Assertions.assertTrue(headings(document, 1).stream().noneMatch("Index of Individuals"::equals)
			|| document.sections().indexOf(new ReportSection.Heading(2, "Media"))
				< document.sections().indexOf(new ReportSection.Heading(1, "Index of Individuals")));
	}

	@Test
	void theMediaCanBeAnAppendixAfterTheIndexes(){
		final ReportDocument document = report(everything().with(ReportOption.MEDIA_PLACEMENT, MediaPlacement.APPENDIX));
		final List<String> headings = headings(document, 1);

		Assertions.assertTrue(headings.contains("Appendix: Media"), headings.toString());
		Assertions.assertFalse(headings.contains("Media"), headings.toString());
		Assertions.assertTrue(headings.indexOf("Appendix: Media") > headings.indexOf("Index of Individuals"), headings.toString());
		Assertions.assertEquals(4, images(document).size());
	}

	@Test
	void anImageOnEachPageHasAPageBreakBetweenTheImages(){
		final ReportDocument document = report(everything().with(ReportOption.MEDIA_PLACEMENT, MediaPlacement.SEPARATE_PAGES));

		//(three documents: a break between them)
		Assertions.assertEquals(2, pageBreaks(document));
	}

	@Test
	void aNumberOfImagesPerPageBreaksThePageAfterThem(){
		final ReportDocument two = report(everything().with(ReportOption.IMAGES_PER_PAGE, 2));
		final ReportDocument one = report(everything().with(ReportOption.IMAGES_PER_PAGE, 1));
		final ReportDocument four = report(everything().with(ReportOption.IMAGES_PER_PAGE, 4));

		Assertions.assertEquals(1, pageBreaks(two));
		Assertions.assertEquals(2, pageBreaks(one));
		Assertions.assertEquals(0, pageBreaks(four));
	}

	@Test
	void theMediaDoNotAppearWhenTheOptionIsOff(){
		final ReportDocument document = report(everything().with(ReportOption.MEDIA, false).with(ReportOption.MEDIA_PLACEMENT, MediaPlacement.INLINE));

		Assertions.assertFalse(headings(document, 2).contains("Media"), headings(document, 2).toString());
	}


	/* ----- Size of the images ---------------------------------------------- */

	@Test
	void theImagesAreEmbeddedAsTheyAreByDefault(){
		final ReportDocument document = report(everything());

		Assertions.assertTrue(images(document).stream().anyMatch(image -> isTheMedia(image, 1)));
		Assertions.assertTrue(images(document).stream().anyMatch(image -> isTheMedia(image, 2)));
	}

	@Test
	void theImagesOverTheLimitAreReduced() throws IOException{
		//(800 x 600 is half a megapixel: a limit of one leaves it, and the media are the files themselves)
		final ReportDocument unchanged = report(everything().with(ReportOption.MAX_IMAGE_MEGAPIXELS, 1));
		Assertions.assertTrue(images(unchanged).stream().anyMatch(image -> isTheMedia(image, 1)));
		Assertions.assertTrue(images(unchanged).stream().anyMatch(image -> isTheMedia(image, 2)));

		//(the limits are in whole megapixels: a smaller image is the one of the portrait, here a thumbnail)
		final ReportDocument thumbnail = report(everything().with(ReportOption.THUMBNAIL_SIZE, 100));
		final ReportSection.Image portrait = images(thumbnail).getFirst();
		final BufferedImage image = read(portrait.file());
		Assertions.assertEquals(100, Math.max(image.getWidth(), image.getHeight()));
	}


	/* ----- The portrait ---------------------------------------------------- */

	@Test
	void theWholePictureIsThePortraitByDefault(){
		final ReportSection.Image portrait = images(report(everything())).getFirst();

		Assertions.assertTrue(isTheMedia(portrait, 0));
		Assertions.assertTrue(portrait.caption().contains("x=100"), portrait.caption());
	}

	@Test
	void theRegionOfThePortraitCanBeTheImage() throws IOException{
		final ReportDocument document = report(everything().with(ReportOption.PORTRAIT_MODE, PortraitMode.CROP));
		final ReportSection.Image portrait = images(document).getFirst();

		final BufferedImage image = read(portrait.file());
		Assertions.assertEquals(200, image.getWidth());
		Assertions.assertEquals(150, image.getHeight());
		//(the region is the image: nothing to say of it)
		Assertions.assertFalse(portrait.caption().contains("x=100"), portrait.caption());
		//(the picture is still in the media)
		Assertions.assertTrue(images(document).stream().anyMatch(image2 -> isTheMedia(image2, 0)));
	}

	@Test
	void thePictureOfThePortraitCanBeLeftOutOfTheMedia(){
		final ReportDocument document = report(everything().with(ReportOption.PORTRAIT_MODE, PortraitMode.CROP_WITHOUT_ORIGINAL));

		Assertions.assertFalse(images(document).stream().anyMatch(image -> isTheMedia(image, 0)));
		Assertions.assertTrue(images(document).stream().anyMatch(image -> isTheMedia(image, 1)));
		Assertions.assertTrue(images(document).stream().anyMatch(image -> isTheMedia(image, 2)));
		Assertions.assertEquals(3, images(document).size());
	}

	@Test
	void theNameOfThePersonCanBeTheTitleOfThePortrait(){
		final ReportSection.Image portrait = images(report(everything().with(ReportOption.PORTRAIT_NAME_TITLE, true))).getFirst();

		Assertions.assertEquals("Maria Rossi", portrait.caption());
	}

	@Test
	void thePortraitHasThePositionAsked(){
		Assertions.assertEquals(ImageAlignment.DEFAULT, images(report(everything())).getFirst().alignment());
		Assertions.assertEquals(ImageAlignment.WRAP_RIGHT,
			images(report(everything().with(ReportOption.PORTRAIT_ALIGNMENT, ImageAlignment.WRAP_RIGHT))).getFirst().alignment());
	}

	@Test
	void theOriginalIsKeptWhenThereIsNoCrop(){
		model.getRecordById("I1").getChildren().removeIf(child -> IndividualReader.TAG_PREFERRED_IMAGE.equals(child.getTag()));
		model.getRecordById("I1").addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_PREFERRED_IMAGE)
			.addChild(FLEFRecord.createChildWithTagAndValue(CropReader.TAG_MEDIA, "M1")));

		final ReportDocument document = report(everything().with(ReportOption.PORTRAIT_MODE, PortraitMode.CROP_WITHOUT_ORIGINAL));

		Assertions.assertTrue(images(document).stream().anyMatch(image -> isTheMedia(image, 0)));
		Assertions.assertEquals(4, images(document).size());
	}

}
