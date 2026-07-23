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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.SourceCitationDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchStatusHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ScrollableContainerHost;
import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Panel for editing a {@code CONCLUSION_STRUCTURE} according to FLEF 0.1.0.
 * <p>
 * Structure:
 * <pre>
 * CONCLUSION_STRUCTURE :=
 * n CONCLUSION    {1:1}
 *   +1 CONTEXT <RESOLUTION_CONTEXT>    {1:1}
 *   +1 RESOLVES @<XREF:ID>@    {0:M}
 *   +1 PREFERRED @<XREF:ID>@    {0:1}
 *   +1 PROOF_STATUS <PROOF_STATUS_VALUE>    {1:1}
 *   +1 NARRATIVE <TEXT>    {0:1}
 *   +1 RESEARCH @<XREF:RESEARCH_STATUS>@    {0:M}
 *   +1 DATE <DATE>    {0:1}
 *   +1 <<SOURCE_CITATION>>    {0:M}
 * </pre>
 */
public class ConclusionPanel extends JPanel{

	// UI components
	private final JTextField contextField = new JTextField(30);
	private final JComboBox<String> proofStatusCombo = new JComboBox<>(new String[]{
		"", "unresearched", "conflicting_evidence", "preponderance_of_evidence", "proven", "disproven"
	});
	private final JTextArea narrativeArea = new JTextArea(4, 30);
	private final JScrollPane narrativeScrollPane = GUIHelper.createScrollPane(narrativeArea);
	private final JTextField dateField = new JTextField(15);

	// RESOLVES (0:M) - References to conflicting events or associations
	private final DefaultListModel<String> resolvesListModel = new DefaultListModel<>();
	private final JList<String> resolvesList = new JList<>(resolvesListModel);
	private final List<String> resolvesIds = new ArrayList<>();

	// PREFERRED (0:1) - The record determined to be the true one
	private final JTextField preferredDisplayField = new JTextField(20);
	private final JButton browsePreferredBtn = new JButton("Browse...");
	private final JButton clearPreferredBtn = new JButton("Clear");
	private String preferredId;

	// RESEARCH (0:M) - References to research questions
	private final DefaultListModel<String> researchListModel = new DefaultListModel<>();
	private final JList<String> researchList = new JList<>(researchListModel);
	private final List<String> researchIds = new ArrayList<>();

	// SOURCE_CITATION (0:M) - Sources that support the conclusion
	private final DefaultListModel<String> sourceListModel = new DefaultListModel<>();
	private final JList<String> sourceList = new JList<>(sourceListModel);
	private final List<FLEFRecord> sourceCitations = new ArrayList<>();

	private final FLEFModel model;
	private final Dialog parentDialog;
	private final RecordTypeHandler<?> sourceHandler = new SourceHandler();
	private final RecordTypeHandler<?> researchHandler = new ResearchStatusHandler();

	/**
	 * Constructs a new ConclusionPanel.
	 *
	 * @param model  the FLEF model
	 * @param parent the parent dialog (used for showing message dialogs)
	 */
	public ConclusionPanel(FLEFModel model, Dialog parent){
		this.model = model;
		this.parentDialog = parent;
		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout("ins 10, fillx, top", "[right]rel[grow]", "[]5[]5[]5[]5[]5[]"));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// CONTEXT
		add(new JLabel("Context:"), "align label");
		contextField.setToolTipText("e.g., 'birth_date', 'marriage_place', 'parentage', 'death_cause', 'relationship_type'");
		add(contextField, "growx,wrap");

		// PROOF_STATUS
		add(new JLabel("Proof Status:"), "align label");
		add(proofStatusCombo, "growx,wrap");

		// NARRATIVE
		add(new JLabel("Narrative:"), "align label,top");
		narrativeArea.setLineWrap(true);
		narrativeArea.setWrapStyleWord(true);
		add(narrativeScrollPane, "growx,wrap");

		// DATE
		add(new JLabel("Date:"), "align label");
		add(dateField, "growx,wrap");

		// RESOLVES
		add(createResolvesPanel(), "span 2,growx,wrap");

		// PREFERRED
		add(createPreferredPanel(), "span 2,growx,wrap");

		// RESEARCH (0:M)
		add(createResearchPanel(), "span 2,growx,wrap");

