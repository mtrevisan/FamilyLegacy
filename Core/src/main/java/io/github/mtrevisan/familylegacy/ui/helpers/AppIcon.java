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

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;


/**
 * The icon of the application: the {@linkplain AppLogo logo} cut close around its drawing, so that it fills the square.
 * <p>
 * The small sizes ({@code /images/app-icon-<size>.png}, up to 48 pixels) are prepared one by one, so that the outline survives instead
 * of fading into a shapeless mass: up to 24 pixels a simplified drawing (the magnifier and the main branches of the tree, without the
 * leaves), from 32 to 48 the full drawing with its lines strengthened. The larger sizes are scaled from {@code /images/app-icon.png}.
 * <p>
 * The same image is the icon of the windows (title bar, task bar, Alt+Tab), of the About dialog and, through {@link #main(String[])},
 * of the Windows executable (the packaging writes it as an {@code .ico} file for jpackage).
 */
public final class AppIcon{

	private static final Logger LOGGER = LoggerFactory.getLogger(AppIcon.class);

	/** The icon at 256 pixels, from which the sizes that have not been prepared are scaled. */
	private static final String LARGE_RESOURCE = "/images/app-icon.png";
	/** The icon at a size that has been prepared on its own. */
	private static final String SMALL_RESOURCE = "/images/app-icon-%d.png";
	/** The sizes that have been prepared on their own, ascending. */
	private static final int[] PREPARED_SIZES = {16, 20, 24, 32, 40, 48};

	/**
	 * The sizes Windows picks from, for a window (title bar 16-24, task bar 32-40, Alt+Tab 48-64) and for the executable (Explorer,
	 * Start menu, desktop shortcut), the larger ones at high DPI.
	 */
	private static final int[] SIZES = {16, 20, 24, 32, 40, 48, 64, 128, 256};
	/** The display scales the icon of a dialog is drawn for, so that it stays sharp on a high-DPI screen. */
	private static final double[] DISPLAY_SCALES = {1., 1.25, 1.5, 2., 3.};


	private AppIcon(){}


	/**
	 * The images to give to {@link java.awt.Window#setIconImages(List)}.
	 *
	 * @return the icon at several sizes (empty if the image cannot be read).
	 */
	public static List<Image> windowImages(){
		return WindowImages.IMAGES;
	}

	/** Rendered once, the first time they are asked for (the warm-up of the application does it away from the event dispatch thread). */
	private static final class WindowImages{
		private static final List<Image> IMAGES = renderAll(SIZES);
	}

	/**
	 * The icon to show in a dialog, e.g. the About dialog.
	 *
	 * @param size	The size of the icon, in (unscaled) pixels.
	 * @return the icon, sharp at every display scale, or {@code null} if the image cannot be read.
	 */
	public static Icon icon(final int size){
		return multiResolution(AppIcon::render, size);
	}

	/**
	 * An icon that stays sharp at every display scale: the image is drawn for each scale, instead of being enlarged by the screen.
	 *
	 * @param renderer	Draws the image at a given size, in pixels (may return {@code null}).
	 * @param size	The size of the icon, in (unscaled) pixels.
	 * @return the icon, or {@code null} if no image could be drawn.
	 */
	static Icon multiResolution(final IntFunction<BufferedImage> renderer, final int size){
		final List<Image> variants = new ArrayList<>(DISPLAY_SCALES.length);
		for(final double scale : DISPLAY_SCALES){
			final BufferedImage image = renderer.apply((int)Math.round(size * scale));
			if(image != null)
				variants.add(image);
		}
		return (variants.isEmpty()? null: new ImageIcon(new BaseMultiResolutionImage(variants.toArray(Image[]::new))));
	}

	/**
	 * Draws the icon at the given size.
	 * <p>
	 * A size that has been prepared is used as is; a smaller one is scaled from the next prepared size (so that it keeps the thicker
	 * lines), a larger one from the large image.
	 *
	 * @param size	The width and height of the image, in pixels.
	 * @return the image, or {@code null} if the image cannot be read.
	 */
	public static BufferedImage render(final int size){
		String resource = LARGE_RESOURCE;
		for(final int prepared : PREPARED_SIZES)
			if(prepared >= size){
				resource = String.format(Locale.ROOT, SMALL_RESOURCE, prepared);
				break;
			}

		final URL url = AppIcon.class.getResource(resource);
		if(url == null){
			LOGGER.error("Resource not found: `{}`", resource);

			return null;
		}

		try{
			final BufferedImage image = ImageIO.read(url);
			if(image == null){
				LOGGER.error("No reader for image `{}`", resource);

				return null;
			}

			return toSize(image, size);
		}
		catch(final IOException | RuntimeException e){
			LOGGER.error("Unable to read image `{}`", resource, e);

			return null;
		}
	}

