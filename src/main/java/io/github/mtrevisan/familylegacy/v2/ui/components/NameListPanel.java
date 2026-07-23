package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.SourceCitationDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ScrollableContainerHost;
import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Panel for editing a list of {@code NAME_STRUCTURE} according to FLEF 0.1.0.
 * <p>
 * The actual record structure uses the following tags:
 * <pre>
 * n NAME    {1:1}
 *   +1 VALUE <TEXT>    {1:1}
 *   +1 TYPE <NAME_TYPE>    {0:1}
 *   +1 LOCALE <LOCALE_CODE>    {0:1}
 *   +1 VALID_FROM    {0:1}
 *     +2 <<DATE_STRUCTURE>>    {1:1}
 *   +1 VALID_TO    {0:1}
 *     +2 <<DATE_STRUCTURE>>    {1:1}
 *   +1 <<TEXT_VALUE_VARIANT>>    {0:M}
 *   +1 NOTE @<XREF:NOTE>@    {0:M}
 *   +1 <<SOURCE_CITATION>>    {0:M}
 * </pre>
 * Note: the TEXT_VALUE_VARIANT nodes are represented as PHONETIC or TRANSCRIPTION.
 */
public class NameListPanel extends JPanel{

	private final FLEFModel model;
	private final Dialog parentDialog;

	private final NoteHandler noteHandler = new NoteHandler();
	private final SourceHandler sourceHandler = new SourceHandler();

	private final DefaultListModel<String> nameListModel = new DefaultListModel<>();
	private final JList<String> nameList = new JList<>(nameListModel);
	private final List<NameEntry> nameEntries = new ArrayList<>();

	/**
	 * Represents a TEXT_VALUE_VARIANT (PHONETIC or TRANSCRIPTION).
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
	 * Internal representation of a NAME with full TEXT_VALUE support.
	 * <p>
	 * This corresponds to:
	 * NAME
	 * +1 VALUE (the text value)
	 * +1 TYPE (optional)
	 * +1 LOCALE (optional)
	 * +1 VALID_FROM (optional DATE_STRUCTURE)
	 * +1 VALID_TO (optional DATE_STRUCTURE)
	 * +1 PHONETIC / TRANSCRIPTION (variants)
	 * +1 NOTE
	 * +1 SOURCE_CITATION
	 */
	private static class NameEntry{
		private final String value;
		private final String type;
		private final String locale;
		private final FLEFRecord validFrom;   // DATE_STRUCTURE record
		private final FLEFRecord validTo;     // DATE_STRUCTURE record
		private final List<VariantEntry> variants;
		private final List<String> noteIds;
		private final List<FLEFRecord> sourceCitations;

		NameEntry(String value, String type, String locale,
			FLEFRecord validFrom, FLEFRecord validTo,
			List<VariantEntry> variants, List<String> noteIds,
			List<FLEFRecord> sourceCitations){
			this.value = value;
			this.type = type != null? type: "";
			this.locale = locale != null? locale: "";
			this.validFrom = validFrom;
			this.validTo = validTo;
			this.variants = variants != null? variants: new ArrayList<>();
			this.noteIds = noteIds != null? noteIds: new ArrayList<>();
			this.sourceCitations = sourceCitations != null? sourceCitations: new ArrayList<>();
		}

		@Override
		public String toString(){
			StringBuilder sb = new StringBuilder(value);
			if(!type.isEmpty()){
				sb.append(" [").append(type).append("]");
			}
			if(!locale.isEmpty()){
				sb.append(" (").append(locale).append(")");
			}
			if(validFrom != null || validTo != null){
				sb.append(" [");
				if(validFrom != null){
					sb.append("from ").append(extractDateValue(validFrom));
				}
				if(validTo != null){
					if(validFrom != null) sb.append(" ");
					sb.append("to ").append(extractDateValue(validTo));
				}
				sb.append("]");
			}
			if(!variants.isEmpty()){
				sb.append(" (").append(variants.size()).append(" variants)");
			}
			if(!noteIds.isEmpty()){
				sb.append(" (").append(noteIds.size()).append(" notes)");
			}
			if(!sourceCitations.isEmpty()){
				sb.append(" (").append(sourceCitations.size()).append(" sources)");
			}
			return sb.toString();
		}

