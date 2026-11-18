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

import io.github.mtrevisan.familylegacy.i18n.I18N;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;


/**
 * What the user is told when a file has been saved (a backup, an export, a report...): where it is, and a button to open the directory
 * with the file selected, if the system can select it (otherwise the directory is opened).
 */
public final class SavedFileNotice{

	private static final Logger LOGGER = LoggerFactory.getLogger(SavedFileNotice.class);


	private SavedFileNotice(){}


	/**
	 * Tells that a file has been saved, with the button to open its directory.
	 *
	 * @param owner	The window the message is over.
	 * @param title	The title of the message.
	 * @param message	What to tell the user (the name of the file is added if the message does not have it).
	 * @param file	The file that was saved.
	 */
	public static void show(final Component owner, final String title, final String message, final File file){
		if(GraphicsEnvironment.isHeadless())
			return;

		final Object[] options = {I18N.t("button.ok"), I18N.t("button.open.directory")};
		final String text = (message.contains(file.getName()) || message.contains(file.getAbsolutePath())
			? message
			: message + StringUtils.LF + file.getAbsolutePath());
		final int choice = JOptionPane.showOptionDialog(owner, text, title, JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
			null, options, options[0]);
		if(choice == 1 && !reveal(file))
			JOptionPane.showMessageDialog(owner, I18N.l("dialog.report.open_directory.error") + StringUtils.LF + file.getParent(),
				I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Opens the directory of a file with the file selected, if the system can do it, otherwise just the directory.
	 *
	 * @param file	The file.
	 * @return	Whether something was opened.
	 */
	public static boolean reveal(final File file){
		if(GraphicsEnvironment.isHeadless() || file == null)
			return false;

		try{
			if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR) && file.exists()){
				Desktop.getDesktop()
					.browseFileDirectory(file);
				return true;
			}
		}
		catch(final RuntimeException re){
			LOGGER.warn("Cannot select {} in its directory: {}", file, re.toString());
		}

		try{
			final File directory = (file.isDirectory()? file: file.getAbsoluteFile().getParentFile());
			return (directory != null && FileHelper.browse(directory));
		}
		catch(final IOException | InterruptedException e){
			if(e instanceof InterruptedException)
				Thread.currentThread().interrupt();
			LOGGER.warn("Cannot open the directory of {}: {}", file, e.toString());
			return false;
		}
	}

}
