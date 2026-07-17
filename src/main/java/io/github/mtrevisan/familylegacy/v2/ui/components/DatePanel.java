package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.SourceCitationDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Panel for editing a {@code DATE_STRUCTURE} according to FLEF 0.1.0.
 * <p>
 * Structure:
 * <pre>
 * DATE_STRUCTURE :=
 * n DATE    {1:1}
 *   +1 <<DATE_VALUE>>    {1:1}
 *   +1 <<SOURCE_CITATION>>    {0:M}
 *   +1 <<EVIDENCE_QUALIFIERS>>    {0:1}
 * </pre>
 * <p>
 * DATE_VALUE can be VALUE, BOUNDED, or SPANNING.
 * This panel uses a JTabbedPane to switch between the three modes.
 */
public class DatePanel extends JPanel{

	private final FLEFModel model;
	private final Dialog parentDialog;

	private final SourceHandler sourceHandler = new SourceHandler();

	// Tabs for date types
	private final SingleDatePanel valueDatePanel = new SingleDatePanel();
	private final BoundedDatePanel boundedDatePanel = new BoundedDatePanel();
	private final SpanningDatePanel spanningDatePanel = new SpanningDatePanel();

	// Source Citations (0:M)
	private final DefaultListModel<String> sourceListModel = new DefaultListModel<>();
	private final JList<String> sourceList = new JList<>(sourceListModel);
	private final List<FLEFRecord> sourceCitations = new ArrayList<>();

	// Evidence Qualifiers
	private final JComboBox<String> certaintyCombo = new JComboBox<>(new String[]{"", "challenged", "disproven", "proven"});
	private final JComboBox<String> credibilityCombo = new JComboBox<>(new String[]{"", "0", "1", "2", "3"});


	public DatePanel(FLEFModel model, Dialog parent){
		this.model = model;
		this.parentDialog = parent;
		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout("ins 0, fillx, wrap 1", "[grow]", "[]5[]5[]5"));
		setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

		// Tabbed pane for date types
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Value", valueDatePanel);
		tabbedPane.addTab("Bounded", boundedDatePanel);
		tabbedPane.addTab("Spanning", spanningDatePanel);

		add(tabbedPane, "growx,wrap");

		// Source Citations
		add(createSourcePanel(), "growx");

