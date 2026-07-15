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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CalendarHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.FamilyHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/**
 * Dialog for editing a DNA_MATCH_RECORD according to FLEF 0.0.9.
 */
public class DNAMatchDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -8959258199361230237L;


	static{
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new FamilyHandler());
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new EventHandler());
		HandlerRegistry.register(new CulturalNormHandler());
		HandlerRegistry.register(new RepositoryHandler());
		HandlerRegistry.register(new CalendarHandler());
	}

	// ========== Basic fields ==========
	private final JTextField idField = new JTextField(10);
	private final JComboBox<String> artifactSourceCombo = new JComboBox<>(new String[]{
		"", "AncestryDNA", "MyHeritage", "23andMe", "FamilyTreeDNA", "LivingDNA", "Geno", "Other"
	});

	// ========== Individual A (1:1) ==========
	private final JTextField individualADisplayField = new JTextField(20);
	private final JButton individualABrowseBtn = new JButton("Browse...");
	private final JButton individualAClearBtn = new JButton("Clear");
	private String individualAId;

	// ========== Individual B (1:1) ==========
	private final JTextField individualBDisplayField = new JTextField(20);
	private final JButton individualBBrowseBtn = new JButton("Browse...");
	private final JButton individualBClearBtn = new JButton("Clear");
	private String individualBId;

	// ========== SHARED_DNA (1:1) ==========
	private final JTextField sharedDNAField = new JTextField(15);

	// ========== SEGMENTS_COUNT (0:1) ==========
	private final JTextField segmentsCountField = new JTextField(10);

	// ========== CHROMOSOME_MAP (0:M) ==========
	private final DefaultListModel<ChromosomeEntry> chromosomeModel = new DefaultListModel<>();
	private final JList<ChromosomeEntry> chromosomeList = new JList<>(chromosomeModel);
	private final List<ChromosomeEntry> chromosomeEntries = new ArrayList<>();

	// ========== SOURCE_CITATION (0:M) ==========
	private final DefaultListModel<String> sourceCitationListModel = new DefaultListModel<>();
	private final JList<String> sourceCitationList = new JList<>(sourceCitationListModel);
	private final List<FLEFRecord> sourceCitationRecords = new ArrayList<>();

	// ========== MODIFICATION (1:1) ==========
	private final ModificationPanel modificationPanel;

	// ========== Buttons ==========
	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	// ========== Handlers ==========
	private final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler("INDIVIDUAL");
	private final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler("SOURCE");

	// ========== Inner class for Chromosome entry ==========
	private static class ChromosomeEntry{
		String chromosome;
		String start;
		String end;

		ChromosomeEntry(String chromosome, String start, String end){
			this.chromosome = chromosome;
			this.start = start;
			this.end = end;
		}

		@Override
		public String toString(){
			return "Chr " + chromosome + ": " + start + " - " + end;
		}
	}

	// ==================== Constructors ====================
	public DNAMatchDialog(Frame parent, FLEFModel model, FLEFRecord record){
		super(parent, "Edit DNA Match", model, record);

		this.modificationPanel = new ModificationPanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(850, 750));
		pack();
		setLocationRelativeTo(parent);
	}

	public DNAMatchDialog(Frame parent, FLEFModel model){
		super(parent, "New DNA Match", model, null);

		this.modificationPanel = new ModificationPanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(850, 750));
		pack();
		setLocationRelativeTo(parent);
	}

	// ==================== UI Initialization ====================
	@Override
	protected void initComponents(){
		setLayout(new BorderLayout(10, 10));

		JTabbedPane tabbedPane = new JTabbedPane();

		// --- Basic tab ---
		tabbedPane.addTab("Basic", createBasicPanel());

		// --- Chromosomes tab ---
		tabbedPane.addTab("Chromosomes", createChromosomesPanel());

		// --- Source Citations tab ---
		tabbedPane.addTab("Source Citations", createSourceCitationsPanel());

		// --- Modification tab ---
		tabbedPane.addTab("Modification", modificationPanel);

		add(tabbedPane, BorderLayout.CENTER);

		// --- Button panel ---
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		saveButton.addActionListener(e -> save());
		cancelButton.addActionListener(e -> dispose());
	}

	// ==================== Panel factories ====================

	private JPanel createBasicPanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]10[]10[]10[]10[]10[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// ID (read-only)
		idField.setEditable(false);
		idField.setText(record.getId());
		panel.add(new JLabel("ID:"), "align label");
		panel.add(idField, "growx,wrap");

		// ARTIFACT_SOURCE (1:1) - marked with an asterisk
		panel.add(new JLabel("Artifact Source*:"), "align label");
		panel.add(artifactSourceCombo, "growx,wrap");

		// INDIVIDUAL_A (1:1) - marked with an asterisk
		panel.add(new JLabel("Individual A*:"), "align label");
		individualADisplayField.setEditable(false);
		individualADisplayField.setBackground(UIManager.getColor("TextField.background"));
		JPanel aPanel = new JPanel(new BorderLayout(5, 5));
		aPanel.add(individualADisplayField, BorderLayout.CENTER);
		JPanel aBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		aBtnPanel.add(individualABrowseBtn);
		aBtnPanel.add(individualAClearBtn);
		aPanel.add(aBtnPanel, BorderLayout.EAST);
		panel.add(aPanel, "growx,wrap");

		// INDIVIDUAL_B (1:1) - marked with an asterisk
		panel.add(new JLabel("Individual B*:"), "align label");
		individualBDisplayField.setEditable(false);
		individualBDisplayField.setBackground(UIManager.getColor("TextField.background"));
		JPanel bPanel = new JPanel(new BorderLayout(5, 5));
		bPanel.add(individualBDisplayField, BorderLayout.CENTER);
		JPanel bBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		bBtnPanel.add(individualBBrowseBtn);
		bBtnPanel.add(individualBClearBtn);
		bPanel.add(bBtnPanel, BorderLayout.EAST);
		panel.add(bPanel, "growx,wrap");

		// SHARED_DNA (1:1) - marked with an asterisk
		panel.add(new JLabel("Shared DNA (cM)*:"), "align label");
		panel.add(sharedDNAField, "growx,wrap");

		// SEGMENTS_COUNT (0:1)
		panel.add(new JLabel("Segments Count:"), "align label");
		panel.add(segmentsCountField, "growx");

		// Listeners
		individualABrowseBtn.addActionListener(e -> browseIndividualA());
		individualAClearBtn.addActionListener(e -> {
			individualAId = null;
			individualADisplayField.setText("");
		});
		individualBBrowseBtn.addActionListener(e -> browseIndividualB());
		individualBClearBtn.addActionListener(e -> {
			individualBId = null;
			individualBDisplayField.setText("");
		});

		return panel;
	}

	private JPanel createChromosomesPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder("Chromosome Map"));

		chromosomeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		chromosomeList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editChromosome();
				}
			}
		});
		JScrollPane scrollPane = new JScrollPane(chromosomeList);
		scrollPane.setPreferredSize(new Dimension(200, 100));
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add Chromosome");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(editBtn);
		btnPanel.add(deleteBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		chromosomeList.addListSelectionListener(e -> {
			boolean selected = chromosomeList.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addChromosome());
		editBtn.addActionListener(e -> editChromosome());
		deleteBtn.addActionListener(e -> deleteChromosome());

		return panel;
	}

	private JPanel createSourceCitationsPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder("Source Citation"));

		sourceCitationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		sourceCitationList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editSourceCitation();
				}
			}
		});
		JScrollPane scrollPane = new JScrollPane(sourceCitationList);
		scrollPane.setPreferredSize(new Dimension(200, 100));
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add Source");
		JButton newBtn = new JButton("New Source");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(newBtn);
		btnPanel.add(editBtn);
		btnPanel.add(deleteBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		sourceCitationList.addListSelectionListener(e -> {
			boolean selected = sourceCitationList.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addSourceCitation());
		newBtn.addActionListener(e -> createNewSource());
		editBtn.addActionListener(e -> editSourceCitation());
		deleteBtn.addActionListener(e -> deleteSourceCitation());

		return panel;
	}

	// ==================== Individual browsing ====================

	private void browseIndividualA(){
		if(individualHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Individual handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, individualHandler, selectedId -> {
			if(selectedId != null){
				individualAId = selectedId;
				FLEFRecord rec = model.getRecordById(selectedId);
				if(rec != null){
					individualADisplayField.setText(individualHandler.getDisplayName(rec));
				}
				else{
					individualADisplayField.setText(selectedId);
				}
			}
		}
		);
		dialog.setVisible(true);
	}

	private void browseIndividualB(){
		if(individualHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Individual handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, individualHandler, selectedId -> {
			if(selectedId != null){
				individualBId = selectedId;
				FLEFRecord rec = model.getRecordById(selectedId);
				if(rec != null){
					individualBDisplayField.setText(individualHandler.getDisplayName(rec));
				}
				else{
					individualBDisplayField.setText(selectedId);
				}
			}
		}
		);
		dialog.setVisible(true);
	}

	// ==================== Chromosome methods ====================

	private void addChromosome(){
		ChromosomeEntry entry = showChromosomeDialog(null);
		if(entry != null){
			chromosomeEntries.add(entry);
			chromosomeModel.addElement(entry);
		}
	}

	private void editChromosome(){
		int idx = chromosomeList.getSelectedIndex();
		if(idx == -1)
			return;
		ChromosomeEntry existing = chromosomeEntries.get(idx);
		ChromosomeEntry updated = showChromosomeDialog(existing);
		if(updated != null){
			chromosomeEntries.set(idx, updated);
			chromosomeModel.set(idx, updated);
		}
	}

	private void deleteChromosome(){
		int idx = chromosomeList.getSelectedIndex();
		if(idx == -1)
			return;
		if(!showConfirm("Confirm", "Remove this chromosome entry?"))
			return;
		chromosomeEntries.remove(idx);
		chromosomeModel.remove(idx);
	}

	private ChromosomeEntry showChromosomeDialog(ChromosomeEntry existing){
		JDialog dialog = new JDialog(this, existing == null? "Add Chromosome": "Edit Chromosome", true);
		dialog.setLayout(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]10[]10[]"));
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		JComboBox<String> chromosomeCombo = new JComboBox<>(new String[]{
			"", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
			"11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
			"21", "22", "X", "Y"
		});
		JTextField startField = new JTextField(10);
		JTextField endField = new JTextField(10);

		if(existing != null){
			chromosomeCombo.setSelectedItem(existing.chromosome);
			startField.setText(existing.start);
			endField.setText(existing.end);
		}

		dialog.add(new JLabel("Chromosome*:"), "align label");
		dialog.add(chromosomeCombo, "growx,wrap");
		dialog.add(new JLabel("Start Position*:"), "align label");
		dialog.add(startField, "growx,wrap");
		dialog.add(new JLabel("End Position*:"), "align label");
		dialog.add(endField, "growx,wrap");

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		dialog.add(btnPanel, "span 2,growx");

		final ChromosomeEntry[] result = {null};
		okBtn.addActionListener(e -> {
			String chrom = (String)chromosomeCombo.getSelectedItem();
			String start = startField.getText().trim();
			String end = endField.getText().trim();
			if(chrom == null || chrom.isEmpty()){
				JOptionPane.showMessageDialog(dialog, "Chromosome is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if(start.isEmpty()){
				JOptionPane.showMessageDialog(dialog, "Start position is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if(end.isEmpty()){
				JOptionPane.showMessageDialog(dialog, "End position is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			result[0] = new ChromosomeEntry(chrom, start, end);
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setMinimumSize(new Dimension(350, 200));
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);

		return result[0];
	}

	// ==================== Source Citation methods ====================

	private void addSourceCitation(){
		SourceCitationDialog dialog = new SourceCitationDialog(getParentFrame(), model, null);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord citation = dialog.getCitationRecord();
			if(citation != null){
				citation.setLevel(1);
				citation.setTag("SOURCE_CITATION");
				sourceCitationRecords.add(citation);
				sourceCitationListModel.addElement(getSourceCitationDisplay(citation));
			}
		}
	}

	private void editSourceCitation(){
		int idx = sourceCitationList.getSelectedIndex();
		if(idx == -1)
			return;
		FLEFRecord existing = sourceCitationRecords.get(idx);
		SourceCitationDialog dialog = new SourceCitationDialog(getParentFrame(), model, existing);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord updated = dialog.getCitationRecord();
			if(updated != null){
				sourceCitationRecords.set(idx, updated);
				sourceCitationListModel.set(idx, getSourceCitationDisplay(updated));
			}
		}
	}

	private void deleteSourceCitation(){
		int idx = sourceCitationList.getSelectedIndex();
		if(idx == -1)
			return;
		if(!showConfirm("Confirm", "Remove this source citation?"))
			return;
		sourceCitationRecords.remove(idx);
		sourceCitationListModel.remove(idx);
	}

	private String getSourceCitationDisplay(FLEFRecord citation){
		String sourceId = citation.getValue();
		if(sourceId != null){
			FLEFRecord rec = model.getRecordById(sourceId);
			if(rec != null && sourceHandler != null)
				return sourceHandler.getDisplayName(rec);
			return sourceId;
		}
		return "[empty]";
	}

	private void createNewSource(){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		JDialog dialog = sourceHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
	}

	// ==================== Load Data ====================

	@Override
	protected void loadData(){
		idField.setText(record.getId());

		// ARTIFACT_SOURCE (1:1)
		String artifact = FLEFRecordUtils.getChildValue(record, "ARTIFACT_SOURCE");
		artifactSourceCombo.setSelectedItem(artifact != null? artifact: "");

		// INDIVIDUAL_A (1:1)
		String aId = FLEFRecordUtils.getChildValue(record, "INDIVIDUAL_A");
		if(aId != null && !aId.isEmpty()){
			individualAId = aId;
			FLEFRecord rec = model.getRecordById(aId);
			if(rec != null && individualHandler != null){
				individualADisplayField.setText(individualHandler.getDisplayName(rec));
			}
			else{
				individualADisplayField.setText(aId);
			}
		}

		// INDIVIDUAL_B (1:1)
		String bId = FLEFRecordUtils.getChildValue(record, "INDIVIDUAL_B");
		if(bId != null && !bId.isEmpty()){
			individualBId = bId;
			FLEFRecord rec = model.getRecordById(bId);
			if(rec != null && individualHandler != null){
				individualBDisplayField.setText(individualHandler.getDisplayName(rec));
			}
			else{
				individualBDisplayField.setText(bId);
			}
		}

		// SHARED_DNA (1:1)
		sharedDNAField.setText(FLEFRecordUtils.getChildValue(record, "SHARED_DNA"));

		// SEGMENTS_COUNT (0:1)
		segmentsCountField.setText(FLEFRecordUtils.getChildValue(record, "SEGMENTS_COUNT"));

		// CHROMOSOME_MAP (0:M)
		chromosomeEntries.clear();
		chromosomeModel.clear();
		FLEFRecord chromMap = FLEFRecordUtils.findChild(record, "CHROMOSOME_MAP");
		if(chromMap != null){
			for(FLEFRecord child : chromMap.getChildren()){
				if("CHROMOSOME".equals(child.getTag())){
					String chrom = child.getValue();
					String start = null;
					String end = null;
					for(FLEFRecord sub : child.getChildren()){
						if("START_POSITION".equals(sub.getTag())){
							start = sub.getValue();
						}
						else if("END_POSITION".equals(sub.getTag())){
							end = sub.getValue();
						}
					}
					if(chrom != null && start != null && end != null){
						ChromosomeEntry entry = new ChromosomeEntry(chrom, start, end);
						chromosomeEntries.add(entry);
						chromosomeModel.addElement(entry);
					}
				}
			}
		}

		// SOURCE_CITATION (0:M)
		sourceCitationRecords.clear();
		sourceCitationListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("SOURCE_CITATION".equals(child.getTag())){
				sourceCitationRecords.add(child);
				sourceCitationListModel.addElement(getSourceCitationDisplay(child));
			}
		}

		// MODIFICATION
		modificationPanel.loadFromRecord(record);
	}

	// ==================== Validation ====================

	@Override
	protected boolean validateData(){
		// ARTIFACT_SOURCE (1:1)
		String artifact = (String)artifactSourceCombo.getSelectedItem();
		if(artifact == null || artifact.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"ARTIFACT_SOURCE is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			artifactSourceCombo.requestFocusInWindow();
			return false;
		}

		// INDIVIDUAL_A (1:1)
		if(individualAId == null || individualAId.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"INDIVIDUAL_A is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		// INDIVIDUAL_B (1:1)
		if(individualBId == null || individualBId.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"INDIVIDUAL_B is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		// SHARED_DNA (1:1)
		if(sharedDNAField.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(this,
				"SHARED_DNA is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			sharedDNAField.requestFocusInWindow();
			return false;
		}

		// MODIFICATION_STRUCTURE (1:1)
		if(!modificationPanel.hasData()){
			JOptionPane.showMessageDialog(this,
				"Modification is required.\nPlease add a CREATION date.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return modificationPanel.validateRequiredFields();
	}

	// ==================== Save ====================

	@Override
	protected void saveRecord(){
		// Validation is already done by save() before calling this method
		record.getChildren().clear();

		// ARTIFACT_SOURCE (1:1)
		String artifact = (String)artifactSourceCombo.getSelectedItem();
		if(artifact != null && !artifact.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "ARTIFACT_SOURCE", artifact);
		}

		// INDIVIDUAL_A (1:1)
		if(individualAId != null && !individualAId.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "INDIVIDUAL_A", individualAId);
		}

		// INDIVIDUAL_B (1:1)
		if(individualBId != null && !individualBId.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "INDIVIDUAL_B", individualBId);
		}

		// SHARED_DNA (1:1)
		String shared = sharedDNAField.getText().trim();
		if(!shared.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "SHARED_DNA", shared);
		}

		// SEGMENTS_COUNT (0:1)
		String segments = segmentsCountField.getText().trim();
		if(!segments.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "SEGMENTS_COUNT", segments);
		}

		// CHROMOSOME_MAP (0:M)
		if(!chromosomeEntries.isEmpty()){
			FLEFRecord chromMap = new FLEFRecord();
			chromMap.setLevel(1);
			chromMap.setTag("CHROMOSOME_MAP");
			record.addChild(chromMap);

			for(ChromosomeEntry entry : chromosomeEntries){
				FLEFRecord chrom = new FLEFRecord();
				chrom.setLevel(2);
				chrom.setTag("CHROMOSOME");
				chrom.setValue(entry.chromosome);
				chromMap.addChild(chrom);

				FLEFRecord start = new FLEFRecord();
				start.setLevel(3);
				start.setTag("START_POSITION");
				start.setValue(entry.start);
				chrom.addChild(start);

				FLEFRecord end = new FLEFRecord();
				end.setLevel(3);
				end.setTag("END_POSITION");
				end.setValue(entry.end);
				chrom.addChild(end);
			}
		}

		// SOURCE_CITATION (0:M)
		for(FLEFRecord citation : sourceCitationRecords){
			citation.setLevel(1);
			citation.setTag("SOURCE_CITATION");
			record.addChild(citation);
		}

		// MODIFICATION
		modificationPanel.saveToRecord(record);

		if(isNew){
			model.addRecord(record);
		}
		dispose();
	}

	// ==================== Overrides ====================

	@Override
	protected FLEFRecord createNewRecord(){
		return FLEFRecord.createMainRecord(generateNewId(), "DNA_MATCH");
	}

	@Override
	protected String generateNewId(){
		return FLEFRecordUtils.generateNewId(model, "DNA_MATCH", "D");
	}

	private Frame getParentFrame(){
		Container parent = getParent();
		while(parent != null && !(parent instanceof Frame)){
			parent = parent.getParent();
		}
		return (Frame)parent;
	}

	// ==================== Main per test ====================

	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();

		// Aggiungi un individuo di esempio
		FLEFRecord ind = FLEFRecord.createMainRecord("I1", "INDIVIDUAL");
		FLEFRecord name = new FLEFRecord();
		name.setLevel(1);
		name.setTag("NAME");
		FLEFRecord given = new FLEFRecord();
		given.setLevel(2);
		given.setTag("INDIVIDUAL_NAME");
		given.setValue("John");
		name.addChild(given);
		FLEFRecord family = new FLEFRecord();
		family.setLevel(2);
		family.setTag("FAMILY_NAME");
		family.setValue("Doe");
		name.addChild(family);
		ind.addChild(name);
		model.addRecord(ind);

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test DNA Match Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New DNA Match");
			btn.addActionListener(e -> {
				DNAMatchDialog dialog = new DNAMatchDialog(frame, model);
				dialog.setVisible(true);
				System.out.println("DNA Match saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