	/** Makes the image ARGB and exactly {@code size} by {@code size}. */
	private static BufferedImage toSize(final BufferedImage image, final int size) throws IOException{
		final BufferedImage argb = ResourceHelper.toARGB(image);
		if(argb.getWidth() == size && argb.getHeight() == size)
			return argb;

		//scaled in steps, so that the thin lines of the drawing do not break up as with a single reduction
		return ResourceHelper.toARGB(Thumbnails.of(argb)
			.forceSize(size, size)
			.asBufferedImage());
	}

	private static List<Image> renderAll(final int[] sizes){
		final List<Image> images = new ArrayList<>(sizes.length);
		for(final int size : sizes){
			final BufferedImage image = render(size);
			if(image != null)
				images.add(image);
		}
		return Collections.unmodifiableList(images);
	}


	/**
	 * Writes the icon as a Windows {@code .ico} file, with an image for each of the sizes Windows uses.
	 * <p>
	 * The images up to 128 pixels are stored as 32-bit bitmaps, the one of 256 pixels as PNG (as Windows itself does).
	 *
	 * @param out	The stream to write to.
	 * @throws IOException	If the image cannot be read, or the stream cannot be written.
	 */
	public static void writeIco(final OutputStream out) throws IOException{
		final List<byte[]> entries = new ArrayList<>(SIZES.length);
		for(final int size : SIZES){
			final BufferedImage image = render(size);
			if(image == null)
				throw new IOException("Unable to draw the icon at " + size + " pixels");

			entries.add(size >= 256? png(image): dib(image));
		}

		final DataOutputStream data = new DataOutputStream(out);
		//ICONDIR: reserved, type (1 = icon), count
		writeShortLE(data, 0);
		writeShortLE(data, 1);
		writeShortLE(data, SIZES.length);
		//ICONDIRENTRY for each image, then the images
		int offset = 6 + 16 * SIZES.length;
		for(int i = 0; i < SIZES.length; i ++){
			final int size = SIZES[i];
			//width and height: 0 stands for 256
			data.writeByte(size >= 256? 0: size);
			data.writeByte(size >= 256? 0: size);
			//colors in the palette (none), reserved
			data.writeByte(0);
			data.writeByte(0);
			//color planes, bits per pixel
			writeShortLE(data, 1);
			writeShortLE(data, 32);
			writeIntLE(data, entries.get(i).length);
			writeIntLE(data, offset);
			offset += entries.get(i).length;
		}
		for(final byte[] entry : entries)
			data.write(entry);
		data.flush();
	}

	private static byte[] png(final BufferedImage image) throws IOException{
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		if(!ImageIO.write(image, "png", out))
			throw new IOException("No PNG writer");

		return out.toByteArray();
	}

	/** A 32-bit device-independent bitmap as an {@code .ico} wants it: double height (image plus mask), rows from the bottom. */
	private static byte[] dib(final BufferedImage image) throws IOException{
		final int size = image.getWidth();
		//the mask has one bit per pixel, each row padded to 4 bytes; the alpha channel makes it irrelevant, so it is all zeros
		final int maskRowBytes = ((size + 31) / 32) * 4;
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream(40 + size * size * 4 + maskRowBytes * size);
		final DataOutputStream data = new DataOutputStream(bytes);
		//BITMAPINFOHEADER
		writeIntLE(data, 40);
		writeIntLE(data, size);
		writeIntLE(data, size * 2);
		writeShortLE(data, 1);
		writeShortLE(data, 32);
		//compression (BI_RGB), image size (may be 0 for BI_RGB), resolution, colors used, important colors
		for(int i = 0; i < 6; i ++)
			writeIntLE(data, 0);
		for(int y = size - 1; y >= 0; y --)
			for(int x = 0; x < size; x ++)
				//ARGB written little-endian is the BGRA order of the bitmap
				writeIntLE(data, image.getRGB(x, y));
		data.write(new byte[maskRowBytes * size]);
		data.flush();
		return bytes.toByteArray();
	}

	private static void writeShortLE(final DataOutputStream data, final int value) throws IOException{
		data.writeByte(value & 0xFF);
		data.writeByte((value >>> 8) & 0xFF);
	}

	private static void writeIntLE(final DataOutputStream data, final int value) throws IOException{
		writeShortLE(data, value & 0xFFFF);
		writeShortLE(data, (value >>> 16) & 0xFFFF);
	}


	/**
	 * Writes the {@code .ico} file of the executable; it is run by the packaging, before jpackage.
	 *
	 * @param args	The path of the file to write.
	 * @throws IOException	If the icon cannot be drawn or written.
	 */
	static void main(final String[] args) throws IOException{
		if(args.length != 1)
			throw new IllegalArgumentException("Usage: AppIcon <output .ico file>");

		final Path output = Path.of(args[0]);
		final Path parent = output.toAbsolutePath()
			.getParent();
		if(parent != null)
			Files.createDirectories(parent);
		try(final OutputStream out = Files.newOutputStream(output)){
			writeIco(out);
		}
	}

}
