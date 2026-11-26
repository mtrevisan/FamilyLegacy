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
import io.github.mtrevisan.familylegacy.importer.ImportReport;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.ProgressDialog;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Window;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


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


	private final List<FileImporter> importers;


	/**
	 * @param importers	the importers that are installed (see {@link io.github.mtrevisan.familylegacy.importer.FileImporters#available()})
	 */
	public ImportFileTool(final List<FileImporter> importers){
		this.importers = List.copyOf(importers);
	}


	@Override
	public String getName(){
		return I18N.t(importers.isEmpty()? "import.format.none": "menu.file.import");
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return !importers.isEmpty();
	}

	@Override
	public void run(final ToolContext context){
		//one chooser for all the formats: the first filter shows everything that can be imported, the others narrow it to a format
		final JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(I18N.t("dialog.import.title"));
		chooser.setAcceptAllFileFilterUsed(false);
		final FileNameExtensionFilter allFormats = new FileNameExtensionFilter(I18N.t("import.files.all"), allExtensions());
		chooser.addChoosableFileFilter(allFormats);
		final Map<FileFilter, FileImporter> filters = new HashMap<>(importers.size());
		for(final FileImporter candidate : importers){
			final FileNameExtensionFilter filter = new FileNameExtensionFilter(candidate.fileDescription(),
				candidate.fileExtensions().toArray(String[]::new));
			chooser.addChoosableFileFilter(filter);
			filters.put(filter, candidate);
		}
		chooser.setFileFilter(allFormats);
		if(chooser.showOpenDialog(context.owner()) != JFileChooser.APPROVE_OPTION)
			return;

		final File file = chooser.getSelectedFile();

		//a format chosen in the filter is the user's word; otherwise the format is recognised from the file
		final FileImporter importer = (filters.containsKey(chooser.getFileFilter())
			? filters.get(chooser.getFileFilter())
			: recognise(context, file));
		if(importer == null)
			return;

		final int confirm = MessageDialogs.confirm(context.owner(),
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

		final ImportReport report = new ImportReport();

		final SwingWorker<FLEFModel, Void> worker = new SwingWorker<>(){
			@Override
			protected FLEFModel doInBackground() throws Exception{
				return importer.read(file.toPath(), progress::update, report);
			}

			@Override
			protected void done(){
				//we are on the EDT here: close the dialog first, then save the imported model and open the saved file
				progress.close();

				try{
					final FLEFModel imported = get();
					//the embedded files (BLOB) are written before the model is saved: its media are linked to them while it is not open yet
					resolveEmbeddedFiles(owner, imported, report, file.getParentFile());
					//cancelling the save discards the import: no report
					if(context.openImported(imported, file))
						//after the file is opened, tell what could not be kept
						ImportReportDialog.show(owner, report);
				}
				catch(final InterruptedException ie){
					Thread.currentThread()
						.interrupt();
				}
				catch(final ExecutionException ee){
					LOGGER.error("Import of {} failed", file, ee);
					final Throwable cause = (ee.getCause() != null? ee.getCause(): ee);
					MessageDialogs.show(context.owner(),
						importer.invalidFileMessage() + StringUtils.LF + cause.getMessage(), I18N.t("error.import.title"),
						JOptionPane.ERROR_MESSAGE);
				}
			}
		};

		worker.execute();
		//modal: it blocks the caller until done() disposes the dialog
		progress.setVisible(true);
	}

	private String[] allExtensions(){
		return importers.stream()
			.flatMap(importer -> importer.fileExtensions().stream())
			.distinct()
			.toArray(String[]::new);
	}

	/**
	 * Finds the importer of the file among the installed ones: the one that accepts it, or the one the user picks when more than one does.
	 * A file that no importer accepts is told to the user, with the formats that can be imported.
	 *
	 * @return	The importer, or {@code null} if there is none (already told to the user) or the user closed the question.
	 */
	private FileImporter recognise(final ToolContext context, final File file){
		final List<FileImporter> candidates = importers.stream()
			.filter(importer -> importer.accepts(file.toPath()))
			.toList();
		if(candidates.size() == 1)
			return candidates.getFirst();

		if(candidates.isEmpty()){
			final String supported = importers.stream()
				.map(FileImporter::fileDescription)
				.map(description -> "- " + description)
				.collect(Collectors.joining(StringUtils.LF));
			MessageDialogs.show(context.owner(), I18N.tf("error.import.unsupported", file.getName(), supported),
				I18N.t("error.import.title"), JOptionPane.ERROR_MESSAGE);
			return null;
		}

		final String[] names = candidates.stream()
			.map(FileImporter::menuLabel)
			.toArray(String[]::new);
		final Object choice = JOptionPane.showInputDialog(context.owner(), I18N.t("import.format.choose.message"),
			I18N.t("import.format.choose.title"), JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
		if(choice == null)
			return null;
		return candidates.get(Arrays.asList(names)
			.indexOf(choice));
	}

	/**
	 * Asks the user where to save the files embedded in the imported file, writes them and links their media. Closing the question or the
	 * folder chooser (ESC, cancel) keeps the media as they are, and reports them.
	 */
	private static void resolveEmbeddedFiles(final Window owner, final FLEFModel imported, final ImportReport report,
			final File startFolder){
		final int count = report.pendingFiles()
			.size();
		if(count == 0)
			return;

		final int answer = MessageDialogs.confirm(owner, I18N.tf("dialog.import.files.message", count),
			I18N.t("dialog.import.files.title"), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE);
		File folder = null;
		if(answer == JOptionPane.OK_OPTION){
			final JFileChooser chooser = new JFileChooser(startFolder);
			chooser.setDialogTitle(I18N.t("dialog.import.files.title"));
			chooser.setApproveButtonText(I18N.t("dialog.import.files.choose"));
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if(chooser.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION)
				folder = chooser.getSelectedFile();
		}
		if(folder == null)
			EmbeddedFiles.decline(report);
		else
			EmbeddedFiles.writeAndLink(imported, report, folder);
	}

}
