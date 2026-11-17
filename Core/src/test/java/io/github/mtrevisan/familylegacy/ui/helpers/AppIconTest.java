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

import javax.imageio.ImageIO;
import javax.swing.Icon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;


class AppIconTest{

	@Test
	void rendersTheIconAtTheSizeAsked(){
		for(final int size : new int[]{16, 18, 48, 50, 256}){
			final BufferedImage image = AppIcon.render(size);

			Assertions.assertNotNull(image, "size " + size);
			Assertions.assertEquals(size, image.getWidth());
			Assertions.assertEquals(size, image.getHeight());
			//the (rounded) corners are transparent, at most for the antialiasing of the edge; the center is drawn
			Assertions.assertTrue((image.getRGB(0, size - 1) >>> 24) < 64, "corner of size " + size);
			Assertions.assertNotEquals(0, image.getRGB(size / 2, size / 2) >>> 24, "center of size " + size);
		}
	}

	@Test
	void windowImagesCoverTheSizesWindowsUses(){
		final List<Image> images = AppIcon.windowImages();

		final int[] widths = images.stream()
			.mapToInt(image -> image.getWidth(null))
			.toArray();
		Assertions.assertArrayEquals(new int[]{16, 20, 24, 32, 40, 48, 64, 128, 256}, widths);
	}

	@Test
	void dialogIconHasTheSizeAsked(){
		final Icon icon = AppIcon.icon(64);

		Assertions.assertNotNull(icon);
		Assertions.assertEquals(64, icon.getIconWidth());
		Assertions.assertEquals(64, icon.getIconHeight());
	}

	@Test
	void writesAValidIcoFile() throws IOException{
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		AppIcon.writeIco(out);
		final byte[] bytes = out.toByteArray();
		final ByteBuffer buffer = ByteBuffer.wrap(bytes)
			.order(ByteOrder.LITTLE_ENDIAN);

		Assertions.assertEquals(0, buffer.getShort(0));
		Assertions.assertEquals(1, buffer.getShort(2));
		final int count = buffer.getShort(4);
		Assertions.assertEquals(9, count);

		int expectedOffset = 6 + 16 * count;
		for(int i = 0; i < count; i ++){
			final int entry = 6 + 16 * i;
			final int size = (bytes[entry] == 0? 256: bytes[entry] & 0xFF);
			Assertions.assertEquals(32, buffer.getShort(entry + 6), "bits per pixel of " + size);
			final int length = buffer.getInt(entry + 8);
			final int offset = buffer.getInt(entry + 12);
			Assertions.assertEquals(expectedOffset, offset, "offset of " + size);
			expectedOffset += length;

			final byte[] data = Arrays.copyOfRange(bytes, offset, offset + length);
			if(size == 256){
				//the large image is a PNG
				final BufferedImage png = ImageIO.read(new ByteArrayInputStream(data));
				Assertions.assertNotNull(png);
				Assertions.assertEquals(256, png.getWidth());
			}
			else{
				//the others are bitmaps: header of 40 bytes, double height (image and mask), 32 bits per pixel
				final ByteBuffer dib = ByteBuffer.wrap(data)
					.order(ByteOrder.LITTLE_ENDIAN);
				Assertions.assertEquals(40, dib.getInt(0));
				Assertions.assertEquals(size, dib.getInt(4));
				Assertions.assertEquals(size * 2, dib.getInt(8));
				Assertions.assertEquals(32, dib.getShort(14));
				Assertions.assertEquals(40 + size * size * 4 + ((size + 31) / 32) * 4 * size, length, "length of " + size);
			}
		}
		Assertions.assertEquals(bytes.length, expectedOffset);
	}

}
