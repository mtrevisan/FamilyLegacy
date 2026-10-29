package io.github.mtrevisan.familylegacy.ui.helpers;

import io.github.mtrevisan.familylegacy.ui.components.projections.chronomap.ChronomapTimeline;
import io.github.mtrevisan.familylegacy.ui.tools.ToolDialogs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.DefaultKeyboardFocusManager;
import java.awt.GraphicsEnvironment;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * What is global (the key dispatchers, the windows, the caches) must not outlive what asked for it, or the windows would never be
 * garbage collected.
 */
class ReleasedResourcesTest{

	/** A focus manager that tells how many key dispatchers are registered. */
	private static final class CountingFocusManager extends DefaultKeyboardFocusManager{

		int dispatchers(){
			final List<KeyEventDispatcher> dispatchers = getKeyEventDispatchers();
			return (dispatchers != null? dispatchers.size(): 0);
		}

	}

	private interface Body{

		void run(CountingFocusManager manager);

	}


	/** Runs the body on the event thread, with a focus manager that counts the key dispatchers (restoring the previous one). */
	private static void onEventThread(final Body body) throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The windows need a display");

		SwingUtilities.invokeAndWait(() -> {
			final KeyboardFocusManager previous = KeyboardFocusManager.getCurrentKeyboardFocusManager();
			final CountingFocusManager manager = new CountingFocusManager();
			KeyboardFocusManager.setCurrentKeyboardFocusManager(manager);
			try{
				body.run(manager);
			}
			finally{
				KeyboardFocusManager.setCurrentKeyboardFocusManager(previous);
			}
		});
	}


	@Test
	void theKeyDispatcherOfAnEnhancedFieldGoesAwayWithItsWindow() throws InvocationTargetException, InterruptedException{
		onEventThread(manager -> {
			//the first one loads the class, that registers (once) its own static dispatcher
			final JFrame warmUp = new JFrame();
			final JTextField first = new JTextField();
			warmUp.add(first);
			DualActionTextFieldEnhancer.install(first);
			warmUp.pack();
			warmUp.dispose();
			final int before = manager.dispatchers();

			final JFrame frame = new JFrame();
			final JTextField field = new JTextField();
			frame.add(field);
			DualActionTextFieldEnhancer.install(field);
			frame.pack();
			Assertions.assertEquals(before + 1, manager.dispatchers(), "the dispatcher works while the window is there");

			frame.dispose();
			Assertions.assertEquals(before, manager.dispatchers(), "the dispatcher outlived the window");
		});
	}

	@Test
	void theKeyDispatcherOfATimelineGoesAwayWithItsWindow() throws InvocationTargetException, InterruptedException{
		onEventThread(manager -> {
			final int before = manager.dispatchers();

			final JFrame frame = new JFrame();
			frame.add(new ChronomapTimeline());
			frame.pack();
			Assertions.assertEquals(before + 1, manager.dispatchers(), "the dispatcher works while the window is there");

			frame.dispose();
			Assertions.assertEquals(before, manager.dispatchers(), "the dispatcher outlived the window");
		});
	}

	@Test
	void aToolDialogIsDisposedWhenItIsClosedWithTheTitleBar() throws InvocationTargetException, InterruptedException{
		onEventThread(manager -> {
			final JDialog dialog = new JDialog();
			ToolDialogs.installEscapeToClose(dialog);

			Assertions.assertEquals(WindowConstants.DISPOSE_ON_CLOSE, dialog.getDefaultCloseOperation(),
				"closed with the title bar, it would only be hidden, and its window would never be released");
		});
	}

	@Test
	void theCacheOfTheLoaderIsBounded() throws InterruptedException{
		final int keys = 10;
		final AsyncResourceLoader<String> loader = new AsyncResourceLoader<>(3);
		final CountDownLatch loaded = new CountDownLatch(keys);

		for(int i = 0; i < keys; i ++)
			loader.load("key" + i, () -> new String[]{"value"}, values -> loaded.countDown());

		Assertions.assertTrue(loaded.await(10, TimeUnit.SECONDS));
		Assertions.assertEquals(3, loader.cacheSize());
	}

}
