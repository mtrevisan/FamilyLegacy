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
package io.github.mtrevisan.familylegacy.ui.dialogs.records;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextArea;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.PanelKey;
import io.github.mtrevisan.familylegacy.ui.dialogs.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.ui.dialogs.api.LinkRole;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionTargetHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.MediaHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Window;
import java.io.IOException;


/**
 * Dialog for editing a {@code CONCLUSION_RECORD} according to FLEF 0.2.0.
 * <p>
 * Structure:
 * <pre>
 * record ConclusionRecord {
 *   issue: Text
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
 * Tab 1 (Properties): issue, proof status, narrative, resolves, preferred
 * Tab 6 (Research): research
 * Tab 7 (Sources): source
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class ConclusionRecordDialog extends BaseRecordDialog{

	private static final String ISSUE_IDENTITY = "identity";

	public static final String PROPERTY_CONCLUSION = "conclusion";


	private final JPanel propertiesPanel;

	private final BoundTextField issueField;
	private final EntityListPanel resolvesPanel;
	private final BoundComboBox<FLEFRecord> preferredCombo;
	private final BoundComboBox<String> proofStatusCombo;
	private final BoundTextArea narrativeArea;


	public static ConclusionRecordDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, ConclusionRecordDialog::new);
	}

	public static ConclusionRecordDialog createEdit(final Window parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, ConclusionRecordDialog::new);
	}


	private ConclusionRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, ConclusionHandler.getInstance());

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]5[]10[]10[]10[]");

		issueField = new BoundTextField(ConclusionReader.TAG_ISSUE);
		resolvesPanel = EntityListPanel.createForOneOfReference(ConclusionReader.TAG_RESOLVES, this, I18N.t("dialog.conclusion.resolves"), model)
			.withHandlerTypes(EventHandler.class, EventParticipationHandler.class, RelationshipHandler.class,
				IndividualHandler.class, IndividualAttributeHandler.class, GroupHandler.class, GroupAttributeHandler.class,
				IdentityHypothesisHandler.class, PlaceHandler.class, PlaceRelationshipHandler.class,
				RepositoryHandler.class, SourceHandler.class, MediaHandler.class, CulturalNormHandler.class,
				HistoricEventHandler.class);
		resolvesPanel.addPropertyChangeListener(PROPERTY_CONCLUSION, evt -> updatePreferredCombo());
		preferredCombo = new BoundComboBox<>(ConclusionReader.TAG_PREFERRED);
		preferredCombo.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus){
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

				final ConclusionTargetHandler handler = ConclusionTargetHandler.getInstance();
				final String displayText = (value != null
					? handler.getDisplayText((FLEFRecord)value, model)
					: StringUtils.SPACE);
				setText(displayText);

				return this;
			}
		});
		proofStatusCombo = new BoundComboBox<>(ConclusionReader.TAG_PROOF_STATUS, ConclusionReader.PROOF_STATUSES);
		proofStatusCombo.setI18NPrefix("enum.proof_status");
		narrativeArea = new BoundTextArea(ConclusionReader.TAG_NARRATIVE, 5, 30);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, this.record)
			.withComponent(PanelKey.RESEARCH_QUESTION, ConclusionReader.TAG_RESEARCH, I18N.t("dialog.component.research.questions"))
			.withComponent(PanelKey.SOURCE, ConclusionReader.TAG_SOURCE, I18N.t("dialog.component.sources_with_citations"))
			.withComponent(PanelKey.PRIVACY, ConclusionReader.TAG_PRIVACY, null)
			.withComponent(PanelKey.AUDIT, ConclusionReader.TAG_AUDIT, null)
			.build();

		components.bind(issueField);
		components.bind(proofStatusCombo);
		components.bind(narrativeArea);


		// Set up the image carousel selection listener on the source list
		setupSourceListSelection();

		finalizeDialog(parent);
	}


	/**
	 * Prepares a new conclusion that resolves the given record: the record is added to the resolved targets, the issue is
	 * suggested if still empty, and the research questions that have the record as target are linked.
	 *
	 * @param target	The record to resolve (it must already be in the model).
	 * @param issue	The suggested issue, used only if the issue is empty.
	 * @return	This dialog.
	 */
	public ConclusionRecordDialog withResolvedTarget(final FLEFRecord target, final String issue){
		resolvesPanel.addItemDirectly(target);
		updatePreferredCombo();

		if(issueField.isEmpty() && StringUtils.isNotBlank(issue))
			issueField.setText(issue);

		final EntityListPanel questionsPanel = (EntityListPanel)components.getPanel(PanelKey.RESEARCH_QUESTION);
		for(final FLEFRecord question : model.getRecordsByType(ResearchQuestionHandler.TYPE))
			if(hasTarget(question, target.getId()))
				questionsPanel.addItemDirectly(question);

		return this;
	}

	/**
	 * Starts the new conclusion with a proof status and a narrative, used only where still empty. The conclusion is not saved: the
	 * user completes it, and decides whether to save it.
	 *
	 * @param proofStatus	The proof status to propose.
	 * @param narrative	The text to start the narrative with.
	 * @return	This dialog.
	 */
	public ConclusionRecordDialog withDraft(final String proofStatus, final String narrative){
		if(StringUtils.isNotBlank(proofStatus))
			proofStatusCombo.setSelectedItem(proofStatus);
		if(narrativeArea.isEmpty() && StringUtils.isNotBlank(narrative))
			narrativeArea.setText(narrative);

		return this;
	}

	private static boolean hasTarget(final FLEFRecord question, final String targetId){
		for(final FLEFRecord questionTarget : FLEFRecordHelper.findChildren(question, ResearchQuestionReader.TAG_TARGET)){
			final FLEFRecord reference = questionTarget.getTheOnlyChild();
			if(reference != null && targetId.equals(reference.getValue()))
				return true;
		}
		return false;
	}

	@Override
	protected JPanel createPropertiesPanel(){
		// issue
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.conclusion.issue") + "*:", GUIHelper.withTooltip(issueField, "tooltip.conclusion.issue"));

		// proof status
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.conclusion.proof_status") + "*:", GUIHelper.withTooltip(proofStatusCombo, "tooltip.conclusion.proof_status"));

		// narrative
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.conclusion.narrative") + ":", GUIHelper.withTooltip(narrativeArea, "tooltip.conclusion.narrative"));

		// resolves
		GUIHelper.addComponent(propertiesPanel, GUIHelper.withTooltip(resolvesPanel, "tooltip.conclusion.resolves"));

		// preferred
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.conclusion.preferred") + ":", GUIHelper.withTooltip(preferredCombo, "tooltip.conclusion.preferred"));

		return propertiesPanel;
	}

	@Override
	protected JPanel createResearchPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]");

		final JPanel researchQuestionPanel = components.getPanel(PanelKey.RESEARCH_QUESTION);
		GUIHelper.addComponent(panel, researchQuestionPanel);

		return panel;
	}

	@Override
	protected JPanel createSourcesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel sourcePanel = components.getPanel(PanelKey.SOURCE);
		GUIHelper.addComponent(panel, sourcePanel);

		// Image carousel below the source list
		// It will be hidden if no images are found
		GUIHelper.addComponent(panel, imageCarouselPanel);

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
		components.load(record);

		resolvesPanel.load(record);
		updatePreferredCombo();

		if(record.hasChildren()){
			// preferred
			final FLEFRecord preferred = FLEFRecordHelper.extractRecordFromOneOfReference(record, ConclusionReader.TAG_PREFERRED, model);
			if(preferred != null && !preferred.isEmpty())
				// Find and select in combo
				preferredCombo.setSelectedItem(preferred);
		}


		// Initially, update carousel based on the first selected source (if any)
		updateCarouselFromSelectedSource();
	}

	private void updatePreferredCombo(){
		preferredCombo.updateItems(resolvesPanel.getItems());
	}

	@Override
	protected boolean validData(){
		if(issueField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.conclusion.issue")),
				tabbedPane, propertiesPanel, issueField);

			return false;
		}

		if(!proofStatusCombo.isValued()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.conclusion.proof_status")),
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
		if(selectedPreferred != null){
			final FLEFRecord preferred = FLEFRecordHelper.getOrCreateTargetNode(record, ConclusionReader.TAG_PREFERRED);
			FLEFRecordHelper.updateChildValue(preferred, selectedPreferred.getTag(), selectedPreferred.getId());
		}
	}


	public static void main(final String[] args) throws IOException{
		GUIHelper.launch(ConclusionRecordDialog::createEdit, "/tests/test.flef", "CC1");
	}


	@Override
	public void link(final LinkRole role, final FLEFRecord record){
		//the conclusion can resolve the record only once it is in the model
		if(role == LinkRole.RESOLVED_TARGET && record.getId() != null && model.getRecordById(record.getId()) != null)
			withResolvedTarget(record, (Strings.CI.equals(record.getTag(), IdentityHypothesisReader.TYPE)? ISSUE_IDENTITY: null));
	}

}
