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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Prepares the images a report embeds: a region of a picture (the portrait of a person), a picture reduced to a maximum number of pixels or
 * to a longest side.
 * <p>
 * The images that were changed are written to a folder of the temporary files (removed when the application exits), and the section of the
 * report refers to them; an image that needs no change, or that cannot be read, is left where it is.
 * </p>
 */
final class ReportImages{

	private static final Logger LOGGER = LoggerFactory.getLogger(ReportImages.class);

	private static final long MEGAPIXEL = 1_000_000L;

	/** What was already written, by the image and the way it was changed (the same portrait is asked in the chapter and in the media). */
	private static final Map<String, Path> CACHE = new ConcurrentHashMap<>();
	private static volatile Path directory;


	private ReportImages(){}


	/**
	 * @param file	The image.
	 * @param crop	The region of the image to keep, {@code null} for the whole.
	 * @param longestSide	The longest side the result may have, in pixels; 0 for no limit.
	 * @param maxMegapixels	The most pixels the result may have, in millions; 0 for no limit.
	 * @return	The image itself if nothing has to be changed (or it cannot be read), otherwise a file with the changed one.
	 */
	static Path prepare(final Path file, final Rectangle crop, final int longestSide, final int maxMegapixels){
		if(crop == null && longestSide <= 0 && maxMegapixels <= 0)
			return file;

		final String key = file.toAbsolutePath() + "|" + lastModified(file) + "|" + crop + "|" + longestSide + "|" + maxMegapixels;
		final Path cached = CACHE.get(key);
		if(cached != null && Files.exists(cached))
			return cached;

		try{
			final Path out = write(file, crop, longestSide, maxMegapixels, key);
			CACHE.put(key, out);
			return out;
		}
		catch(final IOException | RuntimeException e){
			LOGGER.warn("Cannot prepare the image {} for the report, it is embedded as it is: {}", file, e.getMessage());
			return file;
		}
	}

	private static long lastModified(final Path file){
		try{
			return Files.getLastModifiedTime(file).toMillis();
		}
		catch(final IOException ignored){
			return 0L;
		}
	}


	private static Path write(final Path file, final Rectangle crop, final int longestSide, final int maxMegapixels, final String key)
			throws IOException{
		if(!Files.isRegularFile(file))
			return file;

		try(final ImageInputStream stream = ImageIO.createImageInputStream(file.toFile())){
			final Iterator<ImageReader> readers = (stream != null? ImageIO.getImageReaders(stream): null);
			if(readers == null || !readers.hasNext())
				return file;

			final ImageReader reader = readers.next();
			try{
				reader.setInput(stream, false, true);
				final int width = reader.getWidth(0);
				final int height = reader.getHeight(0);
				final Rectangle region = (crop != null? crop.intersection(new Rectangle(0, 0, width, height)): new Rectangle(0, 0, width, height));
				if(region.isEmpty())
					return file;

				final int[] size = targetSize(region.width, region.height, longestSide, maxMegapixels);
				if(crop == null && size[0] == width && size[1] == height)
					return file;

				//(decoded already reduced, as much as the target allows, so that a huge scan does not need all its memory)
				final int subsampling = Math.max(1, Math.min(region.width / size[0], region.height / size[1]));
				final ImageReadParam parameters = reader.getDefaultReadParam();
				parameters.setSourceRegion(region);
				parameters.setSourceSubsampling(subsampling, subsampling, 0, 0);
				final BufferedImage decoded = reader.read(0, parameters);

				final BufferedImage result = (decoded.getWidth() == size[0] && decoded.getHeight() == size[1]
					? decoded: scaled(decoded, size[0], size[1]));
				final boolean opaque = !result.getColorModel().hasAlpha();
				final String format = (opaque? "jpg": "png");
				final Path out = Files.createTempFile(directory(), "image-" + Integer.toHexString(key.hashCode()) + "-", "." + format);
				out.toFile().deleteOnExit();
				if(!ImageIO.write(opaque? opaque(result): result, format, out.toFile()))
					throw new IOException("No writer for " + format);
				return out;
			}
			finally{
				reader.dispose();
			}
		}
	}

	/**
	 * @return	The width and the height of an image of the size reduced to the longest side and to the maximum number of pixels, the aspect
	 * 	ratio kept; the size itself if it is within both (an image is never enlarged).
	 */
	static int[] targetSize(final int width, final int height, final int longestSide, final int maxMegapixels){
		double factor = 1.;
		if(longestSide > 0)
			factor = Math.min(factor, (double)longestSide / Math.max(width, height));
		if(maxMegapixels > 0 && (long)width * height > maxMegapixels * MEGAPIXEL)
			factor = Math.min(factor, Math.sqrt((double)(maxMegapixels * MEGAPIXEL) / ((double)width * height)));
		if(factor >= 1.)
			return new int[]{width, height};

		return new int[]{Math.max(1, (int)Math.floor(width * factor)), Math.max(1, (int)Math.floor(height * factor))};
	}

	private static BufferedImage scaled(final BufferedImage source, final int width, final int height){
		final BufferedImage result = new BufferedImage(width, height,
			(source.getColorModel().hasAlpha()? BufferedImage.TYPE_INT_ARGB: BufferedImage.TYPE_INT_RGB));
		final Graphics2D g = result.createGraphics();
		try{
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.drawImage(source, 0, 0, width, height, null);
		}
		finally{
			g.dispose();
		}
		return result;
	}

	/** @return	The image as RGB, that a JPEG writer accepts (a palette or a gray image is converted). */
	private static BufferedImage opaque(final BufferedImage source){
		if(source.getType() == BufferedImage.TYPE_INT_RGB || source.getType() == BufferedImage.TYPE_3BYTE_BGR)
			return source;

		final BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = result.createGraphics();
		try{
			g.drawImage(source, 0, 0, null);
		}
		finally{
			g.dispose();
		}
		return result;
	}

	private static Path directory() throws IOException{
		Path result = directory;
		if(result == null)
			synchronized(ReportImages.class){
				result = directory;
				if(result == null){
					result = Files.createTempDirectory("flef-report-images-");
					result.toFile().deleteOnExit();
					directory = result;
				}
			}
		return result;
	}

}
