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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


/** The images a report embeds are reduced to a largest size, or cut to the region of a portrait. */
class ReportImagesTest{

	@TempDir
	Path folder;


	private Path picture(final String name, final int width, final int height) throws IOException{
		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = image.createGraphics();
		g.setColor(Color.ORANGE);
		g.fillRect(0, 0, width, height);
		g.setColor(Color.BLUE);
		g.fillRect(width / 2, height / 2, width / 2, height / 2);
		g.dispose();
		final Path file = folder.resolve(name);
		ImageIO.write(image, "png", file.toFile());
		return file;
	}

	private static BufferedImage read(final Path file) throws IOException{
		return ImageIO.read(file.toFile());
	}


	@Test
	void theTargetSizeKeepsTheAspectRatio(){
		Assertions.assertArrayEquals(new int[]{2000, 1000}, ReportImages.targetSize(2000, 1000, 0, 0));
		Assertions.assertArrayEquals(new int[]{500, 250}, ReportImages.targetSize(2000, 1000, 500, 0));
		//(2 Mpx, to 1 Mpx: the sides are divided by the square root of two)
		final int[] size = ReportImages.targetSize(2000, 1000, 0, 1);
		Assertions.assertTrue((long)size[0] * size[1] <= 1_000_000L, size[0] + "x" + size[1]);
		Assertions.assertTrue((long)size[0] * size[1] > 990_000L, size[0] + "x" + size[1]);
		Assertions.assertEquals(2, Math.round((double)size[0] / size[1]));
	}

	@Test
	void anImageIsNeverEnlarged(){
		Assertions.assertArrayEquals(new int[]{100, 50}, ReportImages.targetSize(100, 50, 500, 0));
		Assertions.assertArrayEquals(new int[]{100, 50}, ReportImages.targetSize(100, 50, 0, 4));
		Assertions.assertArrayEquals(new int[]{100, 50}, ReportImages.targetSize(100, 50, 500, 4));
	}

	@Test
	void theTighterOfTheTwoLimitsWins(){
		//2 Mpx, longest side 100: the side limit (the megapixels would leave 1414)
		Assertions.assertArrayEquals(new int[]{100, 50}, ReportImages.targetSize(2000, 1000, 100, 1));
	}

	@Test
	void anImageThatNeedsNoChangeStaysWhereItIs() throws IOException{
		final Path file = picture("small.png", 100, 50);

		Assertions.assertSame(file, ReportImages.prepare(file, null, 0, 0));
		Assertions.assertSame(file, ReportImages.prepare(file, null, 500, 0));
		Assertions.assertSame(file, ReportImages.prepare(file, null, 0, 2));
	}

	@Test
	void anImageOverTheLimitOfMegapixelsIsReduced() throws IOException{
		final Path file = picture("large.png", 2000, 1000);

		final Path reduced = ReportImages.prepare(file, null, 0, 1);

		Assertions.assertNotEquals(file, reduced);
		final BufferedImage image = read(reduced);
		Assertions.assertTrue((long)image.getWidth() * image.getHeight() <= 1_000_000L, image.getWidth() + "x" + image.getHeight());
		//(the original is not touched)
		Assertions.assertEquals(2000, read(file).getWidth());
	}

	@Test
	void anImageIsReducedToTheLongestSide() throws IOException{
		final Path file = picture("large.png", 2000, 1000);

		final BufferedImage image = read(ReportImages.prepare(file, null, 400, 0));

		Assertions.assertEquals(400, image.getWidth());
		Assertions.assertEquals(200, image.getHeight());
	}

	@Test
	void aPortraitIsTheRegionOfThePicture() throws IOException{
		final Path file = picture("group.png", 1000, 800);

		final BufferedImage image = read(ReportImages.prepare(file, new Rectangle(100, 50, 300, 200), 0, 0));

		Assertions.assertEquals(300, image.getWidth());
		Assertions.assertEquals(200, image.getHeight());
	}

	@Test
	void aRegionBeyondTheImageIsCutToTheImage() throws IOException{
		final Path file = picture("group.png", 1000, 800);

		final BufferedImage image = read(ReportImages.prepare(file, new Rectangle(900, 700, 300, 300), 0, 0));

		Assertions.assertEquals(100, image.getWidth());
		Assertions.assertEquals(100, image.getHeight());
	}

	@Test
	void aPortraitCanBeReducedToo() throws IOException{
		final Path file = picture("group.png", 1000, 800);

		final BufferedImage image = read(ReportImages.prepare(file, new Rectangle(100, 50, 300, 200), 150, 0));

		Assertions.assertEquals(150, image.getWidth());
		Assertions.assertEquals(100, image.getHeight());
	}

	@Test
	void theSameRequestIsWrittenOnce() throws IOException{
		final Path file = picture("large.png", 2000, 1000);

		Assertions.assertEquals(ReportImages.prepare(file, null, 400, 0), ReportImages.prepare(file, null, 400, 0));
	}

	@Test
	void whatCannotBeReadIsLeftAsItIs() throws IOException{
		final Path notAnImage = Files.writeString(folder.resolve("text.png"), "not an image");
		final Path missing = folder.resolve("missing.png");

		Assertions.assertSame(notAnImage, ReportImages.prepare(notAnImage, null, 100, 1));
		Assertions.assertSame(missing, ReportImages.prepare(missing, new Rectangle(0, 0, 10, 10), 100, 1));
	}

}
