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
package io.github.mtrevisan.familylegacy.ui.tools.files.importation;

import io.github.mtrevisan.familylegacy.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.dialogs.ProgressDialog;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.ui.tools.ToolOperation;
import org.apache.commons.lang3.StringUtils;

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
 * Imports a GEDCOM file by converting it into a new FLEF file, which
 * becomes the current document.
 * <p>
 * GEDCOM 5.5, 5.5.1, and 7 are read; the version is the one declared
 * by the header of the file (see {@link GEDCOMToFLEFConverter}).
 * <p>
 * Once the conversion succeeds, the user chooses where to save the FLEF
 * file; the file is written and then opened, as Open would do (see
 * {@link io.github.mtrevisan.familylegacy.ui.tools.files.FileMenuController#openImported}).
 * Cancelling the choice discards the import.
 * <p>
 * The tool is deliberately simple: it never merges into the current
 * model. A real merge-with-conflict-resolution flow would need a diff
 * engine and a resolution dialog, which are out of scope for this tool.
 */
public final class ImportGedcomTool implements ToolOperation{


	@Override
	public String getName(){
		return "GEDCOM 5.5.x / 7.0…";
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
			I18N.tf("confirmation.overwrite.import.message", file.getName()),
			I18N.t("confirmation.overwrite.import.title"),
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if(confirm != JOptionPane.YES_OPTION)
			return;

		// Resolve the owner Window (JDialog needs a Window, not a Component).
		final Window owner = (context.owner() instanceof Window w)
			? w
			: SwingUtilities.getWindowAncestor(context.owner());

		final ProgressDialog progress = new ProgressDialog(owner, I18N.t("dialog.import.gedcom.title"), I18N.tf("dialog.import.progress", file.getName()));

		final SwingWorker<FLEFModel, Void> worker = new SwingWorker<>(){
			@Override
			protected FLEFModel doInBackground() throws Exception{
				progress.update(-1, I18N.t("progress.reading_file"));
				final String gedcomContent;
				try(final BufferedReader br = GEDCOMHelper.getBufferedReader(
					new FileInputStream(file))){
					gedcomContent = br.lines()
						.collect(Collectors.joining(System.lineSeparator()));
				}

				progress.update(-1, I18N.t("progress.parsing_gedcom"));
				final GEDCOMParser parser = new GEDCOMParser();
				final List<GEDCOMNode> roots;
				try(final StringReader reader = new StringReader(gedcomContent)){
					roots = parser.parse(reader);
				}

				final GEDCOMToFLEFConverter converter = new GEDCOMToFLEFConverter();
				return converter.convert(roots, progress::update);
			}

			@Override
			protected void done(){
				// We are on the EDT here: close the dialog first, then
				// save the imported model and open the saved file.
				progress.close();

				try{
					final FLEFModel imported = get();
					context.openImported(imported, file);
				}
				catch(final InterruptedException ex){
					Thread.currentThread().interrupt();
				}
				catch(final ExecutionException ex){
					final Throwable cause = (ex.getCause() != null)? ex.getCause(): ex;
					JOptionPane.showMessageDialog(context.owner(),
						I18N.l("error.import.gedcom.invalid") + StringUtils.LF + cause.getMessage(), I18N.t("error.import.title"), JOptionPane.ERROR_MESSAGE);
				}
			}
		};

		worker.execute();
		// Modal: blocks the caller until done() disposes the dialog.
		progress.setVisible(true);
	}

}
