package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Window;
import java.io.Serial;

/**
 * Modal progress dialog. Every mutator is safe to call from any
 * thread: it marshals to the EDT internally.
 */
public final class ProgressDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 3743606341704752187L;


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
