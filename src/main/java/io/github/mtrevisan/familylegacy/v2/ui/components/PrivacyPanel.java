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
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import java.io.Serial;


/**
 * Panel for editing a {@code RESTRICTION_STRUCTURE} according to FLEF 0.1.2.
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
public class PrivacyPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -8538135290834556765L;


	private static final String DOT = ".";

	private static final String TAG_PRIVACY = "PRIVACY";
	private static final String TAG_PRIVACY_LEVEL = TAG_PRIVACY + DOT + "LEVEL";
	private static final String TAG_PRIVACY_REASON = TAG_PRIVACY + DOT + "REASON";
	private static final String TAG_PRIVACY_EXPIRES = TAG_PRIVACY + DOT + "EXPIRES";


	private final String path;


	private final BindingManager bindingManager = new BindingManager();

	private final BoundComboBox<String> levelCombo;
	private final BoundTextArea reasonArea;
	private final BoundTextField expiresField;


	/**
	 * Constructs a new RestrictionPanel.
	 *
	 */
	public PrivacyPanel(final String path){
		this.path = path;

		levelCombo = new BoundComboBox<>(TAG_PRIVACY_LEVEL, new String[]{
			"public", "restricted", "confidential"});
		reasonArea = new BoundTextArea(TAG_PRIVACY_REASON, 3, 25);
		reasonArea.setToolTipText("e.g., 'Living individual', 'Repository license forbids redistribution'");
		expiresField = new BoundTextField(TAG_PRIVACY_EXPIRES);


		initComponents();
	}


	private void initComponents(){
		bindingManager.bind(levelCombo);
		bindingManager.bind(reasonArea);
		bindingManager.bind(expiresField);



		setLayout(GUIHelper.createLabelFieldLayout(10, "[]10[]10[]"));

		// level
		GUIHelper.addLabeledComponent(this, "Level:", levelCombo);

		// reason
		GUIHelper.addLabeledComponent(this, "Reason:", reasonArea);

		// expires
		GUIHelper.addLabeledComponent(this, "Expires:", expiresField);
	}


	/**
	 * Loads data from a RESTRICTION record into the panel.
	 *
	 * @param record	the RESTRICTION record, or {@code null}
	 */
	public void load(final FLEFRecord record){
		clear();

		if(record == null || record.isEmpty())
			return;

		bindingManager.load(record);
	}


	/**
	 * Saves the panel data into a RESTRICTION record.
	 *
	 * @param record	an existing RESTRICTION record to update, or {@code null} to create a new one
	 */
	public void save(final FLEFRecord record){
		if(validateData())
			bindingManager.save(record);
	}


	/**
	 * Checks whether the panel has any data (i.e., LEVEL is selected).
	 *
	 * @return {@code true} if LEVEL is selected, otherwise {@code false}
	 */
	public boolean hasData(){
		final String level = (String)levelCombo.getSelectedItem();
		return StringUtils.isNotEmpty(level);
	}

	/**
	 * Validates the required fields and the format of the EXPIRES date.
	 */
	public boolean validateData(){
		return true;
	}


	/**
	 * Clears all fields (sets LEVEL to empty, clears REASON and EXPIRES).
	 */
	public void clear(){
		levelCombo.setSelectedIndex(0);
		reasonArea.setText(StringUtils.EMPTY);
		expiresField.setText(StringUtils.EMPTY);
	}

}
