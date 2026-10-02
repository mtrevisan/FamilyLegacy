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
import io.github.mtrevisan.familylegacy.io.model.readers.CulturalNormReader;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceCitationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.Window;
import java.io.IOException;


/**
 * Dialog for editing a {@code CULTURAL_NORM_RECORD} according to FLEF 0.1.3.
 * <p>
 * Structure:
 * <pre>
 * record CulturalNormRecord {
 *   id: LocalID
 *   title?: Text
 *   rule_type?: enum {
 *     age_of_majority, marriage_minimum_age, baptism_age, confirmation_age, military_service_age, retirement_age,
 *     naming_convention, surname_transmission, patronymic_system, matronymic_system, title_usage,
 *     inheritance_rule, succession_rule, dowry_practice, guardianship_rule, adoption_practice,
 *     marriage_practice, marriage_prohibited_degree, widowhood_rule,
 *     residence_pattern, household_structure, social_classification,
 *     religious_practice, burial_practice,
 *     citizenship_rule, legitimacy_rule,
 *     age_difference_convention, generational_interval
 *   } | Text
 *   place?: PlaceCitation
 *   valid_from?: DateStructure
 *   valid_to?: DateStructure
 *   source*: SourceCitation
 *   note*: Xref&lt;NoteRecord&gt;
 *   evidence?: EvidenceQualifiers
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): title, rule_type, place, valid_from, valid_to, evidence
 * Tab 5 (Context): ContextImpactRecord (context[cultural_norm] = this norm)
 * Tab 6 (Research): ConclusionRecord (resolves = this norm), ResearchQuestionRecord (target[cultural_norm] = this norm)
 * Tab 7 (Sources): source
 * Tab 8 (Notes): note
 * Tab 10 (Audit): audit
 */
public class CulturalNormRecordDialog extends BaseRecordDialog{

	private final BoundTextField titleField;
	private final BoundComboBox<String> typeCombo;
	private final EntityField placeField;
	private final EvidenceQualifiersPanel placeEvidencePanel;
	private final DateField validFromField;
	private final DateField validToField;


	public static CulturalNormRecordDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, CulturalNormRecordDialog::new);
	}

	public static CulturalNormRecordDialog createEdit(final Window parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, CulturalNormRecordDialog::new);
	}


	private CulturalNormRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, CulturalNormHandler.getInstance());

		titleField = new BoundTextField(CulturalNormReader.TAG_TITLE);
		typeCombo = new BoundComboBox<>(CulturalNormReader.TAG_TYPE, GUIHelper.fillCombo(CulturalNormReader.TYPES, null));
		typeCombo.setI18NPrefix("enum.cultural.norm.rule.type");
		typeCombo.setEditable(true);
		placeField = EntityField.createForStructureWithReference(PlaceHandler.TYPE, this, model, PlaceCitationHandler.class);
		placeEvidencePanel = new EvidenceQualifiersPanel(CulturalNormReader.TAG_PLACE_EVIDENCE, I18N.t("dialog.component.evidence"));
		validFromField = DateField.createWithWrapperTag(CulturalNormReader.TAG_VALID_FROM, this, I18N.t("dialog.date.valid.from"), model);
		validToField = DateField.createWithWrapperTag(CulturalNormReader.TAG_VALID_TO, this, I18N.t("dialog.date.valid.to"), model);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_CONTEXT, ContextImpactHandler.TYPE, I18N.t("dialog.component.context.impact"))
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, ConclusionHandler.TYPE, I18N.t("dialog.component.conclusions"))
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, ResearchQuestionHandler.TYPE, I18N.t("dialog.component.research.questions"))
			.withComponent(PanelKey.SOURCE, CulturalNormReader.TAG_SOURCE, I18N.t("dialog.component.sources.with.citations"))
			.withComponent(PanelKey.NOTE, CulturalNormReader.TAG_NOTE, null)
			.withComponent(PanelKey.EVIDENCE, CulturalNormReader.TAG_EVIDENCE, I18N.t("dialog.component.evidence"))
			.withComponent(PanelKey.AUDIT, CulturalNormReader.TAG_AUDIT, null)
			.build();

		components.bind(titleField);
		components.bind(typeCombo);


		// Set up the image carousel selection listener on the source list
		setupSourceListSelection();

		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		final JPanel propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]5[]10[]10[]10[]");

		// title
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.cultural.norm.title") + ":", titleField);

		// rule type
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.cultural.norm.rule.type") + ":", typeCombo);

		// place panel:
		final JPanel placePanel = new JPanel(new MigLayout("ins 10,hidemode 3,fillx,top,wrap 1",
			"[grow,fill]", "[]10[]"));
		placePanel.setBorder(new TitledBorder(I18N.t("dialog.place.with.citation")));
		placePanel.add(placeField, "growx");
		placePanel.add(placeEvidencePanel, "growx");
		GUIHelper.addComponent(propertiesPanel, placePanel);

		// validity range:
		final JPanel validityPanel = GUIHelper.createLabelFieldPanel(5, "[]10[]");
		validityPanel.setBorder(BorderFactory.createTitledBorder(I18N.t("dialog.validity.range")));
		// valid from
		GUIHelper.addLabeledComponent(validityPanel, I18N.t("dialog.valid.from") + ":", validFromField);
		// valid to
		GUIHelper.addLabeledComponent(validityPanel, I18N.t("dialog.valid.to") + ":", validToField);
		GUIHelper.addComponent(propertiesPanel, validityPanel);

		// evidence
		final JPanel evidencePanel = components.getPanel(PanelKey.EVIDENCE);
		GUIHelper.addComponent(propertiesPanel, evidencePanel);

		return propertiesPanel;
	}

	@Override
	protected JPanel createContextPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel contextPanel = components.getPanel(PanelKey.CONTEXT_IMPACT_ON_CONTEXT);
		GUIHelper.addComponent(panel, contextPanel);

		return panel;
	}

	@Override
	protected JPanel createResearchPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]");

		// conclusion
		final JPanel conclusionPanel = components.getPanel(PanelKey.CONCLUSION_ON_RESOLVES);
		GUIHelper.addComponent(panel, conclusionPanel);

		// research question
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


	@Override
	protected void loadData(){
		components.load(record);

		placeField.load(record);
		placeEvidencePanel.load(record);
		validFromField.load(record);
		validToField.load(record);


		// Initially, update carousel based on the first selected source (if any)
		updateCarouselFromSelectedSource();
	}

	@Override
	protected void saveData(){
		components.save(record);

		placeField.saveReferences(record);
		placeEvidencePanel.save(record);
		validFromField.save(record);
		validToField.save(record);
	}


	public static void main(final String[] args) throws IOException{
		GUIHelper.launch(CulturalNormRecordDialog::createEdit, "/tests/test.flef", "CN1");
	}

}
