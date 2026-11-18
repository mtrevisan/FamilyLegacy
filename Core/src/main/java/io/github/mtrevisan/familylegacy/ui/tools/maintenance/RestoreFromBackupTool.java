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
package io.github.mtrevisan.familylegacy.ui.tools.maintenance;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.dialogs.ProgressDialog;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;


/**
 * Loads a FLEF file (typically a backup) and replaces the current model
 * with it. The change is immediate and irreversible: the tool asks for
 * confirmation before doing anything.
 * <p>
 * The unsaved changes of the open document are offered for saving first. The file may be plain or compressed text, as with Open, and is
 * read in the background, with a progress dialog.
 * <p>
 * The restored model becomes the new in-memory model, and as every change it is written to the file of the document: the content
 * of that file is replaced with the backup, which is said in the confirmation. A document that has no file is asked where to be written.
 */
public final class RestoreFromBackupTool implements ToolOperation{

	@Override
	public String getName(){
		return I18N.t("menu.file.restore");
	}

	@Override
	public void run(final ToolContext context){
		if(context == null)
			return;

		final JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(I18N.t("dialog.restore.choose"));
		chooser.setFileFilter(new FileNameExtensionFilter(I18N.t("file.filter.flef"), "flef"));
		if(chooser.showOpenDialog(context.owner()) != JFileChooser.APPROVE_OPTION)
			return;

		final File file = chooser.getSelectedFile();
		if(file == null)
			return;
		if(!file.exists()){
			JOptionPane.showMessageDialog(context.owner(), I18N.tf("error.file.not_found", file.getAbsolutePath()),
				I18N.t("dialog.restore.title"), JOptionPane.ERROR_MESSAGE);

			return;
		}

		final int choice = JOptionPane.showConfirmDialog(context.owner(),
			I18N.tf("confirmation.restore_from_backup.message", file.getName()),
			I18N.t("confirmation.restore_from_backup.title"),
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if(choice != JOptionPane.YES_OPTION)
			return;

		//the open document is about to be replaced
		if(!context.confirmDiscardChanges())
			return;

		read(context, file);
	}

	private static void read(final ToolContext context, final File file){
		final ProgressDialog progress = new ProgressDialog(context.owner(), I18N.t("dialog.restore.title"),
			I18N.tf("dialog.file.open.progress", file.getName()));

		final SwingWorker<FLEFModel, Void> worker = new SwingWorker<>(){
			@Override
			protected FLEFModel doInBackground() throws Exception{
				progress.update(0, I18N.t("progress.reading_file"));

				return new FLEFParser().parse(file.toPath(), progress::update);
			}

			@Override
			protected void done(){
				progress.close();

				try{
					context.restoreModel(get());
				}
				catch(final InterruptedException ex){
					Thread.currentThread().interrupt();
				}
				catch(final ExecutionException ex){
					final Throwable cause = (ex.getCause() != null? ex.getCause(): ex);
					JOptionPane.showMessageDialog(context.owner(),
						I18N.l(cause instanceof IOException? "dialog.restore.error.read": "dialog.restore.error.invalid") + StringUtils.LF
							+ cause.getMessage(), I18N.t("dialog.restore.title"), JOptionPane.ERROR_MESSAGE);
				}
			}
		};

		worker.execute();
		//modal: blocks the caller until done() disposes the dialog
		progress.setVisible(true);
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasModel());
	}

}
