package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.FlowLayout;


/**
 * Panel for editing the APPROXIMATE structure of a date.
 * <p>
 * Structure (real tags):
 * APPROXIMATE
 * +1 BASIS <APPROXIMATION_BASIS>
 * +1 CULTURAL_NORM @<XREF:CULTURAL_NORM>@
 * +1 MARGIN <DURATION>
 */
public class ApproximatePanel extends JPanel{

	private final FLEFModel model;

	private final JCheckBox approximateCheck = new JCheckBox("Approximate");
	private final JComboBox<String> basisCombo = new JComboBox<>(new String[]{"", "stated", "calculated", "conventional", "unspecified"});
	private final JTextField culturalNormField = new JTextField(15);
	private final JButton browseCulturalNormBtn = new JButton("Browse");
	private final JButton clearCulturalNormBtn = new JButton("Clear");
	private String culturalNormId;
	private final JTextField marginField = new JTextField(10);

	private final CulturalNormHandler culturalNormHandler = new CulturalNormHandler();

	public ApproximatePanel(FLEFModel model){
		this.model = model;
		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout("ins 0, fillx", "[right]rel[grow]", "[]5[]"));
		setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

		add(approximateCheck, "span 2,growx,wrap");

		// Basis
		JPanel basisPanel = new JPanel(new MigLayout("ins 0, fillx", "[right]rel[grow]"));
		basisPanel.add(new JLabel("Basis:"), "align label");
		basisPanel.add(basisCombo, "growx");
		add(basisPanel, "growx,wrap");

		// Cultural Norm
		JPanel normPanel = new JPanel(new MigLayout("ins 0, fillx", "[right]rel[grow]"));
		JPanel normFieldPanel = new JPanel(new BorderLayout(5, 0));
		normFieldPanel.add(culturalNormField, BorderLayout.CENTER);
		JPanel normBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		normBtnPanel.add(browseCulturalNormBtn);
		normBtnPanel.add(clearCulturalNormBtn);
		normFieldPanel.add(normBtnPanel, BorderLayout.EAST);
		culturalNormField.setEditable(false);
		culturalNormField.setBackground(UIManager.getColor("TextField.background"));

		normPanel.add(new JLabel("Cultural Norm:"), "align label");
		normPanel.add(normFieldPanel, "growx");
		add(normPanel, "growx,wrap");

		browseCulturalNormBtn.addActionListener(e -> browseCulturalNorm());
		clearCulturalNormBtn.addActionListener(e -> clearCulturalNorm());
		clearCulturalNormBtn.setEnabled(false);

		// Margin
		JPanel marginPanel = new JPanel(new MigLayout("ins 0, fillx", "[right]rel[grow]"));
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
		culturalNormField.setEnabled(enabled);
		browseCulturalNormBtn.setEnabled(enabled);
		clearCulturalNormBtn.setEnabled(enabled && culturalNormId != null);
		marginField.setEnabled(enabled);
	}

	private void browseCulturalNorm(){
		if(model == null){
			JOptionPane.showMessageDialog(this, "Model not available.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			(Frame)SwingUtilities.getWindowAncestor(this), model, culturalNormHandler, selectedId -> {
			if(selectedId != null){
				culturalNormId = selectedId;
				FLEFRecord rec = model.getRecordById(selectedId);
				if(rec != null){
					culturalNormField.setText(culturalNormHandler.getDisplayName(rec));
				}
				else{
					culturalNormField.setText(selectedId);
				}
				clearCulturalNormBtn.setEnabled(true);
			}
		});
		dialog.setVisible(true);
	}

	private void clearCulturalNorm(){
		culturalNormId = null;
		culturalNormField.setText("");
		clearCulturalNormBtn.setEnabled(false);
	}

	/**
	 * Loads from an APPROXIMATE record.
	 *
	 * @param approxRecord the APPROXIMATE record, or null
	 */
	public void loadFromRecord(FLEFRecord approxRecord){
		clear();
		if(approxRecord == null){
			return;
		}
		approximateCheck.setSelected(true);

		String basis = FLEFRecordUtils.getChildValue(approxRecord, "BASIS");
		basisCombo.setSelectedItem(basis != null? basis: "");

		String normId = FLEFRecordUtils.getChildValue(approxRecord, "CULTURAL_NORM");
		if(normId != null){
			culturalNormId = normId;
			FLEFRecord rec = model != null? model.getRecordById(normId): null;
			if(rec != null){
				culturalNormField.setText(culturalNormHandler.getDisplayName(rec));
			}
			else{
				culturalNormField.setText(normId);
			}
			clearCulturalNormBtn.setEnabled(true);
		}

		String margin = FLEFRecordUtils.getChildValue(approxRecord, "MARGIN");
		marginField.setText(margin != null? margin: "");

		updateEnabled();
	}

	/**
	 * Saves the approximate data into an APPROXIMATE child of the given parent record.
	 * If the check box is not selected, does nothing.
	 *
	 * @param parent the parent record (e.g., VALUE, NOT_BEFORE, etc.)
	 */
	public void saveToRecord(FLEFRecord parent){
		if(!approximateCheck.isSelected()){
			return;
		}

		// Create an APPROXIMATE node as a child of parent
		FLEFRecord approx = FLEFRecord.createChild(1, "APPROXIMATE");

		String basis = (String)basisCombo.getSelectedItem();
		if(basis != null && !basis.isEmpty()){
			approx.addChild(FLEFRecord.createChildWithValue(2, "BASIS", basis));
		}

		if(culturalNormId != null && !culturalNormId.isEmpty()){
			approx.addChild(FLEFRecord.createChildWithValue(2, "CULTURAL_NORM", culturalNormId));
		}

		String margin = marginField.getText().trim();
		if(!margin.isEmpty()){
			approx.addChild(FLEFRecord.createChildWithValue(2, "MARGIN", margin));
		}

		// Only add if there is at least one child
		if(approx.hasChildren()){
			parent.addChild(approx);
		}
	}

	public void clear(){
		approximateCheck.setSelected(false);
		basisCombo.setSelectedIndex(0);
		culturalNormId = null;
		culturalNormField.setText("");
		clearCulturalNormBtn.setEnabled(false);
		marginField.setText("");
		updateEnabled();
	}

	public boolean hasData(){
		return approximateCheck.isSelected();
	}

	public boolean validateRequiredFields(){
		if(approximateCheck.isSelected()){
			String basis = (String)basisCombo.getSelectedItem();
			if(basis == null || basis.isEmpty()){
				JOptionPane.showMessageDialog(this, "Basis is required when Approximate is selected.",
					"Validation Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			if("conventional".equals(basis) && (culturalNormId == null || culturalNormId.isEmpty())){
				JOptionPane.showMessageDialog(this, "Cultural Norm is required when Basis is 'conventional'.",
					"Validation Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		return true;
	}

}
