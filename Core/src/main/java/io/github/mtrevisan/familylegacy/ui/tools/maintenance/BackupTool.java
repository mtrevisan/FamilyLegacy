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

import io.github.mtrevisan.familylegacy.backup.ProjectArchive;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.FLEFWriter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.preferences.AppPreferences;
import io.github.mtrevisan.familylegacy.preferences.TagStore;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.ProgressDialog;
import io.github.mtrevisan.familylegacy.ui.helpers.SavedFileNotice;
import io.github.mtrevisan.familylegacy.util.AtomicFileWriter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;


/**
 * Saves a copy of the current model, as a ZIP archive of the project (the default) or as a single FLEF file. The name of the file is
 * pre-filled with a timestamp so a sequence of backups never overwrites itself.
 * <p>
 * The ZIP holds the FLEF file, the folder of the data that the application keeps next to it (tags, bookmarks, ignored findings) and, if
 * the user asks, the local media files: one file, nothing to forget when it is copied or sent (see {@link ProjectArchive}). The single
 * FLEF file has the data folder written beside it.
 * <p>
 * Backup is a document-level operation: it does not change the current
 * file, it does not clear the dirty flag, and it does not touch the
 * recent files list. The model is written exactly as it is, without
 * running the validator: if the model has errors, they are preserved,
 * so a backup can be used to recover from a bad edit.
 * <p>
 * The file is written in the background, with a progress dialog, as Save does. A backup saved in the folder of the project is not
 * forbidden, but the user is told that it does not protect from the loss of the disk.
 */
public final class BackupTool implements ToolOperation{

	private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
	private static final String FLEF_EXTENSION = "flef";
	private static final String INCLUDE_MEDIA_KEY = "backup.include_media";


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
			MessageDialogs.show(context.owner(),
				I18N.t("message.no_model_loaded"), I18N.t("dialog.backup.title"), JOptionPane.WARNING_MESSAGE);

