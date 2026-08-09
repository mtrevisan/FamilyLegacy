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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.DatePanel;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;


public class DateDialog extends JDialog{

	private final DatePanel datePanel;

	private FLEFRecord result;
	private boolean saved = false;


	/**
	 * Creates a new dialog to create a new record.
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @return	A new dialog instance.
	 */
	public static DateDialog createNew(final Dialog parent, final FLEFModel model, final String title){
		return new DateDialog(parent, model, title, null);
	}

	/**
	 * Creates a new dialog to edit an existing record.
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @param record	The record to edit (must not be {@code null}).
	 * @return	A new dialog instance.
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


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final DateDialog dialog = DateDialog.createNew(null, model, "Date Test");
			dialog.setVisible(true);
		});
	}

}
