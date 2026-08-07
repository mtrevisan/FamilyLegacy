package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.DatePanel;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dialog;


public class DateDialog extends JDialog{

	private final DatePanel datePanel;

	private FLEFRecord result;
	private boolean saved = false;


	/**
	 * Creates a new DateDialog.
	 *
	 * @param parent	The parent dialog.
	 * @param model	The FLEF model.
	 * @param title	The dialog title.
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
	 * @param record the initial DATE_STRUCTURE record (can be null)
	 */
	public static DateDialog createEdit(final Dialog parent, final FLEFModel model, final String title,
			final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new DateDialog(parent, model, title, record);
	}


	private DateDialog(final Dialog parent, final FLEFModel model, final String title, final FLEFRecord initialDate){
		super(parent, title, ModalityType.APPLICATION_MODAL);

		datePanel = new DatePanel(this, model);
		datePanel.load(initialDate);

		initComponents();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		setLayout(new MigLayout("ins 10,fill,wrap 1", "[grow]", "[grow][]"));

		// Date panel
		final JPanel dateWrapper = new JPanel(new MigLayout("ins 0,fill"));
		dateWrapper.add(datePanel, "grow");
		add(dateWrapper, "grow");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			() -> {
				if(datePanel.validateData()){
					result = datePanel.save();
					saved = true;

					dispose();
				}
			},
			() -> {
				saved = false;
				result = null;

				dispose();
			});
		add(buttonPanel, BorderLayout.SOUTH);
	}

	/**
	 * Returns the saved DATE_STRUCTURE record, or {@code null} if canceled or no data.
	 *
	 * @return the DATE_STRUCTURE record, or null
	 */
	public FLEFRecord getRecord(){
		return (saved? result: null);
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

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

}
