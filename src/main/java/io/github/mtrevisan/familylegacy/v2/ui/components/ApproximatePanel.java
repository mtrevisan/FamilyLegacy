package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;


/**
 * Common approximate fields for a single date.
 */
public class ApproximatePanel extends JPanel{
	private final JCheckBox approximateCheck = new JCheckBox("Approximate");
	private final JComboBox<String> basisCombo = new JComboBox<>(new String[]{"", "stated", "calculated", "conventional", "unspecified"});
	private final JTextField culturalNormField = new JTextField(15);
	private final JButton browseCulturalNormBtn = new JButton("Browse");
	private final JButton clearCulturalNormBtn = new JButton("Clear");
	private String culturalNormId;
	private final JTextField marginField = new JTextField(10);

	private final CulturalNormHandler culturalNormHandler = new CulturalNormHandler();

	ApproximatePanel(){
		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout("ins 0, fillx", "[right]rel[grow]", "[]5[]5"));
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
		if(culturalNormHandler == null){
			JOptionPane.showMessageDialog(this, "Cultural Norm handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			(Frame)SwingUtilities.getWindowAncestor(this), null, culturalNormHandler, selectedId -> {
			if(selectedId != null){
				culturalNormId = selectedId;
				culturalNormField.setText(culturalNormHandler.getDisplayName(null));
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

	public void loadFromRecord(FLEFRecord approxRecord){
		if(approxRecord == null){
			clear();
			return;
		}
		approximateCheck.setSelected(true);
		String basis = FLEFRecordUtils.getChildValue(approxRecord, "BASIS");
		basisCombo.setSelectedItem(basis != null? basis: "");
		String normId = FLEFRecordUtils.getChildValue(approxRecord, "CULTURAL_NORM");
		if(normId != null){
			culturalNormId = normId;
			culturalNormField.setText(culturalNormHandler != null? culturalNormHandler.getDisplayName(null): normId);
			clearCulturalNormBtn.setEnabled(true);
		}
		String margin = FLEFRecordUtils.getChildValue(approxRecord, "MARGIN");
		marginField.setText(margin != null? margin: "");
		updateEnabled();
	}

	public void saveToRecord(FLEFRecord target){
		if(!approximateCheck.isSelected()) return;
		FLEFRecordUtils.updateChildValue(target, "BASIS", (String)basisCombo.getSelectedItem());
		if(culturalNormId != null && !culturalNormId.isEmpty()){
			FLEFRecordUtils.updateChildValue(target, "CULTURAL_NORM", culturalNormId);
		}
		String margin = marginField.getText().trim();
		if(!margin.isEmpty()){
			FLEFRecordUtils.updateChildValue(target, "MARGIN", margin);
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
