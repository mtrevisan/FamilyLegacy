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
import io.github.mtrevisan.familylegacy.io.FLEFWriter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.dialogs.ProgressDialog;
import io.github.mtrevisan.familylegacy.ui.helpers.SavedFileNotice;
import io.github.mtrevisan.familylegacy.util.AtomicFileWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;


/**
 * Saves a copy of the current model to a user-chosen file. The name of
 * the file is pre-filled with a timestamp so a sequence of backups
 * never overwrites itself.
 * <p>
 * Backup is a document-level operation: it does not change the current
 * file, it does not clear the dirty flag, and it does not touch the
 * recent files list. The model is written exactly as it is, without
 * running the validator: if the model has errors, they are preserved,
 * so a backup can be used to recover from a bad edit.
 * <p>
 * The file is written in the background, with a progress dialog, as Save does.
 */
public final class BackupTool implements ToolOperation{

	private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");


	@Override
	public String getName(){
		return I18N.t("menu.file.backup");
	}

	@Override
	public void run(final ToolContext context){
		if(context == null)
			return;

		final FLEFModel model = context.model();
		if(model == null){
			JOptionPane.showMessageDialog(context.owner(),
				I18N.t("message.no_model_loaded"), I18N.t("dialog.backup.title"), JOptionPane.WARNING_MESSAGE);

			return;
		}

		final JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(I18N.t("dialog.backup.choose"));
		chooser.setFileFilter(new FileNameExtensionFilter(I18N.t("file.filter.backup"), "flef"));
		chooser.setSelectedFile(new File("backup-" + TS.format(LocalDateTime.now()) + ".flef"));
		if(chooser.showSaveDialog(context.owner()) != JFileChooser.APPROVE_OPTION)
			return;

		File target = chooser.getSelectedFile();
		if(!Strings.CI.endsWith(target.getName(), ".flef"))
			target = new File(target.getParentFile(), target.getName() + ".flef");

		if(target.exists()){
			final int choice = JOptionPane.showConfirmDialog(context.owner(),
				I18N.t("confirmation.overwrite.backup.message"),
				I18N.t("confirmation.overwrite.backup.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(choice != JOptionPane.YES_OPTION)
				return;
		}

		write(context, model, target);
	}

	private static void write(final ToolContext context, final FLEFModel model, final File target){
		final ProgressDialog progress = new ProgressDialog(context.owner(), I18N.t("dialog.backup.title"),
			I18N.tf("dialog.file.save.progress", target.getName()));

		final SwingWorker<Void, Void> worker = new SwingWorker<>(){
			@Override
			protected Void doInBackground() throws Exception{
				progress.update(0, I18N.t("progress.serializing"));
				final String content = FLEFWriter.createCompact()
					.writeToString(model, progress::update);

				progress.update(-1, I18N.t("progress.writing_file"));
				AtomicFileWriter.write(target.toPath(), content, StandardCharsets.UTF_8);
				return null;
			}

			@Override
			protected void done(){
				progress.close();

				try{
					get();

					SavedFileNotice.show(context.owner(), I18N.t("dialog.backup.title"),
						I18N.l("dialog.backup.saved") + StringUtils.LF + target.getAbsolutePath(), target);
				}
				catch(final InterruptedException ex){
					Thread.currentThread().interrupt();
				}
				catch(final ExecutionException ex){
					final Throwable cause = (ex.getCause() != null? ex.getCause(): ex);
					JOptionPane.showMessageDialog(context.owner(),
						I18N.l("dialog.backup.error") + StringUtils.LF + cause.getMessage(), I18N.t("dialog.backup.title"),
						JOptionPane.ERROR_MESSAGE);
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
