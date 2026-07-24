package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.SourceCitationDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Panel for editing a {@code DATE_STRUCTURE} according to FLEF 0.1.0.
 * <p>
 * The actual structure (real tags):
 * <pre>
 * DATE (wrapper)
 *   +1 VALUE | BOUNDED | SPANNING
 *   +1 SOURCE (SOURCE_CITATION)
 *   +1 EVIDENCE_QUALIFIERS
 * </pre>
 * This panel uses a JTabbedPane to switch between VALUE, BOUNDED, and SPANNING.
 */
public class DatePanel extends JPanel{

	private final FLEFModel model;
	private final Dialog parentDialog;

	private final SourceHandler sourceHandler = new SourceHandler();

	// Tabs for date types
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final SingleDatePanel pointDatePanel;
	private final BoundedDatePanel boundedDatePanel;
	private final SpanningDatePanel spanningDatePanel;

	// Source Citations (0:M) - direct children of DATE
	private final DefaultListModel<String> sourceListModel = new DefaultListModel<>();
	private final JList<String> sourceList = new JList<>(sourceListModel);
	private final List<FLEFRecord> sourceCitations = new ArrayList<>();

	// Evidence Qualifiers - direct child of DATE
	private final JComboBox<String> certaintyCombo = new JComboBox<>(new String[]{"", "challenged", "disproven", "proven"});
	private final JComboBox<String> credibilityCombo = new JComboBox<>(new String[]{"", "0", "1", "2", "3"});

	public DatePanel(Dialog parent, FLEFModel model){
		this.model = model;
		this.parentDialog = parent;

		// Initialize child panels with the model
		this.pointDatePanel = new SingleDatePanel(model);
		this.boundedDatePanel = new BoundedDatePanel(model);
		this.spanningDatePanel = new SpanningDatePanel(model);

		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout("ins 0, fillx, wrap 1", "[grow]", "[]5[]5[]"));
		setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

		// Tabbed pane for date types
		JPanel pointOuter = new JPanel(new MigLayout("ins 0, fillx"));
		pointOuter.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		JPanel valueWrapper = new JPanel(new MigLayout("ins 7, fillx", "[right]rel[grow]"));
		valueWrapper.setBorder(BorderFactory.createTitledBorder("Value Date"));
		valueWrapper.add(pointDatePanel, "growx");
		pointOuter.add(valueWrapper, "growx");
		tabbedPane.addTab("Point", pointOuter);
		tabbedPane.addTab("Bounded", boundedDatePanel);
		tabbedPane.addTab("Spanning", spanningDatePanel);
		tabbedPane.addChangeListener(e -> {
			switch(tabbedPane.getSelectedIndex()){
				case 0 -> {
					boundedDatePanel.clear();
					spanningDatePanel.clear();
				}
				case 1 -> {
					pointDatePanel.clear();
					spanningDatePanel.clear();
				}
				case 2 -> {
					pointDatePanel.clear();
					boundedDatePanel.clear();
				}
			}
		});
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

