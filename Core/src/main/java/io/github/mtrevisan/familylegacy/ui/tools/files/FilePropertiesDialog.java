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
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


/**
 * Read-only dialog showing the properties of the current document: the
 * file (when saved), its size, its last modification date, and the
 * statistics of the model it contains.
 * <p>
 * The output is plain text so that the user can select and copy it, and
 * a "Copy to Clipboard" button copies the whole text in one click.
 */
public final class FilePropertiesDialog extends JDialog{

	private static final DateTimeFormatter TIMESTAMP_FORMAT =
		DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());


	private FilePropertiesDialog(final Window owner, final File file, final FLEFModel model){
		super(owner, I18N.t("dialog.file_properties.title"), ModalityType.APPLICATION_MODAL);
		ToolDialogs.installEscapeToClose(this);

		final String report = buildReport(file, model);

		final JTextArea area = new JTextArea(report);
		area.setEditable(false);
		area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		area.setCaretPosition(0);

		final JScrollPane scroll = new JScrollPane(area);
		scroll.setPreferredSize(new Dimension(620, 440));

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


	public static void show(final Window owner, final File file, final FLEFModel model){
		new FilePropertiesDialog(owner, file, model)
			.setVisible(true);
	}


	/* ======================================================================
	 *                          Report
	 * ====================================================================== */

	private static String buildReport(final File file, final FLEFModel model){
		final StringBuilder sb = new StringBuilder();

		ReportText.heading(sb, I18N.t("dialog.file_properties.title"));
		appendFileInfo(sb, file);
		ReportText.section(sb, "text.report.section.model");
		ReportText.modelStatistics(sb, model);

		return sb.toString();
	}

	private static void appendFileInfo(final StringBuilder sb, final File file){
		ReportText.section(sb, "text.report.section.file");
		if(file == null){
			final String none = "\u2014";
			ReportText.line(sb, "text.report.name", I18N.t("text.report.unsaved"));
			ReportText.line(sb, "text.report.location", none);
			ReportText.line(sb, "text.report.size", none);
			ReportText.line(sb, "text.report.last_modified", none);
			ReportText.line(sb, "text.report.readable", none);
			ReportText.line(sb, "text.report.writable", none);
			ReportText.line(sb, "text.report.encoding", detectEncoding(null));
			ReportText.blank(sb);

			return;
		}

		ReportText.line(sb, "text.report.name", file.getName());
		ReportText.line(sb, "text.report.location", file.getAbsolutePath());
		ReportText.line(sb, "text.report.size", formatBytes(file.length()));
		ReportText.line(sb, "text.report.last_modified", TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(file.lastModified())));
		ReportText.line(sb, "text.report.readable", I18N.t(file.canRead()? "text.report.yes": "text.report.no"));
		ReportText.line(sb, "text.report.writable", I18N.t(file.canWrite()? "text.report.yes": "text.report.no"));
		ReportText.line(sb, "text.report.encoding", detectEncoding(file));
		ReportText.blank(sb);
	}

	private static String detectEncoding(final File file){
		if(file == null)
			return I18N.t("text.report.new_document");
		if(!file.exists())
			return "UTF-8";

		try(final InputStream is = new FileInputStream(file)){
			final byte[] bom = new byte[3];
			final int read = is.read(bom);
			if(read >= 3 && bom[0] == (byte)0xEF && bom[1] == (byte)0xBB && bom[2] == (byte)0xBF)
				return "UTF-8 (BOM)";

			if(read >= 2 && bom[0] == (byte)0xFF && bom[1] == (byte)0xFE)
				return "UTF-16 LE";

			if(read >= 2 && bom[0] == (byte)0xFE && bom[1] == (byte)0xFF)
				return "UTF-16 BE";
		}
		catch(final IOException ignored){}
		return "UTF-8";
	}

	/** @return	The size, in the unit that keeps the number readable: a file of a few bytes is not "0 KB". */
	private static String formatBytes(final long bytes){
		if(bytes < 1024L)
			return bytes + " B";

		final long kb = bytes / 1024L;
		if(kb < 1024L)
			return kb + " KB";
		final long mb = kb / 1024L;
		if(mb < 1024L)
			return GUIHelper.format("%.2f MB", kb / 1024.);
		return GUIHelper.format("%.2f GB", mb / 1024.);
	}

}
