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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Window;


/**
 * Modal progress dialog. Every mutator is safe to call from any
 * thread: it marshals to the EDT internally.
 */
public final class ProgressDialog extends JDialog{

	private final JProgressBar bar = new JProgressBar(0, 100);
	private final JLabel message = new JLabel(" ");


	public ProgressDialog(final Window owner, final String title, final String initialMessage){
		super(owner, title, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		message.setText(initialMessage);

		final JPanel content = new JPanel(new BorderLayout(0, 10));
		content.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
		content.add(message, BorderLayout.CENTER);
		content.add(bar, BorderLayout.SOUTH);

		bar.setIndeterminate(true);
		bar.setStringPainted(true);

		setContentPane(content);
		pack();
		setResizable(false);
		setLocationRelativeTo(owner);
	}

	/**
	 * @param percent 0-100 for a determinate step, or a negative value
	 *                to switch back to an indeterminate animation
	 * @param text    short description of the current step
	 */
	public void update(final int percent, final String text){
		SwingUtilities.invokeLater(() -> {
			if(percent < 0){
				bar.setIndeterminate(true);
				bar.setString(null);
			}
			else{
				bar.setIndeterminate(false);
				bar.setValue(percent);
				bar.setString(percent + "%");
			}
			message.setText(text);
		});
	}

	public void close(){
		SwingUtilities.invokeLater(this::dispose);
	}

}
