package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.DatePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;


/**
 * Dialog for editing a {@code DATE_STRUCTURE} according to FLEF 0.1.0.
 * <p>
 * This dialog opens a {@link DatePanel} for editing a date structure.
 * It is invoked from other panels when a date needs to be edited.
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
	 * @param initialDate the initial DATE_STRUCTURE record (can be null)
	 */
	public DateDialog(Dialog parent, FLEFModel model, String title, FLEFRecord initialDate){
		super(parent, title, true);

		datePanel = new DatePanel(model, this);
		datePanel.loadFromRecord(initialDate);

		initComponents();
		pack();
		setMinimumSize(new Dimension(600, 450));
		setLocationRelativeTo(parent);
	}

	private void initComponents(){
		setLayout(new BorderLayout(10, 10));

		// Date panel
		JPanel panel = new JPanel(new BorderLayout(10, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.add(datePanel, BorderLayout.CENTER);
		add(panel, BorderLayout.CENTER);

		// Buttons
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okButton = new JButton("OK");
		JButton cancelButton = new JButton("Cancel");
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
		Action escapeAction = new AbstractAction(){
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
	public static FLEFRecord showDateDialog(Dialog parent, FLEFModel model, String title, FLEFRecord initialDate){
		DateDialog dialog = new DateDialog(parent, model, title, initialDate);
		dialog.setVisible(true);
		return dialog.getDateRecord();
	}

}
