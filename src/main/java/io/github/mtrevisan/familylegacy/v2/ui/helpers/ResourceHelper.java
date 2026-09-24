/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.v2.ui.helpers;

import net.coobird.thumbnailator.Thumbnails;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;


public final class ResourceHelper{

	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceHelper.class);


	private ResourceHelper(){}


	/**
	 * Loads an image from the classpath or, when not found there, from the
	 * file system, applies an optional crop, and returns it as an
	 * {@link ImageIcon}.
	 * <p>
	 * Works for any format supported by {@link ImageIO} (raster and, when
	 * {@code imageio-batik} is on the classpath, vector formats such as SVG)
	 * and falls back to rendering the first page of a PDF document.
	 *
	 * @param filename the resource name (classpath) or path (filesystem)
	 * @param crop     the crop rectangle in image coordinates; may be {@code null}
	 * @return the loaded image, or {@code null} on failure
	 */
	public static ImageIcon getCroppedImageFromResource(final String filename, final Rectangle crop){
		final Object source = resolveSource(filename);
		if(source == null){
			LOGGER.error("Resource not found: `{}`", filename);

			return null;
		}

		final BufferedImage original;
		try{
			original = readBufferedImage(source);
		}
		catch(final IOException ex){
			LOGGER.error("Unable to read image `{}`", filename, ex);

			return null;
		}
		if(original == null){
			LOGGER.error("No reader for image `{}`. Registered suffixes: {}",
				filename, Arrays.toString(ImageIO.getReaderFileSuffixes()));

			return null;
		}

		// Normalize before returning: the ImageIcon created below is a Swing
		// wrapper, but the BufferedImage underneath may also be used by
		// callers that need ARGB (crop, further resize, compositing). See
		// toARGB for why the normalization is required.
		final BufferedImage normalized = toARGB(original);
		final BufferedImage result = crop(normalized, crop);
		return (result != null? new ImageIcon(result): null);
	}


	public static ImageIcon getCroppedImage(final String filename, final Rectangle crop) throws IOException{
		BufferedImage image = ResourceHelper.readBufferedImage(new File(filename));
		if(image != null){
			image = toARGB(image);
			if(crop != null){
				final Rectangle bounds = new Rectangle(0, 0, image.getWidth(), image.getHeight());
				final Rectangle clamped = bounds.intersection(crop);
				image = image.getSubimage(clamped.x, clamped.y, clamped.width, clamped.height);
			}
			return new ImageIcon(image);
		}
		return null;
	}

	public static ImageIcon getImageFromResource(final String filename){
		return getCroppedImageFromResource(filename, null);
	}

	public static ImageIcon getResizedImageFromResource(final String filename, final Dimension newDimension){
		final ImageIcon croppedImage = getCroppedImageFromResource(filename, null);
		return resizeForceEvenHeight(croppedImage, newDimension.width, newDimension.height);
	}

	public static ImageIcon getResizedImageFromResource(final String filename, final int width, final int height){
		final ImageIcon croppedImage = getCroppedImageFromResource(filename, null);
		return resize(croppedImage, width, height);
	}

	public static ImageIcon getCroppedResizedImageFromResource(final String filename, final Rectangle crop,
		final int width, final int height){
		final ImageIcon croppedImage = getCroppedImageFromResource(filename, crop);
		if(croppedImage == null){
			LOGGER.error("Non-existent image for {}", filename);

			return null;
		}

		return resize(croppedImage, width, height);
	}

	public static ImageIcon getImageFixedHeightFromResource(final String filename, final int height){
		final ImageIcon croppedImage = getCroppedImageFromResource(filename, null);
		return resizeFixedHeight(croppedImage, height);
	}


	/* ======================================================================
	 *                          Resize
	 * ====================================================================== */

	public static ImageIcon resize(final ImageIcon icon, final Dimension newDimension){
		return resize(icon, newDimension.width, newDimension.height);
	}

	public static ImageIcon resize(final ImageIcon icon, final int width, final int height){
		if(icon == null)
			return null;

		try{
			final BufferedImage original = toBufferedImage(icon);
			final BufferedImage scaled = resize(original, width, height, false);
			return (scaled != null? new ImageIcon(scaled): null);
		}
		catch(final Exception e){
			LOGGER.error(null, e);
		}
		return null;
	}

	public static ImageIcon resizeForceEvenHeight(final ImageIcon icon, final int width, final int height){
		if(icon == null)
			return null;

		try{
			final BufferedImage original = toBufferedImage(icon);
			final BufferedImage scaled = resize(original, width, height, true);
			return (scaled != null? new ImageIcon(scaled): null);
		}
		catch(final Exception e){
			LOGGER.error(null, e);
		}
		return null;
	}

	private static ImageIcon resizeFixedHeight(final ImageIcon icon, final int height){
		if(icon == null)
			return null;

		try{
			final BufferedImage original = toBufferedImage(icon);
			final BufferedImage scaled = Thumbnails.of(toARGB(original))
				.height(height)
				.asBufferedImage();

			return new ImageIcon(toARGB(scaled));
		}
		catch(final Exception e){
			LOGGER.error(null, e);
		}
		return null;
	}

	/**
	 * Resizes the given image keeping the aspect ratio, optionally forcing
	 * an even height, and always returns an ARGB image.
	 * <p>
	 * The input is normalized to {@link BufferedImage#TYPE_INT_ARGB} before
	 * the resize: PNG icons with a single colour and transparency are often
	 * decoded as palette images, and a resize that ignores the palette
	 * would collapse the transparent pixels to opaque black, turning the
	 * whole icon into a black square.
	 *
	 * @param image      the image to resize; may be {@code null}
	 * @param width      the target width
	 * @param height     the target height
	 * @param evenHeight when {@code true}, forces the resulting height to
	 *                   be even
	 * @return the resized ARGB image, or {@code null} on failure
	 * @throws IOException if the resize fails
	 */
	public static BufferedImage resize(final BufferedImage image, final int width, final int height,
			final boolean evenHeight) throws IOException{
		if(image == null)
			return null;

		BufferedImage scaled = Thumbnails.of(toARGB(image))
			.size(width, height)
			.keepAspectRatio(true)
			.asBufferedImage();

		if(evenHeight && (scaled.getHeight() & 1) != 0)
			scaled = Thumbnails.of(scaled)
				.forceSize(scaled.getWidth(), scaled.getHeight() + 1)
				.asBufferedImage();

		// Thumbnailator can return a palette image too; normalise the
		// output as well so the caller always gets ARGB back.
		return toARGB(scaled);
	}


	/* ======================================================================
	 *                          Crop
	 * ====================================================================== */

	/**
	 * Clamps the requested crop to the image bounds and returns the
	 * resulting sub-image. Returns {@code null} when the crop does not
	 * intersect the image at all.
	 *
	 * @param image the source image; may be {@code null}
	 * @param crop  the requested crop; {@code null} returns the image unchanged
	 * @return the cropped image, or {@code null} when the crop is outside
	 *         the image bounds
	 */
	public static BufferedImage crop(final BufferedImage image, final Rectangle crop){
		if(image == null)
			return null;

		if(crop == null)
			return image;

		final Rectangle bounds = new Rectangle(0, 0, image.getWidth(), image.getHeight());
		final Rectangle clamped = bounds.intersection(crop);
		if(clamped.isEmpty())
			return null;

		try{
			return image.getSubimage(clamped.x, clamped.y, clamped.width, clamped.height);
		}
		catch(final RasterFormatException rfe){
			LOGGER.error(null, rfe);

			return null;
		}
	}


	/* ======================================================================
	 *                          Reading
	 * ====================================================================== */

	/**
	 * Reads an image from the given source, returning a fully rasterized
	 * {@link BufferedImage}.
	 * <p>
	 * The source can be any of:
	 * <ul>
	 *   <li>{@link URL} — opened as a stream, since
	 *       {@link ImageIO#createImageInputStream(Object)} does not accept
	 *       URLs and returns {@code null} for them;</li>
	 *   <li>{@link File}, {@link Path}, {@link RandomAccessFile} — read
	 *       through an {@link ImageInputStream}, which is seekable and does
	 *       not load the whole file into memory;</li>
	 *   <li>{@link InputStream} — cached in memory by {@code ImageIO};</li>
	 *   <li>{@link ImageInputStream} — used as-is.</li>
	 * </ul>
	 * When no ImageIO reader is available and the source is a PDF file,
	 * the first page is rendered with PDFBox as a fallback.
	 *
	 * @param source the image source; must not be {@code null}
	 * @return the decoded image, or {@code null} when no reader is available
	 * @throws IOException if reading fails
	 */
	public static BufferedImage readBufferedImage(final Object source) throws IOException{
		Objects.requireNonNull(source, "Source cannot be null");

		// URL: ImageIO.createImageInputStream does not accept URL, so open
		// the stream here and let ImageIO.read(InputStream) create the
		// cache internally. The try-with-resources closes the URL stream;
		// ImageIO.read does not close the stream it receives.
		if(source instanceof URL url){
			try(final InputStream is = url.openStream()){
				final BufferedImage image = ImageIO.read(is);
				if(image != null)
					return image;
			}
			return readPdfFallback(source);
		}

		// Other sources go through the ImageInputStream path.
		try(final ImageInputStream input = openImageInputStream(source)){
			if(input != null){
				final BufferedImage image = readFromImageInputStream(input);
				if(image != null)
					return image;
			}
		}

		return readPdfFallback(source);
	}

	/**
	 * Kept for backward compatibility. Delegates to
	 * {@link #readBufferedImage(Object)}.
	 */
	public static BufferedImage readBufferedImage(final File file) throws IOException{
		return readBufferedImage((Object)file);
	}

	private static BufferedImage readFromImageInputStream(final ImageInputStream input) throws IOException{
		final Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
		if(!readers.hasNext())
			return null;

		final ImageReader reader = readers.next();
		try{
			reader.setInput(input);
			// Force reader initialization before read(0): some vector
			// readers materialize the rasterization pipeline on
			// width/height calls, and read(0) would fail without it.
			reader.getWidth(0);
			reader.getHeight(0);
			return reader.read(0);
		}
		finally{
			reader.dispose();
		}
	}

	/**
	 * Opens an {@link ImageInputStream} for the given source, or returns
	 * {@code null} when the source type is not supported. The stream is
	 * owned by the caller and must be closed.
	 */
	private static ImageInputStream openImageInputStream(final Object source) throws IOException{
		if(source instanceof ImageInputStream iis)
			return iis;

		if(source instanceof InputStream is)
			return ImageIO.createImageInputStream(is);

		if(source instanceof File file)
			return (file.exists()? ImageIO.createImageInputStream(file): null);

		if(source instanceof Path path)
			return (Files.exists(path)? ImageIO.createImageInputStream(path.toFile()): null);

		if(source instanceof RandomAccessFile raf)
			return ImageIO.createImageInputStream(raf);

		return null;
	}

	/**
	 * Renders the first page of a PDF source, or returns {@code null} when
	 * the source is not a PDF. The check is intentionally narrow: calling
	 * PDFBox on a non-PDF source produces a confusing error that masks the
	 * real reason (no ImageIO reader available for that format).
	 */
	private static BufferedImage readPdfFallback(final Object source) throws IOException{
		if(!isPdfSource(source))
			return null;

		final File file = toFile(source);
		if(file == null || !file.exists())
			return null;

		try(final PDDocument document = PDDocument.load(file)){
			final PDFRenderer renderer = new PDFRenderer(document);
			return renderer.renderImageWithDPI(0, 100, ImageType.RGB);
		}
	}

	private static boolean isPdfSource(final Object source){
		if(source instanceof File file)
			return file.getName().toLowerCase(Locale.ROOT).endsWith(".pdf");
		if(source instanceof Path path){
			final Path fileName = path.getFileName();
			return fileName != null && fileName.toString().toLowerCase(Locale.ROOT).endsWith(".pdf");
		}
		if(source instanceof URL url){
			final String p = url.getPath();
			return p != null && p.toLowerCase(Locale.ROOT).endsWith(".pdf");
		}
		return false;
	}

	/** Best-effort conversion of a source to a {@link File}, or {@code null}. */
	private static File toFile(final Object source){
		if(source instanceof File file)
			return file;

		if(source instanceof Path path)
			return path.toFile();

		if(source instanceof URL url){
			try{
				return new File(url.toURI());
			}
			catch(final URISyntaxException | IllegalArgumentException ignored){
				return null;
			}
		}
		return null;
	}

	/**
	 * Resolves a resource name to a URL (classpath) or a File (filesystem).
	 * Returns {@code null} when neither is found.
	 */
	private static Object resolveSource(final String filename){
		final URL resource = ResourceHelper.class.getResource(filename);
		if(resource != null)
			return resource;

		final File file = FileHelper.loadFile(filename);
		return (file != null && file.exists()? file: null);
	}


	/* ======================================================================
	 *                          Image conversions
	 * ====================================================================== */

	/**
	 * Returns an ARGB copy of the given image, so that subsequent operations
	 * (resize, crop, draw) always see a full 8-bit alpha channel.
	 * <p>
	 * PNG icons with a single colour and transparency are often decoded as
	 * {@code TYPE_BYTE_INDEXED} or {@code TYPE_BYTE_BINARY}: the palette
	 * carries a "transparent" index whose RGB is typically {@code (0,0,0)}.
	 * Once the image goes through an operation that ignores the palette
	 * (Thumbnailator, or a {@code drawImage} onto an opaque surface) the
	 * transparent pixels collapse to opaque black and the whole icon turns
	 * into a black square. Converting to ARGB up front keeps the alpha
	 * channel intact all the way through.
	 *
	 * @param image the source image; may be {@code null}
	 * @return an ARGB copy, or the source itself when it is already ARGB,
	 *         or {@code null} when the input is {@code null}
	 */
	public static BufferedImage toARGB(final BufferedImage image){
		if(image == null)
			return null;

		if(image.getType() == BufferedImage.TYPE_INT_ARGB)
			return image;

		final BufferedImage argb = new BufferedImage(
			image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = argb.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return argb;
	}

	private static BufferedImage toBufferedImage(final ImageIcon icon){
		if(icon == null)
			return null;

		if(icon.getImage() instanceof BufferedImage bi)
			return toARGB(bi);

		final BufferedImage buffered = new BufferedImage(
			icon.getIconWidth(),
			icon.getIconHeight(),
			BufferedImage.TYPE_INT_ARGB
		);
		final Graphics2D g2 = buffered.createGraphics();
		g2.drawImage(icon.getImage(), 0, 0, null);
		g2.dispose();
		return buffered;
	}

	public static BufferedImage toBufferedImage(final Image img){
		if(img instanceof BufferedImage bi)
			return toARGB(bi);

		//create a buffered image with transparency
		final BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null),
			BufferedImage.TYPE_INT_ARGB);

		//draw the image on to the buffered image
		final Graphics2D bGr = bimage.createGraphics();
		bGr.drawImage(img, 0, 0, null);
		bGr.dispose();

		//return the buffered image
		return bimage;
	}

}
