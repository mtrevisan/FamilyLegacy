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
package io.github.mtrevisan.familylegacy.ui.components.media;

import io.github.mtrevisan.familylegacy.imaging.MediaRegions;
import io.github.mtrevisan.familylegacy.imaging.MediaRegions.Region;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BooleanSupplier;


/** The viewer reads the image of a media and tells, in the tooltip, who is in the regions under the mouse. */
class MediaViewerDialogTest{

	private static final String AUDIT = "\taudit.creation.date 2020-01-01T00:00:00Z\n";
	private static final String FACE = "\t\tcrop {\n\t\t\tx 20\n\t\t\ty 10\n\t\t\twidth 40\n\t\t\theight 30\n\t\t}\n";
	private static final String GROUP = "\t\tcrop {\n\t\t\tx 0\n\t\t\ty 0\n\t\t\twidth 200\n\t\t\theight 100\n\t\t}\n";

	@TempDir
	Path directory;

	private MediaViewerDialog dialog;


	@BeforeEach
	void setUp(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
	}

	@AfterEach
	void tearDown(){
		if(dialog != null)
			dialog.dispose();
	}


	/** Anna has the face as preferred image and cites the media with it; Bruno cites the face too, and the group. */
	private FLEFModel model(final String uri) throws IOException{
		final File file = directory.resolve("photo.png").toFile();
		ImageIO.write(new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB), "png", file);

