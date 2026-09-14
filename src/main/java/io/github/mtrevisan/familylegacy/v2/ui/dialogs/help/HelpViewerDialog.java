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

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.IOException;
import java.net.URI;


/**
 * A modal dialog that displays an HTML document inside a scrollable
 * {@link JEditorPane}.
 * <p>
 * External hyperlinks ({@code http}, {@code https}, {@code mailto}) are
 * opened in the system browser through {@link Desktop#browse(URI)}. If
 * the platform does not support browsing, the link is ignored and no
 * error is shown, so a click on a link never blocks the help content.
 * <p>
 * The dialog is read-only: the user can select and copy text, but cannot
 * edit it. The caret is reset to the top after the content is loaded,
 * so the view always starts from the beginning of the document.
 */
public final class HelpViewerDialog extends JDialog{

	private HelpViewerDialog(final Window owner, final String title, final String html){
		super(owner, title, ModalityType.APPLICATION_MODAL);

		final JEditorPane editor = new JEditorPane();
		editor.setContentType("text/html");
		editor.setEditable(false);
		editor.setCaretPosition(0);
		editor.setText(html);
		editor.setCaretPosition(0);

		editor.addHyperlinkListener(e -> {
			if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
				openExternal(e.getURL() != null? e.getURL().toString(): null);
		});

		final JScrollPane scroll = new JScrollPane(editor);
		scroll.setPreferredSize(new Dimension(760, 560));

		final JButton close = new JButton("Close");
		close.addActionListener(e -> dispose());

		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(close);

		setLayout(new BorderLayout(8, 8));
		add(scroll, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(owner);
	}


	/**
	 * Opens the dialog with the given HTML content.
	 *
	 * @param owner the parent window; may be {@code null}
	 * @param title the dialog title
	 * @param html  the HTML document; must not be {@code null}
	 */
	public static void show(final Window owner, final String title, final String html){
		final HelpViewerDialog dialog = new HelpViewerDialog(owner, title, html);
		dialog.setVisible(true);
	}


	private static void openExternal(final String url){
		if(url == null || !Desktop.isDesktopSupported())
			return;
		final Desktop desktop = Desktop.getDesktop();
		if(!desktop.isSupported(Desktop.Action.BROWSE))
			return;
		try{
			desktop.browse(URI.create(url));
		}
		catch(final IOException | IllegalArgumentException ignored){
			// A failed browse must not interrupt the help session.
		}
	}

}
