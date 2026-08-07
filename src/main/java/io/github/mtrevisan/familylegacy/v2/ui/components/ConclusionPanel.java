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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchStatusHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.io.Serial;
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

	@Serial
	private static final long serialVersionUID = -2652632946970438571L;


	private static final String TAG_CONTEXT = "CONTEXT";
	private static final String TAG_PROOF_STATUS = "PROOF_STATUS";
	private static final String TAG_NARRATIVE = "NARRATIVE";
	private static final String TAG_DATE = "DATE";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_RESOLVES = "RESOLVES";
	private static final String TAG_PREFERRED = "PREFERRED";
	private static final String TAG_RESEARCH = "RESEARCH";


	static{
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new ResearchStatusHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final FLEFModel model;
	private final Dialog parent;

	private final String path;

	// Bound fields
	private final BoundTextField contextField = new BoundTextField(TAG_CONTEXT, 30);
	private final BoundComboBox<String> proofStatusCombo = new BoundComboBox<>(TAG_PROOF_STATUS,
		new String[]{StringUtils.EMPTY, "unresearched", "conflicting_evidence", "preponderance_of_evidence", "proven", "disproven"});
	private final BoundTextArea narrativeArea = new BoundTextArea(TAG_NARRATIVE, 3, 25);
	private final BoundTextField dateField = new BoundTextField(TAG_DATE, 15);

	// RESOLVES
	private final ResolvesListPanel resolvesPanel;

	// PREFERRED (0:1) - single selection
	private final JTextField preferredDisplayField = new JTextField(20);
	private final JButton browsePreferredBtn = new JButton("Browse...");
	private final JButton clearPreferredBtn = new JButton("Clear");
	private String preferredId;

	// RESEARCH
	private final ResearchStatusListPanel researchPanel;

	// SOURCE_CITATION
	private final SourceCitationListPanel sourceCitationPanel;


	/**
	 * Constructs a new ConclusionPanel.
	 *
	 * @param parent the parent dialog (used for showing message dialogs)
	 * @param model  the FLEF model
	 */
	public ConclusionPanel(final String path, Dialog parent, FLEFModel model){
		this.parent = parent;

		this.model = model;
		this.path = path;

		this.resolvesPanel = new ResolvesListPanel(model, parent);
		this.researchPanel = new ResearchStatusListPanel(parent, model);
		this.sourceCitationPanel = new SourceCitationListPanel(TAG_SOURCE, parent, model);

		initComponents();
	}


	private void initComponents(){
		bindingManager.bind(contextField);
		bindingManager.bind(proofStatusCombo);
		bindingManager.bind(narrativeArea);
		bindingManager.bind(dateField);

		setLayout(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]5[]5[]5[]5[]"));

		// CONTEXT
		add(new JLabel("Context*:"), "align label");
		contextField.setToolTipText("e.g., 'birth_date', 'marriage_place', 'parentage', 'death_cause', 'relationship_type'");
		add(contextField, "growx,wrap");

		// PROOF_STATUS
		add(new JLabel("Proof Status*:"), "align label");
		add(proofStatusCombo, "growx,wrap");

		// NARRATIVE
		add(new JLabel("Narrative:"), "align label,top");
		add(GUIHelper.createScrollPane(narrativeArea), "growx,wrap");

		// DATE
		add(new JLabel("Date:"), "align label");
		dateField.setToolTipText("ISO 8601 date (YYYY-MM-DD)");
		add(dateField, "growx,wrap");

		// RESOLVES
		add(resolvesPanel, "span 2,growx,wrap");

		// PREFERRED
		add(createPreferredPanel(), "span 2,growx,wrap");

		// RESEARCH
		add(researchPanel, "span 2,growx,wrap");

		// SOURCE_CITATION
		add(sourceCitationPanel, "span 2,growx,wrap");
	}


	private JPanel createPreferredPanel(){
		final JPanel panel = new JPanel(new MigLayout("fillx,top", "[right]rel[grow]"));
		panel.setBorder(new TitledBorder("Preferred Record"));

		preferredDisplayField.setEditable(false);
		preferredDisplayField.setBackground(UIManager.getColor("TextField.background"));

		final JPanel preferredPanel = new JPanel(new BorderLayout(5, 5));
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
		String input = JOptionPane.showInputDialog(parent,
			"Enter the XREF ID of the preferred record (e.g., @E123@, @I456@):",
			"Select Preferred Record", JOptionPane.PLAIN_MESSAGE);
		if(StringUtils.isNotEmpty(input)){
			preferredId = input.trim();
			preferredDisplayField.setText(preferredId);
			clearPreferredBtn.setEnabled(true);
		}
	}

	private void clearPreferred(){
		preferredId = null;
		preferredDisplayField.setText(StringUtils.EMPTY);
		clearPreferredBtn.setEnabled(false);
	}


	/**
	 * Loads data from a CONCLUSION record.
	 *
	 * @param record the CONCLUSION record, or null
	 */
	public void load(FLEFRecord record){
		clear();

		final FLEFRecord conclusionRecord = FLEFRecordHelper.findChild(record, path);
		if(conclusionRecord == null)
			return;

		bindingManager.load(conclusionRecord);

		// RESOLVES
		List<String> resolves = new ArrayList<>();
		for(FLEFRecord child : conclusionRecord.getChildren())
			if("RESOLVES".equals(child.getTag()) && child.getValue() != null)
				resolves.add(child.getValue());
		resolvesPanel.setItems(resolves);

		// PREFERRED
		preferredId = FLEFRecordHelper.getChildValue(conclusionRecord, "PREFERRED");
		if(preferredId != null && !preferredId.isEmpty()){
			preferredDisplayField.setText(preferredId);
			clearPreferredBtn.setEnabled(true);
		}

		// RESEARCH
		List<String> researchIds = new ArrayList<>();
		for(FLEFRecord child : conclusionRecord.getChildren())
			if("RESEARCH".equals(child.getTag()) && child.getValue() != null)
				researchIds.add(child.getValue());
		researchPanel.setItems(researchIds);

		// SOURCE_CITATION
		sourceCitationPanel.load(conclusionRecord);
	}

	/**
	 * Saves the current data into a CONCLUSION record.
	 *
	 * @param target the target record (will be cleared and filled)
	 * @return the target record, or null if no data
	 */
	public FLEFRecord save(FLEFRecord target){
		if(!hasData())
			return null;

		FLEFRecord record = target != null? target: FLEFRecord.createChild(path);
		FLEFRecordHelper.removeAllChildren(record);

		// Save bound fields
		bindingManager.save(record);

		// Save RESOLVES
		for(String id : resolvesPanel.getItems())
			FLEFRecordHelper.updateChildValue(record, TAG_RESOLVES, XRefHelper.formatXRef(id));

		// Save PREFERRED
		if(preferredId != null && !preferredId.isEmpty())
			FLEFRecordHelper.updateChildValue(record, TAG_PREFERRED, preferredId);

		// Save RESEARCH
		for(String id : researchPanel.getItems())
			FLEFRecordHelper.updateChildValue(record, TAG_RESEARCH, XRefHelper.formatXRef(id));

		// Save SOURCE_CITATION
		record.addChildrenWithTag(ConclusionPanel.TAG_SOURCE, sourceCitationPanel.getItems());

		return record;
	}

	/**
	 * Clears all fields.
	 */
	public void clear(){
		contextField.setText(StringUtils.EMPTY);
		proofStatusCombo.setSelectedIndex(0);
		narrativeArea.setText(StringUtils.EMPTY);
		dateField.setText(StringUtils.EMPTY);
		resolvesPanel.clear();
		preferredId = null;
		preferredDisplayField.setText(StringUtils.EMPTY);
		clearPreferredBtn.setEnabled(false);
		researchPanel.clear();
		sourceCitationPanel.clear();
	}

	/**
	 * Checks whether this panel has any data.
	 *
	 * @return true if there is data, false otherwise
	 */
	public boolean hasData(){
		String context = contextField.getText();
		String proofStatus = (String)proofStatusCombo.getSelectedItem();
		return (!context.isEmpty()
			|| (proofStatus != null && !proofStatus.isEmpty())
			|| !narrativeArea.isEmpty()
			|| !resolvesPanel.isEmpty()
			|| (preferredId != null && !preferredId.isEmpty())
			|| !researchPanel.isEmpty()
			|| !sourceCitationPanel.isEmpty());
	}

	/**
	 * Validates the required fields.
	 *
	 * @return true if valid, false otherwise
	 */
	public boolean validateData(){
		String context = contextField.getText();
		if(context.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(parent,
				"CONTEXT is required for a conclusion.",
				null, null, contextField);

			return false;
		}

		String proofStatus = (String)proofStatusCombo.getSelectedItem();
		if(proofStatus == null || proofStatus.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(parent,
				"PROOF_STATUS is required for a conclusion.",
				null, null, proofStatusCombo);

			return false;
		}

		return true;
	}

	public List<String> getResolvesIds(){
		return resolvesPanel.getItems();
	}

	public String getPreferredId(){
		return preferredId;
	}

	public List<String> getResearchIds(){
		return researchPanel.getItems();
	}

}
