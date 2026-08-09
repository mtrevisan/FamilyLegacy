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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dialog;
import java.io.Serial;


/**
 * Panel for editing the APPROXIMATE structure of a date.
 * <p>
 * Structure:
 * <pre>
 * struct Approximate {
 *   basis?: enum {
 *     stated,
 *     calculated,
 *     conventional,
 *     unspecified
 *   }
 *   cultural_norm?: Xref&lt;CulturalNormRecord&gt;
 *   margin?: Duration
 *   require if basis == conventional: cultural_norm
 * }
 * </pre>
 */
public class ApproximatePanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 5106748907289247149L;


	private static final String TAG_BASIS = "BASIS";
	private static final String TAG_CULTURAL_NORM = "CULTURAL_NORM";
	private static final String TAG_MARGIN = "MARGIN";


	private final String path;

	private final JCheckBox approximateCheck = new JCheckBox("Approximate");
	private final JComboBox<String> basisCombo = new JComboBox<>(new String[]{StringUtils.EMPTY, "stated", "calculated", "conventional", "unspecified"});
	private final CulturalNormListPanel culturalNormPanel;
	private final JTextField marginField = new JTextField(10);


	public ApproximatePanel(String path, Dialog parent, FLEFModel model){
		this.path = path;

		culturalNormPanel = new CulturalNormListPanel(TAG_CULTURAL_NORM, parent, model);


		initComponents();
	}


	private void initComponents(){
		setLayout(new MigLayout("ins 0,fillx", "[right]rel[grow]", "[]5[]"));
		setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

		add(approximateCheck, "span 2,growx,wrap");

		// Basis
		JPanel basisPanel = new JPanel(new MigLayout("ins 0,fillx", "[right]rel[grow]"));
		basisPanel.add(new JLabel("Basis:"), "align label");
		basisPanel.add(basisCombo, "growx");
		add(basisPanel, "growx,wrap");

		// Cultural Norm
		add(culturalNormPanel, "growx,wrap");

		// Margin
		JPanel marginPanel = new JPanel(new MigLayout("ins 0,fillx", "[right]rel[grow]"));
		marginPanel.add(new JLabel("Margin:"), "align label");
		marginPanel.add(marginField, "growx");
		marginField.setToolTipText("ISO 8601 Duration (e.g., P2Y for +/- 2 years)");
		add(marginPanel, "growx,wrap");

		approximateCheck.addActionListener(e -> updateEnabled());
		updateEnabled();
	}

	private void updateEnabled(){
		boolean enabled = approximateCheck.isSelected();
		basisCombo.setEnabled(enabled);
		marginField.setEnabled(enabled);
	}

	public void loadFromRecord(FLEFRecord record){
		clear();

		final FLEFRecord approxRecord = FLEFRecordHelper.findChild(record, path);
		if(approxRecord == null)
			return;

		approximateCheck.setSelected(true);

		String basis = FLEFRecordHelper.getChildValue(approxRecord, "BASIS");
		basisCombo.setSelectedItem(StringUtils.defaultString(basis));

		culturalNormPanel.load(record);

		String margin = FLEFRecordHelper.getChildValue(approxRecord, "MARGIN");
		marginField.setText(margin);

		updateEnabled();
	}

	/**
	 * Saves the approximate data into an APPROXIMATE child of the given parent record.
	 * If the check box is not selected, does nothing.
	 *
	 * @param parent the parent record (e.g., VALUE, NOT_BEFORE, etc.)
	 */
	public void saveToRecord(FLEFRecord parent){
		if(!approximateCheck.isSelected())
			return;

		// Create an APPROXIMATE node as a child of parent
		FLEFRecord approx = FLEFRecord.createChild(path);

		String basis = (String)basisCombo.getSelectedItem();
		if(basis != null && !basis.isEmpty())
			FLEFRecordHelper.updateChildValue(approx, TAG_BASIS, basis);

		culturalNormPanel.save(parent);

		String margin = marginField.getText()
			.trim();
		if(!margin.isEmpty())
			FLEFRecordHelper.updateChildValue(approx, TAG_MARGIN, margin);

		parent.addChild(approx);
	}

	public void clear(){
		approximateCheck.setSelected(false);
		basisCombo.setSelectedIndex(0);
		culturalNormPanel.clear();
		marginField.setText(StringUtils.EMPTY);
		updateEnabled();
	}

	public boolean hasData(){
		return approximateCheck.isSelected();
	}

	public boolean validateData(){
		if(approximateCheck.isSelected()){
			String basis = (String)basisCombo.getSelectedItem();
			if(basis == null || basis.isEmpty()){
				JOptionPane.showMessageDialog(this, "Basis is required when Approximate is selected.",
					"Validation Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			if("conventional".equals(basis) && culturalNormPanel.isEmpty()){
				JOptionPane.showMessageDialog(this, "Cultural Norm is required when Basis is 'conventional'.",
					"Validation Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		return true;
	}

}
