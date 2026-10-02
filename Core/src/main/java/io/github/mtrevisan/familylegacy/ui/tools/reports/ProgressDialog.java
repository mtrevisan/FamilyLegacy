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
package io.github.mtrevisan.familylegacy.ui.tools.reports;

import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Modal dialog showing the progress of a long-running report generation.
 *
 * <p>The dialog is purely passive: it displays a message and a percentage,
 * and exposes a cancellation flag that the background task is expected to
 * poll between major steps. Closing the dialog via the window close button
 * sets the same flag, so the user has two ways to abort.</p>
 *
 * <p>All mutating methods are safe to call from any thread; they marshal
 * to the Event Dispatch Thread internally.</p>
 */
public final class ProgressDialog extends JDialog{

	private final JLabel messageLabel = new JLabel(StringUtils.SPACE);
	private final JProgressBar progressBar = new JProgressBar(0, 100);
	private final JButton cancelButton = new JButton(I18N.t("button.cancel"));
	private final AtomicBoolean cancelled = new AtomicBoolean(false);


	public ProgressDialog(final Window owner, final String title){
		super(owner, title, ModalityType.APPLICATION_MODAL);

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setLayout(new BorderLayout(0, 0));

		final JPanel content = new JPanel(new MigLayout(
			"ins 16,wrap 1,fillx", "[grow,fill]", "[]12[]"));
		content.add(messageLabel, "growx");
		progressBar.setStringPainted(true);
		content.add(progressBar, "growx, h 22!");

		final JPanel buttons = new JPanel();
		cancelButton.addActionListener(e -> {
			cancelled.set(true);
			cancelButton.setEnabled(false);
			messageLabel.setText("Cancelling…");
		});
		buttons.add(cancelButton);

		add(content, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);

		// Closing the window also means "cancel".
		addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(final WindowEvent e){
				cancelled.set(true);
				cancelButton.setEnabled(false);
				messageLabel.setText("Cancelling…");
			}
		});

		setPreferredSize(new Dimension(360, 130));
		pack();
		setLocationRelativeTo(owner);
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/** Whether the user has asked to cancel the running task. */
	public boolean isCancelled(){
		return cancelled.get();
	}

	/**
	 * Updates the dialog with the current percentage and message. Thread-safe:
	 * the actual UI mutation is marshalled to the EDT.
	 */
	public void update(final int percent, final String message){
		SwingUtilities.invokeLater(() -> {
			progressBar.setValue(Math.clamp(percent, 0, 100));
			if(message != null && !message.isBlank())
				messageLabel.setText(message);
		});
	}

	/**
	 * Convenience adapter so the dialog can be used directly as a
	 * {@link ReportProgressListener}.
	 */
	public ReportProgressListener asListener(){
		return this::update;
	}

}
