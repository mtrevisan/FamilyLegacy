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
import io.github.mtrevisan.familylegacy.ui.helpers.ProjectInfo;
import io.github.mtrevisan.familylegacy.ui.helpers.ReportText;
import org.apache.commons.lang3.StringUtils;

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
import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Read-only dialog that reports environment and model diagnostics.
 * <p>
 * The content is grouped into several sections:
 * <ul>
 *   <li><b>Application</b> — application name and version;</li>
 *   <li><b>Runtime</b> — Java version, VM, OS, locale, encoding, and
 *       available processors;</li>
 *   <li><b>Memory</b> — maximum, allocated, and free heap memory;</li>
 *   <li><b>Garbage Collector</b> — names of the active garbage collectors;</li>
 *   <li><b>User</b> — user home and working directory;</li>
 *   <li><b>Graphics</b> — screen resolution, DPI, and 2D acceleration;</li>
 *   <li><b>Disk Space</b> — model file path and free disk space;</li>
 *   <li><b>Model</b> — total number of records in the loaded
 *       {@link FLEFModel}, grouped by tag.</li>
 * </ul>
 * The output is plain text so that the user can select, copy, and paste
 * it into a bug report. A "Copy to Clipboard" button copies the whole
 * text in one click.
 */
public final class DiagnosticsDialog extends JDialog{

	private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


	private DiagnosticsDialog(final Window owner, final FLEFModel model, final File modelFile){
		super(owner, I18N.t("dialog.diagnostics.title"), ModalityType.APPLICATION_MODAL);

		GUIHelper.installEscapeToClose(this);

		final String report = buildReport(model, modelFile);

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


		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(copy);

		setLayout(new BorderLayout(8, 8));
		add(scroll, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(owner);
	}


	public static void show(final Window owner, final FLEFModel model, final File modelFile){
		final DiagnosticsDialog dialog = new DiagnosticsDialog(owner, model, modelFile);
		dialog.setVisible(true);
	}


	/* ======================================================================
	 *                          Report
	 * ====================================================================== */

	private static String buildReport(final FLEFModel model, final File modelFile){
		final StringBuilder sb = new StringBuilder();

		ReportText.heading(sb, I18N.t("text.diagnostics.heading"));
		ReportText.line(sb, "text.diagnostics.generated", LocalDateTime.now().format(TIMESTAMP_FORMAT));
		ReportText.blank(sb);

		appendApplication(sb);
		appendRuntime(sb);
		appendMemory(sb);
		appendGarbageCollector(sb);
		appendUser(sb);
		appendGraphics(sb);
		appendDisk(sb, modelFile);
		ReportText.section(sb, "text.report.section.model");
		ReportText.modelStatistics(sb, model);

		return sb.toString();
	}

	private static void appendApplication(final StringBuilder sb){
		ReportText.section(sb, "text.diagnostics.section.application");
		ReportText.line(sb, "text.diagnostics.app_name", ProjectInfo.getAppName());
		ReportText.line(sb, "text.diagnostics.app_version", ProjectInfo.getAppVersion());
		ReportText.blank(sb);
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
		ReportText.line(sb, "text.diagnostics.file_encoding", Charset.defaultCharset().displayName());
		ReportText.line(sb, "text.diagnostics.processors", Runtime.getRuntime().availableProcessors());
		ReportText.line(sb, "text.diagnostics.uptime", formatDuration(ManagementFactory.getRuntimeMXBean().getUptime()));
		ReportText.line(sb, "text.diagnostics.threads", ManagementFactory.getThreadMXBean().getThreadCount());
		ReportText.line(sb, "text.diagnostics.jvm_arguments", String.join(StringUtils.SPACE, ManagementFactory.getRuntimeMXBean().getInputArguments()));
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
		if(bytes < 1024l * 1024l)
			return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.);
		final long mb = bytes / (1024L * 1024L);
		if(mb >= 1024L)
			return String.format(Locale.ROOT, "%.2f GB", mb / 1024.);
		return mb + " MB";
	}

	private static String formatDuration(final long millis){
		final Duration duration = Duration.ofMillis(millis);
		return String.format(Locale.ROOT, "%d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
	}

	private static void appendGarbageCollector(final StringBuilder sb){
		ReportText.section(sb, "text.diagnostics.section.garbage_collector");
		try{
			final List<String> gcNames = new CopyOnWriteArrayList<>();
			for(final var gc : ManagementFactory.getGarbageCollectorMXBeans())
				gcNames.add(gc.getName());
			ReportText.line(sb, "text.diagnostics.gc_name", String.join(", ", gcNames));
		}
		catch(final Exception e){
			ReportText.line(sb, "text.diagnostics.gc_name", "unavailable");
		}
		ReportText.blank(sb);
	}

	private static void appendUser(final StringBuilder sb){
		ReportText.section(sb, "text.diagnostics.section.user");
		ReportText.line(sb, "text.diagnostics.user_home", System.getProperty("user.home"));
		ReportText.line(sb, "text.diagnostics.user_dir", System.getProperty("user.dir"));
		ReportText.blank(sb);
	}

	private static void appendGraphics(final StringBuilder sb){
		ReportText.section(sb, "text.diagnostics.section.graphics");
		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		final Dimension screenSize = toolkit.getScreenSize();
		ReportText.line(sb, "text.diagnostics.screen_resolution", screenSize.width + "x" + screenSize.height);
		ReportText.line(sb, "text.diagnostics.screen_dpi", toolkit.getScreenResolution());

		// Check 2D hardware acceleration via system properties
		final boolean d3d = "true".equals(System.getProperty("sun.java2d.d3d"));
		final boolean opengl = "true".equals(System.getProperty("sun.java2d.opengl"));
		final boolean metal = "true".equals(System.getProperty("sun.java2d.metal"));
		final String pipeline;
		if(d3d) pipeline = "Direct3D";
		else if(opengl) pipeline = "OpenGL";
		else if(metal) pipeline = "Metal";
		else pipeline = "software";
		ReportText.line(sb, "text.diagnostics.graphics_accelerated", pipeline);
		ReportText.line(sb, "text.diagnostics.awt_toolkit", System.getProperty("awt.toolkit"));
		ReportText.blank(sb);
	}

	private static void appendDisk(final StringBuilder sb, final File modelFile){
		ReportText.section(sb, "text.diagnostics.section.disk");
		if(modelFile != null){
			ReportText.line(sb, "text.diagnostics.model_file", modelFile.getAbsolutePath());
			ReportText.line(sb, "text.diagnostics.model_file_size", formatBytes(modelFile.length()));
			ReportText.line(sb, "text.diagnostics.model_file_modified",
				LocalDateTime.ofInstant(Instant.ofEpochMilli(modelFile.lastModified()), ZoneId.systemDefault()).format(TIMESTAMP_FORMAT));
			final File folder = modelFile.getAbsoluteFile().getParentFile();
			ReportText.line(sb, "text.diagnostics.disk_free", formatBytes(folder != null? folder.getFreeSpace(): 0l));
		}
		else
			ReportText.line(sb, "text.diagnostics.model_file", "(unsaved)");
		ReportText.blank(sb);
	}

}
