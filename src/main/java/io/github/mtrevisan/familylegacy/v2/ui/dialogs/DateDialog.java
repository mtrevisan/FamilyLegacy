package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.DatePanel;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;


/**
 * Dialog for editing a {@code DATE_STRUCTURE} according to FLEF 0.1.0.
 * <p>
 * This dialog opens a {@link DatePanel} for editing a date structure.
 * It is invoked from other panels when a date needs to be edited.
 * <p>
 * Structure:
 * <pre>
 * DATE_STRUCTURE :=
 * n <<DATE_VALUE>>    {1:1}
 * n <<SOURCE_CITATION>>    {0:M}
 * n <<EVIDENCE_QUALIFIERS>>    {0:1}
 * </pre>
 */
public class DateDialog extends JDialog{

	private final DatePanel datePanel;
	private FLEFRecord result;
	private boolean saved = false;


	/**
	 * Creates a new DateDialog.
	 *
	 * @param parent      the parent dialog
	 * @param model       the FLEF model
	 * @param title       the dialog title
	 */
	public static DateDialog createNew(final Dialog parent, final FLEFModel model, final String title){
		return new DateDialog(parent, model, title, null);
	}

	/**
	 * Creates a new DateDialog.
	 *
	 * @param parent      the parent dialog
	 * @param model       the FLEF model
	 * @param title       the dialog title
	 * @param existingEntry the initial DATE_STRUCTURE record (can be null)
	 */
	public static DateDialog createEdit(final Dialog parent, final FLEFModel model, final String title,
			final FLEFRecord existingEntry){
		if(existingEntry == null)
			throw new IllegalArgumentException("existingEntry cannot be null");

		return new DateDialog(parent, model, title, existingEntry);
	}

	private DateDialog(final Dialog parent, final FLEFModel model, final String title, final FLEFRecord initialDate){
		super(parent, title, true);

		datePanel = new DatePanel(this, model);
		datePanel.loadFromRecord(initialDate);

		initComponents();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		setLayout(new BorderLayout(10, 10));

		// Date panel
		final JPanel panel = new JPanel(new BorderLayout(10, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.add(datePanel, BorderLayout.CENTER);
		add(panel, BorderLayout.CENTER);

		// Buttons
		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		final JButton okButton = new JButton("OK");
		final JButton cancelButton = new JButton("Cancel");
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		okButton.addActionListener(e -> {
			if(datePanel.validateRequiredFields()){
				result = datePanel.saveToRecord(null);
				saved = true;
				dispose();
			}
		});

		cancelButton.addActionListener(e -> {
			saved = false;
			result = null;
			dispose();
		});

		// Enter key triggers OK
		getRootPane().setDefaultButton(okButton);

		// Escape key triggers Cancel
		final Action escapeAction = new AbstractAction(){
			@Override
			public void actionPerformed(ActionEvent e){
				saved = false;
				result = null;
				dispose();
			}
		};
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
			KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "escape");
		getRootPane().getActionMap().put("escape", escapeAction);
	}

	/**
	 * Returns the saved DATE_STRUCTURE record, or {@code null} if cancelled or no data.
	 *
	 * @return the DATE_STRUCTURE record, or null
	 */
	public FLEFRecord getDateRecord(){
		return saved? result: null;
	}

	/**
	 * Returns whether the dialog was saved (OK pressed).
	 *
	 * @return true if saved, false otherwise
	 */
	public boolean isSaved(){
		return saved;
	}

	/**
	 * Convenience method to show the dialog and return the selected date record.
	 *
	 * @param parent      the parent dialog
	 * @param model       the FLEF model
	 * @param title       the dialog title
	 * @param initialDate the initial DATE_STRUCTURE record (can be null)
	 * @return the saved DATE_STRUCTURE record, or null
	 */
	public static FLEFRecord showDateDialog(final Dialog parent, final FLEFModel model, final String title,
			final FLEFRecord initialDate){
		final DateDialog dialog = createEdit(parent, model, title, initialDate);
		dialog.setVisible(true);
		return dialog.getDateRecord();
	}

}
