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
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityReferenceListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.Dialog;
import java.io.Serial;


/* ONGOING */
/**
 * Dialog for editing a {@code CONCLUSION_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record ConclusionRecord {
 *   id: LocalID
 *   context: Text
 *   resolves*: ConclusionTarget
 *   preferred?: ConclusionTarget
 *   proof_status: enum { unresearched, conflicting_evidence, supported, proven, disproven }
 *   narrative?: Text
 *   research*: Xref&lt;ResearchQuestionRecord&gt;
 *   date?: Date
 *   source*: SourceCitation
 *   restriction?: RestrictionStructure
 *   modification: ModificationStructure
 *
 *   require preferred in resolves
 * }
 * </pre>
 */
public class ConclusionRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -2667811782933374258L;


	private static final String TAG_CONTEXT = "CONTEXT";
	private static final String TAG_RESOLVES = "RESOLVES";
	private static final String TAG_PREFERRED = "PREFERRED";
	private static final String TAG_PROOF_STATUS = "PROOF_STATUS";
	private static final String TAG_NARRATIVE = "NARRATIVE";
	private static final String TAG_RESEARCH = "RESEARCH";
	private static final String TAG_DATE = "DATE";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_RESTRICTION = "RESTRICTION";


	static{
		HandlerRegistry.register(new ConclusionHandler());
		HandlerRegistry.register(new ResearchQuestionHandler());
	}


	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]10[]10[]10[]10[]10[]"));

	private final BindingManager bindingManager = new BindingManager();

	private final BoundTextField contextField;
//	private final ConclusionTargetListPanel resolvesPanel;
	private final BoundComboBox<String> preferredCombo;
	private final BoundComboBox<String> proofStatusCombo;
	private final BoundTextArea narrativeArea;
	private final EntityReferenceListPanel researchPanel;
	private final DateField dateField;
	private final SourceCitationListPanel sourcePanel;
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;


	public static ConclusionRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, ConclusionRecordDialog::new);
	}

	public static ConclusionRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, ConclusionRecordDialog::new);
	}


	private ConclusionRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(ConclusionHandler.TYPE));

		contextField = new BoundTextField(TAG_CONTEXT, 30);
