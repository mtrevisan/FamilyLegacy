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
package io.github.mtrevisan.familylegacy.ui.tools.tools;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.ui.tools.ToolOperation;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;


/**
 * Loads a FLEF file (typically a backup) and replaces the current model
 * with it. The change is immediate and irreversible: the tool asks for
 * confirmation before doing anything.
 * <p>
 * Unlike Open, which is a document-level operation, Restore is a
 * model-level operation: the current file path and the dirty flag are
 * not touched. The restored model becomes the new in-memory model, and
 * the user is expected to save it to a new file if they want to keep
 * it.
 */
public final class RestoreFromBackupTool implements ToolOperation{


	@Override
	public String getName(){
		return "Restore from Backup…";
	}

	@Override
	public void run(final ToolContext context){
		if(context == null)
			return;

		final JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Choose a backup to restore");
		chooser.setFileFilter(new FileNameExtensionFilter("FLEF files (*.flef)", "flef"));
		if(chooser.showOpenDialog(context.owner()) != JFileChooser.APPROVE_OPTION)
			return;

		final File file = chooser.getSelectedFile();
		if(file == null || !file.exists())
			return;

		final int choice = JOptionPane.showConfirmDialog(context.owner(),
			"Restore will replace the current model with the content of:" + StringUtils.LF
				+ file.getName() + "\n\nContinue?",
			"Restore from Backup", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if(choice != JOptionPane.YES_OPTION)
			return;

		try{
			final String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
			final FLEFModel restored = new FLEFParser().parse(content);

			context.replaceModel(restored);
		}
		catch(final IOException ex){
			JOptionPane.showMessageDialog(context.owner(),
				"Unable to read the backup:" + StringUtils.LF + ex.getMessage(),
				"Restore from Backup", JOptionPane.ERROR_MESSAGE);
		}
		catch(final RuntimeException ex){
			JOptionPane.showMessageDialog(context.owner(),
				"The backup is not a valid FLEF document:" + StringUtils.LF + ex.getMessage(),
				"Restore from Backup", JOptionPane.ERROR_MESSAGE);
		}
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasModel());
	}

}
