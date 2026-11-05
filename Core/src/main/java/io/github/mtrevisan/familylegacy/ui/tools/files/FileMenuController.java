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
package io.github.mtrevisan.familylegacy.ui.tools.files;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.FLEFWriter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.ModelHistory;
import io.github.mtrevisan.familylegacy.preferences.MediaPathDisplay;
import io.github.mtrevisan.familylegacy.ui.dialogs.ProgressDialog;
import io.github.mtrevisan.familylegacy.util.AtomicFileWriter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Cursor;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * Coordinates the operations of the {@code File} menu.
 * <p>
 * The menu is different from the other menus of the application: it
 * does not operate on the model, it operates on the <b>document</b>
 * (the file the model came from). The controller therefore owns a
 * shared state that no other menu has: the current file.
 * <p>
 * <b>There are no unsaved changes.</b> Every change to the model is written to the file of the document by itself, a fraction of a
 * second after it is made (see {@link #documentChanged()}): the file is always, but for that moment, what is in memory, and no
 * command asks whether to save. What replaces the document or ends the application (New, Open, Exit, Import, Restore) first
 * writes what is still pending ({@link #confirmDiscardChanges()}).
 * <p>
 * The only document that has no file is the empty one the application starts with. At the first change the user is asked where to
 * write it, and from then on it is saved as the others. If the user declines, the document stays in memory, and the changes are lost
 * unless the user chooses a file when closing or replacing it (the question is asked then).
 * <p>
 * The controller delegates all the model-specific work to the enclosing
 * frame through the callbacks and the {@link ModelHistory}, that tells whether the model differs from what is on the file.
 */
public final class FileMenuController{

	private static final Logger LOGGER = LoggerFactory.getLogger(FileMenuController.class);

	private static final String FLEF_EXTENSION = "flef";
	/** How long a change waits for others before it is written, so that a burst of edits makes a single write. */
	private static final int AUTOSAVE_DELAY = 600;


	private final JFrame owner;
	private final Supplier<FLEFModel> currentModel;
	private final Consumer<FLEFModel> modelReplacer;
	private final ModelHistory history;

	private final RecentFilesManager recentFiles = new RecentFilesManager();

	private final Timer autosaveTimer;
	/** One thread writes, so that the writes are in the order the changes were made. */
	private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
		final Thread thread = new Thread(runnable, "autosave");
		thread.setDaemon(true);
		return thread;
	});

	private File currentFile;

	/** Last directory used by a file chooser, so the next one opens there. */
	private File lastDirectory;

	/** The user did not choose a file for the document that has none: no more asking until it is closed or replaced. */
	private boolean saveDeclined;
	/** An automatic save failed and the user knows: no more asking until one succeeds. */
	private boolean failureReported;


	/**
	 * @param owner	The window of the application.
	 * @param currentModel	The model that is open.
	 * @param modelReplacer	Replaces the open model (and the history) with another.
	 * @param history	The history of the open model: it knows whether what is in memory is what is on the file.
	 */
	public FileMenuController(final JFrame owner, final Supplier<FLEFModel> currentModel, final Consumer<FLEFModel> modelReplacer,
			final ModelHistory history){
		this.owner = owner;
		this.currentModel = currentModel;
		this.modelReplacer = modelReplacer;
		this.history = history;

		autosaveTimer = new Timer(AUTOSAVE_DELAY, e -> autoSave());
		autosaveTimer.setRepeats(false);
	}


	/* ======================================================================
	 *                          State queries
	 * ====================================================================== */

	/** Returns the file currently open, or {@code null} for a document that has none yet. */
	public File currentFile(){
		return currentFile;
	}

	/** Returns the recent files manager, for the Recent submenu. */
	public RecentFilesManager recentFiles(){
		return recentFiles;
	}

	private void setCurrentFile(final File file){
		currentFile = file;
		saveDeclined = false;
		failureReported = false;
		MediaPathDisplay.setDocumentFile(file);
		if(file != null){
			lastDirectory = file.getParentFile();
			recentFiles.add(file);
		}
	}


	/* ======================================================================
	 *                          Saving by itself
	 * ====================================================================== */

	/**
	 * Tells that the model may have changed (a step of the history, an undo, a redo, a restored backup): if it differs from the file,
	 * it is written soon. Call it on the event dispatch thread; it is quick.
	 */
	public void documentChanged(){
		if(!history.isModified())
			return;

		if(currentFile == null){
			//not here: this is called while an event is being dispatched
			if(!saveDeclined)
				SwingUtilities.invokeLater(this::askWhereToSave);

			return;
		}

		autosaveTimer.restart();
	}

	/** Writes the changes to the file, in the background, from the copy of the model that the history gives. */
	private void autoSave(){
		if(currentFile == null || !history.isModified())
			return;

		final File target = currentFile;
		final ModelHistory.SaveTicket ticket = history.prepareSave();
		writer.execute(() -> {
			try{
				write(target, ticket.view());

				SwingUtilities.invokeLater(() -> {
					failureReported = false;
					//a file the document has left in the meantime (Save as) is not the one that is up to date
					if(target.equals(currentFile))
						history.markSaved(ticket);
				});
			}
			catch(final IOException | RuntimeException e){
				SwingUtilities.invokeLater(() -> reportAutosaveFailure(e));
			}
		});
	}

	private void reportAutosaveFailure(final Exception e){
		LOGGER.warn("Automatic save of {} failed", currentFile, e);

		//it is tried again at the next change; the user is told once
		if(failureReported)
			return;

		failureReported = true;
		showError(I18N.t("error.file.autosave"), e);
	}

	/** Asks where to write a document that has no file, once it has something to write. */
	private void askWhereToSave(){
		if(currentFile != null || saveDeclined || !history.isModified())
			return;

		final File target = chooseSaveTarget(I18N.t("dialog.file.autosave.title"), lastDirectory, null);
		if(target == null){
			saveDeclined = true;
			return;
		}

		if(writeTo(target))
			setCurrentFile(target);
	}

	/**
	 * Writes what is still pending, now, before the document is replaced or the application ends.
	 *
	 * @return	{@code true} if nothing is lost and the operation can go on; {@code false} if the user cancelled it, because the document
	 * 	has no file and the user did not want to choose one, or because the changes could not be written.
	 */
	public boolean confirmDiscardChanges(){
		autosaveTimer.stop();
		if(currentModel.get() == null)
			return true;

		if(currentFile == null)
			return history.isModified()? askWhatToDoWithTheDocument(): true;

		//what is being written in the background first, then what is left
		try{
			writer.submit(() -> {})
				.get();
		}
		catch(final InterruptedException ex){
			Thread.currentThread().interrupt();

			return false;
		}
		catch(final ExecutionException ignored){
			//nothing was being written that matters here: what is left is written below
		}
		if(!history.isModified())
			return true;

		final Cursor cursor = owner.getCursor();
		owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try{
			final ModelHistory.SaveTicket ticket = history.prepareSave();
			write(currentFile, ticket.view());
			history.markSaved(ticket);
			return true;
		}
		catch(final IOException | RuntimeException e){
			return JOptionPane.showConfirmDialog(owner,
				I18N.tf("confirmation.autosave_failed.message", e.getMessage()), I18N.t("confirmation.autosave_failed.title"),
				JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) == JOptionPane.YES_OPTION;
		}
		finally{
			owner.setCursor(cursor);
		}
	}

	/** The document has changes and no file: choose one, lose them, or give up what was being done. */
	private boolean askWhatToDoWithTheDocument(){
		final int choice = JOptionPane.showOptionDialog(owner,
			I18N.t("dialog.unsaved.message"), I18N.t("dialog.unsaved.title"),
			JOptionPane.YES_NO_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE,
			null,
			new Object[]{I18N.t("button.save"), I18N.t("button.discard"), I18N.t("button.cancel")},
			I18N.t("button.save"));

		if(choice == 2 || choice == JOptionPane.CLOSED_OPTION)
			return false;
		if(choice == 0)
			return saveAs();

		// choice == 1: discard
		return true;
	}


	/* ======================================================================
	 *                          New / Open
	 * ====================================================================== */

	/** Creates a new empty document: the user chooses the file it is written to. */
	public void newFile(){
		if(!confirmDiscardChanges())
			return;

		final File target = chooseSaveTarget(I18N.t("dialog.file.new.title"), lastDirectory, null);
		if(target == null)
			return;

		final FLEFModel model = new FLEFModel();
		if(!writeTo(target, model))
			return;

		modelReplacer.accept(model);
		setCurrentFile(target);
	}

	/** Opens a file through a chooser. */
	public void openFile(){
		if(!confirmDiscardChanges())
			return;

		final JFileChooser chooser = new JFileChooser(lastDirectory);
		chooser.setDialogTitle(I18N.t("dialog.file.open.title"));
		chooser.setFileFilter(new FileNameExtensionFilter(I18N.t("file.filter.flef"), FLEF_EXTENSION));
		if(chooser.showOpenDialog(owner) != JFileChooser.APPROVE_OPTION)
			return;

		openFile(chooser.getSelectedFile());
	}

	/**
	 * Opens a file of the recent files list. A file that is not there any more is not opened, and the user is asked whether to remove
	 * it from the list.
	 */
	public void openRecent(final File file){
		if(file == null)
			return;

		if(!file.exists()){
			reportMissingFile(file);

			return;
		}

		if(!confirmDiscardChanges())
			return;

		openFile(file);
	}

	/** Opens the given file directly, without a chooser (and without writing what is pending: the callers do). */
	public void openFile(final File file){
		if(file == null)
			return;
		if(!file.exists()){
			reportMissingFile(file);

			return;
		}

		final Window ownerWindow = owner;

		// Must be created on the EDT, before the worker starts. The
		// dialog is modal but pumps events while visible, so the worker
		// can update it from its own thread via update().
		final ProgressDialog progress = new ProgressDialog(ownerWindow,
			I18N.t("dialog.file.open.title"), I18N.tf("dialog.file.open.progress", file.getName()));

		final SwingWorker<FLEFModel, Void> worker = new SwingWorker<>(){
			@Override
			protected FLEFModel doInBackground() throws Exception{
				progress.update(0, I18N.t("progress.reading_file"));

				final FLEFParser parser = new FLEFParser();
				return parser.parse(file.toPath(), progress::update);
			}

			@Override
			protected void done(){
				// We are on the EDT: close the dialog, then apply the result.
				progress.close();

				try{
					final FLEFModel model = get();

					modelReplacer.accept(model);
					setCurrentFile(file);
				}
				catch(final InterruptedException ex){
					Thread.currentThread().interrupt();
				}
				catch(final ExecutionException ex){
					final Throwable cause = (ex.getCause() != null? ex.getCause(): ex);
					if(cause instanceof IOException io)
						showError(I18N.t("error.file.open"), io);
					else
						showError(I18N.t("error.file.invalid_flef"),
							(cause instanceof RuntimeException re)? re: new RuntimeException(cause));
				}
			}
		};

		worker.execute();
		// Modal: blocks the caller until done() disposes the dialog.
		progress.setVisible(true);
	}

	/**
	 * Tells the user that the file is not there. If it is in the recent files, asks whether to remove it from the list (the list
	 * keeps the files that were opened, and one can be moved or deleted in the meantime).
	 */
	private void reportMissingFile(final File file){
		if(!recentFiles.contains(file)){
			JOptionPane.showMessageDialog(owner, I18N.tf("error.file.not_found", file.getAbsolutePath()), I18N.t("error.file.title"),
				JOptionPane.ERROR_MESSAGE);

			return;
		}

		final int remove = JOptionPane.showConfirmDialog(owner,
			I18N.tf("confirmation.recent_file_missing.message", file.getAbsolutePath()),
			I18N.t("confirmation.recent_file_missing.title"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		if(remove == JOptionPane.YES_OPTION)
			recentFiles.remove(file);
	}


	/* ======================================================================
	 *                          Save as
	 * ====================================================================== */

	/**
	 * Writes the document to a file chosen by the user, which becomes the file of the document: the changes are saved to it from now
	 * on, and the previous file is left as it was last written.
	 */
	public boolean saveAs(){
		final File target = chooseSaveTarget(I18N.t("dialog.file.save.title"), lastDirectory, currentFile);
		if(target == null)
			return false;

		if(writeTo(target)){
			setCurrentFile(target);
			return true;
		}
		return false;
	}


	/* ======================================================================
	 *                          Import
	 * ====================================================================== */

	/**
	 * Saves a model imported from another format (GEDCOM, Gramps XML) as a new FLEF file, chosen by the user, and opens that file
	 * as the current document.
	 * <p>
	 * The file chooser starts next to the imported file, with its name and the FLEF extension. If the user cancels the chooser, the
	 * import is discarded and the current document is left as it is.
	 *
	 * @param source	The file the model was imported from.
	 */
	public void openImported(final FLEFModel importedModel, final File source){
		final File suggested = new File(source.getParentFile(),
			FilenameUtils.getBaseName(source.getName()) + "." + FLEF_EXTENSION);
		final File target = chooseSaveTarget(I18N.t("dialog.file.save_imported.title"), source.getParentFile(), suggested);
		if(target == null)
			return;

		// the document is the saved file, read back as any other file
		if(writeTo(target, importedModel))
			openFile(target);
	}


	/**
	 * Asks the user for the FLEF file to save to, appending the extension when not typed and confirming the overwrite of an
	 * existing file.
	 *
	 * @param directory	The directory the chooser starts in, may be {@code null}.
	 * @param suggested	The file initially selected, may be {@code null}.
	 * @return	The file, or {@code null} if the user cancelled.
	 */
	private File chooseSaveTarget(final String title, final File directory, final File suggested){
		final JFileChooser chooser = new JFileChooser(directory);
		chooser.setDialogTitle(title);
		chooser.setFileFilter(new FileNameExtensionFilter(I18N.t("file.filter.flef"), FLEF_EXTENSION));
		if(suggested != null)
			chooser.setSelectedFile(suggested);
		if(chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION)
			return null;

		File target = chooser.getSelectedFile();
		// Append the FLEF extension when the user did not type one.
		if(!target.getName().toLowerCase().endsWith("." + FLEF_EXTENSION))
			target = new File(target.getParentFile(), target.getName() + "." + FLEF_EXTENSION);

		if(target.exists()){
			final int confirm = JOptionPane.showConfirmDialog(owner,
				I18N.t("confirmation.overwrite.save_as.message"),
				I18N.t("confirmation.overwrite.save_as.title"),
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
			if(confirm != JOptionPane.YES_OPTION)
				return null;
		}
		return target;
	}


	/* ======================================================================
	 *                          Exit
	 * ====================================================================== */

	/**
	 * Closes the window of the application, after what is pending has been written.
	 *
	 * @return	Whether the window was closed: {@code false} if the user cancelled, or the changes could not be written.
	 */
	public boolean exit(){
		if(!confirmDiscardChanges())
			return false;

		owner.dispose();
		return true;
	}


	/* ======================================================================
	 *                          Properties
	 * ====================================================================== */

	/** Opens the properties dialog for the current document. */
	public void showProperties(){
		FilePropertiesDialog.show(owner, currentFile, currentModel.get());
	}


	/* ======================================================================
	 *                          Internal
	 * ====================================================================== */

	private boolean writeTo(final File file){
		return writeTo(file, currentModel.get());
	}

	/** Writes the model with a progress window (a user asked for it, and waits for it). */
	private boolean writeTo(final File file, final FLEFModel model){
		if(model == null)
			return false;

		final ProgressDialog progress = new ProgressDialog(owner,
			I18N.t("dialog.file.save.title"), I18N.tf("dialog.file.save.progress", file.getName()));

		// Holder for the result, read after the modal dialog returns.
		final boolean[] success = { false };

		final SwingWorker<Void, Void> worker = new SwingWorker<>(){
			@Override
			protected Void doInBackground() throws Exception{
				progress.update(0, I18N.t("progress.serializing"));
				final String content = FLEFWriter.createCompact()
					.writeToString(model, progress::update);

				// The file write itself is a single blocking call, so we
				// can only show it as an indeterminate step.
				progress.update(-1, I18N.t("progress.writing_file"));
				AtomicFileWriter.write(file.toPath(), content, StandardCharsets.UTF_8);
				return null;
			}

			@Override
			protected void done(){
				progress.close();
				try{
					get();
					//only the open document is saved by this: an imported model, written to its new file, is not the open one
					if(model == currentModel.get())
						history.markSaved();
					success[0] = true;
				}
				catch(final InterruptedException ex){
					Thread.currentThread().interrupt();
				}
				catch(final ExecutionException ex){
					final Throwable cause = (ex.getCause() != null? ex.getCause(): ex);
					if(cause instanceof IOException io)
						showError(I18N.t("error.file.save"), io);
					else
						showError(I18N.t("error.file.save"), new RuntimeException(cause));
				}
			}
		};

		worker.execute();
		// Modal: pumps events until done() disposes the dialog.
		progress.setVisible(true);

		return success[0];
	}

	/** Writes the model, on the calling thread. */
	private static void write(final File file, final FLEFModel model) throws IOException{
		final String content = FLEFWriter.createCompact()
			.writeToString(model);
		AtomicFileWriter.write(file.toPath(), content, StandardCharsets.UTF_8);
	}

	private void showError(final String message, final Exception ex){
		JOptionPane.showMessageDialog(owner,
			message + ":" + StringUtils.LF + ex.getMessage(),
			I18N.t("error.file.title"), JOptionPane.ERROR_MESSAGE);
	}

}
