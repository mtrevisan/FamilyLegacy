package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Dialog for editing a {@code PLACE_STRUCTURE} according to FLEF 0.0.9.
 * <p>
 * Structure:
 * <pre>
 * PLACE_STRUCTURE :=
 * n PLACE @<XREF:PLACE>@    {1:1}
 *   +1 <<SOURCE_CITATION>>    {0:M}
 *   +1 <<EVIDENCE_QUALIFIERS>>    {0:1}
 * </pre>
 */
public class PlaceStructureDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 6489523892351201199L;


	private final FLEFModel model;
	private final FLEFRecord placeRecord;
	private boolean saved;

	// Source Citations {0:M}
	private final DefaultListModel<String> sourceListModel = new DefaultListModel<>();
	private final JList<String> sourceList = new JList<>(sourceListModel);
	private final List<FLEFRecord> sourceCitations = new ArrayList<>();

	private final EvidenceQualifiersPanel qualifiers = new EvidenceQualifiersPanel("MAP", "Evidence");

	private final JButton okButton = new JButton("OK");
	private final JButton cancelButton = new JButton("Cancel");


	public PlaceStructureDialog(final Dialog parent, final FLEFModel model, final FLEFRecord existingRecord){
		super(parent, "Edit Place Structure", true);

		this.model = model;
		this.placeRecord = existingRecord;

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	private void initComponents(){
		setLayout(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]5[]5[]"));

		add(createSourceCitationsPanel(), "growx");

		add(qualifiers, "span 2, growx, wrap");

		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, "growx");

		okButton.addActionListener(e -> save());
		cancelButton.addActionListener(e -> dispose());
	}

	private JPanel createSourceCitationsPanel(){
		final JPanel panel = new JPanel(new MigLayout("fillx,top"));
		panel.setBorder(new TitledBorder("Source Citations"));

		sourceList.setVisibleRowCount(4);
		sourceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		GUIHelper.installBehavior(sourceList,
			this::editSourceCitation,
			() -> {},
			this::removeSourceCitation,
			builder -> {
				builder.item("Create New...", this::createNewSourceAndAddCitation);
				builder.item("Add Existing...", this::addSourceCitation);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editSource);
				builder.selectionSensitiveItem("Edit Citation...", this::editSourceCitation);
				builder.selectionSensitiveItem("Remove", this::removeSourceCitation);
			});

		final JScrollPane scrollPane = GUIHelper.createScrollPane(sourceList);
		panel.add(scrollPane, "growx,wrap");

		return panel;
	}

	// --- Source Citation Actions (Analogous to NoteDialog) ---

	private void addSourceCitation(){
		final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
		final GenericSelectionDialog<?> selDialog = new GenericSelectionDialog<>(
			this, model, sourceHandler, selectedId -> {
			if(selectedId != null){
				final FLEFRecord citation = FLEFRecord.createChildWithValue("SOURCE", XRefHelper.formatXRef(selectedId));
				sourceCitations.add(citation);
				sourceListModel.addElement(getSourceCitationDisplay(citation));
			}
		});
		selDialog.setVisible(true);
	}

	private void createNewSourceAndAddCitation(){
		final Set<String> before = new HashSet<>();
		for(final FLEFRecord rec : model.getRecordsByType("SOURCE")){
			if(rec.getId() != null){
				before.add(rec.getId());
			}
		}

		final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
		final JDialog dialog = sourceHandler.createNewDialog(this, model);
		dialog.setVisible(true);

		String newSourceId = null;
		for(final FLEFRecord rec : model.getRecordsByType("SOURCE")){
			if(rec.getId() != null && !before.contains(rec.getId())){
				newSourceId = rec.getId();
				break;
			}
		}

		if(newSourceId != null){
			final FLEFRecord citationRecord = FLEFRecord.createChildWithValue("SOURCE", XRefHelper.formatXRef(newSourceId));
			final SourceCitationDialog citationDialog = SourceCitationDialog.createEdit(this, model, citationRecord);
			citationDialog.setVisible(true);

			if(citationDialog.isSaved()){
				final FLEFRecord savedCitation = citationDialog.getRecord();
				if(savedCitation != null){
					savedCitation.setTag("SOURCE");
					sourceCitations.add(savedCitation);
					sourceListModel.addElement(getSourceCitationDisplay(savedCitation));
				}
			}
		}
	}

	private void editSource(){
		final int idx = sourceList.getSelectedIndex();
		if(idx == -1){
			return;
		}

		final FLEFRecord citation = sourceCitations.get(idx);
		final String rawId = XRefHelper.extractXRef(citation.getValue());
		final FLEFRecord rec = model.getRecordById(rawId);
		if(rec == null){
			JOptionPane.showMessageDialog(this, "Source record not found: " + rawId, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
		final JDialog dialog = sourceHandler.createEditDialog(this, model, rec);
		dialog.setVisible(true);

		sourceListModel.set(idx, getSourceCitationDisplay(citation));
	}

	private void editSourceCitation(){
		final int idx = sourceList.getSelectedIndex();
		if(idx == -1){
			return;
		}

		final FLEFRecord existing = sourceCitations.get(idx);
		final SourceCitationDialog dialog = SourceCitationDialog.createEdit(this, model, existing);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			final FLEFRecord updated = dialog.getRecord();
			if(updated != null){
				sourceCitations.set(idx, updated);
				sourceListModel.set(idx, getSourceCitationDisplay(updated));
			}
		}
	}

	private void removeSourceCitation(){
		final int idx = sourceList.getSelectedIndex();
		if(idx == -1){
			return;
		}

		if(JOptionPane.showConfirmDialog(this, "Remove this source citation?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
			sourceCitations.remove(idx);
			sourceListModel.remove(idx);
		}
	}

	private String getSourceCitationDisplay(final FLEFRecord citation){
		final String rawSourceId = XRefHelper.extractXRef(citation.getValue());
		if(rawSourceId != null){
			final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
			final FLEFRecord record = model.getRecordById(rawSourceId);
			return (record != null? sourceHandler.getDisplayText(record, model) : rawSourceId);
		}
		return "[empty]";
	}

	private void loadData(){
		sourceCitations.clear();
		sourceListModel.clear();
		for(final FLEFRecord child : placeRecord.getChildren()){
			if("SOURCE".equals(child.getTag())){
				sourceCitations.add(child);
				sourceListModel.addElement(getSourceCitationDisplay(child));
			}
		}

		qualifiers.load(placeRecord);
	}

	private void save(){
		if(StringUtils.isBlank(placeRecord.getValue())){
			JOptionPane.showMessageDialog(this, "Please select a valid Place.", "Validation Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Rebuild children
		FLEFRecordHelper.removeAllChildren(placeRecord);
		for(final FLEFRecord citation : sourceCitations){
			citation.setTag("SOURCE");
			placeRecord.addChild(citation);
		}

		// ---- EVIDENCE_QUALIFIERS for place ----
		String placeSourceType = qualifiers.getSourceType();
		String placeInformationType = qualifiers.getInformationType();
		String placeEvidenceType = qualifiers.getEvidenceType();
		if((placeSourceType != null && !placeSourceType.isEmpty())
				|| (placeInformationType != null && !placeInformationType.isEmpty())
				|| (placeEvidenceType != null && !placeEvidenceType.isEmpty())){
			FLEFRecordHelper.updateChildValue(placeRecord, "SOURCE_TYPE", placeSourceType);
			FLEFRecordHelper.updateChildValue(placeRecord, "INFORMATION_TYPE", placeInformationType);
			FLEFRecordHelper.updateChildValue(placeRecord, "EVIDENCE_TYPE", placeEvidenceType);
		}

		saved = true;
		dispose();
	}

	public boolean isSaved(){
		return saved;
	}

	public FLEFRecord getPlaceRecord(){
		return placeRecord;
	}

}
