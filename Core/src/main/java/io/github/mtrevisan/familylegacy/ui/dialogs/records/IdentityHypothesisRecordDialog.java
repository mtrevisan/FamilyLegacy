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
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextArea;
import io.github.mtrevisan.familylegacy.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.RecordDiffDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

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
 * Dialog for editing an {@code IDENTITY_HYPOTHESIS_RECORD} according to FLEF 0.2.0.
 * <p>
 * Structure:
 * <pre>
 * record IdentityHypothesisRecord {
 *   identity+: IdentityCandidate
 *   rationale?: Text
 *   source*: SourceCitation
 *   confidence?: Confidence
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
 * Tab 1 (Properties): identity 1, identity 2, rationale, confidence
 * Tab 5 (Context): ContextImpactRecord (target[identity_hypothesis] = this hypothesis)
 * Tab 6 (Research): ConclusionRecord (resolves = this hypothesis), ResearchQuestionRecord (target[identity_hypothesis] = this hypothesis)
 * Tab 7 (Sources): source
 * Tab 10 (Audit): audit
 */
public class IdentityHypothesisRecordDialog extends BaseRecordDialog{

	private final JPanel propertiesPanel;

	private final EntityField identity1Field;
	private final EntityField identity2Field;
	private final BoundTextArea rationaleArea;
	private final BoundComboBox<String> confidenceCombo;

	private final JButton compareIdentitiesButton = new JButton(I18N.t("dialog.identity_hypothesis.button.compare"));


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

		identity1Field = EntityField.createForRecordFromOneofReference(IdentityHypothesisReader.TAG_IDENTITY, this, model)
			.withHandlerTypes(IndividualHandler.class, GroupHandler.class, PlaceHandler.class);
		identity2Field = EntityField.createForRecordFromOneofReference(IdentityHypothesisReader.TAG_IDENTITY, this, model)
			.withHandlerTypes(IndividualHandler.class, GroupHandler.class, PlaceHandler.class);
		rationaleArea = new BoundTextArea(IdentityHypothesisReader.TAG_RATIONALE, 3, 30);
		confidenceCombo = new BoundComboBox<>(IdentityHypothesisReader.TAG_CONFIDENCE, GUIHelper.fillCombo(ConfidenceReader.CONFIDENCES, null));
		confidenceCombo.setI18NPrefix("enum.confidence");

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, this.record)
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_TARGET, ContextImpactHandler.TYPE, I18N.t("dialog.component.context.impacts"))
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, ConclusionHandler.TYPE, I18N.t("dialog.component.conclusions"))
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, ResearchQuestionHandler.TYPE, I18N.t("dialog.component.research.questions"))
			.withComponent(PanelKey.SOURCE, IdentityHypothesisReader.TAG_SOURCE, I18N.t("dialog.component.sources_with_citations"))
			.withComponent(PanelKey.AUDIT, IdentityHypothesisReader.TAG_AUDIT, null)
			.build();

		components.bind(rationaleArea);
		components.bind(confidenceCombo);

		compareIdentitiesButton.addActionListener(e -> openIdentityComparison());

		// Set up the image carousel selection listener on the source list
		setupSourceListSelection();

		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// identity 1
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.identity_hypothesis.identity.one") + "*:", GUIHelper.withTooltip(identity1Field, "tooltip.identity_hypothesis.identity.one"));

		// identity 2
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.identity_hypothesis.identity.two") + "*:", GUIHelper.withTooltip(identity2Field, "tooltip.identity_hypothesis.identity.two"));

		// compare button, right under the two identity fields
		final JPanel compareRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		compareRow.setOpaque(false);
		compareRow.add(compareIdentitiesButton);
		GUIHelper.addComponent(propertiesPanel, compareRow);

		// rationale
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.identity_hypothesis.rationale") + ":", GUIHelper.withTooltip(rationaleArea, "tooltip.identity_hypothesis.rationale"));

		// confidence
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.identity_hypothesis.confidence") + ":", GUIHelper.withTooltip(confidenceCombo, "tooltip.identity_hypothesis.confidence"));

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
				I18N.t("error.identities_set"),
				I18N.t("error.title"),
				JOptionPane.WARNING_MESSAGE);

			return;
		}

		final FLEFRecord identity1 = model.getRecordById(identity1Ref.getId());
		final FLEFRecord identity2 = model.getRecordById(identity2Ref.getId());
		if(identity1 == null || identity2 == null){
			JOptionPane.showMessageDialog(this,
				I18N.t("error.identities_not_present"),
				I18N.t("error.title"),
				JOptionPane.WARNING_MESSAGE);

			return;
		}

		RecordDiffDialog.showComparison(
			SwingUtilities.getWindowAncestor(this),
			I18N.t("dialog.identity_hypothesis.comparison.title"),
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

			label.setText(I18N.t("dialog.identity_hypothesis.identity") + "*:");
			field.setEntity(FLEFRecord.createMainRecord(parentEntity.getText(), parentEntity.getPath()));

			GUIHelper.setComponentEnabled(field, false);
		}
	}

	/**
	 * Starts the new hypothesis with the two records it is about, a rationale and a confidence. The hypothesis is not saved: the user
	 * completes it, and decides whether to save it.
	 *
	 * @param first	The first record (it must already be in the model).
	 * @param second	The second record (it must already be in the model).
	 * @param rationale	The text to start the rationale with, used only where still empty.
	 * @param confidence	The confidence to propose ({@code low}, {@code medium}, {@code high}), or {@code null}.
	 * @return	This dialog.
	 */
	public IdentityHypothesisRecordDialog withCandidates(final FLEFRecord first, final FLEFRecord second, final String rationale,
			final String confidence){
		identity1Field.setEntity(first);
		identity2Field.setEntity(second);

		if(rationaleArea.isEmpty() && rationale != null && !rationale.isBlank())
			rationaleArea.setText(rationale);
		if(confidence != null)
			confidenceCombo.setSelectedItem(confidence);

		return this;
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
				I18N.tf("validation.required", I18N.t("dialog.identity_hypothesis.identity.one")),
				tabbedPane, propertiesPanel, identity1Field);

			return false;
		}

		if(!identity2Field.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.identity_hypothesis.identity.two")),
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
				I18N.tf("validation.required_not_same", I18N.t("dialog.identity_hypothesis.identity.one"), I18N.t("dialog.identity_hypothesis.identity.two")),
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
