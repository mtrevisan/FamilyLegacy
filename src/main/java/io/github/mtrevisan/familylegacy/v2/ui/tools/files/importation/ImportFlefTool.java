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
package io.github.mtrevisan.familylegacy.v2.ui.tools.files.importation;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;


/**
 * Imports a FLEF file by parsing it and replacing the current model
 * with the parsed one.
 * <p>
 * Unlike Open, which is a document-level operation (it changes the
 * current file), Import is a model-level operation: the imported
 * records are merged into the current model, or replace it, according
 * to the user's choice. The document (the current file path) is left
 * unchanged, so the user can continue editing and save under the
 * original name.
 * <p>
 * The tool is deliberately simple: it either replaces the model or
 * refuses to import when the current model is not empty. A real
 * merge-with-conflict-resolution flow would need a diff engine and a
 * resolution dialog, which are out of scope for this tool.
 */
public final class ImportFlefTool implements ToolOperation{


	@Override
	public String getName(){
		return "FLEF File…";
	}

	@Override
	public void run(final ToolContext context){
		final JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Import FLEF file");
		chooser.setFileFilter(new FileNameExtensionFilter("FLEF files (*.flef)", "flef"));
		if(chooser.showOpenDialog(context.owner()) != JFileChooser.APPROVE_OPTION)
			return;

		final File file = chooser.getSelectedFile();
		try{
			final String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
			final FLEFModel imported = new FLEFParser().parse(content);

			// The import replaces the current model. The user is warned
			// when the current model is not empty, so an accidental
			// import does not silently destroy work.
			final int confirm = JOptionPane.showConfirmDialog(context.owner(),
				"Import will replace the current model with the content of:\n"
					+ file.getName() + "\n\nContinue?",
				"Confirm Import", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(confirm != JOptionPane.YES_OPTION)
				return;

			context.replaceModel(imported);
		}
		catch(final IOException ex){
			JOptionPane.showMessageDialog(context.owner(),
				"Unable to read the file:\n" + ex.getMessage(),
				"Import Error", JOptionPane.ERROR_MESSAGE);
		}
		catch(final RuntimeException ex){
			JOptionPane.showMessageDialog(context.owner(),
				"The file is not a valid FLEF document:\n" + ex.getMessage(),
				"Import Error", JOptionPane.ERROR_MESSAGE);
		}
	}

}
