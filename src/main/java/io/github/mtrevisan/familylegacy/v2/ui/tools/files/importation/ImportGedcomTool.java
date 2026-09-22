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

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.ProgressDialog;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


/**
 * Imports a GEDCOM file by parsing it and replacing the current model
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
public final class ImportGedcomTool implements ToolOperation{


	@Override
	public String getName(){
		return "GEDCOM 5.5.1…";
	}

	@Override
	public void run(final ToolContext context){
		final JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Import GEDCOM file");
		chooser.setFileFilter(new FileNameExtensionFilter("GEDCOM files (*.ged)", "ged"));
		if(chooser.showOpenDialog(context.owner()) != JFileChooser.APPROVE_OPTION)
			return;

		final File file = chooser.getSelectedFile();

		final int confirm = JOptionPane.showConfirmDialog(context.owner(),
			"Import will replace the current model with the content of:\n"
				+ file.getName() + "\n\nContinue?",
			"Confirm Import", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if(confirm != JOptionPane.YES_OPTION)
			return;

		// Resolve the owner Window (JDialog needs a Window, not a Component).
		final Window owner = (context.owner() instanceof Window w)
			? w
			: SwingUtilities.getWindowAncestor(context.owner());

		final ProgressDialog progress = new ProgressDialog(owner, "Import GEDCOM",
			"Importing " + file.getName() + "…");

		final SwingWorker<FLEFModel, Void> worker = new SwingWorker<>(){
			@Override
			protected FLEFModel doInBackground() throws Exception{
				progress.update(-1, "Reading file…");
				final String gedcomContent;
				try(final BufferedReader br = GEDCOMHelper.getBufferedReader(
					new FileInputStream(file))){
					gedcomContent = br.lines()
						.collect(Collectors.joining(System.lineSeparator()));
				}

				progress.update(-1, "Parsing GEDCOM…");
				final GEDCOMParser parser = new GEDCOMParser();
				final List<GEDCOMNode> roots;
				try(final StringReader reader = new StringReader(gedcomContent)){
					roots = parser.parse(reader);
				}

				final GEDCOMToFLEFConverter converter = new GEDCOMToFLEFConverter();
				return converter.convert(roots,
					(percent, text) -> progress.update(percent, text));
			}

			@Override
			protected void done(){
				// We are on the EDT here. Close the dialog first, then
				// schedule the model swap: replaceModel() itself uses
				// invokeLater, so the order of the two queued tasks is
				// preserved (close, then swap).
				progress.close();

				try{
					final FLEFModel imported = get();
					context.replaceModel(imported);
				}
				catch(final InterruptedException ex){
					Thread.currentThread().interrupt();
				}
				catch(final ExecutionException ex){
					final Throwable cause = (ex.getCause() != null)? ex.getCause(): ex;
					JOptionPane.showMessageDialog(context.owner(),
						"The file is not a valid GEDCOM document:\n" + cause.getMessage(),
						"Import Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		};

		worker.execute();
		// Modal: blocks the caller until done() disposes the dialog.
		progress.setVisible(true);
	}

}
