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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;


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
		super(owner, "File Properties", ModalityType.APPLICATION_MODAL);

		final String report = buildReport(file, model);

		final JTextArea area = new JTextArea(report);
		area.setEditable(false);
		area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		area.setCaretPosition(0);

		final JScrollPane scroll = new JScrollPane(area);
		scroll.setPreferredSize(new Dimension(620, 440));

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


	public static void show(final Window owner, final File file, final FLEFModel model){
		new FilePropertiesDialog(owner, file, model)
			.setVisible(true);
	}


	/* ======================================================================
	 *                          Report
	 * ====================================================================== */

	private static String buildReport(final File file, final FLEFModel model){
		final StringBuilder sb = new StringBuilder();

		sb.append("=== File Properties ===\n\n");
		appendFileInfo(sb, file);
		appendModelInfo(sb, model);

		return sb.toString();
	}

	private static void appendFileInfo(final StringBuilder sb, final File file){
		sb.append("--- File ---\n");
		if(file == null){
			sb.append("Name:           (unsaved)\n");
			sb.append("Location:       —\n");
			sb.append("Size:           —\n");
			sb.append("Last modified:  —\n");
			sb.append("Readable:       —\n");
			sb.append("Writable:       —\n");
			sb.append("Encoding:       ").append(detectEncoding(null)).append('\n');
			sb.append('\n');

			return;
		}
		sb.append("Name:           ").append(file.getName()).append('\n');
		sb.append("Location:       ").append(file.getAbsolutePath()).append('\n');
		sb.append("Size:           ").append(formatBytes(file.length())).append('\n');
		sb.append("Last modified:  ").append(TIMESTAMP_FORMAT.format(
			Instant.ofEpochMilli(file.lastModified()))).append('\n');
		sb.append("Readable:       ").append(file.canRead()? "yes": "no").append('\n');
		sb.append("Writable:       ").append(file.canWrite()? "yes": "no").append('\n');
		sb.append("Encoding:       ").append(detectEncoding(file)).append('\n');
		sb.append('\n');
	}

	private static void appendModelInfo(final StringBuilder sb, final FLEFModel model){
		sb.append("--- Model ---\n");
		if(model == null){
			sb.append("No model loaded.\n");
			return;
		}
		try{
			final List<FLEFRecord> all = model.getRecords();
			sb.append("Total records:  ").append(all.size()).append('\n');
			sb.append('\n');
			sb.append("By tag:\n");

			final Map<String, Long> byTag = new TreeMap<>();
			for(final FLEFRecord r : all)
				if(r.getTag() != null)
					byTag.merge(r.getTag(), 1L, Long::sum);

			for(final Map.Entry<String, Long> e : byTag.entrySet())
				sb.append(String.format("  %-24s %d%n", e.getKey(), e.getValue()));
		}
		catch(final RuntimeException ex){
			sb.append("Unable to compute model statistics: ")
				.append(ex.getMessage()).append('\n');
		}
	}

	private static String detectEncoding(final File file){
		if(file == null)
			return "UTF-8 (new document)";
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

	private static String formatBytes(final long bytes){
		final long kb = bytes / 1024L;
		if(kb < 1024L)
			return kb + " KB";
		final long mb = kb / 1024L;
		if(mb < 1024L)
			return String.format(Locale.ENGLISH, "%.2f MB", kb / 1024.0);
		return String.format(Locale.ENGLISH, "%.2f GB", mb / 1024.0);
	}

}