		/**
		 * Extracts a human-readable date string from a DATE_STRUCTURE record.
		 */
		private static String extractDateValue(final FLEFRecord record){
			if(record == null){
				return "";
			}

			// The record is a DATE_STRUCTURE, which contains a DATE node.
			FLEFRecord dateNode = FLEFRecordUtils.findChild(record, "DATE");
			if(dateNode == null){
				return "";
			}

			// DATE has a DATE_VALUE child.
			FLEFRecord dateValue = FLEFRecordUtils.findChild(dateNode, "DATE_VALUE");
			if(dateValue == null){
				return "";
			}

			return extractDateValueNode(dateValue);
		}

		private static String extractDateValueNode(final FLEFRecord dateValue){
			// Try VALUE -> QUALIFIED_DATE -> SINGLE_DATE -> ISO
			FLEFRecord valueNode = FLEFRecordUtils.findChild(dateValue, "VALUE");
			if(valueNode != null){
				FLEFRecord qualified = FLEFRecordUtils.findChild(valueNode, "QUALIFIED_DATE");
				return formatQualifiedDate(qualified);
			}

			// Try BOUNDED
			FLEFRecord bounded = FLEFRecordUtils.findChild(dateValue, "BOUNDED");
			if(bounded != null){
				return formatBounded(bounded);
			}

			// Try SPANNING
			FLEFRecord spanning = FLEFRecordUtils.findChild(dateValue, "SPANNING");
			if(spanning != null){
				return formatSpanning(spanning);
			}

			return "";
		}

		private static String formatQualifiedDate(final FLEFRecord qualifiedDate){
			if(qualifiedDate == null){
				return "";
			}

			FLEFRecord singleDate = FLEFRecordUtils.findChild(qualifiedDate, "SINGLE_DATE");
			if(singleDate == null){
				return "";
			}

			String value = formatSingleDate(singleDate);
			FLEFRecord approximate = FLEFRecordUtils.findChild(qualifiedDate, "APPROXIMATE");
			if(approximate != null){
				value = "~" + value;
			}

			return value;
		}

		private static String formatSingleDate(final FLEFRecord singleDate){
			FLEFRecord iso = FLEFRecordUtils.findChild(singleDate, "ISO");
			if(iso != null){
				return iso.getValue();
			}

			FLEFRecord century = FLEFRecordUtils.findChild(singleDate, "CENTURY");
			if(century != null){
				return formatCentury(century);
			}

			FLEFRecord decade = FLEFRecordUtils.findChild(singleDate, "DECADE");
			if(decade != null){
				return formatDecade(decade);
			}

			return "";
		}

		private static String formatCentury(final FLEFRecord century){
			String ordinal = century.getValue();
			if(ordinal == null || ordinal.isBlank()){
				return "";
			}

			StringBuilder result = new StringBuilder(ordinal)
											  .append("th century");

			String part = FLEFRecordUtils.getChildValue(century, "PART");
			if(part != null && !part.isBlank()){
				result.append(" (")
					.append(part)
					.append(')');
			}

			return result.toString();
		}

		private static String formatDecade(final FLEFRecord decade){
			String value = decade.getValue();
			return (value != null
						  ? value + "s"
						  : "");
		}

		private static String formatBounded(final FLEFRecord bounded){
			String before = "";
			FLEFRecord notBefore = FLEFRecordUtils.findChild(bounded, "NOT_BEFORE");
			if(notBefore != null){
				before = formatQualifiedDate(FLEFRecordUtils.findChild(notBefore, "QUALIFIED_DATE"));
			}

			String after = "";
			FLEFRecord notAfter = FLEFRecordUtils.findChild(bounded, "NOT_AFTER");
			if(notAfter != null){
				after = formatQualifiedDate(FLEFRecordUtils.findChild(notAfter, "QUALIFIED_DATE"));
			}

			if(!before.isEmpty() && !after.isEmpty()){
				return "between " + before + " and " + after;
			}
			if(!before.isEmpty()){
				return "after " + before;
			}
			if(!after.isEmpty()){
				return "before " + after;
			}
			return "";
		}

