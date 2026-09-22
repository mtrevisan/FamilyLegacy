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
package io.github.mtrevisan.familylegacy.v2.ui.tools;

import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;


/**
 * Modal dialog that displays the output of a {@link ToolOperation}.
 * <p>
 * Two content modes are supported:
 * <ul>
 *   <li>HTML — used by structured reports (validation, statistics,
 *       consistency) that benefit from headings and tables;</li>
 *   <li>plain text — used by the file comparator, which shows a
 *       unified diff where whitespace alignment matters.</li>
 * </ul>
 * In both cases the user can copy the content to the clipboard, so a
 * report can be pasted into an email, a bug report, or a research log.
 */
public final class ReportDialog extends JDialog{

	private ReportDialog(final Window owner, final String title, final String content, final boolean isHtml){
		super(owner, title, ModalityType.APPLICATION_MODAL);

		final JEditorPane editor = new JEditorPane();
		editor.setContentType(isHtml? "text/html": "text/plain");
		editor.setEditable(false);
		if(!isHtml)
			editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		editor.setText(content);
		// Reset the caret to the top: setText moves it to the end, which
		// would make the dialog open showing the last line of the report.
		editor.setCaretPosition(0);

		final JScrollPane scroll = new JScrollPane(editor);
		scroll.setPreferredSize(new Dimension(760, 560));

		final JButton copy = new JButton("Copy to Clipboard");
		copy.addActionListener(e -> {
			final StringSelection selection = new StringSelection(editor.getText());
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

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(owner);
	}


	/** Shows an HTML report. */
	public static void showHtml(final Window owner, final String title, final String html){
		final ReportDialog dialog = new ReportDialog(owner, title, html, true);
		dialog.setVisible(true);
	}

	/** Shows a plain-text report. */
	public static void showText(final Window owner, final String title, final String text){
		final ReportDialog dialog = new ReportDialog(owner, title, text, false);
		dialog.setVisible(true);
	}


	/** Escapes text so it can be embedded in an HTML document. */
	public static String escape(final String text){
		if(text == null)
			return StringUtils.EMPTY;

		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}

	/** Wraps arbitrary body HTML in a document with a shared stylesheet. */
	public static String document(final String bodyHtml){
		return """
			<html>
			<head>
			<style>
			  body { font-family: SansSerif; font-size: 12px; margin: 14px; }
			  h1 { font-size: 17px; margin: 0 0 10px 0; }
			  h2 { font-size: 14px; margin: 16px 0 6px 0; }
			  p, li { margin: 4px 0; }
			  code { font-family: Monospaced; background: #f0f0f0; padding: 1px 3px; }
			  table { border-collapse: collapse; margin: 6px 0; }
			  th, td { text-align: left; padding: 3px 10px 3px 0; }
			  .ok { color: #1a7f1a; }
			  .warn { color: #b06a00; }
			  .err { color: #b00020; }
			  .hint { color: #555; font-style: italic; }
			</style>
			</head>
			<body>
			%s
			</body>
			</html>
			""".formatted(bodyHtml);
	}

}