		// Evidence Qualifiers
		add(createEvidencePanel(), "growx");
	}

	private JPanel createSourcePanel(){
		JPanel panel = new JPanel(new MigLayout("fillx"));
		panel.setBorder(new TitledBorder("Source Citations"));

		sourceList.setVisibleRowCount(3);
		sourceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton editBtn = new JButton("Edit");
		JButton removeBtn = new JButton("Remove");

		btnPanel.add(addBtn);
		btnPanel.add(editBtn);
		btnPanel.add(removeBtn);
		editBtn.setEnabled(false);
		removeBtn.setEnabled(false);

		sourceList.addListSelectionListener(e -> {
			boolean selected = sourceList.getSelectedIndex() >= 0;
			editBtn.setEnabled(selected);
			removeBtn.setEnabled(selected);
		});

		addBtn.addActionListener(e -> addSourceCitation());
		editBtn.addActionListener(e -> editSourceCitation());
		removeBtn.addActionListener(e -> removeSourceCitation());

		JScrollPane scrollPane = new JScrollPane(sourceList);
		scrollPane.setPreferredSize(new Dimension(0, 60));
		panel.add(scrollPane, "growx,wrap");
		panel.add(btnPanel, "growx");
		return panel;
	}

	private JPanel createEvidencePanel(){
		JPanel panel = new JPanel(new MigLayout("ins 5, fillx", "[right]rel[grow]", "[]5[]"));
		panel.setBorder(new TitledBorder("Evidence Qualifiers"));

		panel.add(new JLabel("Certainty:"), "align label");
		panel.add(certaintyCombo, "growx,wrap");

		panel.add(new JLabel("Credibility:"), "align label");
		panel.add(credibilityCombo, "growx,wrap");

		return panel;
	}

	// ==================== Source Citation Methods ====================

	private void addSourceCitation(){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(this, "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		GenericSelectionDialog<?> selDialog = new GenericSelectionDialog<>(
			(Frame)SwingUtilities.getWindowAncestor(this), model, sourceHandler, selectedId -> {
			if(selectedId != null){
				FLEFRecord citation = FLEFRecord.createChildWithValue(1, "SOURCE_CITATION", selectedId);
				sourceCitations.add(citation);
				sourceListModel.addElement(getSourceCitationDisplay(citation));
			}
		});
		selDialog.setVisible(true);
	}

	private void editSourceCitation(){
		int idx = sourceList.getSelectedIndex();
		if(idx == -1) return;

		FLEFRecord existing = sourceCitations.get(idx);
		SourceCitationDialog dialog = new SourceCitationDialog(
			(Frame)SwingUtilities.getWindowAncestor(this), model, existing);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			FLEFRecord updated = dialog.getCitationRecord();
			if(updated != null){
				sourceCitations.set(idx, updated);
				sourceListModel.set(idx, getSourceCitationDisplay(updated));
			}
		}
	}

	private void removeSourceCitation(){
		int idx = sourceList.getSelectedIndex();
		if(idx == -1) return;

		if(JOptionPane.showConfirmDialog(this, "Remove this source citation?", "Confirm",
			JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
			sourceCitations.remove(idx);
			sourceListModel.remove(idx);
		}
	}

	private String getSourceCitationDisplay(FLEFRecord citation){
		String sourceId = citation.getValue();
		if(sourceId != null){
			FLEFRecord rec = model.getRecordById(sourceId);
			if(rec != null && sourceHandler != null){
				return sourceHandler.getDisplayName(rec);
			}
			return sourceId;
		}
		return "[empty]";
	}

	// ==================== Load / Save ====================

	public void loadFromRecord(FLEFRecord dateStructure){
		clear();

		if(dateStructure == null){
			return;
		}

		// DATE_STRUCTURE è il nodo "DATE"
		// I figli di DATE sono DATE_VALUE, SOURCE_CITATION, EVIDENCE_QUALIFIERS
		FLEFRecord dateValue = FLEFRecordUtils.findChild(dateStructure, "DATE_VALUE");
		if(dateValue != null){
			loadDateValue(dateValue);
		}

		// SOURCE_CITATION
		sourceCitations.clear();
		sourceListModel.clear();
		for(FLEFRecord child : dateStructure.getChildren()){
			if("SOURCE_CITATION".equals(child.getTag())){
				sourceCitations.add(child);
				sourceListModel.addElement(getSourceCitationDisplay(child));
			}
		}

		// EVIDENCE_QUALIFIERS
		FLEFRecord evidence = FLEFRecordUtils.findChild(dateStructure, "EVIDENCE_QUALIFIERS");
		if(evidence != null){
			String certainty = FLEFRecordUtils.getChildValue(evidence, "CERTAINTY");
			certaintyCombo.setSelectedItem(certainty != null? certainty: "");
			String credibility = FLEFRecordUtils.getChildValue(evidence, "CREDIBILITY");
			credibilityCombo.setSelectedItem(credibility != null? credibility: "");
		}
	}

	private void loadDateValue(FLEFRecord dateValue){
		// VALUE
		FLEFRecord value = FLEFRecordUtils.findChild(dateValue, "VALUE");
		if(value != null){
			FLEFRecord qualified = FLEFRecordUtils.findChild(value, "QUALIFIED_DATE");
			if(qualified != null){
				valueDatePanel.loadFromQualifiedDate(qualified);
			}
		}

		// BOUNDED
		FLEFRecord bounded = FLEFRecordUtils.findChild(dateValue, "BOUNDED");
		if(bounded != null){
			boundedDatePanel.loadFromRecord(bounded);
		}

		// SPANNING
		FLEFRecord spanning = FLEFRecordUtils.findChild(dateValue, "SPANNING");
		if(spanning != null){
			spanningDatePanel.loadFromRecord(spanning);
		}
	}

	public FLEFRecord saveToRecord(FLEFRecord target){
		if(!hasData()){
			return null;
		}

		FLEFRecord record = target != null? target: new FLEFRecord();
		record.setTag("DATE");

		// DATE_VALUE
		FLEFRecord dateValue = FLEFRecord.createChild(2, "DATE_VALUE");

		// VALUE
		if(valueDatePanel.hasData()){
			FLEFRecord value = FLEFRecord.createChild(3, "VALUE");
			FLEFRecord qualified = valueDatePanel.saveToQualifiedDate(null);
			if(qualified != null && !qualified.getChildren().isEmpty()){
				FLEFRecord qualifiedAdjusted = FLEFRecordUtils.copyRecordWithLevel(qualified, 4);
				value.addChild(qualifiedAdjusted);
				dateValue.addChild(value);
			}
		}

		// BOUNDED
		if(boundedDatePanel.hasData()){
			FLEFRecord bounded = FLEFRecord.createChild(3, "BOUNDED");
			FLEFRecord boundedData = boundedDatePanel.saveToRecord(null);
			if(boundedData != null && !boundedData.getChildren().isEmpty()){
				for(FLEFRecord child : boundedData.getChildren()){
					FLEFRecord adjustedChild = FLEFRecordUtils.copyRecordWithLevel(child, 4);
					bounded.addChild(adjustedChild);
				}
				dateValue.addChild(bounded);
			}
		}

		// SPANNING
		if(spanningDatePanel.hasData()){
			FLEFRecord spanning = FLEFRecord.createChild(3, "SPANNING");
			FLEFRecord spanningData = spanningDatePanel.saveToRecord(null);
			if(spanningData != null && !spanningData.getChildren().isEmpty()){
				for(FLEFRecord child : spanningData.getChildren()){
					FLEFRecord adjustedChild = FLEFRecordUtils.copyRecordWithLevel(child, 4);
					spanning.addChild(adjustedChild);
				}
				dateValue.addChild(spanning);
			}
		}

		if(!dateValue.getChildren().isEmpty()){
			record.addChild(dateValue);
		}

		// SOURCE_CITATION (livello 2)
		for(FLEFRecord citation : sourceCitations){
			citation.setLevel(2);
			citation.setTag("SOURCE_CITATION");
			record.addChild(citation);
		}

		// EVIDENCE_QUALIFIERS (livello 2)
		String certainty = (String)certaintyCombo.getSelectedItem();
		String credibility = (String)credibilityCombo.getSelectedItem();
		if((certainty != null && !certainty.isEmpty()) || (credibility != null && !credibility.isEmpty())){
			FLEFRecord evidence = FLEFRecord.createChild(2, "EVIDENCE_QUALIFIERS");
			if(certainty != null && !certainty.isEmpty()){
				FLEFRecordUtils.updateChildValue(evidence, "CERTAINTY", certainty);
			}
			if(credibility != null && !credibility.isEmpty()){
				FLEFRecordUtils.updateChildValue(evidence, "CREDIBILITY", credibility);
			}
			record.addChild(evidence);
		}

		return record;
	}

	public void clear(){
		valueDatePanel.clear();
		boundedDatePanel.clear();
		spanningDatePanel.clear();
		sourceCitations.clear();
		sourceListModel.clear();
		certaintyCombo.setSelectedIndex(0);
		credibilityCombo.setSelectedIndex(0);
	}

	public boolean hasData(){
		return valueDatePanel.hasData()
					 || boundedDatePanel.hasData()
					 || spanningDatePanel.hasData();
	}

	public boolean validateRequiredFields(){
		if(valueDatePanel.hasData()){
			return valueDatePanel.validateRequiredFields();
		}
		if(boundedDatePanel.hasData()){
			return boundedDatePanel.validateRequiredFields();
		}
		if(spanningDatePanel.hasData()){
			return spanningDatePanel.validateRequiredFields();
		}
		return true;
	}

}