		private static String formatSpanning(final FLEFRecord spanning){
			String fromValue = "";
			FLEFRecord from = FLEFRecordUtils.findChild(spanning, "FROM");
			if(from != null){
				fromValue = formatQualifiedDate(FLEFRecordUtils.findChild(from, "QUALIFIED_DATE"));
			}

			String toValue = "";
			FLEFRecord to = FLEFRecordUtils.findChild(spanning, "TO");
			if(to != null){
				toValue = formatQualifiedDate(FLEFRecordUtils.findChild(to, "QUALIFIED_DATE"));
			}

			if(!fromValue.isEmpty() && !toValue.isEmpty()){
				return "from " + fromValue + " to " + toValue;
			}
			if(!fromValue.isEmpty()){
				return "from " + fromValue;
			}
			if(!toValue.isEmpty()){
				return "until " + toValue;
			}
			return "";
		}

	}


	public NameListPanel(FLEFModel model, Dialog parent){
		this.model = model;
		this.parentDialog = parent;

		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout("fillx"));
		setBorder(new TitledBorder("Names"));

		nameList.setVisibleRowCount(3);
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

	// ==================== Data Loading ====================

	public void loadFromRecord(FLEFRecord record){
		nameEntries.clear();
		nameListModel.clear();

		// Find all direct children with tag "NAME"
		List<FLEFRecord> nameNodes = record.findChildren("NAME");
		for(FLEFRecord nameNode : nameNodes){
			// The name value is stored in a child with tag "VALUE"
			String value = FLEFRecordUtils.getChildValue(nameNode, "VALUE");
			if(value == null || value.isEmpty()){
				continue;
			}

			// Optional fields
			String type = FLEFRecordUtils.getChildValue(nameNode, "TYPE");
			String locale = FLEFRecordUtils.getChildValue(nameNode, "LOCALE");

			// VALID_FROM (DATE_STRUCTURE)
			FLEFRecord validFrom = null;
			FLEFRecord validFromStruct = FLEFRecordUtils.findChild(nameNode, "VALID_FROM");
			if(validFromStruct != null){
				validFrom = FLEFRecordUtils.findChild(validFromStruct, "DATE_STRUCTURE");
			}

			// VALID_TO (DATE_STRUCTURE)
			FLEFRecord validTo = null;
			FLEFRecord validToStruct = FLEFRecordUtils.findChild(nameNode, "VALID_TO");
			if(validToStruct != null){
				validTo = FLEFRecordUtils.findChild(validToStruct, "DATE_STRUCTURE");
			}

			// TEXT_VALUE_VARIANT: PHONETIC and TRANSCRIPTION
			List<VariantEntry> variants = new ArrayList<>();
			for(FLEFRecord child : nameNode.getChildren()){
				String tag = child.getTag();
				if("PHONETIC".equals(tag)){
					String system = child.getValue();
					String variantValue = FLEFRecordUtils.getChildValue(child, "VALUE");
					if(system != null && !system.isEmpty() && variantValue != null && !variantValue.isEmpty()){
						variants.add(new VariantEntry("PHONETIC", system, null, variantValue));
					}
				}
				else if("TRANSCRIPTION".equals(tag)){
					String system = child.getValue();
					String transType = FLEFRecordUtils.getChildValue(child, "TYPE");
					String variantValue = FLEFRecordUtils.getChildValue(child, "VALUE");
					if(system != null && !system.isEmpty() && variantValue != null && !variantValue.isEmpty()){
						variants.add(new VariantEntry("TRANSCRIPTION", system, transType, variantValue));
					}
				}
			}

			// NOTE references
			List<String> noteIds = new ArrayList<>();
			for(FLEFRecord child : nameNode.getChildren()){
				if("NOTE".equals(child.getTag()) && child.getValue() != null){
					noteIds.add(child.getValue());
				}
			}

			// SOURCE_CITATION
			List<FLEFRecord> sourceCitations = new ArrayList<>();
			for(FLEFRecord child : nameNode.getChildren()){
				if("SOURCE".equals(child.getTag())){
					sourceCitations.add(child);
				}
			}

			nameEntries.add(new NameEntry(value, type, locale,
				validFrom, validTo,
				variants, noteIds, sourceCitations));
			nameListModel.addElement(nameEntries.getLast().toString());
		}
	}

	// ==================== Data Saving ====================

