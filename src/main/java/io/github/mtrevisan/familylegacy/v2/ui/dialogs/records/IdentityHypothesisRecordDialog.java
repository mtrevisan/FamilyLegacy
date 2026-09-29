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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs.records;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.RecordDiffDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.IOException;
import java.util.List;
import java.util.Objects;


/**
 * Dialog for editing an {@code IDENTITY_HYPOTHESIS_RECORD} according to FLEF 0.1.3.
 * <p>
 * Structure:
 * <pre>
 * record IdentityHypothesisRecord {
 *   id: LocalID
 *   identity+: IdentityCandidate
 *   comment?: Text
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 *   audit: AuditStructure
 *
 *   require count(identity) == 2
 *   require identity[0] != identity[1]
 *   require type(identity[0]) == type(identity[1])
 * }
 *
 * IdentityCandidate = oneof {
 *   individual: Xref&lt;IndividualRecord&gt;
 *   group: Xref&lt;GroupRecord&gt;
 *   place: Xref&lt;PlaceRecord&gt;
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): identity 1, identity 2, comment, evidence
 * Tab 5 (Context): ContextImpactRecord (target[identity_hypothesis] = this hypothesis)
 * Tab 6 (Research): ConclusionRecord (resolves = this hypothesis), ResearchQuestionRecord (target[identity_hypothesis] = this hypothesis)
 * Tab 7 (Sources): source
 * Tab 10 (Audit): audit
 */
public class IdentityHypothesisRecordDialog extends BaseRecordDialog{

	private final JPanel propertiesPanel;

	private final EntityField identity1Field;
	private final EntityField identity2Field;
	private final BoundTextArea commentArea;
	private final JButton compareIdentitiesButton = new JButton(I18N.t("dialog.identity.hypothesis.button.compare"));


	public static IdentityHypothesisRecordDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, IdentityHypothesisRecordDialog::new);
	}

	public static IdentityHypothesisRecordDialog createEdit(final Window parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, IdentityHypothesisRecordDialog::new);
	}


	private IdentityHypothesisRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, IdentityHypothesisHandler.getInstance());

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]");

		identity1Field = EntityField.createForRecordFromOneofReference(IdentityHypothesisHandler.TAG_IDENTITY, this, model)
			.withHandlerTypes(IndividualHandler.class, GroupHandler.class, PlaceHandler.class);
		identity2Field = EntityField.createForRecordFromOneofReference(IdentityHypothesisHandler.TAG_IDENTITY, this, model)
			.withHandlerTypes(IndividualHandler.class, GroupHandler.class, PlaceHandler.class);
		commentArea = new BoundTextArea(IdentityHypothesisHandler.TAG_COMMENT, 3, 30);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_TARGET, ContextImpactHandler.TYPE, I18N.t("dialog.component.context.impact"))
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, ConclusionHandler.TYPE, I18N.t("dialog.component.conclusions"))
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, ResearchQuestionHandler.TYPE, I18N.t("dialog.component.research.questions"))
			.withComponent(PanelKey.SOURCE, IdentityHypothesisHandler.TAG_SOURCE, I18N.t("dialog.component.sources.with.citations"))
			.withComponent(PanelKey.NOTE, IdentityHypothesisHandler.TAG_NOTE, null)
			.withComponent(PanelKey.EVIDENCE, IdentityHypothesisHandler.TAG_EVIDENCE, I18N.t("dialog.component.evidence"))
			.withComponent(PanelKey.AUDIT, IdentityHypothesisHandler.TAG_AUDIT, null)
			.build();

		components.bind(commentArea);

		compareIdentitiesButton.addActionListener(e -> openIdentityComparison());

		// Set up the image carousel selection listener on the source list
		setupSourceListSelection();

		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// identity 1
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.identity.hypothesis.identity.one") + "*:", identity1Field);

		// identity 2
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.identity.hypothesis.identity.two") + "*:", identity2Field);

		// compare button, right under the two identity fields
		final JPanel compareRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		compareRow.setOpaque(false);
		compareRow.add(compareIdentitiesButton);
		GUIHelper.addComponent(propertiesPanel, compareRow);

		// comment
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.identity.hypothesis.comment") + ":", commentArea);

		// evidence
		final JPanel evidencePanel = components.getPanel(PanelKey.EVIDENCE);
		GUIHelper.addComponent(propertiesPanel, evidencePanel);

		return propertiesPanel;
	}

	@Override
	protected JPanel createContextPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel contextPanel = components.getPanel(PanelKey.CONTEXT_IMPACT_ON_TARGET);
		GUIHelper.addComponent(panel, contextPanel);

		return panel;
	}

	@Override
	protected JPanel createResearchPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]");

		final JPanel conclusionPanel = components.getPanel(PanelKey.CONCLUSION_ON_RESOLVES);
		GUIHelper.addComponent(panel, conclusionPanel);

		final JPanel researchQuestionPanel = components.getPanel(PanelKey.RESEARCH_QUESTION_ON_TARGET);
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
	protected JPanel createNotesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel notePanel = components.getPanel(PanelKey.NOTE);
		GUIHelper.addComponent(panel, notePanel);

		return panel;
	}

	@Override
	protected JPanel createAuditPanel(){
		return components.getPanel(PanelKey.AUDIT);
	}

	/**
	 * Opens a read-only {@link RecordDiffDialog} showing how the two
	 * identity candidates differ. Both candidates must be set and must
	 * resolve to a record in the model.
	 */
	private void openIdentityComparison(){
		final FLEFRecord identity1Ref = identity1Field.getEntity();
		final FLEFRecord identity2Ref = identity2Field.getEntity();
		if(identity1Ref == null || identity1Ref.getId() == null || identity2Ref == null || identity2Ref.getId() == null){
			JOptionPane.showMessageDialog(this,
				I18N.t("error.identities.set"),
				I18N.t("error.title"),
				JOptionPane.WARNING_MESSAGE);

			return;
		}

		final FLEFRecord identity1 = model.getRecordById(identity1Ref.getId());
		final FLEFRecord identity2 = model.getRecordById(identity2Ref.getId());
		if(identity1 == null || identity2 == null){
			JOptionPane.showMessageDialog(this,
				I18N.t("error.identities.not.present"),
				I18N.t("error.title"),
				JOptionPane.WARNING_MESSAGE);

			return;
		}

		RecordDiffDialog.showComparison(
			SwingUtilities.getWindowAncestor(this),
			I18N.t("dialog.identity.hypothesis.comparison.title"),
			identity1,
			identity2);
	}



	public void withIdentity(final FLEFRecord identity){
		super.withParentEntity(identity);

		if(parentEntity != null && !parentEntity.isEmpty()){
			final List<FLEFRecord> entities = FLEFRecordHelper.extractRecordsFromOneOfReference(record, parentEntity.getPath(), model);

			final boolean choice = (entities.isEmpty() || entities.get(0).getId().equals(identity.getId()));
			final EntityField field = (choice? identity1Field: identity2Field);
			final JLabel label = GUIHelper.getLabeledComponent(choice? identity2Field: identity1Field);

			label.setText(I18N.t("dialog.identity.hypothesis.identity") + "*:");
			field.setEntity(FLEFRecord.createMainRecord(parentEntity.getText(), parentEntity.getPath()));

			GUIHelper.setComponentVisible(field, false);
		}
	}

	@Override
	protected void loadData(){
		identity1Field.load(record, 0);
		identity2Field.load(record, 1);

		components.load(record);


		// Initially, update carousel based on the first selected source (if any)
		updateCarouselFromSelectedSource();
	}

	@Override
	protected boolean validData(){
		if(!identity1Field.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.identity.hypothesis.identity.one")),
				tabbedPane, propertiesPanel, identity1Field);

			return false;
		}

		if(!identity2Field.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.identity.hypothesis.identity.two")),
				tabbedPane, propertiesPanel, identity2Field);

			return false;
		}

		// identities must be different records
		final FLEFRecord identity1 = identity1Field.getEntity();
		final String identity1Id = (identity1 != null? identity1.getId(): null);
		final FLEFRecord identity2 = identity2Field.getEntity();
		final String identity2Id = (identity2 != null? identity2.getId(): null);
		if(Objects.equals(identity1Id, identity2Id)){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required.not.same", I18N.t("dialog.identity.hypothesis.identity.one"), I18N.t("dialog.identity.hypothesis.identity.two")),
				tabbedPane, propertiesPanel, identity1Field);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		identity1Field.saveReferences(record);
		identity2Field.saveReferences(record);

		components.save(record);
	}


	public static void main(final String[] args) throws IOException{
		GUIHelper.launch(IdentityHypothesisRecordDialog::createEdit, "/tests/test.flef", "IH1");
	}

}
