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
package io.github.mtrevisan.familylegacy.v2.ui.tools.compare;

import io.github.mtrevisan.familylegacy.v2.ui.helpers.DiffUtils;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ReportDialog;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


/**
 * Tool that opens two FLEF (or any text) files and shows a unified diff.
 * <p>
 * The diff is computed line by line using {@link DiffUtils}. The output
 * is the conventional unified format: unchanged lines prefixed with a
 * space, deletions with {@code -}, insertions with {@code +}, and
 * modifications shown as a deletion immediately followed by an
 * insertion.
 * <p>
 * The tool does not depend on the loaded model: the two sides are read
 * directly from disk, so the user can compare any two files, including
 * versions of the same file kept outside the application.
 */
public final class CompareFilesTool implements ToolOperation{

	@Override
	public String getName(){
		return "Compare Two Files…";
	}

	@Override
	public void run(final ToolContext context){
		final JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Select the two files to compare");
		chooser.setMultiSelectionEnabled(true);
		chooser.setFileFilter(new FileNameExtensionFilter("FLEF files (*.flef, *.gedg)", "flef", "gedg", "txt"));

		if(chooser.showOpenDialog(context.owner()) != JFileChooser.APPROVE_OPTION)
			return;

		final java.io.File[] selected = chooser.getSelectedFiles();
		if(selected.length != 2){
			JOptionPane.showMessageDialog(context.owner(),
				"Select exactly two files.",
				"Compare Files",
				JOptionPane.WARNING_MESSAGE);
			return;
		}

		try{
			final String left = Files.readString(Path.of(selected[0].getAbsolutePath()));
			final String right = Files.readString(Path.of(selected[1].getAbsolutePath()));
			final String diff = computeUnifiedDiff(left, right);
			ReportDialog.showText(context.owner(),
				"Diff — " + selected[0].getName() + " vs " + selected[1].getName(),
				diff);
		}
		catch(final IOException e){
			JOptionPane.showMessageDialog(context.owner(),
				"Unable to read the selected files:\n" + e.getMessage(),
				"Compare Files",
				JOptionPane.ERROR_MESSAGE);
		}
	}


	private static String computeUnifiedDiff(final String left, final String right){
		final List<String> leftLines = left.lines().toList();
		final List<String> rightLines = right.lines().toList();

		final List<DiffUtils.DiffEntry> entries =
			DiffUtils.computeDiff(leftLines, rightLines);

		final StringBuilder out = new StringBuilder();
		for(final DiffUtils.DiffEntry entry : entries){
			switch(entry.operation()){
				case EQUAL -> out.append("  ").append(entry.leftLine()).append("\n");
				case DELETE -> out.append("- ").append(entry.leftLine()).append("\n");
				case INSERT -> out.append("+ ").append(entry.rightLine()).append("\n");
				case MODIFIED -> {
					out.append("- ").append(entry.leftLine()).append("\n");
					out.append("+ ").append(entry.rightLine()).append("\n");
				}
			}
		}
		return out.toString();
	}

}