	public void saveToRecord(FLEFRecord record){
		// Remove all existing NAME children
		List<FLEFRecord> toRemove = record.findChildren("NAME");
		for(FLEFRecord child : toRemove){
			record.removeChild(child);
		}

		int baseLevel = 1;
		for(NameEntry entry : nameEntries){
			FLEFRecord nameNode = FLEFRecord.createChild(baseLevel, "NAME");

			// VALUE
			FLEFRecord valueNode = FLEFRecord.createChildWithValue(baseLevel + 1, "VALUE", entry.value);
			nameNode.addChild(valueNode);

			// TYPE
			if(entry.type != null && !entry.type.isEmpty()){
				FLEFRecord typeNode = FLEFRecord.createChildWithValue(baseLevel + 1, "TYPE", entry.type);
				nameNode.addChild(typeNode);
			}

			// LOCALE
			if(entry.locale != null && !entry.locale.isEmpty()){
				FLEFRecord localeNode = FLEFRecord.createChildWithValue(baseLevel + 1, "LOCALE", entry.locale);
				nameNode.addChild(localeNode);
			}

			// VALID_FROM
			if(entry.validFrom != null && !entry.validFrom.getChildren().isEmpty()){
				FLEFRecord validFromStruct = FLEFRecord.createChild(baseLevel + 1, "VALID_FROM");
				FLEFRecord dateStructCopy = copyRecordWithLevel(entry.validFrom, baseLevel + 2);
				validFromStruct.addChild(dateStructCopy);
				nameNode.addChild(validFromStruct);
			}

			// VALID_TO
			if(entry.validTo != null && !entry.validTo.getChildren().isEmpty()){
				FLEFRecord validToStruct = FLEFRecord.createChild(baseLevel + 1, "VALID_TO");
				FLEFRecord dateStructCopy = copyRecordWithLevel(entry.validTo, baseLevel + 2);
				validToStruct.addChild(dateStructCopy);
				nameNode.addChild(validToStruct);
			}

			// TEXT_VALUE_VARIANT: PHONETIC / TRANSCRIPTION
			for(VariantEntry variant : entry.variants){
				FLEFRecord variantNode = FLEFRecord.createChildWithValue(
					baseLevel + 1, variant.type, variant.system);
				if("TRANSCRIPTION".equals(variant.type) && variant.transcriptionType != null && !variant.transcriptionType.isEmpty()){
					FLEFRecord transTypeNode = FLEFRecord.createChildWithValue(
						baseLevel + 2, "TYPE", variant.transcriptionType);
					variantNode.addChild(transTypeNode);
				}
				FLEFRecord variantValueNode = FLEFRecord.createChildWithValue(
					baseLevel + 2, "VALUE", variant.value);
				variantNode.addChild(variantValueNode);
				nameNode.addChild(variantNode);
			}

			// NOTE references
			for(String noteId : entry.noteIds){
				if(noteId != null && !noteId.isEmpty()){
					FLEFRecord noteNode = FLEFRecord.createChildWithValue(baseLevel + 1, "NOTE", noteId);
					nameNode.addChild(noteNode);
				}
			}

			// SOURCE_CITATION
			for(FLEFRecord citation : entry.sourceCitations){
				citation.setLevel(baseLevel + 1);
				citation.setTag("SOURCE");
				nameNode.addChild(citation);
			}

			record.addChild(nameNode);
		}
	}

	/**
	 * Copies a record and all its children, adjusting the level offset.
	 */
	private FLEFRecord copyRecordWithLevel(FLEFRecord source, int newLevel){
		if(source == null) return null;
		FLEFRecord copy = new FLEFRecord();
		copy.setLevel(newLevel);
		copy.setTag(source.getTag());
		copy.setValue(source.getValue());
		for(FLEFRecord child : source.getChildren()){
			copy.addChild(copyRecordWithLevel(child, newLevel + 1));
		}
		return copy;
	}

	// ==================== Actions ====================

	private void createNewName(){
		NameEntry newEntry = showNameDialog(null);
		if(newEntry != null){
			nameEntries.add(newEntry);
			nameListModel.addElement(newEntry.toString());
		}
	}

	private void editName(){
		int idx = nameList.getSelectedIndex();
		if(idx == -1) return;

		NameEntry current = nameEntries.get(idx);
		NameEntry updated = showNameDialog(current);
		if(updated != null){
			nameEntries.set(idx, updated);
			nameListModel.set(idx, updated.toString());
		}
	}

	private void deleteName(){
		int idx = nameList.getSelectedIndex();
		if(idx == -1) return;

		int confirm = JOptionPane.showConfirmDialog(parentDialog,
			"Remove this name?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			nameEntries.remove(idx);
			nameListModel.remove(idx);
		}
	}

	// ==================== Main Name Dialog ====================

