package io.github.mtrevisan.familylegacy.v2.ui.tools.files;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
			final java.awt.datatransfer.StringSelection selection =
				new java.awt.datatransfer.StringSelection(area.getText());
			java.awt.Toolkit.getDefaultToolkit()
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
		new FilePropertiesDialog(owner, file, model).setVisible(true);
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
			sb.append("Not saved yet.\n\n");
			return;
		}
		sb.append("Name:           ").append(file.getName()).append('\n');
		sb.append("Location:       ").append(file.getAbsolutePath()).append('\n');
		sb.append("Size:           ").append(formatBytes(file.length())).append('\n');
		sb.append("Last modified:  ").append(TIMESTAMP_FORMAT.format(
			Instant.ofEpochMilli(file.lastModified()))).append('\n');
		sb.append("Readable:       ").append(file.canRead()? "yes": "no").append('\n');
		sb.append("Writable:       ").append(file.canWrite()? "yes": "no").append('\n');
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

	private static String formatBytes(final long bytes){
		final long kb = bytes / 1024L;
		if(kb < 1024L)
			return kb + " KB";
		final long mb = kb / 1024L;
		if(mb < 1024L)
			return String.format("%.2f MB", kb / 1024.0);
		return String.format("%.2f GB", mb / 1024.0);
	}

}
