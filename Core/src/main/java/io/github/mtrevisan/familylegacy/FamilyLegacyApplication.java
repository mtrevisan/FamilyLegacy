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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.FamilyLegacyFrame;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.TreeType;
import io.github.mtrevisan.familylegacy.ui.preferences.AppearanceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
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


	public static void main(final String[] args){
		final File fileToOpen = (args.length > 0? new File(args[0]): null);

		SwingUtilities.invokeLater(() -> {
			try{
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
			catch(final Exception ignored){}

			//apply persisted language and font before any window is created
			AppearanceManager.applySaved();

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
		});
	}

}