		JScrollPane scrollPane = GUIHelper.createScrollPane(sourceList);
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
		GenericSelectionDialog<?> selDialog = new GenericSelectionDialog<>(
			(Frame)SwingUtilities.getWindowAncestor(this), model, sourceHandler, selectedId -> {
			if(selectedId != null){
				FLEFRecord citation = FLEFRecord.createChildWithValue(1, "SOURCE", selectedId);
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
				updated.setLevel(1);
				updated.setTag("SOURCE");
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
			if(rec != null){
				return sourceHandler.getDisplayName(rec);
			}
			return sourceId;
		}
		return "[empty]";
	}

	// ==================== Load / Save ====================

	/**
	 * Loads data from a DATE wrapper record.
	 *
	 * @param dateWrapper the DATE record (wrapper), or null
	 */
	public void loadFromRecord(FLEFRecord dateWrapper){
		clear();

		if(dateWrapper == null){
			return;
		}

		// The dateWrapper is the DATE node. Its direct children are:
		// POINT, BOUNDED, SPANNING, SOURCE (citations), EVIDENCE_QUALIFIERS
		// Load the date value: one of POINT, BOUNDED, SPANNING
		FLEFRecord point = FLEFRecordUtils.findChild(dateWrapper, "POINT");
		if(point != null){
			tabbedPane.setSelectedIndex(0);
			pointDatePanel.loadFromRecord(point);
		}
		else{
			FLEFRecord bounded = FLEFRecordUtils.findChild(dateWrapper, "BOUNDED");
			if(bounded != null){
				tabbedPane.setSelectedIndex(1);
				boundedDatePanel.loadFromRecord(bounded);
			}
			else{
				FLEFRecord spanning = FLEFRecordUtils.findChild(dateWrapper, "SPANNING");
				if(spanning != null){
					tabbedPane.setSelectedIndex(2);
					spanningDatePanel.loadFromRecord(spanning);
				}
			}
		}

		// Load SOURCE_CITATION (direct children with tag "SOURCE")
		sourceCitations.clear();
		sourceListModel.clear();
		for(FLEFRecord child : dateWrapper.getChildren()){
			if("SOURCE".equals(child.getTag())){
				sourceCitations.add(child);
				sourceListModel.addElement(getSourceCitationDisplay(child));
			}
		}

		// Load EVIDENCE_QUALIFIERS
		FLEFRecord evidence = FLEFRecordUtils.findChild(dateWrapper, "EVIDENCE_QUALIFIERS");
		if(evidence != null){
			String certainty = FLEFRecordUtils.getChildValue(evidence, "CERTAINTY");
			certaintyCombo.setSelectedItem(certainty != null? certainty: "");
			String credibility = FLEFRecordUtils.getChildValue(evidence, "CREDIBILITY");
			credibilityCombo.setSelectedItem(credibility != null? credibility: "");
		}
	}

	/**
	 * Saves the current data into a DATE wrapper record.
	 *
	 * @param target an existing DATE record to update, or null to create a new one
	 * @return the DATE record, or null if no data
	 */
	public FLEFRecord saveToRecord(FLEFRecord target){
		if(!hasData()){
			return null;
		}

		FLEFRecord record = target != null? target: new FLEFRecord();
		FLEFRecordUtils.removeAllChildren(record);
		record.setTag("DATE");

		// Save the date value: VALUE, BOUNDED, or SPANNING
		switch(tabbedPane.getSelectedIndex()){
			case 0 -> saveValue(record);
			case 1 -> saveBounded(record);
			case 2 -> saveSpanning(record);
			default -> { /* do nothing */ }
		}

		// Save SOURCE_CITATION (level 1)
		for(FLEFRecord citation : sourceCitations){
			FLEFRecord copy = FLEFRecordUtils.copyRecordWithLevel(citation, 1);
			copy.setTag("SOURCE");
			record.addChild(copy);
		}

		// Save EVIDENCE_QUALIFIERS (level 1)
		String certainty = (String)certaintyCombo.getSelectedItem();
		String credibility = (String)credibilityCombo.getSelectedItem();
		if((certainty != null && !certainty.isEmpty()) || (credibility != null && !credibility.isEmpty())){
			FLEFRecord evidence = FLEFRecord.createChild(1, "EVIDENCE_QUALIFIERS");
			FLEFRecordUtils.updateChildValue(evidence, "CERTAINTY", certainty);
			FLEFRecordUtils.updateChildValue(evidence, "CREDIBILITY", credibility);
			record.addChild(evidence);
		}

		tabbedPane.setSelectedIndex(0);

		return record;
	}

	public void clear(){
		pointDatePanel.clear();
		boundedDatePanel.clear();
		spanningDatePanel.clear();
		sourceCitations.clear();
		sourceListModel.clear();
		certaintyCombo.setSelectedIndex(0);
		credibilityCombo.setSelectedIndex(0);
	}

	public boolean hasData(){
		return switch(tabbedPane.getSelectedIndex()){
			case 0 -> pointDatePanel.hasData();
			case 1 -> boundedDatePanel.hasData();
			case 2 -> spanningDatePanel.hasData();
			default -> false;
		};
	}

	public boolean validateRequiredFields(){
		return switch(tabbedPane.getSelectedIndex()){
			case 0 -> pointDatePanel.validateRequiredFields();
			case 1 -> boundedDatePanel.validateRequiredFields();
			case 2 -> spanningDatePanel.validateRequiredFields();
			default -> true;
		};
	}

	// ==================== Save helpers ====================

	/**
	 * Saves a VALUE date.
	 * Creates a VALUE node with children ISO/CENTURY/DECADE and APPROXIMATE.
	 */
	private void saveValue(FLEFRecord parent){
		if(!pointDatePanel.hasData()){
			return;
		}

		FLEFRecord valueNode = FLEFRecord.createChild(1, "POINT");
		FLEFRecord dateNode = pointDatePanel.saveToRecord(null);
		if(dateNode != null && dateNode.hasChildren()){
			// The children of dateNode are the actual date tags (ISO, CENTURY, DECADE) and APPROXIMATE
			for(FLEFRecord child : dateNode.getChildren()){
				child.setLevel(2);
				valueNode.addChild(child);
			}
			parent.addChild(valueNode);
		}
	}

	/**
	 * Saves a BOUNDED date.
	 * The BoundedDatePanel returns a node with NOT_BEFORE and NOT_AFTER (each containing date tags).
	 */
	private void saveBounded(FLEFRecord parent){
		if(!boundedDatePanel.hasData()){
			return;
		}

		FLEFRecord boundedNode = boundedDatePanel.saveToRecord(null);
		if(boundedNode != null && boundedNode.hasChildren()){
			// boundedNode already contains NOT_BEFORE/NOT_AFTER with proper levels
			for(FLEFRecord child : boundedNode.getChildren()){
				child.setLevel(1);
				parent.addChild(child);
			}
		}
	}

	/**
	 * Saves a SPANNING date.
	 * The SpanningDatePanel returns a node with FROM and TO (each containing date tags).
	 */
	private void saveSpanning(FLEFRecord parent){
		if(!spanningDatePanel.hasData()){
			return;
		}

		FLEFRecord spanningNode = spanningDatePanel.saveToRecord(null);
		if(spanningNode != null && spanningNode.hasChildren()){
			for(FLEFRecord child : spanningNode.getChildren()){
				child.setLevel(1);
				parent.addChild(child);
			}
		}
	}


	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			DatePanel dialog = new DatePanel(null, model);
			dialog.setVisible(true);
		});
	}

}
