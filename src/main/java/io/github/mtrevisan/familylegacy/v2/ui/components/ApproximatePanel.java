package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.io.Serial;


/**
 * Panel for editing the APPROXIMATE structure of a date.
 * <p>
 * Structure (real tags):
 * APPROXIMATE
 * +1 BASIS <APPROXIMATION_BASIS>    {0:1}
 * +1 CULTURAL_NORM @<XREF:CULTURAL_NORM>@    {0:1}
 * +1 MARGIN <DURATION>    {0:1}
 */
public class ApproximatePanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 5106748907289247149L;


	static{
		HandlerRegistry.register(new CulturalNormHandler());
	}

	private final Dialog parentDialog;

	private final String path;
	private final FLEFModel model;

	private final JCheckBox approximateCheck = new JCheckBox("Approximate");
	private final JComboBox<String> basisCombo = new JComboBox<>(new String[]{StringUtils.EMPTY, "stated", "calculated", "conventional", "unspecified"});
	private final JTextField culturalNormField = new JTextField(15);
	private final JButton browseCulturalNormBtn = new JButton("Browse");
	private final JButton clearCulturalNormBtn = new JButton("Clear");
	private String culturalNormId;
	private final JTextField marginField = new JTextField(10);

	private final CulturalNormHandler culturalNormHandler = new CulturalNormHandler();

	public ApproximatePanel(String path, Dialog parent, FLEFModel model){
		this.parentDialog = parent;

		this.path = path;
		this.model = model;

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
		JPanel normPanel = new JPanel(new MigLayout("ins 0,fillx", "[right]rel[grow]"));
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
			parentDialog, model, culturalNormHandler, selectedId -> {
			if(selectedId != null){
				culturalNormId = selectedId;
				FLEFRecord rec = model.getRecordById(selectedId);
				if(rec != null){
					culturalNormField.setText(culturalNormHandler.getDisplayText(rec, model));
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
		culturalNormField.setText(StringUtils.EMPTY);
		clearCulturalNormBtn.setEnabled(false);
	}

	public void loadFromRecord(FLEFRecord record){
		clear();

		final FLEFRecord approxRecord = FLEFRecordHelper.findChild(record, path);
		if(approxRecord == null)
			return;

		approximateCheck.setSelected(true);

		String basis = FLEFRecordHelper.getChildValue(approxRecord, "BASIS");
		basisCombo.setSelectedItem(StringUtils.defaultString(basis));

		String normId = FLEFRecordHelper.getChildValue(approxRecord, "CULTURAL_NORM");
		if(normId != null){
			culturalNormId = normId;
			FLEFRecord rec = model != null? model.getRecordById(normId): null;
			if(rec != null){
				culturalNormField.setText(culturalNormHandler.getDisplayText(rec, model));
			}
			else{
				culturalNormField.setText(normId);
			}
			clearCulturalNormBtn.setEnabled(true);
		}

		String margin = FLEFRecordHelper.getChildValue(approxRecord, "MARGIN");
		marginField.setText(StringUtils.defaultString(margin));

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
		if(basis != null && !basis.isEmpty()){
			approx.addChild(FLEFRecord.createChildWithValue("BASIS", basis));
		}

		if(culturalNormId != null && !culturalNormId.isEmpty()){
			approx.addChild(FLEFRecord.createChildWithValue("CULTURAL_NORM", XRefHelper.formatXRef(culturalNormId)));
		}

		String margin = marginField.getText().trim();
		if(!margin.isEmpty()){
			approx.addChild(FLEFRecord.createChildWithValue("MARGIN", margin));
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
		culturalNormField.setText(StringUtils.EMPTY);
		clearCulturalNormBtn.setEnabled(false);
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
			if("conventional".equals(basis) && (culturalNormId == null || culturalNormId.isEmpty())){
				JOptionPane.showMessageDialog(this, "Cultural Norm is required when Basis is 'conventional'.",
					"Validation Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		return true;
	}

}
