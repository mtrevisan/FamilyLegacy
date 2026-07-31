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
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.SourceCitationDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Panel for editing a list of {@code PERSONAL_NAME_STRUCTURE} according to FLEF 0.1.0.
 * <p>
 * Structure:
 * <pre>
 * PERSONAL_NAME_STRUCTURE :=
 * n NAME    {1:1}
 *   +1 TYPE <NAME_TYPE>    {0:1}
 *   +1 PART    {1:M}
 *     +2 TYPE <NAME_PART_TYPE>    {1:1}
 *     +2 VALUE <TEXT>    {1:1}
 *     +2 <<TEXT_VALUE_VARIANT>>    {0:M}
 *   +1 CULTURAL_NORM @<XREF:CULTURAL_NORM>@    {0:M}
 *   +1 NOTE @<XREF:NOTE>@    {0:M}
 *   +1 <<SOURCE_CITATION>>    {0:M}
 * </pre>
 */
public class PersonalNamePanel extends JPanel{

	static{
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new CulturalNormHandler());
	}

	private final FLEFModel model;
	private final Dialog parentDialog;

	private final NoteHandler noteHandler = new NoteHandler();
	private final SourceHandler sourceHandler = new SourceHandler();
	private final CulturalNormHandler culturalNormHandler = new CulturalNormHandler();

	private final DefaultListModel<String> nameListModel = new DefaultListModel<>();
	private final JList<String> nameList = new JList<>(nameListModel);
	private final List<PersonalNameEntry> nameEntries = new ArrayList<>();

	/**
	 * Represents a TEXT_VALUE_VARIANT for a name part.
	 */
	private static class VariantEntry{
		private final String type;       // "PHONETIC" or "TRANSCRIPTION"
		private final String system;
		private final String transcriptionType;
		private final String value;

		VariantEntry(String type, String system, String transcriptionType, String value){
			this.type = type;
			this.system = system;
			this.transcriptionType = transcriptionType;
			this.value = value;
		}

		@Override
		public String toString(){
			StringBuilder sb = new StringBuilder();
			if("PHONETIC".equals(type)){
				sb.append("Phonetic [").append(system).append("]: ").append(value);
			}
			else{
				sb.append("Transcription [").append(system).append("]");
				if(transcriptionType != null && !transcriptionType.isEmpty()){
					sb.append(" (").append(transcriptionType).append(")");
				}
				sb.append(": ").append(value);
			}
			return sb.toString();
		}
	}

	/**
	 * Represents a PART of a personal name.
	 */
	private static class PartEntry{
		private final String type;
		private final String value;
		private final List<VariantEntry> variants;

		PartEntry(String type, String value, List<VariantEntry> variants){
			this.type = StringUtils.defaultString(type);
			this.value = StringUtils.defaultString(value);
			this.variants = variants != null? variants: new ArrayList<>();
		}

		@Override
		public String toString(){
			StringBuilder sb = new StringBuilder();
			if(!type.isEmpty()){
				sb.append("[").append(type).append("] ");
			}
			sb.append(value);
			if(!variants.isEmpty()){
				sb.append(" (").append(variants.size()).append(" variants)");
			}
			return sb.toString();
		}
	}

	/**
	 * Internal representation of a PERSONAL_NAME_STRUCTURE.
	 */
	private static class PersonalNameEntry{
		private final String type;
		private final List<PartEntry> parts;
		private final List<String> culturalNormIds;
		private final List<String> noteIds;
		private final List<FLEFRecord> sourceCitations;

		PersonalNameEntry(String type, List<PartEntry> parts,
			List<String> culturalNormIds, List<String> noteIds,
			List<FLEFRecord> sourceCitations){
			this.type = StringUtils.defaultString(type);
			this.parts = parts != null? parts: new ArrayList<>();
			this.culturalNormIds = culturalNormIds != null? culturalNormIds: new ArrayList<>();
			this.noteIds = noteIds != null? noteIds: new ArrayList<>();
			this.sourceCitations = sourceCitations != null? sourceCitations: new ArrayList<>();
		}

		public String getFullName(){
			StringBuilder sb = new StringBuilder();
			for(PartEntry part : parts){
				if(!sb.isEmpty()) sb.append(StringUtils.SPACE);
				sb.append(part.value);
			}
			return sb.toString();
		}

		@Override
		public String toString(){
			StringBuilder sb = new StringBuilder(getFullName());
			if(!type.isEmpty()){
				sb.append(" [").append(type).append("]");
			}
//			if(!parts.isEmpty()){
//				sb.append(" (").append(parts.size()).append(" parts)");
//			}
			if(!culturalNormIds.isEmpty()){
				sb.append(" (").append(culturalNormIds.size()).append(" norms)");
			}
			if(!noteIds.isEmpty()){
				sb.append(" (").append(noteIds.size()).append(" notes)");
			}
			if(!sourceCitations.isEmpty()){
				sb.append(" (").append(sourceCitations.size()).append(" sources)");
			}
			return sb.toString();
		}
	}


	public PersonalNamePanel(FLEFModel model, Dialog parent){
		this.model = model;
		this.parentDialog = parent;
		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout("fillx"));
		setBorder(new TitledBorder("Names"));

		nameList.setVisibleRowCount(4);
		nameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		GUIHelper.installBehavior(nameList,
			() -> nameList.getSelectedIndex() >= 0,
			this::editName,
			this::createNewName,
			this::deleteName,
			builder -> {
				builder.item("Create New...", this::createNewName);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editName);
				builder.selectionSensitiveItem("Delete", this::deleteName);
			});

		JScrollPane scrollPane = GUIHelper.createScrollPane(nameList);
		add(scrollPane, "growx,wrap");
	}


	public void loadFromRecord(FLEFRecord record){
		nameEntries.clear();
		nameListModel.clear();

		List<FLEFRecord> nameStructs = record.findChildren("NAME");
		for(FLEFRecord nameStruct : nameStructs){
			String type = FLEFRecordHelper.getChildValue(nameStruct, "TYPE");

			List<PartEntry> parts = new ArrayList<>();
			for(FLEFRecord child : nameStruct.getChildren()){
				if("PART".equals(child.getTag())){
					String partType = FLEFRecordHelper.getChildValue(child, "TYPE");
					String value = FLEFRecordHelper.getChildValue(child, "VALUE");
					if(value == null || value.isEmpty()) continue;

					List<VariantEntry> variants = new ArrayList<>();
					for(FLEFRecord variantChild : child.getChildren()){
						String tag = variantChild.getTag();
						if("PHONETIC".equals(tag)){
							String system = variantChild.getValue();
							String variantValue = FLEFRecordHelper.getChildValue(variantChild, "VALUE");
							if(system != null && !system.isEmpty() && variantValue != null && !variantValue.isEmpty()){
								variants.add(new VariantEntry("PHONETIC", system, null, variantValue));
							}
						}
						else if("TRANSCRIPTION".equals(tag)){
							String system = variantChild.getValue();
							String transType = FLEFRecordHelper.getChildValue(variantChild, "TYPE");
							String variantValue = FLEFRecordHelper.getChildValue(variantChild, "VALUE");
							if(system != null && !system.isEmpty() && variantValue != null && !variantValue.isEmpty()){
								variants.add(new VariantEntry("TRANSCRIPTION", system, transType, variantValue));
							}
						}
					}
					parts.add(new PartEntry(partType, value, variants));
				}
			}

			List<String> culturalNormIds = new ArrayList<>();
			for(FLEFRecord child : nameStruct.getChildren()){
				if("CULTURAL_NORM".equals(child.getTag()) && child.getValue() != null){
					culturalNormIds.add(child.getValue());
				}
			}

			List<String> noteIds = new ArrayList<>();
			for(FLEFRecord child : nameStruct.getChildren()){
				if("NOTE".equals(child.getTag()) && child.getValue() != null){
					noteIds.add(child.getValue());
				}
			}

			List<FLEFRecord> sourceCitations = new ArrayList<>();
			for(FLEFRecord child : nameStruct.getChildren()){
				if("SOURCE".equals(child.getTag())){
					sourceCitations.add(child);
				}
			}

			nameEntries.add(new PersonalNameEntry(type, parts, culturalNormIds, noteIds, sourceCitations));
			nameListModel.addElement(nameEntries.getLast().toString());
		}
	}


	public void saveToRecord(FLEFRecord record){
		List<FLEFRecord> toRemove = record.findChildren("NAME");
		for(FLEFRecord child : toRemove){
			record.removeChild(child);
		}

		for(PersonalNameEntry entry : nameEntries){
			FLEFRecord nameStruct = FLEFRecord.createChild("NAME");

			if(entry.type != null && !entry.type.isEmpty()){
				FLEFRecord type = FLEFRecord.createChildWithValue("TYPE", entry.type);
				nameStruct.addChild(type);
			}

			for(PartEntry part : entry.parts){
				FLEFRecord partStruct = FLEFRecord.createChild("PART");

				if(part.type != null && !part.type.isEmpty()){
					FLEFRecord partType = FLEFRecord.createChildWithValue("TYPE", part.type);
					partStruct.addChild(partType);
				}

				FLEFRecord partValue = FLEFRecord.createChildWithValue("VALUE", part.value);
				partStruct.addChild(partValue);

				for(VariantEntry variant : part.variants){
					FLEFRecord variantStruct = FLEFRecord.createChildWithValue(
						variant.type, variant.system);
					if("TRANSCRIPTION".equals(variant.type) && variant.transcriptionType != null && !variant.transcriptionType.isEmpty()){
						FLEFRecord transType = FLEFRecord.createChildWithValue(
							"TYPE", variant.transcriptionType);
						variantStruct.addChild(transType);
					}
					FLEFRecord variantValue = FLEFRecord.createChildWithValue(
						"VALUE", variant.value);
					variantStruct.addChild(variantValue);
					partStruct.addChild(variantStruct);
				}

				nameStruct.addChild(partStruct);
			}

			for(String id : entry.culturalNormIds){
				if(id != null && !id.isEmpty()){
					FLEFRecord norm = FLEFRecord.createChildWithValue("CULTURAL_NORM", XRefHelper.formatXRef(id));
					nameStruct.addChild(norm);
				}
			}

			for(String id : entry.noteIds){
				if(id != null && !id.isEmpty()){
					FLEFRecord note = FLEFRecord.createChildWithValue("NOTE", XRefHelper.formatXRef(id));
					nameStruct.addChild(note);
				}
			}

			for(FLEFRecord citation : entry.sourceCitations){
				citation.setTag("SOURCE");
				nameStruct.addChild(citation);
			}

			record.addChild(nameStruct);
		}
	}


	private void createNewName(){
		PersonalNameEntry newEntry = showNameDialog(null);
		if(newEntry != null){
			nameEntries.add(newEntry);
			nameListModel.addElement(newEntry.toString());
		}
	}

	private void editName(){
		int idx = nameList.getSelectedIndex();
		if(idx == -1) return;
		PersonalNameEntry current = nameEntries.get(idx);
		PersonalNameEntry updated = showNameDialog(current);
		if(updated != null){
			nameEntries.set(idx, updated);
			nameListModel.set(idx, updated.toString());
		}
	}

	private void deleteName(){
		int idx = nameList.getSelectedIndex();
		if(idx == -1) return;
		if(JOptionPane.showConfirmDialog(parentDialog, "Remove this name?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
			nameEntries.remove(idx);
			nameListModel.remove(idx);
		}
	}


	private PersonalNameEntry showNameDialog(PersonalNameEntry initial){
		JDialog dialog = new JDialog(parentDialog, initial == null? "Add Personal Name": "Edit Personal Name", true);
		dialog.setLayout(new MigLayout("ins 10, fillx", "[right]rel[grow]", "[]5[]5[]"));

		// NAME TYPE
		JComboBox<String> typeCombo = new JComboBox<>(new String[]{
			StringUtils.EMPTY, "official", "religious", "birth",
			"married", "maiden", "divorce", "adoption", "fostering",
			"legal", "immigrant", "adapted",
			"aka", "nickname", "artistic", "professional", "user",
			"regnal", "slavename"
		});
		if(initial != null && !initial.type.isEmpty()){
			typeCombo.setSelectedItem(initial.type);
		}
		dialog.add(new JLabel("Name Type:"), "align label");
		dialog.add(typeCombo, "growx,wrap");

		// PARTS
		JPanel partsPanel = new JPanel(new MigLayout("ins 0, fillx", "[grow]"));
		partsPanel.setBorder(new TitledBorder("Parts"));
		DefaultListModel<PartEntry> partModel = new DefaultListModel<>();
		JList<PartEntry> partList = new JList<>(partModel);
		List<PartEntry> currentParts = new ArrayList<>(initial != null? initial.parts: new ArrayList<>());
		for(PartEntry p : currentParts){
			partModel.addElement(p);
		}

		JScrollPane partScroll = GUIHelper.createScrollPane(partList);

		GUIHelper.installBehavior(partList,
			() -> partList.getSelectedIndex() >= 0,
			() -> {
				int idx = partList.getSelectedIndex();
				if(idx != -1){
					PartEntry current = currentParts.get(idx);
					PartEntry updated = showPartDialog(dialog, current);
					if(updated != null){
						currentParts.set(idx, updated);
						partModel.set(idx, updated);
					}
				}
			},
			() -> {
				PartEntry newPart = showPartDialog(dialog, null);
				if(newPart != null){
					currentParts.add(newPart);
					partModel.addElement(newPart);
				}
			},
			() -> {
				int idx = partList.getSelectedIndex();
				if(idx != -1){
					if(JOptionPane.showConfirmDialog(dialog, "Remove this part?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
						currentParts.remove(idx);
						partModel.remove(idx);
					}
				}
			},
			builder -> {
				builder.item("Add Part...", () -> {
					PartEntry newPart = showPartDialog(dialog, null);
					if(newPart != null){
						currentParts.add(newPart);
						partModel.addElement(newPart);
					}
				});
				builder.separator();
				builder.selectionSensitiveItem("Edit Part...", () -> {
					int idx = partList.getSelectedIndex();
					if(idx != -1){
						PartEntry current = currentParts.get(idx);
						PartEntry updated = showPartDialog(dialog, current);
						if(updated != null){
							currentParts.set(idx, updated);
							partModel.set(idx, updated);
						}
					}
				});
				builder.selectionSensitiveItem("Remove Part", () -> {
					int idx = partList.getSelectedIndex();
					if(idx != -1){
						if(JOptionPane.showConfirmDialog(dialog, "Remove this part?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
							currentParts.remove(idx);
							partModel.remove(idx);
						}
					}
				});
			});

		partsPanel.add(partScroll, "growx,wrap");
		dialog.add(partsPanel, "span 2,growx,wrap");

		// CULTURAL NORMS (popup)
		JPanel normPanel = new JPanel(new MigLayout("ins 0, fillx", "[grow]"));
		normPanel.setBorder(new TitledBorder("Cultural Norms"));
		DefaultListModel<String> normModel = new DefaultListModel<>();
		JList<String> normList = new JList<>(normModel);
		List<String> currentNormIds = new ArrayList<>(initial != null? initial.culturalNormIds: new ArrayList<>());
		for(String id : currentNormIds){
			normModel.addElement(getCulturalNormDisplayName(id));
		}

		JScrollPane normScroll = GUIHelper.createScrollPane(normList);

		GUIHelper.installBehavior(normList,
			() -> normList.getSelectedIndex() >= 0,
			null,
			() -> addCulturalNorm(dialog, currentNormIds, normModel),
			() -> removeCulturalNorm(dialog, normList, currentNormIds, normModel),
			builder -> {
				builder.item("Add Cultural Norm...", () -> addCulturalNorm(dialog, currentNormIds, normModel));
				builder.separator();
				builder.selectionSensitiveItem("Remove", () -> removeCulturalNorm(dialog, normList, currentNormIds, normModel));
			});

		normPanel.add(normScroll, "growx,wrap");
		dialog.add(normPanel, "span 2,growx,wrap");

		// NOTES (popup)
		JPanel notesPanel = new JPanel(new MigLayout("ins 0, fillx", "[grow]"));
		notesPanel.setBorder(new TitledBorder("Notes"));
		DefaultListModel<String> noteModel = new DefaultListModel<>();
		JList<String> noteList = new JList<>(noteModel);
		List<String> currentNoteIds = new ArrayList<>(initial != null? initial.noteIds: new ArrayList<>());
		for(String id : currentNoteIds){
			noteModel.addElement(getNoteDisplayName(id));
		}

		JScrollPane noteScroll = GUIHelper.createScrollPane(noteList);

		GUIHelper.installBehavior(noteList,
			() -> noteList.getSelectedIndex() >= 0,
			() -> editNoteInList(dialog, noteList, currentNoteIds, noteModel),
			() -> addNoteInList(dialog, currentNoteIds, noteModel),
			() -> removeNoteFromList(dialog, noteList, currentNoteIds, noteModel),
			builder -> {
				builder.item("Create New...", () -> createNewNoteInList(dialog, currentNoteIds, noteModel));
				builder.item("Add Existing...", () -> addNoteInList(dialog, currentNoteIds, noteModel));
				builder.separator();
				builder.selectionSensitiveItem("Edit...", () -> editNoteInList(dialog, noteList, currentNoteIds, noteModel));
				builder.selectionSensitiveItem("Remove", () -> removeNoteFromList(dialog, noteList, currentNoteIds, noteModel));
			});

		notesPanel.add(noteScroll, "growx,wrap");
		dialog.add(notesPanel, "span 2,growx,wrap");

		// SOURCE CITATIONS (popup)
		JPanel sourcePanel = new JPanel(new MigLayout("ins 0, fillx", "[grow]"));
		sourcePanel.setBorder(new TitledBorder("Source Citations"));
		DefaultListModel<String> sourceModel = new DefaultListModel<>();
		JList<String> sourceList = new JList<>(sourceModel);
		List<FLEFRecord> currentSources = new ArrayList<>(initial != null? initial.sourceCitations: new ArrayList<>());
		for(FLEFRecord citation : currentSources){
			sourceModel.addElement(getSourceCitationDisplay(citation));
		}

		JScrollPane sourceScroll = GUIHelper.createScrollPane(sourceList);

		GUIHelper.installBehavior(sourceList,
			() -> sourceList.getSelectedIndex() >= 0,
			() -> editSourceInList(dialog, sourceList, currentSources, sourceModel),
			() -> addSourceInList(dialog, currentSources, sourceModel),
			() -> removeSourceFromList(dialog, sourceList, currentSources, sourceModel),
			builder -> {
				builder.item("Add Existing...", () -> addSourceInList(dialog, currentSources, sourceModel));
				builder.separator();
				builder.selectionSensitiveItem("Edit...", () -> editSourceInList(dialog, sourceList, currentSources, sourceModel));
				builder.selectionSensitiveItem("Remove", () -> removeSourceFromList(dialog, sourceList, currentSources, sourceModel));
			});

		sourcePanel.add(sourceScroll, "growx,wrap");
		dialog.add(sourcePanel, "span 2,growx,wrap");

		// OK / Cancel
		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		dialog.add(btnPanel, "span 2,growx");

		final PersonalNameEntry[] result = {null};
		okBtn.addActionListener(e -> {
			String type = (String)typeCombo.getSelectedItem();
			if(currentParts.isEmpty()){
				JOptionPane.showMessageDialog(dialog,
					"At least one name part is required.",
					"Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			result[0] = new PersonalNameEntry(
				type != null && !type.isEmpty()? type: null,
				currentParts,
				currentNormIds,
				currentNoteIds,
				currentSources);
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(parentDialog);
		dialog.setVisible(true);
		return result[0];
	}


	/**
	 * Shows a dialog to create or edit a name part.
	 * Variants are managed as a list with popup behavior.
	 */
	private PartEntry showPartDialog(Dialog parent, PartEntry initial){
		JDialog dialog = new JDialog(parent, initial == null? "Add Part": "Edit Part", true);
		dialog.setLayout(new MigLayout("ins 10, fillx", "[right]rel[grow]", "[]5[]5[]"));

		// PART TYPE
		JComboBox<String> typeCombo = new JComboBox<>(new String[]{
			StringUtils.EMPTY,
			// Personal and birth names:
			"given",					// Primary personal name / baptismal name / Ism.
			"generation",			// Generation name (e.g., Zì 辈 in East Asian genealogies).
			// Direct family relationships (Descent)
			"patronymic",			// Name derived from father's given name (Ivanov, bin Hasan, Eriksson).
			"matronymic",			// Name derived from mother's given name.
			"kunya",					// Arabic teknonym ("Father of..." / "Mother of...", e.g., Abu Bakr).
			// Extended family and social belonging
			"family",				// Generic surname / family name.
			"family nickname",	// Branch nickname / agnatic alias (e.g., historical local house aliases).
			"lineage",				// Line of descent / dynastic branch.
			"house",					// Ancestral house / estate / German Hofname / Japanese Uji.
			"clan",					// Clan / Gens / Sippe.
			"tribal",				// Tribal affiliation / Indigenous nation.
			"caste",					// Caste / Jāti (for South Asian contexts).
			// Geographical and territorial origin
			"toponymic",			// Place of origin, lordship, or estate (da, von, de, van).
			// Titles, roles and professions
			"title",					// Noble, academic, or professional title (Lord, Sir, Count).
			"occupational",		// Historical trade or profession before hereditary surnames (Faber, Baker).
			"prefix",				// Name prefix (Dr., Don, Prof.).
			"suffix",				// Generational or honorific suffix (Jr., III, Ph.D.).
			// Assumed names, nicknames and contextual
			"nickname",				// Personal epithet / Agnomen / Laqab.
			"regnal",				// Regnal or papal name assumed upon accession.
			"religious",			// Monastic, clerical, or initiation name (Brother Aloysius).
			"posthumous"			// Posthumous or temple name (Shihao 諡號 / Miaohao 廟號 in East Asia).
		});
		if(initial != null && !initial.type.isEmpty()){
			typeCombo.setSelectedItem(initial.type);
		}
		dialog.add(new JLabel("Part Type:"), "align label");
		dialog.add(typeCombo, "growx,wrap");

		// PART VALUE
		JTextField valueField = new JTextField(25);
		if(initial != null){
			valueField.setText(initial.value);
		}
		dialog.add(new JLabel("Value:"), "align label");
		dialog.add(valueField, "growx,wrap");

		// VARIANTS (list with popup)
		JPanel variantsPanel = new JPanel(new MigLayout("ins 0, fillx", "[grow]"));
		variantsPanel.setBorder(new TitledBorder("Variants"));
		DefaultListModel<VariantEntry> variantModel = new DefaultListModel<>();
		JList<VariantEntry> variantList = new JList<>(variantModel);
		List<VariantEntry> currentVariants = new ArrayList<>(initial != null? initial.variants: new ArrayList<>());
		for(VariantEntry v : currentVariants){
			variantModel.addElement(v);
		}

		JScrollPane variantScroll = GUIHelper.createScrollPane(variantList);

		GUIHelper.installBehavior(variantList,
			() -> variantList.getSelectedIndex() >= 0,
			() -> { // double-click → edit
				int idx = variantList.getSelectedIndex();
				if(idx != -1){
					VariantEntry current = currentVariants.get(idx);
					VariantEntry updated = showVariantDialog(dialog, current);
					if(updated != null){
						currentVariants.set(idx, updated);
						variantModel.set(idx, updated);
					}
				}
			},
			() -> { // INSERT → add
				VariantEntry newVariant = showVariantDialog(dialog, null);
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
					VariantEntry newVariant = showVariantDialog(dialog, null);
					if(newVariant != null){
						currentVariants.add(newVariant);
						variantModel.addElement(newVariant);
					}
				});
				builder.separator();
				builder.selectionSensitiveItem("Edit Variant...", () -> {
					int idx = variantList.getSelectedIndex();
					if(idx != -1){
						VariantEntry current = currentVariants.get(idx);
						VariantEntry updated = showVariantDialog(dialog, current);
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

		final PartEntry[] result = {null};
		okBtn.addActionListener(e -> {
			String type = (String)typeCombo.getSelectedItem();
			String value = valueField.getText().trim();

			if(type == null || type.isEmpty()){
				JOptionPane.showMessageDialog(dialog, "Part Type cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if(value.isEmpty()){
				JOptionPane.showMessageDialog(dialog, "Value cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			result[0] = new PartEntry(type, value, currentVariants);
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
		return result[0];
	}

	/**
	 * Shows a dialog to create or edit a TEXT_VALUE_VARIANT.
	 */
	private VariantEntry showVariantDialog(Dialog parent, VariantEntry initial){
		JDialog dialog = new JDialog(parent, initial == null? "Add Variant": "Edit Variant", true);
		dialog.setLayout(new MigLayout("ins 10, fillx", "[right]rel[grow]", "[]5[]5[]"));

		JComboBox<String> typeCombo = new JComboBox<>(new String[]{"PHONETIC", "TRANSCRIPTION"});
		if(initial != null){
			typeCombo.setSelectedItem(initial.type);
		}

		JTextField systemField = new JTextField(20);
		if(initial != null){
			systemField.setText(initial.system);
		}
		systemField.setToolTipText("e.g., 'ipa', 'romaji', 'pinyin', 'wadegiles'");

		JComboBox<String> transTypeCombo = new JComboBox<>(new String[]{StringUtils.EMPTY, "romanized", "anglicized", "cyrillized", "francized", "gairaigized", "latinized"});
		if(initial != null && "TRANSCRIPTION".equals(initial.type) && initial.transcriptionType != null){
			transTypeCombo.setSelectedItem(initial.transcriptionType);
		}
		transTypeCombo.setEnabled("TRANSCRIPTION".equals(typeCombo.getSelectedItem()));
		typeCombo.addActionListener(e -> transTypeCombo.setEnabled("TRANSCRIPTION".equals(typeCombo.getSelectedItem())));

		JTextField valueField = new JTextField(20);
		if(initial != null){
			valueField.setText(initial.value);
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

			if(system.isEmpty()){
				JOptionPane.showMessageDialog(dialog, "System cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if(value.isEmpty()){
				JOptionPane.showMessageDialog(dialog, "Value cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
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
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
		return result[0];
	}


	private void addCulturalNorm(Dialog parent, List<String> currentNormIds, DefaultListModel<String> listModel){
		GenericSelectionDialog<?> selDialog = new GenericSelectionDialog<>(
			parent, model, culturalNormHandler, selectedId -> {
			if(selectedId != null && !currentNormIds.contains(selectedId)){
				currentNormIds.add(selectedId);
				listModel.addElement(getCulturalNormDisplayName(selectedId));
			}
		});
		selDialog.setVisible(true);
	}

	private void removeCulturalNorm(Dialog parent, JList<String> list, List<String> currentNormIds, DefaultListModel<String> listModel){
		int idx = list.getSelectedIndex();
		if(idx != -1){
			if(JOptionPane.showConfirmDialog(parent, "Remove this cultural norm?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
				currentNormIds.remove(idx);
				listModel.remove(idx);
			}
		}
	}


	private void addNoteInList(Dialog parent, List<String> currentNoteIds, DefaultListModel<String> listModel){
		GenericSelectionDialog<?> selDialog = new GenericSelectionDialog<>(
			parent, model, noteHandler, selectedId -> {
			if(selectedId != null && !currentNoteIds.contains(selectedId)){
				currentNoteIds.add(selectedId);
				listModel.addElement(getNoteDisplayName(selectedId));
			}
		});
		selDialog.setVisible(true);
	}

	private void createNewNoteInList(Dialog parent, List<String> currentNoteIds, DefaultListModel<String> listModel){
		Set<String> before = new HashSet<>(currentNoteIds);
		JDialog newNoteDialog = noteHandler.createNewDialog(parent, model);
		newNoteDialog.setVisible(true);
		for(FLEFRecord rec : model.getRecordsByType("NOTE")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !currentNoteIds.contains(id)){
				currentNoteIds.add(id);
				listModel.addElement(getNoteDisplayName(id));
				break;
			}
		}
	}

	private void editNoteInList(Dialog parent, JList<String> list, List<String> currentNoteIds, DefaultListModel<String> listModel){
		int idx = list.getSelectedIndex();
		if(idx == -1) return;
		String id = currentNoteIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null) return;
		JDialog editDialog = noteHandler.createEditDialog(parent, model, rec);
		editDialog.setVisible(true);
		String newDisplay = getNoteDisplayName(id);
		listModel.set(idx, newDisplay);
	}

	private void removeNoteFromList(Dialog parent, JList<String> list, List<String> currentNoteIds, DefaultListModel<String> listModel){
		int idx = list.getSelectedIndex();
		if(idx != -1){
			if(JOptionPane.showConfirmDialog(parent, "Remove this note?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
				currentNoteIds.remove(idx);
				listModel.remove(idx);
			}
		}
	}


	private void addSourceInList(Dialog parent, List<FLEFRecord> currentSources, DefaultListModel<String> listModel){
		GenericSelectionDialog<?> selDialog = new GenericSelectionDialog<>(
			parent, model, sourceHandler, selectedId -> {
			if(selectedId != null){
				FLEFRecord citation = FLEFRecord.createChildWithValue("SOURCE", selectedId);
				currentSources.add(citation);
				listModel.addElement(getSourceCitationDisplay(citation));
			}
		});
		selDialog.setVisible(true);
	}

	private void editSourceInList(Dialog parent, JList<String> list, List<FLEFRecord> currentSources, DefaultListModel<String> listModel){
		int idx = list.getSelectedIndex();
		if(idx == -1) return;
		FLEFRecord existing = currentSources.get(idx);
		SourceCitationDialog dialog = new SourceCitationDialog(parent, model, existing);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord updated = dialog.getCitationRecord();
			if(updated != null){
				currentSources.set(idx, updated);
				listModel.set(idx, getSourceCitationDisplay(updated));
			}
		}
	}

	private void removeSourceFromList(Dialog parent, JList<String> list, List<FLEFRecord> currentSources, DefaultListModel<String> model){
		int idx = list.getSelectedIndex();
		if(idx != -1){
			if(JOptionPane.showConfirmDialog(parent, "Remove this source citation?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
				currentSources.remove(idx);
				model.remove(idx);
			}
		}
	}


	private String getCulturalNormDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null) return culturalNormHandler.getDisplayText(rec);
		return id;
	}

	private String getNoteDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null) return noteHandler.getDisplayText(rec);
		return id;
	}

	private String getSourceCitationDisplay(FLEFRecord citation){
		String sourceId = citation.getValue();
		if(sourceId != null){
			FLEFRecord rec = model.getRecordById(sourceId);
			if(rec != null){
				return sourceHandler.getDisplayText(rec);
			}
			return sourceId;
		}
		return "[empty]";
	}

	public boolean hasData(){
		return !nameEntries.isEmpty();
	}

	public boolean validateData(){
		if(nameEntries.isEmpty()){
			JOptionPane.showMessageDialog(parentDialog,
				"At least one name is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		for(PersonalNameEntry entry : nameEntries){
			if(entry.parts.isEmpty()){
				JOptionPane.showMessageDialog(parentDialog,
					"Name '" + entry.getFullName() + "' has no parts.",
					"Validation Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		return true;
	}

}
