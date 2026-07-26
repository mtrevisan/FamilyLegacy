package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/**
 * Panel for editing a list of {@code NAME_STRUCTURE} according to FLEF 0.1.0.
 */
public class NameListPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 8111273439637814448L;

	private final FLEFModel model;
	private final Dialog parentDialog;

	private final DefaultListModel<String> nameListModel = new DefaultListModel<>();
	private final JList<String> nameList = new JList<>(nameListModel);
	private final List<NameEntry> nameEntries = new ArrayList<>();

	/**
	 * Internal representation of a NAME with full TEXT_VALUE support.
	 */
	private static class NameEntry{
		private final String value;
		private final String type;
		private final String locale;
		private final FLEFRecord validFrom;
		private final FLEFRecord validTo;
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
			if(!type.isEmpty()) sb.append(" [").append(type).append("]");
			if(!locale.isEmpty()) sb.append(" (").append(locale).append(")");
			if(validFrom != null || validTo != null){
				sb.append(" [");
				if(validFrom != null){
					sb.append("from ").append(extractDateSummary(validFrom));
				}
				if(validTo != null){
					if(validFrom != null) sb.append(" ");
					sb.append("to ").append(extractDateSummary(validTo));
				}
				sb.append("]");
			}
			if(!variants.isEmpty()) sb.append(" (").append(variants.size()).append(" variants)");
			if(!noteIds.isEmpty()) sb.append(" (").append(noteIds.size()).append(" notes)");
			if(!sourceCitations.isEmpty()) sb.append(" (").append(sourceCitations.size()).append(" sources)");
			return sb.toString();
		}

		/**
		 * Extracts a human-readable summary from a DATE wrapper (tag "DATE").
		 * The wrapper can contain POINT, BOUNDED, or SPANNING.
		 */
		private static String extractDateSummary(FLEFRecord dateWrapper){
			if(dateWrapper == null) return "";
			FLEFRecord point = FLEFRecordUtils.findChild(dateWrapper, "POINT");
			if(point != null) return formatPointDate(point);
			FLEFRecord bounded = FLEFRecordUtils.findChild(dateWrapper, "BOUNDED");
			if(bounded != null) return formatBounded(bounded);
			FLEFRecord spanning = FLEFRecordUtils.findChild(dateWrapper, "SPANNING");
			if(spanning != null) return formatSpanning(spanning);
			return "";
		}

		private static String formatPointDate(FLEFRecord point){
			if(point == null) return "";

			String dateStr = formatSingleDate(point);
			if(dateStr.isEmpty()) return "";

			FLEFRecord approx = FLEFRecordUtils.findChild(point, "APPROXIMATE");
			if(approx != null){
				String basis = FLEFRecordUtils.getChildValue(approx, "BASIS");
				String margin = FLEFRecordUtils.getChildValue(approx, "MARGIN");
				if(basis != null || margin != null){
					dateStr += " (approx";
					if(basis != null) dateStr += " basis: " + basis;
					if(margin != null) dateStr += " margin: " + margin;
					dateStr += ")";
				}
				else{
					dateStr += " (approx)";
				}
			}
			return dateStr;
		}

		private static String formatBounded(FLEFRecord bounded){
			String before = extractEndpointDate(bounded, "NOT_BEFORE");
			String after = extractEndpointDate(bounded, "NOT_AFTER");
			if(!before.isEmpty() && !after.isEmpty()) return "between " + before + " and " + after;
			if(!before.isEmpty()) return "after " + before;
			if(!after.isEmpty()) return "before " + after;
			return "[bounded]";
		}

		private static String formatSpanning(FLEFRecord spanning){
			String from = extractEndpointDate(spanning, "FROM");
			String to = extractEndpointDate(spanning, "TO");
			if(!from.isEmpty() && !to.isEmpty()) return "from " + from + " to " + to;
			if(!from.isEmpty()) return "from " + from;
			if(!to.isEmpty()) return "until " + to;
			return "[spanning]";
		}

		private static String extractEndpointDate(FLEFRecord parent, String childTag){
			FLEFRecord endpoint = FLEFRecordUtils.findChild(parent, childTag);
			if(endpoint == null) return "";
			String dateStr = formatSingleDate(endpoint);
			if(dateStr.isEmpty()) return "";

			FLEFRecord approx = FLEFRecordUtils.findChild(endpoint, "APPROXIMATE");
			if(approx != null){
				String basis = FLEFRecordUtils.getChildValue(approx, "BASIS");
				String margin = FLEFRecordUtils.getChildValue(approx, "MARGIN");
				if(basis != null || margin != null){
					dateStr += " (approx";
					if(basis != null) dateStr += " basis: " + basis;
					if(margin != null) dateStr += " margin: " + margin;
					dateStr += ")";
				}
				else{
					dateStr += " (approx)";
				}
			}
			return dateStr;
		}

		private static String formatSingleDate(FLEFRecord parent){
			FLEFRecord fullDate = FLEFRecordUtils.findChild(parent, "FULL_DATE");
			if(fullDate != null && fullDate.getValue() != null){
				String calendar = FLEFRecordUtils.getChildValue(fullDate, "CALENDAR");
				return fullDate.getValue() + (calendar != null? " (" + calendar + ")": "");
			}
			FLEFRecord century = FLEFRecordUtils.findChild(parent, "CENTURY");
			if(century != null && century.getValue() != null){
				String part = FLEFRecordUtils.getChildValue(century, "PART");
				String calendar = FLEFRecordUtils.getChildValue(century, "CALENDAR");
				String centuryStr = century.getValue() + "th century";
				if(part != null) centuryStr += " (" + part + ")";
				if(calendar != null) centuryStr += " (" + calendar + ")";
				return centuryStr;
			}
			FLEFRecord decade = FLEFRecordUtils.findChild(parent, "DECADE");
			if(decade != null && decade.getValue() != null){
				String calendar = FLEFRecordUtils.getChildValue(decade, "CALENDAR");
				return decade.getValue() + "s" + (calendar != null? " (" + calendar + ")": "");
			}
			return "";
		}
	}

	// ==================== Constructors ====================

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

		List<FLEFRecord> nameNodes = record.findChildren("NAME");
		for(FLEFRecord nameNode : nameNodes){
			String value = FLEFRecordUtils.getChildValue(nameNode, "VALUE");
			if(value == null || value.isEmpty()) continue;

			String type = FLEFRecordUtils.getChildValue(nameNode, "TYPE");
			String locale = FLEFRecordUtils.getChildValue(nameNode, "LOCALE");

			// VALID_FROM: contains a DATE wrapper
			FLEFRecord validFrom = null;
			FLEFRecord validFromStruct = FLEFRecordUtils.findChild(nameNode, "VALID_FROM");
			if(validFromStruct != null){
				validFrom = FLEFRecordUtils.findChild(validFromStruct, "DATE");
			}

			// VALID_TO
			FLEFRecord validTo = null;
			FLEFRecord validToStruct = FLEFRecordUtils.findChild(nameNode, "VALID_TO");
			if(validToStruct != null){
				validTo = FLEFRecordUtils.findChild(validToStruct, "DATE");
			}

			// Variants: PHONETIC and TRANSCRIPTION
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

			// SOURCE citations
			List<FLEFRecord> sourceCitations = new ArrayList<>();
			for(FLEFRecord child : nameNode.getChildren()){
				if("SOURCE".equals(child.getTag())){
					sourceCitations.add(child);
				}
			}

			nameEntries.add(new NameEntry(value, type, locale,
				validFrom, validTo,
				variants, noteIds, sourceCitations));
			nameListModel.addElement(nameEntries.get(nameEntries.size() - 1).toString());
		}
	}

	// ==================== Data Saving ====================

	public void saveToRecord(FLEFRecord record){
		// Remove all existing NAME children
		List<FLEFRecord> toRemove = record.findChildren("NAME");
		for(FLEFRecord child : toRemove){
			record.removeChild(child);
		}

		for(NameEntry entry : nameEntries){
			FLEFRecord nameNode = FLEFRecord.createChild("NAME");

			// VALUE (name text)
			FLEFRecord valueNode = FLEFRecord.createChildWithValue("VALUE", entry.value);
			nameNode.addChild(valueNode);

			// TYPE
			if(entry.type != null && !entry.type.isEmpty()){
				FLEFRecord typeNode = FLEFRecord.createChildWithValue("TYPE", entry.type);
				nameNode.addChild(typeNode);
			}

			// LOCALE
			if(entry.locale != null && !entry.locale.isEmpty()){
				FLEFRecord localeNode = FLEFRecord.createChildWithValue("LOCALE", entry.locale);
				nameNode.addChild(localeNode);
			}

			// VALID_FROM
			if(entry.validFrom != null && entry.validFrom.hasChildren()){
				FLEFRecord validFromStruct = FLEFRecord.createChild("VALID_FROM");
				validFromStruct.addChild(entry.validFrom);
				nameNode.addChild(validFromStruct);
			}

			// VALID_TO
			if(entry.validTo != null && entry.validTo.hasChildren()){
				FLEFRecord validToStruct = FLEFRecord.createChild("VALID_TO");
				validToStruct.addChild(entry.validTo);
				nameNode.addChild(validToStruct);
			}

			// PHONETIC / TRANSCRIPTION
			for(VariantEntry variant : entry.variants){
				FLEFRecord variantNode = FLEFRecord.createChildWithValue(variant.getType(), variant.getSystem());
				if("TRANSCRIPTION".equals(variant.getType()) && variant.getTranscriptionType() != null && !variant.getTranscriptionType().isEmpty()){
					FLEFRecord transTypeNode = FLEFRecord.createChildWithValue("TYPE", variant.getTranscriptionType());
					variantNode.addChild(transTypeNode);
				}
				FLEFRecord variantValueNode = FLEFRecord.createChildWithValue("VALUE", variant.getValue());
				variantNode.addChild(variantValueNode);
				nameNode.addChild(variantNode);
			}

			// NOTE references
			for(String noteId : entry.noteIds){
				if(noteId != null && !noteId.isEmpty()){
					FLEFRecord noteNode = FLEFRecord.createChildWithValue("NOTE", noteId);
					nameNode.addChild(noteNode);
				}
			}

			// SOURCE citations
			for(FLEFRecord citation : entry.sourceCitations){
				citation.setTag("SOURCE");
				nameNode.addChild(citation);
			}

			record.addChild(nameNode);
		}
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

		// VALUE (name text)
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

		// ----- VARIANTS (using VariantListPanel) -----
		VariantListPanel variantPanel = new VariantListPanel(model, dialog, "Variants");
		variantPanel.loadVariants(initial != null? initial.variants: new ArrayList<>());
		dialog.add(variantPanel, "span 2,growx,wrap");

		// ----- NOTES (using NoteListPanel) -----
		NoteListPanel notePanel = new NoteListPanel(model, dialog, "Notes");
		notePanel.loadFromNoteIds(initial != null? initial.noteIds: new ArrayList<>());
		dialog.add(notePanel, "span 2,growx,wrap");

		// ----- SOURCE CITATIONS (using SourceCitationListPanel) -----
		SourceCitationListPanel sourcePanel = new SourceCitationListPanel(model, dialog, "Source Citations");
		sourcePanel.loadFromCitations(initial != null? initial.sourceCitations: new ArrayList<>());
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
				return;
			}

			String type = (String)typeCombo.getSelectedItem();
			String locale = (String)localeCombo.getSelectedItem();

			FLEFRecord validFromRecord = validFromPanel.saveToRecord();
			FLEFRecord validToRecord = validToPanel.saveToRecord();

			result[0] = new NameEntry(value,
				type != null && !type.isEmpty()? type: null,
				locale != null && !locale.isEmpty()? locale: null,
				validFromRecord,
				validToRecord,
				variantPanel.getVariants(),
				notePanel.getNoteIds(),
				sourcePanel.getCitations());
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(parentDialog);
		dialog.setVisible(true);
		return result[0];
	}

	// ==================== Public API ====================

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
