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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityReferenceListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionTargetHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dialog;
import java.io.Serial;
import java.util.function.Consumer;


/**
 * Dialog for editing a {@code CONCLUSION_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record ConclusionRecord {
 *   id: LocalID
 *   context: Text
 *   proof_status: enum { unresearched, conflicting_evidence, supported, proven, disproven }
 *   narrative?: Text
 *   resolves*: ConclusionTarget
 *   preferred?: ConclusionTarget
 *   research*: Xref&lt;ResearchQuestionRecord&gt;
 *   source*: SourceCitation
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 *
 *   require preferred in resolves
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): context, proof status, narrative, resolves, preferred
 * Tab 6 (Research): research
 * Tab 7 (Sources): source
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class ConclusionRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -2667811782933374258L;


	public static final String PROPERTY_CONCLUSION = "conclusion";


	private static final String TAG_CONTEXT = "CONTEXT";
	private static final String TAG_PROOF_STATUS = "PROOF_STATUS";
	private static final String TAG_NARRATIVE = "NARRATIVE";
	private static final String TAG_RESOLVES = "RESOLVES";
	private static final String TAG_PREFERRED = "PREFERRED";
	private static final String TAG_RESEARCH_QUESTION = "RESEARCH_QUESTION";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_PRIVACY = "PRIVACY";
	private static final String TAG_AUDIT = "AUDIT";


	private final RecordDialogComponents components;

	private final JPanel propertiesPanel;

	private final BoundTextField contextField;
	private final EntityReferenceListPanel resolvesPanel;
	private final BoundComboBox<FLEFRecord> preferredCombo;
	private final BoundComboBox<String> proofStatusCombo;
	private final BoundTextArea narrativeArea;


	public static ConclusionRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, ConclusionRecordDialog::new);
	}

	public static ConclusionRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, ConclusionRecordDialog::new);
	}


	private ConclusionRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, ConclusionHandler.class);

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]5[]10[]10[]10[]");

		contextField = new BoundTextField(TAG_CONTEXT);
		resolvesPanel = EntityReferenceListPanel.createForRecord(TAG_RESOLVES, this, "Resolves", model, ConclusionTargetHandler.class);
		resolvesPanel.addPropertyChangeListener(PROPERTY_CONCLUSION, evt -> updatePreferredCombo());
		preferredCombo = new BoundComboBox<>(TAG_PREFERRED);
		preferredCombo.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus){
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

				var handler = HandlerRegistry.getHandler(ConclusionTargetHandler.class);
				final String displayText = (value != null
					? handler.getDisplayText((FLEFRecord)value, model)
					: StringUtils.SPACE);
				setText(displayText);

				return this;
			}
		});
		proofStatusCombo = new BoundComboBox<>(TAG_PROOF_STATUS, new String[]{
			StringUtils.EMPTY,
			"unresearched", "conflicting_evidence", "supported", "proven", "disproven"
		});
		narrativeArea = new BoundTextArea(TAG_NARRATIVE, 5, 30);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, TAG_RESEARCH_QUESTION, "Research Questions", ResearchQuestionHandler.class, ConclusionHandler.class)
			.withComponent(PanelKey.SOURCE, TAG_SOURCE, "Sources with Citations", SourceHandler.class, SourceCitationHandler.class)
			.withComponent(PanelKey.PRIVACY, TAG_PRIVACY, null, null, null)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null, null, null)
			.build();

		components.bind(contextField);
		components.bind(proofStatusCombo);
		components.bind(narrativeArea);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// context
		GUIHelper.addLabeledComponent(propertiesPanel, "Context*:", contextField);

		// proof status
		GUIHelper.addLabeledComponent(propertiesPanel, "Proof Status*:", proofStatusCombo);

		// narrative
		GUIHelper.addLabeledComponent(propertiesPanel, "Narrative:", narrativeArea);

		// resolves
		GUIHelper.addComponent(propertiesPanel, resolvesPanel);

		// preferred
		GUIHelper.addLabeledComponent(propertiesPanel, "Preferred:", preferredCombo);

		return propertiesPanel;
	}

	@Override
	protected JPanel createResearchPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]");

		final JPanel researchQuestionPanel = components.getPanel(PanelKey.RESEARCH_QUESTION_ON_TARGET);
		GUIHelper.addComponent(panel, researchQuestionPanel);

		return panel;
	}

	@Override
	protected JPanel createSourcesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel sourcePanel = components.getPanel(PanelKey.SOURCE);
		GUIHelper.addComponent(panel, sourcePanel);

		return panel;
	}

	@Override
	protected JPanel createPrivacyPanel(){
		return components.getPanel(PanelKey.PRIVACY);
	}

	@Override
	protected JPanel createAuditPanel(){
		return components.getPanel(PanelKey.AUDIT);
	}


	@Override
	protected void loadData(){
		contextField.setText(FLEFRecordHelper.getChildValue(record, TAG_CONTEXT));
		proofStatusCombo.setSelectedItem(FLEFRecordHelper.getChildValue(record, TAG_PROOF_STATUS));
		narrativeArea.setText(FLEFRecordHelper.getChildValue(record, TAG_NARRATIVE));

		resolvesPanel.load(record);
		updatePreferredCombo();

		if(record.hasChildren()){
			// preferred
			final FLEFRecord preferred = FLEFRecordHelper.extractRecordFromOneOfReference(record, TAG_PREFERRED, model);
			if(preferred != null && !preferred.isEmpty())
				// Find and select in combo
				preferredCombo.setSelectedItem(preferred);
		}

		components.load(record);
	}

	private void updatePreferredCombo(){
		preferredCombo.updateItems(resolvesPanel.getItems());
	}

	@Override
	protected boolean validData(){
		if(contextField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Context is required.",
				tabbedPane, propertiesPanel, contextField);

			return false;
		}

		String proofStatus = (String)proofStatusCombo.getSelectedItem();
		if(StringUtils.isEmpty(proofStatus)){
			GUIHelper.showValidationErrorAndFocus(this,
				"Proof status is required.",
				tabbedPane, propertiesPanel, proofStatusCombo);
			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		components.save(record);

		resolvesPanel.save(record);

		// preferred
		final FLEFRecord selectedPreferred = (FLEFRecord)preferredCombo.getSelectedItem();
		if(selectedPreferred != null)
			FLEFRecordHelper.updateChildValue(record, TAG_PREFERRED, selectedPreferred.getValue());
	}


	public static void main(final String[] args){
//		GUIHelper.launch(ConclusionRecordDialog::createNew);

		final FLEFRecord conclusion = FLEFRecord.createMainRecord("CC1", "CONCLUSION");
		conclusion.addChild(FLEFRecord.createChildWithTagAndValue("CONTEXT", "fdgh"));
		conclusion.addChild(FLEFRecord.createChildWithTag("RESOLVES")
			.addChild(FLEFRecord.createChildWithTagAndValue("PLACE", "@P1@"))
		);
		conclusion.addChild(FLEFRecord.createChildWithTag("RESOLVES")
			.addChild(FLEFRecord.createChildWithTagAndValue("PLACE", "@P2@"))
		);
		conclusion.addChild(FLEFRecord.createChildWithTagAndValue("PROOF_STATUS", "proven"));

		final FLEFRecord place = FLEFRecord.createMainRecord("P1", "PLACE");
		place.addChild(FLEFRecord.createChildWithTag("NAME")
			.addChild(FLEFRecord.createChildWithTag("TEXT")
				.addChild(FLEFRecord.createChildWithTagAndValue("VALUE", "f"))
			)
		);
		place.addChild(FLEFRecord.createChildWithTagAndValue("CONCLUSION", conclusion.getId()));

		final Consumer<FLEFModel> modelFiller = model -> {
			model.addRecord(conclusion);
			model.addRecord(place);
		};
		GUIHelper.launch(ConclusionRecordDialog::createEdit, modelFiller, conclusion);
	}

}