//		resolvesPanel = new ConclusionTargetListPanel(TAG_RESOLVES, parent, model);
//		resolvesPanel.addPropertyChangeListener(ConclusionTargetListPanel.PROPERTY_ITEMS, evt -> updatePreferredCombo());
		preferredCombo = new BoundComboBox<>(TAG_PREFERRED, new String[]{
			StringUtils.EMPTY});
		proofStatusCombo = new BoundComboBox<>(TAG_PROOF_STATUS, new String[]{
			StringUtils.EMPTY,
			"unresearched", "conflicting_evidence", "supported", "proven", "disproven"
		});
		narrativeArea = new BoundTextArea(TAG_NARRATIVE, 5, 30);
		researchPanel = new EntityReferenceListPanel(TAG_RESEARCH, this, "Research Questions", model, ResearchQuestionHandler.TYPE)
			.withParentEntity(this.record.getId(), ConclusionHandler.TYPE);
		dateField = DateField.createWithWrapperTag(TAG_DATE, this, "Conclusion Date", model);
		sourcePanel = new SourceCitationListPanel(TAG_SOURCE, this, "Sources", model);
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		modificationPanel = new ModificationPanel(this);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(contextField);
		bindingManager.bind(preferredCombo);
		bindingManager.bind(proofStatusCombo);
		bindingManager.bind(narrativeArea);


		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Restriction", restrictionPanel);
		tabbedPane.addTab("Modification", modificationPanel);

		finalizeLayout(tabbedPane);
	}

	private JPanel createMainPanel(){
		// context
		mainPanel.add(new JLabel("Context*:"), "align label");
		mainPanel.add(contextField, "growx,wrap");

		// resolves
//		mainPanel.add(resolvesPanel, "span 2,growx,wrap");

		// preferred
		mainPanel.add(new JLabel("Preferred:"), "align label");
		mainPanel.add(preferredCombo, "growx,wrap");

		// proof status
		mainPanel.add(new JLabel("Proof Status*:"), "align label");
		mainPanel.add(proofStatusCombo, "growx,wrap");

		// narrative
		mainPanel.add(new JLabel("Narrative:"), "align label");
		mainPanel.add(GUIHelper.createScrollPane(narrativeArea), "growx,wrap");

		// research
		mainPanel.add(researchPanel, "span 2,growx,wrap");

		// date
		mainPanel.add(new JLabel("Date:"), "align label");
		mainPanel.add(dateField, "growx");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]"));
		panel.add(sourcePanel, "growx");
		return panel;
	}

	private void updatePreferredCombo(){
		final String currentSelection = (String)preferredCombo.getSelectedItem();
		preferredCombo.removeAllItems();
		preferredCombo.addItem(StringUtils.EMPTY);

//		for(final FLEFRecord target : resolvesPanel.getItems()){
//			final String display = resolvesPanel.getDisplay(target);
//			final String id = XRefHelper.extractXRef(target.getValue());
//			preferredCombo.addItem(id + " - " + display);
//		}

		if(currentSelection != null && !currentSelection.isEmpty())
			// Restore selection
			for(int i = 0; i < preferredCombo.getItemCount(); i ++)
				if(preferredCombo.getItemAt(i).equals(currentSelection)){
					preferredCombo.setSelectedIndex(i);

					break;
				}
	}

	@Override
	protected void loadData(){
		contextField.setText(FLEFRecordHelper.getChildValue(record, TAG_CONTEXT));
		proofStatusCombo.setSelectedItem(FLEFRecordHelper.getChildValue(record, TAG_PROOF_STATUS));
		narrativeArea.setText(FLEFRecordHelper.getChildValue(record, TAG_NARRATIVE));

		dateField.load(record);
//		resolvesPanel.load(record);

		updatePreferredCombo();

		// Load preferred
		String prefRef = FLEFRecordHelper.getChildValue(record, TAG_PREFERRED);
		if(StringUtils.isNotEmpty(prefRef)){
			String prefId = XRefHelper.extractXRef(prefRef);
			// Find and select in combo
			for(int i = 0; i < preferredCombo.getItemCount(); i ++){
				String item = preferredCombo.getItemAt(i);
				if(item != null && item.startsWith(prefId)){
					preferredCombo.setSelectedIndex(i);
					break;
				}
			}
		}

		researchPanel.load(record);
		sourcePanel.load(record);
		restrictionPanel.load(record);
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		String context = contextField.getText();
		if(context.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Context is required.",
				tabbedPane, mainPanel, contextField);
			return false;
		}

		String proofStatus = (String)proofStatusCombo.getSelectedItem();
		if(StringUtils.isEmpty(proofStatus)){
			GUIHelper.showValidationErrorAndFocus(this,
				"Proof status is required.",
				tabbedPane, mainPanel, proofStatusCombo);
			return false;
		}

		// Check preferred in resolves constraint
//		String selectedPreferred = (String)preferredCombo.getSelectedItem();
//		if(StringUtils.isNotEmpty(selectedPreferred)){
//			// Extract the ID from the selected item
//			int dashIdx = selectedPreferred.indexOf(" - ");
//			if(dashIdx > 0){
//				String prefId = selectedPreferred.substring(0, dashIdx);
//				List<String> resolveIds = resolvesPanel.getTargetIds();
//				if(!resolveIds.contains(prefId)){
//					GUIHelper.showValidationErrorAndFocus(this,
//						"Preferred must be one of the resolves.",
//						tabbedPane, mainPanel, preferredCombo);
//					return false;
//				}
//			}
//		}

		return true;
	}

	@Override
	protected void saveData(){
		// Simple fields
		FLEFRecordHelper.updateChildValue(record, TAG_CONTEXT, contextField.getText());
		FLEFRecordHelper.updateChildValue(record, TAG_PROOF_STATUS, (String)proofStatusCombo.getSelectedItem());
		FLEFRecordHelper.updateChildValue(record, TAG_NARRATIVE, narrativeArea.getText());

		dateField.save(record);
//		resolvesPanel.save(record);

		// Preferred
		FLEFRecordHelper.removeChildren(record, TAG_PREFERRED);
		String selectedPreferred = (String)preferredCombo.getSelectedItem();
		if(StringUtils.isNotEmpty(selectedPreferred)){
			int dashIdx = selectedPreferred.indexOf(" - ");
			if(dashIdx > 0){
				String prefId = selectedPreferred.substring(0, dashIdx);
				FLEFRecordHelper.updateChildValue(record, TAG_PREFERRED, XRefHelper.formatXRef(prefId));
			}
		}

		researchPanel.saveReferences(record);
		sourcePanel.save(record);
		restrictionPanel.save(record);
		modificationPanel.save(record);
	}


	public static void main(final String[] args){
		GUIHelper.launch(ConclusionRecordDialog::createNew);
	}

}
