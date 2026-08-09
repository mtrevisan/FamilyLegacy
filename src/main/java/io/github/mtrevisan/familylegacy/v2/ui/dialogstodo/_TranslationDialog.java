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
package io.github.mtrevisan.familylegacy.v2.ui.dialogstodo;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.Serial;


/**
 * Dialog for editing a {@code TRANSLATION} structure according to FLEF 0.0.9.
 * <p>
 * Structure:
 * <pre>
 * TRANSLATION :=
 *   +1 LOCALE <LOCALE_CODE>    {0:1}
 *   +1 VALUE <SUBMITTER_TRANSLATED_TEXT>    {1:1}
 *   +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class _TranslationDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 6510714972204378850L;


	private final FLEFRecord transRecord;
	private boolean saved = false;

	private final JTextField localeField = new JTextField(10);
	private final JTextArea valueArea = new JTextArea(3, 20);

	private final ModificationPanel modificationPanel;

	private final JButton okButton = new JButton("OK");
	private final JButton cancelButton = new JButton("Cancel");

	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);


	/**
	 * Creates a dialog to edit an existing translation.
	 *
	 * @param parent      the parent dialog
	 * @param model       the FLEF model
	 * @param transRecord the translation record to edit
	 */
	public _TranslationDialog(JDialog parent, FLEFModel model, FLEFRecord transRecord){
		super(parent, transRecord == null? "Add Translation": "Edit Translation", true);

		this.transRecord = transRecord != null? transRecord: FLEFRecord.createChild("TRANSLATION");
		this.modificationPanel = new ModificationPanel(this);


		initComponents();

		if(transRecord != null)
			loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	/**
	 * Creates a dialog to create a new translation.
	 *
	 * @param parent the parent dialog
	 * @param model  the FLEF model
	 */
	public _TranslationDialog(JDialog parent, FLEFModel model){
		this(parent, model, null);
	}


	private void initComponents(){
		setLayout(new BorderLayout(10, 10));

		JTabbedPane tabbedPane = new JTabbedPane();

		JPanel basicPanel = new JPanel(new MigLayout("ins 10", "[right]rel[grow]", "[]10[]"));

		basicPanel.add(new JLabel("Locale:"), "align label");
		basicPanel.add(localeField, "growx,wrap");

		basicPanel.add(new JLabel("Value:"), "align label,top");
		JScrollPane scroll = GUIHelper.createScrollPane(valueArea);
		basicPanel.add(scroll, "growx,wrap");

		tabbedPane.addTab("Basic", basicPanel);

		tabbedPane.addTab("Modification", modificationPanel);

		add(tabbedPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		btnPanel.add(okButton);
		btnPanel.add(cancelButton);
		add(btnPanel, BorderLayout.SOUTH);

		okButton.addActionListener(e -> {
			if(validateData()){
				saveData();
				saved = true;
				dispose();
			}
		});
		cancelButton.addActionListener(e -> dispose());
	}


	private void loadData(){
		localeField.setText(FLEFRecordHelper.getChildValue(transRecord, "LOCALE"));
		valueArea.setText(FLEFRecordHelper.getChildValue(transRecord, "VALUE"));

		// MODIFICATION
		modificationPanel.load(transRecord);
	}

	private boolean validateData(){
		if(StringUtils.isEmpty(valueArea.getText())){
			JOptionPane.showMessageDialog(this,
				"VALUE is required for a translation.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			valueArea.requestFocusInWindow();

			return false;
		}

		return true;
	}

	private void saveData(){
		FLEFRecordHelper.removeAllChildren(transRecord);

		// Main fields
		FLEFRecordHelper.updateChildValue(transRecord, "LOCALE", localeField.getText().trim());
		FLEFRecordHelper.updateChildValue(transRecord, "VALUE", valueArea.getText().trim());

		// MODIFICATION
		modificationPanel.save(transRecord);
	}

	public boolean isSaved(){
		return saved;
	}

	public FLEFRecord getTranslationRecord(){
		return transRecord;
	}


	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){}

		FLEFModel model = new FLEFModel();
		HandlerRegistry.register(new NoteHandler());

		SwingUtilities.invokeLater(() -> {
			JDialog parent = new JDialog();
			parent.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			_TranslationDialog dialog = new _TranslationDialog(parent, model);
			dialog.setVisible(true);

			if(dialog.isSaved()){
				System.out.println("Translation saved.");
			}
			System.exit(0);
		});
	}

}
