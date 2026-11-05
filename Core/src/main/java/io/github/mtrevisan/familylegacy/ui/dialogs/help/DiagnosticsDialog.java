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
package io.github.mtrevisan.familylegacy.ui.dialogs.help;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.ReportText;

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
import java.util.Locale;


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

	private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


	private DiagnosticsDialog(final Window owner, final FLEFModel model){
		super(owner, I18N.t("dialog.diagnostics.title"), ModalityType.APPLICATION_MODAL);
		GUIHelper.installEscapeToClose(this);

		final String report = buildReport(model);

		final JTextArea area = new JTextArea(report);
		area.setEditable(false);
		area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		area.setCaretPosition(0);

		final JScrollPane scroll = new JScrollPane(area);
		scroll.setPreferredSize(new Dimension(680, 480));

		final JButton copy = new JButton(I18N.t("button.copy_to_clipboard"));
		copy.addActionListener(e -> {
			final StringSelection selection = new StringSelection(area.getText());
			Toolkit.getDefaultToolkit()
				.getSystemClipboard()
				.setContents(selection, selection);
		});

		final JButton close = new JButton(I18N.t("button.close"));
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

		ReportText.heading(sb, I18N.t("text.diagnostics.heading"));
		ReportText.line(sb, "text.diagnostics.generated", LocalDateTime.now().format(TIMESTAMP_FORMAT));
		ReportText.blank(sb);

		appendRuntime(sb);
		appendMemory(sb);
		ReportText.section(sb, "text.report.section.model");
		ReportText.modelStatistics(sb, model);

		return sb.toString();
	}

	private static void appendRuntime(final StringBuilder sb){
		ReportText.section(sb, "text.diagnostics.section.runtime");
		ReportText.line(sb, "text.diagnostics.java_version", System.getProperty("java.version"));
		ReportText.line(sb, "text.diagnostics.java_vendor", System.getProperty("java.vendor"));
		ReportText.line(sb, "text.diagnostics.java_home", System.getProperty("java.home"));
		ReportText.line(sb, "text.diagnostics.vm_name", System.getProperty("java.vm.name"));
		ReportText.line(sb, "text.diagnostics.vm_version", System.getProperty("java.vm.version"));
		ReportText.line(sb, "text.diagnostics.os_name", System.getProperty("os.name"));
		ReportText.line(sb, "text.diagnostics.os_version", System.getProperty("os.version"));
		ReportText.line(sb, "text.diagnostics.os_arch", System.getProperty("os.arch"));
		ReportText.line(sb, "text.diagnostics.locale", Locale.getDefault());
		ReportText.line(sb, "text.diagnostics.file_encoding", System.getProperty("file.encoding"));
		ReportText.line(sb, "text.diagnostics.processors", Runtime.getRuntime().availableProcessors());
		ReportText.blank(sb);
	}

	private static void appendMemory(final StringBuilder sb){
		final Runtime rt = Runtime.getRuntime();
		final long total = rt.totalMemory();
		final long free = rt.freeMemory();

		ReportText.section(sb, "text.diagnostics.section.memory");
		ReportText.line(sb, "text.diagnostics.heap_max", formatBytes(rt.maxMemory()));
		ReportText.line(sb, "text.diagnostics.heap_allocated", formatBytes(total));
		ReportText.line(sb, "text.diagnostics.heap_used", formatBytes(total - free));
		ReportText.line(sb, "text.diagnostics.heap_free", formatBytes(free));
		ReportText.blank(sb);
	}

	private static String formatBytes(final long bytes){
		final long mb = bytes / (1024L * 1024L);
		if(mb >= 1024L)
			return String.format("%.2f GB", mb / 1024.);
		return mb + " MB";
	}

}
