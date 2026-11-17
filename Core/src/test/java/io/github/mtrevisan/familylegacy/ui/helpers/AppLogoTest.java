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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import java.util.List;


class AppLogoTest{

	@Test
	void logoHasTheSizeAskedAndAVariantForEachDisplayScale(){
		final Icon icon = AppLogo.icon(128);

		Assertions.assertNotNull(icon);
		Assertions.assertEquals(128, icon.getIconWidth());
		Assertions.assertEquals(128, icon.getIconHeight());

		final Image image = ((ImageIcon)icon).getImage();
		final List<Image> variants = ((MultiResolutionImage)image).getResolutionVariants();
		final int[] widths = variants.stream()
			.mapToInt(variant -> variant.getWidth(null))
			.toArray();
		Assertions.assertArrayEquals(new int[]{128, 160, 192, 256, 384}, widths);
		//the corners of the logo are transparent, its center is drawn
		final BufferedImage first = (BufferedImage)variants.getFirst();
		Assertions.assertEquals(0, first.getRGB(0, 0) >>> 24);
		Assertions.assertNotEquals(0, first.getRGB(64, 64) >>> 24);
	}

}
