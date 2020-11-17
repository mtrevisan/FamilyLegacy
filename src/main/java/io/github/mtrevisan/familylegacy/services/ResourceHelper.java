/**
 * Copyright (c) 2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.services;

import io.github.mtrevisan.familylegacy.services.images.AnimatedGifEncoder;
import io.github.mtrevisan.familylegacy.services.images.GifDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;


public final class ResourceHelper{

	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceHelper.class);

	private static final String EXTENSION_GIF = ".gif";


	private ResourceHelper(){}

	public static ImageIcon getImage(final String filename){
		final URL imgURL = ResourceHelper.class.getResource(filename);
		return new ImageIcon(imgURL);
	}

	public static ImageIcon getImage(final String filename, final Dimension newDimension){
		return getImage(getImage(filename), newDimension.width, newDimension.height);
	}

	public static ImageIcon getImage(final ImageIcon icon, final Dimension newDimension){
		return getImage(icon, newDimension.width, newDimension.height);
	}

	public static ImageIcon getImage(final String filename, final int width, final int height){
		return getImage(getImage(filename), width, height);
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
		catch(IOException ex){
			LOGGER.error(null, ex);
		}
		return null;
	}

	private static ImageIcon scaleImage(final ImageIcon icon, final int w, final int h, final int hints) throws IOException{
		ImageIcon scaled;
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
				encoder.addFrame(toBufferedImage(((Image)frame).getScaledInstance(w, h, hints)));
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

}
