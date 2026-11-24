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

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Iterator;
import java.util.Locale;


/**
 * The documents that have pages (a PDF, a multi-page TIFF), and a page of one of them as an image. The crops and the regions of a page are in the
 * pixels of the image that is made here (a PDF is rendered at {@link #PDF_DPI}), so every place that shows a page, to crop it or to look at it,
 * must get its image from here.
 */
public final class PagedDocuments{

	private static final Logger LOGGER = LoggerFactory.getLogger(PagedDocuments.class);

	public static final String EXTENSION_PDF = ResourceHelper.EXTENSION_PDF;
	private static final String EXTENSION_TIF = ".tif";
	private static final String EXTENSION_TIFF = ".tiff";

	/** The resolution at which the pages of a PDF are rendered. */
	public static final int PDF_DPI = 150;


	/**
	 * A page of a document.
	 *
	 * @param image     the page ({@code null} if the page does not exist or cannot be read)
	 * @param pageCount how many pages the document has
	 */
	public record Page(BufferedImage image, int pageCount){}


	private PagedDocuments(){}


	/** @return whether the file (a URI or a path) is a document with pages: a PDF or a TIFF */
	public static boolean isPaged(final String uri){
		if(uri == null)
			return false;

		final String lower = uri.toLowerCase(Locale.ROOT);
		return (lower.endsWith(EXTENSION_PDF) || lower.endsWith(EXTENSION_TIF) || lower.endsWith(EXTENSION_TIFF));
	}

	/**
	 * Renders a page of a document: Apache PDFBox for a PDF, ImageIO (with TwelveMonkeys) for a TIFF.
	 *
	 * @param file the file
	 * @param page the 1-based page
	 * @return the page, with the number of pages of the document; {@code null} if the file does not exist or is not a readable document
	 */
	public static Page render(final File file, final int page){
		if(file == null || !file.exists())
			return null;

		return (file.getName().toLowerCase(Locale.ROOT).endsWith(EXTENSION_PDF)? renderPdf(file, page): renderImage(file, page));
	}


	private static Page renderPdf(final File file, final int page){
		try(final PDDocument document = Loader.loadPDF(file)){
			final int totalPages = document.getNumberOfPages();
			if(page < 1 || page > totalPages)
				return new Page(null, totalPages);

			final Image rendered = new PDFRenderer(document).renderImageWithDPI(page - 1, PDF_DPI, ImageType.RGB);
			if(rendered instanceof BufferedImage buffered)
				return new Page(buffered, totalPages);

			final BufferedImage copy = new BufferedImage(rendered.getWidth(null), rendered.getHeight(null), BufferedImage.TYPE_INT_RGB);
			final Graphics2D g = copy.createGraphics();
			try{
				g.drawImage(rendered, 0, 0, null);
			}
			finally{
				g.dispose();
			}
			return new Page(copy, totalPages);
		}
		catch(final IOException ioe){
			//(not worth telling if the reading was only interrupted by a quick change of page)
			if(!(ioe instanceof ClosedChannelException))
				LOGGER.error("Error reading PDF: {}", ioe.getMessage());
			return null;
		}
	}

	private static Page renderImage(final File file, final int page){
		try(final ImageInputStream stream = ImageIO.createImageInputStream(file)){
			if(stream == null)
				return null;

			final Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
			if(!readers.hasNext())
				return null;

			final ImageReader reader = readers.next();
			try{
				reader.setInput(stream, true, true);
				final int totalPages = reader.getNumImages(true);
				return new Page((page >= 1 && page <= totalPages? reader.read(page - 1): null), totalPages);
			}
			finally{
				reader.dispose();
			}
		}
		catch(final IOException ioe){
			LOGGER.error("Error reading image file: {}", ioe.getMessage());
			return null;
		}
	}

}
