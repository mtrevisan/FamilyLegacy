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
package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.Serial;


/* DONE */
/**
 * Panel for editing a {@code RESTRICTION_STRUCTURE} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * struct RestrictionStructure {
 *   level: enum {
 *     public,
 *     restricted,
 *     confidential
 *   }
 *   reason?: enum { living_person, privacy_law, copyright, repository_license, sensitive_information } | Text
 *   expires?: Date
 * }
 * </pre>
 */
public class RestrictionPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -8538135290834556765L;


	private static final String DOT = ".";

	private static final String TAG_LEVEL = "LEVEL";
	private static final String TAG_RATIONALE = "RATIONALE";
	private static final String TAG_EXPIRES = "EXPIRES";


	private final Dialog parent;

	private final String path;

	private final BindingManager bindingManager = new BindingManager();

	private final BoundComboBox<String> levelCombo;
	private final BoundTextArea rationaleArea;
	private final BoundTextField expiresField;


	/**
	 * Constructs a new RestrictionPanel.
	 *
	 * @param parent the parent dialog (used for showing message dialogs)
	 */
	public RestrictionPanel(final String path, final Dialog parent){
		this.parent = parent;

		this.path = path;

		levelCombo = new BoundComboBox<>(path + DOT + TAG_LEVEL, new String[]{StringUtils.EMPTY, "public", "restricted", "confidential"});
		rationaleArea = new BoundTextArea(path + DOT + TAG_RATIONALE, 3, 25);
		rationaleArea.setToolTipText("e.g., 'Living individual', 'Repository license forbids redistribution'");
		expiresField = new BoundTextField(path + DOT + TAG_EXPIRES, 15);

		initComponents();
	}


	private void initComponents(){
		setLayout(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]10[]10[]"));

		bindingManager.bind(levelCombo);
		bindingManager.bind(rationaleArea);
		bindingManager.bind(expiresField);

		// LEVEL
		add(new JLabel("Level*:"), "align label");
		add(levelCombo, "growx,wrap");

		// RATIONALE
		add(new JLabel("Rationale:"), "align label,top");
		add(GUIHelper.createScrollPane(rationaleArea), "growx,wrap");

		// EXPIRES
		add(new JLabel("Expires:"), "align label");
		add(expiresField, "growx");
	}


	/**
	 * Loads data from a RESTRICTION record into the panel.
	 *
	 * @param record the RESTRICTION record, or {@code null}
	 */
	public void load(final FLEFRecord record){
		clear();

		if(record == null)
			return;

		final FLEFRecord restriction = FLEFRecordHelper.findChild(record, path);
		if(restriction == null)
			return;

		bindingManager.load(restriction);
	}


	/**
	 * Saves the panel data into a RESTRICTION record.
	 *
	 * @param targetRecord an existing RESTRICTION record to update, or {@code null} to create a new one
	 */
	public void save(final FLEFRecord targetRecord){
		FLEFRecordHelper.removeChild(targetRecord, path);

		bindingManager.save(targetRecord);
	}


	/**
	 * Checks whether the panel has any data (i.e., LEVEL is selected).
	 *
	 * @return {@code true} if LEVEL is selected, otherwise {@code false}
	 */
	public boolean hasData(){
		final String level = (String)levelCombo.getSelectedItem();
		return (level != null && !level.isEmpty());
	}

	/**
	 * Validates the required fields and the format of the EXPIRES date.
	 *
	 * @return {@code true} if LEVEL is selected and EXPIRES (if present) is a valid ISO 8601 date,
	 * otherwise {@code false}
	 */
	public boolean validateData(){
		final String level = (String)levelCombo.getSelectedItem();
		if(level == null || level.isEmpty()){
			JOptionPane.showMessageDialog(parent, "Restriction LEVEL is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		return true;
	}


	/**
	 * Clears all fields (sets LEVEL to empty, clears RATIONALE and EXPIRES).
	 */
	public void clear(){
		levelCombo.setSelectedIndex(0);
		rationaleArea.setText(StringUtils.EMPTY);
		expiresField.setText(StringUtils.EMPTY);
	}

}