		// SOURCE_CITATION (0:M)
		add(createSourcePanel(), "span 2,growx,wrap");
	}

	// ==================== Resolves Panel ====================

	private JPanel createResolvesPanel(){
		JPanel panel = new JPanel(new MigLayout("fillx, top"));
		panel.setBorder(new TitledBorder("Resolves (Conflicting Events/Associations)"));
		resolvesList.setVisibleRowCount(3);
		resolvesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		GUIHelper.installBehavior(resolvesList,
			() -> resolvesList.getSelectedIndex() >= 0,
			this::editResolves,                // double‑click → edit
			this::addResolves,                 // INSERT key → add
			this::removeResolves,              // DELETE key → remove
			builder -> {
				builder.item("Add Existing...", this::addResolves);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editResolves);
				builder.selectionSensitiveItem("Remove", this::removeResolves);
			});

		JScrollPane scrollPane = GUIHelper.createScrollPane(resolvesList);
		panel.add(scrollPane, "growx,wrap");
		return panel;
	}

	private void addResolves(){
		String input = JOptionPane.showInputDialog(parentDialog,
			"Enter the XREF ID of the conflicting event or association (e.g., @E123@):",
			"Add Resolves", JOptionPane.PLAIN_MESSAGE);
		if(input != null && !input.trim().isEmpty()){
			String id = input.trim();
			if(!resolvesIds.contains(id)){
				resolvesIds.add(id);
				resolvesListModel.addElement(id);
			}
		}
	}

	private void editResolves(){
		int idx = resolvesList.getSelectedIndex();
		if(idx == -1) return;

		String current = resolvesIds.get(idx);
		String input = JOptionPane.showInputDialog(parentDialog,
			"Edit XREF ID:", "Edit Resolves", JOptionPane.PLAIN_MESSAGE);
		if(input != null && !input.trim().isEmpty()){
			String newId = input.trim();
			if(!resolvesIds.contains(newId) || newId.equals(current)){
				resolvesIds.set(idx, newId);
				resolvesListModel.set(idx, newId);
			}
			else{
				JOptionPane.showMessageDialog(parentDialog,
					"This ID is already in the list.", "Duplicate", JOptionPane.WARNING_MESSAGE);
			}
		}
	}

	private void removeResolves(){
		int idx = resolvesList.getSelectedIndex();
		if(idx == -1) return;
		if(JOptionPane.showConfirmDialog(parentDialog, "Remove this reference?",
			"Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
			resolvesIds.remove(idx);
			resolvesListModel.remove(idx);
		}
	}

	// ==================== Preferred Panel ====================

	private JPanel createPreferredPanel(){
		JPanel panel = new JPanel(new MigLayout("fillx, top", "[right]rel[grow]", ""));
		panel.setBorder(new TitledBorder("Preferred Record"));

		preferredDisplayField.setEditable(false);
		preferredDisplayField.setBackground(UIManager.getColor("TextField.background"));

		JPanel preferredPanel = new JPanel(new BorderLayout(5, 5));
		preferredPanel.add(preferredDisplayField, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		btnPanel.add(browsePreferredBtn);
		btnPanel.add(clearPreferredBtn);
		preferredPanel.add(btnPanel, BorderLayout.EAST);

		panel.add(preferredPanel, "growx,wrap");

		browsePreferredBtn.addActionListener(e -> browsePreferred());
		clearPreferredBtn.addActionListener(e -> clearPreferred());
		clearPreferredBtn.setEnabled(false);

		return panel;
	}

	private void browsePreferred(){
		String input = JOptionPane.showInputDialog(parentDialog,
			"Enter the XREF ID of the preferred record (e.g., @E123@, @I456@):",
			"Select Preferred Record", JOptionPane.PLAIN_MESSAGE);
		if(input != null && !input.trim().isEmpty()){
			preferredId = input.trim();
			preferredDisplayField.setText(preferredId);
			clearPreferredBtn.setEnabled(true);
		}
	}

	private void clearPreferred(){
		preferredId = null;
		preferredDisplayField.setText("");
		clearPreferredBtn.setEnabled(false);
	}

	// ==================== Research Panel ====================

	private JPanel createResearchPanel(){
		JPanel panel = new JPanel(new MigLayout("fillx, top"));
		panel.setBorder(new TitledBorder("Research References"));
		researchList.setVisibleRowCount(3);
		researchList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		GUIHelper.installBehavior(researchList,
			() -> researchList.getSelectedIndex() >= 0,
			this::editResearch,                // double‑click → edit
			this::addResearch,                 // INSERT key → add
			this::removeResearch,              // DELETE key → remove
			builder -> {
				builder.item("Add Existing...", this::addResearch);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editResearch);
				builder.selectionSensitiveItem("Remove", this::removeResearch);
			});

		JScrollPane scrollPane = GUIHelper.createScrollPane(researchList);
		panel.add(scrollPane, "growx,wrap");
		return panel;
	}

	private void addResearch(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			(Frame)SwingUtilities.getWindowAncestor(parentDialog), model, researchHandler,
			selectedId -> {
				if(selectedId != null && !researchIds.contains(selectedId)){
					researchIds.add(selectedId);
					researchListModel.addElement(getResearchDisplayName(selectedId));
				}
			});
		dialog.setVisible(true);
	}

	private void editResearch(){
		int idx = researchList.getSelectedIndex();
		if(idx == -1) return;

		String id = researchIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(parentDialog, "Research record not found: " + id,
				"Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		JDialog dialog = researchHandler.createEditDialog(
			(Frame)SwingUtilities.getWindowAncestor(parentDialog), model, rec);
		dialog.setVisible(true);

		researchListModel.set(idx, getResearchDisplayName(id));
	}

	private void removeResearch(){
		int idx = researchList.getSelectedIndex();
		if(idx == -1) return;
		if(JOptionPane.showConfirmDialog(parentDialog, "Remove this research reference?",
			"Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
			researchIds.remove(idx);
			researchListModel.remove(idx);
		}
	}

	private String getResearchDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null) return researchHandler.getDisplayName(rec);
		return id;
	}

	// ==================== Source Citations Panel ====================

	private JPanel createSourcePanel(){
		JPanel panel = new JPanel(new MigLayout("fillx, top"));
		panel.setBorder(new TitledBorder("Source Citations"));
		sourceList.setVisibleRowCount(3);
		sourceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		GUIHelper.installBehavior(sourceList,
			() -> sourceList.getSelectedIndex() >= 0,
			this::editSource,                  // double‑click → edit
			this::addSource,                   // INSERT key → add
			this::removeSource,                // DELETE key → remove
			builder -> {
				builder.item("Add Existing...", this::addSource);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editSource);
				builder.selectionSensitiveItem("Remove", this::removeSource);
			});

		JScrollPane scrollPane = GUIHelper.createScrollPane(sourceList);
		panel.add(scrollPane, "growx,wrap");
		return panel;
	}

	private void addSource(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			(Frame)SwingUtilities.getWindowAncestor(parentDialog), model, sourceHandler,
			selectedId -> {
				if(selectedId != null){
					FLEFRecord citation = FLEFRecord.createChildWithValue(1, "SOURCE", selectedId);
					sourceCitations.add(citation);
					sourceListModel.addElement(getSourceCitationDisplay(citation));
				}
			});
		dialog.setVisible(true);
	}

	private void editSource(){
		int idx = sourceList.getSelectedIndex();
		if(idx == -1) return;

		FLEFRecord existing = sourceCitations.get(idx);
		SourceCitationDialog dialog = new SourceCitationDialog(
			(Frame)SwingUtilities.getWindowAncestor(parentDialog), model, existing);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			FLEFRecord updated = dialog.getCitationRecord();
			if(updated != null){
				sourceCitations.set(idx, updated);
				sourceListModel.set(idx, getSourceCitationDisplay(updated));
			}
		}
	}

	private void removeSource(){
		int idx = sourceList.getSelectedIndex();
		if(idx == -1) return;
		if(JOptionPane.showConfirmDialog(parentDialog, "Remove this source citation?",
			"Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
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

	public void loadFromRecord(FLEFRecord conclusionRecord){
		clear();

		if(conclusionRecord == null){
			return;
		}

		// CONTEXT
		String context = FLEFRecordUtils.getChildValue(conclusionRecord, "CONTEXT");
		contextField.setText(context != null? context: "");

		// PROOF_STATUS
		String proofStatus = FLEFRecordUtils.getChildValue(conclusionRecord, "PROOF_STATUS");
		proofStatusCombo.setSelectedItem(proofStatus != null? proofStatus: "");

		// NARRATIVE
		String narrative = FLEFRecordUtils.getChildValue(conclusionRecord, "NARRATIVE");
		narrativeArea.setText(narrative != null? narrative: "");

		// DATE
		String date = FLEFRecordUtils.getChildValue(conclusionRecord, "DATE");
		dateField.setText(date != null? date: "");

		// RESOLVES
		resolvesIds.clear();
		resolvesListModel.clear();
		for(FLEFRecord child : conclusionRecord.getChildren()){
			if("RESOLVES".equals(child.getTag()) && child.getValue() != null){
				resolvesIds.add(child.getValue());
				resolvesListModel.addElement(child.getValue());
			}
		}

		// PREFERRED
		String preferred = FLEFRecordUtils.getChildValue(conclusionRecord, "PREFERRED");
		if(preferred != null && !preferred.isEmpty()){
			preferredId = preferred;
			preferredDisplayField.setText(preferred);
			clearPreferredBtn.setEnabled(true);
		}

		// RESEARCH
		researchIds.clear();
		researchListModel.clear();
		for(FLEFRecord child : conclusionRecord.getChildren()){
			if("RESEARCH".equals(child.getTag()) && child.getValue() != null){
				researchIds.add(child.getValue());
				researchListModel.addElement(getResearchDisplayName(child.getValue()));
			}
		}

		// SOURCE_CITATION
		sourceCitations.clear();
		sourceListModel.clear();
		for(FLEFRecord child : conclusionRecord.getChildren()){
			if("SOURCE".equals(child.getTag())){
				sourceCitations.add(child);
				sourceListModel.addElement(getSourceCitationDisplay(child));
			}
		}
	}

	public FLEFRecord saveToRecord(FLEFRecord targetRecord){
		if(!hasData()){
			return null;
		}

		FLEFRecord record = targetRecord != null? targetRecord: new FLEFRecord();
		// Level and tag will be set by the caller

		// CONTEXT
		String context = contextField.getText().trim();
		FLEFRecordUtils.updateChildValue(record, "CONTEXT", context);

		// PROOF_STATUS
		String proofStatus = (String)proofStatusCombo.getSelectedItem();
		FLEFRecordUtils.updateChildValue(record, "PROOF_STATUS", proofStatus);

		// NARRATIVE
		String narrative = narrativeArea.getText().trim();
		FLEFRecordUtils.updateChildValue(record, "NARRATIVE", narrative);

		// DATE
		String date = dateField.getText().trim();
		FLEFRecordUtils.updateChildValue(record, "DATE", date);

		// RESOLVES (0:M)
		FLEFRecordUtils.removeChildren(record, "RESOLVES");
		for(String id : resolvesIds){
			FLEFRecordUtils.addChild(record, "RESOLVES", id);
		}

		// PREFERRED (0:1)
		FLEFRecordUtils.updateChildValue(record, "PREFERRED", preferredId);

		// RESEARCH (0:M)
		FLEFRecordUtils.removeChildren(record, "RESEARCH");
		for(String id : researchIds){
			FLEFRecordUtils.addChild(record, "RESEARCH", id);
		}

		// SOURCE_CITATION (0:M)
		FLEFRecordUtils.removeChildren(record, "SOURCE");
		for(FLEFRecord citation : sourceCitations){
			citation.setLevel(1);
			citation.setTag("SOURCE");
			record.addChild(citation);
		}

		return record;
	}

	public void clear(){
		contextField.setText("");
		proofStatusCombo.setSelectedIndex(0);
		narrativeArea.setText("");
		dateField.setText("");
		resolvesIds.clear();
		resolvesListModel.clear();
		preferredId = null;
		preferredDisplayField.setText("");
		clearPreferredBtn.setEnabled(false);
		researchIds.clear();
		researchListModel.clear();
		sourceCitations.clear();
		sourceListModel.clear();
	}

	public boolean hasData(){
		String context = contextField.getText().trim();
		String proofStatus = (String)proofStatusCombo.getSelectedItem();
		return !context.isEmpty() || (proofStatus != null && !proofStatus.isEmpty());
	}

	public boolean validateRequiredFields(){
		String context = contextField.getText().trim();
		if(context.isEmpty()){
			JOptionPane.showMessageDialog(parentDialog,
				"CONTEXT is required for a conclusion.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		String proofStatus = (String)proofStatusCombo.getSelectedItem();
		if(proofStatus == null || proofStatus.isEmpty()){
			JOptionPane.showMessageDialog(parentDialog,
				"PROOF_STATUS is required for a conclusion.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		return true;
	}

}
