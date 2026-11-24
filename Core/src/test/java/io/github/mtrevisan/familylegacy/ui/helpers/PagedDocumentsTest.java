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
package io.github.mtrevisan.familylegacy.ui.helpers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;


/** The pages of a PDF and of a multi-page TIFF are read as images, and told how many there are. */
class PagedDocumentsTest{

	@TempDir
	Path directory;


	/** A PDF of three pages: A6 (105 x 148 mm), A5 and A4. */
	static File pdf(final Path directory) throws IOException{
		final File file = directory.resolve("book.pdf").toFile();
		try(final PDDocument document = new PDDocument()){
			document.addPage(new PDPage(PDRectangle.A6));
			document.addPage(new PDPage(PDRectangle.A5));
			document.addPage(new PDPage(PDRectangle.A4));
			document.save(file);
		}
		return file;
	}

	/** A TIFF of two pages: 40 x 30 and 20 x 10 pixels. */
	static File tiff(final Path directory) throws IOException{
		final File file = directory.resolve("scan.tiff").toFile();
		final Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("tiff");
		Assertions.assertTrue(writers.hasNext(), "no TIFF writer");
		final ImageWriter writer = writers.next();
		try(final ImageOutputStream stream = ImageIO.createImageOutputStream(file)){
			writer.setOutput(stream);
			writer.prepareWriteSequence(null);
			writer.writeToSequence(new IIOImage(new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB), null, null), null);
			writer.writeToSequence(new IIOImage(new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB), null, null), null);
			writer.endWriteSequence();
		}
		finally{
			writer.dispose();
		}
		return file;
	}


	@Test
	void theDocumentsThatHavePagesAreRecognized(){
		Assertions.assertTrue(PagedDocuments.isPaged("book.pdf"));
		Assertions.assertTrue(PagedDocuments.isPaged("C:\\scans\\SCAN.TIF"));
		Assertions.assertTrue(PagedDocuments.isPaged("scan.tiff"));
		Assertions.assertFalse(PagedDocuments.isPaged("photo.jpg"));
		Assertions.assertFalse(PagedDocuments.isPaged(null));
	}

	@Test
	void thePagesOfAPdfAreRenderedAtTheirOwnSize() throws IOException{
		final File file = pdf(directory);

		final PagedDocuments.Page first = PagedDocuments.render(file, 1);
		final PagedDocuments.Page third = PagedDocuments.render(file, 3);

		Assertions.assertEquals(3, first.pageCount());
		//(A6 is 105 x 148 mm, at 150 dpi 620 x 874 pixels; A4 is 1240 x 1754)
		Assertions.assertEquals(620, first.image().getWidth(), 2);
		Assertions.assertEquals(874, first.image().getHeight(), 2);
		Assertions.assertEquals(1240, third.image().getWidth(), 2);
	}

	@Test
	void thePagesOfATiffAreRead() throws IOException{
		final File file = tiff(directory);

		final PagedDocuments.Page first = PagedDocuments.render(file, 1);
		final PagedDocuments.Page second = PagedDocuments.render(file, 2);

		Assertions.assertEquals(2, first.pageCount());
		Assertions.assertEquals(40, first.image().getWidth());
		Assertions.assertEquals(20, second.image().getWidth());
	}

	@Test
	void aPageThatDoesNotExistHasNoImageButTheCountIsKnown() throws IOException{
		final PagedDocuments.Page pdf = PagedDocuments.render(pdf(directory), 4);
		final PagedDocuments.Page tiff = PagedDocuments.render(tiff(directory), 0);

		Assertions.assertNull(pdf.image());
		Assertions.assertEquals(3, pdf.pageCount());
		Assertions.assertNull(tiff.image());
		Assertions.assertEquals(2, tiff.pageCount());
	}

	@Test
	void aMissingOrBrokenFileIsNothing() throws IOException{
		Assertions.assertNull(PagedDocuments.render(directory.resolve("missing.pdf").toFile(), 1));
		Assertions.assertNull(PagedDocuments.render(null, 1));
		final File broken = directory.resolve("broken.pdf").toFile();
		java.nio.file.Files.writeString(broken.toPath(), "not a pdf");
		Assertions.assertNull(PagedDocuments.render(broken, 1));
	}

}
