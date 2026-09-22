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
package io.github.mtrevisan.familylegacy.v2.ui.tools.files;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.FLEFWriter;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.ProgressDialog;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * Coordinates the operations of the {@code File} menu.
 * <p>
 * The menu is different from the other menus of the application: it
 * does not operate on the model, it operates on the <b>document</b>
 * (the file the model came from). The controller therefore owns a
 * shared state that no other menu has: the current file and the dirty
 * flag.
 * <p>
 * The controller delegates all the model-specific work to the enclosing
 * frame through three callbacks:
 * <ul>
 *   <li>{@code currentModel} — returns the model currently loaded, or
 *       {@code null} when no document is open;</li>
 *   <li>{@code modelReplacer} — replaces the current model with a new
 *       one, rebuilding the frame's UI around it;</li>
 *   <li>{@code modelClean} — marks the current model as clean, called
 *       after a successful save.</li>
 * </ul>
 * The controller is the single point that knows the current file and
 * the dirty flag; the frame only knows the model.
 * <p>
 * <b>Unsaved changes.</b> Every operation that discards the current
 * document (New, Open, Exit) prompts the user when the model is dirty.
 * The prompt offers three choices: save, discard, cancel. The
 * controller returns {@code false} to cancel the operation when the
 * user chooses cancel, or when the save fails.
 */
public final class FileMenuController{

	private static final String FLEF_EXTENSION = "flef";
	private static final String GEDCOM_EXTENSION = "ged";


	private final JFrame owner;
	private final Supplier<FLEFModel> currentModel;
	private final Consumer<FLEFModel> modelReplacer;
	private final Runnable modelClean;

	private final RecentFilesManager recentFiles = new RecentFilesManager();

	private File currentFile;
	private boolean dirty;

	/** Last directory used by a file chooser, so the next one opens there. */
	private File lastDirectory;


	public FileMenuController(final JFrame owner,
		final Supplier<FLEFModel> currentModel,
		final Consumer<FLEFModel> modelReplacer,
		final Runnable modelClean){
		this.owner = Objects.requireNonNull(owner);
		this.currentModel = Objects.requireNonNull(currentModel);
		this.modelReplacer = Objects.requireNonNull(modelReplacer);
		this.modelClean = (modelClean != null? modelClean: () -> {});
	}


	/* ======================================================================
	 *                          State queries
	 * ====================================================================== */

	/** Returns the file currently open, or {@code null} for an unsaved document. */
	public File currentFile(){
		return currentFile;
	}

	/** Returns whether the current document has unsaved changes. */
	public boolean isDirty(){
		return dirty;
	}

	/** Marks the current document as dirty. Called when the model is mutated. */
	public void markDirty(){
		dirty = true;
	}

	/** Returns the recent files manager, for the Recent submenu. */
	public RecentFilesManager recentFiles(){
		return recentFiles;
	}


	/* ======================================================================
	 *                          New / Open
	 * ====================================================================== */

	/** Creates a new empty document. */
	public void newFile(){
		if(!confirmDiscardChanges())
			return;

		modelReplacer.accept(new FLEFModel());
		currentFile = null;
		dirty = false;
	}

	/** Opens a file through a chooser. */
	public void openFile(){
		if(!confirmDiscardChanges())
			return;

		final JFileChooser chooser = new JFileChooser(lastDirectory);
		chooser.setDialogTitle("Open FLEF file");
		chooser.setFileFilter(new FileNameExtensionFilter("FLEF files (*.flef)", FLEF_EXTENSION));
		if(chooser.showOpenDialog(owner) != JFileChooser.APPROVE_OPTION)
			return;

		openFile(chooser.getSelectedFile());
	}

	/** Opens the given file directly, without a chooser. */
	public void openFile(final File file){
		if(file == null || !file.exists())
			return;

		final Window ownerWindow = owner;

		// Must be created on the EDT, before the worker starts. The
		// dialog is modal but pumps events while visible, so the worker
		// can update it from its own thread via update().
		final ProgressDialog progress = new ProgressDialog(ownerWindow,
			"Open FLEF file", "Opening " + file.getName() + "…");

		final SwingWorker<FLEFModel, Void> worker = new SwingWorker<>(){
			@Override
			protected FLEFModel doInBackground() throws Exception{
				progress.update(0, "Reading file…");
				return new FLEFParser()
					.parse(file.toPath(), progress::update);
			}

			@Override
			protected void done(){
				// We are on the EDT: close the dialog, then apply the result.
				progress.close();

				try{
					final FLEFModel model = get();

					modelReplacer.accept(model);
					currentFile = file;
					lastDirectory = file.getParentFile();
					dirty = false;
					recentFiles.add(file);
				}
				catch(final InterruptedException ex){
					Thread.currentThread().interrupt();
				}
				catch(final ExecutionException ex){
					final Throwable cause = (ex.getCause() != null? ex.getCause(): ex);
					if(cause instanceof IOException io)
						showError("Unable to open the file", io);
					else
						showError("The file is not a valid FLEF document",
							(cause instanceof RuntimeException re)? re: new RuntimeException(cause));
				}
			}
		};

		worker.execute();
		// Modal: blocks the caller until done() disposes the dialog.
		progress.setVisible(true);
	}