		return new FLEFParser().parse("individual I1 {\n\tname.part.type given\n\tname.part.value Anna & <Co>\n"
			+ "\tpreferred_image {\n\t\tmedia M1\n" + FACE + "\t}\n"
			+ "\tmedia {\n\t\tmedia M1\n" + FACE + "\t}\n" + AUDIT + "}\n"
			+ "individual I2 {\n\tname.part.type given\n\tname.part.value Bruno\n"
			+ "\tmedia {\n\t\tmedia M1\n" + FACE + "\t}\n"
			+ "\tmedia {\n\t\tmedia M1\n" + GROUP + "\t}\n" + AUDIT + "}\n"
			+ "media M1 {\n\turi " + uri + "\n\tkind image\n" + AUDIT + "}\n");
	}

	private MediaViewerDialog viewer(final FLEFModel model){
		dialog = new MediaViewerDialog(null, model, model.getRecordById("M1"));
		return dialog;
	}


	@Test
	void theImageIsRead() throws IOException{
		final FLEFModel model = model(directory.resolve("photo.png").toString());

		Assertions.assertDoesNotThrow(() -> viewer(model).load());
		Assertions.assertTrue(dialog.getTitle().contains("photo.png"), dialog.getTitle());
	}

	@Test
	void aMissingFileIsAnError() throws IOException{
		final FLEFModel model = model(directory.resolve("missing.png").toString());

		Assertions.assertThrows(IOException.class, () -> viewer(model).load());
	}

	@Test
	void aMediaWithoutUriIsAnErrorToo() throws IOException{
		final FLEFModel model = model("x");
		final FLEFRecord media = model.getRecordById("M1");
		media.removeChild(media.getChildren().getFirst());

		Assertions.assertThrows(IOException.class, () -> viewer(model).load());
	}

	@Test
	void theTooltipNamesThePeopleAndMarksThePortraits() throws IOException{
		final FLEFModel model = model(directory.resolve("photo.png").toString());
		viewer(model).load();
		final List<Region> regions = MediaRegions.of(model, "M1");
		Assertions.assertEquals(2, regions.size());

		final String text = dialog.tooltipFor(MediaRegions.at(regions, 30, 20, null));

		Assertions.assertTrue(text.startsWith("<html>") && text.endsWith("</html>"), text);
		//(the portrait is only of the person whose preferred image it is, the names are escaped for the HTML of the tooltip)
		Assertions.assertTrue(text.contains("Anna &amp; &lt;Co&gt;"), text);
		Assertions.assertEquals(1, StringUtils.countMatches(text, "(portrait)"), text);
		Assertions.assertTrue(text.indexOf("(portrait)") < text.indexOf("Bruno"), "the portrait is Anna's, that comes first: " + text);
		Assertions.assertTrue(text.contains("Bruno"), text);
		Assertions.assertFalse(text.contains("<Co>"), text);
		//(the smallest region first: the face, then the group, where Bruno is again)
		Assertions.assertEquals(2, StringUtils.countMatches(text, "Bruno"), text);
	}

	/** Anna cites a face on page 2, Bruno a face and the group on page 3, of a document of three pages. */
	private FLEFModel pagedModel(final String uri){
		return new FLEFParser().parse("individual I1 {\n\tname.part.type given\n\tname.part.value Anna\n"
			+ "\tmedia {\n\t\tmedia M1\n\t\tpage 2\n" + FACE + "\t}\n" + AUDIT + "}\n"
			+ "individual I2 {\n\tname.part.type given\n\tname.part.value Bruno\n"
			+ "\tmedia {\n\t\tmedia M1\n\t\tpage 3\n" + FACE + "\t}\n"
			+ "\tmedia {\n\t\tmedia M1\n\t\tpage 3\n" + GROUP + "\t}\n" + AUDIT + "}\n"
			+ "media M1 {\n\turi " + uri + "\n\tkind document\n" + AUDIT + "}\n");
	}

	/** Waits (the pages are rendered away from the event dispatch thread) until the condition holds. */
	private static void waitFor(final BooleanSupplier condition) throws Exception{
		final long limit = System.currentTimeMillis() + 15_000;
		while(!condition.getAsBoolean() && System.currentTimeMillis() < limit){
			SwingUtilities.invokeAndWait(() -> {});
			Thread.sleep(20);
		}
		Assertions.assertTrue(condition.getAsBoolean(), "the page was not shown in time");
	}

	private void browse(final File document) throws Exception{
		viewer(pagedModel(document.getAbsolutePath())).load();

		Assertions.assertEquals(1, dialog.currentPage());
		Assertions.assertEquals(0, dialog.regionCount(), "nobody is tagged on the first page");

		dialog.showPage(2);
		Assertions.assertEquals(2, dialog.currentPage());
		waitFor(() -> dialog.regionCount() == 1);

		dialog.showPage(1);
		//(a page that was seen is not rendered again)
		Assertions.assertEquals(0, dialog.regionCount());
		Assertions.assertEquals(1, dialog.currentPage());
	}


	@Test
	void aPdfIsBrowsedPageByPageEachWithItsOwnRegions() throws Exception{
		final File file = directory.resolve("book.pdf").toFile();
		try(final PDDocument document = new PDDocument()){
			document.addPage(new PDPage(PDRectangle.A6));
			document.addPage(new PDPage(PDRectangle.A5));
			document.addPage(new PDPage(PDRectangle.A4));
			document.save(file);
		}

		browse(file);

		Assertions.assertEquals(3, dialog.pageCount());
		dialog.showPage(3);
		waitFor(() -> dialog.regionCount() == 2);
		//(a page that does not exist is ignored)
		dialog.showPage(4);
		dialog.showPage(0);
		Assertions.assertEquals(3, dialog.currentPage());
	}

	@Test
	void aMultiPageTiffIsBrowsedToo() throws Exception{
		final File file = directory.resolve("scan.tiff").toFile();
		final ImageWriter writer = ImageIO.getImageWritersByFormatName("tiff").next();
		try(final ImageOutputStream stream = ImageIO.createImageOutputStream(file)){
			writer.setOutput(stream);
			writer.prepareWriteSequence(null);
			for(int page = 0; page < 3; page ++)
				writer.writeToSequence(new IIOImage(new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB), null, null), null);
			writer.endWriteSequence();
		}
		finally{
			writer.dispose();
		}

		browse(file);

		Assertions.assertEquals(3, dialog.pageCount());
	}

	@Test
	void anImageHasOnePage() throws IOException{
		final FLEFModel model = model(directory.resolve("photo.png").toString());
		viewer(model).load();

		Assertions.assertEquals(1, dialog.pageCount());
		dialog.showPage(2);
		Assertions.assertEquals(1, dialog.currentPage());
		Assertions.assertEquals(2, dialog.regionCount());
	}

	@Test
	void aDocumentThatCannotBeReadIsAnError() throws IOException{
		final File broken = directory.resolve("broken.pdf").toFile();
		java.nio.file.Files.writeString(broken.toPath(), "not a pdf");

		Assertions.assertThrows(IOException.class, () -> viewer(pagedModel(broken.getAbsolutePath())).load());
	}
}
