package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.io.Serial;
import java.util.List;


/**
 * Panel for managing a list of TEXT_VALUE_VARIANT entries (PHONETIC/TRANSCRIPTION).
 * <p>
 * Provides:
 * <ul>
 *   <li>Add a new variant</li>
 *   <li>Edit an existing variant</li>
 *   <li>Remove a variant</li>
 * </ul>
 */
public class VariantListPanel extends AbstractListPanel<VariantEntry>{

	@Serial
	private static final long serialVersionUID = -298718064629353117L;


	public VariantListPanel(FLEFModel model, Dialog parentDialog){
		super(model, parentDialog, "Variants");
	}

	public VariantListPanel(FLEFModel model, Dialog parentDialog, String borderTitle){
		super(model, parentDialog, borderTitle);
	}

	@Override
	protected String getDisplay(VariantEntry item){
		return item.toString();
	}

	@Override
	protected VariantEntry showAddDialog(){
		return showVariantDialog(null);
	}

	@Override
	protected VariantEntry showEditDialog(VariantEntry existing){
		return showVariantDialog(existing);
	}

	/**
	 * Shows a dialog to add or edit a variant.
	 *
	 * @param initial the existing variant, or {@code null} for a new one
	 * @return the updated variant, or {@code null} if canceled
	 */
	private VariantEntry showVariantDialog(VariantEntry initial){
		JDialog dialog = new JDialog(parentDialog,
			initial == null? "Add Variant": "Edit Variant", true);
		dialog.setLayout(new MigLayout("ins 10, fillx", "[right]rel[grow]", "[]5[]5[]"));

		JComboBox<String> typeCombo = new JComboBox<>(new String[]{"PHONETIC", "TRANSCRIPTION"});
		if(initial != null){
			typeCombo.setSelectedItem(initial.getType());
		}

		JTextField systemField = new JTextField(20);
		if(initial != null){
			systemField.setText(initial.getSystem());
		}
		systemField.setToolTipText("e.g., 'ipa', 'romaji', 'pinyin', 'wadegiles'");

		JComboBox<String> transTypeCombo = new JComboBox<>(new String[]{StringUtils.EMPTY, "romanized", "anglicized", "cyrillized", "francized", "gairaigized", "latinized"});
		if(initial != null && "TRANSCRIPTION".equals(initial.getType()) && initial.getTranscriptionType() != null){
			transTypeCombo.setSelectedItem(initial.getTranscriptionType());
		}
		transTypeCombo.setEnabled("TRANSCRIPTION".equals(typeCombo.getSelectedItem()));

		typeCombo.addActionListener(e -> {
			transTypeCombo.setEnabled("TRANSCRIPTION".equals(typeCombo.getSelectedItem()));
		});

		JTextField valueField = new JTextField(20);
		if(initial != null){
			valueField.setText(initial.getValue());
		}

		dialog.add(new JLabel("Type:"), "align label");
		dialog.add(typeCombo, "growx,wrap");

		dialog.add(new JLabel("System:"), "align label");
		dialog.add(systemField, "growx,wrap");

		dialog.add(new JLabel("Transcription Type:"), "align label");
		dialog.add(transTypeCombo, "growx,wrap");

		dialog.add(new JLabel("Value:"), "align label");
		dialog.add(valueField, "growx,wrap");

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		dialog.add(btnPanel, "span 2,growx");

		final VariantEntry[] result = {null};
		okBtn.addActionListener(e -> {
			String type = (String)typeCombo.getSelectedItem();
			String system = systemField.getText().trim();
			String value = valueField.getText().trim();

			if(system.isEmpty() || value.isEmpty()){
				JOptionPane.showMessageDialog(dialog,
					"System and Value are required.",
					"Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			String transType = "TRANSCRIPTION".equals(type)? (String)transTypeCombo.getSelectedItem(): null;
			result[0] = new VariantEntry(type, system,
				transType != null && !transType.isEmpty()? transType: null,
				value);
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(parentDialog);
		dialog.setVisible(true);
		return result[0];
	}

	/**
	 * Loads a list of variants into the panel.
	 *
	 * @param variants the list of variants
	 */
	public void loadVariants(List<VariantEntry> variants){
		setItems(variants);
	}

	/**
	 * Returns the list of variants.
	 *
	 * @return the variants
	 */
	public List<VariantEntry> getVariants(){
		return getItems();
	}

}

