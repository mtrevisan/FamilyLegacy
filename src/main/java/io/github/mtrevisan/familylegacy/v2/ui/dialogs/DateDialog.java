package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.DatePanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;


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
	 * Creates a new DateDialog with an initial date.
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
		datePanel.load(initialDate);

		initComponents();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		setLayout(new MigLayout("ins 10,fill,wrap 1", "[grow]", "[grow][]"));

		// Date panel
		JPanel dateWrapper = new JPanel(new MigLayout("ins 0,fill"));
		dateWrapper.add(datePanel, "grow");
		add(dateWrapper, "grow");

		JPanel buttonPanel = new JPanel(new MigLayout("ins 0,align right", "[][][]", "[]"));
		JButton okButton = new JButton("OK");
		JButton cancelButton = new JButton("Cancel");
		buttonPanel.add(okButton, "gapright 5");
		buttonPanel.add(cancelButton);
		add(buttonPanel, "growx");

		okButton.addActionListener(e -> {
			if(datePanel.validateData()){
				result = datePanel.save();
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
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
		getRootPane().getActionMap().put("escape", escapeAction);
	}

	/**
	 * Returns the saved DATE_STRUCTURE record, or {@code null} if cancelled or no data.
	 *
	 * @return the DATE_STRUCTURE record, or null
	 */
	public FLEFRecord getRecord(){
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
		return dialog.getRecord();
	}

}
