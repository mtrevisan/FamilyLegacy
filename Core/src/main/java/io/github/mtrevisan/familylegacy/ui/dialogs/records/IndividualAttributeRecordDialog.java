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
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceCitationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Window;
import java.io.IOException;


/**
 * Dialog for editing a {@code INDIVIDUAL_ATTRIBUTE_RECORD} according to FLEF 0.1.3.
 * <p>
 * Structure:
 * <pre>
 * record IndividualAttributeRecord {
 *   id: LocalID
 *   individual: Xref&lt;IndividualRecord&gt;
 *   type: enum {
 *     characteristic, residence, occupation, possession, military_rank, caste, social_class, ethnicity, citizenship,
 *     nationality, ssn, title, children_count, marriages_count, religion, language, literacy, education
 *   } | Text
 *   value?: Text
 *   valid_from?: DateStructure
 *   valid_to?: DateStructure
 *   place?: PlaceCitation
 *   source*: SourceCitation
 *   note*: Xref&lt;NoteRecord&gt;
 *   evidence?: EvidenceQualifiers
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): individual, type, value, valid_from, valid_to, place, evidence
 * Tab 5 (Context): ContextImpactRecord (target[individual_attribute] = this attribute)
 * Tab 6 (Research): ConclusionRecord (resolves = this attribute), ResearchQuestionRecord (target[individual_attribute] = this attribute)
 * Tab 7 (Sources): source
 * Tab 8 (Notes): note
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class IndividualAttributeRecordDialog extends BaseRecordDialog{

	private final JPanel propertiesPanel;

	private final BoundComboBox<String> typeCombo;
	private final BoundTextField valueField;
	private final DateField validFromField;
	private final DateField validToField;
	private final EntityField placeField;


	public static IndividualAttributeRecordDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, IndividualAttributeRecordDialog::new);
	}

	public static IndividualAttributeRecordDialog createEdit(final Window parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, IndividualAttributeRecordDialog::new);
	}


	private IndividualAttributeRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, IndividualAttributeHandler.getInstance());

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]5[]10[]10[]10[]");

		typeCombo = new BoundComboBox<>(IndividualAttributeReader.TAG_TYPE, GUIHelper.fillCombo(IndividualAttributeReader.TYPES, null));
		typeCombo.setI18NPrefix("enum.individual.attribute.type");
		typeCombo.setEditable(true);
		valueField = new BoundTextField(IndividualAttributeReader.TAG_VALUE);
		validFromField = DateField.createWithWrapperTag(IndividualAttributeReader.TAG_VALID_FROM, this, I18N.t("dialog.valid.from"), model);
		validToField = DateField.createWithWrapperTag(IndividualAttributeReader.TAG_VALID_TO, this, I18N.t("dialog.valid.to"), model);
		placeField = EntityField.createForStructureWithReference(IndividualAttributeReader.TAG_PLACE, this, model, PlaceCitationHandler.class);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_TARGET, ContextImpactHandler.TYPE, I18N.t("dialog.component.context.impact"))
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, ConclusionHandler.TYPE, I18N.t("dialog.component.conclusions"))
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, ResearchQuestionHandler.TYPE, I18N.t("dialog.component.research.questions"))
			.withComponent(PanelKey.SOURCE, IndividualAttributeReader.TAG_SOURCE, I18N.t("dialog.component.sources.with.citations"))
			.withComponent(PanelKey.NOTE, IndividualAttributeReader.TAG_NOTE, null)
			.withComponent(PanelKey.EVIDENCE, IndividualAttributeReader.TAG_EVIDENCE, I18N.t("dialog.component.evidence"))
			.withComponent(PanelKey.PRIVACY, IndividualAttributeReader.TAG_PRIVACY, null)
			.withComponent(PanelKey.AUDIT, IndividualAttributeReader.TAG_AUDIT, null)
			.build();

		components.bind(typeCombo);
		components.bind(valueField);


		// Set up the image carousel selection listener on the source list
		setupSourceListSelection();

		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// individual
		//parentEntity

		// type
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.individual.attribute.type") + "*:", typeCombo);

		// value
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.individual.attribute.value") + ":", valueField);

		// validity range:
		final JPanel validityPanel = GUIHelper.createLabelFieldPanel(5, "[]5[]");
		validityPanel.setBorder(BorderFactory.createTitledBorder(I18N.t("dialog.validity.range")));
		// valid from
		GUIHelper.addLabeledComponent(validityPanel, I18N.t("dialog.valid.from") + ":", validFromField);
		// valid to
		GUIHelper.addLabeledComponent(validityPanel, I18N.t("dialog.valid.to") + ":", validToField);
		GUIHelper.addComponent(propertiesPanel, validityPanel);

		// place
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.place") + ":", placeField);

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
	protected JPanel createPrivacyPanel(){
		return components.getPanel(PanelKey.PRIVACY);
	}

	@Override
	protected JPanel createAuditPanel(){
		return components.getPanel(PanelKey.AUDIT);
	}


	public IndividualAttributeRecordDialog withIndividual(final String individualId){
		if(StringUtils.isNotEmpty(individualId)){
			if(!confirmRecordExistsForType(individualId, IndividualHandler.getInstance().getLabel()))
				return this;

			final FLEFRecord temporary = FLEFRecord.createMainRecord(individualId, IndividualHandler.TYPE);
			withParentEntity(temporary);
			refreshLayout();
		}

		return this;
	}

	private void refreshLayout(){
		if(isShowing()){
			revalidate();
			repaint();

			pack();
		}
	}


	@Override
	protected void loadData(){
		// load parent individual reference
		final String individualId = FLEFRecordHelper.getChildValue(record, IndividualAttributeReader.TAG_INDIVIDUAL);
		if(StringUtils.isNotEmpty(individualId)){
			final FLEFRecord temporary = FLEFRecord.createMainRecord(individualId, IndividualHandler.TYPE);
			withParentEntity(temporary);
		}


		validFromField.load(record);
		validToField.load(record);
		placeField.load(record);

		components.load(record);


		// Initially, update carousel based on the first selected source (if any)
		updateCarouselFromSelectedSource();
	}

	@Override
	protected boolean validData(){
		if(parentEntity.isEmpty()){
			JOptionPane.showMessageDialog(this,
				I18N.tf("validation.required", I18N.t("validation.required.parent.field")),
				I18N.t("validation.title"), JOptionPane.ERROR_MESSAGE);

			return false;
		}

		if(!typeCombo.isValued()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.individual.type")),
				tabbedPane, propertiesPanel, typeCombo);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		record.getChildren()
			.removeIf(child -> IndividualAttributeReader.TAG_INDIVIDUAL.equalsIgnoreCase(child.getTag()));
		record.addChild(FLEFRecord.createChildWithTagAndValue(parentEntity.getPath(), parentEntity.getText()));


		validFromField.save(record);
		validToField.save(record);
		placeField.saveReferences(record);

		components.save(record);
	}


	public static void main(final String[] args) throws IOException{
		GUIHelper.launch(IndividualAttributeRecordDialog::createEdit, "/tests/test.flef", "IA1");
	}

}
