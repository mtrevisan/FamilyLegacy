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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs.help;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;


/**
 * Read-only dialog that reports environment and model diagnostics.
 * <p>
 * The content is grouped into three sections:
 * <ul>
 *   <li><b>Runtime</b> — Java version, VM, OS, locale, encoding, and
 *       available processors;</li>
 *   <li><b>Memory</b> — maximum, allocated, and free heap memory;</li>
 *   <li><b>Model</b> — total number of records in the loaded
 *       {@link FLEFModel}, grouped by tag.</li>
 * </ul>
 * The output is plain text so that the user can select, copy, and paste
 * it into a bug report. A "Copy to Clipboard" button copies the whole
 * text in one click.
 */
public final class DiagnosticsDialog extends JDialog{

	private static final DateTimeFormatter TIMESTAMP_FORMAT =
		DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


	private DiagnosticsDialog(final Window owner, final FLEFModel model){
		super(owner, "Diagnostics", ModalityType.APPLICATION_MODAL);

		final String report = buildReport(model);

		final JTextArea area = new JTextArea(report);
		area.setEditable(false);
		area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		area.setCaretPosition(0);

		final JScrollPane scroll = new JScrollPane(area);
		scroll.setPreferredSize(new Dimension(680, 480));

		final JButton copy = new JButton("Copy to Clipboard");
		copy.addActionListener(e -> {
			final StringSelection selection = new StringSelection(area.getText());
			Toolkit.getDefaultToolkit()
				.getSystemClipboard()
				.setContents(selection, selection);
		});

		final JButton close = new JButton("Close");
		close.addActionListener(e -> dispose());

		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(copy);
		buttons.add(close);

		setLayout(new BorderLayout(8, 8));
		add(scroll, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(owner);
	}


	public static void show(final Window owner, final FLEFModel model){
		final DiagnosticsDialog dialog = new DiagnosticsDialog(owner, model);
		dialog.setVisible(true);
	}


	/* ======================================================================
	 *                          Report
	 * ====================================================================== */

	private static String buildReport(final FLEFModel model){
		final StringBuilder sb = new StringBuilder();

		sb.append("=== Family Legacy Diagnostics ===\n");
		sb.append("Generated: ").append(LocalDateTime.now()
			.format(TIMESTAMP_FORMAT)).append("\n");
		sb.append("\n");

		appendRuntime(sb);
		appendMemory(sb);
		appendModel(sb, model);

		return sb.toString();
	}

	private static void appendRuntime(final StringBuilder sb){
		sb.append("--- Runtime ---\n");
		sb.append("Java version:   ")
			.append(System.getProperty("java.version")).append("\n");
		sb.append("Java vendor:    ")
			.append(System.getProperty("java.vendor")).append("\n");
		sb.append("Java home:      ")
			.append(System.getProperty("java.home")).append("\n");
		sb.append("VM name:        ")
			.append(System.getProperty("java.vm.name")).append("\n");
		sb.append("VM version:     ")
			.append(System.getProperty("java.vm.version")).append("\n");
		sb.append("OS name:        ")
			.append(System.getProperty("os.name")).append("\n");
		sb.append("OS version:     ")
			.append(System.getProperty("os.version")).append("\n");
		sb.append("OS arch:        ")
			.append(System.getProperty("os.arch")).append("\n");
		sb.append("Locale:         ")
			.append(java.util.Locale.getDefault()).append("\n");
		sb.append("File encoding:  ")
			.append(System.getProperty("file.encoding")).append("\n");
		sb.append("Processors:     ")
			.append(Runtime.getRuntime()
				.availableProcessors()).append("\n");
		sb.append("\n");
	}

	private static void appendMemory(final StringBuilder sb){
		final Runtime rt = Runtime.getRuntime();
		final long max = rt.maxMemory();
		final long total = rt.totalMemory();
		final long free = rt.freeMemory();
		final long used = total - free;

		sb.append("--- Memory ---\n");
		sb.append("Heap max:       ").append(formatBytes(max)).append("\n");
		sb.append("Heap allocated: ").append(formatBytes(total)).append("\n");
		sb.append("Heap used:      ").append(formatBytes(used)).append("\n");
		sb.append("Heap free:      ").append(formatBytes(free)).append("\n");
		sb.append("\n");
	}

	private static void appendModel(final StringBuilder sb, final FLEFModel model){
		sb.append("--- Model ---\n");
		if(model == null){
			sb.append("No model loaded.\n");
			return;
		}
		try{
			final List<FLEFRecord> all = model.getRecords();
			sb.append("Total records:  ").append(all.size()).append("\n");
			sb.append("\n");
			sb.append("By tag:\n");

			final Map<String, Long> byTag = all.stream()
				.filter(r -> r.getTag() != null)
				.collect(Collectors.groupingBy(FLEFRecord::getTag,
					TreeMap::new, Collectors.counting()));

			for(final Map.Entry<String, Long> entry : byTag.entrySet())
				sb.append(String.format("  %-24s %d%n", entry.getKey(), entry.getValue()));
		}
		catch(final RuntimeException e){
			sb.append("Unable to compute model statistics: ")
				.append(e.getMessage()).append("\n");
		}
	}

	private static String formatBytes(final long bytes){
		final long mb = bytes / (1024L * 1024L);
		if(mb >= 1024L)
			return String.format("%.2f GB", mb / 1024.0);
		return mb + " MB";
	}

}
