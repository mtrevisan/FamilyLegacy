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

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.importer.FileImporter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.dialogs.ProgressDialog;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Window;
import java.io.File;
import java.util.concurrent.ExecutionException;


/**
 * Imports a file of another genealogy program by converting it into a new FLEF file, which becomes the current document. The format is
 * the one of a {@link FileImporter}, a plugin: this tool is the same for all of them.
 * <p>
 * Once the conversion succeeds, the user chooses where to save the FLEF file; the file is written and then opened, as Open would do (see
 * {@link io.github.mtrevisan.familylegacy.ui.tools.files.FileMenuController#openImported}). Cancelling the choice discards the import.
 * <p>
 * The tool is deliberately simple: it never merges into the current model. A real merge-with-conflict-resolution flow would need a diff
 * engine and a resolution dialog, which are out of scope for this tool.
 */
public final class ImportFileTool implements ToolOperation{

	private static final Logger LOGGER = LoggerFactory.getLogger(ImportFileTool.class);

	private final FileImporter importer;


	public ImportFileTool(final FileImporter importer){
		this.importer = importer;
	}


	@Override
	public String getName(){
		return importer.menuLabel();
	}

	@Override
	public void run(final ToolContext context){
		final JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(importer.dialogTitle());
		chooser.setFileFilter(new FileNameExtensionFilter(importer.fileDescription(), importer.fileExtensions().toArray(String[]::new)));
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

		//the import replaces the open document: offer to save its changes first
		if(!context.confirmDiscardChanges())
			return;

		//resolve the owner Window (JDialog needs a Window, not a Component)
		final Window owner = (context.owner() instanceof Window w
			? w
			: SwingUtilities.getWindowAncestor(context.owner()));

		final ProgressDialog progress = new ProgressDialog(owner, importer.dialogTitle(),
			I18N.tf("dialog.import.progress", file.getName()));

		final SwingWorker<FLEFModel, Void> worker = new SwingWorker<>(){
			@Override
			protected FLEFModel doInBackground() throws Exception{
				return importer.read(file.toPath(), progress::update);
			}

			@Override
			protected void done(){
				//we are on the EDT here: close the dialog first, then save the imported model and open the saved file
				progress.close();

				try{
					final FLEFModel imported = get();
					context.openImported(imported, file);
				}
				catch(final InterruptedException ie){
					Thread.currentThread().interrupt();
				}
				catch(final ExecutionException ee){
					LOGGER.error("Import of {} failed", file, ee);
					final Throwable cause = (ee.getCause() != null? ee.getCause(): ee);
					JOptionPane.showMessageDialog(context.owner(),
						importer.invalidFileMessage() + StringUtils.LF + cause.getMessage(), I18N.t("error.import.title"),
						JOptionPane.ERROR_MESSAGE);
				}
			}
		};

		worker.execute();
		//modal: it blocks the caller until done() disposes the dialog
		progress.setVisible(true);
	}

}
