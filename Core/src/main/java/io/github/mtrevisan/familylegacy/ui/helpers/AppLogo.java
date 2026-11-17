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
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;


/**
 * The logo of the application ({@code /images/logo.png}), shown where there is room for it (e.g. the help); the
 * {@linkplain AppIcon icon} is the same drawing, cut closer.
 */
public final class AppLogo{

	private static final Logger LOGGER = LoggerFactory.getLogger(AppLogo.class);

	private static final String RESOURCE = "/images/logo.png";


	private AppLogo(){}


	/**
	 * The logo at the given size.
	 *
	 * @param size	The size of the logo, in (unscaled) pixels.
	 * @return the logo, sharp at every display scale, or {@code null} if the image cannot be read.
	 */
	public static Icon icon(final int size){
		final BufferedImage original = Original.IMAGE;
		return (original != null? AppIcon.multiResolution(target -> scale(original, target), size): null);
	}

	/** Read once, the first time it is needed (it is large: the sizes shown are scaled from it). */
	private static final class Original{
		private static final BufferedImage IMAGE = read();
	}

	private static BufferedImage read(){
		final URL url = AppLogo.class.getResource(RESOURCE);
		if(url == null){
			LOGGER.error("Resource not found: `{}`", RESOURCE);

			return null;
		}

		try{
			final BufferedImage image = ImageIO.read(url);
			if(image == null)
				LOGGER.error("No reader for image `{}`", RESOURCE);
			return image;
		}
		catch(final IOException e){
			LOGGER.error("Unable to read image `{}`", RESOURCE, e);

			return null;
		}
	}

	private static BufferedImage scale(final BufferedImage original, final int size){
		try{
			//scaled in steps, so that the thin lines of the drawing do not break up as with a single reduction
			return Thumbnails.of(original)
				.size(size, size)
				.asBufferedImage();
		}
		catch(final IOException e){
			LOGGER.error("Unable to scale image `{}`", RESOURCE, e);

			return null;
		}
	}

}
