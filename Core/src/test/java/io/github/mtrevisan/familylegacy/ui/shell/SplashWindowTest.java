package io.github.mtrevisan.familylegacy.ui.shell;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;


/**
 * The splash window.
 * <p>
 * To <b>see</b> it, run {@link #main(String[])} (the green arrow beside it in the IDE): it opens only the splash, with the
 * progress advancing, then closes it. Setting the environment variable {@code SPLASH_IMAGES} to a folder also makes
 * {@link #paintsTheLogoTheNameAndTheProgress()} save the image there.
 */
class SplashWindowTest{

	private static final int STEPS = 20;


	/** Opens only the splash, so it can be looked at: the progress goes from 0 to 100% in about six seconds. */
	public static void main(final String[] args) throws Exception{
		final SplashWindow[] holder = new SplashWindow[1];
		SwingUtilities.invokeAndWait(() -> {
			holder[0] = new SplashWindow();
			holder[0].setProgress(0, STEPS);
			holder[0].setVisible(true);
		});
		for(int step = 1; step <= STEPS; step ++){
			Thread.sleep(300l);
			final int value = step;
			SwingUtilities.invokeAndWait(() -> holder[0].setProgress(value, STEPS));
		}
		Thread.sleep(1_000l);
		SwingUtilities.invokeAndWait(holder[0]::dispose);
	}


	@Test
	void paintsTheLogoTheNameAndTheProgress() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

		final SplashWindow[] holder = new SplashWindow[1];
		SwingUtilities.invokeAndWait(() -> {
			holder[0] = new SplashWindow();
			holder[0].setProgress(13, 20);
			holder[0].snapToProgress();
			holder[0].setVisible(true);
		});
		try{
			//the layout is queued: flushed in separate calls
			for(int i = 0; i < 3; i ++)
				SwingUtilities.invokeAndWait(() -> {});

			final BufferedImage[] image = new BufferedImage[1];
			SwingUtilities.invokeAndWait(() -> {
				final SplashWindow splash = holder[0];
				image[0] = new BufferedImage(splash.getWidth(), splash.getHeight(), BufferedImage.TYPE_INT_ARGB);
				splash.getContentPane().paint(image[0].getGraphics());
			});

			final BufferedImage painted = image[0];
			//the rounded corner is transparent, the middle of the gradient is drawn
			Assertions.assertEquals(0, painted.getRGB(1, 1) >>> 24);
			Assertions.assertEquals(255, painted.getRGB(painted.getWidth() - 40, 40) >>> 24);
			//the logo (at the left) is not the plain background: it has many different colors
			Assertions.assertTrue(distinctColors(painted, 40, 50, 190, 190) > 50);
			//the bar is filled up to 13/20 of its width, and the rest is only the track
			final int barY = painted.getHeight() - 48;
			Assertions.assertNotEquals(painted.getRGB(100, barY), painted.getRGB(painted.getWidth() - 100, barY));

			final String folder = System.getenv("SPLASH_IMAGES");
			if(folder != null)
				ImageIO.write(painted, "png", new File(folder, "splash.png"));
		}
		finally{
			SwingUtilities.invokeAndWait(holder[0]::dispose);
		}
	}

	@Test
	void theProgressIsClampedAndGlidesToTheRealOne() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

		final SplashWindow[] holder = new SplashWindow[1];
		SwingUtilities.invokeAndWait(() -> holder[0] = new SplashWindow());
		try{
			SwingUtilities.invokeAndWait(() -> {
				holder[0].setProgress(50, 20);
				holder[0].snapToProgress();
			});
			final double[] shown = new double[1];
			SwingUtilities.invokeAndWait(() -> shown[0] = holder[0].displayedProgress());
			Assertions.assertEquals(1., shown[0], 1e-9);

			SwingUtilities.invokeAndWait(() -> holder[0].setProgress(-3, 20));
			Thread.sleep(1_500l);
			SwingUtilities.invokeAndWait(() -> shown[0] = holder[0].displayedProgress());
			Assertions.assertEquals(0., shown[0], 1e-9);
		}
		finally{
			SwingUtilities.invokeAndWait(holder[0]::dispose);
		}
	}

	private static int distinctColors(final BufferedImage image, final int x, final int y, final int width, final int height){
		final java.util.Set<Integer> colors = new java.util.HashSet<>();
		for(int j = y; j < y + height; j += 3)
			for(int i = x; i < x + width; i += 3)
				colors.add(image.getRGB(i, j));
		return colors.size();
	}

}
