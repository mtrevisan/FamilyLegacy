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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs.structures;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.DateStructurePanel;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;
import java.util.Objects;


public class DateStructureDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 4594342471311960844L;


	private final DateStructurePanel datePanel;

	private FLEFRecord result;
	private boolean saved;


	/**
	 * Creates a new dialog to create a new record.
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @return	A new dialog instance.
	 */
	public static DateStructureDialog createNew(final Dialog parent, final FLEFModel model, final String title){
		return new DateStructureDialog(parent, model, title, null);
	}

	/**
	 * Creates a new dialog to edit an existing record.
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @param record	The record to edit (must not be {@code null}).
	 * @return	A new dialog instance.
	 */
	public static DateStructureDialog createEdit(final Dialog parent, final FLEFModel model, final String title,
			final FLEFRecord record){
		Objects.requireNonNull(record, "Record cannot be null");

		return new DateStructureDialog(parent, model, title, record);
	}


	private DateStructureDialog(final Dialog parent, final FLEFModel model, final String title, final FLEFRecord record){
		super(parent, title, ModalityType.APPLICATION_MODAL);

		datePanel = new DateStructurePanel(this, model);
		datePanel.load(record);


		initComponents();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		setLayout(GUIHelper.createLabelFieldLayout(10, "[grow][]"));

		// Date panel
		final JPanel dateWrapper = GUIHelper.createLabelFieldPanel(0, "[]");
		GUIHelper.addComponent(dateWrapper, datePanel);
		GUIHelper.addComponent(this, dateWrapper);

		final JPanel buttonPanel = GUIHelper.createSaveCancelButtonPanel(this,
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


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final DateStructureDialog dialog = DateStructureDialog.createNew(null, model, "Date Test");
			dialog.setVisible(true);
		});
	}

}
