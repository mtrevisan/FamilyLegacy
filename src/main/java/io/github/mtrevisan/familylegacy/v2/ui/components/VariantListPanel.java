package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.TextValueVariantDialog;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
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
public class VariantListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = -298718064629353117L;


	public VariantListPanel(final Dialog parentDialog, final FLEFModel model){
		super(parentDialog, "Variants", model);
	}

	public VariantListPanel(final Dialog parentDialog, final String borderTitle, final FLEFModel model){
		super(parentDialog, borderTitle, model);
	}


	@Override
	protected void initComponents(){
		super.initComponents();

		GUIHelper.installBehavior(list,
			this::editItem,
			this::createNewItem,
			this::removeItem,
			builder -> {
				builder.item("Create New...", this::createNewItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			});
	}

	@Override
	protected String getDisplay(final FLEFRecord item){
		return item.toString();
	}

	@Override
	protected FLEFRecord showAddDialog(){
		// For text variants, "Add" is the same as "Create New"
		return showCreateNewDialog();
	}

	/**
	 * Creates a new variant and adds it to the list.
	 */
	@Override
	protected FLEFRecord showCreateNewDialog(){
		final TextValueVariantDialog dialog = TextValueVariantDialog.createNew(parentDialog, model);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parentDialog, "Text Variant entry not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		return showTextVariantDialog(existing);
	}

	/**
	 * Shows a dialog to create or edit a text variant entry.
	 *
	 * @param existing the existing text variant record, or {@code null} for a new one
	 * @return the (possibly updated) record, or {@code null} if cancelled
	 */
	private FLEFRecord showTextVariantDialog(FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parentDialog, "Text Variant not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final JDialog dialog = TextValueVariantDialog.createEdit(parentDialog, model, existing);
		dialog.setVisible(true);

		// Return the same record (it was updated in place)
		return existing;
	}

	/**
	 * Shows a dialog to add or edit a variant.
	 *
	 * @param initial the existing variant, or {@code null} for a new one
	 * @return the updated variant, or {@code null} if canceled
	 */
	private VariantEntry showVariantDialog(final VariantEntry initial){
		final JDialog dialog = new JDialog(parentDialog,
			initial == null? "Add Variant": "Edit Variant", true);
		dialog.setLayout(new MigLayout("ins 10,fillx", "[right]rel[grow]", "[]5[]5[]"));

		final JComboBox<String> typeCombo = new JComboBox<>(new String[]{"PHONETIC", "TRANSCRIPTION"});
		if(initial != null)
			typeCombo.setSelectedItem(initial.getType());

		final JTextField systemField = new JTextField(20);
		if(initial != null)
			systemField.setText(initial.getSystem());
		systemField.setToolTipText("e.g., 'ipa', 'romaji', 'pinyin', 'wadegiles'");

		final JComboBox<String> transTypeCombo = new JComboBox<>(new String[]{StringUtils.EMPTY, "romanized", "anglicized", "cyrillized", "francized", "gairaigized", "latinized"});
		if(initial != null && "TRANSCRIPTION".equals(initial.getType()) && initial.getTranscriptionType() != null)
			transTypeCombo.setSelectedItem(initial.getTranscriptionType());
		transTypeCombo.setEnabled("TRANSCRIPTION".equals(typeCombo.getSelectedItem()));

		typeCombo.addActionListener(e -> {
			transTypeCombo.setEnabled("TRANSCRIPTION".equals(typeCombo.getSelectedItem()));
		});

		final JTextField valueField = new JTextField(20);
		if(initial != null)
			valueField.setText(initial.getValue());

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
			final String type = (String)typeCombo.getSelectedItem();
			final String system = systemField.getText().trim();
			final String value = valueField.getText().trim();
			if(system.isEmpty() || value.isEmpty()){
				JOptionPane.showMessageDialog(dialog,
					"System and Value are required.",
					"Validation Error", JOptionPane.ERROR_MESSAGE);

				return;
			}

			final String transType = "TRANSCRIPTION".equals(type)? (String)transTypeCombo.getSelectedItem(): null;
			result[0] = new VariantEntry(type, system,
				(transType != null && !transType.isEmpty()? transType: null),
				value);
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(parentDialog);
		dialog.setVisible(true);

		return result[0];
	}

	//TODO
	public void load(final FLEFRecord record){
		clear();
//		for(final FLEFRecord child : record.getChildren()){
//			if("PHONETIC".equals(child.getTag()) || "TRANSCRIPTION".equals(child.getTag())){
//				variantRecords.add(child);
//				variantListModel.addElement(getVariantDisplay(child));
//			}
//		}
	}

	//TODO
	public void save(final FLEFRecord record){
//		for(final VariantEntry variant : variantPanel.getVariants()){
//			record.addChild(variant);
//		}
	}

	/**
	 * Returns the list of variants.
	 *
	 * @return the variants
	 */
	public List<FLEFRecord> getVariants(){
		return getItems();
	}

}