	/* ======================================================================
	 *                          Save
	 * ====================================================================== */

	/** Saves the current document. If it has no file yet, prompts for one. */
	public boolean save(){
		if(currentFile == null)
			return saveAs();

		return writeTo(currentFile);
	}

	/** Saves the current document under a new file, chosen by the user. */
	public boolean saveAs(){
		final JFileChooser chooser = new JFileChooser(lastDirectory);
		chooser.setDialogTitle("Save FLEF file");
		chooser.setFileFilter(new FileNameExtensionFilter("FLEF files (*.flef)", FLEF_EXTENSION));
		if(currentFile != null)
			chooser.setSelectedFile(currentFile);
		if(chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION)
			return false;

		File target = chooser.getSelectedFile();
		// Append the FLEF extension when the user did not type one.
		if(!target.getName().toLowerCase().endsWith("." + FLEF_EXTENSION))
			target = new File(target.getParentFile(), target.getName() + "." + FLEF_EXTENSION);

		if(target.exists()){
			final int confirm = JOptionPane.showConfirmDialog(owner,
				"The file already exists. Overwrite it?",
				"Confirm Overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(confirm != JOptionPane.YES_OPTION)
				return false;
		}

		if(writeTo(target)){
			currentFile = target;
			lastDirectory = target.getParentFile();
			recentFiles.add(target);
			return true;
		}
		return false;
	}


	/* ======================================================================
	 *                          Exit
	 * ====================================================================== */

	/** Closes the application, prompting for unsaved changes first. */
	public void exit(){
		if(!confirmDiscardChanges())
			return;
		owner.dispose();
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
		final FLEFModel model = currentModel.get();
		if(model == null)
			return false;

		final ProgressDialog progress = new ProgressDialog(owner,
			"Save FLEF file", "Saving " + file.getName() + "…");

		// Holder for the result, read after the modal dialog returns.
		final boolean[] success = { false };

		final SwingWorker<Void, Void> worker = new SwingWorker<>(){
			@Override
			protected Void doInBackground() throws Exception{
				progress.update(0, "Serializing…");
				final String content = FLEFWriter.createCompact()
					.writeToString(model, progress::update);

				// The file write itself is a single blocking call, so we
				// can only show it as an indeterminate step.
				progress.update(-1, "Writing file…");
				Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
				return null;
			}

			@Override
			protected void done(){
				progress.close();
				try{
					get();
					dirty = false;
					modelClean.run();
					success[0] = true;
				}
				catch(final InterruptedException ex){
					Thread.currentThread().interrupt();
				}
				catch(final ExecutionException ex){
					final Throwable cause = (ex.getCause() != null? ex.getCause(): ex);
					if(cause instanceof IOException io)
						showError("Unable to save the file", io);
					else
						showError("Unable to save the file", new RuntimeException(cause));
				}
			}
		};

		worker.execute();
		// Modal: pumps events until done() disposes the dialog.
		progress.setVisible(true);

		return success[0];
	}

	/**
	 * Prompts the user when the current document has unsaved changes.
	 * Returns {@code true} when the operation can proceed (either the
	 * model is clean, the user saved successfully, or the user chose to
	 * discard the changes) and {@code false} when the user cancelled.
	 */
	private boolean confirmDiscardChanges(){
		if(!dirty || currentModel.get() == null)
			return true;

		final int choice = JOptionPane.showOptionDialog(owner,
			"The current document has unsaved changes.\n"
				+ "Save before proceeding?",
			"Unsaved Changes",
			JOptionPane.YES_NO_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE,
			null,
			new Object[]{"Save", "Discard", "Cancel"},
			"Save");

		if(choice == 2 || choice == JOptionPane.CLOSED_OPTION)
			return false;
		if(choice == 0)
			return save();

		// choice == 1: discard
		return true;
	}

	private void showError(final String message, final Exception ex){
		JOptionPane.showMessageDialog(owner,
			message + ":\n" + ex.getMessage(),
			"File Error", JOptionPane.ERROR_MESSAGE);
	}

}