			return;
		}

		final String documentPath = context.documentPath();
		final String prefix = (documentPath != null? FilenameUtils.getBaseName(documentPath) + "-": StringUtils.EMPTY);
		final String baseName = prefix + "backup-" + TS.format(LocalDateTime.now());

		final FileNameExtensionFilter zipFilter = new FileNameExtensionFilter(I18N.t("file.filter.backup.zip"), ProjectArchive.EXTENSION);
		final FileNameExtensionFilter flefFilter = new FileNameExtensionFilter(I18N.t("file.filter.backup"), FLEF_EXTENSION);
		final JCheckBox includeMedia = new JCheckBox(I18N.t("dialog.backup.include_media"),
			Boolean.parseBoolean(AppPreferences.getInstance().stringValue(INCLUDE_MEDIA_KEY, Boolean.FALSE.toString())));
		includeMedia.setToolTipText(I18N.t("dialog.backup.include_media.tooltip"));

		File target;
		boolean zip;
		while(true){
			final JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle(I18N.t("dialog.backup.choose"));
			chooser.setAcceptAllFileFilterUsed(false);
			chooser.addChoosableFileFilter(zipFilter);
			chooser.addChoosableFileFilter(flefFilter);
			chooser.setFileFilter(zipFilter);
			chooser.setAccessory(includeMedia);
			chooser.setSelectedFile(new File(baseName + "." + ProjectArchive.EXTENSION));
			//the name follows the format, and the media are a matter of the archive only
			chooser.addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY, e -> {
				final boolean archive = (chooser.getFileFilter() == zipFilter);
				includeMedia.setEnabled(archive);
				final File selected = chooser.getSelectedFile();
				final String name = (selected != null? FilenameUtils.getBaseName(selected.getName()): baseName);
				chooser.setSelectedFile(new File(name + "." + (archive? ProjectArchive.EXTENSION: FLEF_EXTENSION)));
			});
			if(chooser.showSaveDialog(context.owner()) != JFileChooser.APPROVE_OPTION)
				return;

			target = chooser.getSelectedFile();
			//what the user typed decides, else the format chosen in the list
			if(Strings.CI.endsWith(target.getName(), "." + ProjectArchive.EXTENSION))
				zip = true;
			else if(Strings.CI.endsWith(target.getName(), "." + FLEF_EXTENSION))
				zip = false;
			else{
				zip = (chooser.getFileFilter() == zipFilter);
				target = new File(target.getParentFile(), target.getName() + "." + (zip? ProjectArchive.EXTENSION: FLEF_EXTENSION));
			}

			if(isInFolderOfProject(documentPath, target)){
				final int another = MessageDialogs.confirm(context.owner(),
					I18N.tf("confirmation.backup_in_project.message", target.getAbsoluteFile().getParent()),
					I18N.t("confirmation.backup_in_project.title"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if(another == JOptionPane.YES_OPTION)
					continue;
			}
			break;
		}

		if(target.exists()){
			final int choice = MessageDialogs.confirm(context.owner(),
				I18N.t("confirmation.overwrite.backup.message"),
				I18N.t("confirmation.overwrite.backup.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(choice != JOptionPane.YES_OPTION)
				return;
		}

		final boolean withMedia = zip && includeMedia.isSelected();
		AppPreferences.getInstance().setStringValue(INCLUDE_MEDIA_KEY, Boolean.toString(includeMedia.isSelected()));
		write(context, model, target, documentPath, zip, withMedia);
	}

	/** @return	Whether the file is in the same folder as the FLEF file of the document. */
	private static boolean isInFolderOfProject(final String documentPath, final File target){
		if(documentPath == null)
			return false;

		final Path project = Path.of(documentPath).toAbsolutePath().normalize().getParent();
		final Path backup = target.toPath().toAbsolutePath().normalize().getParent();
		return (project != null && project.equals(backup));
	}

	private static void write(final ToolContext context, final FLEFModel model, final File target, final String documentPath,
			final boolean zip, final boolean withMedia){
		final ProgressDialog progress = new ProgressDialog(context.owner(), I18N.t("dialog.backup.title"),
			I18N.tf("dialog.file.save.progress", target.getName()));

		//what the user is told besides where the backup is
		final SwingWorker<String, Void> worker = new SwingWorker<>(){
			@Override
			protected String doInBackground() throws Exception{
				progress.update(0, I18N.t("progress.serializing"));
				if(zip){
					final ProjectArchive.WriteResult result = ProjectArchive.write(target.toPath(), model, documentPath, withMedia,
						progress::update);
					return summary(result, withMedia);
				}

				AtomicFileWriter.write(target.toPath(), StandardCharsets.UTF_8,
					writer -> FLEFWriter.createCompact().write(model, writer, progress::update));
				//what the application keeps next to the file (bookmarks, ignored findings) travels with the backup...
				if(documentPath != null)
					TagStore.copyMetaDirectory(documentPath, target.getAbsolutePath());
				//...and so do the tags: the model carries them
				new TagStore().save(target.getAbsolutePath(), model.tags());
				return I18N.t("dialog.backup.single_file_note");
			}

			@Override
			protected void done(){
				progress.close();

				try{
					final String note = get();

					SavedFileNotice.show(context.owner(), I18N.t("dialog.backup.title"),
						I18N.l("dialog.backup.saved") + StringUtils.LF + target.getAbsolutePath()
							+ (note.isEmpty()? StringUtils.EMPTY: StringUtils.LF + StringUtils.LF + note), target);
				}
				catch(final InterruptedException ex){
					Thread.currentThread().interrupt();
				}
				catch(final ExecutionException ex){
					final Throwable cause = (ex.getCause() != null? ex.getCause(): ex);
					MessageDialogs.show(context.owner(),
						I18N.l("dialog.backup.error") + StringUtils.LF + cause.getMessage(), I18N.t("dialog.backup.title"),
						JOptionPane.ERROR_MESSAGE);
				}
			}
		};

		worker.execute();
		//modal: blocks the caller until done() disposes the dialog
		progress.setVisible(true);
	}

	/** @return	What happened to the media of an archive (nothing if they were not asked for). */
	static String summary(final ProjectArchive.WriteResult result, final boolean withMedia){
		if(!withMedia)
			return StringUtils.EMPTY;

		final StringBuilder sb = new StringBuilder(I18N.tf("dialog.backup.media_summary", result.mediaIncluded()));
		if(result.mediaMissing() > 0)
			sb.append(StringUtils.LF).append(I18N.tf("dialog.backup.media_missing", result.mediaMissing()));
		if(result.mediaRemote() > 0)
			sb.append(StringUtils.LF).append(I18N.tf("dialog.backup.media_remote", result.mediaRemote()));
		return sb.toString();
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasModel());
	}

}
