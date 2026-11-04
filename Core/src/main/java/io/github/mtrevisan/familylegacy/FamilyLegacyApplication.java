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
package io.github.mtrevisan.familylegacy;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarParser;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.PlaceholderImages;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.TreeIcons;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.TreeType;
import io.github.mtrevisan.familylegacy.ui.preferences.AppearanceManager;
import io.github.mtrevisan.familylegacy.ui.shell.FamilyLegacyFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;


/**
 * Entry point of the packaged application (the Windows executable).
 * <p>
 * It opens an empty document, or the FLEF file given as the first argument (so that a double click on a file, or
 * {@code FamilyLegacy.exe file.flef}, opens it). The other documents are opened from the File menu.
 */
public final class FamilyLegacyApplication{

	private static final Logger LOGGER = LoggerFactory.getLogger(FamilyLegacyApplication.class);


	private FamilyLegacyApplication(){}


	/** Classes whose static initializers are slow (image decoding, locale tables): warmed up away from the event dispatch thread. */
	private static final String[] WARM_UP_CLASSES = {
		PlaceholderImages.class.getName(),
		TreeIcons.class.getName(),
		GregorianCalendarParser.class.getName(),
		CalendarType.class.getName(),
		DateReader.class.getName()
	};


	public static void main(final String[] args){
		final File fileToOpen = (args.length > 0? new File(args[0]): null);

		SwingUtilities.invokeLater(() -> {
			try{
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
			catch(final Exception ignored){}

			//apply persisted language and font before any window is created
			AppearanceManager.applySaved();

			FLEFRecord.onMultipleChildren(record -> JOptionPane.showMessageDialog(null,
				"Record with more than one child: " + record, I18N.t("error.title"), JOptionPane.ERROR_MESSAGE));

			final JProgressBar progress = new JProgressBar(0, WARM_UP_CLASSES.length);
			final JWindow splash = createSplash(progress);
			splash.setVisible(true);

			warmUp(progress, () -> {
				splash.dispose();
				openFrame(fileToOpen);
			});
		});
	}

	private static JWindow createSplash(final JProgressBar progress){
		final JLabel title = new JLabel("FamilyLegacy", SwingConstants.CENTER);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 22.f));
		final JLabel message = new JLabel(I18N.t("splash.loading"), SwingConstants.CENTER);

		final JPanel content = new JPanel(new BorderLayout(0, 10));
		content.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY),
			BorderFactory.createEmptyBorder(24, 32, 24, 32)));
		content.add(title, BorderLayout.NORTH);
		content.add(message, BorderLayout.CENTER);
		content.add(progress, BorderLayout.SOUTH);

		final JWindow splash = new JWindow();
		splash.setContentPane(content);
		splash.setMinimumSize(new Dimension(360, 0));
		splash.pack();
		splash.setLocationRelativeTo(null);
		return splash;
	}

	private static void openFrame(final File fileToOpen){
		final FamilyLegacyFrame frame = new FamilyLegacyFrame(TreeType.FAMILY, new FLEFModel());
		frame.setVisible(true);

		if(fileToOpen != null){
			if(fileToOpen.isFile())
				frame.openFile(fileToOpen);
			else
				LOGGER.warn("File not found, ignored: {}", fileToOpen);
		}

		//after the window is shown, so it does not interfere with the creation of the UI fonts
		SwingUtilities.invokeLater(AppearanceManager::preloadFontFamilies);
	}

	/**
	 * Initializes the classes that would otherwise freeze the UI for seconds the first time an individual is shown, off the event
	 * dispatch thread, advancing the progress bar after each one; then runs {@code onDone} on the event dispatch thread.
	 */
	private static void warmUp(final JProgressBar progress, final Runnable onDone){
		final Thread thread = new Thread(() -> {
			int done = 0;
			for(final String className : WARM_UP_CLASSES){
				try{
					Class.forName(className, true, FamilyLegacyApplication.class.getClassLoader());
				}
				catch(final ClassNotFoundException | LinkageError e){
					LOGGER.debug("Cannot warm up {}", className, e);
				}
				final int value = ++done;
				SwingUtilities.invokeLater(() -> progress.setValue(value));
			}
			SwingUtilities.invokeLater(onDone);
		}, "warm-up");
		thread.setDaemon(true);
		thread.start();
	}

}
