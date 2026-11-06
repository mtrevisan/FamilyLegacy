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

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;


/**
 * Shows the download of an update, that can be canceled (with the button, ESC or the close button of the window).
 * <p>
 * The dialog is modal: {@link #setVisible(boolean) setVisible(true)} returns when {@link #close()} is called. Every method is safe to
 * call from any thread.
 * </p>
 */
public final class UpdateDownloadDialog extends JDialog{

	private static final KeyStroke ESCAPE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
	private static final double MEGABYTE = 1024. * 1024.;

	private final JProgressBar bar = new JProgressBar(0, 100);
	private final JLabel message = new JLabel();
	private final Runnable onCancel;


	/**
	 * @param owner	The window that owns the dialog.
	 * @param title	The title of the dialog.
	 * @param text	What is being downloaded.
	 * @param onCancel	Called, once the user asks to cancel (the dialog stays open until {@link #close()}).
	 */
	public UpdateDownloadDialog(final Window owner, final String title, final String text, final Runnable onCancel){
		super(owner, title, ModalityType.APPLICATION_MODAL);

		this.onCancel = onCancel;

		message.setText(text);
		bar.setIndeterminate(true);
		bar.setStringPainted(true);

		final JButton cancelButton = new JButton(I18N.t("button.cancel"));
		cancelButton.addActionListener(e -> cancel(cancelButton));
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		buttons.add(cancelButton);

		final JPanel south = new JPanel(new BorderLayout(0, 10));
		south.add(bar, BorderLayout.NORTH);
		south.add(buttons, BorderLayout.SOUTH);

		final JPanel content = new JPanel(new BorderLayout(0, 10));
		content.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
		content.add(message, BorderLayout.CENTER);
		content.add(south, BorderLayout.SOUTH);
		setContentPane(content);

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(final WindowEvent e){
				cancel(cancelButton);
			}
		});
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ESCAPE, "escape");
		getRootPane().getActionMap().put("escape", new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent e){
				cancel(cancelButton);
			}
		});

		pack();
		setMinimumSize(getSize());
		setResizable(false);
		setLocationRelativeTo(owner);
	}

	private void cancel(final JButton cancelButton){
		if(!cancelButton.isEnabled())
			return;

		cancelButton.setEnabled(false);
		onCancel.run();
	}

	/**
	 * @param done	The bytes received.
	 * @param total	The bytes expected, or -1 if unknown.
	 */
	public void progress(final long done, final long total){
		SwingUtilities.invokeLater(() -> {
			if(total <= 0){
				bar.setIndeterminate(true);
				bar.setString(String.format(Locale.ROOT, "%.1f MB", done / MEGABYTE));
			}
			else{
				bar.setIndeterminate(false);
				bar.setValue((int)Math.min(100l, done * 100l / total));
				bar.setString(String.format(Locale.ROOT, "%.1f / %.1f MB", done / MEGABYTE, total / MEGABYTE));
			}
		});
	}

	/** Closes the dialog. */
	public void close(){
		SwingUtilities.invokeLater(this::dispose);
	}

}
