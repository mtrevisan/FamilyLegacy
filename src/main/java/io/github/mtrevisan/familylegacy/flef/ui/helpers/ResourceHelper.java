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
package io.github.mtrevisan.familylegacy.flef.ui.helpers;

import io.github.mtrevisan.familylegacy.flef.helpers.FileHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.images.AnimatedGifEncoder;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.images.GifDecoder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;


public final class ResourceHelper{

	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceHelper.class);

	private static final String EXTENSION_GIF = ".gif";


	private ResourceHelper(){}

	public static ImageIcon getOriginalImage(final String filename){
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
		return new ImageIcon(imgURL);
	}

	public static ImageIcon getImage(final String filename, final Dimension newDimension){
		return getImage(getOriginalImage(filename), newDimension.width, newDimension.height);
	}

	public static ImageIcon getImage(final ImageIcon icon, final Dimension newDimension){
		return getImage(icon, newDimension.width, newDimension.height);
	}

	public static ImageIcon getImage(final String filename, final int width, final int height){
		return getImage(getOriginalImage(filename), width, height);
	}

	private static ImageIcon getImage(final ImageIcon icon, final int width, final int height){
		try{
			final int iconWidth = icon.getIconWidth();
			final int iconHeight = icon.getIconHeight();
			final double ratio = Math.min((double)width / iconWidth, (double)height / iconHeight);
			final int w = (int)(iconWidth * ratio);
			final int h = (int)(iconHeight * ratio);
			final int hints = Image.SCALE_SMOOTH;
			return scaleImage(icon, w, h, hints);
		}
		catch(final Exception e){
			LOGGER.error(null, e);
		}
		return null;
	}

	private static ImageIcon scaleImage(final ImageIcon icon, final int w, final int h, final int hints) throws IOException{
		final ImageIcon scaled;
		if(icon.getDescription().endsWith(EXTENSION_GIF)){
			final GifDecoder decoder = new GifDecoder();
			decoder.read(icon.getDescription());

			final int frameCount = decoder.getFrameCount();
			final int loopCount = decoder.getLoopCount();

			final AnimatedGifEncoder encoder = new AnimatedGifEncoder();
			encoder.setTransparent(Color.BLACK, true);
			encoder.setRepeat(loopCount);

			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			encoder.start(os);
			for(int frameNumber = 0; frameNumber < frameCount; frameNumber ++){
				final BufferedImage frame = decoder.getFrame(frameNumber);
				final int delay = decoder.getDelay(frameNumber);
				encoder.setDelay(delay);
				encoder.addFrame(toBufferedImage(frame.getScaledInstance(w, h, hints)));
			}
			encoder.finish();

			os.flush();
			os.close();

			scaled = new ImageIcon(os.toByteArray());
		}
		else
			scaled = new ImageIcon(icon.getImage()
				.getScaledInstance(w, h, hints));
		return scaled;
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

	public static BufferedImage readImage(final File file) throws IOException{
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
