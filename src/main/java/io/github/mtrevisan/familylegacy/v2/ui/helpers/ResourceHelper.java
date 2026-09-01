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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;


public final class ResourceHelper{

	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceHelper.class);


	private ResourceHelper(){}


	public static ImageIcon getCroppedImage(final String filename, final Rectangle crop){
		URL imgURL = ResourceHelper.class.getResource(filename);
		if(imgURL == null){
			final File file = FileHelper.loadFile(filename);
			try{
				imgURL = file.toURI()
					.toURL();
			}
			catch(final MalformedURLException ignored){
				return null;
			}
		}

		final ImageIcon imageIcon = new ImageIcon(imgURL);
		if(crop != null){
			final BufferedImage original = toBufferedImage(imageIcon);
			if(original == null)
				return null;

			//clamp the requested crop to the actual image bounds, so an out-of-range
			//rectangle degrades gracefully instead of throwing
			final Rectangle bounds = new Rectangle(0, 0, original.getWidth(), original.getHeight());
			final Rectangle clamped = bounds.intersection(crop);
			if(clamped.isEmpty())
				return null;

			try{
				final BufferedImage cropped = original.getSubimage(clamped.x, clamped.y, clamped.width,
					clamped.height);
				return new ImageIcon(cropped);
			}
			catch(final RasterFormatException rfe){
				LOGGER.error(null, rfe);

				return null;
			}
		}

		return imageIcon;
	}

	public static ImageIcon getImage(final String filename){
		return getCroppedImage(filename, null);
	}

	public static ImageIcon getResizedImage(final String filename, final Dimension newDimension){
		final ImageIcon croppedImage = getCroppedImage(filename, null);
		return resize(croppedImage, newDimension.width, newDimension.height);
	}

	public static ImageIcon getResizedImage(final String filename, final int width, final int height){
		final ImageIcon croppedImage = getCroppedImage(filename, null);
		return resize(croppedImage, width, height);
	}

	public static ImageIcon getCroppedResizedImage(final String filename, final Rectangle crop, final int width,
			final int height){
		final ImageIcon croppedImage = getCroppedImage(filename, crop);
		if(croppedImage == null){
			LOGGER.error("Non-existent image for {}", filename);

			return null;
		}

		return resize(croppedImage, width, height);
	}


	public static ImageIcon resize(final ImageIcon icon, final Dimension newDimension){
		return resize(icon, newDimension.width, newDimension.height);
	}

	public static ImageIcon resize(final ImageIcon icon, final int width, final int height){
		try{
			final BufferedImage original = toBufferedImage(icon);
			final BufferedImage scaled = Thumbnails.of(original)
				.size(width, height)
				.keepAspectRatio(true)
				.asBufferedImage();

			return new ImageIcon(scaled);
		}
		catch(final Exception e){
			LOGGER.error(null, e);
		}
		return null;
	}


	private static BufferedImage toBufferedImage(final ImageIcon icon){
		if(icon == null || icon.getIconWidth() < 0)
			return null;

		if(icon.getImage() instanceof BufferedImage)
			return (BufferedImage)icon.getImage();

		final BufferedImage buffered = new BufferedImage(
			icon.getIconWidth(),
			icon.getIconHeight(),
			BufferedImage.TYPE_INT_ARGB
		);
		final Graphics2D g2d = buffered.createGraphics();
		g2d.drawImage(icon.getImage(), 0, 0, null);
		g2d.dispose();
		return buffered;
	}

	public static BufferedImage toBufferedImage(final Image img){
		if(img instanceof BufferedImage)
			return (BufferedImage)img;

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


	public static ImageIcon getImageFixedHeight(final String filename, final int height){
		final ImageIcon croppedImage = getCroppedImage(filename, null);
		return resizeFixedHeight(croppedImage, height);
	}

	private static ImageIcon resizeFixedHeight(final ImageIcon icon, final int height){
		try{
			final BufferedImage original = toBufferedImage(icon);
			final BufferedImage scaled = Thumbnails.of(original)
				.height(height)
				.asBufferedImage();

			return new ImageIcon(scaled);
		}
		catch(final Exception e){
			LOGGER.error(null, e);
		}
		return null;
	}


	public static BufferedImage readBufferedImage(final File file) throws IOException{
		if(!file.exists())
			throw new IllegalArgumentException("File `" + file.getPath() + "` does not exists.");

		try(final ImageInputStream input = ImageIO.createImageInputStream(file)){
			final Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if(readers.hasNext()){
				final ImageReader reader = readers.next();
				try{
					reader.setInput(input);
					return reader.read(0);
				}
				finally{
					reader.dispose();
				}
			}
			else{
				//try to read a PDF
				try(final PDDocument document = PDDocument.load(file)){
					final PDFRenderer renderer = new PDFRenderer(document);
					return renderer.renderImageWithDPI(0, 100, ImageType.RGB);
				}
				catch(final IllegalArgumentException ignored){
					throw new IllegalArgumentException("No reader for " + file.getPath());
				}
			}
		}
	}

}