	private NameEntry showNameDialog(NameEntry initial){
		JDialog dialog = new JDialog(parentDialog, initial == null? "Add Name": "Edit Name", true);
		dialog.setLayout(new MigLayout("ins 10, fillx", "[right]rel[grow]", "[]5[]5[]5[]5[]5[]"));

		// VALUE
		JTextArea valueArea = new JTextArea(3, 25);
		valueArea.setLineWrap(true);
		valueArea.setWrapStyleWord(true);
		if(initial != null) valueArea.setText(initial.value);

		JScrollPane valueScrollPane = GUIHelper.createScrollPane(valueArea);

		dialog.add(new JLabel("Name Value:"), "align label,top");
		dialog.add(valueScrollPane, "growx,wrap");

		// TYPE
		JComboBox<String> typeCombo = new JComboBox<>(new String[]{"", "official", "colonial", "indigenous"});
		if(initial != null && !initial.type.isEmpty()){
			typeCombo.setSelectedItem(initial.type);
		}

		dialog.add(new JLabel("Type:"), "align label");
		dialog.add(typeCombo, "growx,wrap");

		// LOCALE
		JComboBox<String> localeCombo = new JComboBox<>(new String[]{"", "en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la"});
		if(initial != null && !initial.locale.isEmpty()){
			localeCombo.setSelectedItem(initial.locale);
		}

		dialog.add(new JLabel("Locale:"), "align label");
		dialog.add(localeCombo, "growx,wrap");

		// ----- VALID FROM / VALID TO -----
		DateFieldPanel validFromPanel = new DateFieldPanel(model, dialog, "Valid From");
		validFromPanel.loadFromRecord(initial != null? initial.validFrom: null);

		DateFieldPanel validToPanel = new DateFieldPanel(model, dialog, "Valid To");
		validToPanel.loadFromRecord(initial != null? initial.validTo: null);

		dialog.add(validFromPanel, "span 2,growx,wrap");
		dialog.add(validToPanel, "span 2,growx,wrap");

		// ----- VARIANTS -----
		JPanel variantsPanel = new JPanel(new MigLayout("ins 0, fillx", "[grow]", ""));
		variantsPanel.setBorder(new TitledBorder("Variants"));
		DefaultListModel<String> variantModel = new DefaultListModel<>();
		JList<String> variantList = new JList<>(variantModel);
		List<VariantEntry> currentVariants = new ArrayList<>(initial != null? initial.variants: new ArrayList<>());
		for(VariantEntry v : currentVariants){
			variantModel.addElement(v.toString());
		}

		JScrollPane variantScroll = GUIHelper.createScrollPane(variantList);

		GUIHelper.installBehavior(variantList,
			() -> variantList.getSelectedIndex() >= 0,
			() -> {
				int idx = variantList.getSelectedIndex();
				if(idx != -1){
					VariantEntry current = currentVariants.get(idx);
					VariantEntry updated = showVariantDialog(dialog, current);
					if(updated != null){
						currentVariants.set(idx, updated);
						variantModel.set(idx, updated.toString());
					}
				}
			},
			() -> {
				VariantEntry newVariant = showVariantDialog(dialog, null);
				if(newVariant != null){
					currentVariants.add(newVariant);
					variantModel.addElement(newVariant.toString());
				}
			},
			() -> {
				int idx = variantList.getSelectedIndex();
				if(idx != -1){
					if(JOptionPane.showConfirmDialog(dialog, "Remove this variant?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
						currentVariants.remove(idx);
						variantModel.remove(idx);
					}
				}
			},
			builder -> {
				builder.item("Add...", () -> {
					VariantEntry newVariant = showVariantDialog(dialog, null);
					if(newVariant != null){
						currentVariants.add(newVariant);
						variantModel.addElement(newVariant.toString());
					}
				});
				builder.separator();
				builder.selectionSensitiveItem("Edit...", () -> {
					int idx = variantList.getSelectedIndex();
					if(idx != -1){
						VariantEntry current = currentVariants.get(idx);
						VariantEntry updated = showVariantDialog(dialog, current);
						if(updated != null){
							currentVariants.set(idx, updated);
							variantModel.set(idx, updated.toString());
						}
					}
				});
				builder.selectionSensitiveItem("Remove", () -> {
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

		// ----- NOTES -----
		JPanel notesPanel = new JPanel(new MigLayout("ins 0, fillx", "[grow]", ""));
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
			() -> {
				int idx = noteList.getSelectedIndex();
				if(idx != -1){
					editNote(dialog, currentNoteIds, noteModel, idx);
				}
			},
			() -> addExistingNote(dialog, currentNoteIds, noteModel),
			() -> {
				int idx = noteList.getSelectedIndex();
				if(idx != -1){
					if(JOptionPane.showConfirmDialog(dialog, "Remove this note?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
						currentNoteIds.remove(idx);
						noteModel.remove(idx);
					}
				}
			},
			builder -> {
				builder.item("Create New...", () -> createNewNote(dialog, currentNoteIds, noteModel));
				builder.item("Add Existing...", () -> addExistingNote(dialog, currentNoteIds, noteModel));
				builder.separator();
				builder.selectionSensitiveItem("Edit...", () -> {
					int idx = noteList.getSelectedIndex();
					if(idx != -1){
						editNote(dialog, currentNoteIds, noteModel, idx);
					}
				});
				builder.selectionSensitiveItem("Remove", () -> {
					int idx = noteList.getSelectedIndex();
					if(idx != -1){
						if(JOptionPane.showConfirmDialog(dialog, "Remove this note?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
							currentNoteIds.remove(idx);
							noteModel.remove(idx);
						}
					}
				});
			});

		notesPanel.add(noteScroll, "growx,wrap");
		dialog.add(notesPanel, "span 2,growx,wrap");

		// ----- SOURCE CITATIONS -----
		JPanel sourcePanel = new JPanel(new MigLayout("ins 0, fillx", "[grow]", ""));
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
			() -> {
				int idx = sourceList.getSelectedIndex();
				if(idx != -1){
					editSource(dialog, currentSources, sourceModel, idx);
				}
			},
			() -> addExistingSource(dialog, currentSources, sourceModel),
			() -> {
				int idx = sourceList.getSelectedIndex();
				if(idx != -1){
					if(JOptionPane.showConfirmDialog(dialog, "Remove this source citation?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
						currentSources.remove(idx);
						sourceModel.remove(idx);
					}
				}
			},
			builder -> {
				builder.item("Add Existing...", () -> addExistingSource(dialog, currentSources, sourceModel));
				builder.separator();
				builder.selectionSensitiveItem("Edit...", () -> {
					int idx = sourceList.getSelectedIndex();
					if(idx != -1){
						editSource(dialog, currentSources, sourceModel, idx);
					}
				});
				builder.selectionSensitiveItem("Remove", () -> {
					int idx = sourceList.getSelectedIndex();
					if(idx != -1){
						if(JOptionPane.showConfirmDialog(dialog, "Remove this source citation?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
							currentSources.remove(idx);
							sourceModel.remove(idx);
						}
					}
				});
			});

		sourcePanel.add(sourceScroll, "growx,wrap");
		dialog.add(sourcePanel, "span 2,growx,wrap");

		// ---- OK / Cancel ----
		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		dialog.add(btnPanel, "span 2,growx");

		final NameEntry[] result = {null};
		okBtn.addActionListener(e -> {
			String value = valueArea.getText().trim();
			if(value.isEmpty()){
				JOptionPane.showMessageDialog(dialog, "Name value cannot be empty.",
					"Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			// Validate dates
			if(!validFromPanel.validateRequiredFields() || !validToPanel.validateRequiredFields()){
				// The panels show their own error messages
				return;
			}

			String type = (String)typeCombo.getSelectedItem();
			String locale = (String)localeCombo.getSelectedItem();

			// Save date structures
			FLEFRecord validFromRecord = validFromPanel.saveToRecord();
			FLEFRecord validToRecord = validToPanel.saveToRecord();

			result[0] = new NameEntry(value,
				type != null && !type.isEmpty()? type: null,
				locale != null && !locale.isEmpty()? locale: null,
				validFromRecord,
				validToRecord,
				currentVariants,
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

	// ==================== Variant Dialog ====================

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

		JComboBox<String> transTypeCombo = new JComboBox<>(new String[]{"", "romanized", "anglicized", "cyrillized", "francized", "gairaigized", "latinized"});
		if(initial != null && "TRANSCRIPTION".equals(initial.type) && initial.transcriptionType != null){
			transTypeCombo.setSelectedItem(initial.transcriptionType);
		}
		transTypeCombo.setEnabled("TRANSCRIPTION".equals(typeCombo.getSelectedItem()));

		typeCombo.addActionListener(e -> {
			transTypeCombo.setEnabled("TRANSCRIPTION".equals(typeCombo.getSelectedItem()));
		});

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
				JOptionPane.showMessageDialog(dialog, "System cannot be empty.",
					"Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if(value.isEmpty()){
				JOptionPane.showMessageDialog(dialog, "Value cannot be empty.",
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
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
		return result[0];
	}

	// ==================== Note helper methods ====================

	private void addExistingNote(Dialog dialog, List<String> currentNoteIds, DefaultListModel<String> noteModel){
		GenericSelectionDialog<?> selDialog = new GenericSelectionDialog<>(
			getParentFrame(dialog), model, noteHandler, selectedId -> {
			if(selectedId != null && !currentNoteIds.contains(selectedId)){
				currentNoteIds.add(selectedId);
				noteModel.addElement(getNoteDisplayName(selectedId));
			}
		});
		selDialog.setVisible(true);
	}

	private void createNewNote(Dialog dialog, List<String> currentNoteIds, DefaultListModel<String> noteModel){
		Set<String> before = new HashSet<>(currentNoteIds);
		JDialog newNoteDialog = noteHandler.createNewDialog(getParentFrame(dialog), model);
		newNoteDialog.setVisible(true);
		for(FLEFRecord rec : model.getRecordsByType("NOTE")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !currentNoteIds.contains(id)){
				currentNoteIds.add(id);
				noteModel.addElement(getNoteDisplayName(id));
				break;
			}
		}
	}

	private void editNote(Dialog dialog, List<String> currentNoteIds, DefaultListModel<String> noteModel, int idx){
		String noteId = currentNoteIds.get(idx);
		FLEFRecord rec = model.getRecordById(noteId);
		if(rec == null){
			JOptionPane.showMessageDialog(dialog, "Note record not found: " + noteId, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog editDialog = noteHandler.createEditDialog(getParentFrame(dialog), model, rec);
		editDialog.setVisible(true);
		noteModel.set(idx, getNoteDisplayName(noteId));
	}

	// ==================== Source helper methods ====================

	private void addExistingSource(Dialog dialog, List<FLEFRecord> currentSources, DefaultListModel<String> sourceModel){
		GenericSelectionDialog<?> selDialog = new GenericSelectionDialog<>(
			getParentFrame(dialog), model, sourceHandler, selectedId -> {
			if(selectedId != null){
				FLEFRecord citation = FLEFRecord.createChildWithValue(1, "SOURCE", selectedId);
				currentSources.add(citation);
				sourceModel.addElement(getSourceCitationDisplay(citation));
			}
		});
		selDialog.setVisible(true);
	}

	private void editSource(Dialog dialog, List<FLEFRecord> currentSources, DefaultListModel<String> sourceModel, int idx){
		FLEFRecord citation = currentSources.get(idx);
		if(citation == null) return;
		SourceCitationDialog editDialog = new SourceCitationDialog(getParentFrame(dialog), model, citation);
		editDialog.setVisible(true);
		if(editDialog.isSaved()){
			FLEFRecord updated = editDialog.getCitationRecord();
			if(updated != null){
				currentSources.set(idx, updated);
				sourceModel.set(idx, getSourceCitationDisplay(updated));
			}
		}
	}

	// ==================== Utility Methods ====================

	private Frame getParentFrame(Container container){
		Container parent = container;
		while(parent != null && !(parent instanceof Frame)){
			parent = parent.getParent();
		}
		return (Frame)parent;
	}

	private String getNoteDisplayName(String noteId){
		FLEFRecord rec = model.getRecordById(noteId);
		if(rec != null) return noteHandler.getDisplayName(rec);
		return noteId;
	}

	private String getSourceCitationDisplay(FLEFRecord citation){
		String sourceId = citation.getValue();
		if(sourceId != null){
			FLEFRecord rec = model.getRecordById(sourceId);
			if(rec != null){
				return sourceHandler.getDisplayName(rec);
			}
			return sourceId;
		}
		return "[empty]";
	}

	public boolean hasData(){
		return !nameEntries.isEmpty();
	}

	public boolean validateRequiredFields(){
		if(nameEntries.isEmpty()){
			JOptionPane.showMessageDialog(parentDialog,
				"At least one name is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

}
