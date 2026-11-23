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
package io.github.mtrevisan.familylegacy.ui.tools.reports.operations;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.reports.ReportPreferences;
import io.github.mtrevisan.familylegacy.reports.renderers.ReportRenderers;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.SavedFileNotice;
import io.github.mtrevisan.reports.ReportRenderer;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import java.awt.Component;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * What the dialogs that write a report in a file (the one of a report, the one of a book) ask and tell: where, and in which format,
 * whether to overwrite what is there, where the file is when it is done.
 */
final class ReportTargets{

	private ReportTargets(){}


	/** @return	The label of the format in the dialog that asks where to save the report. */
	private static String formatLabel(final ReportRenderer renderer){
		return I18N.t("checkbox.format_" + renderer.id(), renderer.displayName());
	}

	/**
	 * Asks where to save the report and in which format: the formats offered are the ones of the available renderers, so a format whose plugin
	 * is missing is not listed.
	 *
	 * @param parent	The component the dialogs are shown over.
	 * @param preferences	What the user chose the last time (the format and the folder).
	 * @param baseName	The name proposed for the file, without the extension.
	 * @return	The chosen file (or directory, for a format that is one) with the renderer that writes it, {@code null} if the user gave up.
	 */
	static Map.Entry<Path, ReportRenderer> ask(final Component parent, final ReportPreferences preferences, final String baseName){
		final List<ReportRenderer> renderers = ReportRenderers.available();
		if(renderers.isEmpty()){
			MessageDialogs.show(parent,
				I18N.t("dialog.report.no_format.message"), I18N.t("dialog.report.no_format.title"), JOptionPane.WARNING_MESSAGE);

			return null;
		}

		final Map<FileFilter, ReportRenderer> filters = new LinkedHashMap<>();
		for(final ReportRenderer renderer : renderers)
			filters.put(new RendererFilter(renderer), renderer);

		final JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(I18N.t("dialog.report.save.title"));
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);
		filters.keySet()
			.forEach(chooser::addChoosableFileFilter);

		//the format of the last time if it is still available, else the default one, else the first
		final List<String> remembered = preferences.formats();
		final String preferred = (!remembered.isEmpty()? remembered.getFirst(): ReportRenderers.DEFAULT_ID);
		FileFilter initial = filters.keySet().iterator().next();
		for(final Map.Entry<FileFilter, ReportRenderer> entry : filters.entrySet())
			if(entry.getValue().id().equals(preferred)){
				initial = entry.getKey();
				break;
			}
		chooser.setFileFilter(initial);

		final String lastDirectory = preferences.directory();
		final File directory = (lastDirectory != null? new File(lastDirectory): null);
		if(directory != null && directory.isDirectory())
			chooser.setCurrentDirectory(directory);
		chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), baseName + filters.get(initial).extension()));
		//changing the format changes the suggested name too
		chooser.addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY, event -> {
			final ReportRenderer renderer = filters.get(chooser.getFileFilter());
			if(renderer == null)
				return;

			final File current = chooser.getSelectedFile();
			final String name = (current != null? current.getName(): baseName);
			chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), withExtension(name, renderers, renderer)));
		});

		if(chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null)
			return null;

		final ReportRenderer renderer = filters.get(chooser.getFileFilter());
		final File chosen = chooser.getSelectedFile();
		return Map.entry(new File(chosen.getParentFile(), withExtension(chosen.getName(), renderers, renderer)).toPath(), renderer);
	}

	/** @return	The name with the extension of the renderer instead of the one of another format (added if there is none). */
	private static String withExtension(final String name, final List<ReportRenderer> renderers, final ReportRenderer renderer){
		String base = name;
		for(final ReportRenderer candidate : renderers)
			if(base.endsWith(candidate.extension())){
				base = base.substring(0, base.length() - candidate.extension().length());
				break;
			}
		return base + renderer.extension();
	}

	/**
	 * Asks before overwriting: the report is written with a fixed name, so a previous one in the same folder would be lost.
	 *
	 * @param parent	The component the dialog is shown over.
	 * @param outDir	The folder the files are written in.
	 * @param names	The names of the files.
	 * @return	Whether to go on.
	 */
	static boolean confirmOverwrite(final Component parent, final Path outDir, final Collection<String> names){
		final List<String> existing = names.stream()
			.filter(name -> Files.exists(outDir.resolve(name)))
			.toList();
		if(existing.isEmpty())
			return true;

		return (JOptionPane.showConfirmDialog(parent,
			I18N.l("dialog.report.overwrite.message") + StringUtils.LF + " • " + String.join(StringUtils.LF + " • ", existing),
			I18N.t("dialog.report.overwrite.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION);
	}

	/** Tells that the files are written, and offers to open the folder with the first file selected, if the system can do it. */
	static void promptOpenFolder(final Component parent, final Path dir, final List<Path> files){
		//only the action: Esc (or closing the window) dismisses the message, an Ok button would add nothing
		final Object[] dialogOptions = {I18N.t("button.open.directory")};
		final StringBuilder msg = new StringBuilder(I18N.l("dialog.report.complete.message"))
			.append(StringUtils.LF);
		for(final Path f : files)
			msg.append(" • ")
				.append(f.getFileName())
				.append(StringUtils.LF);

		final int choice = JOptionPane.showOptionDialog(parent,
			msg.toString(),
			I18N.t("dialog.report.complete.title"),
			JOptionPane.DEFAULT_OPTION,
			JOptionPane.INFORMATION_MESSAGE,
			null,
			dialogOptions,
			dialogOptions[0]);

		if(choice == 0 && !SavedFileNotice.reveal((files.isEmpty()? dir: files.getFirst()).toFile()))
			MessageDialogs.show(parent, I18N.l("dialog.report.open_directory.error") + StringUtils.LF + dir,
				I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);
	}


	/** Shows the files of a format (or the directories, for the format that is one). */
	private static final class RendererFilter extends FileFilter{

		private final ReportRenderer renderer;


		private RendererFilter(final ReportRenderer renderer){
			this.renderer = renderer;
		}

		@Override
		public boolean accept(final File file){
			return (file.isDirectory() || !renderer.isDirectory() && file.getName().endsWith(renderer.extension()));
		}

		@Override
		public String getDescription(){
			return formatLabel(renderer);
		}

	}

}
