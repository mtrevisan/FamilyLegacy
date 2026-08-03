package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/* ONGOING */
/**
 * Panel for managing a list of PART records that belong to a PERSONAL_NAME_STRUCTURE.
 * Each part is stored as a FLEFRecord with tag "PART" and children:
 * <ul>
 *   <li>TYPE (optional) – the type of the part (given, family, etc.)</li>
 *   <li>VALUE – the textual value</li>
 *   <li>PHONETIC or TRANSCRIPTION variants (optional)</li>
 * </ul>
 * <p>
 * This panel extends {@link AbstractListPanel} and follows the same pattern
 * as {@link NoteListPanel} and {@link CulturalNormListPanel}.
 */
public class PartListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = 1L;

	private static final String TAG_PART = "PART";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_PHONETIC = "PHONETIC";
	private static final String TAG_TRANSCRIPTION = "TRANSCRIPTION";
	private static final String TAG_SYSTEM = "SYSTEM";  // not used directly, stored as value of PHONETIC/TRANSCRIPTION

	private final Dialog parentDialog;

	/**
	 * Constructs a PartListPanel with a titled border.
	 *
	 * @param parentDialog the parent dialog (for showing modal dialogs)
	 * @param model        the FLEF model
	 */
	public PartListPanel(final Dialog parentDialog, final FLEFModel model){
		this(parentDialog, "Parts", model);
	}

	/**
	 * Constructs a PartListPanel with a custom border title.
	 *
	 * @param parentDialog the parent dialog
	 * @param borderTitle  the border title, or {@code null} for no border
	 * @param model        the FLEF model
	 */
	public PartListPanel(final Dialog parentDialog, final String borderTitle, final FLEFModel model){
		super(parentDialog, borderTitle, model);

		this.parentDialog = parentDialog;
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
	protected String getDisplay(final FLEFRecord part){
		if(part == null)
			return "--";

		String type = FLEFRecordHelper.getChildValue(part, TAG_TYPE);
		String value = FLEFRecordHelper.getChildValue(part, TAG_VALUE);
		StringBuilder sb = new StringBuilder();
		if(StringUtils.isNotEmpty(type)){
			sb.append("[").append(type).append("] ");
		}
		sb.append(StringUtils.defaultString(value));
		// Count variants
		int variantCount = 0;
		for(FLEFRecord child : part.getChildren()){
			if(TAG_PHONETIC.equals(child.getTag()) || TAG_TRANSCRIPTION.equals(child.getTag())){
				variantCount++;
			}
		}
		if(variantCount > 0){
			sb.append(" (").append(variantCount).append(" variants)");
		}
		return sb.toString();
	}

	@Override
	protected FLEFRecord showAddDialog(){
		// For parts, "Add" is the same as "Create New"
		return showCreateNewDialog();
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		return showPartDialog(null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parentDialog, "Part not found", "Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		return showPartDialog(existing);
	}

	/**
	 * Shows a dialog to create or edit a PART record.
	 *
	 * @param initial the existing PART record, or {@code null} for a new one
	 * @return the (possibly updated) record, or {@code null} if cancelled
	 */
	private FLEFRecord showPartDialog(final FLEFRecord initial){
		JDialog dialog = new JDialog(parentDialog, initial == null? "Add Part": "Edit Part", true);
		dialog.setLayout(new MigLayout("ins 10, fillx", "[right]rel[grow]", "[]5[]5[]"));

		// Part Type combo
		JComboBox<String> typeCombo = new JComboBox<>(new String[]{
			StringUtils.EMPTY,
			// Personal and birth names:
			"given", "generation",
			// Direct family relationships (Descent)
			"patronymic", "matronymic", "kunya",
			// Extended family and social belonging
			"family", "family nickname", "lineage", "house", "clan", "tribal", "caste",
			// Geographical and territorial origin
			"toponymic",
			// Titles, roles and professions
			"title", "occupational", "prefix", "suffix",
			// Assumed names, nicknames and contextual
			"nickname", "regnal", "religious", "posthumous"
		});
		if(initial != null){
			String currentType = FLEFRecordHelper.getChildValue(initial, TAG_TYPE);
			if(currentType != null){
				typeCombo.setSelectedItem(currentType);
			}
		}
		dialog.add(new JLabel("Part Type:"), "align label");
		dialog.add(typeCombo, "growx,wrap");

		// Value field
		JTextField valueField = new JTextField(25);
		if(initial != null){
			String currentValue = FLEFRecordHelper.getChildValue(initial, TAG_VALUE);
			valueField.setText(StringUtils.defaultString(currentValue));
		}
		dialog.add(new JLabel("Value:"), "align label");
		dialog.add(valueField, "growx,wrap");

		// Variants panel (list of PHONETIC/TRANSCRIPTION children)
		JPanel variantsPanel = new JPanel(new MigLayout("ins 0, fillx", "[grow]"));
		variantsPanel.setBorder(new TitledBorder("Variants"));
		DefaultListModel<FLEFRecord> variantModel = new DefaultListModel<>();
		JList<FLEFRecord> variantList = new JList<>(variantModel);
		List<FLEFRecord> currentVariants = new ArrayList<>();
		if(initial != null){
			for(FLEFRecord child : initial.getChildren()){
				if(TAG_PHONETIC.equals(child.getTag()) || TAG_TRANSCRIPTION.equals(child.getTag())){
					currentVariants.add(child);
					variantModel.addElement(child);
				}
			}
		}
		JScrollPane variantScroll = GUIHelper.createScrollPane(variantList);

		GUIHelper.installBehavior(variantList,
			() -> { // double-click → edit
				int idx = variantList.getSelectedIndex();
				if(idx != -1){
					FLEFRecord current = currentVariants.get(idx);
					FLEFRecord updated = showVariantDialog(dialog, current);
					if(updated != null){
						currentVariants.set(idx, updated);
						variantModel.set(idx, updated);
					}
				}
			},
			() -> { // INSERT → add
				FLEFRecord newVariant = showVariantDialog(dialog, null);
				if(newVariant != null){
					currentVariants.add(newVariant);
					variantModel.addElement(newVariant);
				}
			},
			() -> { // DELETE → remove
				int idx = variantList.getSelectedIndex();
				if(idx != -1){
					if(JOptionPane.showConfirmDialog(dialog, "Remove this variant?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
						currentVariants.remove(idx);
						variantModel.remove(idx);
					}
				}
			},
			builder -> {
				builder.item("Add Variant...", () -> {
					FLEFRecord newVariant = showVariantDialog(dialog, null);
					if(newVariant != null){
						currentVariants.add(newVariant);
						variantModel.addElement(newVariant);
					}
				});
				builder.separator();
				builder.selectionSensitiveItem("Edit Variant...", () -> {
					int idx = variantList.getSelectedIndex();
					if(idx != -1){
						FLEFRecord current = currentVariants.get(idx);
						FLEFRecord updated = showVariantDialog(dialog, current);
						if(updated != null){
							currentVariants.set(idx, updated);
							variantModel.set(idx, updated);
						}
					}
				});
				builder.selectionSensitiveItem("Remove Variant", () -> {
					int idx = variantList.getSelectedIndex();
					if(idx != -1){
						if(JOptionPane.showConfirmDialog(dialog, "Remove this variant?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
							currentVariants.remove(idx);
							variantModel.remove(idx);
						}
					}
				});
			});

		variantsPanel.add(variantScroll, "growx,wrap");
		dialog.add(variantsPanel, "span 2,growx,wrap");

		// OK / Cancel
		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		dialog.add(btnPanel, "span 2,growx");

		final FLEFRecord[] result = {null};
		okBtn.addActionListener(e -> {
			String type = (String)typeCombo.getSelectedItem();
			String value = valueField.getText().trim();

			if(StringUtils.isEmpty(value)){
				JOptionPane.showMessageDialog(dialog, "Value cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			FLEFRecord partRecord = (initial != null)? initial: FLEFRecord.createEmpty();
			partRecord.setTag(TAG_PART);
			if(StringUtils.isNotEmpty(type)){
				FLEFRecordHelper.updateChildValue(partRecord, TAG_TYPE, type);
			}
			else{
				FLEFRecordHelper.removeChildren(partRecord, TAG_TYPE);
			}
			FLEFRecordHelper.updateChildValue(partRecord, TAG_VALUE, value);

			// Remove old variants
			for(FLEFRecord child : partRecord.getChildren()){
				if(TAG_PHONETIC.equals(child.getTag()) || TAG_TRANSCRIPTION.equals(child.getTag())){
					partRecord.removeChild(child);
				}
			}
			// Add current variants
			for(FLEFRecord variant : currentVariants){
				partRecord.addChild(variant);
			}

			result[0] = partRecord;
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(parentDialog);
		dialog.setVisible(true);

		return result[0];
	}

	/**
	 * Shows a dialog to create or edit a variant (PHONETIC or TRANSCRIPTION) record.
	 *
	 * @param initial the existing variant record, or {@code null} for a new one
	 * @return the (possibly updated) record, or {@code null} if cancelled
	 */
	private FLEFRecord showVariantDialog(final Dialog parent, final FLEFRecord initial){
		JDialog dialog = new JDialog(parent, initial == null? "Add Variant": "Edit Variant", true);
		dialog.setLayout(new MigLayout("ins 10, fillx", "[right]rel[grow]", "[]5[]5[]"));

		// Type: PHONETIC or TRANSCRIPTION
		JComboBox<String> typeCombo = new JComboBox<>(new String[]{"PHONETIC", "TRANSCRIPTION"});
		if(initial != null){
			typeCombo.setSelectedItem(initial.getTag());
		}

		// System field (stored as the value of the variant record)
		JTextField systemField = new JTextField(20);
		if(initial != null){
			systemField.setText(StringUtils.defaultString(initial.getValue()));
		}
		systemField.setToolTipText("e.g., 'ipa', 'romaji', 'pinyin', 'wadegiles'");

		// Transcription type (only for TRANSCRIPTION)
		JComboBox<String> transTypeCombo = new JComboBox<>(new String[]{StringUtils.EMPTY, "romanized", "anglicized", "cyrillized", "francized", "gairaigized", "latinized"});
		if(initial != null && TAG_TRANSCRIPTION.equals(initial.getTag())){
			String transType = FLEFRecordHelper.getChildValue(initial, TAG_TYPE);
			if(transType != null){
				transTypeCombo.setSelectedItem(transType);
			}
		}
		transTypeCombo.setEnabled(TAG_TRANSCRIPTION.equals(typeCombo.getSelectedItem()));
		typeCombo.addActionListener(e -> transTypeCombo.setEnabled(TAG_TRANSCRIPTION.equals(typeCombo.getSelectedItem())));

		// Value field
		JTextField valueField = new JTextField(20);
		if(initial != null){
			String val = FLEFRecordHelper.getChildValue(initial, TAG_VALUE);
			valueField.setText(StringUtils.defaultString(val));
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

		final FLEFRecord[] result = {null};
		okBtn.addActionListener(e -> {
			String type = (String)typeCombo.getSelectedItem();
			String system = systemField.getText().trim();
			String value = valueField.getText().trim();

			if(system.isEmpty()){
				JOptionPane.showMessageDialog(dialog, "System cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if(value.isEmpty()){
				JOptionPane.showMessageDialog(dialog, "Value cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			FLEFRecord variantRecord = (initial != null)? initial: FLEFRecord.createEmpty();
			variantRecord.setTag(type);
			variantRecord.setValue(system);
			FLEFRecordHelper.updateChildValue(variantRecord, TAG_VALUE, value);

			if(TAG_TRANSCRIPTION.equals(type)){
				String transType = (String)transTypeCombo.getSelectedItem();
				if(StringUtils.isNotEmpty(transType)){
					FLEFRecordHelper.updateChildValue(variantRecord, TAG_TYPE, transType);
				}
				else{
					FLEFRecordHelper.removeChildren(variantRecord, TAG_TYPE);
				}
			}
			else{
				// For PHONETIC, remove any TYPE child
				FLEFRecordHelper.removeChildren(variantRecord, TAG_TYPE);
			}

			result[0] = variantRecord;
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);

		return result[0];
	}

	// ------------------------------------------------------------------------
	// Load / Save methods (to integrate with a NAME structure)
	// ------------------------------------------------------------------------

	/**
	 * Loads all PART children from the given NAME structure into this panel.
	 *
	 * @param nameStruct the NAME record containing PART children
	 */
	public void load(final FLEFRecord nameStruct){
		List<FLEFRecord> parts = new ArrayList<>();
		for(FLEFRecord child : nameStruct.getChildren())
			if(TAG_PART.equals(child.getTag()))
				parts.add(child);
		setItems(parts);
	}

	/**
	 * Saves the current list of parts as children of the given NAME structure.
	 * All existing PART children are removed and replaced.
	 *
	 * @param nameStruct the NAME record to save into
	 */
	public void save(final FLEFRecord nameStruct){
		// Remove existing PART children
		FLEFRecordHelper.removeChildren(nameStruct, TAG_PART);

		// Add each part as a child
		for(FLEFRecord part : getItems()){
			part.setTag(TAG_PART);
			nameStruct.addChild(part);
		}
	}

	/**
	 * Returns whether there is at least one part.
	 *
	 * @return {@code true} if the list is not empty
	 */
	public boolean hasData(){
		return !items.isEmpty();
	}

	/**
	 * Validates that each part has a non-empty VALUE.
	 *
	 * @return {@code true} if all parts are valid
	 */
	public boolean validateData(){
		for(FLEFRecord part : items){
			String value = FLEFRecordHelper.getChildValue(part, TAG_VALUE);
			if(StringUtils.isEmpty(value)){
				JOptionPane.showMessageDialog(parentDialog,
					"Part has no value.",
					"Validation Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		return true;
	}

}
